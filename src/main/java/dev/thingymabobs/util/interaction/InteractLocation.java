package dev.thingymabobs.util.interaction;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class InteractLocation {
	public int hash;
	public Level world;
	public BlockPos pos;


	public InteractLocation(Level world, BlockPos pos) {
		this.world = world;
		this.pos = pos;
		recomputeHash();
	}
	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(pos);
	}
	public InteractLocation(FriendlyByteBuf buf) {
		pos = buf.readBlockPos();
		recomputeHash();
	}



	public void setWorld(Level world) {
		this.world = world;
		recomputeHash();
	}


	protected void recomputeHash() {
		if (world == null) hash = pos.hashCode();
		else hash = pos.hashCode() * world.dimension().hashCode();
	}
	@Override
	public int hashCode() {
		return hash;
	}
	@Override
	public boolean equals(Object obj) {
		return obj instanceof InteractLocation that && that.pos.equals(pos) && that.world.equals(world);
	}
	@Override
	public String toString() {
		return "InteractLocation["+world+", "+pos+"]@" + Integer.toHexString(this.hashCode());
	}
	


	public BlockState getBlockState() {
		return world.getBlockState(pos);
	}
	public Block getBlock() {
		BlockState state = world.getBlockState(pos);
		if (state == null) return null;
		return state.getBlock();
	}
	public <T extends Block> T getBlock(Class<T> type) {
		BlockState state = world.getBlockState(pos);
		if (state == null) return null;
		Block block = state.getBlock();
		if (type.isInstance(block)) return type.cast(block);
		return null;
	}
	public BlockEntity getBlockEntity() {
		return world.getBlockEntity(pos);
	}
	public SmartBlockEntity getSmartBlockEntity() {
		BlockEntity entity = world.getBlockEntity(pos);
		if (entity instanceof SmartBlockEntity sbe) return sbe;
		return null;
	}
	public <T extends BlockEntity> T getBlockEntity(Class<T> type) {
		BlockEntity entity = world.getBlockEntity(pos);
		if (type.isInstance(entity)) return type.cast(entity);
		return null;
	}
	public boolean isClient() {
		return world.isClientSide;
	}
	public boolean isServer() {
		return !world.isClientSide;
	}



	@FunctionalInterface
	protected static interface Reader {
		public InteractLocation read(FriendlyByteBuf buf);
	}
}