import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;

public class Enemy {
	// enemy position
	public double x, y;
	// enemy size
	public int width = 32, height = 32;
	// enemy speed and health
	public double speed;
	public int health;
	// direction enemy is facing
	private double facingAngle = 0;
	// enemy image
	private BufferedImage image;

	// max health for health bar
	private int maxHealth;
	// should we draw the health bar
	private boolean showHealthBar = false;

	// states for enemy brain
	private enum AIState {
		PATROL, CHASE
	}

	// current enemy brain state
	private AIState currentState = AIState.PATROL;

	// timers for actions
	private long lastDirectionChangeTime = 0;
	private long lastShotTime = 0;
	// how long to wait between shots
	private final long SHOOT_COOLDOWN = 1200;
	// how long to wait before changing patrol direction
	private int patrolDirectionCooldown = 6000;
	// used for random numbers
	private static final Random rand = new Random();

	// types of enemies
	public enum EnemyType {
		NORMAL, HEAVY
	}

	public Enemy(double startX, double startY, EnemyType type) {
		this.x = startX;
		this.y = startY;
		// if normal enemy set normal stats
		if (type == EnemyType.NORMAL) {
			this.maxHealth = 3;
			this.speed = 1.5;
			try {
				image = ImageIO.read(new File("enemy.png"));
			} catch (IOException e) {
				// image failed to load
			}
			// if heavy enemy set heavy stats
		} else {
			this.maxHealth = 5;
			this.speed = 1.0;
			try {
				image = ImageIO.read(new File("heavy.png"));
			} catch (IOException e) {
				// image failed to load
			}
		}
		// set health to max health
		this.health = this.maxHealth;
		// start facing a random direction
		this.facingAngle = rand.nextDouble() * 2 * Math.PI;
	}

	/**
	 * runs all enemy logic decides what to do then does it
	 */
	public void update(Player player, Floor floor, int tileSize, ArrayList<Bullet> bullets) {
		updateAIState(player, floor, tileSize);
		performAction(player, floor, tileSize, bullets);
		move(floor, tileSize);
	}

	/**
	 * decides if enemy should chase player or patrol checks distance and if it can
	 * see the player
	 */
	private void updateAIState(Player player, Floor floor, int tileSize) {
		// check distance to player
		double distanceToPlayer = Math.hypot(player.x - x, player.y - y);

		// if chasing see if we should stop
		if (currentState == AIState.CHASE) {
			// if player is too far or behind a wall stop chasing
			if (distanceToPlayer > 15 * tileSize || !floor.hasLineOfSight((int) (x / tileSize), (int) (y / tileSize),
					(int) (player.x / tileSize), (int) (player.y / tileSize))) {
				currentState = AIState.PATROL;
			}
			return;
		}

		// set how far the enemy can see (5 tiles)
		double visionRange = 5.0 * tileSize;

		// check if the player is close enough to be seen
		if (distanceToPlayer < visionRange) {

		    // check if there are no walls blocking the view between enemy and player
		    if (floor.hasLineOfSight((int) (x / tileSize), (int) (y / tileSize),
		                             (int) (player.x / tileSize), (int) (player.y / tileSize))) {

		        // calculate the angle from the enemy to the player
		        double angleToPlayer = Math.atan2(player.y - y, player.x - x);

		        // figure out how far off the player is from where the enemy is facing
		        double angleDifference = facingAngle - angleToPlayer;

		        // adjust angleDifference so it's between -PI and +PI (to avoid weird math)
		        while (angleDifference <= -Math.PI)
		            angleDifference += 2 * Math.PI;
		        while (angleDifference > Math.PI)
		            angleDifference -= 2 * Math.PI;

		        // if the player is in front of the enemy (within 90 degrees view cone)
		        if (Math.abs(angleDifference) < Math.PI / 4) {
		            // switch to chasing the player
		            currentState = AIState.CHASE;
		        }
		    }
		}
			
	}

