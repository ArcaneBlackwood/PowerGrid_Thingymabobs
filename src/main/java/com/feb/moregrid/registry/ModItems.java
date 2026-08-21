package com.feb.moregrid.registry;

import com.feb.moregrid.MoreGrid;
import com.feb.moregrid.blocks.PoisonousPotatoBatteryBlockItem;
import com.feb.moregrid.blocks.PotatoBatteryBlockItem;
import com.feb.moregrid.client.CustomModelItemRenderer;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.CustomRenderedItems;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MoreGrid.MOD_ID);
	public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
                DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MoreGrid.MOD_ID);

	private static final ResourceKey<CreativeModeTab> POWERGRID_TAB = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath("powergrid", "base")
	);

	public static final DeferredItem<Item> TRANSFORMER =
                ITEMS.registerSimpleItem("transformer", new Item.Properties());
	public static final DeferredItem<Item> SCR =
                ITEMS.registerSimpleItem("scr", new Item.Properties());
	public static final DeferredItem<Item> DRY_CELL =
                ITEMS.registerSimpleItem("dry_cell", new Item.Properties().stacksTo(16));
	public static final DeferredItem<Item> DIP_SWITCH =
                ITEMS.registerSimpleItem("dip_switch", new Item.Properties());
	public static final DeferredItem<Item> CERAMIC_CAPACITOR =
                ITEMS.registerSimpleItem("ceramic_capacitor", new Item.Properties());
	public static final DeferredItem<Item> SMALL_DIODE =
                ITEMS.registerSimpleItem("small_diode", new Item.Properties());
	public static final DeferredItem<Item> SMALL_RESISTOR =
                ITEMS.registerSimpleItem("small_resistor", new Item.Properties());
	public static final DeferredItem<Item> TALL_CONNECTOR =
                ITEMS.registerSimpleItem("tall_connector", new Item.Properties());
	public static final DeferredItem<Item> BUZZER =
                ITEMS.registerSimpleItem("buzzer", new Item.Properties());
	public static final DeferredItem<Item> VARIABLE_BUZZER =
                ITEMS.registerSimpleItem("variable_buzzer", new Item.Properties());
	public static final DeferredItem<Item> SHUNT =
                ITEMS.registerSimpleItem("shunt", new Item.Properties());

	public static final DeferredItem<BlockItem> POWER_SHUNT =
                ITEMS.registerSimpleBlockItem("power_shunt", ModBlocks.POWER_SHUNT,
                        new Item.Properties());

	public static final DeferredItem<BlockItem> LV_SWITCH_DPDT =
                ITEMS.registerSimpleBlockItem("lv_switch_dpdt", ModBlocks.LV_SWITCH_DPDT,
                        new Item.Properties());
	public static final DeferredItem<BlockItem> LV_SWITCH_SPDT =
                ITEMS.registerSimpleBlockItem("lv_switch_spdt", ModBlocks.LV_SWITCH_SPDT,
                        new Item.Properties());
	public static final DeferredItem<BlockItem> LV_SWITCH_TPST =
                ITEMS.registerSimpleBlockItem("lv_switch_tpst", ModBlocks.LV_SWITCH_TPST,
                        new Item.Properties());
	public static final DeferredItem<BlockItem> LV_SWITCH_DPST =
                ITEMS.registerSimpleBlockItem("lv_switch_dpst", ModBlocks.LV_SWITCH_DPST,
                        new Item.Properties());

	public static final DeferredItem<BlockItem> MV_SWITCH_DPDT =
                ITEMS.registerSimpleBlockItem("mv_switch_dpdt", ModBlocks.MV_SWITCH_DPDT,
                        new Item.Properties());
	public static final DeferredItem<BlockItem> MV_SWITCH_SPDT =
                ITEMS.registerSimpleBlockItem("mv_switch_spdt", ModBlocks.MV_SWITCH_SPDT,
                        new Item.Properties());
	public static final DeferredItem<BlockItem> MV_SWITCH_TPST =
                ITEMS.registerSimpleBlockItem("mv_switch_tpst", ModBlocks.MV_SWITCH_TPST,
                        new Item.Properties());
	public static final DeferredItem<BlockItem> MV_SWITCH_DPST =
                ITEMS.registerSimpleBlockItem("mv_switch_dpst", ModBlocks.MV_SWITCH_DPST,
                        new Item.Properties());

	public static final DeferredItem<BlockItem> POISONOUS_POTATO_BATTERY =
                ITEMS.registerSimpleBlockItem("poisonous_potato_battery", ModBlocks.POISONOUS_POTATO_BATTERY,
                        new Item.Properties());
	public static final DeferredItem<BlockItem> POTATO_BATTERY_ARRAY =
                ITEMS.registerSimpleBlockItem("potato_battery_array", ModBlocks.POTATO_BATTERY_ARRAY,
                        new Item.Properties());
	public static final DeferredItem<BlockItem> POISONOUS_POTATO_BATTERY_ARRAY =
                ITEMS.registerSimpleBlockItem("poisonous_potato_battery_array", ModBlocks.POISONOUS_POTATO_BATTERY_ARRAY,
                        new Item.Properties());
	public static final DeferredItem<BlockItem> POTATO_BATTERY_BLOCK =
		ITEMS.registerItem("potato_battery_block", PotatoBatteryBlockItem.CONSTRUCTOR,
                        new Item.Properties());
	public static final DeferredItem<BlockItem> POISONOUS_POTATO_BATTERY_BLOCK =
		ITEMS.registerItem("poisonous_potato_battery_block", PoisonousPotatoBatteryBlockItem.CONSTRUCTOR,
                        new Item.Properties());


	public static final DeferredItem<?>[] ALL_ITEMS = {
		TRANSFORMER, SCR, DRY_CELL, DIP_SWITCH, BUZZER, VARIABLE_BUZZER, SHUNT,
		POWER_SHUNT,
		CERAMIC_CAPACITOR, SMALL_DIODE, SMALL_RESISTOR, TALL_CONNECTOR,
		LV_SWITCH_DPDT, LV_SWITCH_SPDT, LV_SWITCH_TPST, LV_SWITCH_DPST,
		MV_SWITCH_DPDT, MV_SWITCH_SPDT, MV_SWITCH_TPST, MV_SWITCH_DPST,
		POISONOUS_POTATO_BATTERY, POTATO_BATTERY_ARRAY, POISONOUS_POTATO_BATTERY_ARRAY, POTATO_BATTERY_BLOCK, POISONOUS_POTATO_BATTERY_BLOCK
	};
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
                CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.moregrid"))
                        .withTabsAfter(POWERGRID_TAB)
                        .icon(() -> TRANSFORMER.get().getDefaultInstance())
                        .displayItems((parameters, output) -> {
                                for (var item : ALL_ITEMS) output.accept(item.get());
                        })
                        .build());

	private ModItems() { }


	public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		registerItemRenderer(event, new CustomModelItemRenderer(),
			POTATO_BATTERY_ARRAY.get(),
			POISONOUS_POTATO_BATTERY_ARRAY.get(),
			POTATO_BATTERY_BLOCK.get(),
			POISONOUS_POTATO_BATTERY_BLOCK.get(),
			POWER_SHUNT.get());
	}


	public static void registerItemRenderer(
		RegisterClientExtensionsEvent event, CustomRenderedItemModelRenderer renderer, Item... items
	) {
		for (int i = 1; i < items.length; i++)
                        CustomRenderedItems.register(items[i]);
		event.registerItem(
			SimpleCustomRenderer.create(items[0], renderer),
			items
		);
	}
}
