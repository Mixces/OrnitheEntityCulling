package me.mixces.entityculling;

import net.ornithemc.osl.entrypoints.api.ModInitializer;

public class EntityCulling implements ModInitializer {

	/* debug */
	public static int renderedBlockEntities = 0;
	public static int culledBlockEntities = 0;
	public static int renderedEntities = 0;
	public static int culledEntities = 0;

	@Override
	public void init() {
		System.out.println("Entity Culling Initialized");
	}

	public static void resetDebugFields() {
		renderedBlockEntities = 0;
		culledBlockEntities = 0;
		renderedEntities = 0;
		culledEntities = 0;
	}
}
