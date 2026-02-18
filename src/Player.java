import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

// this is the player character. the guy u control
public class Player {
	// where the player is on the screen
	public double x, y;
	// how big the player is
	public int width = 32, height = 32;
	// how fast the player moves
	public double speed = 2.0;
	// if the player is moving in a direction
	private boolean movingLeft, movingRight, movingUp, movingDown;
	// what angle the player is looking at
	private double facingAngle = 0;
	// the picture of the player
	private BufferedImage image;

	// players health
	public int health;
	public final int maxHealth = 7;
	// if the player can get hurt or not
	public boolean isInvincible = false;
	// when the invincibility started
	private long invincibilityStartTime;
	// how long invincibility lasts in milliseconds
	private final long INVINCIBILITY_DURATION = 1500;

	// Ammo and Reloading
	// for bullets and shooting
	public final int maxAmmo = 10;
	public int ammo;
	// if the player is reloading their gun
	public boolean isReloading = false;
	// when the reload started
	private long reloadStartTime;
	private final long RELOAD_TIME = 1500; // 1.5 seconds to reload

	public Player(double x, double y) {
		this.health = maxHealth; // start with full health
		this.ammo = maxAmmo; // start with full ammo
		setPosition(x, y); // put the player at the start position
		loadSkin("player.png"); // load the player picture
	}

	/**
	 * Loads a character skin from a given file path.
	 * loads a player picture from a file
	 *
	 * @param imagePath The file path of the image (e.g., "player.png"). where the picture is like "player.png"
	 */
	public void loadSkin(String imagePath) {
		try {
			// Using getResource is more reliable for finding files, especially in a JAR
			// this is a good way to find files
			image = ImageIO.read(getClass().getResource(imagePath));
		} catch (IOException | IllegalArgumentException e) {
			//System.err.println("Error loading skin image via getResource: " + imagePath + ". Trying file path...");
			// Fallback to the original file loading method if getResource fails
			// if that dont work try this other way
			try {
				image = ImageIO.read(new File(imagePath));
			} catch (IOException ex) {
				//System.err.println("Fallback file loading also failed for: " + imagePath);
				// if both ways fail then oh well
			}
		}
	}

	/**
	 * This method is added to ensure your GamePanel doesn't crash if it tries to
	 * call setSkinColor. It now correctly points to the default skin.
	 * this is just here so the game dont crash. it just loads the normal skin
	 * @param c the color which we ignore
	 */
	public void setSkinColor(Color c) {
		// This is a compatibility method. The correct way to change skins
		// is now by calling loadSkin(String imagePath).
		// the right way to change skin is loadSkin
		loadSkin("player.png");
	}

	/**
	 * this is called all the time to update the player. like moving and stuff
	 * @param floor the floor the player is on
	 * @param tileSize how big the tiles r
	 */
	public void update(Floor floor, int tileSize) {
		// if ur invincible check if time is up
		if (isInvincible && System.currentTimeMillis() - invincibilityStartTime > INVINCIBILITY_DURATION) {
			isInvincible = false;
		}

		// if ur reloading check if its done
		if (isReloading && System.currentTimeMillis() - reloadStartTime > RELOAD_TIME) {
			isReloading = false;
			ammo = maxAmmo; // fill up the ammo
		}

		// u move slower when reloading
		double currentSpeed = isReloading ? speed / 2 : speed; // Slower while reloading
		double moveSpeed = currentSpeed;
		// if ur moving diagonal u shudnt be faster
		if ((movingUp || movingDown) && (movingLeft || movingRight)) {
			moveSpeed /= Math.sqrt(2);
		}
		// check left and right movement
		double moveX = 0;
		if (movingLeft)
			moveX -= moveSpeed;
		if (movingRight)
			moveX += moveSpeed;
		if (moveX != 0 && canMove(x + moveX, y, floor, tileSize))
			x += moveX;

		// check up and down movement
		double moveY = 0;
		if (movingUp)
			moveY -= moveSpeed;
		if (movingDown)
			moveY += moveSpeed;
		if (moveY != 0 && canMove(x, y + moveY, floor, tileSize))
			y += moveY;
	}

