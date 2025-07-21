package me.mixces.entityculling.handler;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.ArmorStandEntity;
import net.minecraft.entity.living.mob.hostile.boss.EnderDragonEntity;
import net.minecraft.entity.living.mob.hostile.boss.WitherEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/* adapted from patcher */
public class CullingHandler {
	public static final CullingHandler INSTANCE = new CullingHandler();
	private final Minecraft minecraft = Minecraft.getInstance();
	private final EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
	private final ConcurrentHashMap<Object, OcclusionQuery<?>> queries = new ConcurrentHashMap<>();
	private int destroyTimer;
	public boolean shouldPerformCulling = false;

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

		return performOcclusionQuery(entity.getUuid(), () -> {
			Box box = entity.getShape().expand(.2, .2, .2).move(-dispatcher.cameraX, -dispatcher.cameraY, -dispatcher.cameraZ);
			BoundingBoxUtil.drawSelectionBoundingBox(box);
		});
	}

	public boolean shouldCullTileEntity(BlockEntity tileEntity) {
		if (!shouldPerformCulling || tileEntity.getWorld() != minecraft.player.world) {
			return false;
		}

		return performOcclusionQuery(tileEntity.getPos(), () -> {
			BlockPos pos = tileEntity.getPos();
			Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
			box = box.move(-dispatcher.cameraX, -dispatcher.cameraY, -dispatcher.cameraZ);
			BoundingBoxUtil.drawSelectionBoundingBox(box);
		});
	}

	private boolean performOcclusionQuery(Object key, Runnable drawBounds) {
		OcclusionQuery<?> query = queries.computeIfAbsent(key, OcclusionQuery::new);
		if (query.refresh) {
			query.nextQuery = query.getQuery();
			query.refresh = false;
			GL15.glBeginQuery(GL33.GL_ANY_SAMPLES_PASSED, query.nextQuery);
			drawBounds.run();
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

		Set<UUID> entityIds = minecraft.world.getEntities().stream().map(Entity::getUuid).collect(Collectors.toSet());
		Set<BlockPos> blockPositions = minecraft.world.blockEntities.stream().map(BlockEntity::getPos).collect(Collectors.toSet());

		queries.entrySet().removeIf(entry -> {
			Object key = entry.getKey();
			OcclusionQuery<?> query = entry.getValue();

			boolean shouldRemove = (key instanceof UUID && !entityIds.contains(key)) ||
				(key instanceof BlockPos && !blockPositions.contains(key));

			if (shouldRemove && query.nextQuery != 0) {
				GL15.glDeleteQueries(query.nextQuery);
			}
			return shouldRemove;
		});
	}
}
