package com.gg.engine;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Renders and updates a game without using states, and instead contains all the
 * aspects of the game in a single update/render pattern.
 * 
 * @author Robert Guidry
 */
public abstract class SimpleGame {

	/**
	 * The default target FPS, used for when the target fps is LTE 0.
	 */
	private static final int DEFAULT_TARGET_FPS = 30;

	/**
	 * The main game loop executor.
	 */
	private ScheduledFuture<?> gameLoopExecutor;

	/**
	 * The amount of frames per second that the game should try to run at.
	 */
	private int targetFps;

	/**
	 * The last time, in milliseconds, that a frame was drawn.
	 */
	private long lastFrameTime;

	/**
	 * The time, in milliseconds, that the current frame started at.
	 */
	private long now;

	/**
	 * Starts the game if it is not already running.
	 */
	public final void start() {
		// if game is already started
		if (gameLoopExecutor != null) {
			throw new RuntimeException("game already running");
		}

		// set last frame time to current time to avoid creating a large delta value
		this.lastFrameTime = System.currentTimeMillis();

		// start executor
		this.createExecutor();
	}

	/**
	 * Creates a game loop executor to run the game loop at the current target fps.
	 * If the target fps is
	 */
	private void createExecutor() {
		// validate target fps
		if (this.targetFps <= 0) {
			this.targetFps = DEFAULT_TARGET_FPS;
		}

		// calculate frame time in nanoseconds
		final long frameTimeNanos = TimeUnit.SECONDS.toNanos(1L) / (long) this.targetFps;

		// start executor
		this.gameLoopExecutor = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> frame(), 0L,
				frameTimeNanos, TimeUnit.NANOSECONDS);
	}

	/**
	 * Updates and renders the next frame.
	 */
	private void frame() {
		// set the current time
		this.now = System.currentTimeMillis();

		// calculate the delta
		final long delta = this.now - this.lastFrameTime;

		// set last frame time to now since we will not be using it anymore this frame
		this.lastFrameTime = this.now;

		// update the game
		this.update(delta);

		// renders the game
		this.render();
	}

	/**
	 * Sets the target FPS of the game engine. If the game is running, the current
	 * game loop executor is cancelled and replaced by a new one that runs at the
	 * specified target FPS.
	 * 
	 * @param targetFps the amount of frames per second to have the game try to run
	 *                  at
	 */
	public void setTargetFps(int targetFps) {
		// set target fps
		this.targetFps = targetFps;

		// if game loop is running
		if (this.gameLoopExecutor != null) {
			// cancel old executor
			this.gameLoopExecutor.cancel(true);
			this.gameLoopExecutor = null;

			// start new executor
			this.createExecutor();
		}
	}

	/**
	 * Updates the game elements.
	 * 
	 * @param delta the amount of milliseconds since the last frame
	 */
	public abstract void update(long delta);

	/**
	 * Renders the game elements.
	 */
	public abstract void render();

	/**
	 * Returns the time, in milliseconds, that the current frame started. This is
	 * effectively a replacement for {@link System#currentTimeMillis()}.
	 * 
	 * @return the time, in miliseconds, that the current frame started
	 */
	public long now() {
		return this.now;
	}
}
