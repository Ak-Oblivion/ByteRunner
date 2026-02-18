// this class is for a single level in the game. it holds info about it
public class Level {
	// the number of the level like 1 2 or 3
	private final int levelNumber;
	// how many floors are in this level
	private final int numberOfFloors;
	// how much time u have to finish it
	private final int timeLimitInSeconds;
	// if u can play this level or not
	private boolean isUnlocked;
	// --- NEW: Add a seed for deterministic generation ---
	// this is a seed so the level is the same every time u play it
	private final long seed;

	public Level(int levelNumber, int numberOfFloors, int timeLimitInSeconds, boolean isUnlocked, long seed) {
		this.levelNumber = levelNumber;
		this.numberOfFloors = numberOfFloors;
		this.timeLimitInSeconds = timeLimitInSeconds;
		this.isUnlocked = isUnlocked;
		this.seed = seed; // --- NEW ---
	}

	public int getLevelNumber() {
		return levelNumber;
	}

	public int getNumberOfFloors() {
		return numberOfFloors;
	}

	public int getTimeLimitInSeconds() {
		return timeLimitInSeconds;
	}

	public boolean isUnlocked() {
		return isUnlocked;
	}

	public void setUnlocked(boolean unlocked) {
		this.isUnlocked = unlocked;
	}

	// --- NEW: Getter for the seed ---
	public long getSeed() {
		return seed;
	}
}