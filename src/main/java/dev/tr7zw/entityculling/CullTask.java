package dev.tr7zw.entityculling;

import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.util.Vec3d;
import dev.tr7zw.entityculling.access.Cullable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.ArmorStandEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

public class CullTask implements Runnable {

	public boolean requestCull = false;

	private final OcclusionCullingInstance culling;
    private final Minecraft client = Minecraft.getInstance();
	private final int sleepDelay = 10;
	private final int hitboxLimit = 50;
	public long lastTime = 0;

	// reused preallocated vars
	private final Vec3d lastPos = new Vec3d(0, 0, 0);
	private final Vec3d aabbMin = new Vec3d(0, 0, 0);
	private final Vec3d aabbMax = new Vec3d(0, 0, 0);

	public CullTask(OcclusionCullingInstance culling) {
		this.culling = culling;
	}

	@Override
	public void run() {
		while (client != null) { // not correct, but the running field is hidden
			try {
				Thread.sleep(sleepDelay);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("Shutting down culling task!");
	}

	private boolean isSkippableArmorstand(Entity entity) {
	    return entity instanceof ArmorStandEntity && ((ArmorStandEntity) entity).isMarker();
	}
}
