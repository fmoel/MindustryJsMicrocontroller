package mindustrymod.jslogic;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import arc.util.*;
import mindustry.logic.*;
import arc.func.*;

import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;

public class JsExecutor extends LExecutor implements Debugger {
    private boolean isInitialized;
    public long instructionsPerTick;

    public Context context;
    public Scriptable scope;
    public String code;
    public String cooperativeCode;

    // Control the javascript thread
    public boolean isRunning = false;
    private Thread executionThread;
    public Object singleStepLock = new Object();
    public final Object startLock = new Object();
    public boolean hasErrors = false;

    private int currentLineNumber = 1;
    public String consoleLog = "";
    public Cons<String> consoleListener;
    public JsWrapper jsWrapper;
    public JsWrapper.Console console;
    public long sleepUntil = 0;

    public JsExecutor() {
        this.isInitialized = false;
        unit = new LVar("@unit");
        executionThread = new Thread(() -> {
            while(true){
                try{
                    synchronized(startLock){
                        startLock.wait();
                    }
                    isRunning = true;
                    hasErrors = false;
                    while (isRunning){
                        initializeContext(); // Initialize the context and start the script
                        try {
                            context.evaluateString(scope, this.cooperativeCode, "script", 1, null);
                        } catch (AbortCodeExecution e) {
                            isRunning = false;
                        } catch (Throwable e){
                            hasErrors = true;
                            isRunning = false;
                            console.error(getStackTrace(e));                            
                        } finally {
                            cleanupContext(); 
                        }
                    }
                }catch(InterruptedException e){
                    // this interrupt would stop the thread
                    isRunning = false;
                    Log.info("JS thread finds a bitter end");
                    return;
                }catch(Throwable e){
                    Log.err(e);
                }
            }
        });
        executionThread.start();
    }

    // Loads the JavaScript code into the executor
    public void load(String code) {

        thisv = new LVar("@this");
        thisv.setobj(build);

        this.code = code;

        this.isInitialized = !code.isEmpty();
        cooperativeCode = makeCodeCooperative(code);

        // interrupt running program
        if(isRunning){
            for(long timeOut = Time.millis() + 10; isRunning && timeOut < Time.millis(); ){
                executionThread.interrupt();
                try{
                    Thread.sleep(1);
                }catch(InterruptedException e){}
            }
        }

        try{
            Thread.sleep(1);
        }catch(InterruptedException e){}

        // start the the (new) code
        synchronized(startLock){
            startLock.notify();
        }
    }

    public String makeCodeCooperative(String code) {        
        // yield while({cpu.yield()||}1);  
        String cooperativeCode = code.replaceAll("\\bwhile\\b(\\s*)\\(", "while$1(cpu.yield()||");

        // yield for(var i = 0; i < 1;{cpu.yield(),}i--); 
        cooperativeCode = cooperativeCode.replaceAll("\\bfor\\b(\\s*)\\(([^;]*;[^;]*;)", "for$1($2cpu.yield(),");
        return cooperativeCode;
    }

    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    // Executes exactly one line of code (or until one LAsm call)
    @Override
    public void runOnce() {
        if (!isInitialized) {
            return;
        }
        if (sleepUntil > Time.nanos()) {
            return;
        }
        synchronized (singleStepLock) {
            singleStepLock.notify(); // Resume the paused thread
        }
    }

    // will be called from the script thread eg. via cpu.yield()
    public void sendToYield() {
        synchronized (singleStepLock) {
            try {
                singleStepLock.wait();
            } catch (InterruptedException e) {
                throw new AbortCodeExecution();
            }
        }
    }

    // Returns if the executor is initialized with code
    @Override
    public boolean initialized() {
        return isInitialized;
    }

    public class AbortCodeExecution extends Error{}

    public static class SandboxNativeJavaObject extends NativeJavaObject {
        public SandboxNativeJavaObject(Scriptable scope, Object javaObject, Class<?> staticType) {
            super(scope, javaObject, staticType);
        }

        @Override
        public Object get(String name, Scriptable start) {
            if (name.equals("getClass")) {
                return NOT_FOUND;
            }

            return super.get(name, start);
        }
    }

