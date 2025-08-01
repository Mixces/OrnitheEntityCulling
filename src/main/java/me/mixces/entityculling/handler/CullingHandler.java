package me.mixces.entityculling.handler;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tessellator;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.ArmorStandEntity;
import net.minecraft.entity.living.mob.hostile.boss.EnderDragonEntity;
import net.minecraft.entity.living.mob.hostile.boss.WitherEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/* adapted from patcher */
public class CullingHandler {
	public static final CullingHandler INSTANCE = new CullingHandler();
	private final Minecraft minecraft = Minecraft.getInstance();
	private final EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
	private final ConcurrentHashMap<Object, OcclusionQuery> queries = new ConcurrentHashMap<>();
	private final ArrayDeque<Integer> queryPool = new ArrayDeque<>();
	private int destroyTimer;
	private final int delay = 25;
	public boolean shouldPerformCulling = false;
	private static final double DISTANCE_THRESHOLD_SQ = 4096.0;
	private static final Box UNIT_BOX = new Box(0, 0, 0, 1, 1, 1);
	private final Set<UUID> tempEntityIds = new HashSet<>();
	private final Set<BlockPos> tempBlockPositions = new HashSet<>();
	private final List<Object> keysToRemove = new ArrayList<>();

	public boolean shouldCullEntity(Entity entity) {
		if (!shouldPerformCulling || entity == minecraft.player || entity.world != minecraft.player.world) {
			return false;
		}
		if (entity.getSquaredDistanceTo(minecraft.player) > DISTANCE_THRESHOLD_SQ) {
			return true;
		}
		if (isSpecialEntity(entity)) {
			return false;
		}
		return performOcclusionQuery(entity.getUuid(), () -> {
			Box entityBox = entity.getShape();
			Box expandedBox = entityBox.expand(0.2, 0.2, 0.2);
			Box translatedBox = expandedBox.move(-dispatcher.cameraX, -dispatcher.cameraY, -dispatcher.cameraZ);
			drawSelectionBoundingBox(translatedBox);
		});
	}

	private boolean isSpecialEntity(Entity entity) {
		if (entity instanceof ArmorStandEntity) {
			return ((ArmorStandEntity) entity).isMarker();
		}
		return entity.isInvisibleTo(minecraft.player) ||
			entity instanceof WitherEntity ||
			entity instanceof EnderDragonEntity;
	}

	public boolean shouldCullBlockEntity(BlockEntity blockEntity) {
		if (!shouldPerformCulling || blockEntity.getWorld() != minecraft.player.world) {
			return false;
		}
		return performOcclusionQuery(blockEntity.getPos(), () -> {
			BlockPos pos = blockEntity.getPos();
			Box box = UNIT_BOX.move(
				pos.getX() - dispatcher.cameraX,
				pos.getY() - dispatcher.cameraY,
				pos.getZ() - dispatcher.cameraZ
			);
			drawSelectionBoundingBox(box);
		});
	}

	private boolean performOcclusionQuery(Object key, Runnable drawBounds) {
		OcclusionQuery query = queries.computeIfAbsent(key, k -> new OcclusionQuery());
		if (query.refresh) {
			synchronized (queryPool) {
				query.nextQuery = !queryPool.isEmpty() ? queryPool.poll() : GL15.glGenQueries();
			}
			query.refresh = false;
			GL15.glBeginQuery(GL33.GL_ANY_SAMPLES_PASSED, query.nextQuery);
			drawBounds.run();
			GL15.glEndQuery(GL33.GL_ANY_SAMPLES_PASSED);
		}
		return query.occluded;
	}

	public void updateQueries() {
		long nanoTime = System.nanoTime() / 1_000_000L;
		for (OcclusionQuery query : queries.values()) {
			if (query.nextQuery != 0 && GL15.glGetQueryObjecti(query.nextQuery, GL15.GL_QUERY_RESULT_AVAILABLE) != 0) {
				query.occluded = GL15.glGetQueryObjecti(query.nextQuery, GL15.GL_QUERY_RESULT) == 0;
				synchronized (queryPool) {
					queryPool.offer(query.nextQuery);
				}
				query.nextQuery = 0;

			} else if (query.nextQuery == 0 && nanoTime - query.executionTime > delay) {
				query.executionTime = nanoTime;
				query.refresh = true;
			}
		}
	}

	public void cleanupQueries() {
		if (destroyTimer++ < 120) {
			return;
		}
		destroyTimer = 0;
		tempEntityIds.clear();
		tempBlockPositions.clear();
		keysToRemove.clear();
		for (Entity entity : minecraft.world.getEntities()) {
			tempEntityIds.add(entity.getUuid());
		}
		for (BlockEntity blockEntity : minecraft.world.blockEntities) {
			tempBlockPositions.add(blockEntity.getPos());
		}
		for (Map.Entry<Object, OcclusionQuery> entry : queries.entrySet()) {
			Object key = entry.getKey();
			boolean shouldRemove = (key instanceof UUID && !tempEntityIds.contains(key)) ||
				(key instanceof BlockPos && !tempBlockPositions.contains(key));
			if (shouldRemove) {
				keysToRemove.add(key);
				OcclusionQuery query = entry.getValue();
				if (query.nextQuery != 0) {
					GL15.glDeleteQueries(query.nextQuery);
				}
			}
		}
		for (Object key : keysToRemove) {
			queries.remove(key);
		}
	}

	public void drawSelectionBoundingBox(Box bb) {
		GlStateManager.disableAlphaTest();
		GlStateManager.disableCull();
		GlStateManager.depthMask(false);
		GlStateManager.colorMask(false, false, false, false);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder worldRenderer = tessellator.getBuilder();
		worldRenderer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormat.POSITION);
		double minX = bb.minX, minY = bb.minY, minZ = bb.minZ;
		double maxX = bb.maxX, maxY = bb.maxY, maxZ = bb.maxZ;
		worldRenderer.vertex(maxX, maxY, maxZ).nextVertex();
		worldRenderer.vertex(maxX, maxY, minZ).nextVertex();
		worldRenderer.vertex(minX, maxY, maxZ).nextVertex();
		worldRenderer.vertex(minX, maxY, minZ).nextVertex();
		worldRenderer.vertex(minX, minY, maxZ).nextVertex();
		worldRenderer.vertex(minX, minY, minZ).nextVertex();
		worldRenderer.vertex(minX, maxY, minZ).nextVertex();
		worldRenderer.vertex(minX, minY, minZ).nextVertex();
		worldRenderer.vertex(maxX, maxY, minZ).nextVertex();
		worldRenderer.vertex(maxX, minY, minZ).nextVertex();
		worldRenderer.vertex(maxX, maxY, maxZ).nextVertex();
		worldRenderer.vertex(maxX, minY, maxZ).nextVertex();
		worldRenderer.vertex(minX, maxY, maxZ).nextVertex();
		worldRenderer.vertex(minX, minY, maxZ).nextVertex();
		worldRenderer.vertex(minX, minY, maxZ).nextVertex();
		worldRenderer.vertex(maxX, minY, maxZ).nextVertex();
		worldRenderer.vertex(minX, minY, minZ).nextVertex();
		worldRenderer.vertex(maxX, minY, minZ).nextVertex();
		tessellator.end();
		GlStateManager.depthMask(true);
		GlStateManager.colorMask(true, true, true, true);
		GlStateManager.enableAlphaTest();
	}

	public static class OcclusionQuery {
		private int nextQuery;
		private boolean refresh = true;
		private boolean occluded;
		private long executionTime = 0;
	}
}
