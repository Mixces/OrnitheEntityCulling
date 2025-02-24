package dev.tr7zw.entityculling;

import com.logisticscraft.occlusionculling.OcclusionCullingInstance;

public class EntityCullingModBase {

	public static EntityCullingModBase instance = new EntityCullingModBase();
//	public OcclusionCullingInstance culling;
//	public CullTask cullTask;
	private Thread cullThread;
	public int renderedBlockEntities = 0;
	public int skippedBlockEntities = 0;
	public int renderedEntities = 0;
	public int skippedEntities = 0;

	public void onInitialize() {
		instance = this;
//		culling = new OcclusionCullingInstance(128, new Provider());
//		cullTask = new CullTask(culling);

//		cullThread = new Thread(cullTask, "CullThread");
		cullThread.setUncaughtExceptionHandler((thread, ex) -> {
			System.out.println("The CullingThread has crashed! Please report the following stacktrace!");
			ex.printStackTrace();
		});
		cullThread.start();
	}

//	public void worldTick() {
//		cullTask.requestCull = true;
//	}
//
//	public void clientTick() {
//		cullTask.requestCull = true;
//	}
}
