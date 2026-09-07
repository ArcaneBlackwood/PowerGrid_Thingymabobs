package dev.thingymabobs.registry.capabilities;

import java.util.function.Supplier;

import dev.thingymabobs.registry.ModBlockEntities;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;

public class ItemCapability<B extends BlockEntity & ItemCapability.Provider> implements ICapability {
	Supplier<BlockEntityType<B>> blockEntity;
	public ItemCapability(
		Supplier<BlockEntityType<B>> blockEntity
	) {
		this.blockEntity = blockEntity;
	}
	@Override
	public void apply(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.ItemHandler.BLOCK,
				ModBlockEntities.ELECTRIC_FURNACE.get(),
				(be, context) -> be.getItemCapability(context)
		);
	}
	public static interface Provider {
		public IItemHandler getItemCapability(Direction direction);
	}
}