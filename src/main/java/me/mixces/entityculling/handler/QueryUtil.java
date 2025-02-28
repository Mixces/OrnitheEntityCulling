package me.mixces.entityculling.handler;

import org.lwjgl.opengl.GL15;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueryUtil {
	private static final Queue<Integer> queryPool = new ConcurrentLinkedQueue<>();

	public static int getQuery() {
		if (!queryPool.isEmpty()) {
			return queryPool.poll();
		}
		return GL15.glGenQueries();
	}
}
