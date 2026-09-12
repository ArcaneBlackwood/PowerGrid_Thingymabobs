package dev.thingymabobs.blocks.Zoey.PlasmaGlobe;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.world.phys.Vec3;

/**
 * Pure data/animation model for a single plasma arc inside the globe.
 * No rendering code lives here — PlasmaGlobeRenderer reads this to draw.
 *
 * NOTE: this used to be your renderer class name. PlasmaGlobeEntity's
 * `List<PlasmaTendril> tendrils` field expects a data holder, not a
 * BlockEntityRenderer, so the renderer got renamed to PlasmaGlobeRenderer.
 */
public class PlasmaTendril {

    public final Vec3 start;
    public final Vec3 end;
    public final long seed;
    public final int lifetime;
    public int age;

    public PlasmaTendril(Vec3 start, Vec3 end, long seed, int lifetime) {
        this.start = start;
        this.end = end;
        this.seed = seed;
        this.lifetime = lifetime;
        this.age = 0;
    }
}