	/**
	 * does an action based on the current state if chasing face player and shoot if
	 * patrolling change direction sometimes
	 */
	private void performAction(Player player, Floor floor, int tileSize, ArrayList<Bullet> bullets) {
		// if we are chasing the player
		if (currentState == AIState.CHASE) {
			// face the player
			facingAngle = Math.atan2(player.y - y, player.x - x);
			// if we can shoot
			if (System.currentTimeMillis() - lastShotTime > SHOOT_COOLDOWN) {
				// and we can see the player
				if (floor.hasLineOfSight((int) (x / tileSize), (int) (y / tileSize), (int) (player.x / tileSize),
						(int) (player.y / tileSize))) {
					// shoot a bullet
					bullets.add(new Bullet(x, y, facingAngle, false));
					lastShotTime = System.currentTimeMillis();
				}
			}
			// if we are patrolling
		} else {
			// if its time to change direction
			if (System.currentTimeMillis() - lastDirectionChangeTime > patrolDirectionCooldown) {
				lastDirectionChangeTime = System.currentTimeMillis();
				patrolDirectionCooldown = rand.nextInt(3000) + 2000;
				// pick a new random direction
				facingAngle = rand.nextDouble() * 2 * Math.PI;
			}
		}
	}

	/**
	 * moves the enemy forward checks for walls before moving
	 */
	private void move(Floor floor, int tileSize) {
		// go slower when patrolling
		double currentSpeed = (currentState == AIState.CHASE) ? speed : speed * 0.7;
		// calculate how much to move
		double moveX = Math.cos(facingAngle) * currentSpeed;
		double moveY = Math.sin(facingAngle) * currentSpeed;

		double nextX = x + moveX;
		double nextY = y + moveY;

		// if we are about to hit a wall
		if (!canMove(nextX, nextY, floor, tileSize)) {
			// and if we are patrolling
			if (currentState == AIState.PATROL) {
				// pick a new direction right away
				facingAngle = rand.nextDouble() * 2 * Math.PI;
				lastDirectionChangeTime = System.currentTimeMillis();
			}
			return;
		}

		// update position
		x = nextX;
		y = nextY;
	}

	/**
	 * checks if the enemy can move to a new spot without hitting a wall
	 */
	private boolean canMove(double nextX, double nextY, Floor floor, int tileSize) {
		int tileXLeft = (int) ((nextX - width / 2.0) / tileSize);
		int tileXRight = (int) ((nextX + width / 2.0 - 1) / tileSize);
		int tileYTop = (int) ((nextY - height / 2.0) / tileSize);
		int tileYBottom = (int) ((nextY + height / 2.0 - 1) / tileSize);
		return floor.isWalkable(tileXLeft, tileYTop) && floor.isWalkable(tileXRight, tileYTop)
				&& floor.isWalkable(tileXLeft, tileYBottom) && floor.isWalkable(tileXRight, tileYBottom);
	}

	/**
	 * makes the enemy take damage shows health bar and starts chasing player
	 */
	public void takeDamage(int amount) {
		// dont take damage if already dead
		if (health <= 0)
			return;

		this.health -= amount;
		this.showHealthBar = true;
		// start chasing the player when hit
		this.currentState = AIState.CHASE;
	}

	/**
	 * draws the enemy on screen rotates it to face the right direction also draws
	 * health bar if needed
	 */
	public void draw(Graphics2D g2) {
		// save current rotation
		AffineTransform oldTransform = g2.getTransform();
		// move and rotate to draw the enemy
		g2.translate(x, y);
		g2.rotate(facingAngle - Math.toRadians(90));
		// if image exists draw it
		if (image != null) {
			g2.drawImage(image, -width / 2, -height / 2, width, height, null);
		}
		// restore old rotation
		g2.setTransform(oldTransform);
		// if health bar should be shown draw it
		if (showHealthBar) {
			drawHealthBar(g2);
		}
	}

	/**
	 * draws a health bar above the enemy
	 */
	private void drawHealthBar(Graphics2D g2) {
		int barWidth = 30;
		int barHeight = 5;
		int barX = (int) (x - barWidth / 2.0);
		int barY = (int) (y - height / 2.0 - 10);
		// draw gray background
		g2.setColor(Color.DARK_GRAY);
		g2.fillRect(barX, barY, barWidth, barHeight);
		// calculate health percentage
		double healthPercentage = Math.max(0, (double) health / maxHealth);
		// draw red health part
		g2.setColor(Color.RED);
		g2.fillRect(barX, barY, (int) (barWidth * healthPercentage), barHeight);
	}

	/**
	 * gets the enemys hitbox for collision detection
	 */
	public Rectangle getBounds() {
		return new Rectangle((int) (x - width / 2.0), (int) (y - height / 2.0), width, height);
	}
}