package dev.thingymabobs.component.trancievers;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public class ItemProperty extends ComponentProperty<ItemStack> {
	public ItemProperty(String namespace, String name) {
		super(namespace, name);
	}

	@Override
	public ItemStack defaultValue() {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack parse(String item) throws RuntimeException {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack read(HolderLookup.Provider registries, @Nullable Tag tag) {
		if (tag == null || !(tag instanceof CompoundTag tagC)) return ItemStack.EMPTY;
		return ItemStack.parseOptional(registries, tagC);
	}

	@Override
	public String toString(ItemStack item) {
		return item.toString();
	}

	@Override
	public Tag write(HolderLookup.Provider registries, ItemStack item) {
		return item.saveOptional(registries);
	}
	
}
