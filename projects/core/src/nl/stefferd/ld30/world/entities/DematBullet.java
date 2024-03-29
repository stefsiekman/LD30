package nl.stefferd.ld30.world.entities;

import nl.stefferd.ld30.Assets;
import nl.stefferd.ld30.world.World;
import nl.stefferd.ld30.world.tiles.Tile;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class DematBullet extends Entity {
	
	private Vector2 direction;

	public DematBullet(World world, float x, float y, Vector2 dir) {
		super(world, new Sprite(Assets.entityDematBullit), x, y);
		direction = dir.nor();
	}
	
	@Override
	public void update() {
		super.update();
		
		// Move the bullet
		Vector2 mv = direction.cpy().scl(1000 * Gdx.graphics.getDeltaTime());
		x += mv.x;
		y += mv.y;
		
		// Check for collision
		Tile collision = world.getTileAt(x, y);
		// If colliding
		if (collision != null) {
			// break the tile
			world.breakTile(collision);
			// remove this bullet from the world
			destroy();
		}
		
		// Check for age
		if (lifetime > 0.5f)
			destroy();
	}

	@Override
	protected Rectangle getBounds() {
		return null;
	}

}
