package com.feb.moregrid.blocks;

import com.feb.moregrid.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.battery.AbstractBatteryBlock;
import org.patryk3211.powergrid.electricity.battery.BatterySpec;
import org.patryk3211.powergrid.electricity.battery.CustomConnectivityHandler;
import org.patryk3211.powergrid.electricity.battery.SimpleBatterySpec;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.electricity.redstoneconverter.IRedstoneConverterBehaviour;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

@MethodsReturnNonnullByDefault
public class PoisonousPotatoBatteryBlock extends AbstractBatteryBlock<PotatoBatteryBlockEntity> implements IAcceptConnector, IHaveElectricProperties, IRedstoneConverterBehaviour {
    public static final BooleanProperty BAKED = APotatoBatteryArray.BAKED;
    
    public static final BatterySpec BATTERY_SPEC = new SimpleBatterySpec(
		26f,
		26f,
		e -> 1.3f * e + 1.7f,
		e -> (float) Math.exp(6.5f - 11f * e) + 25
    );

    public PoisonousPotatoBatteryBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(BAKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BAKED);
    }

    @Override
    public BatterySpec getSpec() {
        return BATTERY_SPEC;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved) {
        if(oldState.getBlock() == state.getBlock())
            return;
        if(moved)
            return;
        // fabric: see comment in FluidTankItem
//        Consumer<FluidTankBlockEntity> consumer = FluidTankItem.IS_PLACING_NBT
//                ? FluidTankBlockEntity::queueConnectivityUpdate
//                : FluidTankBlockEntity::updateConnectivity;
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onBlockEntityUse(level, pos, be -> {
            if(be instanceof PotatoBatteryBlockEntity block) {
                block = (PotatoBatteryBlockEntity)block.getControllerBE();
				if (player.isCreative() && player.isShiftKeyDown()) {
                	block.setEnergy(block.getCapacity());
                    level.setBlockAndUpdate(pos, state.setValue(BAKED, false));
                    if (!level.isClientSide) be.resetThermals();
					return InteractionResult.SUCCESS;
				}
            }
            return InteractionResult.FAIL;
        });
    }
	
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return onBlockEntityUseItemOn(level, pos, be -> {
            if(!(be instanceof PotatoBatteryBlockEntity block1))
                return ItemInteractionResult.FAIL;
            if(!(block1.getControllerBE() instanceof PotatoBatteryBlockEntity block))
                return ItemInteractionResult.FAIL;
            
            boolean hasItems = stack.is(Items.POISONOUS_POTATO) && stack.getCount() >= 8;
            if (hasItems || (player.isShiftKeyDown() && player.isCreative())) {
                if(!player.isCreative() || !player.isShiftKeyDown()) {
                    double usage = block.getEnergy() / block.getCapacity();
                    boolean baked = state.getValue(BAKED).booleanValue();
                    int usedPotatos = baked ? 8 : (int)(8.9999 - 8*usage);
                    int processedPotatos = (int)(8.4999 - 8*usage);
                    stack.shrink(usedPotatos);
                    ItemStack potatos = null;
                    if (!baked && processedPotatos > 0)
                        potatos = new ItemStack(Items.BONE_MEAL, processedPotatos);
                    if (potatos != null)
                        if (!player.addItem(potatos)) player.spawnAtLocation(potatos);
                }
                block.setEnergy(block.getCapacity());
                level.setBlockAndUpdate(pos, state.setValue(BAKED, false));
                if (!level.isClientSide) be.resetThermals();
                if(player.isCreative() && player.isShiftKeyDown())
                    return ItemInteractionResult.CONSUME;
                else
                    return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.FAIL;
        });
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var stacks = super.getDrops(state, builder);
        var be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if(be instanceof PotatoBatteryBlockEntity battery) {
            for(var stack : stacks) {
                if(stack.is(this.asItem())) {
                    var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    tag.putDouble("Energy", Math.floor(battery.getIndividualEnergy()));
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    break;
                }
            }
        }
        return stacks;
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
        Voltage.max(BATTERY_SPEC.calculateVoltage(1), player, tooltip);
        Power.max(stack, player, tooltip);
        float charge;
        float maxCharge = getSpec().getMaxCharge();
        if(!stack.has(DataComponents.CUSTOM_DATA) || !stack.get(DataComponents.CUSTOM_DATA).contains("Energy")) {
            charge = getSpec().getInitialCharge() / maxCharge;
        } else {
            charge = (float) (stack.get(DataComponents.CUSTOM_DATA).copyTag().getDouble("Energy") / maxCharge);
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
                .add(Lang.numberConstant(maxCharge / 3600))
                .add(Component.literal(" "))
                .add(Unit.ENERGY.get())
                .style(ChatFormatting.GREEN)
                .addTo(tooltip);
    }

    public static boolean isBattery(BlockState state) {
        return state.getBlock() instanceof PoisonousPotatoBatteryBlock;
    }
    
}
