import java.awt.*;

public class Bullet {
	// bullet position
	public double x, y;
	// bullet direction
	private final double angle;
	// bullet speed
	private final double speed;
	// bullet color
	private final Color color;
	// is this a player bullet or enemy bullet
	public final boolean isPlayerBullet;
	// bullet size
	public final int width = 8, height = 8;

	public Bullet(double x, double y, double angle, boolean isPlayerBullet) {
		this.x = x;
		this.y = y;
		this.angle = angle;
		this.isPlayerBullet = isPlayerBullet;

		// if player bullet make it fast and cyan
		if (isPlayerBullet) {
			this.speed = 6.0;
			this.color = Color.CYAN;
		// if enemy bullet make it slow and red
		} else {
			this.speed = 4.0;
			this.color = Color.RED;
		}
	}

	/**
	 * moves the bullet
	 * based on its angle and speed
	 */
	public void update() {
		x += Math.cos(angle) * speed;
		y += Math.sin(angle) * speed;
	}

	/**
	 * draws the bullet on screen
	 * as a small circle
	 */
	public void draw(Graphics2D g2) {
		g2.setColor(color);
		g2.fillOval((int) (x - width / 2.0), (int) (y - height / 2.0), width, height);
	}

	/**
	 * gets the bullets hitbox
	 * for collision detection
	 */
	public Rectangle getBounds() {
		return new Rectangle((int) (x - width / 2.0), (int) (y - height / 2.0), width, height);
	}
}