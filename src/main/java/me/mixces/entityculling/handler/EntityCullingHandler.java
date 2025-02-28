package me.mixces.entityculling.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.ArmorStandEntity;
import net.minecraft.entity.living.mob.hostile.boss.EnderDragonEntity;
import net.minecraft.entity.living.mob.hostile.boss.WitherEntity;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/* adapted from patcher */
public class EntityCullingHandler {
	public static final EntityCullingHandler INSTANCE = new EntityCullingHandler();
	private final Minecraft minecraft = Minecraft.getInstance();
	private final EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
	private final ConcurrentHashMap<UUID, OcclusionQuery> queries = new ConcurrentHashMap<>();
	private int destroyTimer;
	private boolean shouldPerformCulling = false;

	public boolean shouldCullEntity(Entity entity) {
		if (!shouldPerformCulling || entity == minecraft.player || entity.world != minecraft.player.world) {
			return false;
		}
		if (entity.getSquaredDistanceTo(minecraft.player) > 4096) {
			return true;
		}
		if ((entity instanceof ArmorStandEntity && ((ArmorStandEntity) entity).isMarker()) ||
			(entity.isInvisibleTo(minecraft.player) && !(entity instanceof ArmorStandEntity)) ||
			(entity instanceof WitherEntity) || (entity instanceof EnderDragonEntity)) {
			return false;
		}
		OcclusionQuery query = queries.computeIfAbsent(entity.getUuid(), OcclusionQuery::new);
		if (query.refresh) {
			query.nextQuery = QueryUtil.getQuery();
			query.refresh = false;
			GL15.glBeginQuery(GL33.GL_ANY_SAMPLES_PASSED, query.nextQuery);
			Box box = entity.getShape().expand(.2, .2, .2).move(-dispatcher.cameraX, -dispatcher.cameraY, -dispatcher.cameraZ);
			BoundingBoxUtil.drawSelectionBoundingBox(box);
			GL15.glEndQuery(GL33.GL_ANY_SAMPLES_PASSED);
		}
		return query.occluded;
	}

	public void updateQueries() {
		long nanoTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
		queries.values().forEach(query -> {
			if (query.nextQuery != 0 && GL15.glGetQueryObjecti(query.nextQuery, GL15.GL_QUERY_RESULT_AVAILABLE) != 0) {
				query.occluded = GL15.glGetQueryObjecti(query.nextQuery, GL15.GL_QUERY_RESULT) == 0;
				GL15.glDeleteQueries(query.nextQuery);
				query.nextQuery = 0;
			} else if (query.nextQuery == 0 && nanoTime - query.executionTime > 25) {
				query.executionTime = nanoTime;
				query.refresh = true;
			}
		});
	}

	public void cleanupQueries() {
		if (destroyTimer++ < 120) {
			return;
		}
		destroyTimer = 0;
		Set<UUID> entities = minecraft.world.getEntities().stream().map(Entity::getUuid).collect(Collectors.toSet());
		queries.entrySet().removeIf(entry -> {
			OcclusionQuery query = entry.getValue();
			if (!entities.contains(query.uuid)) {
				if (query.nextQuery != 0) {
					GL15.glDeleteQueries(query.nextQuery);
				}
				return true;
			}
			return false;
		});
	}

	public void setShouldPerformCulling(boolean shouldPerformCulling) {
		this.shouldPerformCulling = shouldPerformCulling;
	}

	private static class OcclusionQuery {
		private final UUID uuid;
		private int nextQuery;
		private boolean refresh = true;
		private boolean occluded;
		private long executionTime = 0;

		public OcclusionQuery(UUID uuid) {
			this.uuid = uuid;
		}
	}
}
