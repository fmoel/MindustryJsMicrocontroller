package mindustrymod.jslogic;

import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.logic.*;
import mindustry.logic.LExecutor.*;
import mindustry.Vars;
import mindustrymod.jslogic.JsLogicBlock.*;
import mindustry.world.blocks.logic.LogicBlock.*;
import mindustry.world.blocks.logic.LogicDisplay.*;
import mindustry.world.meta.BlockFlag;
import org.mozilla.javascript.*;

/* WARNING:
 * Everything in this class can be exposed to JS. 
 * This includes classes enums and other things. 
 * Make sure to properly encapsulate all public classes, variables etc.
 * In case you need other public classes in JS then the one existing,
 * they must be actively allowed in JsExecutor.SandboxContextFactory.makeContext.
*/
public class JsWrapper {
    private JsExecutor executor;
    public CPU cpu;
    public JsConsole console;

    public JsWrapper(JsExecutor executor, Scriptable scope) {
        this.executor = executor;
        this.console = new JsConsole(executor);
        cpu = new CPU();

        ScriptableObject.putProperty(scope, "cpu", Context.javaToJS(cpu, scope));
        ScriptableObject.putProperty(scope, "console", Context.javaToJS(console, scope));
        
        createJsEnum(scope, RadarSort.class);
        createJsEnum(scope, RadarTarget.class);
        createJsEnum(scope, BlockFlag.class);
    }
    
    private <T extends Enum<T>> void createJsEnum(Scriptable scope, Class<T> enumType){
        createJsEnum(scope, enumType, null);
    }

    private <T extends Enum<T>> void createJsEnum(Scriptable scope, Class<T> enumType, String name){
        Scriptable enumObj = executor.context.newObject(scope);
        for(T enumValue : enumType.getEnumConstants()){
            ScriptableObject.putConstProperty(enumObj, enumValue.name(), enumValue);
        }
        String className = name  == null ? enumType.getName().substring(enumType.getName().lastIndexOf(".") + 1) : name;
        ScriptableObject.putProperty(scope, className, Context.javaToJS(enumObj, scope));
    }

    public class CPU extends JsBuilding {
        private final JsLogicBuild logicBuild;
        public final JsCanvas canvas;
        public long instructionsPerTick;

        public CPU() {
            super(executor.build);
            instructionsPerTick = executor.instructionsPerTick;
            logicBuild = (JsLogicBuild) executor.build;
            canvas = new JsCanvas(executor);
        }

        public Scriptable links() {
            Scriptable object = executor.context.newObject(executor.scope);
            for (LogicLink link : logicBuild.links) {
                if (link.lastBuild != null) {
                    JsBuilding generic = new JsBuilding(link.lastBuild);
                    Object genericWrapped = Context.javaToJS(generic, executor.scope);
                    ScriptableObject.putProperty(object, link.name, genericWrapped);
                }
            }
            return object;
        }

        public JsBuilding link(String linkName) {
            LogicLink logicLink = logicBuild.links.find(l -> l.name == linkName);
            if (logicLink != null && logicLink.lastBuild != null) {
                return new JsBuilding(logicLink.lastBuild);
            }
            return null;
        }

        public Scriptable linkArray() {
            Object[] links = logicBuild.links
                    .select(link -> link.lastBuild != null)
                    .map(link -> Context.javaToJS(new JsBuilding(link.lastBuild), executor.scope))
                    .toArray(Object.class);
            Scriptable object = executor.context.newArray(executor.scope, links);
            return object;
        }

        public boolean linkNameIsValid(String linkName) {
            LogicLink logicLink = logicBuild.links.find(l -> l.name == linkName);
            return logicLink != null;
        }

        public String[] getLinkNames() {
            return logicBuild.links.map(link -> (String) link.name).toArray(String.class);
        }

        public JsUnit bind(String unitType) {
            LExecutor.UnitBindI unitBind = new LExecutor.UnitBindI(Vars.logicVars.get(unitType, executor.privileged));
            
            unitBind.run(executor);
            if (executor.unit != null) {
                return new JsUnit((Unit) executor.unit.objval);
            }
            return null;
        }

        public JsUnit bind(JsUnit unit) {
            LExecutor.UnitBindI unitBind = new LExecutor.UnitBindI(unit.target);
            unitBind.run(executor);
            if (executor.unit != null) {
                return new JsUnit((Unit) executor.unit.objval);
            }
            return null;
        }

        public void sleep(long milliseconds) {
            executor.sleepUntil = Time.nanos() + Time.millisToNanos(milliseconds);
            executor.sendToYield();
        }

        public void yield() {
            executor.sendToYield();
        }

