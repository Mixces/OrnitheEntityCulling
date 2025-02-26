package me.mixces.entityculling.handler;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.ArmorStandEntity;
import net.minecraft.entity.living.mob.hostile.boss.EnderDragonEntity;
import net.minecraft.entity.living.mob.hostile.boss.WitherEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CullHandler {

	public static final Map<Integer, Boolean> ENTITY_CULL_RESULTS = new ConcurrentHashMap<>();
	public static final Map<BlockPos, Boolean> BLOCK_CULL_RESULTS = new ConcurrentHashMap<>();

	/* debug */
	public static int renderedBlockEntities = 0;
	public static int culledBlockEntities = 0;
	public static int renderedEntities = 0;
	public static int culledEntities = 0;

	public boolean shouldRenderEntity(World world, Entity camera, Entity entity) {
		if (isEntityBlacklisted(entity)) {
			return true;
		}

		Vec3d vecCamera = camera.getEyePosition(1.0F);
		Vec3d vecEntity = entity.getEyePosition(1.0F);
		HitResult result = world.rayTrace(vecCamera, vecEntity);
		return result == null || (result.type == HitResult.Type.ENTITY && result.entity == entity);
	}

	public boolean shouldRenderBlockEntity(World world, Entity camera, BlockEntity blockEntity) {
		Vec3d vecCamera = camera.getEyePosition(1.0F);
		Vec3d vecBlock = new Vec3d(blockEntity.getPos().getX() + 0.5D, blockEntity.getPos().getY() + 0.5D, blockEntity.getPos().getZ() + 0.5D);
		HitResult result = world.rayTrace(vecCamera, vecBlock);
		return result == null || (result.type == HitResult.Type.BLOCK && result.getPos().equals(blockEntity.getPos()));
	}

	public static void resetDebugFields() {
		renderedBlockEntities = 0;
		culledBlockEntities = 0;
		renderedEntities = 0;
		culledEntities = 0;
	}

	public static boolean isEntityBlacklisted(Entity entity) {
		/* for servers */
		return (entity instanceof ArmorStandEntity && entity.isInvisible()) ||
			(entity instanceof EnderDragonEntity) || (entity instanceof WitherEntity);
	}
}
