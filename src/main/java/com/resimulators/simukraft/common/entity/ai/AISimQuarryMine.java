package com.resimulators.simukraft.common.entity.ai;

import com.resimulators.simukraft.common.entity.entitysim.EntitySim;
import com.resimulators.simukraft.common.entity.player.SaveSimData;
import com.resimulators.simukraft.common.tileentity.TileMiner;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;


public class AISimQuarryMine extends EntityAIBase{
    private World world;
    private EntitySim sim;
    private int miningDelay = 2;
    private BlockPos targetPos;
    private boolean pathFound = false;
    private double currentDistance = 0;
    private int timeout = 10;
    private int stairPosX = 0;
    private int stairPosY = 0;
    private int stairPosZ = 0;
    private int tries = 0;
    // direction, 0 = right, 1 = forward, 2 = left, 3 = backwards
    private int direction = 0;
    private int x = 0;
    private int y = 0;
    private int z = 0;
    private int width = 0;
    private int depth = 0;
    private TileMiner miner;
    private Random rand = new Random();
    public AISimQuarryMine(EntitySim sim, World world) {
        this.sim = sim;
        this.world = world;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (sim.isWorking() && sim.getJobBlockPos() != null && world.getTileEntity(sim.getJobBlockPos()) != null){
        boolean shouldmine = ((TileMiner) Objects.requireNonNull(world.getTileEntity(sim.getJobBlockPos()))).isShouldmine();
        return sim.getProfession() == 7 && sim.isWorking() && shouldmine;
        }
        return false;
    }

