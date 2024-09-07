package mindustrymod.jslogic;

import arc.func.*;
import arc.graphics.*;
import arc.scene.actions.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.core.GameState.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.LExecutor.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import static mindustry.Vars.*;

public class JsDialog extends BaseDialog {
    @Nullable
    private JsExecutor executor;
    Cons<String> consumer = s -> {
    };
    private TextArea codeEditor = new TextArea("");
    private TextArea consoleOutput = new TextArea("console.log():");
    private String logText = "";
    private Table controlButtons = new Table();
    private boolean isRunning = false;

    public JsDialog() {
        super("JavaScript Logic Editor");

        clearChildren();

        isRunning = false;
        shouldPause = true;

        addCloseListener();

        // Setup listeners
        shown(this::setup);
        hidden(() -> {
            consumer.get(codeEditor.getText());
            if(executor != null){
                executor.setConsoleListener(null);
            }
        });
        onResize(this::setup);
    }

    private void setup() {
        // Code editor setup
        codeEditor.setPrefRows(15);
        codeEditor.getStyle().font = Fonts.logic;
        codeEditor.setStyle(codeEditor.getStyle());
        codeEditor.setMessageText("//Enter your JavaScript code here...");

        // Console output setup
        consoleOutput.setPrefRows(5);
        consoleOutput.update(() -> {
            consoleOutput.setText(logText);
            consoleOutput.setCursorPosition(logText.length());
        });

        // Control buttons setup
        controlButtons.defaults().size(120f, 50f);
        setupControlButtons();

        // Layout setup
        clearChildren();
        add(codeEditor).grow().pad(10f);
        pane(p -> {
            p.margin(10f).marginRight(16f);
            p.table(Tex.button, t -> {
                t.defaults().fillX().height(45f);
                if(executor != null){
                    for (var varName : executor.getAllVariableNames()) {
                        Color varColor = Pal.gray;
                        float stub = 8f, mul = 0.5f, pad = 4;

                        t.add(new Image(Tex.whiteui, varColor.cpy().mul(mul))).width(stub);
                        t.stack(new Image(Tex.whiteui, varColor), new Label(" " + varName + " ", Styles.outlineLabel) {
                            {
                                setColor(Pal.accent);
                            }
                        }).padRight(pad);

                        t.add(new Image(Tex.whiteui, Pal.gray.cpy().mul(mul))).width(stub);
                        t.table(Tex.pane, out -> {
                            float period = 15f;
                            float[] counter = { -1f };
                            Label label = out.add("").style(Styles.outlineLabel).padLeft(4).padRight(4).width(140f)
                                    .wrap().get();
                            label.update(() -> {
                                if (counter[0] < 0 || (counter[0] += Time.delta) >= period) {
                                    Object varValue = executor.getVariableValue(varName);
                                    String text = getDisplayableValue(varValue);
                                    if (!label.textEquals(text)) {
                                        label.setText(text);
                                        if (counter[0] >= 0f) {
                                            label.actions(Actions.color(Pal.accent), Actions.color(Color.white, 0.2f));
                                        }
                                    }
                                    counter[0] = 0f;
                                }
                            });
                            label.act(1f);
                        }).padRight(pad);

                        t.add(new Image(Tex.whiteui,
                                typeColor(executor.getVariableValue(varName), new Color()).mul(mul)))
                                .update(i -> i
                                        .setColor(typeColor(executor.getVariableValue(varName), i.color).mul(mul)))
                                .width(stub);

                        t.stack(new Image(Tex.whiteui, typeColor(executor.getVariableValue(varName), new Color())) {
                            {
                                update(() -> setColor(typeColor(executor.getVariableValue(varName), color)));
                            }
                        }, new Label(() -> " " + typeName(executor.getVariableValue(varName)) + " ") {
                            {
                                setStyle(Styles.outlineLabel);
                            }
                        });

                        t.row();

                        t.add().growX().colspan(6).height(4).row();
                    }
                }
            });
        });
        row();
        add(controlButtons).growX().pad(5f);
        row();
        add(consoleOutput).growX().pad(5f);
    }

    public static Color typeColor(Object s, Color color) {
        return color.set(
                s instanceof Number ? Pal.place
                        : s == null ? Color.darkGray
                                : s instanceof String ? Pal.ammo
                                        : s instanceof JsWrapper.JsBuilding ? Pal.logicBlocks
                                                : s instanceof JsWrapper.JsUnit ? Pal.logicUnits
                                                        : Color.white);
    }

    public static String typeName(Object s) {
        return s instanceof Number ? "number"
                : s == null ? "null"
                        : s instanceof String ? "string"
                                : s instanceof JsWrapper.JsBuilding ? "building"
                                        : s instanceof JsWrapper.JsUnit ? "unit" : "unknown";
    }

    public static String getDisplayableValue(Object varValue) {
        if (varValue == null)
            return "null";
        if (varValue instanceof Number n)
            return Math.abs(n.doubleValue() - n.longValue()) < 0.00001 ? n.longValue() + "" : n.doubleValue() + "";
        if (varValue instanceof JsWrapper.JsGeneric g)
            return g.toString();
        return PrintI.toString(varValue);
    }

    private void setupControlButtons() {
        controlButtons.clearChildren();

        controlButtons.button("@back", Icon.left, this::hide).name("back");
        controlButtons.button("@step", Icon.play, () -> {
            executor.runOnce();
            //consoleOutput.setText("current line: " + executor.getCurrentLineNumber());
        }).disabled(t -> net.active() || isRunning || executor == null);

        if (isRunning) {
            controlButtons.button("@pause", Icon.pause, () -> {
                isRunning = false;
                state.set(State.paused);
                setupControlButtons();
            }).disabled(t -> net.active());
        } else {
            controlButtons.button("@play", Icon.play, () -> {
                state.set(State.playing);
                isRunning = true;
                setupControlButtons();
            }).disabled(t -> net.active());
        }

        controlButtons.button("@reset", Icon.cancel, () -> {
            state.set(State.paused);
            executor.load(codeEditor.getText());
            isRunning = false;
            setupControlButtons();
        }).disabled(t -> net.active());
    }

    public void show(String code, JsExecutor executor, boolean privileged, Cons<String> modified) {
        this.executor = executor;
        codeEditor.setText(executor.code);
        executor.setConsoleListener(log -> {
            logText = log;
        });

        if(executor.console != null){
            logText = executor.console.getLogContent();            
        }else{
            logText = "";
        }
        consoleOutput.setText(logText);

        this.consumer = result -> {
            if (!result.equals(code)) {
                modified.get(result);
            }
        };

        show();
    }
}
