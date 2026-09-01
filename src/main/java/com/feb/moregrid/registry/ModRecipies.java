package com.feb.moregrid.registry;

import java.util.function.Supplier;
import com.feb.moregrid.MoreGrid;
import com.feb.moregrid.blocks.electricfurnace.ElectricFurnaceRecipe;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public enum ModRecipies {
	ELECTRIC_FURNACE(ElectricFurnaceRecipe.Factory::new);

	public static void register(IEventBus modEventBus) {
		Registers.SERIALIZER_REGISTER.register(modEventBus);
		Registers.TYPE_REGISTER.register(modEventBus);
		ElectricFurnaceRecipe.register(modEventBus);
	}


	public static class Registers {
		public static final DeferredRegister<RecipeSerializer<?>> SERIALIZER_REGISTER = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, MoreGrid.MOD_ID);
		public static final DeferredRegister<RecipeType<?>> TYPE_REGISTER = DeferredRegister.create(Registries.RECIPE_TYPE, MoreGrid.MOD_ID);
	}
	public final ResourceLocation id;
	public final Supplier<RecipeSerializer<?>> serializerSupplier;
	private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializerObject;
	private final DeferredHolder<RecipeType<?>, RecipeType<?>> typeObject;
	private final Supplier<RecipeType<?>> type;

	private <T extends RecipeInput, R extends ARecipe<T>> ModRecipies(Supplier<IRecipeFactory<T, R>> newFactory) {
		String name = ModLang.asId(name());
		id = MoreGrid.asResource(name);
		IRecipeFactory<T, R> factory = newFactory.get();
		this.serializerSupplier = () -> new Serializer<>(factory);
		serializerObject = Registers.SERIALIZER_REGISTER.register(name, serializerSupplier);
		typeObject = Registers.TYPE_REGISTER.register(name, () -> RecipeType.simple(id));
		type = typeObject;
	}
	public RecipeType<?> getType() {
		return type.get();
	}



	public static abstract class ARecipe<T extends RecipeInput> implements Recipe<RecipeInput> {
		private ModRecipies recipeType;
		public final T params;

		public ARecipe(ModRecipies recipeType, T params) {
			this.params = params;
			this.recipeType = recipeType;
		}

		@Override
		public ItemStack assemble(RecipeInput arg0, HolderLookup.Provider provider) {
			return getResultItem(provider);
		}
		@Override
		public boolean canCraftInDimensions(int arg0, int arg1) {
			return true;
		}
		@Override
		public RecipeType<?> getType() {
			return recipeType.type.get();
		}
		@Override
		public RecipeSerializer<?> getSerializer() {
			return recipeType.serializerObject.get();
		}
		@Override
		public boolean isSpecial() {
			return true;
		}
		@Override
		public String getGroup() {
			return "processing";
		}
	}


	public static class Serializer<T extends RecipeInput, R extends ARecipe<T>> implements RecipeSerializer<R> {
		private final MapCodec<R> codec;
		private final StreamCodec<RegistryFriendlyByteBuf, R> steamCodec;

		public Serializer(IRecipeFactory<T, R> factory) {
			codec = factory.codec().xmap(factory::create, recipe -> recipe.params);
			steamCodec = factory.streamCodec().map(factory::create, recipe -> recipe.params);
		}
		@Override
		public MapCodec<R> codec() {
			return codec;
		}
		@Override
		public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
			return steamCodec;
		}
	}
	public static interface IRecipeConstructor {
		public ARecipe<?> construct(RecipeInput properties);
	}
	public static interface IRecipeFactory<T extends RecipeInput, R extends ARecipe<T>> {
		public MapCodec<T> codec();
		public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();
		public R create(T props);
	}
}
