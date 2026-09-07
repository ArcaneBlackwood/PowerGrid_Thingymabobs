package dev.thingymabobs.blocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.light.bulb.GrowthLamp;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import dev.thingymabobs.blocks.LavaLampThermalBehaviour.Properties;
import dev.thingymabobs.packets.LavaLampGlobS2CPacket;
import dev.thingymabobs.registry.ModAttachments;
import dev.thingymabobs.registry.ModBlockEntities;
import dev.thingymabobs.registry.ModPackets;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

public class LavaLampEntity extends ElectricBlockEntity implements ElectricBehaviour.SyncAppender {
    public static final Set<LavaLampEntity> ALL_LOADED_LAMPS = new HashSet<>();
    public static void registerLoading(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(LavaLampEntity::onPlayerSleep);
        NeoForge.EVENT_BUS.addListener(LavaLampEntity::onPlayerWake);
    }
    
    public static void onPlayerWake(PlayerWakeUpEvent event) {
        Player that = event.getEntity();
        if (!(that instanceof ServerPlayer player)) return;
		Level level = that.level();
		for (LavaLampEntity lamp : LavaLampEntity.ALL_LOADED_LAMPS) {
			if (lamp.getLevel() != level) continue;
			if (lamp.getBlockPos().distToCenterSqr(null) > lamp.getEffectRadius()) continue;
			ModAttachments.PhantomSleepReduction.set(player, lamp.getInsomniaTimeOffset());
            return;
		}
        ModAttachments.PhantomSleepReduction.set(player, 0);
    }
    public static void onPlayerSleep(CanPlayerSleepEvent event) {
        ServerPlayer that = event.getEntity();
		Level level = that.level();
		for (LavaLampEntity lamp : LavaLampEntity.ALL_LOADED_LAMPS) {
			if (lamp.getLevel() != level) continue;
			if (lamp.getBlockPos().distToCenterSqr(null) > lamp.getEffectRadius()) continue;
			if (!lamp.isActive()) continue;
			event.setProblem(null);
            return;
		}
    }

	public static final LavaLampThermalBehaviour.Properties PROPERTIES = new Properties()
		.setLampPower(150f).setLampMass(0.1f).setLampTemp(1450f).setLampOverheat(2000f)
		.setLavaTemp(75f).setLavaMass(80f).setLavaOverheat(85f)
		.setInterPercent(0.2f).initialize();
    protected static final int WAX_PRECISION = 128;
    protected static final int WAX_TOTAL = 18*WAX_PRECISION;
    public static final ItemEntry<GrowthLamp> BULB = ModdedItems.GROWTH_LAMP;
    public static float IDEAL_TEMPERATURE = PROPERTIES.lavaTemp - 15f;
    public static float RESISTANCE_MIN = 240*240 / (PROPERTIES.lampPower*2);
    public static float RESISTANCE_MAX = 240*240 / PROPERTIES.lampPower;
    public static float PARTICLE_RATE = 1f / (20 * 20);
    public static float PARTICLE_SPEED = 1f / (20f);
    public static int PARTICLE_MAX_VOLUME = WAX_PRECISION*8;
    public static float RATED_VOLTAGE = Mth.sqrt(PROPERTIES.lampPower * RESISTANCE_MAX);
    public static float INSOMNIA_DAYS_ADD = 9;

    protected SwitchedWire wire;
    protected int colorBase, colorGlass, colorWax;
    
    protected int waxTop;
    protected int waxBottom;
    protected ArrayList<Glob> globs = new ArrayList<>(4);
    protected boolean registered = false;

