package dev.thingymabobs.registry;

import org.patryk3211.powergrid.forge.ElectricProperties;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.blocks.battery.PoisonousPotatoBatteryBlockItem;
import dev.thingymabobs.blocks.battery.PotatoBatteryBlockItem;
import dev.thingymabobs.client.CustomModelItemRenderer;
import dev.thingymabobs.client.TooltipProvider;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.foundation.item.TooltipModifier;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Thingymabobs.MOD_ID);
	public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
                DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Thingymabobs.MOD_ID);

	private static final ResourceKey<CreativeModeTab> POWERGRID_TAB = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath("powergrid", "base")
	);

	public static final DeferredItem<Item> TRANSFORMER =
		ITEMS.registerSimpleItem("transformer", new Item.Properties());
	public static final DeferredItem<Item> SCR =
		ITEMS.registerSimpleItem("scr", new Item.Properties());
	public static final DeferredItem<SequencedAssemblyItem> SCR_INCOMPLETE =
		ITEMS.registerItem("scr_incomplete", SequencedAssemblyItem::new, new Item.Properties());
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
	public static final DeferredItem<Item> SMALL_LIGHT_BULB =
		ITEMS.registerSimpleItem("small_bulb", new Item.Properties());
	public static final DeferredItem<Item> TALL_CONNECTOR =
		ITEMS.registerSimpleItem("tall_connector", new Item.Properties());
	public static final DeferredItem<Item> BUZZER =
		ITEMS.registerSimpleItem("buzzer", new Item.Properties());
	public static final DeferredItem<Item> VARIABLE_BUZZER =
		ITEMS.registerSimpleItem("variable_buzzer", new Item.Properties());
	public static final DeferredItem<Item> SHUNT =
		ITEMS.registerSimpleItem("shunt", new Item.Properties());
	public static final DeferredItem<Item> TRANSMITTER =
		ITEMS.registerSimpleItem("transmitter", new Item.Properties());
	public static final DeferredItem<Item> RECIEVER =
		ITEMS.registerSimpleItem("reciever", new Item.Properties());
	public static final DeferredItem<Item> DIRECTIONAL_RECIEVER =
		ITEMS.registerSimpleItem("directional_reciever", new Item.Properties());
	public static final DeferredItem<Item> DISTANCE_RECIEVER =
		ITEMS.registerSimpleItem("distance_reciever", new Item.Properties());
	public static final DeferredItem<Item> ACCELEROMETER =
		ITEMS.registerSimpleItem("accelerometer", new Item.Properties());

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

	public static final DeferredItem<BlockItem> POWER_SHUNT =
		ITEMS.registerSimpleBlockItem("power_shunt", ModBlocks.POWER_SHUNT,
			new Item.Properties());
	public static final DeferredItem<BlockItem> LAVA_LAMP =
		ITEMS.registerSimpleBlockItem("lava_lamp", ModBlocks.LAVA_LAMP,
			new Item.Properties());
	public static final DeferredItem<BlockItem> ELECTRIC_FURNACE =
		ITEMS.registerSimpleBlockItem("electric_furnace", ModBlocks.ELECTRIC_FURNACE,
			new Item.Properties());
	public static final DeferredItem<BlockItem> PLASMA_GLOBE =
		ITEMS.registerSimpleBlockItem("plasma_globe", ModBlocks.PLASMA_GLOBE,
			new Item.Properties());
	
	public static final DeferredItem<?>[] ALL_ITEMS = {
		TRANSFORMER, SCR, DRY_CELL, DIP_SWITCH, BUZZER, VARIABLE_BUZZER, SHUNT, TRANSMITTER, RECIEVER, DIRECTIONAL_RECIEVER, DISTANCE_RECIEVER, ACCELEROMETER,
		CERAMIC_CAPACITOR, SMALL_DIODE, SMALL_RESISTOR, SMALL_LIGHT_BULB, TALL_CONNECTOR,
		LV_SWITCH_DPDT, LV_SWITCH_SPDT, LV_SWITCH_TPST, LV_SWITCH_DPST,
		MV_SWITCH_DPDT, MV_SWITCH_SPDT, MV_SWITCH_TPST, MV_SWITCH_DPST,
		POISONOUS_POTATO_BATTERY, POTATO_BATTERY_ARRAY, POISONOUS_POTATO_BATTERY_ARRAY, POTATO_BATTERY_BLOCK, POISONOUS_POTATO_BATTERY_BLOCK,
		POWER_SHUNT, LAVA_LAMP, ELECTRIC_FURNACE, PLASMA_GLOBE
	};

	public static final DeferredItem<?>[] PROPERTY_ITEMS = {
		LV_SWITCH_DPDT, LV_SWITCH_SPDT, LV_SWITCH_TPST, LV_SWITCH_DPST,
		MV_SWITCH_DPDT, MV_SWITCH_SPDT, MV_SWITCH_TPST, MV_SWITCH_DPST,
		POISONOUS_POTATO_BATTERY, POTATO_BATTERY_ARRAY, POISONOUS_POTATO_BATTERY_ARRAY, POTATO_BATTERY_BLOCK, POISONOUS_POTATO_BATTERY_BLOCK,
		POWER_SHUNT, LAVA_LAMP, ELECTRIC_FURNACE, PLASMA_GLOBE
	};
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
		CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.thingymabobs"))
			.withTabsAfter(POWERGRID_TAB)
			.icon(() -> TRANSFORMER.get().getDefaultInstance())
			.displayItems((parameters, output) -> {
					for (var item : ALL_ITEMS) output.accept(item.get());
			})
			.build());


	private ModItems() { }
	public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        CREATIVE_TABS.register(modBus);
	}

	@OnlyIn(Dist.CLIENT)
	protected static TooltipProvider tooltipProvider;

	@OnlyIn(Dist.CLIENT)
	public static void registerClient(IEventBus modBus) {
		TooltipModifier.REGISTRY.registerProvider(tooltipProvider = new TooltipProvider());
        modBus.addListener(ModItems::registerClientExtensions);
	}
	@OnlyIn(Dist.CLIENT)
	private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		if (FMLLoader.getDist() == Dist.DEDICATED_SERVER) {
			throw new IllegalAccessError("Cannot access models on server!");
		}
		registerItemRenderer(event, new CustomModelItemRenderer(),
			POTATO_BATTERY_ARRAY.get(),
			POISONOUS_POTATO_BATTERY_ARRAY.get(),
			POTATO_BATTERY_BLOCK.get(),
			POISONOUS_POTATO_BATTERY_BLOCK.get(),
			POWER_SHUNT.get());
		for (var item : PROPERTY_ITEMS)
			tooltipProvider.items.put(item.get(), ElectricProperties.create(item.get()));
	}
	@OnlyIn(Dist.CLIENT)
	private static void registerItemRenderer(
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