    @Override
    public void startExecuting() {
        x = 0;
        y = 0;
        z = 0;
        direction = 0;
        stairPosX = 1;
        stairPosY = 0;
        stairPosZ = 0;
        miner = (TileMiner) world.getTileEntity(sim.getJobBlockPos());
        if (miner == null)
            return;
        sim.setHeldItem(EnumHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
        targetPos = sim.getJobBlockPos();
        width = miner.getWidth();
        depth = miner.getDepth()-1;
        x = miner.getXprogress();
        z = miner.getZprogress();
        y = miner.getYprogress();
    }


    @Override
    public void updateTask() {
        if (targetPos == null) {
            pathFound = false;
            if (miningDelay <= 0) {
                miningDelay = 2;
                x++;
                targetPos = sim.getJobBlockPos().add(0, -1, 0);
                targetPos = targetPos.offset(miner.getFacing());
                targetPos = targetPos.offset(miner.getFacing().rotateY());
                if (x > width) {
                    x = 0;
                    z++;
                }
                if (z > depth) {
                    x = 0;
                    z = 0;
                    y++;
                }
                targetPos = targetPos.offset(miner.getFacing().rotateY(), x-1);
                if (x == 0)targetPos = targetPos.offset(miner.getFacing().rotateY());
                targetPos = targetPos.offset(EnumFacing.DOWN, y);
                targetPos = targetPos.offset(miner.getFacing(), z);
                if (targetPos.getY() < 1 || world.getBlockState(targetPos).getBlock().equals(Blocks.BEDROCK)) {
                    targetPos = null;
                    sim.setNotWorking();
                    Objects.requireNonNull(SaveSimData.get(world)).getFaction(sim.getFactionId()).addUnemployedSim(sim.getUniqueID());
                    ((TileMiner) Objects.requireNonNull(world.getTileEntity(sim.getJobBlockPos()))).setHired(false);
                    ((TileMiner) Objects.requireNonNull(world.getTileEntity(sim.getJobBlockPos()))).setId(null);
                    List<UUID> players = Objects.requireNonNull(SaveSimData.get(world)).getFaction(sim.getFactionId()).getPlayers();
                    for (UUID id : players) {
                        EntityPlayer player = world.getPlayerEntityByUUID(id);
                        if (player != null) {
                            player.sendMessage(new TextComponentString("Miner " + sim.getName() + " has finished mining at position " + sim.getJobBlockPos() + "\n and has been fired"));
                        }
                    }

                    sim.setJobBlockPos(null);
                }

            } else miningDelay--;

        } else {
            if (targetPos != sim.getJobBlockPos()) {
                if (checkinvforitem((world.getBlockState(targetPos).getBlock().getItemDropped(world.getBlockState(targetPos),rand,0)),true)){
                if (sim.getDistance(targetPos.getX(),targetPos.getY(),targetPos.getZ()) <= 5){
                addItemtoInv(new ItemStack(Item.getItemFromBlock(world.getBlockState(targetPos).getBlock())));
                world.setBlockState(targetPos, Blocks.AIR.getDefaultState());
                animate();
                if (y == stairPosY) {
                    //to the right of the miner block
                    if (z == 0 && direction == 0 && x == stairPosX) {
                        if (x == width) {
                            if (checkForSlab()){
                            direction = 1;
                            stairPosZ += 1;
                            placeBlock(targetPos, Blocks.WOODEN_SLAB, miner.getFacing().getOpposite());
                            }else endWork("day");
                        } else {
                            if (checkForStairs()){
                            placeBlock(targetPos, Blocks.OAK_STAIRS, miner.getFacing().rotateYCCW());
                            stairPosX += 1;
                            stairPosY += 1;
                        }else endWork("day");}

                    }

                    //direction same facing as the miner blocks
                    if ((x == width || x == width-1) && direction == 1 && z == stairPosZ) {
                        if (x == width){
                            if (z == depth){
                                if (checkForSlab()){
                                placeBlock(targetPos,Blocks.WOODEN_SLAB,EnumFacing.NORTH);
                                direction = 2;
                                stairPosX-= 2;
                                stairPosY++;
                                }else {endWork("day");}
                            }else{
                                if (checkForStairs()){
                                placeBlock(targetPos, Blocks.OAK_STAIRS,miner.getFacing().getOpposite());
                                stairPosZ++;
                                stairPosY++;
                                } else endWork("day");
                            }

                        }else if (x == width-1){
                            if (z == depth){
                                if (checkForStairs()){
                                placeBlock(targetPos,Blocks.OAK_STAIRS,miner.getFacing().rotateY());
                            }else endWork("day");}
                        }
                    }
                    if (direction == 2 && z == depth){
                        System.out.println("why this ain't worken");
                    }
                    //from right to left
                    if ((z == depth || z == depth-1) && direction == 2 && x == stairPosX) {
                        if (z == depth){
                            if (x == 1){
                                if (checkForSlab()){
                                placeBlock(targetPos,Blocks.WOODEN_SLAB,miner.getFacing());
                                direction = 3;
                                stairPosY++;
                                stairPosZ-=2;
                                }else endWork("day");
                            }else{
                                if (checkForStairs()){
                                placeBlock(targetPos,Blocks.OAK_STAIRS,miner.getFacing().rotateY());
                                stairPosY++;
                                stairPosX--;
                            }else endWork("day");}
                        }else if (z == depth-1){
                            if (x == 1){
                                if (checkForStairs()){
                                placeBlock(targetPos,Blocks.OAK_STAIRS,miner.getFacing());
                            }else endWork("day");}
                        }
                    }

                    //coming back towards the miner block
                    if ((x == 1 || x == 2) && direction == 3 && z == stairPosZ) {
                        if (x == 1){
                            if (z == 0){
                                if (checkForSlab()){
                                placeBlock(targetPos,Blocks.WOODEN_SLAB,miner.getFacing());
                                stairPosX++;
                                direction = 0;
                                }else endWork("day");
                            }else{
                                if (checkForStairs()){
                                placeBlock(targetPos,Blocks.OAK_STAIRS,miner.getFacing());
                                stairPosZ--;
                                stairPosY++;
                            }else endWork("day");}
                        }else{
                            if (z == 0){
                                if (checkForStairs()) {
                                    placeBlock(targetPos, Blocks.OAK_STAIRS, miner.getFacing().rotateYCCW());
                                    stairPosY++;
                                    direction = 0;
                                }else endWork("day");
                            }
                        }
                    }
                }
                targetPos = null;
            }else {
                    // pathfinding towards block

                    if (!pathFound && sim.getDistance(targetPos.getX(),targetPos.getY(),targetPos.getZ()) >= 2){
                        if (tries > 2){
                            sim.attemptTeleport(targetPos.getX(),targetPos.getY()+2,targetPos.getZ());
                        }
                        int randomOffset = 0;
                        if (tries != 0) randomOffset = rand.nextInt(tries)-1;

                        Vec3d vec = new Vec3d(targetPos.getX()+randomOffset,targetPos.getY()+1,targetPos.getZ()+randomOffset);
                        pathFound = sim.getNavigator().tryMoveToXYZ(vec.x,vec.y,vec.z,sim.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
                        currentDistance = sim.getDistance(targetPos.getX(),targetPos.getY(),targetPos.getZ());
                        if (pathFound)tries = 0;
                    }else if (sim.getDistance(targetPos.getX(),targetPos.getY(),targetPos.getZ()) >= currentDistance){
                        timeout--;
                    }
                    if (timeout <= 0){
                        timeout = 10;
                        tries++;
                        pathFound = false;
                    }
                }

            }else{
                    endWork("day");
                    targetPos = null;
                }} else {
                targetPos = null;
            }}
        }


    @Override
    public boolean shouldContinueExecuting() {
        if (width != miner.getWidth() || depth+1 != miner.getDepth() || !world.isDaytime()){
            System.out.println("width or depth changed. stopping AIsd");
            endWork("night");
            return false;
        }
        return shouldExecute();
    }

    private void placeBlock(BlockPos pos, Block block, EnumFacing facing) {
        int timesToRotate = 0;
        IBlockState state = block.getDefaultState();
        if (block instanceof BlockStairs && pos != null) {
                EnumFacing blockfacing = block.getDefaultState().getValue(BlockStairs.FACING);

                while (blockfacing != facing) {
                    blockfacing = blockfacing.rotateY();
                    timesToRotate++;
                }}

        if (block instanceof BlockSlab){
            state = block.getDefaultState().withProperty(BlockSlab.HALF,BlockSlab.EnumBlockHalf.TOP);
        }
            switch (timesToRotate) {
                case 0:
                    world.setBlockState(pos, state);
                    break;
                case 1:
                    world.setBlockState(pos, state.withRotation(Rotation.CLOCKWISE_90));
                    break;
                case 2:
                    world.setBlockState(pos, state.withRotation(Rotation.CLOCKWISE_180));
                    break;
                case 3:
                    world.setBlockState(pos, state.withRotation(Rotation.COUNTERCLOCKWISE_90));
                    break;
            }
        }


    private boolean checkinvforitem(@Nullable Item item, boolean adding) {
        IItemHandler inv = sim.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.SOUTH);
        for (int i = 0; i < inv.getSlots(); i++) {
            if (adding) {
                if (inv.getStackInSlot(i).getItem().equals(item)) {
                    if (inv.getStackInSlot(i).getMaxStackSize() > inv.getStackInSlot(i).getCount()) {
                        return true;
                    }
                }else if (inv.getStackInSlot(i).isEmpty())return true;
            } else {
                if (inv.getStackInSlot(i).getItem().equals(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addItemtoInv(ItemStack item) {
        IItemHandler inv = sim.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.SOUTH);
            if (!ItemHandlerHelper.insertItemStacked(inv,item,false).isEmpty()){
                endWork("day");
            }
        }

    private void animate(){
        sim.getLookHelper().setLookPosition(targetPos.getX(),targetPos.getY(),targetPos.getZ(),360,360);
        sim.world.playSound(null,targetPos,world.getBlockState(targetPos).getBlock().getSoundType().getBreakSound(), SoundCategory.BLOCKS,1.0f,(rand.nextFloat() - 0.5f) / 5);
        sim.setActiveHand(EnumHand.MAIN_HAND);
        sim.swingArm(sim.getActiveHand());
    }

    private void endWork(String endOfDay){
        if (endOfDay.equals("day")) {
             String string = String.format("Miner %s, doesn't have enough space to mine. or doesn't have the required%n resources (stairs, slabs, ect) to continue", sim.getName());
             SaveSimData.get(sim.world).getFaction(sim.getFactionId()).sendFactionChatMessage(string);
        }
        sim.setEndWork();
        sim.setNotWorking();
        sim.getNavigator().tryMoveToXYZ(sim.getJobBlockPos().getX(),sim.getJobBlockPos().getY(),sim.getJobBlockPos().getZ(),sim.getAIMoveSpeed());
        miner.setXprogress(x);
        miner.setZprogress(z);
        miner.setYprogress(y);
    }


    private boolean checkForStairs(){
        return checkinvforitem(Item.getItemFromBlock(Blocks.OAK_STAIRS),false);
    }

    private boolean checkForSlab(){
        return checkinvforitem(Item.getItemFromBlock(Blocks.WOODEN_SLAB),false);
    }
}