    @Override
    public void invalidate() {
        super.invalidate();
        if (!registered) return;
        ALL_LOADED_LAMPS.remove(this);
    }
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (!registered) return;
        ALL_LOADED_LAMPS.remove(this);
    }

    public LavaLampEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAVA_LAMP.get(), pos, state);
        //Colors of crafting recipes
        colorBase = DyeColor.YELLOW.getTextureDiffuseColor();
        colorGlass = DyeColor.BLACK.getTextureDiffuseColor();
        colorWax = DyeColor.ORANGE.getTextureDiffuseColor();
        waxTop = 0;
        waxBottom = WAX_TOTAL;
        setLazyTickRate(100);
    }
    @Override
    public void initialize() {
        super.initialize();
        electricBehaviour.setSyncAppender(this);
    }
    @Override
    public ThermalBehaviour specifyThermalBehaviour() {
        return new LavaLampThermalBehaviour(this, PROPERTIES);
    }
    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        BlockState blockState = getBlockState();
        wire = builder.connectSwitch(
            getLampResistance(), builder.terminalNode(0), builder.terminalNode(1),
            LavaLamp.hasFunctionalBulb(blockState.getValue(LavaLamp.STATE)));
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state) {
		super.getRequiredItems(state);
        if(state.getValue(LavaLamp.STATE) == LavaLamp.STATE_EMPTY)
            return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, BULB.get());
        return ItemRequirement.NONE;
    }
    public boolean replaceBulb(Player player, InteractionHand hand, ItemStack usedStack, float hitY) {
        assert level != null;
        BlockState blockState = getBlockState();
        int newState = replaceBulbInternal(player, hand, usedStack, hitY, blockState);
        if(newState != -1) updateState(newState);
        return newState != -1;
    }
    private int replaceBulbInternal(Player player, InteractionHand hand, ItemStack usedStack, float hitY, BlockState blockState) {
        assert level != null;
        int state = blockState.getValue(LavaLamp.STATE);
        boolean stackEmpty = usedStack == null || usedStack.isEmpty();
        if(LavaLamp.hasBulb(state) && stackEmpty) {
            playInteractBulbSound();
            if(!level.isClientSide) {
                ((LavaLampThermalBehaviour)thermalBehaviour).resetLampTemperature();
                if (LavaLamp.hasFunctionalBulb(state)) player.setItemInHand(hand, BULB.asStack());
            }
            notifyUpdate();
            return LavaLamp.STATE_EMPTY;
        } else if (!stackEmpty && usedStack.is(BULB.get()) && (usedStack.getCount() > 1 || player.isCreative())) {
            playInteractBulbSound();
            notifyUpdate();
            if(!level.isClientSide) {
                ((LavaLampThermalBehaviour)thermalBehaviour).resetLampTemperature();
                if (!LavaLamp.hasFunctionalBulb(state) && !player.isCreative()) usedStack.shrink(1);
                if (!(player.getOffhandItem().getItem() instanceof DyeItem dye)) return LavaLamp.STATE_OFF;
                colorBase = colorGlass = colorWax = dye.getDyeColor().getTextureDiffuseColor();
            }
            return LavaLamp.STATE_OFF;
        } else if (!stackEmpty && usedStack.getItem() instanceof DyeItem dye) {
            playInteractDyeSound();
            int newColor = dye.getDyeColor().getTextureDiffuseColor();
            if (hitY < 6/16f) colorBase = newColor;
            else if (hitY < 11/16f) colorWax = newColor;
            else colorGlass = newColor;
            notifyUpdate();
            return state;
        }
        return -1;
    }
    public void playInteractBulbSound() {
        if (level == null || level.isClientSide) return;
        level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.30F, 1.0F);
    }
    public void playInteractDyeSound() {
        if (level == null || level.isClientSide) return;
        level.playSound(null, worldPosition, SoundEvents.DYE_USE, SoundSource.BLOCKS, 0.30F, 1.0F);
    }


    public void playBlowEffect() {
        if(level == null) return;
        var pos = this.worldPosition.getCenter();
        if (level.isClientSide()) {
            SparkParticleData.explodeParticles(level, (float) pos.x, (float) pos.y, (float) pos.z, Direction.UP, 5);
        } else {
            ModdedSoundEvents.FUSE_POPS.playAt(level, pos, 1.0f, 1.0f, false);
        }
    }
    protected void updateState(int newState) {
        BlockState blockState = getBlockState();
        if(!level.isClientSide) {
            LavaLampThermalBehaviour thermal = (LavaLampThermalBehaviour)thermalBehaviour;
            int oldState = blockState.getValue(LavaLamp.STATE), state = newState == -1 ? oldState : newState;
            if (LavaLamp.hasFunctionalBulb(state)) {
                if (thermal.isLampOverheated()) {
                    state = LavaLamp.STATE_BLOWN;
                } else {
                    float temperaturePercent = thermal.getLampTemperature() / PROPERTIES.lampTemp;
                    state = LavaLamp.STATE_OFF;
                    if(temperaturePercent > 0.7f) {
                        state = LavaLamp.STATE_ON_HIGH;
                    } else if(temperaturePercent > 0.35f) {
                        state = LavaLamp.STATE_ON;
                    }
                }
            }
            boolean functionalBulb = LavaLamp.hasFunctionalBulb(state);
            if (oldState != state) {
                wire.setState(functionalBulb);
                if (!functionalBulb) thermal.resetLampTemperature();
                if (state == LavaLamp.STATE_BLOWN) playBlowEffect();
                level.setBlock(worldPosition, blockState.setValue(LavaLamp.STATE, state), Block.UPDATE_ALL_IMMEDIATE);
                notifyUpdate();
            }
            if (functionalBulb) wire.setResistance(getLampResistance());
        }
    }


    @Override
    public void tick() {
        super.tick();
        updateParticles(1/20f);
        if (level.isClientSide) return;
        if (!registered) {
            registered = true;
            ALL_LOADED_LAMPS.add(this);
        }
        spawnParticles();
    }
    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level.isClientSide) return;
        checkWaxTotals();
    }
    public void checkWaxTotals() {
        int waxTotal = waxBottom + waxTop;
        for (Glob glob : globs) waxTotal += glob.volume;
        if (waxTotal == WAX_TOTAL) return;
        waxTop = 0;
        waxBottom = WAX_TOTAL;
        globs.clear();
    }
    public void updateParticles(float deltaTime) {
        Iterator<Glob> globIterator = globs.iterator();
        while (globIterator.hasNext()) {
            Glob glob = globIterator.next();
            glob.progress += glob.speed * deltaTime;
            if (glob.progress < 1.0f) continue;
            if (glob.movingUp) waxTop += glob.volume;
            else waxBottom += glob.volume;
            globIterator.remove();
        }
    }
    public void spawnParticles() {
        if (level == null || level.isClientSide) return;
        float targetWax = getTargetWaxTop();//+1
        float currentWax = (waxTop - waxBottom) / WAX_TOTAL; //-1
        float waxDiff = targetWax - currentWax; //2
        float spawnSpeed = Mth.abs(waxDiff) * 0.3f + 0.7f;
        if (level.random.nextFloat() * spawnSpeed > PARTICLE_RATE) return;
        boolean movingUp = level.random.nextFloat() < waxDiff * 0.25 + 0.5;

        float randVolume = level.random.nextFloat();
        randVolume = randVolume*randVolume*randVolume;
        int volume = Math.min(Mth.floor(PARTICLE_MAX_VOLUME*randVolume), movingUp ? waxBottom : waxTop);
        Glob newGlob = Glob.fromVolume(
            movingUp, PARTICLE_SPEED, volume,
            Rotation.getRandom(level.random), level.random.nextFloat() * 2 - 1, level.random.nextFloat() * 2 - 1
        );
        globs.add(newGlob);
        if (movingUp) waxBottom -= volume;
        else waxTop += volume;
        ModPackets.sendToClientsTracking(new LavaLampGlobS2CPacket(this, newGlob), this);
    }
    public void onParticlePacket(Glob glob, int waxTop, int waxBottom) {
        if (level == null || !level.isClientSide) return;
        globs.add(glob);
        this.waxTop = waxTop;
        this.waxBottom = waxBottom;
    }


    @Override
    public void electricalTick() {
        applyPower(wire);
        updateState(-1);
    }
    @Override
   	protected void applyPower(@Nullable AbstractElectricWire wire) {
		super.applyPower(wire);
		if (thermalBehaviour != null) {
			((LavaLampThermalBehaviour)thermalBehaviour).applyLampPower(wire);
		}
	}
    public float getLampResistance() {
        if (thermalBehaviour == null) return RESISTANCE_MIN;
        return RESISTANCE_MIN + ((RESISTANCE_MAX - RESISTANCE_MIN) / PROPERTIES.lampOverheat) * ((LavaLampThermalBehaviour)thermalBehaviour).getLampTemperature();
    }
    public float getTargetWaxTop() {
        if (thermalBehaviour == null) return -1;
        float lavaTemp = ((LavaLampThermalBehaviour)thermalBehaviour).getLavaTemperature();
        return Mth.clamp((lavaTemp - IDEAL_TEMPERATURE) * (2f / PROPERTIES.lavaTemp), -1, 1);
    }
    public float getEffectRadius() {
        return 16f;
    }
    public boolean isActive() {
        return LavaLamp.isPowered(getBlockState().getValue(LavaLamp.STATE));
    }
    public float getActivityMultiplier() {
        if (!isActive()) return 0f;
        return 2f - Mth.abs(waxBottom - waxBottom) * 1.5f / WAX_TOTAL;
    }
    public int getInsomniaTimeOffset() {
        return Mth.floor(getActivityMultiplier() * INSOMNIA_DAYS_ADD * 2f) * 10 * 60 * 24;
    }

    @Override
    protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (clientPacket) {
            tag.putInt("WaxT", waxTop);
            tag.putInt("WaxB", waxBottom);
        } else {
            checkWaxTotals();
            tag.putInt("WaxT", waxTop);
        }
        tag.putInt("ColorB", colorBase);
        tag.putInt("ColorG", colorGlass);
        tag.putInt("ColorW", colorWax);
    }
    @Override
    protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (clientPacket) {
            waxTop = tag.getInt("WaxT");
            waxBottom = tag.getInt("WaxB");
        } else {
            waxTop = tag.getInt("WaxT");
            if (waxTop < 0 || waxTop > WAX_TOTAL) waxTop = 0;
            waxBottom = WAX_TOTAL - waxTop;
        }
        colorBase = tag.getInt("ColorB");
        colorGlass = tag.getInt("ColorG");
        colorWax = tag.getInt("ColorW");
    }

    @Override
    public void writeToSync(FriendlyByteBuf buff) {
        if (thermalBehaviour == null) {
            buff.writeFloat(0);
            buff.writeFloat(0);
        } else {
            LavaLampThermalBehaviour thermal = (LavaLampThermalBehaviour)thermalBehaviour;
            buff.writeFloat(thermal.getLampTemperature());
            buff.writeFloat(thermal.getLavaTemperature());
        }
    }
    @Override
    public void readFromSync(FriendlyByteBuf buff) {
        if (thermalBehaviour == null) {
            buff.readFloat();
            buff.readFloat();
        } else {
            LavaLampThermalBehaviour thermal = (LavaLampThermalBehaviour)thermalBehaviour;
            thermal.setLampTemperature(buff.readFloat());
            thermal.setLavaTemperature(buff.readFloat());
        }
    }

    public float getVisibleWax(boolean top) {
        return (float)(top ? waxTop : waxBottom) / WAX_TOTAL;
    }
    public int getVisibleWaxRaw(boolean top) {
        return (top ? waxTop : waxBottom);
    }

    public static class Glob {
        public boolean movingUp;
        public float speed;
        public float size;
        public int volume;
        public Rotation rotation;
        public float progress;
        public float x, z;
        public Glob(boolean movingUp, float speed, int volume, float size, Rotation rotation, float x, float z) {
            this.movingUp = movingUp;
            this.speed = speed;
            this.volume = volume;
            this.size = size;
            this.rotation = rotation;
            this.x = x;
            this.z = z;
            progress = 0;
        }
        public static Glob fromVolume(boolean movingUp, float speed, int volume, Rotation rotation, float x, float z) {
            return new Glob(movingUp, speed, volume, (float)Math.cbrt((double)volume / WAX_PRECISION), rotation, x, z);
        }
        public float getVolume() {
            return (float)volume / WAX_TOTAL;
        }
    }
}
