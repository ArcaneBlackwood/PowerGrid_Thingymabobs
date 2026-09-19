package dev.thingymabobs.util;

import org.joml.Vector2f;
import org.joml.Vector3f;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BakedQuadEditor {
	public int[] vertices;
	public VertexFormat format;
	public final int size;
	public BakedQuad base;


	public BakedQuadEditor(BakedQuad quad, VertexFormat format) {
		vertices = quad.getVertices().clone();
		this.format = format;
		size = format.getVertexSize() / 4;
		this.base = quad;
	}
	public BakedQuad compile() {
		return new BakedQuad(vertices, base.getTintIndex(),
			base.getDirection(), base.getSprite(), base.isShade());
	}

	public Vector3f getPosition(int index) {
		if (!format.hasPosition()) return null;
		int offset = index * size + format.getOffset(VertexFormatElement.POSITION) / 4;
		return new Vector3f(Float.intBitsToFloat(vertices[offset]),
			Float.intBitsToFloat(vertices[offset + 1]),
			Float.intBitsToFloat(vertices[offset + 2]));
	}
	public void setPosition(int index, Vector3f pos) {
		if (!format.hasPosition()) return;
		int offset = index * size + format.getOffset(VertexFormatElement.POSITION) / 4;
		vertices[offset] = Float.floatToIntBits(pos.x);
		vertices[offset + 1] = Float.floatToIntBits(pos.y);
		vertices[offset + 2] = Float.floatToIntBits(pos.z);
	}
	public Vector2f getUV(int index) {
		if (!format.hasUV(0)) return null;
		int offset = index * size + format.getOffset(VertexFormatElement.UV) / 4;
		return new Vector2f(Float.intBitsToFloat(vertices[offset]),
			Float.intBitsToFloat(vertices[offset + 1]));
	}
	public void setUV(int index, Vector2f pos) {
		if (!format.hasUV(0)) return;
		int offset = index * size + format.getOffset(VertexFormatElement.UV) / 4;
		vertices[offset] = Float.floatToIntBits(pos.x);
		vertices[offset + 1] = Float.floatToIntBits(pos.y);
	}
	public Vector3f getNormal(int index) {
		if (!format.hasNormal()) return null;
		int offset = index * size + format.getOffset(VertexFormatElement.NORMAL) / 4;
		return new Vector3f(Float.intBitsToFloat(vertices[offset]),
			Float.intBitsToFloat(vertices[offset + 1]),
			Float.intBitsToFloat(vertices[offset + 2]));
	}
	public void setNormal(int index, Vector3f pos) {
		if (!format.hasNormal()) return;
		int offset = index * size + format.getOffset(VertexFormatElement.NORMAL) / 4;
		vertices[offset] = Float.floatToIntBits(pos.x);
		vertices[offset + 1] = Float.floatToIntBits(pos.y);
		vertices[offset + 2] = Float.floatToIntBits(pos.z);
	}
	public Vector3f getColor(int index) {
		if (!format.hasColor()) return null;
		int offset = index * size + format.getOffset(VertexFormatElement.COLOR) / 4;
		return new Vector3f(Float.intBitsToFloat(vertices[offset]),
			Float.intBitsToFloat(vertices[offset + 1]),
			Float.intBitsToFloat(vertices[offset + 2]));
	}
	public void setColor(int index, Vector3f pos) {
		if (!format.hasColor()) return;
		int offset = index * size + format.getOffset(VertexFormatElement.COLOR) / 4;
		vertices[offset] = Float.floatToIntBits(pos.x);
		vertices[offset + 1] = Float.floatToIntBits(pos.y);
		vertices[offset + 2] = Float.floatToIntBits(pos.z);
	}
	public BoundingBox getBounds() {
		if (!format.hasPosition() || vertices.length < size)
			return new BoundingBox(0,0,0,0,0,0);
		float minX = 2, minY = 2, minZ = 2, maxX = -1, maxY = -1, maxZ = -1;
		final int offsetPos = format.getOffset(VertexFormatElement.POSITION) / 4;
		for (int index = 0, end = vertices.length; index < end; index += size) {
			int offset = index + offsetPos;
			float x = Float.intBitsToFloat(vertices[offset]),
				y = Float.intBitsToFloat(vertices[offset+1]),
				z = Float.intBitsToFloat(vertices[offset+2]);
			if (x < minX) minX = x;
			if (x > maxX) maxX = x;
			if (y < minY) minY = y;
			if (y > maxY) maxY = y;
			if (z < minZ) minZ = z;
			if (z > maxZ) maxZ = z;
		}
		float halfPixel = 0.5f / 16f;
		return new BoundingBox(
			(int)((minX+halfPixel)*16), (int)((minY+halfPixel)*16), (int)((minZ+halfPixel)*16),
			(int)((maxX+halfPixel)*16), (int)((maxY+halfPixel)*16), (int)((maxZ+halfPixel)*16));
	}
}
