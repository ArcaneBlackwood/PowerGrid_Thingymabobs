package dev.thingymabobs.util.interaction;

import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import com.simibubi.create.AllItems;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.util.SableUtils;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class InteractionHoldComponent extends InteractionHandler {
	public static final String KEY = "holdComp";
	protected PlacedComponent placed;

	public InteractionHoldComponent(Player player, InteractLocation pos) {
		super(player, pos);
		Location loc = (Location)pos;
		var board = pos.getBlockEntity(CircuitBoardBlockEntity.class);

		Collection<PlacedComponent> components = board.getComponents(Component.class);
		for (PlacedComponent test : components) {
			if (test.x != loc.x || test.y != loc.y) continue;
			placed = test;
			return;
		}
		Thingymabobs.LOGGER.warn("Failed to find component at "+pos);
	}
	public InteractionHoldComponent(Player player, PlacedComponent placed) {
		super(player, new Location(player.level(), placed));
		if (!(placed.component instanceof Capable))
			throw new IllegalArgumentException("The placed component must have a component instance of InteractionHoldComponent.Capable.  Found: "+placed.component+" on "+placed);
		if (!(placed.component instanceof IInteractableComponent))
			throw new IllegalArgumentException("The placed component must have a component instance of IInteractableComponent.  Found: "+placed.component+" on "+placed);
		this.placed = placed;
	}




	@Override
	protected void onStart(@Nullable Player player) {
		CircuitBoardBlockEntity board = location.getBlockEntity(CircuitBoardBlockEntity.class);
		if (board == null || board.isRemoved()) return;

		Capable component = ((Capable)placed.component);

		component.interactOnStart(placed, this, player, getPlayerCount());
	}
	@Override
	protected void onStop(@Nullable Player player) {
		CircuitBoardBlockEntity board = location.getBlockEntity(CircuitBoardBlockEntity.class);
		if (board == null || board.isRemoved()) return;

		Capable component = ((Capable)placed.component);

		component.interactOnStop(placed, this, player, getPlayerCount());
	}
	@Override
	protected boolean tick() {
		CircuitBoardBlockEntity board = location.getBlockEntity(CircuitBoardBlockEntity.class);
		if (board == null || board.isRemoved()) {
			return false;
		}

		Capable component = ((Capable)placed.component);
		if (location.isClient() && tickClient(board, component)) {
			return false;
		}

		return component.interactTick(placed, this);
	}
	/**
	 * @return Returns true if should stop interaction
	 */
	@OnlyIn(Dist.CLIENT)
	protected boolean tickClient(CircuitBoardBlockEntity board, Capable component) {
		if (placed.destroyed) return true;

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (!(mc.hitResult instanceof BlockHitResult hit)) return true;
		if (!hit.getBlockPos().equals(location.pos)) return true;
        if (!mc.gameRenderer.getMainCamera().isDetached()) {
            player.swingTime = 0;
            player.swinging = true;
            player.swingingArm = InteractionHand.MAIN_HAND;
        }
		BlockState state = location.getBlockState();
		var hitLocalPos = hit.getLocation().subtract(location.pos.getX(), location.pos.getY(), location.pos.getZ());
		hitLocalPos = VecHelper.rotateCentered(hitLocalPos, -CircuitBoardBlock.getAngleY(state), Direction.Axis.Y);
		hitLocalPos = VecHelper.rotateCentered(hitLocalPos, -CircuitBoardBlock.getAngleX(state), Direction.Axis.X);

		IInteractableComponent interact = (IInteractableComponent) placed.component;
		AABB outline = interact.getShape(placed).bounds().inflate(1 / 32f);
		if(!outline.contains(hitLocalPos)) return true;
		
		return !component.interactIsValid(player, location.pos);
	}



	@Override
	protected String getKey() {
		return KEY;
	}


	public static class Location extends InteractLocation {
		int x = 0, y = 0;
		public Location(Level world, PlacedComponent placed) {
			super(world, placed.getPos());
			x = placed.x;
			y = placed.y;
			hash ^= x << 16 | y;
		}
		public Location(FriendlyByteBuf buf) {
			super(buf);
			x = buf.readByte();
			y = buf.readByte();
		}
		@Override
		protected void recomputeHash() {
			super.recomputeHash();
			hash ^= x << 16 | y;
		}
		@Override
		public void write(FriendlyByteBuf buf) {
			super.write(buf);
			buf.writeByte(x);
			buf.writeByte(y);
		}
		@Override
		public boolean equals(Object obj) {
			if (!super.equals(obj)) return false;
			return obj instanceof Location that && that.x == this.x && that.y == this.y;
		}
		@Override
		public String toString() {
			return "ComponentLocation["+x+"/"+y+", "+world+", "+pos+"]@" + Integer.toHexString(this.hashCode());
		}
	}

	private static final Vector3d vec = new Vector3d();
	public static interface Capable {
		/**
		 * @return If returns false, stops interaction.  Note on server side this wont sync with clients hand.
		 */
		default public boolean interactTick(PlacedComponent placed, InteractionHoldComponent interact) {
			return true;
		}
		default public void interactOnStart(PlacedComponent placed, InteractionHoldComponent interact, Player player, int newCount) {};
		default public void interactOnStop(PlacedComponent placed, InteractionHoldComponent interact, Player player, int oldCount) {};

		@OnlyIn(Dist.CLIENT)
		default public void interactStart(PlacedComponent placed) {
			setActiveLocal(new InteractionHoldComponent(Minecraft.getInstance().player, placed));
		}
		@OnlyIn(Dist.CLIENT)
		default public void interactionStop() {
			InteractionHandler.clearActiveLocal();
		}
		default public void interactStart(Player player, PlacedComponent placed) {
			InteractionHandler.setActive(player, new Location(player.level(), placed), InteractionHoldComponent::new);
		}
		default public void interactionStop(Player player) {
			InteractionHandler.clearActive(player);
		}


		default public boolean interactIsValid(Player player, BlockPos pos) {
			if (AllItems.WRENCH.isIn(player.getMainHandItem()))
				return false;

        	double reach = player.blockInteractionRange() + 1f;
        	Vec3 eyePosition = player.getEyePosition();
			double distance = SableUtils.getGlobalPos(player.level(), pos, vec)
				.distanceSquared(eyePosition.x, eyePosition.y, eyePosition.z);
			if (distance > reach * reach)
           		return true;
			return true;
		}
		default public boolean interactTry(CircuitBoardBlockEntity be, PlacedComponent placed, Player player) {
			if (AllItems.WRENCH.isIn(player.getMainHandItem()))
				return false;
			if (be.getLevel().isClientSide && player.isLocalPlayer())
				interactStart(placed);
			return true;
		}

		default public boolean interactIsActive(Player player, PlacedComponent placed) {
			InteractionHandler handler = InteractionHandler.getActive(player);
			if (handler == null || !(handler.location instanceof Location loc)) return false;
			if (loc.world != player.level() || !loc.pos.equals(placed.getPos())
				|| loc.x != placed.x || loc.y != placed.y) return false;
			return true;
		}
	}
}
