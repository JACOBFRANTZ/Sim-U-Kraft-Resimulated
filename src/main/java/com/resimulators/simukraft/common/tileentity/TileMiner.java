package com.resimulators.simukraft.common.tileentity;

import com.resimulators.simukraft.GuiHandler;
import com.resimulators.simukraft.SimUKraft;
import com.resimulators.simukraft.common.interfaces.ISimJob;
import com.resimulators.simukraft.network.GetSimIdPacket;
import com.resimulators.simukraft.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;

public class TileMiner extends TileEntity implements ISimJob {
    private UUID id;
    private int professionID = 7;
    private Boolean hired = false;
    private Set<Integer> sims = new HashSet<>();
    private List<String> sims_name = new ArrayList<>();
    //mode, 0 = quarry, 1 = horizontal mining
    private int mode = 0;
    //default mode is quarry and the width of 5 and a depth of 5
    //width of quarry or width of strip mine shaft/ width of quarry
    private int width = 5;
    //depth of the quarry, distance forward from the miner block
    private int depth = 5;
    //height used for strip mine height
    private int height = 3;
    private boolean renderOutline = false;
    private EnumFacing facing;
    private boolean shouldmine = false;
    private int xprogress = 0;
    private int zprogress = 0;
    private int yprogress = 0;

    public TileMiner(EnumFacing facing){
        this.facing = facing;
    }
    @Override
    public int getProfessionID() {
        return professionID;
    }

    @Override
    public void setHired(boolean hired) {
        this.hired = hired;
    }

    @Override
    public boolean getHired() {
        return this.hired;
    }

    @Override
    public String getProfession() {
        return "Miner";
    }

    @Override
    public void removeSimName(String name) {
        sims_name.remove(name);
    }

    @Override
    public void addSimName(String name) {
        sims_name.add(name);
    }

    @Override
    public void addSim(int sim) {
        sims.add(sim);
    }

    @Override
    public List<String> getnames() {
        return sims_name;
    }

    @Override
    public void removeSim(int sim) {
        sims.remove(sim);
    }

    @Override
    public Set<Integer> getSims() {
        return sims;
    }

    @Nullable
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        setHired(nbt.getBoolean("hired"));
        professionID = nbt.getInteger("profession");
        if (nbt.hasKey("facing")){
        facing = EnumFacing.byName(nbt.getString("facing"));
        System.out.println(facing);
        }
        if (nbt.hasKey("id")) {
            id = nbt.getUniqueId("id");
        }
        shouldmine = nbt.getBoolean("shouldmine");
        width = nbt.getInteger("width");
        height = nbt.getInteger("height");
        depth = nbt.getInteger("depth");
        xprogress = nbt.getInteger("xprogress");
        zprogress = nbt.getInteger("zprogress");
        yprogress = nbt.getInteger("yprogress");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("hired", getHired());
        nbt.setInteger("profession", professionID);
        if (facing != null) nbt.setString("facing",facing.getName());
        if (id != null) {
            nbt.setUniqueId("id", id);
        }
        nbt.setBoolean("shouldmine",shouldmine);
        nbt.setInteger("width",width);
        nbt.setInteger("height",height);
        nbt.setInteger("depth",depth);
        nbt.setInteger("xprogress",xprogress);
        nbt.setInteger("zprogress",zprogress);
        nbt.setInteger("yprogress",yprogress);
        return nbt;
    }

    public void openGui(World worldIn, BlockPos pos, EntityPlayer playerIn) {
        if (getHired()) {
            playerIn.openGui(SimUKraft.instance, GuiHandler.GUI.MINER.ordinal(), worldIn, pos.getX(), pos.getY(), pos.getZ());
        } else {
            PacketHandler.INSTANCE.sendToServer(new GetSimIdPacket(pos.getX(), pos.getY(), pos.getZ(),GuiHandler.GUI.HIRED.ordinal()));

        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos,1,writeToNBT(getUpdateTag()));
    }


    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());

    }
    public void setMode(int mode){
        this.mode = mode;
    }
    //mode, horizontal or quarry style
    public int getMode() {
        return mode;
    }
    //sets width of quarry to the right of the block
    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    //sets depth of the quarry to the front of the block
    public void setDepth(int depth) {
        this.depth = depth;
    }
    //gets width
    public int getWidth() {
        return width;
    }
    //gets depth
    public int getDepth() {
        return depth;
    }

    public int getHeight() {
        return height;
    }

    //used to render outline of the quarry. can be used to visualize the space it uses
    public boolean isRenderOutline(){
        return renderOutline;
    }
    //sets boolean to weather draw outline
    public void setRenderOutline(boolean renderOutline){
        this.renderOutline = renderOutline;
    }


    public EnumFacing getFacing(){
        return facing;
    }

    public void setShouldmine(boolean mine){
        shouldmine = mine;
    }

    public boolean isShouldmine(){
        return this.shouldmine;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox(){
        net.minecraft.util.math.AxisAlignedBB box;
        if (getFacing() != null){
        box = new net.minecraft.util.math.AxisAlignedBB(getPos().offset(facing.getOpposite()).offset(facing.getOpposite().rotateY()),getPos().offset(getFacing(),depth+2).offset(getFacing().rotateY(),width + 2));
        } else {
            return super.getRenderBoundingBox();
        }
        return box;
    }

    public void setXprogress(int xprogress) {
        this.xprogress = xprogress;
    }

    public void setZprogress(int zprogress) {
        this.zprogress = zprogress;
    }

    public void setYprogress(int yprogress) {
        this.yprogress = yprogress;
    }

    public int getXprogress() {
        return xprogress;
    }

    public int getZprogress() {
        return zprogress;
    }

    public int getYprogress() {
        return yprogress;
    }
}
