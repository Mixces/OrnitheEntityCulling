package me.mixces.entityculling.handler;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.ArmorStandEntity;
import net.minecraft.entity.living.mob.hostile.boss.EnderDragonEntity;
import net.minecraft.entity.living.mob.hostile.boss.WitherEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CullHandler {

	public static final Map<Integer, Boolean> CULL_RESULTS = new ConcurrentHashMap<>();

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
		Vec3d direction = vecEntity.subtract(vecCamera).normalize();
		Vec3d currentPos = new Vec3d(vecCamera.x, vecCamera.y, vecCamera.z);
		while (currentPos.distanceTo(vecEntity) > 0.1F) {
			BlockPos blockPos = new BlockPos(currentPos.x, currentPos.y, currentPos.z);
			Block block = world.getBlockState(blockPos).getBlock();
			if (block.isOpaqueCube()) {
				return false;
			}
			Vec3d scaledDirection = new Vec3d(direction.x * 0.1F, direction.y * 0.1F, direction.z * 0.1F);
			currentPos = currentPos.add(scaledDirection);
		}
		return true;
	}

	public boolean shouldRenderBlockEntity(World world, Entity camera, BlockEntity blockEntity) {
		Vec3d vecCamera = camera.getEyePosition(1.0F);
		Vec3d vecBlock = new Vec3d(blockEntity.getPos());
		Vec3d direction = vecBlock.subtract(vecCamera).normalize();
		Vec3d currentPos = new Vec3d(vecCamera.x, vecCamera.y, vecCamera.z);
		while (currentPos.distanceTo(vecBlock) > 0.1F) {
			BlockPos blockPos = new BlockPos(currentPos.x, currentPos.y, currentPos.z);
			Block block = world.getBlockState(blockPos).getBlock();
			if (block.isOpaqueCube()) {
				return false;
			}
			Vec3d scaledDirection = new Vec3d(direction.x * 0.1F, direction.y * 0.1F, direction.z * 0.1F);
			currentPos = currentPos.add(scaledDirection);
		}
		return true;
	}

//	public boolean shouldRenderEntity(World world, Entity camera, Entity entity) {
//		if (isEntityBlacklisted(entity)) {
//			return true;
//		}
//
//		Vec3d vecCamera = camera.getEyePosition(1.0F);
//		Vec3d vecEntity = entity.getEyePosition(1.0F);
//
//		HitResult result = world.rayTrace(vecCamera, vecEntity);
//
//		return result == null || (result.type == HitResult.Type.ENTITY && result.entity == entity);
//	}

//	public boolean shouldRenderBlockEntity(World world, Entity camera, BlockEntity blockEntity) {
//		Vec3d vecCamera = camera.getEyePosition(1.0F);
//		Vec3d vecBlock = new Vec3d(blockEntity.getPos().getX() + 0.5D, blockEntity.getPos().getY() + 0.5D, blockEntity.getPos().getZ() + 0.5D);
//
//		HitResult result = world.rayTrace(vecCamera, vecBlock);
//
//		return result == null || (result.type == HitResult.Type.BLOCK && result.getPos().equals(blockEntity.getPos()));
//	}

	private boolean isEntityBlacklisted(Entity entity) {
		/* for servers */
		return (entity instanceof ArmorStandEntity && entity.isInvisible()) ||
			(entity instanceof EnderDragonEntity) || (entity instanceof WitherEntity);
	}

	public static void resetDebugFields() {
		renderedBlockEntities = 0;
		culledBlockEntities = 0;
		renderedEntities = 0;
		culledEntities = 0;
	}
}
