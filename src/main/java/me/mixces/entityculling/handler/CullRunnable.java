package me.mixces.entityculling.handler;

import me.mixces.entityculling.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.World;

public class CullRunnable implements Runnable {

	@Override
	public void run() {
		while (Minecraft.getInstance() != null && ((MinecraftAccessor) Minecraft.getInstance()).getRunning()) {
			try {
				Thread.sleep(25);
				Minecraft minecraft = Minecraft.getInstance();
				World world = minecraft.world;
				if (minecraft != null && world != null) {
					CullHandler.CULL_RESULTS.keySet().removeIf(networkId -> world.getEntity(networkId).getUuid() == null);
					Vec3d vecCamera = minecraft.getCamera().getEyePosition(1.0F);
					for (Entity entity : world.getEntities()) {
						if (entity == null) continue;
						Vec3d vecEntity = entity.getEyePosition(1.0F);
						HitResult result = world.rayTrace(vecCamera, vecEntity);
						boolean shouldCull = result == null || (result.type == HitResult.Type.ENTITY && result.entity == entity);
						CullHandler.CULL_RESULTS.put(entity.getNetworkId(), shouldCull);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
