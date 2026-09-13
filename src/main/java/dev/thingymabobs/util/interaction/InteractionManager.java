package dev.thingymabobs.util.interaction;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.mixin.unit.client.KeyMappingMixin;
import dev.thingymabobs.util.DistLocal;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class InteractionManager {

	
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

		NeoForge.EVENT_BUS.addListener(InteractionManager::tickAllServer);
		NeoForge.EVENT_BUS.addListener(InteractionManager::onPlayerTrack);
		NeoForge.EVENT_BUS.addListener(InteractionManager::onPlayerUntrack);
		NeoForge.EVENT_BUS.addListener(InteractionManager::onLevelUnload);
	}
	@OnlyIn(Dist.CLIENT)
	public static void registerClient(IEventBus modBus) {
		NeoForge.EVENT_BUS.addListener(InteractionManager::tickAllClient);
		NeoForge.EVENT_BUS.addListener(InteractionManager::onServerDisconnect);
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
		InteractionPacket.INSTANCE.sendToClient(trackedPlayer, handler);
	}
	private static void onPlayerUntrack(PlayerEvent.StopTracking event) {
		if (!(event.getEntity() instanceof ServerPlayer trackingPlayer)) return;
		if (!(event.getTarget() instanceof ServerPlayer trackedPlayer)) return;
		if (trackingPlayer == trackedPlayer) return;
		if (getActive(trackedPlayer) == null) return;
		InteractionPacket.INSTANCE.sendToClient(trackedPlayer);
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
		var mc = Minecraft.getInstance();
		if (mc == null || mc.player == null || ACTIVE_LOCAL == null) return false;
		if (!mc.player.isSpectator() && mc.screen == null
			&& !ACTIVE_LOCAL.checkRelease(button, action, modifiers)) return false;
		clearActiveLocal();
		// sometimes minecraft can view keybinds as active even after a release event
		((KeyMappingMixin)mc.options.keyUse).invokeRelease();
		return false;
	}
	@OnlyIn(Dist.CLIENT)
	private static void onServerDisconnect(GameShuttingDownEvent event) {
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
		Thingymabobs.logStackTrace(8);
		InteractionHandler oldHandler = ACTIVE_BLOCKS.get(location.world).remove(location);
		if (oldHandler == null) return;
		oldHandler.onStop(null);
		for (Player player : oldHandler.players)
			ACTIVE_PLAYERS.get(location.world).remove(player);
	}
	protected static void setActive(Player player, InteractLocation location, Constructor constructor) {
		Map<Player, InteractionHandler> players = ACTIVE_PLAYERS.get(player);
		Map<InteractLocation, InteractionHandler> blocks = ACTIVE_BLOCKS.get(player);

		clearActive(player); //Remove player from old handler

		InteractionHandler handler = blocks.get(location);
		if (handler == null) {
			handler = constructor.construct(player, location);
			blocks.put(location, handler);
		}
		
		if (!handler.players.add(player)) return;
		handler.onStart(player);
		players.put(player, handler);
	}
	protected static void clearActive(Player player) {
		InteractionHandler oldHandler = ACTIVE_PLAYERS.get(player).remove(player);

		if (oldHandler == null || !oldHandler.hasPlayer(player)) return;
		oldHandler.onStop(player);
		oldHandler.players.remove(player);

		if (oldHandler.players.size() > 0) return;
		ACTIVE_BLOCKS.get(player).remove(oldHandler.location);
	}
	// #### CLIENT
	@OnlyIn(Dist.CLIENT)
	protected static void setActiveLocal(InteractionHandler handler) {
		handler.players = null;
		ACTIVE_LOCAL = handler;
		handler.onStart(null);
		((KeyMappingMixin)Minecraft.getInstance().options.keyUse).invokeRelease();
		InteractionPacket.INSTANCE.sendToServer(handler);
	}
	@OnlyIn(Dist.CLIENT)
	protected static void clearActiveLocal() {
		if (ACTIVE_LOCAL == null) return;
		ACTIVE_LOCAL.onStop(null);
		ACTIVE_LOCAL = null;
		InteractionPacket.INSTANCE.sendToServer();
	}


	// #### ACTIVE FETCHERS
	/**
	 * Only use me if you need to explicitly get the local version.  Doesnt keep track of how many players are interacting with this.
	 * @deprecated Prefer to use {@link #getActive(InteractLocation)}
	 */
	@OnlyIn(Dist.CLIENT)
	@Deprecated(forRemoval = false)
	protected static @Nullable InteractionHandler getActiveLocal(InteractLocation location) {
		if (location.world != Minecraft.getInstance().level) return null;
		if (ACTIVE_LOCAL != null && ACTIVE_LOCAL.location.equals(location)) return ACTIVE_LOCAL;
		return ACTIVE_BLOCKS.getClient().get(location);
	}
	/**
	 * Only use me if you need to explicitly get the local version.  Doesnt keep track of how many players are interacting with this.
	 * @deprecated Prefer to use {@link #getActive(Player)}
	 */
	@OnlyIn(Dist.CLIENT)
	@Deprecated(forRemoval = false)
	protected static @Nullable InteractionHandler getActiveLocal(Player player) {
		if (ACTIVE_LOCAL != null && Minecraft.getInstance().player == player) return ACTIVE_LOCAL;
		return ACTIVE_PLAYERS.getClient().get(player);
	}
	public static @Nullable InteractionHandler getActive(InteractLocation location) {
		return ACTIVE_BLOCKS.get(location.world).getOrDefault(location, 
			ACTIVE_LOCAL != null && location.equals(ACTIVE_LOCAL.location)
			? ACTIVE_LOCAL : null);
	}
	public static @Nullable InteractionHandler getActive(Player player) {
		return ACTIVE_PLAYERS.get(player).getOrDefault(player,
			ACTIVE_LOCAL != null && ACTIVE_LOCAL.hasPlayer(player)
			? ACTIVE_LOCAL : null);
	}
}
