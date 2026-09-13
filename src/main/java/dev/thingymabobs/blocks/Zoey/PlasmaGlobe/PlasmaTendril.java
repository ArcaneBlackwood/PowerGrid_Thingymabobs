package dev.thingymabobs.blocks.Zoey.PlasmaGlobe;

import org.joml.Vector3f;
import net.minecraft.util.RandomSource;

public class PlasmaTendril {

	// Needs system to generate points and draw line to faces

	// END POINTS of tendril before and after lifetime
	// X,Y,Z angles
    private final Vector3f start; // Start point
    private final Vector3f end; // End point 
	private Vector3f current; // Current point in movment

    private final int lifetime; // Lifetime of the tendril
	private final int sprite; // Index of selected sprite
    private int age; // Age of current tendril

	// Which axis (0=X,1=Y,2=Z) and sign (+1/-1) a face sits on.
	private record Face(int axis, int sign) {};

	private static final float FACE = 1.0f;



	// Tendril sprite counts
	public static final int TendrilSpriteCount = 4; // total count

    public PlasmaTendril(RandomSource random) {
		Face startFace = RandomFace(random);
        this.start = PointOnFace(random, startFace);
        this.end = PointOnFace(random, RandomAdjacentOrSameFace(random, startFace));
		this.current = new Vector3f(this.start);

        this.lifetime = random.nextInt(
			PlasmaGlobeEntity.TRNDRIL_LIFE - PlasmaGlobeEntity.TRNDRIL_LIFE_VARY, 
			PlasmaGlobeEntity.TRNDRIL_LIFE + PlasmaGlobeEntity.TRNDRIL_LIFE_VARY
		);
		this.sprite = random.nextInt(PlasmaTendril.TendrilSpriteCount);
        this.age = 0;
    };

	///TODO: Suggestion, avoid division by changing age to float and range from 0-1.  And create a new float speed thats set to 1f/lifetime.  Since the longer the lifetime, the slower it takes to complete.
	// True if needs destruction, else just steps
	public boolean Step() {
		if (age >= lifetime) { return true; } else { age++; };

		float t = (float) age / (float) lifetime; // movment calc

		current.set(start).lerp(end, t);
		ProjectOntoCubeSurface(current);

		return false;
	};


	// Picks a uniformly random face from the cube
	private static Face RandomFace(RandomSource random) {
		int axis = random.nextInt(3);
		int sign = random.nextBoolean() ? 1 : -1;
		return new Face(axis, sign);
	};


	// Pics at random, same or adjacent face for the end point
	private static Face RandomAdjacentOrSameFace(RandomSource random, Face from) {
		// 0 = same face, 1..4 = the four faces on the other two axes (both signs)
		int choice = random.nextInt(5);
		if (choice == 0) {
			return from;
		};

		int otherAxisPick = (choice - 1) / 2; // 0 or 1, picks between the two remaining axes
		int sign = ((choice - 1) % 2 == 0) ? 1 : -1;

		int[] otherAxes = new int[2];
		int idx = 0;
		for (int axis = 0; axis < 3; axis++) {
			if (axis != from.axis()) {
				otherAxes[idx++] = axis;
			};
		};

		return new Face(otherAxes[otherAxisPick], sign);
	};


	// Random point lying on the given face: the face's axis is pinned to +/-1,
	// the other two axes are randomized across the full [-1, 1] range.
	private static Vector3f PointOnFace(RandomSource random, Face face) {
		Vector3f point = new Vector3f(
				random.nextFloat() * 2f - 1f,
				random.nextFloat() * 2f - 1f,
				random.nextFloat() * 2f - 1f
		);

		switch (face.axis()) {
			case 0 -> point.x = FACE * face.sign();
			case 1 -> point.y = FACE * face.sign();
			case 2 -> point.z = FACE * face.sign();
		}

		return point;
	};


	// Radially rescales a point so its largest-magnitude axis sits exactly on
	// the cube surface (+/-1), pulling it back onto whichever face it's nearest.
	private static void ProjectOntoCubeSurface(Vector3f point) {
		float maxAbs = Math.max(Math.abs(point.x), Math.max(Math.abs(point.y), Math.abs(point.z)));
		if (maxAbs > 1.0e-6f) {
			point.div(maxAbs / FACE);
		}
	};


	public Vector3f GetCurrentPoint() {
		return current;
	};



};