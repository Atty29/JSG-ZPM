package uk.co.atty29.jsgzpm.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import uk.co.atty29.jsgzpm.menu.AtlantisDHDMenu;

/** Uses the installed public JSG DHD's own background without bundling its artwork. */
public final class AtlantisDHDScreen extends AbstractContainerScreen<AtlantisDHDMenu> {
    private static final ResourceLocation BACKGROUND=new ResourceLocation("jsg","textures/gui/container_dhd.png");
    public AtlantisDHDScreen(AtlantisDHDMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=173;inventoryLabelY=79;}
    @Override protected void init(){
        super.init();
        addRenderableWidget(Button.builder(Component.literal("O"),b->{menu.overlayOpen=!menu.overlayOpen;minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);}).bounds(leftPos-24,topPos+4,22,18).build());
        addRenderableWidget(Button.builder(Component.literal("Relink"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1)).bounds(leftPos+103,topPos+3,43,16).build());
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        if(minecraft.getResourceManager().getResource(BACKGROUND).isPresent())g.blit(BACKGROUND,leftPos,topPos,0,0,imageWidth,imageHeight);
        else g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xffb9b9b9);
        if(menu.overlayOpen){g.fill(leftPos-26,topPos+22,leftPos-2,topPos+46,0xff888888);g.fill(leftPos-23,topPos+25,leftPos-5,topPos+43,0xff333333);}
        int amount=(int)(50L*Math.max(0,menu.fuel())/Math.max(1,menu.capacity()));
        g.fill(leftPos+152,topPos+24,leftPos+167,topPos+76,0xff202522);
        g.fill(leftPos+153,topPos+75-amount,leftPos+166,topPos+75,0xff62be65);
        g.fill(leftPos+8,topPos+18,leftPos+14,topPos+24,menu.dhd.inventory.control()?0xff55dd66:0xff993333);
        g.fill(leftPos+17,topPos+18,leftPos+23,topPos+24,menu.linked()?0xff55dd66:0xff993333);
        g.fill(leftPos+26,topPos+18,leftPos+32,topPos+24,menu.status()==4?0xff55dd66:menu.status()==2?0xffffbb44:0xff993333);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,Component.literal("Atlantis DHD"),8,6,0x404040,false);
        g.drawString(font,Component.translatable("gui.upgrades"),8,29,0x404040,false);
        g.drawString(font,playerInventoryTitle,8,79,0x404040,false);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);super.render(g,mx,my,partial);renderTooltip(g,mx,my);
        int x=mx-leftPos,y=my-topPos;
        if(x>=152&&x<=168&&y>=23&&y<=77)g.renderTooltip(font,Component.literal(menu.fuel()+" / "+menu.capacity()+" mB"),mx,my);
        if(x>=8&&x<=32&&y>=18&&y<=25){
            String text=x<15?(menu.dhd.inventory.control()?"Pegasus control crystal installed":"Pegasus control crystal required"):x<24?(menu.linked()?"Linked":"Not linked"):switch(menu.status()){case 0->"No control crystal";case 1->"Not linked";case 2->"Reactor standby";case 3->"No refined naquadah fuel";default->"Reactor running";};
            g.renderTooltip(font,Component.literal(text),mx,my);
        }
        if(hoveredSlot!=null&&!hoveredSlot.hasItem()&&hoveredSlot.index<6){
            String hint=switch(hoveredSlot.index){case 0->"Pegasus DHD control crystal";case 1,2,3->"Glyph, capacity or efficiency crystal (one each)";case 4->"Refined molten naquadah bucket";default->"JSG biome override item";};
            g.renderTooltip(font,Component.literal(hint),mx,my);
        }
    }
}
