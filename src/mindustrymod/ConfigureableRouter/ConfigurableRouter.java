package mindustrymod.ConfigureableRouter;

import java.util.BitSet;

import arc.func.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.ui.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.content.*;
import mindustry.Vars.*;
import mindustry.world.blocks.ControlBlock;
import mindustry.world.blocks.distribution.Router;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

public class ConfigurableRouter extends Router{
    public static ConfigurableRouterDialog dialog;

    public ConfigurableRouter(String name){
        super(name);
        //configurable = true;
        //saveConfig = true;

        /*config(Item.class, (ConfigurableRouterBuild tile, Item item) -> tile.sortItem = item);
        configClear((ConfigurableRouterBuild tile) -> tile.sortItem = null);*/
    }

    public class ConfigurableRouterBuild extends Router.RouterBuild{
        public Item sortItem;
        //public BitSet[] acceptBitSet = {new BitSet(64), new BitSet(64), new BitSet(64), new BitSet(64)};
        //public BitSet[] forwardBitSet = {new BitSet(64), new BitSet(64), new BitSet(64), new BitSet(64)};
        //public Building buildLeft, buildTop, buildRight, buildBottom;

        ConfigurableRouterBuild(){      

        }

        /*@Override
        public Unit unit(){
            return null;
        }*/
    

        @Override
        public boolean canControl(){
            return false;
        }    

        /*@Override
        public boolean shouldShowConfigure(Player player){
            return true;
        }*/

        /*@Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
            if(proximity.size > 4){
                Log.info("[ConfigRouter]: More than 4 buildings in proximity is unsupported.");
                return;
            }else{
                for(var build : proximity){
                    int diffX = build.tileX() - tileX();
                    int diffY = build.tileY() - tileY();
                    boolean moreX = Math.abs(diffX) > Math.abs(diffY);

                    if(diffX < 0 && moreX){
                        buildLeft = build;
                    }else if(diffX > 0 && moreX){
                        buildRight = build;                        
                    }else{
                        if(diffY < 0){
                            buildBottom = build;
                        }else if(diffY > 0){
                            buildTop = build;
                        }else{
                            // overlapping is not proximity!
                        }
                    }
                }
            }
        }*/

        /*@Override
        public void buildConfiguration(Table table){
            table.button(Icon.pencil, Styles.cleari, this::showEditDialog).size(40);
        }

        public void showEditDialog(){
            if(dialog == null){
                dialog = new ConfigurableRouterDialog();
            }
            dialog.show(this);
        }  */
        
        /*@Override
        public Item config(){
            return null;
        }*/

        @Override
        public byte version(){
            return 1;
        }

        /*@Override
        public void write(Writes write){
            super.write(write);

            for(int i = 0; i < 4; i++){
                byte[] byteArray = acceptBitSet[i].toByteArray();
                write.i(byteArray.length);
                write.b(byteArray);

                byteArray = forwardBitSet[i].toByteArray();
                write.i(byteArray.length);
                write.b(byteArray);
            }
        }*/

        /*@Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            /*for(int i = 0; i < 4; i++){
                byte[] byteArray = new byte[read.i()];
                read.b(byteArray);            
                acceptBitSet[i] = BitSet.valueOf(byteArray);

                byteArray = new byte[read.i()];
                read.b(byteArray);            
                forwardBitSet[i] = BitSet.valueOf(byteArray);
            }
        }*/
    }    
}
