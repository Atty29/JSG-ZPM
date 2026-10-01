package uk.co.atty29.jsgzpm.blockentity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import uk.co.atty29.jsgzpm.compat.DHDUpgradeCompat;

public final class DHDInventory extends ItemStackHandler {
    private final AtlantisPegasusDHDBlockEntity owner;
    public int reactor; // 0=no crystal, 1=not linked, 2=standby, 3=no fuel, 4=running
    private boolean filling;
    public final FluidTank tank = new FluidTank(16000) {
        @Override public boolean isFluidValid(FluidStack fluid) {
            ResourceLocation id=ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
            return id!=null && (id.getNamespace().equals("jsg")||id.getNamespace().equals("jsg_core")) && id.getPath().equals("molten_naquadah_refined");
        }
        @Override protected void onContentsChanged() { if(owner!=null)owner.setChanged(); }
    };
    public DHDInventory(AtlantisPegasusDHDBlockEntity owner) { super(6); this.owner=owner; }
    public boolean control() { return DHDUpgradeCompat.item(getStackInSlot(0),"crystal_control_pegasus_dhd"); }
    public boolean has(String name) {
        for(int i=1;i<=3;i++)if(DHDUpgradeCompat.item(getStackInSlot(i),name))return true;
        return false;
    }
    @Override public int getSlotLimit(int slot) { return 1; }
    @Override public boolean isItemValid(int slot,ItemStack stack) {
        if(slot==0)return DHDUpgradeCompat.item(stack,"crystal_control_pegasus_dhd");
        if(slot>=1&&slot<=3) {
            if(!DHDUpgradeCompat.upgrade(stack))return false;
            for(int i=1;i<=3;i++)if(i!=slot&&ItemStack.isSameItem(getStackInSlot(i),stack))return false;
            return true;
        }
        if(slot==4)return stack.getItem() instanceof BucketItem b && tank.isFluidValid(new FluidStack(b.getFluid(),1000));
        if(slot==5)return DHDUpgradeCompat.overlay(stack)!=null;
        return false;
    }
    @Override protected void onContentsChanged(int slot) { if(owner!=null)owner.inventoryChanged(); }
    public void tick() {
        int cap=(int)Math.min(Integer.MAX_VALUE,DHDUpgradeCompat.setting("fluidCapacity",16000)*(has("crystal_upgrade_capacity")?DHDUpgradeCompat.setting("capacityUpgradeMultiplier",2):1));
        tank.setCapacity(Math.max(1,cap));
        if(tank.getFluidAmount()>cap)tank.drain(tank.getFluidAmount()-cap,IFluidHandler.FluidAction.EXECUTE);
        ItemStack bucket=getStackInSlot(4);
        if(!filling && bucket.getItem() instanceof BucketItem b && !bucket.isEmpty()) {
            FluidStack fluid=new FluidStack(b.getFluid(),1000);
            if(tank.fill(fluid,IFluidHandler.FluidAction.SIMULATE)==1000) {
                filling=true;setStackInSlot(4,new ItemStack(Items.BUCKET));tank.fill(fluid,IFluidHandler.FluidAction.EXECUTE);filling=false;
            }
        }
        int previous=reactor;
        var gate=owner.getServerGate();
        if(!control())reactor=0;
        else if(gate==null)reactor=1;
        else {
            var energy=gate.getCapability(ForgeCapabilities.ENERGY).orElse(null);
            if(energy==null || energy.getMaxEnergyStored()<=0)reactor=2;
            else {
                double fraction=(double)energy.getEnergyStored()/energy.getMaxEnergyStored();
                if(fraction>=DHDUpgradeCompat.setting("deactivationLevel",.98))reactor=2;
                else if(fraction<DHDUpgradeCompat.setting("activationLevel",.9)||reactor==3||reactor==4)reactor=tank.isEmpty()?3:4;
                else reactor=2;
                if(reactor==4) {
                    int fe=(int)Math.min(Integer.MAX_VALUE,DHDUpgradeCompat.setting("energyPerNaquadah",8388607)*(has("crystal_upgrade_efficiency")?DHDUpgradeCompat.setting("efficiencyUpgradeMultiplier",1.4):1));
                    tank.drain(1,IFluidHandler.FluidAction.EXECUTE);energy.receiveEnergy(fe,false);
                }
            }
        }
        if(previous!=reactor)owner.inventoryChanged();
    }
    public CompoundTag save() {
        CompoundTag tag=serializeNBT();tag.put("Tank",tank.writeToNBT(new CompoundTag()));tag.putInt("Reactor",reactor);return tag;
    }
    public void load(CompoundTag tag) { deserializeNBT(tag);tank.readFromNBT(tag.getCompound("Tank"));reactor=tag.getInt("Reactor"); }
}
