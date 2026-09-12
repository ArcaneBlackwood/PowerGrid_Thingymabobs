package dev.thingymabobs.blocks.battery;

import net.minecraft.world.item.Item;

public interface IPotatoBattery {
	public Item getUsedItem();
	public Item getReplaceItem();
	public Item getBakedItem();
}
