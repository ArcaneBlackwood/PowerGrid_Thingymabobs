package dev.thingymabobs.util.interaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import com.mojang.datafixers.util.Pair;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.mixin.unit.KeyMappingMixin;
import dev.thingymabobs.registry.ModPackets;
import dev.thingymabobs.util.DistLocal;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public abstract class InteractionHandler {
	// #### REGISTER & CONSTRUCTORS
	public static Map<String, Type> TYPES = new HashMap<>();
	@FunctionalInterface
	protected static interface Constructor {
		public InteractionHandler construct(Player player, InteractLocation pos);
	}
	protected static record Type(Constructor constructor, InteractLocation.Reader reader) { }

	public static void register(IEventBus modBus) {
		TYPES.put(InteractionHold.KEY, new Type(InteractionHold::new, InteractLocation::new));
		TYPES.put(InteractionHoldComponent.KEY, new Type(InteractionHoldComponent::new, InteractionHoldComponent.Location::new));

		NeoForge.EVENT_BUS.addListener(InteractionHandler::tickAllServer);
		NeoForge.EVENT_BUS.addListener(InteractionHandler::onPlayerTrack);
		NeoForge.EVENT_BUS.addListener(InteractionHandler::onPlayerUntrack);
		NeoForge.EVENT_BUS.addListener(InteractionHandler::onLevelUnload);
	}
	@OnlyIn(Dist.CLIENT)
	public static void registerClient(IEventBus modBus) {
		NeoForge.EVENT_BUS.addListener(InteractionHandler::tickAllClient);
		NeoForge.EVENT_BUS.addListener(InteractionHandler::onServerDisconnect);
	}


	

	// #### GLOBAL STATES
	private static InteractionHandler ACTIVE_LOCAL = null;
	/// Can contain duplicate handlers
	private static DistLocal<Map<Player, InteractionHandler>> ACTIVE_PLAYERS = 
		new DistLocal<>($ -> new HashMap<>());
	private static DistLocal<Map<InteractLocation, InteractionHandler>> ACTIVE_BLOCKS =
		new DistLocal<>($ -> new HashMap<>());
	



	// #### EVENT LISTENERS
	private static void tickAllServer(ServerTickEvent.Post event) {
		tickAll(ACTIVE_BLOCKS.getServer());
	}
	private static void tickAll(Map<InteractLocation, InteractionHandler> blocks) {
		for (InteractionHandler handler : blocks.values()) {
			if (handler.tick()) continue;
			clearActive(handler.location);
		}
	}
	private static void onPlayerTrack(PlayerEvent.StartTracking event) {
		if (!(event.getEntity() instanceof ServerPlayer trackingPlayer)) return;
		if (!(event.getTarget() instanceof ServerPlayer trackedPlayer)) return;
		if (trackingPlayer == trackedPlayer) return;
		InteractionHandler handler = getActive(trackedPlayer);
		if (handler == null) return;
		ModPackets.PACKETS.sendTo(trackingPlayer, new InteractionPacketS2C(trackedPlayer, handler));
	}
	private static void onPlayerUntrack(PlayerEvent.StopTracking event) {
		if (!(event.getEntity() instanceof ServerPlayer trackingPlayer)) return;
		if (!(event.getTarget() instanceof ServerPlayer trackedPlayer)) return;
		if (trackingPlayer == trackedPlayer) return;
		if (getActive(trackedPlayer) == null) return;
		ModPackets.PACKETS.sendTo(trackingPlayer, new InteractionPacketS2C(trackedPlayer));
	}
	private static void onLevelUnload(LevelEvent.Unload event) {
		var players = ACTIVE_PLAYERS.get(event.getLevel());
		for (var iter = ACTIVE_BLOCKS.get(event.getLevel()).entrySet().iterator(); iter.hasNext();) {
			Entry<InteractLocation, InteractionHandler> entry = iter.next();
			if (entry.getKey().world != event.getLevel()) continue;
			entry.getValue().onStop(null);
			for (Player player : entry.getValue().players)
				players.remove(player);
			iter.remove();
		}
		if (ACTIVE_LOCAL != null)
			ACTIVE_LOCAL.onStop(null);
		ACTIVE_LOCAL = null;
	}
	// #### CLIENT
	@OnlyIn(Dist.CLIENT)
	private static void tickAllClient(ClientTickEvent.Post event) {
		if (ACTIVE_LOCAL != null && (Minecraft.getInstance().screen != null || !ACTIVE_LOCAL.tick()))
			clearActiveLocal();
		tickAll(ACTIVE_BLOCKS.getClient());
	}
	@OnlyIn(Dist.CLIENT)
	public static boolean onMousePress(final int button, final int action, final int modifiers) {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.player == null || ACTIVE_LOCAL == null) return false;
		if (!mc.player.isSpectator() && mc.screen == null
			&& !ACTIVE_LOCAL.checkRelease(button, action, modifiers)) return false;
		clearActiveLocal();
		// sometimes minecraft can view keybinds as active even after a release event
		((KeyMappingMixin)Minecraft.getInstance().options.keyUse).invokeRelease();
		return false;
	}
	@OnlyIn(Dist.CLIENT)
	private static void onServerDisconnect(GameShuttingDownEvent event) {
		Minecraft.getInstance(); ///TODO: Remove
		var blocks = ACTIVE_BLOCKS.getClient();
		for (var iter = blocks.entrySet().iterator(); iter.hasNext();) {
			Entry<InteractLocation, InteractionHandler> entry = iter.next();
			entry.getValue().onStop(null);
		}
		blocks.clear();
		ACTIVE_PLAYERS.getClient().clear();
		if (ACTIVE_LOCAL != null)
			ACTIVE_LOCAL.onStop(null);
		ACTIVE_LOCAL = null;
	}



	// #### ACTIVE SETTERS & RESETTERS
	protected static void clearActive(InteractLocation location) {
		Thingymabobs.LOGGER.info("InteractionHandler.clearActiveServer");
		Thingymabobs.logStackTrace(8);
		InteractionHandler oldHandler = ACTIVE_BLOCKS.get(location.world).remove(location);
		if (oldHandler == null) return;
		oldHandler.onStop(null);
		for (Player player : oldHandler.players)
			ACTIVE_PLAYERS.get(location.world).remove(player);
	}
	/*protected static void setActive(Player player, String key, BlockPos pos) {
		var constructor = CONSTRUCTORS.get(key);
		if (constructor == null) return;
		setActive(player, constructor, pos);
	}*/
	protected static void setActive(Player player, InteractLocation location, Constructor constructor) {
		Thingymabobs.LOGGER.info("InteractionHandler.setActiveServer");
		Map<Player, InteractionHandler> players = ACTIVE_PLAYERS.get(player);
		Map<InteractLocation, InteractionHandler> blocks = ACTIVE_BLOCKS.get(player);

		clearActive(player); //Remove player from old handler

		Thingymabobs.LOGGER.info("  get block: "+location);
		InteractionHandler handler = blocks.get(location);
		if (handler == null) {
			handler = constructor.construct(player, location);
			Thingymabobs.LOGGER.info("    created: "+handler.hashCode());
			blocks.put(location, handler);
		} else
			Thingymabobs.LOGGER.info("    got: "+handler.hashCode());

		if (!handler.players.add(player)) return;
		Thingymabobs.LOGGER.info("  add player");
		handler.onStart(player);
		players.put(player, handler);
		{
			StringBuilder sb = new StringBuilder();
			sb.append("    Current players: [");
			boolean notFirst = false;
			for (var entry : ACTIVE_PLAYERS.get(player).entrySet()) {
				if (notFirst) sb.append(", ");
				notFirst = true;
				sb.append(entry.getKey()+"@"+player.hashCode()).append("=").append(entry.getValue().hashCode());
			}
			sb.append("]");
			Thingymabobs.LOGGER.info(sb.toString());
		}
	}
	protected static void clearActive(Player player) {
		Thingymabobs.LOGGER.info("InteractionHandler.clearActiveServer 2 player: "+player+"@"+player.hashCode());
		InteractionHandler oldHandler = ACTIVE_PLAYERS.get(player).remove(player);
		Thingymabobs.LOGGER.info("  ACTIVE_PLAYERS now #"+ACTIVE_PLAYERS.get(player).size());

		
		if (oldHandler == null) return;
		Thingymabobs.LOGGER.info("  remove player from handler: "+oldHandler.hashCode());
		if (oldHandler.players.remove(player)) {
			Thingymabobs.LOGGER.info("    OK: "+player);
			oldHandler.onStop(player);
		}

		Thingymabobs.LOGGER.info("  remove player");
		InteractionHandler tmp;


		if (oldHandler.players.size() > 0) return;
		Thingymabobs.LOGGER.info("  remove block at "+oldHandler.location);
		if ((tmp = ACTIVE_BLOCKS.get(player).remove(oldHandler.location)) != null)
			Thingymabobs.LOGGER.info("    OK: "+tmp);
		else {
			StringBuilder sb = new StringBuilder();
			sb.append("    FAIL: [");
			boolean notFirst = false;
			for (var entry : ACTIVE_BLOCKS.get(player).entrySet()) {
				if (notFirst) sb.append(", ");
				notFirst = true;
				sb.append(entry.getKey()).append("=").append(entry.getValue().hashCode());
			}
			sb.append("]");
			Thingymabobs.LOGGER.info(sb.toString());
		}
		Thingymabobs.LOGGER.info("  ACTIVE_BLOCKS now #"+ACTIVE_BLOCKS.get(player).size());
	}
	// #### CLIENT
	@OnlyIn(Dist.CLIENT)
	protected static void setActiveLocal(InteractionHandler handler) {
		Thingymabobs.LOGGER.info("InteractionHandler.setActiveClient");
		ACTIVE_LOCAL = handler;
		handler.onStart(null);
		((KeyMappingMixin)Minecraft.getInstance().options.keyUse).invokeRelease();
		ModPackets.PACKETS.send(new InteractionPacketC2S(handler));
	}
	@OnlyIn(Dist.CLIENT)
	protected static void clearActiveLocal() {
		Thingymabobs.LOGGER.info("InteractionHandler.clearActiveClient");
		if (ACTIVE_LOCAL == null) return;
		ACTIVE_LOCAL.onStop(null);
		ACTIVE_LOCAL = null;
		ModPackets.PACKETS.send(new InteractionPacketC2S());
	}


	// #### ACTIVE FETCHERS
	@OnlyIn(Dist.CLIENT)
	protected static @Nullable InteractionHandler getActiveLocal(InteractLocation location) {
		if (location.world != Minecraft.getInstance().level) return null;
		if (ACTIVE_LOCAL != null && ACTIVE_LOCAL.location.equals(location)) return ACTIVE_LOCAL;
		return ACTIVE_BLOCKS.getClient().get(location);
	}
	@OnlyIn(Dist.CLIENT)
	protected static @Nullable InteractionHandler getActiveLocal(Player player) {
		if (ACTIVE_LOCAL != null && Minecraft.getInstance().player == player) return ACTIVE_LOCAL;
		return ACTIVE_PLAYERS.getClient().get(player);
	}
	public static @Nullable InteractionHandler getActive(Player player) {
		return ACTIVE_PLAYERS.get(player).getOrDefault(player,
			ACTIVE_LOCAL != null && ACTIVE_LOCAL.hasPlayer(player)
			? ACTIVE_LOCAL : null);
	}
	public static @Nullable InteractionHandler getActive(InteractLocation location) {
		return ACTIVE_BLOCKS.get(location.world).getOrDefault(location, 
			ACTIVE_LOCAL != null && location.equals(ACTIVE_LOCAL.location)
			? ACTIVE_LOCAL : null);
	}





	// #### LOCAL VARIABLES & CONSTRUCTORS
	private final Set<Player> players;
	protected final InteractLocation location;

	protected InteractionHandler(Player player, InteractLocation location) {
		if (location.world == null) location.setWorld(player.level());
		if (player.level() != location.world)
			throw new IllegalArgumentException("Player must be in same level as InteractLocation.  Player is in "+player.level()+", expecting "+location.world);
		this.location = location;
		players = new HashSet<>();
	}


	// #### INTERFACE
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
		return players.stream().map((Player player) ->
				new Pair<>(player, getBlockHit(player)))
			.filter((hit) -> hit != null);
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
		Minecraft mc = Minecraft.getInstance();
		if (player == mc.player && mc.hitResult instanceof BlockHitResult hit) return hit;
		return null;
	}
}
