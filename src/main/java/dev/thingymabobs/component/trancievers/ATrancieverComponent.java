package dev.thingymabobs.component.trancievers;

import java.util.List;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.IComponentGoggleInformation;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.components.IRenderedComponent;
import org.patryk3211.powergrid.circuits.components.MirrorableComponent;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Unit;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.component.properties.DynamicFloatProperty;
import dev.thingymabobs.component.properties.LazyConstantProperty;
import dev.thingymabobs.config.properties.CProperties;
import dev.thingymabobs.config.properties.CProperties.ASubProp;
import dev.thingymabobs.config.properties.Thermal;
import dev.thingymabobs.mixin.ISynchronizedComponent;
import dev.thingymabobs.mixin.ThermalBuilderExt;
import dev.thingymabobs.registry.ModLang;
import com.google.common.collect.ImmutableCollection;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.render.RenderTypes;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class ATrancieverComponent extends MirrorableComponent implements IRenderedComponent, IComponentGoggleInformation, IInteractableComponent, ISynchronizedComponent {
	public static final ComponentFootprint FOOTPRINT = new ComponentFootprint.Builder(
				7,3, "component." + Thingymabobs.MOD_ID + ".tranciever", null)
			.addPad(5, 0, 0, "Power -", "-")
			.addPad(5, 2, 1, "Power +", "+")
			.addPad(2, 0, 2, "Signal", "S")
			.addPad(2, 2, 3, "Signal", "S")
			.withItem().withOutline().build();
	public static final ComponentFootprint FOOTPRINT_DIRECTIONAL = new ComponentFootprint.Builder(
				7,3, "component." + Thingymabobs.MOD_ID + ".tranciever", null)
			.addPad(5, 0, 0, "Power -", "-")
			.addPad(5, 2, 1, "Power +", "+")
			.addPad(2, 0, 2, "Signal", "S")
			.addPad(2, 2, 3, "Signal", "S")
			.withArrow()
			.withItem().withOutline().build();
	
	protected static CProperties.Prop CONFIG = null;
	protected static Config CONFIG_TRANS;
	public static float POWER_WATTS;
	public static float POWER_WATTS_MIN;
	public static float POWER_VOLTAGE;
	public static float SIGNAL_WATTS;
	public static float TRANSMIT_RESISTANCE;

	public static float POWER_RESISTANCE, POWER_CURRENT, TRANSMIT_CURRENT_FULL;
	public static void configUpdated(CProperties.Prop prop) {
		CONFIG = prop;
		CONFIG_TRANS = prop.get(Config.class, Config.KEY);
		POWER_WATTS = CONFIG.getThermal().getPower();
		POWER_WATTS_MIN = CONFIG_TRANS.powerMin.getF();
		POWER_VOLTAGE = CONFIG_TRANS.voltage.getF();
		SIGNAL_WATTS = CONFIG_TRANS.signalPower.getF();
		TRANSMIT_RESISTANCE = Math.max(CONFIG_TRANS.resTransmit.getF(), Thingymabobs.RESISTANCE_CLAMP);

		POWER_RESISTANCE = POWER_VOLTAGE*POWER_VOLTAGE/POWER_WATTS_MIN;
		POWER_CURRENT = POWER_WATTS_MIN / POWER_VOLTAGE;
		TRANSMIT_CURRENT_FULL = Mth.sqrt(SIGNAL_WATTS / TRANSMIT_RESISTANCE);

		PROP_POWER_VOLTAGE.markDirty();
		PROP_POWER_MIN.markDirty();
		PROP_TOTAL_POWER_MAX.markDirty();
		PROP_TRANSMIT_CURRENT_FULL.markDirty();
		PROP_TRANSMIT_RESISTANCE.markDirty();
		PROP_RECIEVE_RESISTANCE_MIN.markDirty();
		PROP_RECIEVE_RESISTANCE_MAX.markDirty();
	}
	

	public static final float SLOT_SIZE = 2.5f/16f;
	public static final AABB SLOT1 = AABB.ofSize(new Vec3(5.5f,3.1f,1.5f).scale(1/16f), SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
	public static final AABB SLOT2 = AABB.ofSize(new Vec3(2.5f,3.1f,1.5f).scale(1/16f), SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);


	//Generic properties
	public static final ItemProperty PROP_SLOT1 = new ItemProperty(Thingymabobs.MOD_ID, "tranciever.slot1").hidden().cast();
	public static final ItemProperty PROP_SLOT2 = new ItemProperty(Thingymabobs.MOD_ID, "tranciever.slot2").hidden().cast();
	public static final LazyConstantProperty PROP_POWER_VOLTAGE = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "tranciever.power_voltage",
		() -> Unit.VOLTAGE.formatWithPrefixes(POWER_VOLTAGE).string());
	public static final LazyConstantProperty PROP_POWER_MIN = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "tranciever.power_min",
		() -> Unit.POWER.formatWithPrefixes(POWER_WATTS_MIN).string());
	public static final LazyConstantProperty PROP_TOTAL_POWER_MAX = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "tranciever.total_power_max",
		() -> Unit.POWER.formatWithPrefixes(POWER_WATTS).string());

	//Transmitter properties
	public static final LazyConstantProperty PROP_TRANSMIT_CURRENT_FULL = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "tranciever.transmit_current_full",
		() -> Unit.CURRENT.formatWithPrefixes(TRANSMIT_CURRENT_FULL).string());
	public static final LazyConstantProperty PROP_TRANSMIT_RESISTANCE = new LazyConstantProperty(
		Thingymabobs.MOD_ID, "tranciever.transmit_resistance",
		() -> Unit.RESISTANCE.formatWithPrefixes(TRANSMIT_RESISTANCE).string());

	//Reciever properties
	protected static final Supplier<Float> RES_MIN_PROVIDER = () -> Math.max(CONFIG_TRANS.resMin.getF(), Thingymabobs.RESISTANCE_CLAMP);
	protected static final Supplier<Float> RES_MAX_PROVIDER = () -> Math.max(CONFIG_TRANS.resMax.getF(), Thingymabobs.RESISTANCE_CLAMP);
	//Signal = 15
	public static final DynamicFloatProperty PROP_RECIEVE_RESISTANCE_MIN = new DynamicFloatProperty(
		Thingymabobs.MOD_ID, "tranciever.recieve_resistance_min",
		() -> CONFIG_TRANS.resMinDefault.getF(), RES_MIN_PROVIDER, RES_MAX_PROVIDER).useMetrics();
	//Signal = 0
	public static final DynamicFloatProperty PROP_RECIEVE_RESISTANCE_MAX = new DynamicFloatProperty(
		Thingymabobs.MOD_ID, "tranciever.recieve_resistance_max",
		() -> CONFIG_TRANS.resMaxDefault.getF(), RES_MIN_PROVIDER, RES_MAX_PROVIDER).useMetrics();



	public ATrancieverComponent() {
		super(FOOTPRINT, FOOTPRINT.mirroredY());
	}
	protected ATrancieverComponent(ComponentFootprint footprint) {
		super(footprint, footprint.mirroredY());
	}
	@Override
	protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
		super.addProperties(properties);
		properties.add(PROP_SLOT1, PROP_SLOT2, PROP_POWER_VOLTAGE, PROP_POWER_MIN, PROP_TOTAL_POWER_MAX);
	}
	@Override
	public VoxelShape getShape(@NotNull PlacedComponent placed) {
		return IInteractableComponent.extrudedFootprint(placed, 3.0F / 16.0F);
	}
	@Override
	public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
		ServerState state = new ServerState();
		placed.customData = state;
		state.powerWire = new CleanupElectricWire(POWER_RESISTANCE, builder.terminalNode(1), builder.terminalNode(0));
		state.signalWire = new ElectricWire(getSignalResistance(placed, 0), builder.terminalNode(2), builder.terminalNode(3));
		builder.add(state.powerWire);
		builder.add(state.signalWire);
		ThermalBuilder thermal = CONFIG.getThermal().apply(thermals)
			.addHeatSource(state.powerWire)
			.addHeatSource(state.signalWire);
		((ThermalBuilderExt)thermal).withBuildCallback(
			(thermalUnit) -> {
				Thermal t = CONFIG.getThermal();
				state.overheatPercent = (thermalUnit.getTemperature() - t.getTemp())
					/ (t.getOverheat() - t.getTemp());
			});
	}
	protected boolean setupLink(PlacedComponent placed, State state) {
		Level world = placed.getWorld();
		if (world == null) return false;
		state.link = new LinkComponentBehaviour(isTransmitter(), SLOT1, SLOT2);
		BlockEntity blockEntity = world.getBlockEntity(placed.getPos());
		if (blockEntity == null || !(blockEntity instanceof SmartBlockEntity sBlockEntity)) return false;
		state.link.initialize(placed, sBlockEntity);
		state.link.load(placed);
		if (world.isClientSide()) {
			RenderState newState = new RenderState();
			newState.link = state.link;
			placed.customData = newState;
			return true;
		} else if (state instanceof ServerState serverState) {
			serverState.powerWire.onRemove(() -> {
				if (state.link == null || state.link.link == null) return;
				state.link.onRemove();
				serverState.powerWire.onRemove(null);
			});
			return false;
		}
		return false;
	}



	@Override
	public boolean tick(@NotNull PlacedComponent placed) {
		super.tick(placed);
		if (placed.customData == null) return false;
		if (placed.customData instanceof RenderState) return true;
		if (!(placed.customData instanceof State state)) return true;

		if (state.link == null) if (setupLink(placed, state)) return true;
		if (state.link == null || !(placed.customData instanceof ServerState serverState)) return state.link != null;

		if (isPowered(serverState))
			updateState(placed, serverState, state.link.getRecieved()
				* getOverheatReduction(serverState.overheatPercent));
		else serverState.signalWire.setResistance(getSignalResistance(placed, 0));
		return true;
	}
	public boolean isPowered(ServerState state) {
		return state.powerWire.current() >= POWER_CURRENT;
	}
	/** Replace the installed pack with a fresh Thingymabobs dry-cell item. */
	@Override
	public InteractionResult use(CircuitBoardBlockEntity be, PlacedComponent placed, Player player) {
		if (placed.customData == null || !(placed.customData instanceof State state)) return InteractionResult.PASS;
		if (state.link == null) return InteractionResult.PASS;

		if (state.link.interact(placed, player)) return InteractionResult.SUCCESS;

		return InteractionResult.PASS;
	}
	public void setTransmission(ServerState state, float signalValue) {
		if (state.link == null) return;
		state.link.setTransmission(Math.round(signalValue
			* getOverheatReduction(state.overheatPercent)));
	}

	protected abstract float getSignalResistance(PlacedComponent placed, float signalValue);
	protected abstract boolean isTransmitter();
	protected abstract void updateState(PlacedComponent placed, ServerState state, float currentSignal);
	protected abstract PartialModel getRenderModel(PlacedComponent placed);

	public static float getOverheatReduction(float overheat) {
		overheat = 1 - overheat;
		overheat = overheat * overheat;
		overheat = overheat * overheat;
		return overheat * overheat; //overheat ^ 8
	}

	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource, int light, int overlay) {
		if(placed.customData == null || !(placed.customData instanceof RenderState data)) return;
		if (data.signalValue < 1 && data.link == null) return;

		poseStack.pushPose();
		rotateY(poseStack, FOOTPRINT.getWidth(), FOOTPRINT.getHeight(), placed.get(ORIENTATION));

		if(data.signalValue > 0) {
			int intensity = Mth.floor(data.signalValue * 16 + 15);
			CachedBuffers.partial(getRenderModel(placed), be.getBlockState())
				.disableDiffuse()
				.color(intensity, intensity, intensity, 255)
				.light(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
				.renderInto(poseStack, bufferSource.getBuffer(RenderTypes.additive()));
		}

		if (data.link != null) data.link.render(placed, partialTicks, poseStack, bufferSource, light, overlay);

		poseStack.popPose();
	}
	public static void rotateY(PoseStack poseStack, int width, int height, Orientation rotation) {
		float centerX = width/32f, centerZ = height/32f;
		switch (rotation) {
			case UP:
				centerZ = centerX;
				break;
			case DOWN:
				centerX = centerZ;
				break;
			default:
				break;
		}
		poseStack.translate(centerX, 0, centerZ);
		poseStack.mulPose(Axis.YP.rotationDegrees(rotation.ordinal() * 90));
		poseStack.translate(-centerX, 0, -centerZ);
	}
	@Override
	public boolean addToGoggleTooltip(PlacedComponent placed, List<Component> tooltip, boolean isPlayerSneaking) {
		if (placed.has(LABEL)) {
			var label = placed.get(LABEL);
			if(label.isEmpty()) {
				ModLang.translate(isTransmitter() ? "gui.tranciever.info_transmitter" : "gui.tranciever.info_reciever").forGoggles(tooltip);
			} else {
				ModLang.text(label).forGoggles(tooltip);
			}
		} else {
			ModLang.translate(isTransmitter() ? "gui.tranciever.info_transmitter" : "gui.tranciever.info_reciever").forGoggles(tooltip);
		}
		if(placed.customData == null || !(placed.customData instanceof RenderState data)) return false;

		if (data.signalValue < 0.5f) {
			ModLang.translate("gui.tranciever.no_power")
				.style(ChatFormatting.RED)
				.forGoggles(tooltip);
		} else {
			ModLang.translate("gui.tranciever.signal_value", data.signalValue)
				.style(data.signalValue > 0 ? ChatFormatting.GREEN : ChatFormatting.GOLD)
				.forGoggles(tooltip);
		}
		return true;
	}

	
	@Override
	public void writeToSync(PlacedComponent placed, FriendlyByteBuf buffer) {
		if(placed.customData == null || !(placed.customData instanceof ServerState data)) return;
		if (data.link == null) return;
		buffer.writeByte(isPowered(data) ? Math.round(data.link.getRecieved() * 8f) : -1);
		data.link.save(placed);
	}
	@Override
	public void readFromSync(PlacedComponent placed, FriendlyByteBuf buffer) {
		if(placed.customData == null || !(placed.customData instanceof RenderState data)) return;
		if (data.link == null) return;
		data.signalValue = buffer.readByte();
		if (data.signalValue > 0) data.signalValue = data.signalValue / 8f;
		data.link.load(placed);
	}


	protected static class State {
		public LinkComponentBehaviour link = null;
	}
	protected static class ServerState extends State {
		public CleanupElectricWire powerWire;
		public ElectricWire signalWire;
		public float overheatPercent = 0;
	}
	protected static class RenderState extends State {
		public float signalValue = -1;
	}
	public static class Config extends ASubProp {
		public static final String KEY = "tc";
		public ConfigBase.ConfigFloat powerMin = null, voltage = null,
			resMax = null, resMin = null, resMaxDefault = null, resMinDefault = null, 
			resTransmit = null, signalPower = null;
		public float powerDef, powerMinDef, voltageDef,
			resMaxDef, resMinDef, resMaxDefaultDef, resMinDefaultDef, 
			resTransmitDef, signalPowerDef;

		public Config(float powerMin, float voltage,
			float resMax, float resMin, float resMaxDefault, float resMinDefault, 
			float resTransmit, float signalPower) {
			this.powerMinDef = powerMin;
			this.voltageDef = voltage;

			this.resMaxDef = resMax;
			this.resMinDef = resMin;
			this.resMaxDefaultDef = resMaxDefault;
			this.resMinDefaultDef = resMinDefault;

			this.resTransmitDef = resTransmit;
			this.signalPowerDef = signalPower;
		}

		@Override
		public Class<?> getType() {
			return Config.class;
		}
		@Override
		public void register(String id, CProperties.Builder builder) {
			voltage = builder.f(voltageDef, 0f, id+"_voltage",
				"Rated voltage for all trancievers");
			powerMin = builder.f(powerMinDef, 0f, id+"_power_min",
				"Minimum power for a tranciever to function");

			resMax = builder.f(resMaxDef, 0f, id+"_signal_recv_max",
				"Minimum and maximum settable resistance for the resistance accross signal pins for recievers.");
			resMin = builder.f(resMinDef, 0f, id+"_signal_recv_min");
			resMaxDefault = builder.f(resMaxDefaultDef, 0f, id+"_signal_recv_max_default",
				"Default recieved signal resistances for the recievers.");
			resMinDefault = builder.f(resMinDefaultDef, 0f, id+"_signal_recv_min_default");

			resTransmit = builder.f(resTransmitDef, 0f, id+"_signal_tran_resistance",
				"Resistance accross the signal pins for transmitters");
			signalPower = builder.f(signalPowerDef, 0f, id+"_signal_max_power",
				"The power through the signal pins for the transmitted signal to be at its maximum");
		}
	}
}
