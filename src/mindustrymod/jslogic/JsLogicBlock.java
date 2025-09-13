package mindustrymod.jslogic;

import arc.func.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.logic.LAssembler;
import mindustry.world.blocks.distribution.Router;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.power.PowerNode;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class JsLogicBlock extends LogicBlock {
    public static JsDialog jsDialog;

    public int maxInstructionScale = 5;
    public int instructionsPerTick = 1;
    // privileged only
    public int maxInstructionsPerTick = 100;
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

        /*config(byte[].class, (JsLogicBuild build, byte[] data) -> {
            if(!accessible()) return;

            build.readCompressed(data, true);
        });

        config(String.class, (JsLogicBuild build, String data) -> {
            if(!accessible() || !privileged) return;

            if(data != null && data.length() < maxNameLength){
                build.tag = data;
            }
        });

        config(Character.class, (JsLogicBuild build, Character data) -> {
            if(!accessible() || !privileged) return;
            PowerGraph
            build.iconTag = data;
        });

        config(Integer.class, (JsLogicBuild entity, Integer pos) -> {
            if(!accessible()) return;

            //if there is no valid link in the first place, nobody cares
            if(!entity.validLink(world.build(pos))) return;
            var lbuild = world.build(pos);
            int x = lbuild.tileX(), y = lbuild.tileY();

            LogicLink link = entity.links.find(l -> l.x == x && l.y == y);
            String bname = getLinkName(lbuild.block);

            if(link != null){
                link.active = !link.active;
                //find a name when the base name differs (new block type)
                if(!link.name.startsWith(bname)){
                    link.name = "";
                    link.name = entity.findLinkName(lbuild.block);
                }
                //disable when unlinking
                if(!link.active && lbuild.block.autoResetEnabled && lbuild.lastDisabler == entity){
                    lbuild.enabled = true;
                }
            }else{
                entity.links.remove(l -> world.build(l.x, l.y) == lbuild);
                entity.links.add(new LogicLink(x, y, entity.findLinkName(lbuild.block), true));
            }

            entity.updateCode(entity.code, true, null);
        });        */
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
        public void updateTile(){
            int accumulator;
            super.updateTile();

            if(state.rules.disableWorldProcessors && privileged) return;

            if(privileged){
                if(ipt == 0 || ipt > maxInstructionsPerTick){
                    ipt = maxInstructionsPerTick;
                }
                accumulator = (int) edelta() * ipt;
                jsExecutor.runTimes((int) accumulator);
            }else{
                accumulator = (int) edelta() * ipt;
                if(accumulator > maxInstructionScale * ipt) accumulator = maxInstructionScale * ipt;
            }
            jsExecutor.runTimes(accumulator);
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
