package mindustrymod.ConfigureableRouter;

import arc.Core;
import arc.func.*;
import arc.graphics.g2d.TextureRegion;
import arc.math.Interp;
import arc.scene.Element;
import arc.scene.actions.Actions;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.Vars.*;
import mindustry.content.Blocks;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.logic.LAccess;
import mindustry.world.Tile;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.storage.Unloader;
import mindustry.world.consumers.*;
import mindustry.world.modules.ItemModule.ItemConsumer;
import mindustrymod.ConfigureableRouter.ConfigurableRouter.ConfigurableRouterBuild;

public class ConfigurableRouterDialog extends BaseDialog {
    Cons<String> consumer = s -> {};
    @Nullable ConfigurableRouterBuild router;

    public ConfigurableRouterDialog() {
        super("Router configuration");    
        shouldPause = true;
        clearChildren();

        addCloseListener();

        shown(this::setup);
        /*hidden(() -> consumer.get("bla"));*/
        onResize(() -> {
            setup();
        });

        add(cont).grow().name("canvas");

        row();

        add(buttons).growX().name("canvas");
    }

    public void setup(){

    }
    
    public void rebuild(){
        /*cont.clearChildren();
        buttons.clearChildren();
        buttons.defaults().size(160f, 64f);
        buttons.button("@back", Icon.left, this::hide).name("back");


        if(router.buildTop != null){

            cont.label(() -> "");
            cont.label(() -> "");
            
            var build = router.buildTop;
            var image = cont.image(build.tile.floor().getDisplayIcon(build.tile));
            image.colspan(2);
            
            cont.row();
            
            cont.label(() -> "");
            cont.label(() -> "");
            cont.label(() -> "In"); 
            showItemSelection(cont, build, true);
            cont.label(() -> "Out"); 
            showItemSelection(cont, build, false);
            
            cont.row();
            cont.label(() -> "");
            cont.label(() -> "");
            var proxId = router.proximity.indexOf(build);
            cont.table(t -> {
                var bits = router.acceptBitSet[proxId];

                for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i+1)) {
                    var item = Vars.content.items().get(i);
                    var reqImg = new ReqImage(new ItemImage(item.uiIcon, 1), () -> false);
                    t.add(reqImg);
                }
            });
            cont.table(t -> {
                var bits = router.forwardBitSet[proxId];

                for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i+1)) {
                    var item = Vars.content.items().get(i);
                    var reqImg = new ReqImage(new ItemImage(item.uiIcon, 1), () -> false);
                    t.add(reqImg);
                }
            });
            cont.row();
        }
        var spacer = cont.label(() -> "");
        spacer.height(32);
        cont.row();

        cont.label(() -> "");
        cont.label(() -> "");

        var rimage = cont.image(router.tile.floor().getDisplayIcon(router.tile));
        rimage.colspan(2);*/

    }

    protected int selected = 0;
    // mostly duplicate code from LStatement
    public void showItemSelection(Table table, Building build, boolean showConsume){

        table.button(b -> {
            b.image(Icon.pencilSmall);
            //240
            b.clicked(() -> showSelectTable(b, (t, hide) -> {
                Table[] tables = {
                    // only building
                    new Table(i -> {
                        i.left();
                        int c = 0;
                        for(Item item : Vars.content.items()){
                            if(!item.unlockedNow() || item.hidden) continue;
                            boolean showItem = false;
                            if(showConsume){
                                for(var consumer : build.block.consumers){
                                    if(consumer instanceof ConsumeItems ci){
                                        for(var itemStack : ci.items){
                                            if(itemStack.item == item)
                                            showItem = true;
                                        }
                                    }
                                }
                            }else{
                                if(build.block instanceof GenericCrafter g){
                                    if(g.outputItems != null){
                                        for(ItemStack is : g.outputItems){
                                            if(is.item == item){
                                                showItem = true;
                                            }
                                        }
                                    }else{
                                        if(g.outputItem.item == item){
                                            showItem = true;
                                        }
                                    }                                
                                }
                            }
                            if(!showItem) continue;
                            i.button(new TextureRegionDrawable(item.uiIcon), Styles.flati, Vars.iconSmall, () -> {
                                stype(item, build);
                                hide.run();
                            }).size(40f);

                            if(++c % 6 == 0) i.row();
                        }
                    }),
                    // all items
                    new Table(i -> {
                        i.left();
                        int c = 0;
                        for(Item item : Vars.content.items()){
                            if(!item.unlockedNow() || item.hidden) continue;
                            i.button(new TextureRegionDrawable(item.uiIcon), Styles.flati, Vars.iconSmall, () -> {
                                stype(item, build);
                                hide.run();
                            }).size(40f);

                            if(++c % 6 == 0) i.row();
                        }
                    }),
                };

                Drawable[] icons = {Icon.production, Icon.box};
                Stack stack = new Stack(tables[selected]);
                ButtonGroup<Button> group = new ButtonGroup<>();

                for(int i = 0; i < tables.length; i++){
                    int fi = i;

                    t.button(icons[i], Styles.squareTogglei, () -> {
                        selected = fi;

                        stack.clearChildren();
                        stack.addChild(tables[selected]);

                        t.parent.parent.pack();
                        t.parent.parent.invalidateHierarchy();
                    }).height(50f).growX().checked(selected == fi).group(group);
                }
                t.row();
                t.add(stack).colspan(3).width(240f).left();
            }));
        }, Styles.logict, () -> {}).size(40f).padLeft(-1).color(table.color);        
    }

    public void stype(Item item, Building build){

    }

    // duplicate code from LStatement
    protected void showSelectTable(Button b, Cons2<Table, Runnable> hideCons){
        Table t = new Table(Tex.paneSolid){
            @Override
            public float getPrefHeight(){
                return Math.min(super.getPrefHeight(), Core.graphics.getHeight());
            }

            @Override
            public float getPrefWidth(){
                return Math.min(super.getPrefWidth(), Core.graphics.getWidth());
            }
        };
        t.margin(4);

        //triggers events behind the element to simulate deselection
        Element hitter = new Element();

        Runnable hide = () -> {
            Core.app.post(hitter::remove);
            t.actions(Actions.fadeOut(0.3f, Interp.fade), Actions.remove());
        };

        hitter.fillParent = true;
        hitter.tapped(hide);

        Core.scene.add(hitter);
        Core.scene.add(t);

        t.update(() -> {
            if(b.parent == null || !b.isDescendantOf(Core.scene.root)){
                Core.app.post(() -> {
                    hitter.remove();
                    t.remove();
                });
                return;
            }

            b.localToStageCoordinates(Tmp.v1.set(b.getWidth()/2f, b.getHeight()/2f));
            t.setPosition(Tmp.v1.x, Tmp.v1.y, Align.center);
            if(t.getWidth() > Core.scene.getWidth()) t.setWidth(Core.graphics.getWidth());
            if(t.getHeight() > Core.scene.getHeight()) t.setHeight(Core.graphics.getHeight());
            t.keepInStage();
            t.invalidateHierarchy();
            t.pack();
        });
        t.actions(Actions.alpha(0), Actions.fadeIn(0.3f, Interp.fade));

        t.top().pane(inner -> {
            inner.top();
            hideCons.get(inner, hide);
        }).pad(0f).top().scrollX(false);

        t.pack();
    }    
    
    public void show(ConfigurableRouterBuild router){
        this.router = router;
        rebuild();
        /*this.consumer = result -> {
            if(!result.equals(code)){
                modified.get(result);
            }
        };*/

        show();
    }
}
