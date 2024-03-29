package nl.stefferd.ld30.world;

import java.util.ArrayList;
import java.util.Random;

import nl.stefferd.ld30.Assets;
import nl.stefferd.ld30.Renderable;
import nl.stefferd.ld30.Time;
import nl.stefferd.ld30.world.entities.Entity;
import nl.stefferd.ld30.world.entities.Player;
import nl.stefferd.ld30.world.tiles.Tile;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

public class World implements Renderable {
	
	private int width, height;
	
	private ArrayList<Entity> entities = new ArrayList<Entity>();
	
	private Chunk[] chunks;
	private OrthographicCamera camera, backgroundCamera;
	private Random random;
	private Player player;
	private ParalaxingBackground background;
	private SkyBackground sky;
	private Time time;
	
	public World(int width, int height) {
		this.width = width;
		this.height = height;
		chunks = new Chunk[width * height];
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				chunks[x + y * width] = new Chunk(x, y);
		
		// Init the camera (orthographic) to the dimensions of the screen
		camera = new OrthographicCamera();
		camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		// set camera to the center of the world
		camera.position.set(width * Chunk.SIZE * Tile.SIZE / 2,
				height * Chunk.SIZE * Tile.SIZE / 2, 0);
		
		// Init the camera for the background stuff
		backgroundCamera = new OrthographicCamera();
		backgroundCamera.setToOrtho(false, 1280, 720);
		
		// init the player, the position will be set when start island is loaded
		player = new Player(this, 0, 0);
		
		// generate the world based on the random class set
		random = new Random(0);
		generateWorld();

		// init the sky background images
		sky = new SkyBackground(this);
		
		// init the paralaxing background
		background = new ParalaxingBackground(new Texture[] {
				Assets.backgroundForest, Assets.backgroundMountains,
				Assets.backgroundHills, Assets.backgroundIslands
		});
		
		// init the time for ambient ligthing
		time = new Time();
	}
	
	/**
	 * Adds all the (randomly generated) stuff to the world. Adds it in the form
	 * of tiles, items or possibly entities.
	 */
	private void generateWorld() {
		addStarterIsland();
	}
	
	/**
	 * Adds the starter island in the middle of the world
	 */
	private void addStarterIsland() {
		// Get the center chunk
		int centerChunkX = width / 2;
		int centerChunkY = height / 2;
		
		// Random center position for the island in the chunk
		int xPos = random.nextInt(Chunk.SIZE);
		int yPos = random.nextInt(Chunk.SIZE);
		xPos = 15;
		yPos = 15;
		
		// adds the tile to the chunk
		Assets.addIsland(this, centerChunkX * Chunk.SIZE + xPos,
				centerChunkY * Chunk.SIZE + yPos);
	}
	
	long lastTime = System.currentTimeMillis();
	int frames = 0;
	int fps = 0;
	
	@Override
	public void update() {
		// update the time clock and print time
		time.update();
		
		// update all the entities in the world
		if (entities.size() > 0)
			for (int i = 0; i < entities.size(); i++)
				entities.get(i).update();
		
		// update the player for movement
		player.update();
		
		// make the camera follow the player
		camera.position.x = player.x + 32;
		camera.position.y = player.y + 100;
		camera.update();

		// update the sky background to correspond with the time
		sky.update();
		
		// update the paralaxing backgrounds
		background.update(camera.position.x, camera.position.y,
				width * Chunk.SIZE * Tile.SIZE, height * Chunk.SIZE * Tile.SIZE);
		
		// update the camera for rendering the background
		backgroundCamera.update();
		
		long now = System.currentTimeMillis();
		if (now - lastTime >= 1000) {
			lastTime = now;
			fps = frames;
			frames = 0;
		}
	}

	@Override
	public void render(SpriteBatch batch) {
		Gdx.gl.glClearColor(0.5f, 0.5f, 1, 1);
		
		// Apply the background camera to the sprite batch
		batch.setProjectionMatrix(backgroundCamera.combined);
		
		// render the sky first, so it is the farthest
		sky.render(batch);
		
		// apply the ambient lighting
		Vector3 v = time.getCurrentColor();
		batch.setColor(v.x, v.y, v.z, 1);
		
		// render paralaxing backgrounds
		background.render(batch);
		
		// Apply the camera's transformations for level's sprite rendering
		batch.setProjectionMatrix(camera.combined);
		
		// Render all the chunks this world contains
		for (int i = 0; i < chunks.length; i++) {
			chunks[i].render(batch);
		}
		
		// render all the entities in the world
		for (Entity entity : entities)
			entity.render(batch);
		
		// Render the player on top
		player.render(batch);
		
		// TODO: give own camera for UI
		// Render the UI text
		float xOffs = camera.position.x - camera.viewportWidth / 2;
		float yOffs = camera.position.y + camera.viewportHeight / 2;
		batch.begin();
		Assets.DEFAULT_FONT.draw(batch, "Time: " + time.getFormattedTime(),
				xOffs + 25, yOffs - 25);
		Assets.DEFAULT_FONT.draw(batch, "Day: " + time.getDays(), xOffs + 25,
				yOffs - 45);
		Assets.DEFAULT_FONT.draw(batch, "FPS: " + fps, xOffs + 25, yOffs - 65);
		batch.end();
		
		frames++;
	}
	
