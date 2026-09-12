package dev.thingymabobs.blocks.battery;

import org.joml.Vector3f;
import org.patryk3211.powergrid.config.ThermalValues;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class PotatoThermalBehaviour extends ThermalBehaviour {
	protected float overheatTemperature;
	public float sparks = 0;
	
	public PotatoThermalBehaviour(SmartBlockEntity be, float thermalMass, float dissipationFactor, float overheatTemperature) {
		super(be, thermalMass, dissipationFactor, overheatTemperature);
		this.overheatTemperature = overheatTemperature;
	}
	public static PotatoThermalBehaviour fromConfig(SmartBlockEntity be, float overheatTemperature) {
		Block block = be.getBlockState().getBlock();
		return forMaxPower(be, ThermalValues.getMass(block), ThermalValues.getPower(block), overheatTemperature);
	}
	public static PotatoThermalBehaviour forMaxPower(SmartBlockEntity be, float thermalMass, float power, float overheatTemperature) {
		float targetTemperature = overheatTemperature - 25.0F;
		return new PotatoThermalBehaviour(be, thermalMass, dissipationFactor(power, targetTemperature), overheatTemperature);
	}
	
	@Override
	public void tick() {
		super.tick();

		var world = getWorld();
		if(world.isClientSide && sparks > 0.01 && !blockEntity.isVirtual()) {
			var random = world.getRandom();
			float chance = sparks * 0.8f + 0.2f;
			if (random.nextFloat() < chance) {
				var pos = getPos();
				Direction dir = blockEntity.getBlockState().getValue(BlockStateProperties.FACING).getOpposite();
				explodeParticles(world, pos.getX(), pos.getY(), pos.getZ(), dir, 2);
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static void explodeParticles(Level world, double x, double y, double z, Direction dir, int count) {
		var r = world.random;
		float distance = 8f / 16f;
		float height = 3f / 16f;
		Vector3f planex = new Vector3f(tangent(dir).step());
		Vector3f planey = new Vector3f(bitangent(dir).step());
		Vector3f planez = dir.step();

		for (int i = 0; i < count; ++i) {
			float t = r.nextFloat() * 2.0f - 1.0f;
			Vector3f offset = (switch (r.nextInt(4)) {
				case 0 -> new Vector3f(planex).add(new Vector3f(planey).mul(t));
				case 1 -> new Vector3f(planex).negate().add(new Vector3f(planey).mul(t));
				case 2 -> new Vector3f(planey).add(new Vector3f(planex).mul(t));
				default -> new Vector3f(planey).negate().add(new Vector3f(planex).mul(t));
			});
			Vector3f heading = new Vector3f(offset)
				.mul(1f / Mth.sqrt(1+t*t) * (r.nextFloat() * 0.5f + 0.5f))
				.add(new Vector3f(planez).mul((r.nextFloat()-0.5f) * 0.2f));
			offset.mul(distance);

			world.addParticle(
				SparkParticleData.INSTANCE,
				x + offset.x + 0.5,
				y + offset.y + height + (r.nextFloat()-0.5f) * 0.08f,
				z + offset.z + 0.5,
				heading.x, heading.y, heading.z
			);
		}
	}
	public static Direction tangent(Direction dir) {
		return switch (dir.ordinal()) {
			case 0 -> Direction.EAST;
			case 1 -> Direction.WEST;
			case 2 -> Direction.UP;
			case 3 -> Direction.DOWN;
			case 4 -> Direction.SOUTH;
			case 5 -> Direction.NORTH;
			default -> null;
		};
	}
	public static Direction bitangent(Direction dir) {
		return switch (dir.ordinal()) {
			case 0 -> Direction.NORTH;
			case 1 -> Direction.SOUTH;
			case 2 -> Direction.WEST;
			case 3 -> Direction.EAST;
			case 4 -> Direction.DOWN;
			case 5 -> Direction.UP;
			default -> null;
		};
	}
}
