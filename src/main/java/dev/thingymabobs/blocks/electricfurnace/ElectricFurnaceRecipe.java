package dev.thingymabobs.blocks.electricfurnace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.registry.ModRecipies;
import dev.thingymabobs.registry.ModRecipies.ARecipe;
import dev.thingymabobs.registry.ModRecipies.IRecipeFactory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public class ElectricFurnaceRecipe extends ARecipe<ElectricFurnaceRecipe.Parameters> {
    public static final float DEFAULT_TIME_PER_PROCESS = 10; //20x faster than furnace, speed is scaled by current temp / min temp
	public static final float VANILLA_PROCESS_SCALE = DEFAULT_TIME_PER_PROCESS / 200f;
    public static final float TEMP_SMOKE_MIN = ElectricFurnaceEntity.MAX_TEMPERATURE * 1 / 13;
    public static final float TEMP_SMOKE_BURN = ElectricFurnaceEntity.MAX_TEMPERATURE * 3 / 13;
    public static final float TEMP_SMOKE_MAX = ElectricFurnaceEntity.MAX_TEMPERATURE * 5 / 13;
    public static final float TEMP_SMELT_MIN = ElectricFurnaceEntity.MAX_TEMPERATURE * 10 / 13;

	public static final Map<Item, Burnt> ITEM_BURN_TEMPS = new HashMap<>();
	public ElectricFurnaceRecipe(Parameters params) {
		this(ModRecipies.ELECTRIC_FURNACE, params);
	}
	protected ElectricFurnaceRecipe(ModRecipies recipeType, Parameters params) {
		super(recipeType, params);
	}
	public static void register(IEventBus modEventBus) {
		NeoForge.EVENT_BUS.addListener(ElectricFurnaceRecipe::onServerStarted);
	}
	public static void onServerStarted(ServerStartedEvent event) {
		ServerLevel world = event.getServer().overworld();
		ITEM_BURN_TEMPS.clear();

		List<RecipeHolder<? extends AbstractCookingRecipe>> listVanilla = getAllVanilla(world);
		if (listVanilla == null) return;
		for (RecipeHolder<? extends AbstractCookingRecipe> recipeHold : listVanilla) {
			AbstractCookingRecipe recipe = recipeHold.value();
			if (getIsSmoking(recipe)) continue;
			ITEM_BURN_TEMPS.put(recipe.getResultItem(world.registryAccess()).getItem(), 
				new Burnt(ElectricFurnaceEntity.OVERHEAT_TEMPERATURE, Parameters.BURNT_OUTPUT_DEFAULT));
		}

		for (RecipeHolder<? extends AbstractCookingRecipe> recipeHold : listVanilla) {
			AbstractCookingRecipe recipe = recipeHold.value();
			if (!getIsSmoking(recipe)) continue;
			ITEM_BURN_TEMPS.put(recipe.getResultItem(world.registryAccess()).getItem(), 
				new Burnt(TEMP_SMOKE_BURN, Parameters.BURNT_OUTPUT_DEFAULT));
		}

		List<RecipeHolder<ElectricFurnaceRecipe>> list = getAll(world);
		if (list == null) return;
		for (RecipeHolder<ElectricFurnaceRecipe> recipeHold : list) {
			Parameters params = recipeHold.value().params;
			float burnTemp = params.temps.burnTemp;
			for (ProcessingOutput output : params.output)
				ITEM_BURN_TEMPS.put(output.getStack().getItem(), 
					new Burnt(burnTemp, params.burntOutput));
		}
	}


	//Fetch and find recipies by using ElectricFurnaceRecipe.tryMatch(Level world, List<ItemStack> inputs)

	public static class Burnt {
		public static final Burnt DEFAULT = new Burnt(ElectricFurnaceEntity.OVERHEAT_TEMPERATURE, Parameters.BURNT_OUTPUT_DEFAULT);
		public float temp;
		public NonNullList<ProcessingOutput> item;
		public Burnt(float temp, NonNullList<ProcessingOutput> item) {
			this.temp = temp;
			this.item = item;
		}
	}
	public static class Either {
		public ElectricFurnaceRecipe electric;
		public AbstractCookingRecipe vanilla;
		public boolean vanillaIsSmoking, isElectric;
		public Either(ElectricFurnaceRecipe recipe) {
			electric = recipe;
			vanilla = null;
			isElectric = true;
		}
		public Either(AbstractCookingRecipe recipe) {
			electric = null;
			vanilla = recipe;
			vanillaIsSmoking = getIsSmoking(recipe);
			isElectric = false;
		}

	

		public int testRecipe(Level world, List<ItemStack> inputs, int maxMultiply, boolean[] isProcessing) {
			List<Integer> useCache = getUseCache(inputs);
			int multiplier = 0;
			if (isElectric)
				while (multiplier < maxMultiply && electric.testRecipe(inputs, useCache, isProcessing))
					multiplier += 1;
			else
				while (multiplier < maxMultiply && ElectricFurnaceRecipe.testRecipe(world, vanilla, inputs, useCache, isProcessing))
					multiplier += 1;
			return multiplier;
		}
		private List<ItemStack> results = null;
		private int multiplier = 0;
		public List<ItemStack> realizeRecipe(Level world, List<ItemStack> inputs, int maxMultiply, boolean[] isProcessing) {
			if (isProcessing != null) for (int i = 0; i < ElectricFurnaceEntity.SLOTS_INPUT; i++)
				isProcessing[i] = false;
			multiplier = testRecipe(world, inputs, maxMultiply, isProcessing);
			results = new ArrayList<>();
			List<ProcessingOutput> outputs = isElectric ? electric.params.output
				: List.of(new ProcessingOutput(vanilla.getResultItem(world.registryAccess()), 1));
			for (ProcessingOutput output : outputs) {
				if (multiplier > 1) {
					ItemStack item = output.getStack();
					output = new ProcessingOutput(item.getItem(), item.getCount() * multiplier, item.getComponentsPatch(), output.getChance());
				}
				ItemStack item = output.rollOutput(world.random);
				if (item.isEmpty()) continue;
				results.add(item);
			}
			return results;
		}
		//Inputs must be the same!  Else rerun realizeRecipe
		public List<ItemStack> applyRecipe(Level world, List<ItemStack> inputs, int maxMultiply,float temp) {
			List<Integer> useCache = getUseCache(inputs);
			int expectedMul = 0;
			if (isElectric) while (expectedMul < maxMultiply && electric.testRecipe(inputs, useCache, null)) {
				expectedMul += 1;
				for (int i = 0, im = inputs.size(); i < im; i++)
					inputs.get(i).setCount(useCache.get(i));
			}
			else while (expectedMul < maxMultiply && ElectricFurnaceRecipe.testRecipe(world, vanilla, inputs, useCache, null)) {
				expectedMul += 1;
				for (int i = 0, im = inputs.size(); i < im; i++)
					inputs.get(i).setCount(useCache.get(i));
			}
			if (expectedMul != multiplier)
				throw new IllegalStateException("Expected recipe count does not match precomputed("+expectedMul+"!="+multiplier+").  Re-run realizeRecipe");
			if (temp > getMaxTemp()) {
				results.clear();
				List<ProcessingOutput> outputs = isElectric ? electric.params.burntOutput : Parameters.BURNT_OUTPUT_DEFAULT;
				for (ProcessingOutput output : outputs) {
					if (multiplier > 1) {
						ItemStack item = output.getStack();
						output = new ProcessingOutput(item.getItem(), item.getCount() * multiplier, item.getComponentsPatch(), output.getChance());
					}
					ItemStack item = output.rollOutput(world.random);
					if (item.isEmpty()) continue;
					results.add(item);
				}
                Thingymabobs.LOGGER.info("Burn recipe: into #"+results.size());
			} else 
                Thingymabobs.LOGGER.info("Apply recipe: into #"+results.size());
			return results;
		}
		public int getMultiplier() {
			return multiplier;
		}
		public List<ItemStack> getResults() {
			return results;
		}

		public float getProcessingTime() {
			return (isElectric ? electric.params.duration : vanilla.getCookingTime() * VANILLA_PROCESS_SCALE) * multiplier;
		}
		public float getMinTemp() {
			return isElectric ? electric.params.temps.minTemp : 
				vanillaIsSmoking ? TEMP_SMOKE_MIN : TEMP_SMELT_MIN;
		}
		public float getMaxTemp() {
			return isElectric ? electric.params.temps.maxTemp : 
				vanillaIsSmoking ? TEMP_SMOKE_MAX : ElectricFurnaceEntity.OVERHEAT_TEMPERATURE;
		}
		public boolean shouldBurnInput(float temp) {
			return temp >= getMaxTemp();
		}
		public static float getBurnTemp(ItemStack item) {
			return ITEM_BURN_TEMPS.getOrDefault(item.getItem(), Burnt.DEFAULT).temp;
		}
		public static boolean shouldBurnOutput(float temp, ItemStack item) {
			return temp >= getBurnTemp(item);
		}
		public static List<ItemStack> getBurntOutputItem(RandomSource random, int maxMultiply, ItemStack item) {
			if (item.isEmpty()) return List.of();
			List<ItemStack> results = new ArrayList<>();
			NonNullList<ProcessingOutput> burnts = ITEM_BURN_TEMPS.getOrDefault(item.getItem(), Burnt.DEFAULT).item;
			int count = Math.min(maxMultiply, item.getCount());
			item.shrink(count);
			for (ProcessingOutput output : burnts) {
				if (count > 1) {
					ItemStack item2 = output.getStack();
					output = new ProcessingOutput(item2.getItem(), item2.getCount() * count, item2.getComponentsPatch(), output.getChance());
				}
				ItemStack burnt = output.rollOutput(random);
				if (burnt.isEmpty()) continue;
				results.add(burnt);
			}
			return results;
		}
	}
	protected static boolean getIsSmoking(AbstractCookingRecipe recipe) {
		return recipe instanceof SmokingRecipe || recipe instanceof CampfireCookingRecipe;
	} 

	@Override
	public ItemStack getResultItem(HolderLookup.Provider arg0) {
		return params.output.isEmpty() ? ItemStack.EMPTY
				: params.output.getFirst()
				.getStack();
	}
	@Override
	public boolean matches(RecipeInput props, Level arg1) {
		return props.equals(this.params);
	}
	@Override
	public NonNullList<Ingredient> getIngredients() {
		return params.input;
	}




	private static final Object RECIPE_ELECTRIC_CACHE_KEY = new Object();
	@SuppressWarnings("unchecked")
	public static List<RecipeHolder<ElectricFurnaceRecipe>> getAll(Level world) {
		return (List<RecipeHolder<ElectricFurnaceRecipe>>)(List<?>)RecipeFinder.get(RECIPE_ELECTRIC_CACHE_KEY, world, ElectricFurnaceRecipe::matchRecipies);
	}
	private static final Object RECIPE_VANILLA_CACHE_KEY = new Object();
	@SuppressWarnings("unchecked")
	public static List<RecipeHolder<? extends AbstractCookingRecipe>> getAllVanilla(Level world) {
		return (List<RecipeHolder<? extends AbstractCookingRecipe>>)(List<?>)RecipeFinder.get(RECIPE_VANILLA_CACHE_KEY, world, ElectricFurnaceRecipe::matchVanillaRecipies);
	}
	private static boolean matchRecipies(RecipeHolder<? extends Recipe<?>> recipe) {
		Recipe<?> r = recipe.value();
		if (r.getType() != ModRecipies.ELECTRIC_FURNACE.getType()) return false;
		return true;
	}
	private static boolean matchVanillaRecipies(RecipeHolder<? extends Recipe<?>> recipe) {
		Recipe<?> r = recipe.value();
		if (!(r instanceof AbstractCookingRecipe)) return false;
		//boolean isSmoking = 
		return true;
	}


	public static Either tryMatch(Level world, List<ItemStack> inputs) {
		List<RecipeHolder<ElectricFurnaceRecipe>> list = getAll(world);
		if (list == null) return null;
		for (RecipeHolder<ElectricFurnaceRecipe> recipeHold : list) {
			ElectricFurnaceRecipe recipe = recipeHold.value();
			if (recipe.testRecipe(inputs, getUseCache(inputs), null))
				return new Either(recipe);
		}
		List<RecipeHolder<? extends AbstractCookingRecipe>> listVanilla = getAllVanilla(world);
		if (listVanilla == null) return null;
		for (RecipeHolder<? extends AbstractCookingRecipe> recipeHold : listVanilla) {
			AbstractCookingRecipe recipe = recipeHold.value();
			if (testRecipe(world, recipe, inputs, getUseCache(inputs), null)) 
				return new Either(recipe);
		}
		return null;
	}
	private static final  ThreadLocal<List<Integer>> itemUseCache = new ThreadLocal<>(); 
	private static final List<Integer> getUseCache(List<ItemStack> inputs) {
		List<Integer> useCache = itemUseCache.get();
		if (useCache == null)
			itemUseCache.set(useCache = new ArrayList<>());
		useCache.clear();
		for (ItemStack item : inputs)
			useCache.add(item.isEmpty() ? 0 : item.getCount());
		return useCache;
	}
	private boolean testRecipe(List<ItemStack> inputs, List<Integer> useCache, boolean[] isProcessing) {
		for (Ingredient required : params.input) {
			boolean hasThis = false;
			for (int i = 0, im = inputs.size(); i < im; i++) {
				ItemStack item = inputs.get(i);
				if (useCache.get(i)==0) continue;
				if (!required.test(item)) continue;
				hasThis = true;
				if (isProcessing != null) isProcessing[i] = true;
				useCache.set(i, useCache.get(i)-1);
				break;
			}
			if (hasThis) continue;
			return false;
		}
		return true;
	}
	private static boolean testRecipe(Level world, AbstractCookingRecipe recipe, List<ItemStack> inputs, List<Integer> useCache, boolean[] isProcessing) {
		for (int i = 0, im = inputs.size(); i < im; i++) {
			SingleRecipeInput item = new SingleRecipeInput(inputs.get(i));
			if (useCache.get(i)==0) continue;
			if (!recipe.matches(item, world)) continue;
			useCache.set(i, useCache.get(i)-1);
			if (isProcessing != null) isProcessing[i] = true;
			return true;
		}
		return false;
	}

	


	public static class Factory implements IRecipeFactory<Parameters, ElectricFurnaceRecipe> {
		@Override
		public MapCodec<Parameters> codec() {
			return Parameters.CODEC;
		}
		@Override
		public StreamCodec<RegistryFriendlyByteBuf, Parameters> streamCodec() {
			return Parameters.STREAM_CODEC;
		}
		@Override
		public ElectricFurnaceRecipe create(Parameters props) {
			return new ElectricFurnaceRecipe(props);
		}
	}
	public static class Parameters implements RecipeInput {
		private static final ProcessingOutput CHARCOAL_OUTPUT = new ProcessingOutput(new ItemStack(Items.CHARCOAL, 1), 0.25f);
		public static final NonNullList<ProcessingOutput> BURNT_OUTPUT_DEFAULT = NonNullList.of(CHARCOAL_OUTPUT, CHARCOAL_OUTPUT);
		public NonNullList<Ingredient> input;
		public NonNullList<ProcessingOutput> output;
		public NonNullList<ProcessingOutput> burntOutput;
		public float duration;
		public Temps temps;
		//If bigger than max temp, always burn;
		public Parameters(NonNullList<Ingredient> input, NonNullList<ProcessingOutput> output, NonNullList<ProcessingOutput> burntOutput,
				float duration, Temps temps) {
			this.input = input;
			this.output = output;
			this.burntOutput = burntOutput;
			this.duration = duration;
			this.temps = temps;
		}
		public NonNullList<Ingredient> input() {
			return input;
		}
		public NonNullList<ProcessingOutput> output() {
			return output;
		}
		public NonNullList<ProcessingOutput> burntOutput() {
			return burntOutput;
		}
		public float duration() {
			return duration;
		}
		public Temps temps() {
			return temps;
		}

		public static final class Temps {
			public float minTemp, maxTemp, burnTemp;
			public Temps(float minTemp, float maxTemp, float burnTemp) {
				this.minTemp = minTemp;
				this.maxTemp = maxTemp;
				this.burnTemp = burnTemp < minTemp ? maxTemp : burnTemp;
			}
			public float minTemp() {
				return minTemp;
			}
			public float maxTemp() {
				return maxTemp;
			}
			public float burnTemp() {
				return burnTemp;
			}
		}

		private static MapCodec<Temps> CODE_TEMPS = RecordCodecBuilder.mapCodec(instance2 -> instance2.group(
			Codec.FLOAT.fieldOf("minTemp").forGetter(Temps::minTemp),
			Codec.FLOAT.fieldOf("maxTemp").forGetter(Temps::maxTemp),
			Codec.FLOAT.optionalFieldOf("burnTemp", 0f).forGetter(Temps::burnTemp)
		).apply(instance2, Temps::new));
		public static MapCodec<Parameters> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Ingredient.CODEC.listOf().xmap(
				NonNullList::copyOf, list -> list
			).fieldOf("input").forGetter(Parameters::input),
			ProcessingOutput.CODEC_NEW.listOf().xmap(
				NonNullList::copyOf, list -> list
			).fieldOf("result").forGetter(Parameters::output),
			ProcessingOutput.CODEC_NEW.listOf().xmap(
				NonNullList::copyOf, list -> list
			).optionalFieldOf("burnt", BURNT_OUTPUT_DEFAULT).forGetter(Parameters::burntOutput),
			Codec.FLOAT.optionalFieldOf("duration", DEFAULT_TIME_PER_PROCESS)
				.forGetter(Parameters::duration),
			CODE_TEMPS.fieldOf("temps").forGetter(Parameters::temps)
		).apply(instance, Parameters::new));
		public static StreamCodec<RegistryFriendlyByteBuf, Parameters> STREAM_CODEC =  StreamCodec.composite(
			ByteBufCodecs.collection(NonNullList::createWithCapacity, Ingredient.CONTENTS_STREAM_CODEC), Parameters::input,
			ByteBufCodecs.collection(NonNullList::createWithCapacity, ProcessingOutput.STREAM_CODEC), Parameters::output,
			ByteBufCodecs.collection(NonNullList::createWithCapacity, ProcessingOutput.STREAM_CODEC), Parameters::burntOutput,
			ByteBufCodecs.FLOAT, Parameters::duration,
			StreamCodec.composite(
				ByteBufCodecs.FLOAT, Temps::minTemp,
				ByteBufCodecs.FLOAT, Temps::maxTemp,
				ByteBufCodecs.FLOAT, Temps::burnTemp,
				Temps::new
			), Parameters::temps,
			Parameters::new
		);
		@Override
		public ItemStack getItem(int index) {
			Ingredient ingredient = input.get(index);
			return ingredient.isEmpty() ? ItemStack.EMPTY : ingredient.getItems()[0];
		}
		@Override
		public int size() {
			return input.size();
		}
		@Override
		public boolean isEmpty() {
			return input.isEmpty() || output.isEmpty();
		}
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Parameters other)) return false;
			return input.equals(other.input) && output.equals(other.output) && burntOutput.equals(other.burntOutput)
			 && duration == other.duration && temps.burnTemp == other.temps.burnTemp
			 && temps.maxTemp == other.temps.maxTemp && temps.minTemp == other.temps.minTemp;
		}
	}
}