	/**
	 * Sets a tile to the chunk the position is in. Overrides a tile if one was
	 * already set.
	 * @param tile tile to be set
	 * @param x absolute x index of the tile
	 * @param y absolute y index of the tile
	 * @return true of false, success or not respectively
	 */
	public boolean setTile(Tile tile, int x, int y) {
		// Get the chunk position where the tile is in
		int chunkX = x / Chunk.SIZE;
		int chunkY = y / Chunk.SIZE;
		
		// Stop if not within world bounds
		if (chunkX < 0 || chunkX >= width ||
				chunkY < 0 || chunkY >= height)
			return false;
		
		// Get the tile position within the chunk
		int tileX = x % Chunk.SIZE;
		int tileY = y % Chunk.SIZE;
		
		// Set the position of the tile
		tile.setX(tileX);
		tile.setY(tileY);
		
		// If within bounds
		chunks[chunkX + chunkY * width].setTile(tile, tileX, tileY);
		
		// success
		return true;
	}
	
	/**
	 * Returns whether there is a none-void tile at the given position.
	 * @param x x position to check for
	 * @param y y position to check for
	 * @return true or false, none-void or void tile respectively
	 */
	public boolean isTileAt(float x, float y) {
		return getTileAt(x, y) != null;
	}
	
	/**
	 * Return the tile that is positioned at the position specified.
	 * @param x absolute world x coordinate
	 * @param y absolute world y coordinate
	 * @return Tile object
	 */
	public Tile getTileAt(float x, float y) {
		// check whether the position is in the bounds of the level
		if (x < 0 || x >= width * Chunk.SIZE * Tile.SIZE ||
				y < 0 || y >= height * Chunk.SIZE * Tile.SIZE)
			return null;
		
		// get the chunk indices the position is in
		int chunkX = (int)x / (int)(Chunk.SIZE * Tile.SIZE);
		int chunkY = (int)y / (int)(Chunk.SIZE * Tile.SIZE);
		
		// get the indices of the tile in the chunk to check
		int tileX = (int)(x / Tile.SIZE) % Chunk.SIZE;
		int tileY = (int)(y / Tile.SIZE) % Chunk.SIZE;
		
		// get the tile from the chunk
		Tile tile = chunks[chunkX + chunkY * width].getTile(tileX, tileY);
		
		return tile;
	}
	
	/**
	 * Breaks a tile
	 * @param tile
	 */
	public void breakTile(Tile tile) {
		// Check for breakability
		if (!tile.isBreakable())
			return;
		
		// Remove the tile from the parent chunk
		tile.remove();
		// TODO: fancy breaking animation stuff
	}
	
	public boolean touchesCollidableTile(Rectangle rect) {
		// TODO: optimize
		// Collect all colliding chunks
		ArrayList<Chunk> chunkList = new ArrayList<Chunk>();
		
		for (int i = 0; i < chunks.length; i++)
			if (rect.overlaps(chunks[i].getAbsoluteRectangle()))
				chunkList.add(chunks[i]);
		
		boolean colliding = false;
		
		// Check for tiles in the chunks
		for (Chunk chunk : chunkList) {
			for (int y = 0; y < Chunk.SIZE && !colliding; y++)
				for (int x = 0; x < Chunk.SIZE && !colliding; x++)
					if (chunk.getTile(x, y) != null) {
						Tile tile = chunk.getTile(x, y);
						if (tile.getAbsoluteRectangle()
								.overlaps(rect) && tile.isSolid())
							colliding = true;
					}
			
			if (colliding)
				break;
		}
		
		return colliding;
	}
	
	/**
	 * Sets the position of the player to a tile. Thus snaps to a 64x64 grid.
	 * @param x absolute tile x index
	 * @param y absolute tile y index
	 */
	public void setPlayerPosition(int x, int y) {
		player.setPosition(x * Tile.SIZE, y * Tile.SIZE);
	}
	
	/**
	 * Returns the main player
	 * @return Player class
	 */
	public Player getPlayer() {
		return player;
	}
	
	/**
	 * Returns the camera that is currently used to show the game
	 * @return Camera object
	 */
	public Camera getCurrentCamera() {
		return camera;
	}
	
	/**
	 * Returns the time the world has been running, in seconds
	 * @return float
	 */
	public float getTime() {
		return time.getTime();
	}
	
	/**
	 * Returns the amount of seconds into this day
	 * @return float
	 */
	public float getTimeOfDay() {
		return time.getTimeOfDay();
	}
	
	/**
	 * Removes an entity from the list
	 * @param entity entity to remove
	 */
	public void removeEntity(Entity entity) {
		entities.remove(entity);
	}
	
	/**
	 * Adds an entity to the world class' list
	 * @param entity
	 */
	public void addEntity(Entity entity) {
		entities.add(entity);
	}
	
}
