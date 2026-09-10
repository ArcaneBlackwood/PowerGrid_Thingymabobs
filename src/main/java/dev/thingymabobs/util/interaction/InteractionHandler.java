package dev.thingymabobs.util.interaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import dev.thingymabobs.mixin.unit.KeyMappingMixin;
import dev.thingymabobs.registry.ModPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
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
	private static InteractionHandler ACTIVE_LOCAL = null;
	/// Can contain duplicate handlers
	private static Map<Player, InteractionHandler> ACTIVE_PLAYERS = new HashMap<>();
	private static Map<LevelBlock, InteractionHandler> ACTIVE_BLOCKS = new HashMap<>();
	protected static Map<String, Constructor> CONSTRUCTORS = new HashMap<>();
	protected static interface Constructor {
		public InteractionHandler construct(Player player, BlockPos pos);
	}
	
	// #### REGISTER & CONSTRUCTORS
	public static void register(IEventBus modBus) {
		CONSTRUCTORS.put(InteractionHold.KEY, InteractionHold::new);
		NeoForge.EVENT_BUS.addListener(InteractionHandler::tickAll);
		NeoForge.EVENT_BUS.addListener(InteractionHandler::onPlayerTrack);
		NeoForge.EVENT_BUS.addListener(InteractionHandler::onPlayerUntrack);
		NeoForge.EVENT_BUS.addListener(InteractionHandler::onLevelUnload);
		NeoForge.EVENT_BUS.addListener(InteractionHandler::onServerDisconnect);
	}
	private static void tickAll(ServerTickEvent.Post event) {
		for (InteractionHandler handler : ACTIVE_BLOCKS.values()) {
			if (handler.tick()) continue;
			clearActive(handler.world, handler.pos);
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
		for (var iter = ACTIVE_BLOCKS.entrySet().iterator(); iter.hasNext();) {
			Entry<LevelBlock, InteractionHandler> entry = iter.next();
			if (entry.getKey().world != event.getLevel()) continue;
			entry.getValue().onStop(null);
			for (Player player : entry.getValue().players)
				ACTIVE_PLAYERS.remove(player);
			iter.remove();
		}
		if (ACTIVE_LOCAL != null)
			ACTIVE_LOCAL.onStop(null);
		ACTIVE_LOCAL = null;
	}
	private static void onServerDisconnect(GameShuttingDownEvent event) {
		for (var iter = ACTIVE_BLOCKS.entrySet().iterator(); iter.hasNext();) {
			Entry<LevelBlock, InteractionHandler> entry = iter.next();
			entry.getValue().onStop(null);
		}
		ACTIVE_BLOCKS.clear();
		ACTIVE_PLAYERS.clear();
		if (ACTIVE_LOCAL != null)
			ACTIVE_LOCAL.onStop(null);
		ACTIVE_LOCAL = null;
	}

	@OnlyIn(Dist.CLIENT)
	public static void registerClient(IEventBus modBus) {
		NeoForge.EVENT_BUS.addListener(InteractionHandler::tickAllClient);
	}
	@OnlyIn(Dist.CLIENT)
	private static void tickAllClient(ClientTickEvent.Post event) {
		if (ACTIVE_LOCAL != null && (Minecraft.getInstance().screen != null || !ACTIVE_LOCAL.tick()))
			clearActiveLocal();
		tickAll(null);
	}
	@OnlyIn(Dist.CLIENT)
	public static boolean onMousePress(final int button, final int action, final int modifiers) {
		//Thingymabobs.LOGGER.info("InteractionHandler.onMousePress "+button+", "+action);
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.player == null || ACTIVE_LOCAL == null) return false;
		//Thingymabobs.LOGGER.info("InteractionHandler.onMousePress 2 "+(!mc.player.isSpectator() && mc.screen == null));
		if (!mc.player.isSpectator() && mc.screen == null
			&& !ACTIVE_LOCAL.checkRelease(button, action, modifiers)) return false;
		//Thingymabobs.LOGGER.info("InteractionHandler.onMousePress 3");
		clearActiveLocal();
		// sometimes minecraft can view keybinds as active even after a release event
		((KeyMappingMixin)Minecraft.getInstance().options.keyUse).invokeRelease();
		return false;
	}




	// #### ACTIVE SETTERS & RESETTERS
	@OnlyIn(Dist.CLIENT)
	protected static void setActiveLocal(InteractionHandler handler) {
		//Thingymabobs.LOGGER.info("InteractionHandler.setActiveClient");
		ACTIVE_LOCAL = handler;
		handler.onStart(null);
		((KeyMappingMixin)Minecraft.getInstance().options.keyUse).invokeRelease();
		ModPackets.PACKETS.send(new InteractionPacketC2S(handler));
	}
	@OnlyIn(Dist.CLIENT)
	protected static void clearActiveLocal() {
		//Thingymabobs.LOGGER.info("InteractionHandler.clearActiveClient");
		if (ACTIVE_LOCAL == null) return;
		ACTIVE_LOCAL.onStop(null);
		ACTIVE_LOCAL = null;
		ModPackets.PACKETS.send(new InteractionPacketC2S());
	}
	// #### COMMON
	protected static void clearActive(Level world, BlockPos pos) {
		//Thingymabobs.LOGGER.info("InteractionHandler.clearActiveServer");
		InteractionHandler oldHandler = ACTIVE_BLOCKS.remove(new LevelBlock(world, pos));
		if (oldHandler == null) return;
		oldHandler.onStop(null);
		for (Player player : oldHandler.players)
			ACTIVE_PLAYERS.remove(player);
	}
	protected static void setActive(Player player, String key, BlockPos pos) {
		//.info("InteractionHandler.setActiveServer");
		InteractionHandler handler = ACTIVE_PLAYERS.get(player);
		LevelBlock location = new LevelBlock(player.level(), pos);
		if (handler != null) {
			if (handler.players.remove(player));
				handler.onStop(player);
			if (handler.players.size() == 0)
				ACTIVE_BLOCKS.remove(location);
		} else {
			var constructor = CONSTRUCTORS.get(key);
			if (constructor == null) return;
			handler = constructor.construct(player, pos);
			ACTIVE_PLAYERS.put(player, handler);
		}
		boolean initializing = handler.players.size() == 0;
		if (!handler.players.add(player)) return;
		handler.onStart(player);
		if (initializing)
			ACTIVE_BLOCKS.put(location, handler);
	}
	protected static void clearActive(Player player) {
		//Thingymabobs.LOGGER.info("InteractionHandler.clearActiveServer 2");
		InteractionHandler oldHandler = ACTIVE_PLAYERS.remove(player);
		if (oldHandler == null) return;
		if (oldHandler.players.remove(player))
			oldHandler.onStop(player);
		if (oldHandler.players.size() > 0) return;
		ACTIVE_BLOCKS.remove(new LevelBlock(player.level(), oldHandler.pos));
	}


	// #### ACTIVE FETCHERS
	@OnlyIn(Dist.CLIENT)
	protected static @Nullable InteractionHandler getActiveLocal(Level world, BlockPos pos) {
		if (world != Minecraft.getInstance().level) return null;
		if (ACTIVE_LOCAL != null && ACTIVE_LOCAL.pos.equals(pos)) return ACTIVE_LOCAL;
		return ACTIVE_BLOCKS.get(new LevelBlock(world, pos));
	}
	@OnlyIn(Dist.CLIENT)
	protected static @Nullable InteractionHandler getActiveLocal(Player player) {
		if (ACTIVE_LOCAL != null && Minecraft.getInstance().player == player) return ACTIVE_LOCAL;
		return ACTIVE_PLAYERS.get(player);
	}
	public static @Nullable InteractionHandler getActive(Player player) {
		return ACTIVE_PLAYERS.getOrDefault(player,
			ACTIVE_LOCAL != null && ACTIVE_LOCAL.hasPlayer(player)
			? ACTIVE_LOCAL : null);
	}
	public static @Nullable InteractionHandler getActive(Level world, BlockPos pos) {
		return ACTIVE_BLOCKS.getOrDefault(new LevelBlock(world, pos), 
			ACTIVE_LOCAL != null && ACTIVE_LOCAL.pos == pos && ACTIVE_LOCAL.world == world
			? ACTIVE_LOCAL : null);
	}





	// #### LOCAL VARIABLES & CONSTRUCTORS
	private final Set<Player> players;
	protected final Level world;
	protected final BlockPos pos;

	protected InteractionHandler(Player player, BlockPos pos) {
		world = player.level();
		this.pos = pos;
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
	public static BlockHitResult getBlockHit(Player player) {
		if (player.level().isClientSide && player == Minecraft.getInstance().player) {
			BlockHitResult hit = getBlockHitClient(player);
			if (hit != null) return hit;
		}
		if (!(player.pick(player.blockInteractionRange(), 0, false) instanceof BlockHitResult hit)) return null;
		return hit;
	}
	@OnlyIn(Dist.CLIENT)
	private static BlockHitResult getBlockHitClient(Player player) {
		Minecraft mc = Minecraft.getInstance();
		if (player == mc.player && mc.hitResult instanceof BlockHitResult hit) return hit;
		return null;
	}
	
	private static class LevelBlock {
		public final int hash;
		public final Level world;
		public final BlockPos pos;
		public LevelBlock(Level world, BlockPos pos) {
			hash = pos.hashCode() * world.dimension().hashCode();
			this.world = world;
			this.pos = pos;
		}
		@Override
		public int hashCode() {
			return hash;
		}
		@Override
		public boolean equals(Object obj) {
			return obj instanceof LevelBlock that && that.pos.equals(pos) && that.world.equals(world);
		}
	}
}
