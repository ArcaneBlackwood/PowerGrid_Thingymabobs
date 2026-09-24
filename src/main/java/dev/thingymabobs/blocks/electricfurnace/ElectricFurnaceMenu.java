package dev.thingymabobs.blocks.electricfurnace;

import dev.thingymabobs.registry.ModMenus;
import com.simibubi.create.foundation.gui.menu.MenuBase;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ElectricFurnaceMenu extends MenuBase<ElectricFurnaceEntity> {
	public ElectricFurnaceMenu(int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
		super(ModMenus.ELECTRIC_FURNACE.get(), id, inv, extraData);
	}
	public ElectricFurnaceMenu(int id, Inventory inv, ElectricFurnaceEntity contentHolder) {
		super(ModMenus.ELECTRIC_FURNACE.get(), id, inv, contentHolder);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	protected ElectricFurnaceEntity createOnClient(RegistryFriendlyByteBuf extraData) {
		var world = net.minecraft.client.Minecraft.getInstance().level;
		var be = world.getBlockEntity(extraData.readBlockPos());
		if(be instanceof ElectricFurnaceEntity sbe) {
			sbe.readClient(extraData.readNbt(), extraData.registryAccess());
			return sbe;
		}
		return null;
	}

	@Override
	public boolean stillValid(Player p_39242_) {
		return !contentHolder.isRemoved();
	}
   
	@Override
	protected void initAndReadInventory(ElectricFurnaceEntity contentHolder) {
		contentHolder.onMenuAdded();
	}
	@Override
	protected void saveData(ElectricFurnaceEntity contentHolder) {
		contentHolder.onMenuRemoved();
	}

	@Override
	protected void addSlots() {
		{
			int slotTopStart = 98 - ElectricFurnaceEntity.SLOTS_INPUT * 9;
			for (int i = 0, j = 0; i < ElectricFurnaceEntity.SLOTS_INPUT * 18; i+=18, j++) {
				this.addSlot(new SlotItemHandler(contentHolder.inputInventory, j, slotTopStart + i, 18));
			}
		} {
			int slotTopStart = 98 - ElectricFurnaceEntity.SLOTS_OUTPUT * 9;
			for (int i = 0, j = 0; i < ElectricFurnaceEntity.SLOTS_OUTPUT * 18; i+=18, j++) {
				this.addSlot(new SlotItemHandler(contentHolder.outputInventory, j, slotTopStart + i, 54));
			}
		}
		addPlayerSlots(8, 86);
	}


	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = (Slot)this.slots.get(slotIndex);
		if (slot != null && slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();
			int slotCount = ElectricFurnaceEntity.SLOTS_INPUT + ElectricFurnaceEntity.SLOTS_OUTPUT;
			if (slotIndex < slotCount) {
				if (!this.moveItemStackTo(itemstack1, slotCount, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(itemstack1, 0, slotCount, false)) {
				return ItemStack.EMPTY;
			}

			if (itemstack1.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return itemstack;
	}
}