        public void print(String text){
            cpu.yield();
            p1.setobj(text);
            LExecutor.PrintI print = new LExecutor.PrintI(p1);
            print.run(executor);
        }

        public String format(Object object){
            return PrintI.toString(object);
        }        
    }

    public class JsBuilding extends JsGeneric {

        public JsBuilding(Building b) {
            super(b);
        }

        public JsBuilding(JsBuilding original) {
            super(original);
        }

        public JsBuilding(LVar lvar) {
            super(lvar);
        }
    }

    public class JsGeneric {
        protected LVar ret = new LVar("ret");
        protected LVar p1 = new LVar("p1");
        protected LVar p2 = new LVar("p2");
        protected LVar p3 = new LVar("p3");
        protected LVar p4 = new LVar("p4");
        protected LVar p5 = new LVar("p5");
        protected LVar p6 = new LVar("p6");
        protected final LVar target;

        public JsGeneric(JsGeneric original) {
            target = original.target;
        }

        public JsGeneric(LVar lvar) {
            target = lvar;
        }

        public JsGeneric(String linkName) {
            JsLogicBuild logicBuild = (JsLogicBuild) executor.build;
            LogicLink logicLink = logicBuild.links.find(l -> l.name == linkName);
            target = new LVar(logicLink.name);
            target.setobj(logicLink.lastBuild);
        }

        public JsGeneric(Building b) {
            target = new LVar("JsBuilding");
            target.setobj(b);
        }

        public JsGeneric(Unit u) {
            target = new LVar("JsUnit");
            target.setobj(u);
        }

        public Object sensor(String senseableType) {
            cpu.yield();
            LExecutor.SenseI sense = new LExecutor.SenseI(target, ret, Vars.logicVars.get(senseableType, executor.privileged));
            sense.run(executor);
            return ret.num();
        }

        public void shoot(double x, double y, boolean shoot) {
            cpu.yield();
            p1.setnum(x);
            p2.setnum(y);
            p3.setbool(shoot);
            LExecutor.ControlI control = new LExecutor.ControlI(LAccess.shoot, target, p1, p2, p3, p4);
            control.run(executor);
        }

        public void shootp(JsUnit unit, boolean shoot) {
            cpu.yield();
            p1.setobj(unit.target);
            p2.setbool(shoot);
            LExecutor.ControlI control = new LExecutor.ControlI(LAccess.shootp, target, p1, p2, p3, p4);
            control.run(executor);
        }

        public void color(String color) {
            cpu.yield();
            p1.setnum(0);
            LExecutor.ControlI control = new LExecutor.ControlI(LAccess.color, target, p1, p2, p3, p4);
            control.run(executor);
        }

        public void setConfig(Object config) {
            cpu.yield();
            p1.setnum(0);
            LExecutor.ControlI control = new LExecutor.ControlI(LAccess.config, target, p1, p2, p3, p4);
            control.run(executor);
        }

        public void setEnabled(boolean value) {
            cpu.yield();
            p1.setbool(value);
            LExecutor.ControlI control = new LExecutor.ControlI(LAccess.enabled, target, p1, p2, p3, p4);
            control.run(executor);
        }

        public double read(long address) {
            cpu.yield();
            p1.setnum(address);
            LExecutor.ReadI read = new LExecutor.ReadI(target, p1, ret);
            read.run(executor);
            return ret.num();
        }

        public void write(long address, double value) {
            cpu.yield();
            p1.setnum(address);
            p2.setnum(value);
            LExecutor.WriteI write = new LExecutor.WriteI(target, p1, p2);
            write.run(executor);
        }

        public Object radar(RadarTarget targetType1, RadarTarget targetType2, RadarTarget targetType3, long order, RadarSort sort) {
            cpu.yield();
            p1.setnum(order);
            LExecutor.RadarI radar = new LExecutor.RadarI(targetType1, targetType2, targetType3, sort, target, p1, ret);
            radar.run(executor);
            if (ret.isobj) {                
                if (ret.obj() instanceof Building b) {
                    return new JsBuilding(b);
                }
                if (ret.obj() instanceof Unit u) {
                    return new JsUnit(u);
                }
            }
            return null;
        }

        public void flush(){
            cpu.yield();
            if(target.building() instanceof LogicDisplayBuild){
                LExecutor.DrawFlushI drawFlush = new LExecutor.DrawFlushI(target);
                drawFlush.run(executor);    
            }else{
                LExecutor.PrintFlushI printFlush = new LExecutor.PrintFlushI(target);
                printFlush.run(executor);    
            }
        }

        public String toString() {
            return PrintI.toString(target.objval);
        }
    }

    public class JsUnit extends JsGeneric {
        public JsUnit(LVar lvar) {
            super(lvar);
        }

