package dev.thingymabobs.util;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.FastColor.ABGR32;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BakedQuadEditor {
	public int[] vertices;
	public VertexFormat format;
	public final int size;
	public BakedQuad base;
    public float[] brightness;
    public int[] lightmap;
	public final int offsetPos, offsetUV, offsetNorm, offsetColor;
	public final int uvWidth, uvHeight;


	public BakedQuadEditor(BakedQuad base, int[] vertices, VertexFormat format) {
		this.vertices = vertices;
		this.format = format;
		size = format.getVertexSize() / 4;
		this.base = base;
		offsetPos = format.getOffset(VertexFormatElement.POSITION) / 4;
		offsetUV = format.getOffset(VertexFormatElement.UV) / 4;
		offsetNorm = format.getOffset(VertexFormatElement.NORMAL) / 4;
		offsetColor = format.getOffset(VertexFormatElement.COLOR) / 4;
		var sprite = base.getSprite();
		var spriteContent = sprite.contents();
		uvWidth = spriteContent.width();
		uvHeight = spriteContent.height();
	}
	public BakedQuad compile() {
		return new BakedQuad(vertices, base.getTintIndex(),
			base.getDirection(), base.getSprite(), base.isShade());
	}
	public static BakedQuadEditor combine(BakedQuad base, BakedQuadEditor[] quads) {
		if (quads.length == 0) return null;
		int totalVerttices = 0;
		for (BakedQuadEditor quad : quads) 
			totalVerttices += quad.size;
		final int[] vertices = new int[totalVerttices];
		for (int i=0, j=0, k=0; i < totalVerttices; i++) {
			vertices[i] = quads[k].vertices[j++];
			if (j >= quads[k].size) {
				j = 0;
				k++;
			}
		}
		return new BakedQuadEditor(base, vertices, quads[0].format);
	}
	public static BakedQuadEditor fromClone(BakedQuad quad, VertexFormat format) {
		return new BakedQuadEditor(quad, quad.getVertices().clone(), format);
	}
	public static BakedQuadEditor fromLinked(BakedQuad quad, VertexFormat format) {
		return new BakedQuadEditor(quad, quad.getVertices(), format);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Quad[");
		boolean notFirst = false;
		for (int i=0; i<vertices.length; i+=size) {
			if (notFirst) sb.append(", ");
			sb.append("#(");
			notFirst = true;
			int offset = i + offsetPos;
			sb.append("x").append(Float.intBitsToFloat(vertices[offset])*16);
			sb.append(" y").append(Float.intBitsToFloat(vertices[offset+1])*16);
			sb.append(" z").append(Float.intBitsToFloat(vertices[offset+2])*16);
			var sprite = base.getSprite();
			var spriteContent = sprite.contents();
			offset = i + offsetUV;
			sb.append(" u").append(sprite.getUOffset(Float.intBitsToFloat(vertices[offset])) * spriteContent.width());
			sb.append(" v").append(sprite.getVOffset(Float.intBitsToFloat(vertices[offset+1])) * spriteContent.height());
			offset = i + offsetNorm;
			int normal = vertices[offset];
			sb.append(" nx").append(fromNorm(normal, 0));
			sb.append(" ny").append(fromNorm(normal, 8));
			sb.append(" nz").append(fromNorm(normal, 16));
			offset = i + offsetColor;
			int colorABGR = vertices[offset];
			sb.append(" r").append(ABGR32.red(colorABGR));
			sb.append(" g").append(ABGR32.green(colorABGR));
			sb.append(" b").append(ABGR32.blue(colorABGR));
			sb.append(" a").append(ABGR32.alpha(colorABGR));
			sb.append(")");
		}
		sb.append("]");
		return sb.toString();
	}



	public Vector3f getPosition(int index) {
		if (!format.hasPosition()) return null;
		int offset = index * size + offsetPos;
		return new Vector3f(Float.intBitsToFloat(vertices[offset]),
			Float.intBitsToFloat(vertices[offset + 1]),
			Float.intBitsToFloat(vertices[offset + 2]));
	}
	public void setPosition(int index, Vector3f pos) {
		if (!format.hasPosition()) return;
		int offset = index * size + offsetPos;
		vertices[offset] = Float.floatToIntBits(pos.x);
		vertices[offset + 1] = Float.floatToIntBits(pos.y);
		vertices[offset + 2] = Float.floatToIntBits(pos.z);
	}
	public Vector2f getUV(int index) {
		if (!format.hasUV(0)) return null;
		int offset = index * size + offsetUV;
		var sprite = base.getSprite();
		return new Vector2f(
			sprite.getUOffset(Float.intBitsToFloat(vertices[offset])) * uvWidth,
			sprite.getVOffset(Float.intBitsToFloat(vertices[offset + 1])) * uvHeight);
	}
	public void setUV(int index, Vector2f pos) {
		if (!format.hasUV(0)) return;
		int offset = index * size +offsetUV;
		var sprite = base.getSprite();
		vertices[offset] = Float.floatToIntBits(sprite.getU(pos.x / uvWidth));
		vertices[offset + 1] = Float.floatToIntBits(sprite.getV(pos.y / uvHeight));
	}
	public Vector2i getUVSize() {
		return new Vector2i(uvWidth, uvHeight);
	}
	public Vector3f getNormal(int index) {
		if (!format.hasNormal()) return null;
		int offset = index * size + offsetNorm;
		int packed = vertices[offset];
		return new Vector3f(fromNorm(packed, 0),
			fromNorm(packed, 8),
			fromNorm(packed, 16));
	}
	public void setNormal(int index, Vector3f pos) {
		if (!format.hasNormal()) return;
		int offset = index * size + offsetNorm;
		vertices[offset] = 
			toNorm(pos.x(), 0) |
			toNorm(pos.y(), 8) |
			toNorm(pos.z(), 16);
	}
	protected int toNorm(float val, int offset) {
		if (val < -1) return -127;
		if (val > 1) return 127;
		return (Math.round(val * 127) & 0xFF) << offset;
	}
	protected float fromNorm(int val, int offset) {
		return ((val >> offset) & 0xFF) / 127f;
	}
	/**
	 * @return Returns ABGR
	 */
	public int getColor(int index) {
		if (!format.hasColor()) return 0;
		int offset = index * size + offsetColor;
		return vertices[offset];
	}
	public void setColor(int index, int colorABGR) {
		if (!format.hasColor()) return;
		int offset = index * size + offsetColor;
		vertices[offset] = colorABGR;
	}
	public Vector4f getColorVector(int index) {
		if (!format.hasColor()) return null;
		int offset = index * size + offsetColor;
		int colorABGR = vertices[offset];
		return new Vector4f(
			ABGR32.red(colorABGR),
			ABGR32.green(colorABGR),
			ABGR32.blue(colorABGR),
			ABGR32.alpha(colorABGR)
		);
	}
	public void setColorVector(int index, Vector4f color) {
		if (!format.hasColor()) return;
		int offset = index * size + offsetColor;
		vertices[offset] = ABGR32.color(
			(int)(color.x() * 255),
			(int)(color.y() * 255),
			(int)(color.z() * 255),
			(int)(color.w() * 255)
		);
	}
	public AABB getBounds() {
		if (!format.hasPosition() || vertices.length < size)
			return new AABB(0,0,0,0,0,0);
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
		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
	}
}
