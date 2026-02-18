import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;


public class GamePanel extends JPanel implements Runnable, KeyListener, MouseMotionListener, MouseListener, MouseWheelListener {
	// game screen variables
	private int tileSize;
	private final int screenCols = 20;
	private final int screenRows = 18;
	private final int screenWidth = 1280;
	private final int screenHeight = 720;
	// main game play area variables
	private int gameAreaWidth;
	private int gameAreaHeight;
	private int gameAreaX;
	private int gameAreaY;

	// game loop thread and main objects
	Thread gameThread;
	Player player;
	// list of all floors in a level
	ArrayList<Floor> floors = new ArrayList<>();
	int currentFloorIndex = 0;
	// mouse position
	int mouseX;
	int mouseY;
	// timer for using teleporters
	private long lastTeleportTime = 0;
	private final long TELEPORTER_COOLDOWN = 1000;
	// list of all bullets on screen
	private final ArrayList<Bullet> bullets = new ArrayList<>();
	// is the game paused for a minigame
	private boolean isGamePaused = false;

	// what state the game is in like menu or playing
	private enum GameState {
		MAIN_MENU, LEVEL_SELECT, TUTORIAL, IN_GAME, GAME_OVER, LEVEL_WON, ITEM_SHOP
	}

	// the current game state
	private GameState currentState = GameState.MAIN_MENU;

	// level and timer stuff
	private final ArrayList<Level> levels = new ArrayList<>();
	private int highestLevelUnlocked = 1;
	private int activeLevelNumber = 1;
	private long levelTimeRemaining;
	private long lastSecondUpdateTime;

	// money and player skins
	private int coins = 0;
	private final ArrayList<CharacterSkin> characterSkins = new ArrayList<>();
	private String equippedSkinID = "char_default";
	// name of the save file
	private static final String SAVE_FILE = "byterunner.properties";
	
	private int shopScrollY = 0;

	// fonts for drawing text
	private final Font titleFont = new Font("Krungthep", Font.BOLD, 96);
	private final Font buttonFont = new Font("Monospaced", Font.BOLD, 30);
	private final Font textFont = new Font("Monospaced", Font.PLAIN, 16);
	private final Font smallFont = new Font("Monospaced", Font.BOLD, 14);
	// rectangles for menu buttons
	private Rectangle playButton;
	private Rectangle tutorialButton;
	private Rectangle exitButton;
	private Rectangle shopButton;
	private final ArrayList<Rectangle> levelButtons = new ArrayList<>();
	private final ArrayList<Rectangle> shopItemButtons = new ArrayList<>();
	private Rectangle retryButton;
	private Rectangle menuButton;

	public GamePanel() {
		// setup the panel size and color
		setPreferredSize(new Dimension(screenWidth, screenHeight));
		setBackground(Color.black);
		// makes drawing smoother
		setDoubleBuffered(true);
		// add listeners for keyboard and mouse
		addKeyListener(this);
		addMouseMotionListener(this);
		addMouseListener(this);
		// MODIFIED: Added the mouse wheel listener to enable scrolling
		addMouseWheelListener(this);
		// allows panel to get keyboard input
		setFocusable(true);

		// setup all game data
		initializeSkins();
		loadProgress();
		initializeLevels();
		setupMainMenuButtons();
	}

	/**
	 * loads game progress from a file loads level progress coins and skins
	 */
	private void loadProgress() {
		Properties props = new Properties();
		File saveFile = new File(SAVE_FILE);
		// if no save file exists stop
		if (!saveFile.exists())
			return;

		try (FileInputStream fis = new FileInputStream(saveFile)) {
			// load data from file
			props.load(fis);
			this.highestLevelUnlocked = Integer.parseInt(props.getProperty("highestLevelUnlocked", "1"));
			this.coins = Integer.parseInt(props.getProperty("coins", "0"));
			this.equippedSkinID = props.getProperty("equippedSkinID", "char_default");

			// get which skins are unlocked
			String unlockedSkinsStr = props.getProperty("unlockedSkins", "char_default");
			ArrayList<String> unlockedIds = new ArrayList<>(Arrays.asList(unlockedSkinsStr.split(",")));
			for (CharacterSkin skin : characterSkins) {
				if (unlockedIds.contains(skin.getId())) {
					skin.setUnlocked(true);
				}
			}

		} catch (IOException | NumberFormatException e) {
			// error loading file
		}
	}

	/**
	 * saves game progress to a file saves levels coins and skins
	 */
	private void saveProgress() {
		Properties props = new Properties();
		// put data into properties object
		props.setProperty("highestLevelUnlocked", String.valueOf(this.highestLevelUnlocked));
		props.setProperty("coins", String.valueOf(this.coins));
		props.setProperty("equippedSkinID", String.valueOf(this.equippedSkinID));

		// make a comma separated list of unlocked skins
		String unlockedSkinsStr = characterSkins.stream().filter(CharacterSkin::isUnlocked).map(CharacterSkin::getId)
				.collect(Collectors.joining(","));
		props.setProperty("unlockedSkins", unlockedSkinsStr);

		// write properties to the file
		try (FileOutputStream fos = new FileOutputStream(SAVE_FILE)) {
			props.store(fos, "ByteRunner Game Progress");
		} catch (IOException e) {
			// error saving file
		}
	}