        public JsUnit(Unit u) {
            super(u);
        }

        private void control(LUnitControl type) {
            cpu.yield();
            if (executor.unit.objval != target.objval)
                cpu.bind(this);
            LExecutor.UnitControlI unitControl = new LExecutor.UnitControlI(type, p1, p2, p3, p4, p5);
            unitControl.run(executor);
        }

        public void idle() {
            control(LUnitControl.idle);
        }

        public void move(double x, double y) {
            p1.setnum(x);
            p2.setnum(y);
            control(LUnitControl.move);
        }

        public void pathfind(double x, double y) {
            p1.setnum(x);
            p2.setnum(y);
            control(LUnitControl.pathfind);
        }

        public void approach(double x, double y) {
            p1.setnum(x);
            p2.setnum(y);
            control(LUnitControl.approach);
        }

        public void autoPathFind() {
            control(LUnitControl.autoPathfind);
        }

        public void stop() {
            control(LUnitControl.stop);
        }

        public boolean within(double x, double y, double radius) {
            p1.setnum(x);
            p2.setnum(y);
            p3.setnum(radius);
            control(LUnitControl.within);
            return p4.bool();
        }

        public void boost(boolean value) {
            p1.setbool(value);
            control(LUnitControl.within);
        }

        public void target(double x, double y, boolean shoot) {
            p1.setnum(x);
            p2.setnum(y);
            p3.setbool(shoot);
            control(LUnitControl.target);
        }

        public void targetp(JsUnit unit, boolean shoot) {
            p1.setobj(unit.target.objval);
            p2.setbool(shoot);
            control(LUnitControl.targetp);
        }

        public void itemTake(JsBuilding fromBuilding, Item itemType, long amount) {
            p1.setobj(fromBuilding.target.objval);
            p2.setobj(itemType);
            p3.setnum(amount);
            control(LUnitControl.itemTake);
        }

        public void itemDrop(JsBuilding toBuilding, long amount) {
            p1.setobj(toBuilding.target.objval);
            p2.setnum(amount);
            control(LUnitControl.itemDrop);
        }

        public void payloadTakeUnit() {
            p1.setbool(true);
            control(LUnitControl.payTake);
        }

        public void payloadTakeBlock() {
            p1.setbool(false);
            control(LUnitControl.payTake);
        }

        public void payloadDrop() {
            control(LUnitControl.payDrop);
        }

        public void payloadEnter() {
            control(LUnitControl.payEnter);
        }

        public void mine(double x, double y) {
            p1.setnum(x);
            p2.setnum(y);
            control(LUnitControl.mine);
        }

        public void build(double x, double y, String blockType, long rotation, String config) {
            p1.setnum(x);
            p2.setnum(y);
            p3.setobj(Vars.logicVars.get(blockType, executor.privileged));
            p4.setnum(rotation);
            p5.setobj(Vars.logicVars.get(config, executor.privileged));
            control(LUnitControl.build);
        }

        public void flag(double value) {
            p1.setnum(value);
            control(LUnitControl.flag);
        }

        public GetBlockResult getBlock(double x, double y) {
            p1.setnum(x);
            p2.setnum(y);
            control(LUnitControl.getBlock);

            JsBuilding building = null;
            if (p5.isobj && p5.objval instanceof Building b) {
                building = new JsBuilding(b);
            }
            return new GetBlockResult(p3.objval.toString(), building, p5.objval.toString());
        }

        private LocateResult locate(LLocate type) {
            return locate(type, BlockFlag.battery);
        }

        private LocateResult locate(LLocate type, BlockFlag blockFlag) {
            cpu.yield();
            if (executor.unit.objval != target.objval)
                cpu.bind(this);
            LExecutor.UnitLocateI unitLocate = new LExecutor.UnitLocateI(LLocate.building, blockFlag, p1, p1, p2, p3,
                    p4, ret);
            unitLocate.run(executor);
            if (p4.bool()) {
                if (ret.objval instanceof Building b) {
                    JsBuilding building = new JsBuilding(b);
                    return new LocateResult(p1.numval, p2.numval, building);
                } else {
                    return new LocateResult(p1.numval, p2.numval);
                }
            }
            return null;
        }

        public LocateResult locateBuilding(BlockFlag group, boolean enemy) {
            p1.setbool(enemy);
            return locate(LLocate.building, group);
        }

        public LocateResult locateOre(String oreType) {
            Vars.content.items();
            p1.setobj(Vars.logicVars.get(oreType, executor.privileged));
            return locate(LLocate.ore);
        }

        public Object locateSpawn() {
            return locate(LLocate.spawn);
        }

        public Object locateDamaged() {
            return locate(LLocate.damaged);
        }

