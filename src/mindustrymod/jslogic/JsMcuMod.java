package mindustrymod.jslogic;

import arc.util.*;
import mindustry.mod.*;
import mindustry.content.*;
import mindustry.content.TechTree.TechNode;
import mindustry.world.*;
import mindustry.type.*;
import static mindustry.type.ItemStack.*;

public class JsMcuMod extends Mod{
    public static Block jsMcu;

    public JsMcuMod(){
        Log.info("Loaded ExampleJavaMod constructor.");

        //listen for game load event
        /*Events.on(ClientLoadEvent.class, e -> {
            //show dialog upon startup
            Time.runTask(10f, () -> {
                BaseDialog dialog = new BaseDialog("frog");
                dialog.cont.add("behold").row();
                //mod sprites are prefixed with the mod name (this mod is called 'example-java-mod' in its config)
                dialog.cont.image(Core.atlas.find("example-java-mod-frog")).pad(20f).row();
                dialog.cont.button("I see", dialog::hide).size(100f, 50f);
                dialog.show();
            });
        });*/
    }

    @Override
    public void loadContent(){
        //Log.info("Loading some example content.");
        jsMcu = new JsLogicBlock("micro-processor-js"){{
            requirements(Category.logic, with(Items.copper, 90, Items.lead, 50, Items.silicon, 50));

            instructionsPerTick = 2;
            size = 1;
        }};

        //Log.info("TechTree.size: " + TechTree.all.size);
        TechTree.all.each(n -> {
            if(n.content.name == "micro-processor") {
                Log.info("adding micro-processor-js to micro-processor");
                TechTree.all.add(new TechNode(n, jsMcu, jsMcu.researchRequirements()));
            }else{
                //Log.info("TechTree: " + n.content.name);
            }
        });
    }

}
