package uk.co.atty29.jsgzpm.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import uk.co.atty29.jsgzpm.blockentity.AtlantisPegasusDHDBlockEntity;
import uk.co.atty29.jsgzpm.registry.ModRegistries;

public final class AtlantisDHDMenu extends AbstractContainerMenu {
    public final AtlantisPegasusDHDBlockEntity dhd;
    private final ContainerData data;
    public boolean overlayOpen;
    public AtlantisDHDMenu(int id,Inventory player,FriendlyByteBuf buffer) {
        this(id,player,(AtlantisPegasusDHDBlockEntity)player.player.level().getBlockEntity(buffer.readBlockPos()));
    }
    public AtlantisDHDMenu(int id,Inventory player,AtlantisPegasusDHDBlockEntity dhd) {
        super(ModRegistries.ATLANTIS_DHD_MENU.get(),id);this.dhd=dhd;
        if(dhd==null)throw new IllegalArgumentException("Missing Atlantis DHD");
        addSlot(new SlotItemHandler(dhd.inventory,0,81,40));
        for(int i=0;i<3;i++)addSlot(new SlotItemHandler(dhd.inventory,i+1,9+18*i,40));
        addSlot(new SlotItemHandler(dhd.inventory,4,116,23));
        addSlot(new SlotItemHandler(dhd.inventory,5,-22,26) {
            @Override public boolean isActive(){return overlayOpen;}
            @Override public boolean mayPlace(ItemStack stack){return overlayOpen&&super.mayPlace(stack);}
            @Override public boolean mayPickup(Player player){return overlayOpen&&super.mayPickup(player);}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(player,col+row*9+9,8+col*18,91+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(player,col,8+col*18,149));
        data=player.player.level().isClientSide?new SimpleContainerData(6):new ContainerData(){
            public int get(int i){return switch(i){case 0->dhd.inventory.tank.getFluidAmount()&65535;case 1->dhd.inventory.tank.getFluidAmount()>>>16;case 2->dhd.inventory.tank.getCapacity()&65535;case 3->dhd.inventory.tank.getCapacity()>>>16;case 4->dhd.inventory.reactor;default->dhd.hasLinkedGate()?1:0;};}
            public void set(int i,int value){} public int getCount(){return 6;}
        };
        addDataSlots(data);
    }
    public int fuel(){return (data.get(0)&65535)|((data.get(1)&65535)<<16);}
    public int capacity(){return (data.get(2)&65535)|((data.get(3)&65535)<<16);}
    public int status(){return data.get(4);}
    public boolean linked(){return data.get(5)!=0;}
    @Override public boolean stillValid(Player player){return !dhd.isRemoved()&&player.level()==dhd.getLevel()&&player.distanceToSqr(dhd.getBlockPos().getX()+.5,dhd.getBlockPos().getY()+.5,dhd.getBlockPos().getZ()+.5)<=64;}
    @Override public boolean clickMenuButton(Player player,int button){
        if(!stillValid(player))return false;
        if(button==0){overlayOpen=!overlayOpen;return true;}
        if(button==1){dhd.relink();return true;}
        return false;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index<6){if(!moveItemStackTo(stack,6,slots.size(),true))return ItemStack.EMPTY;}
        else {
            boolean moved=false;
            for(int i=0;i<6;i++)if(slots.get(i).isActive()&&slots.get(i).mayPlace(stack)&&moveItemStackTo(stack,i,i+1,false)){moved=true;break;}
            if(!moved)return ItemStack.EMPTY;
        }
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(player,stack);return copy;
    }
}