        public void unbind() {
            if (executor.unit.objval != target.objval)
                cpu.bind(this);
            control(LUnitControl.unbind);
        }

        public class GetBlockResult {
            public String type, floor;
            public JsBuilding building;

            public GetBlockResult(String type, JsBuilding building, String floor) {
                this.building = building;
                this.type = type;
                this.floor = floor;
            }
        }

        public class LocateResult {
            public double x, y;
            public JsBuilding building;

            public LocateResult(double xval, double yval) {
                x = xval;
                y = yval;
                building = null;
            }

            public LocateResult(double xval, double yval, JsBuilding building) {
                x = xval;
                y = yval;
                this.building = building;
            }
        }
    }

    public class JsCanvas{
        protected LVar p1 = new LVar("p1");
        protected LVar p2 = new LVar("p2");
        protected LVar p3 = new LVar("p3");
        protected LVar p4 = new LVar("p4");
        protected LVar p5 = new LVar("p5");
        protected LVar p6 = new LVar("p6");

        private final LExecutor executor;

        JsCanvas(LExecutor executor){
            this.executor = executor;
        }

        public void draw(GraphicsType type){
            cpu.yield();
            DrawI draw = new DrawI((byte) type.ordinal(), p1, p2, p3, p4, p5, p6);
            draw.run(executor);
        }

        public void clear(long r, long g, long b){
            p1.setnum(r);
            p2.setnum(g);
            p3.setnum(b);
            draw(GraphicsType.clear);
        }
        
        
        public void color(long r, long g, long b, long a){
            p1.setnum(r);
            p2.setnum(g);
            p3.setnum(b);
            p4.setnum(a);
            draw(GraphicsType.color);
        }
        
        
        /*public void col(String color){
            p1.setnum(r.longValue());
            p2.setnum(g.longValue());
            p3.setnum(b.longValue());
            p4.setnum(a.longValue());
            draw(GraphicsType.col);
        }*/
        
        
        public void stroke(long width){
            p1.setnum(width);
            draw(GraphicsType.stroke);
        }
        
        
        public void line(long x, long y, long x2, long y2){
            p1.setnum(x);
            p2.setnum(y);
            p3.setnum(x2);
            p4.setnum(y2);
            draw(GraphicsType.line);
        }
        
        
        public void rect(long x, long y, long x2, long y2){
            p1.setnum(x);
            p2.setnum(y);
            p3.setnum(x2);
            p4.setnum(y2);
            draw(GraphicsType.rect);
        }
        
        
        public void lineRect(long x, long y, long x2, long y2){
            p1.setnum(x);
            p2.setnum(y);
            p3.setnum(x2);
            p4.setnum(y2);
            draw(GraphicsType.lineRect);
        }
        
        
        public void poly(long x, long y, boolean sides, long radius, long rotation){
            p1.setnum(x);
            p2.setnum(y);
            p3.setbool(sides);
            p4.setnum(radius);
            p5.setnum(rotation);
            draw(GraphicsType.poly);        
        }
        
        
        public void linePoly(long x, long y, boolean sides, long radius, long rotation){
            p1.setnum(x);
            p2.setnum(y);
            p3.setbool(sides);
            p4.setnum(radius);
            p5.setnum(rotation);
            draw(GraphicsType.linePoly);        
        }
        
        
        public void triangle(long x, long y, long x2, long y2, long x3, long y3){
            p1.setnum(x);
            p2.setnum(y);
            p3.setnum(x2);
            p4.setnum(y2);
            p5.setnum(x3);
            p6.setnum(y3);
            draw(GraphicsType.triangle);                
        }
        
        
        public void image(long x, long y, String image, long size, long rotation){
            p1.setnum(x);
            p2.setnum(y);            
            p3.setobj(Vars.logicVars.get(image, executor.privileged).objval);
            p4.setnum(size);
            p5.setnum(rotation);
            draw(GraphicsType.image);                
        }
        
        
        public void print(String text, long x, long y, String align){
            executor.textBuffer.setLength(0);
            executor.textBuffer.append(text);
            p1.setnum(x);
            p2.setnum(y);  
            p3.id = 0;          
            //p3.id = DrawStatement.nameToAlign.get(align, Align.bottomLeft);
            draw(GraphicsType.print);                  
        }
        
        
        public void translate(long x, long y){
            p1.setnum(x);
            p2.setnum(y);            
            draw(GraphicsType.print);           
        }
        
        
        public void scale(long x, long y){
            p1.setnum(x);
            p2.setnum(y);            
            draw(GraphicsType.scale);    
        }
    
    
        public void rotate(long x, long y){
            p1.setnum(x);
            p2.setnum(y);            
            draw(GraphicsType.rotate);           
        }
    }
}