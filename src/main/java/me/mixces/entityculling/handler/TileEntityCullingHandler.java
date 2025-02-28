package me.mixces.entityculling.handler;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/* adapted from patcher */
public class TileEntityCullingHandler {
	public static final TileEntityCullingHandler INSTANCE = new TileEntityCullingHandler();
	private final Minecraft minecraft = Minecraft.getInstance();
	private final EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
    private final ConcurrentHashMap<BlockPos, OcclusionQuery> queries = new ConcurrentHashMap<>();
    private int destroyTimer;
    private boolean shouldPerformCulling = false;

    public boolean shouldCullTileEntity(BlockEntity tileEntity) {
        if (!shouldPerformCulling || tileEntity.getWorld() != minecraft.player.world) {
            return false;
        }
		OcclusionQuery query = queries.computeIfAbsent(tileEntity.getPos(), OcclusionQuery::new);
		if (query.refresh) {
			query.nextQuery = QueryUtil.getQuery();
			query.refresh = false;
			GL15.glBeginQuery(GL33.GL_ANY_SAMPLES_PASSED, query.nextQuery);
			BlockPos pos = tileEntity.getPos();
			Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
			box = box.move(-dispatcher.cameraX, -dispatcher.cameraY, -dispatcher.cameraZ);
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
        Set<BlockPos> blockEntities = minecraft.world.blockEntities.stream().map(BlockEntity::getPos).collect(Collectors.toSet());
		queries.entrySet().removeIf(entry -> {
			OcclusionQuery query = entry.getValue();
			if (!blockEntities.contains(query.pos)) {
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
        private final BlockPos pos; // Owner
        private int nextQuery;
        private boolean refresh = true;
        private boolean occluded;
        private long executionTime = 0;

        public OcclusionQuery(BlockPos pos) {
            this.pos = pos;
        }
    }
}
