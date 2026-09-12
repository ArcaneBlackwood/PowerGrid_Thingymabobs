package dev.thingymabobs.blocks.Zoey.PlasmaGlobe;

import java.util.List;

import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.ISocketElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Power;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.utility.Unit;
import com.simibubi.create.foundation.block.IBE;
import dev.thingymabobs.registry.ModBlockEntities;
import dev.thingymabobs.registry.ModLang;
import dev.thingymabobs.util.interaction.InteractionHold;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PlasmaGlobe extends HorizontalElectricBlock implements IBE<PlasmaGlobeEntity>, ISocketElectric, IHaveElectricProperties, InteractionHold.Capable {
	public static final IntegerProperty STATE = IntegerProperty.create("state", 0, 5);
	public static final int STATE_EMPTY = 0;
	public static final int STATE_OFF = 1;
	public static final int STATE_ON_LOW = 2;
	public static final int STATE_ON = 3;
	public static final int STATE_ON_HIGH = 4;
	public static final int STATE_BLOWN = 5;

	public static boolean hasFunctionalTransformer(int state) {
		return state != STATE_BLOWN && state != STATE_EMPTY;
	};

	public static boolean hasBulb(int state) {
		return state != STATE_EMPTY;
	};

	public static boolean isPowered(int state) {
		return state == STATE_ON || state == STATE_ON_HIGH;
	};

	// NEEDS TO BE REDONE
	private static final TerminalBoundingBox SOCKET_NORTH = new TerminalBoundingBox(IDecoratedTerminal.SOCKET, 6.5, 0.25, 10.5, 9.5, 3.25, 13.5).withOrigin(8f, 1.75f, 12.5f);
	private static final TerminalBoundingBox SOCKET_SOUTH = SOCKET_NORTH.rotateAroundY(180);
	private static final TerminalBoundingBox SOCKET_WEST = SOCKET_NORTH.rotateAroundY(-90);
	private static final TerminalBoundingBox SOCKET_EAST = SOCKET_NORTH.rotateAroundY(90);

	private static final VoxelShape SHAPE = box(3.5, 0, 3.5, 12.5, 10.5, 12.5);


	public PlasmaGlobe(Properties settings){
		super(settings.lightLevel(state -> switch(state.getValue(STATE)) {
			case STATE_ON_LOW ->4;
			case STATE_ON -> 5;
			case STATE_ON_HIGH -> 6;
			default -> 0;
		}));
		registerDefaultState(defaultBlockState().setValue(STATE, 0));
	};


	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(STATE);
	}
	@Override
	public void appendProperties(ItemStack stack, Player player, List<Component> tooltip) {
		Voltage.rated(PlasmaGlobeEntity.RATED_VOLTAGE, player, tooltip); 
		Power.rated(150, player, tooltip);
		ModLang.translate("tooltip.temperature.ideal").style(ChatFormatting.GRAY).addTo(tooltip);
		ModLang.builder()
			.add(Component.nullToEmpty(" ")).add(ModLang.number((double)PlasmaGlobeEntity.IDEAL_TEMPERATURE))
			.add(Component.nullToEmpty(" ")).add(Unit.TEMPERATURE.get()).style(ChatFormatting.BLUE).addTo(tooltip);
	}

	@Override
	public Class<PlasmaGlobeEntity> getBlockEntityClass() {
		return PlasmaGlobeEntity.class;
	}
	@Override
	public BlockEntityType<? extends PlasmaGlobeEntity> getBlockEntityType() {
		return ModBlockEntities.PLASMA_GLOBE.get();
	}
	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);

		if (level.isClientSide() && state.getBlock() != oldState.getBlock() || !state.equals(oldState)) {
			if (state.getValue(STATE) == STATE_BLOWN && oldState.getValue(STATE) != STATE_BLOWN) {
				getBlockEntity(level, pos).playBlowEffect();
			}
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if(!player.getMainHandItem().isEmpty())
			return InteractionResult.PASS;
		return onBlockEntityUse(level, pos, be -> {
			if (be.replaceTransformer(player, InteractionHand.MAIN_HAND, ItemStack.EMPTY, (float)hitResult.getLocation().y - pos.getY()))
				return InteractionResult.SUCCESS;
			return interactTry(state, level, pos, player, hitResult);
		});
	}
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if(hand != InteractionHand.MAIN_HAND)
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		return onBlockEntityUseItemOn(level, pos, be -> {
			if (be.replaceTransformer(player, hand, stack, (float)hitResult.getLocation().y - pos.getY() ))
				return ItemInteractionResult.SUCCESS;
			return interactTry(stack, state, level, pos, player, hand, hitResult);
		});
	}
	@Override
	public boolean interactTick(BlockState state, InteractionHold interact) {
		return true;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		var stacks = super.getDrops(state, builder);
		if (state.getValue(STATE) != STATE_EMPTY)
			stacks.add(ModdedItems.GROWTH_LAMP.asStack(1));
		return stacks;
	}

	@Override
	public ITerminalPlacement socket(BlockState state) {
		return switch (state.getValue(HORIZONTAL_FACING)) {
			case EAST -> SOCKET_EAST;
			case NORTH -> SOCKET_NORTH;
			case SOUTH -> SOCKET_SOUTH;
			case WEST -> SOCKET_WEST;
			default -> null;
		};
	}
};
