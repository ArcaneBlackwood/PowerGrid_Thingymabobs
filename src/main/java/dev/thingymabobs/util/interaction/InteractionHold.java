package dev.thingymabobs.util.interaction;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import com.simibubi.create.AllItems;
import dev.thingymabobs.util.SableUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class InteractionHold extends InteractionHandler {
	public static String KEY = "hold";

	public InteractionHold(Player player, BlockPos pos) {
		super(player, pos);
	}

	@Override
	protected void onStart(@Nullable Player player) {
		BlockState state = world.getBlockState(pos);
		if (state == null) return;
		var blockBase = state.getBlock();
		if (!(blockBase instanceof Block block)) return;
		block.interactOnStart(state, this, player, getPlayerCount());
	}
	@Override
	protected void onStop(@Nullable Player player) {
		BlockState state = world.getBlockState(pos);
		if (state == null) return;
		var blockBase = state.getBlock();
		if (!(blockBase instanceof Block block)) return;
		block.interactOnStop(state, this, player, getPlayerCount());
	}
	@Override
	protected boolean tick() {
		BlockState state = world.getBlockState(pos);
		if (state == null || !(state.getBlock() instanceof Block block)) return false;

		if (world.isClientSide && tickClient(world, block))
			return false;


		return block.interactTick(state, this);
	}
	@OnlyIn(Dist.CLIENT)
	protected boolean tickClient(Level world, Block block) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (!(mc.hitResult instanceof BlockHitResult hit)) return true;
		if (!hit.getBlockPos().equals(pos)) return true;
        if (!mc.gameRenderer.getMainCamera().isDetached()) {
            player.swingTime = 0;
            player.swinging = true;
            player.swingingArm = InteractionHand.MAIN_HAND;
        }
		return !block.interactIsValid(player, pos);
	}

	@Override
	protected String getKey() {
		return KEY;
	}


	private static final Vector3d vec = new Vector3d();
	public static interface Block {
		/**
		 * @return If returns false, stops interaction.  Note on server side this wont sync with clients hand.
		 */
		public boolean interactTick(BlockState state, InteractionHold interact);
		default public void interactOnStart(BlockState state, InteractionHold interact, Player player, int newCount) {};
		default public void interactOnStop(BlockState state, InteractionHold interact, Player player, int oldCount) {};

		@OnlyIn(Dist.CLIENT)
		/**
		 * Should only be used on client side.
		 */
		default public void interactStart(BlockPos pos) {
			setActiveLocal(new InteractionHold(Minecraft.getInstance().player, pos));
		}
		default public void interactionStop() {
			InteractionHandler.clearActiveLocal();
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
		default public ItemInteractionResult interactTry(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
			if (AllItems.WRENCH.isIn(stack))
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			if (level.isClientSide && player.isLocalPlayer())
				interactStart(pos);
			return ItemInteractionResult.SUCCESS;
		}
		default public InteractionResult interactTry(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			if (level.isClientSide && player.isLocalPlayer())
				interactStart(pos);
			return InteractionResult.SUCCESS;
		}
	}
}