	/**
	 * creates all the character skins and adds them to a list
	 */
	private void initializeSkins() {
		characterSkins.add(new CharacterSkin("char_default", "Default", 0, "player.png", true));
		characterSkins.add(new CharacterSkin("char_1", "Ronin", 50, "1.png", false));
		characterSkins.add(new CharacterSkin("char_2", "Glitch", 150, "2.png", false));
		characterSkins.add(new CharacterSkin("char_3", "Vector", 200, "3.png", false));
		characterSkins.add(new CharacterSkin("char_4", "Cypher", 265, "4.png", false));
		characterSkins.add(new CharacterSkin("char_5", "Neon", 320, "5.png", false));
		characterSkins.add(new CharacterSkin("char_6", "Apex", 400, "6.png", false));
		characterSkins.add(new CharacterSkin("char_7", "Jolt", 500, "7.png", false));
		characterSkins.add(new CharacterSkin("char_8", "Grid", 590, "8.png", false));
		characterSkins.add(new CharacterSkin("char_9", "Echo", 650, "9.png", false));
		characterSkins.add(new CharacterSkin("char_10", "Pulse", 700, "10.png", false));
		characterSkins.add(new CharacterSkin("char_11", "Reverb", 780, "11.png", false));
		characterSkins.add(new CharacterSkin("char_12", "Volt", 845, "12.png", false));
		characterSkins.add(new CharacterSkin("char_13", "Spike", 900, "13.png", false));
		characterSkins.add(new CharacterSkin("char_14", "Rogue", 950, "14.png", false));
		characterSkins.add(new CharacterSkin("char_15", "Nova", 1000, "15.png", false));
		characterSkins.add(new CharacterSkin("char_16", "Orion", 1050, "16.png", false));
		characterSkins.add(new CharacterSkin("char_17", "Helix", 1100, "17.png", false));
		characterSkins.add(new CharacterSkin("char_18", "Fuse", 1150, "18.png", false));
		characterSkins.add(new CharacterSkin("char_19", "Blaze", 1200, "19.png", false));
		characterSkins.add(new CharacterSkin("char_20", "Flux", 1250, "20.png", false));
		characterSkins.add(new CharacterSkin("char_21", "Warden", 1300, "21.png", false));
		characterSkins.add(new CharacterSkin("char_22", "Kilo", 1350, "22.png", false));
		characterSkins.add(new CharacterSkin("char_23", "Byte", 1400, "23.png", false));
		characterSkins.add(new CharacterSkin("char_24", "Axon", 1450, "24.png", false));
		characterSkins.add(new CharacterSkin("char_25", "Catalyst", 1500, "25.png", false));
		characterSkins.add(new CharacterSkin("char_26", "Zenith", 1550, "26.png", false));
		characterSkins.add(new CharacterSkin("char_27", "Vortex", 1600, "27.png", false));
		characterSkins.add(new CharacterSkin("char_28", "Proxy", 1600, "28.png", false));
		characterSkins.add(new CharacterSkin("char_29", "Phantom", 1600, "29.png", false));
	}

	/**
	 * creates all the levels in the game each level has a number of floors and a
	 * time limit
	 */
	private void initializeLevels() {
		int baseTime = 120;
		int numFloors = 2;
		// master random generator for all levels
		Random seedGenerator = new Random();

		// create 20 levels
		for (int i = 1; i <= 20; i++) {
			// every 2 levels add another floor
			if (i > 1 && (i - 1) % 2 == 0) {
				numFloors++;
				baseTime += 0;
			}
			// give each level a unique seed for generation
			long levelSeed = seedGenerator.nextLong();
			levels.add(new Level(i, numFloors, baseTime, i <= highestLevelUnlocked, levelSeed));
		}
	}

	/**
	 * sets up and starts a specific level clears old data and creates new floors
	 * and player
	 */
	private void startLevel(int levelNumber) {
		activeLevelNumber = levelNumber;
		Level level = levels.get(levelNumber - 1);
		// clear old level data
		floors.clear();
		bullets.clear();
		currentFloorIndex = 0;
		// set timer for the level
		levelTimeRemaining = level.getTimeLimitInSeconds() * 1000L;
		lastSecondUpdateTime = System.nanoTime();

		// create all floors for the level
		for (int i = 0; i < level.getNumberOfFloors(); i++) {
			long floorSeed = level.getSeed() + i;
			floors.add(new Floor(screenCols, screenRows, i, tileSize, floorSeed));
		}

		// create a new player at the start position
		Floor startFloor = floors.get(0);
		player = new Player(startFloor.getStartX() * tileSize + tileSize / 2.0,
				startFloor.getStartY() * tileSize + tileSize / 2.0);

		// load the equipped skin for the player
		characterSkins.stream().filter(s -> s.getId().equals(equippedSkinID)).findFirst()
				.ifPresent(s -> player.loadSkin(s.getImagePath()));

		// change game state to playing
		currentState = GameState.IN_GAME;
	}

