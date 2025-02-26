package me.mixces.entityculling;

import me.mixces.entityculling.handler.CullRunnable;
import net.ornithemc.osl.entrypoints.api.ModInitializer;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;

public class EntityCulling implements ModInitializer {

	public CullRunnable runnable;
	private Thread thread;

	@Override
	public void init() {
//		runnable = new CullRunnable();
//		thread = new Thread(runnable,"Entity culling thread");
//		thread.setUncaughtExceptionHandler((t, ex) -> ex.printStackTrace());
//
//		MinecraftClientEvents.TICK_END.register(minecraft -> {
//			if (!thread.isAlive()) {
//				thread.start();
//			}
//		});
	}
}
