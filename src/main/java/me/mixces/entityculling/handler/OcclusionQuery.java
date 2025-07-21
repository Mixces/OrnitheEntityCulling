package me.mixces.entityculling.handler;

import org.lwjgl.opengl.GL15;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OcclusionQuery<T> {
	public final T id;
	public int nextQuery;
	public boolean refresh = true;
	public boolean occluded;
	public long executionTime = 0;
	private final Queue<Integer> queryPool = new ConcurrentLinkedQueue<>();

	public OcclusionQuery(T id) {
		this.id = id;
	}

	public int getQuery() {
		if (!queryPool.isEmpty()) {
			return queryPool.poll();
		}
		return GL15.glGenQueries();
	}
}
