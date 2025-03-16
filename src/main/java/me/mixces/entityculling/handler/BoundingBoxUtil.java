package me.mixces.entityculling.handler;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tessellator;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;

public class BoundingBoxUtil {
	public static void drawSelectionBoundingBox(Box bb) {
		GlStateManager.disableAlphaTest();
		GlStateManager.disableCull();
		GlStateManager.depthMask(false);
		GlStateManager.colorMask(false, false, false, false);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder worldRenderer = tessellator.getBuilder();
		worldRenderer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormat.POSITION);
		worldRenderer.vertex(bb.maxX, bb.maxY, bb.maxZ).nextVertex();
		worldRenderer.vertex(bb.maxX, bb.maxY, bb.minZ).nextVertex();
		worldRenderer.vertex(bb.minX, bb.maxY, bb.maxZ).nextVertex();
		worldRenderer.vertex(bb.minX, bb.maxY, bb.minZ).nextVertex();
		worldRenderer.vertex(bb.minX, bb.minY, bb.maxZ).nextVertex();
		worldRenderer.vertex(bb.minX, bb.minY, bb.minZ).nextVertex();
		worldRenderer.vertex(bb.minX, bb.maxY, bb.minZ).nextVertex();
		worldRenderer.vertex(bb.minX, bb.minY, bb.minZ).nextVertex();
		worldRenderer.vertex(bb.maxX, bb.maxY, bb.minZ).nextVertex();
		worldRenderer.vertex(bb.maxX, bb.minY, bb.minZ).nextVertex();
		worldRenderer.vertex(bb.maxX, bb.maxY, bb.maxZ).nextVertex();
		worldRenderer.vertex(bb.maxX, bb.minY, bb.maxZ).nextVertex();
		worldRenderer.vertex(bb.minX, bb.maxY, bb.maxZ).nextVertex();
		worldRenderer.vertex(bb.minX, bb.minY, bb.maxZ).nextVertex();
		worldRenderer.vertex(bb.minX, bb.minY, bb.maxZ).nextVertex();
		worldRenderer.vertex(bb.maxX, bb.minY, bb.maxZ).nextVertex();
		worldRenderer.vertex(bb.minX, bb.minY, bb.minZ).nextVertex();
		worldRenderer.vertex(bb.maxX, bb.minY, bb.minZ).nextVertex();
		tessellator.end();
		GlStateManager.depthMask(true);
		GlStateManager.colorMask(true, true, true, true);
		GlStateManager.enableAlphaTest();
	}
}
