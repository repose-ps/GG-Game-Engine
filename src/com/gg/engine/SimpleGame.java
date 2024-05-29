package com.gg.engine;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
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
	 * The default target FPS, used for when the target fps is less than or equal to
	 * 0.
	 */
	private static final int DEFAULT_TARGET_FPS = 30;

	/**
	 * The minimum size that the canvas can be set to when resizing the window.
	 */
	private static final Dimension MINIMUM_CANVAS_SIZE = new Dimension(800, 600);

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
	 * The game window's frame.
	 */
	private final Frame window;

	/**
	 * The component the game is drawn on.
	 */
	private final Canvas canvas;

	/**
	 * The image that the frame is drawn on prior to being drawn on the
	 * {@link #canvas}.
	 */
	private BufferedImage frameBufferImage;

	/**
	 * The graphics instance created from the {@link #frameBufferImage}.
	 */
	private Graphics2D frameBufferGraphics;

	/**
	 * Creates a new SimpleGame instance.
	 */
	public SimpleGame() {
		// initialize variables
		this.window = new Frame();
		this.canvas = new Canvas();
	}

	/**
	 * Starts the game if it is not already running.
	 */
	public final void start() {
		// if game is already started
		if (gameLoopExecutor != null) {
			throw new RuntimeException("game already running");
		}

		// initialize window
		this.canvas.setPreferredSize(MINIMUM_CANVAS_SIZE);
		this.window.add(this.canvas, BorderLayout.CENTER);

		// open window
		this.window.pack();
		this.window.setLocationRelativeTo(null); // center window on screen
		this.window.setVisible(true);

		// set last frame time to current time to avoid creating a large delta value
		this.lastFrameTime = System.currentTimeMillis();

		// start executor
		this.createExecutor();
	}

	/**
	 * Creates a game loop executor to run the game loop at the current target fps.
	 * If the target fps is less than 0, it is set to {@link #DEFAULT_TARGET_FPS}.
	 */
	private void createExecutor() {
		// validate target fps
		if (this.targetFps <= 0) {
			this.targetFps = DEFAULT_TARGET_FPS;
		}

		// calculate frame time in nanoseconds
		final long frameTimeNanos = TimeUnit.SECONDS.toNanos(1L) / (long) this.targetFps;

		// start executor
		this.gameLoopExecutor = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
			try {
				frame();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}, 0L, frameTimeNanos, TimeUnit.NANOSECONDS);
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

		// create graphics instance
		this.createGraphics();

		// update the game
		this.update(delta);

		// renders the game onto the buffer graphics
		this.render(this.frameBufferGraphics);

		// renders the buffer graphics onto the canvas
		this.canvas.getGraphics().drawImage(frameBufferImage, 0, 0, null);
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
	 * Creates the buffer graphics that the frame is drawn on prior to being drawn
	 * on the {@link #canvas}.
	 * 
	 * @return the buffer graphics
	 */
	private Graphics2D createGraphics() {
		// see if we need to create a buffer image, if so return current graphics
		if (this.frameBufferImage != null && this.frameBufferGraphics != null
				&& this.frameBufferImage.getWidth() == canvas.getWidth()
				&& this.frameBufferImage.getHeight() == canvas.getHeight()) {
			return this.frameBufferGraphics;
		}

		// dispose of old graphics
		if (this.frameBufferGraphics != null) {
			this.frameBufferGraphics.dispose();
		}

		// create new buffer image
		this.frameBufferImage = new BufferedImage(this.canvas.getWidth(), this.canvas.getHeight(),
				BufferedImage.TYPE_INT_ARGB);

		// create and return new graphics instance
		return this.frameBufferGraphics = this.frameBufferImage.createGraphics();
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
	public abstract void render(Graphics2D graphics);

	/**
	 * Returns the time, in milliseconds, that the current frame started. This is
	 * effectively a replacement for {@link System#currentTimeMillis()}.
	 * 
	 * @return the time, in miliseconds, that the current frame started
	 */
	public long now() {
		return this.now;
	}

	/**
	 * @return The component the game is drawn on.
	 */
	public Canvas getCanvas() {
		return this.canvas;
	}

	/**
	 * @return The game window's frame.
	 */
	public Frame getWindow() {
		return this.window;
	}
}
