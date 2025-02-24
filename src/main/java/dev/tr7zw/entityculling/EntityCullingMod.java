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
import net.ornithemc.osl.entrypoints.api.ModInitializer;
import net.ornithemc.osl.lifecycle.api.client.ClientWorldEvents;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

public class EntityCullingMod implements ModInitializer {

	public boolean requestCull = false;

	private final OcclusionCullingInstance culling = new OcclusionCullingInstance(128, new Provider());
	private final Minecraft client = Minecraft.getInstance();
//	private final int sleepDelay = 10;
	private final int hitboxLimit = 50;
	public long lastTime = 0;

	// reused preallocated vars
	private final Vec3d lastPos = new Vec3d(0, 0, 0);
	private final Vec3d aabbMin = new Vec3d(0, 0, 0);
	private final Vec3d aabbMax = new Vec3d(0, 0, 0);

	@Override
	public void init() {
		MinecraftClientEvents.TICK_START.register(client ->
			task()
		);
//		MinecraftClientEvents.TICK_START.register(client ->
//			EntityCullingModBase.instance.clientTick()
//		);
//		ClientWorldEvents.TICK_START.register(clientWorld ->
//			EntityCullingModBase.instance.worldTick()
//		);
	}

	private void task() {
		if (client != null && client.world != null) {
			System.out.println("WSIJWSIJWISJ");
			net.minecraft.util.math.Vec3d cameraMC = client.getCamera().getEyePosition(1.0f);
			if (requestCull || !(cameraMC.x == lastPos.x && cameraMC.y == lastPos.y && cameraMC.z == lastPos.z)) {
				long start = System.currentTimeMillis();
				requestCull = false;
				lastPos.set(cameraMC.x, cameraMC.y, cameraMC.z);
				Vec3d camera = lastPos;
				culling.resetCache();
				boolean noCulling = client.player.isSpectator() || client.options.perspective != 0;
				Iterator<BlockEntity> iterator = client.world.blockEntities.iterator();
				BlockEntity entry;
				while(iterator.hasNext()) {
					try {
						entry = iterator.next();
					} catch(NullPointerException | ConcurrentModificationException ex) {
						break; // We are not synced to the main thread, so NPE's/CME are allowed here and way less
						// overhead probably than trying to sync stuff up for no really good reason
					}
					Cullable cullable = (Cullable) entry;
					if (!cullable.isForcedVisible()) {
						if (noCulling) {
							cullable.setCulled(false);
							continue;
						}
						BlockPos pos = entry.getPos();
						if (pos.squaredDistanceTo(cameraMC.x, cameraMC.y, cameraMC.z) < 64*64) { // 64 is the fixed max tile view distance
							aabbMin.set(pos.getX(), pos.getY(), pos.getZ());
							aabbMax.set(pos.getX()+1d, pos.getY()+1d, pos.getZ()+1d);
							boolean visible = culling.isAABBVisible(aabbMin, aabbMax, camera);
							cullable.setCulled(!visible);
						}

					}
				}
				Entity entity = null;
				Iterator<Entity> iterable = client.world.getEntities().iterator();
				while (iterable.hasNext()) {
					try {
						entity = iterable.next();
					} catch (NullPointerException | ConcurrentModificationException ex) {
						break; // We are not synced to the main thread, so NPE's/CME are allowed here and way less
						// overhead probably than trying to sync stuff up for no really good reason
					}
					if (!(entity instanceof Cullable)) {
						continue; // Not sure how this could happen outside from mixin screwing up the inject into Entity
					}
					Cullable cullable = (Cullable) entity;
					if (!cullable.isForcedVisible()) {
						if (noCulling || isSkippableArmorstand(entity)) {
							cullable.setCulled(false);
							continue;
						}
						if (entity.getSourcePos().squaredDistanceTo(cameraMC) > 128) {
							cullable.setCulled(false); // If your entity view distance is larger than tracingDistance just render it
							continue;
						}
						Box boundingBox = entity.getShape();
						if (boundingBox.maxX - boundingBox.minX > hitboxLimit || boundingBox.maxY - boundingBox.minY > hitboxLimit || boundingBox.maxZ - boundingBox.minZ > hitboxLimit) {
							cullable.setCulled(false); // To big to bother to cull
							continue;
						}
						aabbMin.set(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
						aabbMax.set(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
						boolean visible = culling.isAABBVisible(aabbMin, aabbMax, camera);
						cullable.setCulled(!visible);
					}
				}
				lastTime = (System.currentTimeMillis() - start);
			}
		}
	}

	private boolean isSkippableArmorstand(Entity entity) {
		return entity instanceof ArmorStandEntity && ((ArmorStandEntity) entity).isMarker();
	}
}
