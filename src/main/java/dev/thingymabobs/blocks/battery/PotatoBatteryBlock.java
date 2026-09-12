package dev.thingymabobs.blocks.battery;

import dev.thingymabobs.client.CustomModelItemRenderer;
import dev.thingymabobs.registry.ModBlockEntities;
import dev.thingymabobs.registry.ModDataComponents;
import dev.thingymabobs.registry.ModModels;
import com.mojang.datafixers.util.Unit;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.battery.AbstractBatteryBlock;
import org.patryk3211.powergrid.electricity.battery.BatterySpec;
import org.patryk3211.powergrid.electricity.battery.CustomConnectivityHandler;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.electricity.redstoneconverter.IRedstoneConverterBehaviour;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;
import org.patryk3211.powergrid.utility.Lang;
import java.util.List;

@MethodsReturnNonnullByDefault
public class PotatoBatteryBlock extends AbstractBatteryBlock<PotatoBatteryBlockEntity> implements IAcceptConnector, IHaveElectricProperties, IRedstoneConverterBehaviour, CustomModelItemRenderer.Provider, IPotatoBattery {

	public static final BooleanProperty BAKED = APotatoBatteryArray.BAKED;
	
	public PotatoBatteryBlock(Properties settings) {
		super(settings);
		registerDefaultState(defaultBlockState().setValue(BAKED, false));
	}
	@Override
	public PartialModel getModel(ItemStack stack) {
		return stack.has(ModDataComponents.BAKED.get()) ? ModModels.PBB_MODEL_BAKED : ModModels.PBB_MODEL;
	}
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BAKED);
	}
	@Override
	public BatterySpec getSpec() {
		return PotatoBatteryBlockEntity.SPEC_POTATO;
	}
	@Override
	public Item getUsedItem() {
		return Items.BONE_MEAL;
	}
	@Override
	public Item getReplaceItem() {
		return Items.POTATO;
	}
	@Override
	public Item getBakedItem() {
		return Items.BAKED_POTATO;
	}
	

	@Override
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved) {
		if(oldState.getBlock() == state.getBlock())
			return;
		if(moved)
			return;
		// fabric: see comment in FluidTankItem
//		Consumer<FluidTankBlockEntity> consumer = FluidTankItem.IS_PLACING_NBT
//				? FluidTankBlockEntity::queueConnectivityUpdate
//				: FluidTankBlockEntity::updateConnectivity;
		withBlockEntityDo(world, pos, PotatoBatteryBlockEntity::queueConnectivityUpdate);
	}
	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
			var be = world.getBlockEntity(pos);
			if (!(be instanceof PotatoBatteryBlockEntity battery))
				return;
			var wires = GlobalElectricNetworks.getWorldNetworks(world).findConnectedWires(battery.getElectricBehaviour());
			super.onRemove(state, world, pos, newState, moved);
			CustomConnectivityHandler.splitMulti(battery);

			// Rewire all wires that still target the stale behaviour
			wires.forEach(TransmissionLinePart::refreshEndpointNodes);
		}
	}
	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		var stacks = super.getDrops(state, builder);
		var be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if(be instanceof PotatoBatteryBlockEntity battery) {
			for(var stack : stacks) {
				if(stack.is(this.asItem())) {
					stack.set(ModDataComponents.ENERGY, battery.getIndividualEnergy());
					if (state.getValue(BAKED).booleanValue())
						stack.set(ModDataComponents.BLOWN.get(), Unit.INSTANCE);
				}
			}
		}
		return stacks;
	}
	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
		super.setPlacedBy(world, pos, state, placer, itemStack);
		if (itemStack.has(ModDataComponents.ENERGY)) {
			double energy = itemStack.get(ModDataComponents.ENERGY);
			this.withBlockEntityDo(world, pos, (be) -> {
				be.setEnergy(energy);
			});
		}
	}
	@Override
	public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext ctx) {
		var state = super.getStateForPlacement(ctx);
		if(state == null) return null;
		state.setValue(BAKED, ctx.getItemInHand().has(ModDataComponents.BAKED.get()));
		return state;
	}

	@Override
	public Class<PotatoBatteryBlockEntity> getBlockEntityClass() {
		return PotatoBatteryBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PotatoBatteryBlockEntity> getBlockEntityType() {
		return ModBlockEntities.POTATO_BATTERY_BLOCK.get();
	}

	@Override
	public boolean isPolarized() {
		return true;
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		var be = getBlockEntity(level, pos);
		if(be == null)
			return 0;
		var controller = be.getControllerBE();
		double fill;
		if(controller == null) {
			fill = be.getEnergy() / be.getCapacity();
		} else {
			fill = controller.getEnergy() / controller.getCapacity();
		}
		return Mth.floor(fill * 14.0f) + (fill > 0.001 ? 1 : 0);
	}

	@Override
	public float getSignal(Level level, BlockState state, BlockPos pos, Direction face) {
		var be = getBlockEntity(level, pos);
		if(be == null)
			return 0;
		var controller = be.getControllerBE();
		double fill;
		if(controller == null) {
			fill = be.getEnergy() / be.getCapacity();
		} else {
			fill = controller.getEnergy() / controller.getCapacity();
		}
		return (float) fill;
	}

	@Override
	public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
		Voltage.max(PotatoBatteryBlockEntity.SPEC_POTATO.calculateVoltage(1), player, tooltip);
		Power.max(stack, player, tooltip);
		float charge;
		float maxCharge = getSpec().getMaxCharge();
		if(stack.has(ModDataComponents.ENERGY)) {
			charge = (float) (stack.get(ModDataComponents.ENERGY) / maxCharge);
		} else {
			charge = getSpec().getInitialCharge() / maxCharge;
		}
		Lang.translate("tooltip.charge.current")
				.style(ChatFormatting.GRAY).addTo(tooltip);
		Lang.builder()
				.add(Component.literal(" "))
				.add(Lang.numberConstant(charge * 100))
				.add(Component.literal("%"))
				.style(ChatFormatting.AQUA).addTo(tooltip);
		Lang.translate("tooltip.capacity")
				.style(ChatFormatting.GRAY)
				.addTo(tooltip);
		Lang.builder()
				.add(Component.literal(" "))
				.add(Lang.numberConstant(maxCharge / 3.6f))
				.add(Component.literal(" m"))
				.add(org.patryk3211.powergrid.utility.Unit.ENERGY.get())
				.style(ChatFormatting.GREEN)
				.addTo(tooltip);
	}

	public static boolean isBattery(BlockState state) {
		return state.getBlock() instanceof PotatoBatteryBlock;
	}
	
}
