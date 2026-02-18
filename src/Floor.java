import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class Floor {
	// map size
	final int WIDTH, HEIGHT;
	// 2d array for the map tiles
	final int[][] map;
	// tile types
	public static final int FLOOR = 0, WALL = 1, COMPUTER = 2, UP = 3, DOWN = 4;
	// player starting position
	int startX, startY;
	// location of stairs
	int linkedUpX = -1, linkedUpY = -1, linkedDownX = -1, linkedDownY = -1;
	// list of enemies on the floor
	ArrayList<Enemy> enemies = new ArrayList<>();
	// which floor number this is
	private final int floorIndex;
	// size of each tile in pixels
	private final int tileSize;

	// a number to make sure the random generation is the same every time
	private final long seed;

	// has the computer been hacked
	private boolean computerHacked = false;
	// timer for computer cooldown
	private long computerCooldownTime = 0;

	public Floor(int width, int height, int floorIndex, int tileSize, long seed) {
		this.WIDTH = width;
		this.HEIGHT = height;
		this.floorIndex = floorIndex;
		this.tileSize = tileSize;
		// store the seed for this floor
		this.seed = seed;
		map = new int[HEIGHT][WIDTH];
		// create the level
		generate();
	}

	/**
	 * carves out a 3x3 area of floor tiles used to make sure important things arent
	 * blocked
	 */
	private void carveArea(int centerX, int centerY) {
		for (int y = centerY - 1; y <= centerY + 1; y++) {
			for (int x = centerX - 1; x <= centerX + 1; x++) {
				if (x > 0 && x < WIDTH - 1 && y > 0 && y < HEIGHT - 1) {
					if (map[y][x] == WALL) {
						map[y][x] = FLOOR;
					}
				}
			}
		}
	}

	/**
	 * generates the entire floor layout places walls floors computer stairs and
	 * enemies
	 */
	public void generate() {
		// use the floor's seed for random numbers
		Random rand = new Random(this.seed);

		// fill entire map with walls
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++)
				map[y][x] = WALL;
		}
		// carve out floor tiles randomly
		for (int y = 1; y < HEIGHT - 1; y++) {
			for (int x = 1; x < WIDTH - 1; x++)
				if (rand.nextInt(6) != 0)
					map[y][x] = FLOOR;
		}

		// place computer in a random spot
		map[rand.nextInt(HEIGHT - 4) + 2][rand.nextInt(WIDTH - 4) + 2] = COMPUTER;

		// place the UP stairs on the right side
		for (int tries = 0; tries < 200; tries++) {
			int x = rand.nextInt(WIDTH / 2 - 2) + (WIDTH / 2);
			int y = rand.nextInt(HEIGHT / 2 - 2) + 1;
			if (x > 0 && x < WIDTH - 1 && y > 0 && y < HEIGHT - 1) {
				carveArea(x, y);
				map[y][x] = UP;
				linkedUpX = x;
				linkedUpY = y;
				break;
			}
		}

		// place DOWN stairs on the left side if not floor 0
		if (floorIndex > 0) {
			for (int tries = 0; tries < 200; tries++) {
				int x = rand.nextInt(WIDTH / 2 - 2) + 1;
				int y = rand.nextInt(HEIGHT / 2 - 2) + (HEIGHT / 2);
				if (x > 0 && x < WIDTH - 1 && y > 0 && y < HEIGHT - 1) {
					carveArea(x, y);
					map[y][x] = DOWN;
					linkedDownX = x;
					linkedDownY = y;
					break;
				}
			}
		}

		// find a starting spot for the player at the bottom middle
		for (int offset = 0; offset < WIDTH / 2; offset++) {
			int x = (WIDTH / 2) + offset;
			if (map[HEIGHT - 2][x] == FLOOR) {
				startX = x;
				startY = HEIGHT - 2;
				carveArea(startX, startY);
				break;
			}
			x = (WIDTH / 2) - offset;
			if (map[HEIGHT - 2][x] == FLOOR) {
				startX = x;
				startY = HEIGHT - 2;
				carveArea(startX, startY);
				break;
			}
		}

		// spawn enemies
		int totalGuards = Math.min(8, 2 + floorIndex);
		int maxHeavyGuards = Math.min(totalGuards, floorIndex / 2);
		int numHeavyGuards = (maxHeavyGuards > 0) ? rand.nextInt(maxHeavyGuards + 1) : 0;
		int numNormalGuards = totalGuards - numHeavyGuards;

		for (int i = 0; i < numHeavyGuards; i++)
			spawnEnemy(Enemy.EnemyType.HEAVY, rand);
		for (int i = 0; i < numNormalGuards; i++)
			spawnEnemy(Enemy.EnemyType.NORMAL, rand);
	}

	/**
	 * tries to place an enemy on a random floor tile
	 */
	private void spawnEnemy(Enemy.EnemyType type, Random rand) {
		// try 100 times to find a spot
		for (int tries = 0; tries < 100; tries++) {
			int x = rand.nextInt(WIDTH - 2) + 1;
			int y = rand.nextInt(HEIGHT - 2) + 1;
			// if the spot is a floor tile
			if (map[y][x] == FLOOR) {
				// create a new enemy there
				double spawnX = x * this.tileSize + this.tileSize / 2.0;
				double spawnY = y * this.tileSize + this.tileSize / 2.0;
				enemies.add(new Enemy(spawnX, spawnY, type));
				return;
			}
		}
	}

	/**
	 * draws the entire floor and all enemies
	 */
	public void draw(Graphics2D g2, int tileSize) {
		// loop through every tile
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				// pick a color based on the tile type
				switch (map[y][x]) {
				case FLOOR -> g2.setColor(new Color(30, 30, 30));
				case WALL -> g2.setColor(Color.BLUE);
				case COMPUTER -> g2.setColor(Color.YELLOW);
				// up stairs are green if hacked red if not
				case UP -> g2.setColor(computerHacked ? Color.GREEN : new Color(100, 0, 0));
				case DOWN -> g2.setColor(Color.ORANGE);
				}
				// draw the tile
				g2.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);

				// if computer draw a black square on it
				if (map[y][x] == COMPUTER) {
					g2.setColor(Color.BLACK);
					g2.fillRect(x * tileSize + 4, y * tileSize + 4, tileSize - 8, tileSize - 8);
				}

				// draw a grid
				g2.setColor(new Color(0, 255, 0));
				g2.drawRect(x * tileSize, y * tileSize, tileSize, tileSize);
			}
		}
		// draw all enemies
		for (Enemy enemy : enemies)
			enemy.draw(g2);
	}

	public int getTile(int x, int y) {
		if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT)
			return WALL;
		return map[y][x];
	}

	/**
	 * checks if a tile can be walked on
	 */
	public boolean isWalkable(int x, int y) {
		// check if outside map
		if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT)
			return false;
		int tile = map[y][x];
		// can walk on floor and stairs but not computers or walls
		return tile == FLOOR || tile == UP || tile == DOWN;
	}

	/**
	 * checks if there is a straight line of floor tiles between two points with no
	 * walls in the way
	 */
	public boolean hasLineOfSight(int x1, int y1, int x2, int y2) {
		int dx = Math.abs(x2 - x1);
		int dy = -Math.abs(y2 - y1);
		int sx = x1 < x2 ? 1 : -1;
		int sy = y1 < y2 ? 1 : -1;
		int err = dx + dy;
		while (true) {
			// if we hit a wall there is no line of sight
			if (getTile(x1, y1) == WALL)
				return false;
			// if we reached the end we have line of sight
			if (x1 == x2 && y1 == y2)
				break;
			int e2 = 2 * err;
			if (e2 >= dy) {
				err += dy;
				x1 += sx;
			}
			if (e2 <= dx) {
				err += dx;
				y1 += sy;
			}
		}
		return true;
	}

	public int getStartX() {
		return startX;
	}

	public int getStartY() {
		return startY;
	}

	public int getLinkedUpX() {
		return linkedUpX;
	}

	public int getLinkedUpY() {
		return linkedUpY;
	}

	public int getLinkedDownX() {
		return linkedDownX;
	}

	public int getLinkedDownY() {
		return linkedDownY;
	}

	public boolean isComputerHacked() {
		return computerHacked;
	}

	public void setComputerHacked(boolean hacked) {
		this.computerHacked = hacked;
	}

	public long getComputerCooldownTime() {
		return computerCooldownTime;
	}

	public void setComputerCooldownTime(long time) {
		this.computerCooldownTime = time;
	}
}