	/**
	 * creates the game thread and starts it
	 */
	public void startGameThread() {
		gameThread = new Thread(this);
		gameThread.start();
	}

	/**
	 * the main game loop runs 60 times per second to update and draw the game
	 */
	@Override
	public void run() {
		// calculate game area size based on screen size
		this.tileSize = Math.min(screenWidth / screenCols, screenHeight / screenRows);
		this.gameAreaWidth = tileSize * screenCols;
		this.gameAreaHeight = tileSize * screenRows;
		this.gameAreaX = (screenWidth - gameAreaWidth) / 2;
		this.gameAreaY = (screenHeight - gameAreaHeight) / 2;

		// game loop timing variables
		double drawInterval = 1000000000.0 / 60.0;
		double delta = 0;
		long lastTime = System.nanoTime();

		// game loop
		while (gameThread != null) {
			long currentTime = System.nanoTime();
			delta += (currentTime - lastTime) / drawInterval;
			lastTime = currentTime;
			// if enough time has passed update and draw
			if (delta >= 1) {
				update();
				repaint();
				delta--;
			}
		}
	}

	/**
	 * updates all game logic moves player enemies bullets and checks for game over
	 */
	public void update() {
		// dont update if not in game or paused
		if (currentState != GameState.IN_GAME || isGamePaused)
			return;

		// check for game over condition
		if (player.health <= 0 || levelTimeRemaining <= 0) {
			currentState = GameState.GAME_OVER;
			setupGameOverButtons();
			return;
		}

		// update timer every second
		if (System.nanoTime() - lastSecondUpdateTime >= 1000000000) {
			levelTimeRemaining -= 1000;
			lastSecondUpdateTime = System.nanoTime();
		}

		Floor currentFloor = floors.get(currentFloorIndex);
		// update player and point them toward mouse
		player.update(currentFloor, tileSize);
		player.setFacingAngle(Math.atan2(mouseY - (gameAreaY + gameAreaHeight / 2.0), mouseX - (gameAreaX + gameAreaWidth / 2.0)));

		// update all enemies on the current floor
		Iterator<Enemy> enemyIterator = currentFloor.enemies.iterator();
		while (enemyIterator.hasNext()) {
			Enemy enemy = enemyIterator.next();
			// if enemy is dead remove it and give coins
			if (enemy.health <= 0) {
				enemyIterator.remove();
				coins += 10;
				saveProgress();
				continue;
			}
			enemy.update(player, currentFloor, tileSize, bullets);
		}

		// update bullets and check for teleporting
		updateBullets(currentFloor);
		teleportPlayer(currentFloor);
	}

	/**
	 * checks if player is on a teleporter tile moves player between floors or wins
	 * the level
	 */
	private void teleportPlayer(Floor currentFloor) {
		int px = (int) (player.x / tileSize);
		int py = (int) (player.y / tileSize);

		// only teleport if cooldown is over
		if (System.currentTimeMillis() - lastTeleportTime > TELEPORTER_COOLDOWN) {
			// if on an UP teleporter
			if (currentFloor.getTile(px, py) == Floor.UP) {
				// cant use if computer isnt hacked
				if (!currentFloor.isComputerHacked())
					return;

				// if this is the last floor player wins
				if (currentFloorIndex == floors.size() - 1) {
					currentState = GameState.LEVEL_WON;
					coins += 20;
					// unlock next level if this was the latest one
					if (activeLevelNumber == highestLevelUnlocked && highestLevelUnlocked < levels.size()) {
						highestLevelUnlocked++;
						levels.get(highestLevelUnlocked - 1).setUnlocked(true);
					}
					saveProgress();
					menuButton = new Rectangle((screenWidth - 350) / 2, 300, 350, 50);
					return;
				}

				// move to the next floor
				currentFloorIndex++;
				Floor newFloor = floors.get(currentFloorIndex);
				player.setPosition(newFloor.getLinkedDownX() * tileSize + tileSize / 2.0,
						newFloor.getLinkedDownY() * tileSize + tileSize / 2.0);
				lastTeleportTime = System.currentTimeMillis();

				// if on a DOWN teleporter
			} else if (currentFloor.getTile(px, py) == Floor.DOWN && currentFloorIndex > 0) {
				// move to the previous floor
				currentFloorIndex--;
				Floor newFloor = floors.get(currentFloorIndex);
				player.setPosition(newFloor.getLinkedUpX() * tileSize + tileSize / 2.0,
						newFloor.getLinkedUpY() * tileSize + tileSize / 2.0);
				lastTeleportTime = System.currentTimeMillis();
			}
		}
	}

