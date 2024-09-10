package mindustrymod.jslogic;

import arc.func.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.logic.LAssembler;
import mindustry.world.blocks.logic.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class JsLogicBlock extends LogicBlock {
    public static JsDialog jsDialog;

    public int maxInstructionScale = 5;
    public int instructionsPerTick = 1;
    // privileged only
    public int maxInstructionsPerTick = 40;
    public float range = 8 * 10;

    public JsLogicBlock(String name) {
        super(name);
        update = true;
        solid = true;
        configurable = true;
        group = BlockGroup.logic;
        schematicPriority = 5;
        ignoreResizeConfig = true;
        envEnabled = Env.any;
     }

    public class JsLogicBuild extends LogicBlock.LogicBuild{
        /** logic "source code" as list of asm statements */
        public JsExecutor jsExecutor;

        public @Nullable String tag;
        public char iconTag;

        /** Block of code to run after load. */
        public @Nullable Runnable loadBlock;

        {
            jsExecutor = new JsExecutor();
            executor = jsExecutor;
            jsExecutor.privileged = privileged;
            jsExecutor.instructionsPerTick = instructionsPerTick;
            jsExecutor.build = this;
        }
        
        @Override
        public void updateCode(String str, boolean keep, Cons<LAssembler> assemble) {
            updateCode(str);
        }

        @Override
        public void updateCode(String str) {
            if (str != null) {
                code = str;

                try {
                    // store link objects
                    jsExecutor.links = new Building[links.count(l -> l.valid && l.active)];
                    jsExecutor.linkIds.clear();

                    int index = 0;
                    for (LogicLink link : links) {
                        if (link.active && link.valid) {
                            Building build = world.build(link.x, link.y);
                            jsExecutor.links[index++] = build;
                            if (build != null){
                                jsExecutor.linkIds.add(build.id);
                            }
                        }
                    }
                    jsExecutor.load(code);
                } catch (Exception e) {
                    // handle malformed code and replace it with nothing
                    jsExecutor.load("");
                }
            }
        }

        @Override
        public BlockStatus status(){
            if(!enabled){
                return BlockStatus.logicDisable;
            }
            JsExecutor jse = jsExecutor;
            if(jse.hasErrors){
                return BlockStatus.noInput;
            }
            if(!jse.isRunning){
                return BlockStatus.noOutput;
            }
            return BlockStatus.active;
        }

        @Override
        public void showEditDialog(boolean forceEditor) {
            Log.info("showEditDialog for js");
            if(jsDialog == null){
                jsDialog = new JsDialog();
            }
            jsDialog.show(code, jsExecutor, privileged, code -> {
                boolean prev = state.rules.editor;
                // this is a hack to allow configuration to work correctly in the editor for
                // privileged processors
                if (forceEditor){
                    state.rules.editor = true;
                }
                configure(compress(code, relativeConnections()));
                state.rules.editor = prev;
            });
        }
    }
}
