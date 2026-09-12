package dev.thingymabobs.component.trancievers;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.components.OrientableComponent;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.thingymabobs.mixin.LinkBehaviourExt;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LinkComponentBehaviour {
	public LinkBehaviour link;
	protected AABB slot1, slot2;
	protected float value;
	public boolean isTransmitter;

	protected LinkComponentBehaviour(boolean isTransmitter, AABB slot1, AABB slot2) {
		this.isTransmitter = isTransmitter;
		this.slot1 = slot1;
		this.slot2 = slot2;
	}
	public void initialize(PlacedComponent placed, SmartBlockEntity blockEntity) {
		MultiBehaviour<Integer> multi = blockEntity.getBehaviour(MultiBehaviour.getType(Integer.class));
		if (multi == null) {
			multi = new MultiBehaviour<>(Integer.class, blockEntity);
			blockEntity.attachBehaviourLate(multi);
		}

		Pair<ValueBoxTransform, ValueBoxTransform> slots = Pair.of(null, null);
		if (isTransmitter)
			link = LinkBehaviour.transmitter(blockEntity, slots, () -> Math.round(value));
		else {
			link = LinkBehaviour.receiver(blockEntity, slots, null);
			LinkBehaviourExt linkExt = (LinkBehaviourExt)link;
			linkExt.setRecieveCallback((float value) -> this.value = value);
		}
		multi.addBehaviour(placed.x + placed.y * Short.MAX_VALUE, link);
	}
	public void save(PlacedComponent placed) {
		if (link == null || link.blockEntity == null || link.blockEntity.isRemoved()) return;
		Couple<Frequency> key = link.getNetworkKey();
		ItemStack currentSlot1 = key.getFirst().getStack();
		ItemStack currentSlot2 = key.getSecond().getStack();
		if (!ItemStack.matches(placed.get(ATrancieverComponent.PROP_SLOT1), currentSlot1)) {
			placed.set(ATrancieverComponent.PROP_SLOT1, currentSlot1.copy());
			placed.notifyClients(ATrancieverComponent.PROP_SLOT1);
		}
		if (!ItemStack.matches(placed.get(ATrancieverComponent.PROP_SLOT2), currentSlot2)) {
			placed.set(ATrancieverComponent.PROP_SLOT2, currentSlot2.copy());
			placed.notifyClients(ATrancieverComponent.PROP_SLOT2);
		}
	}
	public void load(PlacedComponent placed) {
		if (link == null || link.blockEntity == null || link.blockEntity.isRemoved()) return;
		ItemStack newSlot1 = placed.get(ATrancieverComponent.PROP_SLOT1);
		ItemStack newSlot2 = placed.get(ATrancieverComponent.PROP_SLOT2);
		Couple<Frequency> key = link.getNetworkKey();
		if (!ItemStack.matches(newSlot1, key.getFirst().getStack())) link.setFrequency(true, newSlot1);
		if (!ItemStack.matches(newSlot2, key.getSecond().getStack())) link.setFrequency(false, newSlot2);
	}
	public void onRemove() {
		if (link == null || link.blockEntity == null || link.blockEntity.isRemoved()) return;
		MultiBehaviour<Integer> multi = link.blockEntity.getBehaviour(MultiBehaviour.getType(Integer.class));
		if (multi == null) return;
		multi.removeBehaviour(link);
		link = null;
	}




	protected static Matrix4f ROTATE_XN90 = new Matrix4f(
		1,  0,  0,  0,
		0,  0, 1,  0,
		0,  -1,  0,  0,
		0,  0,  0,  1);
	public void render(PlacedComponent placed, float partialTicks, PoseStack ms, net.minecraft.client.renderer.MultiBufferSource buffer, int light, int overlay) {
		if (link == null) return;

		Entity cameraEntity = net.minecraft.client.Minecraft.getInstance().cameraEntity;
		float max = AllConfigs.client().filterItemRenderDistance.getF();
		{
			CircuitBoardBlockEntity circuitBoard = (placed.getWorld().getBlockEntity(placed.getPos(), ModdedBlockEntities.CIRCUIT_BOARD.get())).orElse(null);
			if ((circuitBoard == null || !circuitBoard.isVirtual()) && cameraEntity != null && cameraEntity.position()
				.distanceToSqr(VecHelper.getCenterOf(placed.getPos())) > (max * max))
				return;
		}
		
		Vec3 hitLocalPos = null;
		{
			Level world = placed.getWorld();
			BlockPos pos = placed.getPos();
			HitResult hit = net.minecraft.client.Minecraft.getInstance().hitResult;
			if (hit != null && hit instanceof BlockHitResult result && result.getBlockPos().equals(pos))
				hitLocalPos = transformHitPosLocal(placed, world, pos, hit);
		}

		var freqs = link.getNetworkKey();
		for (boolean first : Iterate.trueAndFalse) {
			AABB slot = (first ? slot1 : slot2);
			ItemStack stack = freqs.get(first).getStack();
			//LevelRenderer.renderLineBox(ms, buffer.getBuffer(RenderType.lines()), slot, 1, 0, 0, 1);

			ms.pushPose();
			ms.translate(((float)slot.minX + slot.maxX) * 0.5f,
				((float)slot.minY + slot.maxY) * 0.5f,
				((float)slot.minZ + slot.maxZ) * 0.5f);
			ms.scale((float)slot.getXsize(), (float)slot.getYsize(), (float)slot.getZsize());
			ms.mulPose(ROTATE_XN90);
			if (!stack.isEmpty()) {
				ms.pushPose();
				ms.scale(2,2,2);
				ValueBoxRenderer.renderItemIntoValueBox(stack, ms, buffer, light, overlay);
				ms.popPose();
			}
			if (hitLocalPos != null && slot.contains(hitLocalPos)) {
				ms.pushPose();
				ms.scale(4,4,4);
				ms.translate(-0.5f, -0.5f, 0);
				(stack.isEmpty() ? AllIcons.VALUE_BOX_HOVER_4PX : AllIcons.VALUE_BOX_HOVER_6PX)
					.render(ms, buffer, 0xffffff);
				ms.popPose();
			}
			ms.popPose();
		}
	}


	public boolean interact(PlacedComponent placed, Player player) {
		Level world = placed.getWorld();
		boolean isClient = world.isClientSide();
		if (link == null && !isClient) return false;

		BlockPos pos = placed.getPos();
		HitResult hit = world.isClientSide() ? net.minecraft.client.Minecraft.getInstance().hitResult : player.pick(20.0, 0.0f, false);
		if (hit == null || !(hit instanceof BlockHitResult result)) return false;
		if (!result.getBlockPos().equals(pos) ) return false; //Double check

		var mainHand = player.getMainHandItem();
		if (AllItems.LINKED_CONTROLLER.isIn(mainHand))
			return false;
		if (AllItems.WRENCH.isIn(mainHand))
			return false;

		if (player.hasItemInSlot(EquipmentSlot.OFFHAND)) {
			if (isClient) return true;
			link.setFrequency(true, mainHand);
			link.setFrequency(false, player.getOffhandItem());
			world.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, .25f, .1f);
			return true;
		}

		Vec3 hitLocalPos = transformHitPosLocal(placed, world, pos, hit);
	
		for (boolean first : Iterate.trueAndFalse) {
			AABB slot = (first ? slot1 : slot2).inflate(1 / 32f);
			if (!slot.contains(hitLocalPos)) continue;

			if (isClient) return true;
			link.setFrequency(first, mainHand);
			world.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, .25f, .1f);
			return true;
		}
		return false;
	}
	protected Vec3 transformHitPosLocal(PlacedComponent placed, Level world, BlockPos pos, HitResult hit) {
		Vec3 hitLocalPos = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
		BlockState state = world.getBlockState(pos);
		hitLocalPos = VecHelper.rotateCentered(hitLocalPos, -CircuitBoardBlock.getAngleY(state), Direction.Axis.Y);
		hitLocalPos = VecHelper.rotateCentered(hitLocalPos, -CircuitBoardBlock.getAngleX(state), Direction.Axis.X);
		hitLocalPos = hitLocalPos.add(-placed.x/16f, -2/16f, -placed.y/16f);

		ComponentFootprint footprint = placed.footprint();
		Orientation rotation = placed.get(OrientableComponent.ORIENTATION);
		float centerX = footprint.getWidth()/32f, centerZ = footprint.getHeight()/32f;
		float direction = -1;
		switch (rotation) {
			case UP:
				centerX = centerZ;
				break;
			case DOWN:
				centerZ = centerX;
				break;
			case LEFT:
			case RIGHT:
				direction = 1;
				break;
			default:
				break;
		}
		return VecHelper.rotate(hitLocalPos.add(-centerX, 0, -centerZ), rotation.ordinal() * 90 * direction, Direction.Axis.Y).add(centerX, 0, centerZ);
	}
	
	
	public float getRecieved() {
		return value;
	}
	public void setTransmission(float value) {
		if (value < 0) value = 0;
		if (value > 15) value = 15;
		if (value == this.value) return;
		this.value = value;
		link.notifySignalChange();
	}




	public static class MultiBehaviour<K extends Object> extends BlockEntityBehaviour {
		protected static Map<Class<?>, BehaviourType<MultiBehaviour<?>>> types = new HashMap<>();
		protected Class<?> type;
		protected BiMap<K, BlockEntityBehaviour> behaviours = HashBiMap.create();
		public MultiBehaviour(Class<?> type, SmartBlockEntity be) {
			super(be);
			this.type = type;
			if (!types.containsKey(type)) types.put(type, new BehaviourType<MultiBehaviour<?>>("multi"));
		}
		public void addBehaviour(K key, BlockEntityBehaviour behaviour) {
			behaviours.put(key, behaviour);
			behaviour.blockEntity = this.blockEntity;
			behaviour.initialize();
		}
		public K removeBehaviour(BlockEntityBehaviour behaviour) {
			K key = behaviours.inverse().remove(behaviour);
			if (key == null) return null;
			behaviour.unload();
			return key;
		}
		@Override
		public BehaviourType<?> getType() {
			return types.get(type);
		}
		@SuppressWarnings("unchecked")
		public static <T> BehaviourType<MultiBehaviour<T>> getType(Class<T> type) {
			return (BehaviourType<MultiBehaviour<T>>)(BehaviourType<?>)types.get(type);
		}


		@Override
		public void tick() {
			super.tick();
			for (BlockEntityBehaviour behaviour : behaviours.values())
				behaviour.tick();
		}
		@Override
		public void initialize() {
			super.initialize();
			for (BlockEntityBehaviour behaviour : behaviours.values())
				behaviour.initialize();
		}
		@Override
		public void onBlockChanged(BlockState oldState) {
			super.onBlockChanged(oldState);
			for (BlockEntityBehaviour behaviour : behaviours.values())
				behaviour.onBlockChanged(oldState);
		}
		@Override
		public void onNeighborChanged(BlockPos neighborPos) {
			super.onNeighborChanged(neighborPos);
			for (BlockEntityBehaviour behaviour : behaviours.values())
				behaviour.onNeighborChanged(neighborPos);
		}
		@Override
		public void unload() {
			super.unload();
			for (BlockEntityBehaviour behaviour : behaviours.values())
				behaviour.unload();
		}
		@Override
		public void destroy() {
			super.destroy();
			for (BlockEntityBehaviour behaviour : behaviours.values())
				behaviour.destroy();
		}

		@Override
		public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
			super.write(nbt, registries, clientPacket);
			for (var entry : behaviours.entrySet()) {
				CompoundTag nested = new CompoundTag();
				entry.getValue().write(nested, registries, clientPacket);
				nbt.put(entry.getKey().toString(), nested);
			}
		}
		@Override
		public void writeSafe(CompoundTag nbt, Provider registries) {
			super.writeSafe(nbt, registries);
			for (var entry : behaviours.entrySet()) {
				CompoundTag nested = new CompoundTag();
				entry.getValue().writeSafe(nbt, registries);
				nbt.put(entry.getKey().toString(), nested);
			}
		}
		@Override
		public void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
			super.read(nbt, registries, clientPacket);
			for (var entry : behaviours.entrySet()) {
				CompoundTag nested = nbt.getCompound(entry.getKey().toString());
				if (nested==null) return;
				entry.getValue().read(nested, registries, clientPacket);
			}
		}

		@Override
		public String toString() {
			StringBuilder str = new StringBuilder();
			str.append("MultiBehaviour[");
			boolean first = true;
			for (var entry : behaviours.entrySet()) {
				if (first) first = false;
				else str.append(", ");
				str.append(entry.getKey().toString()).append(":").append(entry.getValue().toString());
			}
			str.append("]");
			return str.toString();
		}
	}
}
