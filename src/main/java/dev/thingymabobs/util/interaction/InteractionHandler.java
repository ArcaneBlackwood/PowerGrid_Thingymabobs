package dev.thingymabobs.util.interaction;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class InteractionHandler {
	protected Set<Player> players;
	protected final InteractLocation location;

	protected InteractionHandler(Player player, InteractLocation location) {
		if (location.world == null) location.setWorld(player.level());
		if (player.level() != location.world)
			throw new IllegalArgumentException("Player must be in same level as InteractLocation.  Player is in "+player.level()+", expecting "+location.world);
		this.location = location;
		players = new HashSet<>();
	}




	/**
	 * @param player When client side, always null.  If value is null on server, all players removed.
	 */
	protected abstract void onStop(@Nullable Player player);
	/**
	 * @param player When client side, always null.  If value is null on server, all players removed.
	 */
	protected abstract void onStart(@Nullable Player player);
	/**
	 * @return If returns false, stops interaction.  Note on server side this wont sync with clients hand.
	 */
	protected abstract boolean tick();
	protected abstract String getKey();
	protected boolean checkRelease(int button, int action, int modifiers) {
		return action == GLFW.GLFW_RELEASE;
	}




	public boolean hasPlayer(Player player) {
		return players==null ? player.isLocalPlayer() : players.contains(player);
	}
	public int getPlayerCount() {
		return players==null ? 1 : players.size();
	}

	public Stream<Pair<Player, BlockHitResult>> getBlockHits() {
		if (players == null) return getBlockHitsClient();
		return players.stream().map((Player player) ->
				new Pair<>(player, getBlockHit(player)))
			.filter((hit) -> hit != null);
	}
	@OnlyIn(Dist.CLIENT)
	public Stream<Pair<Player, BlockHitResult>> getBlockHitsClient() {
		var mc = Minecraft.getInstance();
		if (mc.hitResult instanceof BlockHitResult hit) return Stream.of(new Pair<>(mc.player, hit));
		return Stream.empty();
	}
	public BlockHitResult getBlockHit(Player player) {
		double range = player.blockInteractionRange() + 0.5f;
		Vec3 eyePos = player.getEyePosition(0);
		Vec3 viewNorm = player.getViewVector(0);
		Vec3 endPos = eyePos.add(viewNorm.x * range, viewNorm.y * range, viewNorm.z * range);

		BlockHitResult blockhitresult = null;
		if (player.level() == location.world) {
			BlockState state = location.world.getBlockState(location.pos);
			blockhitresult = location.world.clipWithInteractionOverride(
				eyePos, endPos, location.pos, state.getCollisionShape(location.world, location.pos, CollisionContext.of(player)), state);
		}
		
		if (blockhitresult == null)
			return BlockHitResult.miss(endPos, Direction.getNearest(viewNorm), location.pos);
		else return blockhitresult;
	}

	public static BlockHitResult getBlockHitOcclude(Player player) {
		if (player.level().isClientSide && player == Minecraft.getInstance().player) {
			BlockHitResult hit = getBlockHitOccludeClient(player);
			if (hit != null) return hit;
		}
		if (!(player.pick(player.blockInteractionRange(), 0, false) instanceof BlockHitResult hit)) return null;
		return hit;
	}
	@OnlyIn(Dist.CLIENT)
	private static BlockHitResult getBlockHitOccludeClient(Player player) {
		var mc = Minecraft.getInstance();
		if (player == mc.player && mc.hitResult instanceof BlockHitResult hit) return hit;
		return null;
	}
}