	/**
	 * updates all bullets moves them and checks for collisions with walls enemies
	 * or player
	 */
	private void updateBullets(Floor currentFloor) {
		Iterator<Bullet> bulletIterator = bullets.iterator();
		while (bulletIterator.hasNext()) {
			Bullet bullet = bulletIterator.next();
			bullet.update();
			int tileX = (int) (bullet.x / tileSize);
			int tileY = (int) (bullet.y / tileSize);

			// if bullet hits a wall remove it
			if (!currentFloor.isWalkable(tileX, tileY)) {
				bulletIterator.remove();
				continue;
			}

			// if its a player bullet check for hitting enemies
			if (bullet.isPlayerBullet) {
				for (Enemy enemy : currentFloor.enemies) {
					if (bullet.getBounds().intersects(enemy.getBounds())) {
						enemy.takeDamage(1);
						bulletIterator.remove();
						break;
					}
				}
				// if its an enemy bullet check for hitting player
			} else {
				if (bullet.getBounds().intersects(player.getBounds()) && !player.isInvincible) {
					player.takeDamage(1);
					bulletIterator.remove();
				}
			}
		}
	}

	/**
	 * checks for a computer terminal nearby and starts the minigame
	 */
	private void interact() {
		int px = (int) (player.x / tileSize);
		int py = (int) (player.y / tileSize);
		Floor currentFloor = floors.get(currentFloorIndex);

		// check all tiles around the player
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (i == 0 && j == 0)
					continue;

				// if a computer is found
				if (currentFloor.getTile(px + i, py + j) == Floor.COMPUTER) {
					// dont interact if already hacked or on cooldown
					if (currentFloor.isComputerHacked()
							|| System.currentTimeMillis() < currentFloor.getComputerCooldownTime())
						return;

					// pause the game and open the minigame window
					isGamePaused = true;
					JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
					MiniGameDialog miniGameDialog = new MiniGameDialog(topFrame, (win) -> {
						// this code runs after minigame is closed
						if (win) {
							currentFloor.setComputerHacked(true);
							coins += 5;
							saveProgress();
						} else {
							// if player loses snake game start cooldown
							if (MiniGameDialog.lastGameWasSnake) {
								currentFloor.setComputerCooldownTime(System.currentTimeMillis() + 5000);
							}
						}
						// unpause the game
						isGamePaused = false;
						this.requestFocusInWindow();
					});
					miniGameDialog.setVisible(true);
					return;
				}
			}
		}
	}

	/**
	 * main drawing function calls the correct draw function based on game state
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		// make graphics look smooth
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// choose what to draw
		switch (currentState) {
		case MAIN_MENU -> drawMainMenu(g2);
		case LEVEL_SELECT -> drawLevelSelect(g2);
		case TUTORIAL -> drawTutorial(g2);
		case ITEM_SHOP -> drawItemShop(g2);
		case IN_GAME -> drawInGame(g2);
		case GAME_OVER -> drawGameOver(g2);
		case LEVEL_WON -> drawLevelWon(g2);
		}
	}

	/**
	 * draws a string centered horizontally on the screen
	 */
	private void drawCenteredString(Graphics2D g2, String text, Font font, int y) {
		g2.setFont(font);
		FontMetrics fm = g2.getFontMetrics();
		int x = (screenWidth - fm.stringWidth(text)) / 2;
		g2.drawString(text, x, y);
	}

	/**
	 * draws a string centered inside a rectangle
	 */
	private void drawCenteredStringInRect(Graphics2D g2, String text, Font font, Rectangle rect) {
		g2.setFont(font);
		FontMetrics fm = g2.getFontMetrics(font);
		int x = rect.x + (rect.width - fm.stringWidth(text)) / 2;
		int y = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();
		g2.drawString(text, x, y);
	}

	/**
	 * draws the main menu screen with title and buttons
	 */
	private void drawMainMenu(Graphics2D g2) {
		// draw background
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, screenWidth, screenHeight);
		drawBackgroundGrid(g2);

		// draw title with a glow effect
		g2.setFont(titleFont);
		FontMetrics fm = g2.getFontMetrics(titleFont);
		String titleText = "ByteRunner";
		int titleX = (screenWidth - fm.stringWidth(titleText)) / 2;
		int titleY = 200;

		// draw the glow
		g2.setColor(new Color(0, 255, 255, 60));
		g2.drawString(titleText, titleX + 3, titleY + 3);
		g2.drawString(titleText, titleX - 3, titleY + 3);
		g2.drawString(titleText, titleX + 3, titleY - 3);
		g2.drawString(titleText, titleX - 3, titleY - 3);

		// draw the main text
		g2.setColor(Color.CYAN);
		g2.drawString(titleText, titleX, titleY);

		// draw buttons
		drawHoverButton(g2, "Play", playButton);
		drawHoverButton(g2, "Item Shop", shopButton);
		drawHoverButton(g2, "Tutorial", tutorialButton);
		drawHoverButton(g2, "Exit", exitButton);

		// draw coin amount
		String coinText = "💰 " + coins;
		g2.setFont(buttonFont);
		g2.setColor(Color.YELLOW);
		g2.drawString(coinText, 20, 40);

		// draw old tv scanline effect
		g2.setColor(new Color(0, 0, 0, 70));
		for (int i = 0; i < screenHeight; i += 3) {
			g2.fillRect(0, i, screenWidth, 1);
		}
	}

	/**
	 * helper method to draw a button that highlights when moused over
	 */
	private void drawHoverButton(Graphics2D g2, String text, Rectangle button) {
		g2.setFont(buttonFont);

		// if mouse is on the button
		if (button.contains(mouseX, mouseY)) {
			// draw filled cyan button
			g2.setColor(Color.CYAN);
			g2.fill(button);
			g2.setColor(Color.BLACK);
			drawCenteredStringInRect(g2, text, buttonFont, button);
		} else {
			// draw white outline button
			g2.setColor(Color.WHITE);
			g2.setStroke(new BasicStroke(2));
			g2.draw(button);
			g2.setColor(Color.WHITE);
			drawCenteredStringInRect(g2, text, buttonFont, button);
		}
	}

	/**
	 * creates the rectangle objects for the main menu buttons
	 */
	private void setupMainMenuButtons() {
		int buttonWidth = 300;
		int buttonHeight = 50;
		int centerX = (screenWidth - buttonWidth) / 2;
		int startY = 320;
		int gap = 70;
		playButton = new Rectangle(centerX, startY, buttonWidth, buttonHeight);
		shopButton = new Rectangle(centerX, startY + gap, buttonWidth, buttonHeight);
		tutorialButton = new Rectangle(centerX, startY + gap * 2, buttonWidth, buttonHeight);
		exitButton = new Rectangle(centerX, startY + gap * 3, buttonWidth, buttonHeight);
	}

	/**
	 * draws the level selection screen with a grid of level buttons
	 */
	private void drawLevelSelect(Graphics2D g2) {
		// draw background and title
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, screenWidth, screenHeight);
		g2.setColor(Color.GREEN);
		drawCenteredString(g2, "Select Level", new Font("Monospaced", Font.BOLD, 72), 100);

		// setup button grid
		levelButtons.clear();
		int buttonWidth = 200, buttonHeight = 60;
		int padding = 20;
		int startX = (screenWidth - (5 * buttonWidth + 4 * padding)) / 2;
		int startY = 200;

		// draw each level button
		for (int i = 0; i < levels.size(); i++) {
			Level level = levels.get(i);
			int col = i % 5;
			int row = i / 5;
			int x = startX + col * (buttonWidth + padding);
			int y = startY + row * (buttonHeight + padding);

			Rectangle button = new Rectangle(x, y, buttonWidth, buttonHeight);
			levelButtons.add(button);

			// locked levels are gray
			g2.setColor(level.isUnlocked() ? Color.WHITE : Color.DARK_GRAY);
			g2.draw(button);
			drawCenteredStringInRect(g2, "Level " + (i + 1), buttonFont, button);
		}

		// draw back button
		menuButton = new Rectangle((screenWidth - 250) / 2, screenHeight - 100, 250, 50);
		g2.setColor(Color.WHITE);
		g2.draw(menuButton);
		drawCenteredStringInRect(g2, "Main Menu", buttonFont, menuButton);
	}

	/**
	 * draws the item shop for buying and equipping skins
	 */
	private void drawItemShop(Graphics2D g2) {
		// draw background and title
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, screenWidth, screenHeight);
		g2.setColor(Color.MAGENTA);
		drawCenteredString(g2, "Item Shop", new Font("Monospaced", Font.BOLD, 72), 80);

		// draw coin amount
		String coinText = "💰 " + coins;
		g2.setFont(buttonFont);
		g2.setColor(Color.YELLOW);
		FontMetrics fm = g2.getFontMetrics();
		g2.drawString(coinText, screenWidth - fm.stringWidth(coinText) - 20, 55);

		// setup item grid
		shopItemButtons.clear();
		int gridCols = 6;
		int itemWidth = 160;
		int itemHeight = 180;
		int hPadding = 30;
		int vPadding = 20;
		int totalGridWidth = gridCols * itemWidth + (gridCols - 1) * hPadding;
		int startX = (screenWidth - totalGridWidth) / 2;
		int startY = 120;
		
		// NEW: Add a hint to let the user know they can scroll
        g2.setColor(Color.LIGHT_GRAY);
        g2.setFont(textFont);
        drawCenteredString(g2, "Use the mouse wheel to scroll", textFont, startY - 15);

		// draw each shop item
		for (int i = 0; i < characterSkins.size(); i++) {
			CharacterSkin skin = characterSkins.get(i);
			int col = i % gridCols;
			int row = i / gridCols;
			int x = startX + col * (itemWidth + hPadding);
			// MODIFIED: The Y position is now offset by the scroll amount
			int y = startY + row * (itemHeight + vPadding) - shopScrollY;
			
			// Only draw items that are visible on the screen
            if (y > screenHeight || y + itemHeight < startY) {
                // Add a placeholder rectangle for click detection to work correctly even for off-screen items
                shopItemButtons.add(new Rectangle(x, y, itemWidth, itemHeight));
                continue;
            }

			Rectangle itemBox = new Rectangle(x, y, itemWidth, itemHeight);
			shopItemButtons.add(itemBox);

			// draw item box
			g2.setColor(new Color(40, 40, 40));
			g2.fill(itemBox);
			g2.setColor(Color.MAGENTA);
			g2.draw(itemBox);

			// draw skin preview image
			Image previewImg = skin.getPreviewImage();
			if (previewImg != null) {
				g2.drawImage(previewImg, x + 40, y + 20, itemWidth - 80, 80, null);
			}

			// draw skin name
			g2.setColor(Color.WHITE);
			drawCenteredStringInRect(g2, skin.getName(), textFont, new Rectangle(x, y + 100, itemWidth, 40));

			// draw owned text or price
			if (skin.isUnlocked()) {
				g2.setColor(Color.GREEN);
				drawCenteredStringInRect(g2, "Owned", smallFont, new Rectangle(x, y + 135, itemWidth, 40));
			} else {
				g2.setColor(Color.YELLOW);
				drawCenteredStringInRect(g2, "💰 " + skin.getPrice(), textFont,
						new Rectangle(x, y + 135, itemWidth, 40));
			}

			// draw a green border around the equipped skin
			if (skin.getId().equals(equippedSkinID)) {
				g2.setColor(Color.GREEN);
				g2.setStroke(new BasicStroke(3));
				g2.draw(itemBox);
				g2.setStroke(new BasicStroke(1));
			}
		}

		// draw back button
		menuButton = new Rectangle((screenWidth - 250) / 2, screenHeight - 80, 250, 50);
		g2.setColor(Color.WHITE);
		g2.draw(menuButton);
		drawCenteredStringInRect(g2, "Main Menu", buttonFont, menuButton);
	}

	/**
	 * draws the tutorial screen with instructions
	 */
	private void drawTutorial(Graphics2D g2) {
		// draw background and title
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, screenWidth, screenHeight);
		g2.setColor(Color.GREEN);
		drawCenteredString(g2, "Tutorial", new Font("Monospaced", Font.BOLD, 72), 80);

		// draw tutorial text lines
		g2.setColor(Color.WHITE);
		g2.setFont(textFont);
		String[] lines = { "Objective: Hack the yellow terminal to unlock the green teleporter and advance.",
				"Reach the final teleporter to win the level before the timer runs out.", "", "Controls:",
				"- W, A, S, D: Move", "- Mouse: Aim", "- Left-Click: Shoot", "- R: Reload",
				"- E: Interact with Terminals", "", "Tiles:", "- Yellow: Computer Terminal (Hack with 'E')",
				"- Red/Green: UP Teleporter (Locked/Unlocked)", "- Orange: DOWN Teleporter" };
		for (int i = 0; i < lines.length; i++) {
			FontMetrics fm = g2.getFontMetrics();
			int strX = (screenWidth - fm.stringWidth(lines[i])) / 2;
			g2.drawString(lines[i], strX, 150 + i * 25);
		}

		// draw back button
		menuButton = new Rectangle((screenWidth - 250) / 2, screenHeight - 100, 250, 50);
		g2.setColor(Color.WHITE);
		g2.draw(menuButton);
		drawCenteredStringInRect(g2, "Main Menu", buttonFont, menuButton);
	}

	/**
	 * draws the game over screen
	 */
	private void drawGameOver(Graphics2D g2) {
		// draw the final game state in the background
		drawInGame(g2);
		// draw a dark overlay
		g2.setColor(new Color(0, 0, 0, 150));
		g2.fillRect(0, 0, screenWidth, screenHeight);

		// draw game over text
		g2.setColor(Color.RED);
		drawCenteredString(g2, "Mission Failed", new Font("Monospaced", Font.BOLD, 72), 200);

		// draw retry and menu buttons
		g2.setFont(buttonFont);
		g2.setColor(Color.WHITE);
		g2.draw(retryButton);
		g2.draw(menuButton);
		drawCenteredStringInRect(g2, "Retry", buttonFont, retryButton);
		drawCenteredStringInRect(g2, "Main Menu", buttonFont, menuButton);
	}

	/**
	 * creates the rectangle objects for the game over buttons
	 */
	private void setupGameOverButtons() {
		int buttonWidth = 250;
		int buttonHeight = 50;
		int centerX = (screenWidth - buttonWidth) / 2;
		retryButton = new Rectangle(centerX, 300, buttonWidth, buttonHeight);
		menuButton = new Rectangle(centerX, 370, buttonWidth, buttonHeight);
	}

	/**
	 * draws the level won screen
	 */
	private void drawLevelWon(Graphics2D g2) {
		// draw final game state in background
		drawInGame(g2);
		// draw a dark overlay
		g2.setColor(new Color(0, 0, 0, 150));
		g2.fillRect(0, 0, screenWidth, screenHeight);

		// draw level complete text
		g2.setColor(Color.GREEN);
		drawCenteredString(g2, "Level Complete!", new Font("Monospaced", Font.BOLD, 72), 200);

		// draw menu button
		g2.setColor(Color.WHITE);
		g2.draw(menuButton);
		drawCenteredStringInRect(g2, "Return to Menu", buttonFont, menuButton);
	}

	/**
	 * draws the main game view including floor player enemies bullets and a camera
	 */
	private void drawInGame(Graphics2D g2) {
		Graphics2D g2d = (Graphics2D) g2.create();

		// draw background
		g2d.setColor(Color.BLACK);
		g2d.fillRect(0, 0, screenWidth, screenHeight);
		drawBackgroundGrid(g2d);

		// setup camera to follow player
		g2d.translate(gameAreaX, gameAreaY);
		int camX = (int) (player.x - gameAreaWidth / 2.0);
		int camY = (int) (player.y - gameAreaHeight / 2.0);
		g2d.translate(-camX, -camY);

		// draw all game objects
		floors.get(currentFloorIndex).draw(g2d, tileSize);
		player.draw(g2d);
		for (Bullet bullet : bullets)
			bullet.draw(g2d);

		// move camera back to draw ui
		g2d.translate(camX, camY);
		g2d.translate(-gameAreaX, -gameAreaY);

		// draw ui on top of everything
		drawPlayerUI(g2d);
		g2d.dispose();
	}

	/**
	 * draws a cool retro grid in the background
	 */
	private void drawBackgroundGrid(Graphics2D g2) {
		int gridSize = tileSize > 0 ? tileSize / 2 : 20;
		if (gridSize <= 0)
			return;
		Color glowColor = new Color(255,0,0);;
		// draw bright red lines if in game other wise cyan lines
		if(currentState == GameState.MAIN_MENU) {
			glowColor = new Color(0, 255, 255, 50);
		}
		g2.setColor(glowColor);
		g2.setStroke(new BasicStroke(1));
		for (int x = 0; x < screenWidth; x += gridSize)
			g2.drawLine(x, 0, x, screenHeight);
		for (int y = 0; y < screenHeight; y += gridSize)
			g2.drawLine(0, y, screenWidth, y);
	}

	/**
	 * draws the players heads up display health bar ammo count timer and floor
	 * number
	 */
	private void drawPlayerUI(Graphics2D g2) {
		// draw health bar
		int barWidth = 150, barHeight = 20;
		int barX = gameAreaX + gameAreaWidth - barWidth - 10;
		int barY = gameAreaY + 10;

		g2.setColor(Color.DARK_GRAY);
		g2.fillRect(barX, barY, barWidth, barHeight);
		double healthPercentage = (player != null && player.health > 0) ? (double) player.health / player.maxHealth : 0;
		g2.setColor(Color.GREEN);
		g2.fillRect(barX, barY, (int) (barWidth * healthPercentage), barHeight);
		g2.setColor(Color.WHITE);
		g2.drawRect(barX, barY, barWidth, barHeight);

		// draw ammo text
		g2.setFont(new Font("Monospaced", Font.BOLD, 20));
		g2.setColor(Color.CYAN);
		String ammoText = "";
		if (player != null) {
			ammoText = "AMMO: " + player.ammo + " / " + player.maxAmmo;
			if (player.isReloading)
				ammoText = "RELOADING...";
			else if (player.ammo == 0)
				ammoText = "RELOAD! (R)";
		}
		int textWidth = g2.getFontMetrics().stringWidth(ammoText);
		g2.drawString(ammoText, gameAreaX + gameAreaWidth - textWidth - 10, barY + 50);

		// draw timer
		int minutes = (int) (levelTimeRemaining / 1000) / 60;
		int seconds = (int) (levelTimeRemaining / 1000) % 60;
		String timeText = String.format("TIME: %02d:%02d", minutes, seconds);
		g2.setColor(levelTimeRemaining < 30000 ? Color.RED : Color.ORANGE);
		drawCenteredString(g2, timeText, buttonFont, 40);

		// draw floor number
		String floorText = "Floor: " + (currentFloorIndex + 1) + " / " + floors.size();
		g2.setColor(Color.WHITE);
		g2.setFont(new Font("Monospaced", Font.BOLD, 20));
		g2.drawString(floorText, gameAreaX + 10, gameAreaY + 30);
	}

	/**
	 * handles when a key is pressed down
	 */
	@Override
	public void keyPressed(KeyEvent e) {
		// only handle keys if in game and not paused
		if (currentState == GameState.IN_GAME && !isGamePaused && player != null) {
			player.keyPressed(e);
			// if e key is pressed interact
			if (e.getKeyCode() == KeyEvent.VK_E) {
				interact();
			}
		}
	}

	/**
	 * handles when a key is released
	 */
	@Override
	public void keyReleased(KeyEvent e) {
		// only handle keys if in game and not paused
		if (currentState == GameState.IN_GAME && !isGamePaused && player != null) {
			player.keyReleased(e);
		}
	}

	/**
	 * handles when the mouse button is pressed logic changes based on the game
	 * state
	 */
	public void mousePressed(MouseEvent e) {
		Point p = e.getPoint();
		switch (currentState) {
		// if on main menu check menu buttons
		case MAIN_MENU:
			if (playButton.contains(p)) {
                currentState = GameState.LEVEL_SELECT;
            } else if (shopButton.contains(p)) {
                
                shopScrollY = 0;
                currentState = GameState.ITEM_SHOP;
            } else if (tutorialButton.contains(p)) {
                currentState = GameState.TUTORIAL;
            } else if (exitButton.contains(p)) {
                System.exit(0);
            }
			break;
		// if in shop check shop buttons
		case ITEM_SHOP:
			
            for (int i = 0; i < shopItemButtons.size(); i++) {
                if (shopItemButtons.get(i).contains(p)) {
                    CharacterSkin clickedSkin = characterSkins.get(i);
                    // if skin is unlocked equip it
                    if (clickedSkin.isUnlocked()) {
                        equippedSkinID = clickedSkin.getId();
                        saveProgress();
                    // if have enough coins buy and equip it
                    } else if (coins >= clickedSkin.getPrice()) {
                        coins -= clickedSkin.getPrice();
                        clickedSkin.setUnlocked(true);
                        equippedSkinID = clickedSkin.getId();
                        saveProgress();
                    }
                    return; // Exit after handling a click
                }
            }
            if (menuButton.contains(p)) {
                currentState = GameState.MAIN_MENU;
            }
			break;
		// if on level select check level buttons
		case LEVEL_SELECT:
			for (int i = 0; i < levelButtons.size(); i++) {
				if (levelButtons.get(i).contains(p) && levels.get(i).isUnlocked()) {
					startLevel(i + 1);
					return;
				}
			}
			if (menuButton.contains(p)) {
				currentState = GameState.MAIN_MENU;
			}
			break;
		// if game over check buttons
		case GAME_OVER:
			if (retryButton.contains(p)) {
				startLevel(activeLevelNumber);
			} else if (menuButton.contains(p)) {
				currentState = GameState.MAIN_MENU;
			}
			break;
		// if level won check button
		case LEVEL_WON:
			if (menuButton.contains(p)) {
				currentState = GameState.MAIN_MENU;
			}
			break;
		// if in game shoot bullet
		case IN_GAME:
			if (!isGamePaused && player != null) {
				Bullet b = player.shoot();
				if (b != null) {
					bullets.add(b);
				}
			}
			break;
		// if on tutorial screen check back button
		case TUTORIAL:
			if (menuButton.contains(p)) {
				currentState = GameState.MAIN_MENU;
			}
			break;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// update mouse position for aiming
		mouseX = e.getX();
		mouseY = e.getY();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// update mouse position for aiming and button hovering
		mouseX = e.getX();
		mouseY = e.getY();
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}
	
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // only scroll if we are in the item shop
        if (currentState == GameState.ITEM_SHOP) {
            int scrollSpeed = 25;
            shopScrollY += e.getWheelRotation() * scrollSpeed;

            // calculate the maximum scroll position so we don't scroll past the last item
            int gridCols = 6;
            int itemHeight = 180;
            int vPadding = 20;
            int startY = 120;
            int numRows = (characterSkins.size() + gridCols - 1) / gridCols;
            
            // the total height of all the items in the grid
            int totalContentHeight = numRows * (itemHeight + vPadding);
            // the height of the visible area for items
            int viewableHeight = screenHeight - startY - 100; // Subtract space for title and menu button
            
            int maxScrollY = Math.max(0, totalContentHeight - viewableHeight);

            // clamp the scroll value between 0 and the maximum scroll position
            shopScrollY = Math.max(0, Math.min(shopScrollY, maxScrollY));
        }
    }

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
}