    public static class SandboxWrapFactory extends WrapFactory {
        @Override
        public Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Object javaObject, Class<?> staticType) {
            return new SandboxNativeJavaObject(scope, javaObject, staticType);
        }
    }

    public class SandboxContextFactory extends ContextFactory {
        @Override
        protected boolean hasFeature(Context cx, int featureIndex) {
            switch (featureIndex) {
                case Context.FEATURE_ENABLE_JAVA_MAP_ACCESS:
                    return true;
            }
            return super.hasFeature(cx, featureIndex);
        }

        @Override
        protected Context makeContext() {
            Context cx = super.makeContext();
            cx.setWrapFactory(new SandboxWrapFactory());
            cx.setClassShutter(new ClassShutter() {
                public boolean visibleToScripts(String className) {
                    if (className.startsWith("mindustrymod.jslogic.JsWrapper"))
                        return true;
                    if (className.startsWith("java.lang.Object"))
                        return true;
                    if (className.startsWith("java.lang.Class"))
                        return true;
                    if (className.startsWith("java.lang.String"))
                        return true;
                    if (className.startsWith("java.lang.Double"))
                        return true;

                    Log.info("requested className but denied: " + className);
                    return false;
                }
            });
            return cx;
        }
    }

    // Initializes the context and attaches the debugger
    private void initializeContext() {
        ContextFactory sandboxFactory = new SandboxContextFactory();
        context = sandboxFactory.enterContext();
        // context = Context.enter();
        context.setOptimizationLevel(-1); // Run in interpreted mode for easier debugging
        context.setInstructionObserverThreshold(1);
        context.setGeneratingDebug(true);
        context.setDebugger(this, null);
        scope = context.initStandardObjects();
        
        jsWrapper = new JsWrapper(this, scope);
        this.console = jsWrapper.console;

    }

    public void setConsoleListener(Cons<String> listener) {
        consoleListener = listener;
    }

    public String getConsoleLog() {
        return consoleLog;
    }

    @Override
    public void handleCompilationDone(Context cx, DebuggableScript fnOrScript, String source) {
        // No additional action needed here
    }

    @Override
    public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript) {
        return new SandboxDebugFrame(this);
    }

    private class SandboxDebugFrame implements DebugFrame {

        private final JsExecutor executor;

        public SandboxDebugFrame(JsExecutor executor) {
            this.executor = executor;
        }

        @Override
        public void onEnter(Context cx, Scriptable activation, Scriptable thisObj, Object[] args) {
            // No additional action needed on function entry
        }

        @Override
        public void onLineChange(Context cx, int lineNumber) {
            executor.currentLineNumber = lineNumber;
            console.log("onLineChange line " + lineNumber);
            synchronized (executor.singleStepLock) {
                try {
                    executor.singleStepLock.wait();
                } catch (InterruptedException e) {
                    throw new AbortCodeExecution();
                }
            }
        }

        @Override
        public void onExit(Context cx, boolean byThrow, Object resultOrException) {
            // No additional action needed on function exit
        }

        @Override
        public void onExceptionThrown(Context cx, Throwable ex) {
            ex.printStackTrace();
            executor.cleanupContext();
        }

        @Override
        public void onDebuggerStatement(Context cx) {
            // Handle 'debugger' statements if present
        }
    }

    public int getCurrentLineNumber() {
        return currentLineNumber;
    }

    private void cleanupContext() {
        if (context != null) {
            Context.exit(); // Properly exit the context
            context = null; // Nullify the context to allow reinitialization
            scope = null; // Nullify the scope for a fresh start
        }
    }

    public Set<String> getAllVariableNames() {
        Set<String> variables = new HashSet<>();
        if (scope == null)
            return variables;
        for (Object id : scope.getIds()) {
            variables.add(id.toString());
        }
        return variables;
    }

    public Object getVariableValue(String name) {
        if (scope == null)
            return null;
        Object value = scope.get(name, scope);
        return value == Scriptable.NOT_FOUND ? null : Context.jsToJava(value, Object.class);
    }

    public void setVariableValue(String name, Object value) {
        if (scope == null)
            return;
        scope.put(name, scope, value);
    }
}