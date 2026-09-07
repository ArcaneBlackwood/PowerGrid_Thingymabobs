package dev.thingymabobs.component.trancievers;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.IComponentGoggleInformation;
import org.patryk3211.powergrid.circuits.components.IInteractableComponent;
import org.patryk3211.powergrid.circuits.components.IRenderedComponent;
import org.patryk3211.powergrid.circuits.components.MirrorableComponent;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.ConstantProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Unit;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.mixin.ISynchronizedComponent;
import dev.thingymabobs.registry.ModLang;
import com.google.common.collect.ImmutableCollection;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.render.RenderTypes;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
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
	public static final float SLOT_SIZE = 2.5f/16f;
	public static final AABB SLOT1 = AABB.ofSize(new Vec3(5.5f,3.1f,1.5f).scale(1/16f), SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
	public static final AABB SLOT2 = AABB.ofSize(new Vec3(2.5f,3.1f,1.5f).scale(1/16f), SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

	public static final float POWER_WATTS = 10f;
	public static final float POWER_WATTS_MIN = POWER_WATTS / 2f;
	public static final float POWER_VOLTAGE = 48f;
	public static final float POWER_RESISTANCE = POWER_VOLTAGE*POWER_VOLTAGE/POWER_WATTS_MIN;
	public static final float POWER_CURRENT = POWER_WATTS_MIN / POWER_VOLTAGE;
	
	public static final float SIGNAL_WATTS = 0.1f;
	public static final float TRANSMIT_RESISTANCE = 5_000f;
	public static final float TRANSMIT_CURRENT_FULL = Mth.sqrt(SIGNAL_WATTS / TRANSMIT_RESISTANCE);


	public static final ConstantProperty PROP_POWER_VOLTAGE = new ConstantProperty(Thingymabobs.MOD_ID, "tranciever.power_voltage",
		Unit.VOLTAGE.formatWithPrefixes(POWER_VOLTAGE).component());
	public static final ConstantProperty PROP_POWER_MIN = new ConstantProperty(Thingymabobs.MOD_ID, "tranciever.power_min",
		Unit.POWER.formatWithPrefixes(POWER_WATTS_MIN).component());
	public static final ConstantProperty PROP_TOTAL_POWER_MAX = new ConstantProperty(Thingymabobs.MOD_ID, "tranciever.total_power_max",
		Unit.POWER.formatWithPrefixes(POWER_WATTS + SIGNAL_WATTS).component());
	public static final ItemProperty PROP_SLOT1 = new ItemProperty(Thingymabobs.MOD_ID, "tranciever.slot1").hidden().cast();
	public static final ItemProperty PROP_SLOT2 = new ItemProperty(Thingymabobs.MOD_ID, "tranciever.slot2").hidden().cast();

	public static final ConstantProperty PROP_TRANSMIT_CURRENT_FULL = new ConstantProperty(Thingymabobs.MOD_ID, "tranciever.transmit_current_full",
		Unit.CURRENT.formatWithPrefixes(TRANSMIT_CURRENT_FULL).component());
	public static final ConstantProperty PROP_TRANSMIT_RESISTANCE = new ConstantProperty(Thingymabobs.MOD_ID, "tranciever.transmit_resistance",
		Unit.RESISTANCE.formatWithPrefixes(TRANSMIT_RESISTANCE).component());

	//Signal = 15
    public static final FloatProperty PROP_RECIEVE_RESISTANCE_MIN = 
		new FloatProperty(Thingymabobs.MOD_ID, "tranciever.recieve_resistance_min",  1_000f, 1_000f, 100_000f);
	//Signal = 0
    public static final FloatProperty PROP_RECIEVE_RESISTANCE_MAX =
		new FloatProperty(Thingymabobs.MOD_ID, "tranciever.recieve_resistance_max", 16_000f, 1_000f, 100_000f);



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
	public boolean tick(@NotNull PlacedComponent placed) {
		super.tick(placed);
		if (placed.customData == null) return false;
		if (placed.customData instanceof RenderState) return true;
		if (!(placed.customData instanceof State state)) return true;

		if (state.link == null) if (setupLink(placed, state)) return true;
		if (state.link == null || !(placed.customData instanceof ServerState serverState)) return state.link != null;

		if (isPowered(serverState)) updateState(placed, serverState, state.link.getRecieved());
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

	protected abstract float getSignalResistance(PlacedComponent placed, int signalValue);
	protected abstract boolean isTransmitter();
	protected abstract void updateState(PlacedComponent placed, ServerState state, int currentSignal);
	protected abstract PartialModel getRenderModel(PlacedComponent placed);

	
    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
		ServerState state = new ServerState();
		placed.customData = state;
        state.powerWire = new CleanupElectricWire(POWER_RESISTANCE, builder.terminalNode(1), builder.terminalNode(0));
        state.signalWire = new ElectricWire(getSignalResistance(placed, 0), builder.terminalNode(2), builder.terminalNode(3));
        builder.add(state.powerWire);
        builder.add(state.signalWire);
        thermals.builder()
			.setMaxPower(POWER_WATTS + SIGNAL_WATTS, 175)
			.setOverheatTemperature(180)
			.setThermalMass(0.2f)
			.addHeatSource(state.powerWire)
			.addHeatSource(state.signalWire);
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
    public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        if(placed.customData == null || !(placed.customData instanceof RenderState data)) return;
		if (data.signalValue < 1 && data.link == null) return;

		poseStack.pushPose();
		rotateY(poseStack, FOOTPRINT.getWidth(), FOOTPRINT.getHeight(), placed.get(ORIENTATION));

        if(data.signalValue > 0) {
			int intensity = data.signalValue * 16 + 15;
			CachedBuffers.partial(getRenderModel(placed), be.getBlockState())
				.disableDiffuse()
				.color(intensity, intensity, intensity, 255)
				.light(LightTexture.FULL_BRIGHT)
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

		if (data.signalValue == -1) {
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
		buffer.writeByte(isPowered(data) ? data.link.getRecieved() : -1);
		data.link.save(placed);
	}
	@Override
	public void readFromSync(PlacedComponent placed, FriendlyByteBuf buffer) {
        if(placed.customData == null || !(placed.customData instanceof RenderState data)) return;
		if (data.link == null) return;
		data.signalValue = buffer.readByte();
		data.link.load(placed);
	}


	protected static class State {
		public LinkComponentBehaviour link = null;
	}
	protected static class ServerState extends State {
		public CleanupElectricWire powerWire;
		public ElectricWire signalWire;
	}
	protected static class RenderState extends State {
		public int signalValue = -1;
	}
}
