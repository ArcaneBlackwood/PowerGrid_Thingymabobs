package com.feb.moregrid.registry;

import java.util.function.Supplier;

import com.feb.moregrid.MoreGrid;
import com.feb.moregrid.blocks.electricfurnace.ElectricFurnaceMenu;
import com.feb.moregrid.blocks.electricfurnace.ElectricFurnaceScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
	public static final DeferredRegister<MenuType<?>> MENUS =
		DeferredRegister.create(Registries.MENU, MoreGrid.MOD_ID);

	public static final Supplier<MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE = MENUS.register("electric_furnace", () -> 
		IMenuTypeExtension.create(ElectricFurnaceMenu::new));

	public static void register(IEventBus modBus) {
		MENUS.register(modBus);
	}
	public static void registerClient(IEventBus modBus) {
		modBus.addListener(ModMenus::registerScreens);
	}
	private static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
	}
}
