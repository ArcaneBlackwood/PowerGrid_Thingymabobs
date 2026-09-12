package dev.thingymabobs.blocks.lavalamp;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dev.thingymabobs.config.properties.CProperties.ASubProp;
import dev.thingymabobs.config.properties.CProperties.Builder;

import com.simibubi.create.content.kinetics.fan.AirCurrent;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedDamageTypes;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class LavaLampThermalBehaviour extends ThermalBehaviour {
	//public static final BehaviourType<LavaLampThermalBehaviour> TYPE = new BehaviourType<>("lava_lamp_thermal");
	public static float DELTA_TIME = 1 / 20F;
	public static final int OVERHEAT_TICKS = 2;

	private float lampTemperature, lavaTemperature;
	private float prevLampTemperature, prevLavaTemperature;
	private int lampOverheatTicks = 0, lavaOverheatTicks = 0;
	private Runnable lampOverheatCallback;
	private float cachedAmbientTemperature = -2048f;

	public Properties properties;


	private final Map<AirCurrent, Float> coolingAir = new HashMap<>();
	private float totalCoolingFactorMultiplier;

	private IParticleGenerator particleGenerator = null;

	public float lavaConduction, lavaDissipation, lavaOverheat;
	
	public LavaLampThermalBehaviour(SmartBlockEntity be, Properties properties) {
		super(be, 0, 0, 0);
		this.lampTemperature = cachedAmbientTemperature;
		this.lavaTemperature = cachedAmbientTemperature;
		this.totalCoolingFactorMultiplier = 1.0f;
		this.properties = properties;
	}

	@Override
	public LavaLampThermalBehaviour particleGenerator(IParticleGenerator generator) {
		this.particleGenerator = generator;
		return this;
	}

	@Override
	public void resetTemperature() {
		this.lampTemperature = cachedAmbientTemperature;
		this.lavaTemperature = cachedAmbientTemperature;
	}
	public void resetLampTemperature() {
		this.lampTemperature = cachedAmbientTemperature;
	}

	@Override
	public void addCoolingMultiplier(AirCurrent current, float value) {
		var currentValue = coolingAir.get(current);
		if (currentValue != null) {
			totalCoolingFactorMultiplier -= currentValue;
			if (totalCoolingFactorMultiplier < 1)
				totalCoolingFactorMultiplier = 1;
		}
		coolingAir.put(current, value);
		totalCoolingFactorMultiplier += value;
	}

	@Override
	public void removeCoolingMultiplier(AirCurrent current) {
		var currentValue = coolingAir.remove(current);
		if (currentValue != null)
			totalCoolingFactorMultiplier -= currentValue;
	}

	public void setLampOverheatCallback(Runnable callback) {
		this.lampOverheatCallback = callback;
	}

	@Override
	public void tick() {
		if(cachedAmbientTemperature < ThermalBehaviour.ABSOLUTE_ZERO) {
			cachedAmbientTemperature = ThermalBehaviour.getAmbientTemperature(getWorld(), getPos());
			return;
		}
		if(lampTemperature < ThermalBehaviour.ABSOLUTE_ZERO)
			lampTemperature = cachedAmbientTemperature;
		if(lavaTemperature < ThermalBehaviour.ABSOLUTE_ZERO)
			lavaTemperature = cachedAmbientTemperature;

		var world = getWorld();
		if(!world.isClientSide || blockEntity.isVirtual()) tickServer();
		else tickClient();
	}
	public void tickServer() {
		var world = getWorld();
		var pos = getPos();

		var iter = coolingAir.entrySet().iterator();
		while(iter.hasNext()) {
			var entry = iter.next();
			if(entry.getKey().source.isSourceRemoved() || entry.getKey().source.getSpeed() == 0) {
				totalCoolingFactorMultiplier -= entry.getValue();
				iter.remove();
			}
		}
		// Conduct energy between lamp/lava
		float conductionPower = properties.interConduction * (lampTemperature - lavaTemperature);

		// Dissipate energy
		float lampDissipatedPower = properties.lampDissipation * (lampTemperature - cachedAmbientTemperature);
		float lavaDissipatedPower = properties.lavaDissipation * totalCoolingFactorMultiplier * (lavaTemperature - cachedAmbientTemperature);
		lampTemperature -= (lampDissipatedPower + conductionPower) * DELTA_TIME / properties.lampMass;
		lavaTemperature -= (lavaDissipatedPower - conductionPower) * DELTA_TIME / properties.lavaMass;
		if (lampDissipatedPower < 0 && lampTemperature < cachedAmbientTemperature)
			lampTemperature = cachedAmbientTemperature;
		if (lavaDissipatedPower < 0 && lavaTemperature < cachedAmbientTemperature)
			lavaTemperature = cachedAmbientTemperature;
		if (lampDissipatedPower != 0 || lavaDissipatedPower != 0 || conductionPower != 0)
			world.blockEntityChanged(getPos());


		if (!Float.isFinite(lampTemperature)) {
			// Reset if something went wrong.
			lampTemperature = cachedAmbientTemperature;
			prevLampTemperature = cachedAmbientTemperature;
		}
		if (!Float.isFinite(lavaTemperature)) {
			// Reset if something went wrong.
			lavaTemperature = cachedAmbientTemperature;
			prevLavaTemperature = cachedAmbientTemperature;
		}
		var lampTemperatureDelta = lampTemperature - prevLampTemperature;
		prevLampTemperature = lampTemperature;
		var lavaTemperatureDelta = lavaTemperature - prevLavaTemperature;
		prevLavaTemperature = lavaTemperature;

		if(lampTemperature >= properties.lampOverheat) {
			if(lampTemperatureDelta > 0 && lampOverheatTicks++ >= OVERHEAT_TICKS) {
				if (lampOverheatCallback != null)
					lampOverheatCallback.run();
			} else if(lampTemperatureDelta <= 0) {
				lampOverheatTicks = 0;
				if(lampTemperature > properties.lampOverheat + 10) {
					lampTemperature = properties.lampOverheat + 10;
					blockEntity.sendData();
				}
			}
		}
		if(lavaTemperature >= properties.lavaOverheat) {
			if(lavaTemperatureDelta > 0 && lavaOverheatTicks++ >= OVERHEAT_TICKS) {
				explode(world, pos, blockEntity.getBlockState(), 1.0f);
			} else if(lavaTemperatureDelta <= 0) {
				lavaOverheatTicks = 0;
				if(lavaTemperature > properties.lavaOverheat + 10) {
					lavaTemperature = properties.lavaOverheat + 10;
					blockEntity.sendData();
				}
			}
		}
	}

	public void tickClient() {
		var world = getWorld();
		var pos = getPos();
		if(lampTemperature >= properties.lampTemp) {
			var random = getWorld().getRandom();
			float chance = (lampTemperature - properties.lampOverheat) * properties.lampOverheatDiff;
			if (chance > 0.2 && random.nextFloat() < chance) {
				if (particleGenerator == null) {
					double x = pos.getX() + random.nextDouble();
					double y = pos.getY() + random.nextDouble();
					double z = pos.getZ() + random.nextDouble();
					world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0f, 0.05f, 0.0f);
				} else {
					particleGenerator.generate((x, y, z) ->
						world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0f, 0.05f, 0.0f),
						random);
				}
			}
		}
		if(lavaTemperature >= properties.lavaTemp) {
			var random = getWorld().getRandom();
			float chance = (lavaTemperature - properties.lavaOverheat) * properties.lavaOverheatDiff;
			if (chance > 0.2 && random.nextFloat() < chance) {
				if (particleGenerator == null) {
					double x = pos.getX() + random.nextDouble();
					double y = pos.getY() + random.nextDouble();
					double z = pos.getZ() + random.nextDouble();
					world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0f, 0.05f, 0.0f);
				} else {
					particleGenerator.generate((x, y, z) ->
						world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0f, 0.05f, 0.0f),
						random);
				}
			}
		}
	}

	public static void explode(Level world, BlockPos pos, BlockState state, float power) {
		if(ThermalBehaviour.shouldExplode()) {
			var source = new MachineOverloadDamageSource(ModdedDamageTypes.OVERLOADED_MACHINE.holder(world), state.getBlock());
			// This block must be broken first to allow for damage to propagate.
			world.destroyBlock(pos, false);
			world.explode(null, source, null, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, power, false, Level.ExplosionInteraction.BLOCK);
		} else {
			// Break block without exploding.
			world.destroyBlock(pos, false);
		}
	}

	@Override
	public boolean isOverheated() {
		return lampTemperature >= properties.lampOverheat || lavaTemperature >= properties.lavaOverheat;
	}
	public boolean isLampOverheated() {
		return lampTemperature >= properties.lampOverheat;
	}
	public boolean isLavaOverheated() {
		return lavaTemperature >= properties.lavaOverheat;
	}

	public void applyLampPower(@Nullable AbstractElectricWire wire) {
		if(wire == null)
			return;
		if(wire.isConverged()) {
			var network = wire.getNetwork();
			if(network != null) {
				if(wire.getNode1() != null && network.isLeaf(wire.getNode1()))
					return;
				if(wire.getNode2() != null && network.isLeaf(wire.getNode2()))
					return;
			}
			double power = wire.power();
			if(!Double.isFinite(power)) return;
			lampTemperature += (float) (power * DELTA_TIME / properties.lampMass);
		}
	}

	@Override
	public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		lampTemperature = nbt.getFloat("LampTemperature");
		lavaTemperature = nbt.getFloat("LavaTemperature");
	}

	@Override
	public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		nbt.putFloat("LampTemperature", lampTemperature);
		nbt.putFloat("LavaTemperature", lavaTemperature);
	}

	//@Override
	//spublic BehaviourType<?> getType() {
	//	return TYPE;
	//}

	@Override
	public float getTemperature() {
		return lavaTemperature;
	}
	public float getLampTemperature() {
		return Math.max(lampTemperature, 10);
	}
	public float getLavaTemperature() {
		return lavaTemperature;
	}
	public void setLampTemperature(float value) {
		lampTemperature = value;
	}
	public void setLavaTemperature(float value) {
		lavaTemperature = value;
	}

	@Override
	public void writeToSync(FriendlyByteBuf buffer, boolean useDoubles, Function<OwnedFloatingNode, TransmissionLine> lineLookup) {
	}

	@Override
	public void readFromSync(FriendlyByteBuf buffer, boolean useDoubles) {
	}

	public static class Properties extends ASubProp {
		//thermal mass ΔE/ΔT
		// dissipation coefficient * area
		//conduction = watts / Δdegree
		public float lampDissipation = Float.NaN, lampOverheatDiff = Float.NaN;
		public float interConduction = Float.NaN;
		public float lavaDissipation = Float.NaN, lavaOverheatDiff = Float.NaN;

		public ConfigBase.ConfigFloat lampMassConf, lampTempConf, lavaMassConf, lavaTempConf,
			lampPowerConf, lampOverheatConf, lavaOverheatConf, interPercentConf;
		public float lampMass, lampTemp, lavaMass, lavaTemp,
			lampPower, lampOverheat, lavaOverheat, interPercent;

		public Properties() { }

		public Properties initialize() {
			float interPower = lampPower * interPercent;

			lampDissipation = ThermalBehaviour.dissipationFactor(lampPower * (1f - interPercent), lampTemp);
			interConduction = interPower / (lampTemp - lavaTemp);
			lavaDissipation = ThermalBehaviour.dissipationFactor(interPower, lavaTemp);

			lampOverheatDiff = 1 / (lampOverheat - lampTemp);
			lavaOverheatDiff = 1 / (lavaOverheat - lavaTemp);
			return this;
		}
		public Properties setLampMass(float value) {
			lampMass = value;
			return this;
		}
		public Properties setLampTemp(float value) {
			lampTemp = value;
			return this;
		}
		public Properties setLavaMass(float value) {
			lavaMass = value;
			return this;
		}
		public Properties setLavaTemp(float value) {
			lavaTemp = value;
			return this;
		}
		public Properties setLampPower(float value) {
			lampPower = value;
			return this;
		}
		public Properties setLampOverheat(float value) {
			lampOverheat = value;
			return this;
		}
		public Properties setLavaOverheat(float value) {
			lavaOverheat = value;
			return this;
		}
		public Properties setInterPercent(float value) {
			interPercent = value;
			return this;
		}

		@Override
		public Class<?> getType() {
			return Properties.class;
		}
		@Override
		public void onLoad() {
			lampMass = lampMassConf.getF();
			lampTemp = lampTempConf.getF();
			lavaMass = lavaMassConf.getF();
			lavaTemp = lavaTempConf.getF();
			lampPower = lampPowerConf.getF();
			lampOverheat = lampOverheatConf.getF();
			lavaOverheat = lavaOverheatConf.getF();
			interPercent = interPercentConf.getF();
			initialize();
		}
		@Override
		public void register(String id, Builder builder) {
			lampPowerConf = builder.f(lampPower, 0f, id+"_bulb_power");
			lampMassConf = builder.f(lampMass, 0f, id+"_bulb_mass");
			lampTempConf = builder.f(lampTemp, 0f, id+"_bulb_temperature");
			lampOverheatConf = builder.f(lampOverheat, 0f, id+"_bulb_overheat");
			interPercentConf = builder.f(interPercent, 0f, 1f, id+"_inter_conduction_percent",
				"Amount of power to transfer from the bulb to lava instead of the environment." );
			lavaMassConf = builder.f(lavaMass, 0f, id+"_lava_mass");
			lavaTempConf = builder.f(lavaTemp, 0f, id+"_lava_temperature");
			lavaOverheatConf = builder.f(lavaOverheat, 0f, id+"_lava_overheat");
		}
	}
}