	/**
	 * checks if the player can move to a new spot without hitting a wall
	 * @param nextX the new x spot
	 * @param nextY the new y spot
	 * @param floor the current floor
	 * @param tileSize how big tiles r
	 * @return true if u can move there
	 */
	private boolean canMove(double nextX, double nextY, Floor floor, int tileSize) {
		// check all four corners of the player
		int tileXLeft = (int) ((nextX - width / 2.0) / tileSize);
		int tileXRight = (int) ((nextX + width / 2.0 - 1) / tileSize);
		int tileYTop = (int) ((nextY - height / 2.0) / tileSize);
		int tileYBottom = (int) ((nextY + height / 2.0 - 1) / tileSize);
		// if all corners are in a walkable tile its ok
		return floor.isWalkable(tileXLeft, tileYTop) && floor.isWalkable(tileXRight, tileYTop)
				&& floor.isWalkable(tileXLeft, tileYBottom) && floor.isWalkable(tileXRight, tileYBottom);
	}

	/**
	 * makes a new bullet when the player shoots
	 * @return a new Bullet object or nothing if u cant shoot
	 */
	public Bullet shoot() {
		// u can shoot if u have ammo and arent reloading
		if (ammo > 0 && !isReloading) {
			ammo--; // use one ammo
			return new Bullet(x, y, facingAngle, true); // make the bullet
		}
		return null; // No ammo or is reloading
	}

	/**
	 * starts reloading the gun if u need to
	 */
	public void reload() {
		// only reload if not already reloading and ammo isnt full
		if (!isReloading && ammo < maxAmmo) {
			isReloading = true;
			reloadStartTime = System.currentTimeMillis();
		}
	}

	/**
	 * this makes the player take damage
	 * @param amount how much damage to take
	 */
	public void takeDamage(int amount) {
		// cant take damage if invincible
		if (!isInvincible) {
			health -= amount;
			isInvincible = true; // become invincible for a bit
			invincibilityStartTime = System.currentTimeMillis();
			if (health < 0)
				health = 0; // cant have less than 0 health
		}
	}

	/**
	 * draws the player on the screen
	 * @param g2 the graphics thing to draw with
	 */
	public void draw(Graphics2D g2) {
		// if invincible make the player blink
		if (isInvincible) {
			long timeSinceHit = System.currentTimeMillis() - invincibilityStartTime;
			// this makes it flash on and off
			if ((timeSinceHit / 250) % 2 == 1)
				return; // dont draw so it looks like it disappeared
		}

		// save the current screen position and rotation
		AffineTransform oldTransform = g2.getTransform();
		g2.translate(x, y); // move to the player's spot
		g2.rotate(facingAngle - Math.toRadians(90)); // rotate the player to face the right way

		if (image != null) {
			// draw the player picture
			g2.drawImage(image, -width / 2, -height / 2, width, height, null);
		} else {
			// if there's no picture draw a blue box instead
			g2.setColor(Color.CYAN);
			g2.fillRect(-width / 2, -height / 2, width, height);
		}
		// put the screen back to how it was
		g2.setTransform(oldTransform);
	}

	public void setPosition(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void setFacingAngle(double angle) {
		this.facingAngle = angle;
	}

	/**
	 * when a key is pressed down
	 * @param e the key event
	 */
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_A -> movingLeft = true; // move left
		case KeyEvent.VK_D -> movingRight = true; // move right
		case KeyEvent.VK_W -> movingUp = true; // move up
		case KeyEvent.VK_S -> movingDown = true; // move down
		case KeyEvent.VK_R -> reload(); // reload gun
		}
	}

	/**
	 * when a key is let go
	 * @param e the key event
	 */
	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_A -> movingLeft = false; // stop moving left
		case KeyEvent.VK_D -> movingRight = false; // stop moving right
		case KeyEvent.VK_W -> movingUp = false; // stop moving up
		case KeyEvent.VK_S -> movingDown = false; // stop moving down
		}
	}

	public Rectangle getBounds() {
		return new Rectangle((int) (x - width / 2.0), (int) (y - height / 2.0), width, height);
	}
}