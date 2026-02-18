import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.function.Consumer;

// this is the panel for the snake minigame. slither around and eat stuff
public class SnakePanel extends JPanel {
    // how big each block is
    private final int TILE_SIZE = 20;
    // how wide the game area is in blocks
    private final int GRID_WIDTH = 30;
    // how tall the game area is in blocks
    private final int GRID_HEIGHT = 20;
    // how fast the snake moves. smaller is faster
    private final int GAME_SPEED = 120; // milliseconds
    // how much score u need to win
    private final int WIN_SCORE = 10;
    // how many seconds u have to win
    private final int TIME_LIMIT = 30; // seconds

    // the snake itself. its a list of points
    private LinkedList<Point> snake;
    // where the food is
    private Point food;
    // ur score
    private int score;
    // what way the snake is going. 0 is up, 1 down, 2 left, 3 right
    private int direction;
    // if the game is over or not
    private boolean isGameOver = false;
    // how much time is left
    private int timeLeft;

    // tells the main game if u won
    private final Consumer<Boolean> gameEndCallback;
    // timers for the game
    private Timer gameTimer; // for moving the snake
    private Timer countdownTimer; // for the time limit
    // text at the bottom
    private JLabel statusLabel;

    public SnakePanel(Consumer<Boolean> callback) {
        this.gameEndCallback = callback;
        // set the size of the panel
        setPreferredSize(new Dimension(GRID_WIDTH * TILE_SIZE, GRID_HEIGHT * TILE_SIZE + 40));
        setBackground(Color.BLACK); // cool black background
        setFocusable(true); // so it can hear key presses
        setLayout(new BorderLayout());

        // the label at the bottom for score and time
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        statusLabel.setForeground(Color.GREEN);
        add(statusLabel, BorderLayout.SOUTH);
        
        // start the game
        initGame();

        // listen for when keys are pressed
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e.getKeyCode());
            }
        });
    }

    /**
     * this starts the snake game. makes the snake and the food and timers
     */
    private void initGame() {
        // make a new snake
        snake = new LinkedList<>();
        // put the snake head in the middle
        snake.add(new Point(GRID_WIDTH / 2, GRID_HEIGHT / 2));
        direction = 3; // Start moving right
        score = 0;
        timeLeft = TIME_LIMIT;
        // put some food on the screen
        spawnFood();

        // the timer that runs the game
        gameTimer = new Timer(GAME_SPEED, e -> gameLoop());
        gameTimer.start();
        
        // the timer for the countdown
        countdownTimer = new Timer(1000, e -> {
            timeLeft--; // time goes down
            updateStatusLabel();
            if (timeLeft <= 0) {
                // if time is up u lose
                gameOver(false, "TIME'S UP! ACCESS DENIED!");
            }
        });
        countdownTimer.start();
        updateStatusLabel();
    }

    /**
     * this is the main loop of the game. it runs over and over
     */
    private void gameLoop() {
        if (isGameOver) return; // if game is over stop doing stuff
        move(); // move the snake
        checkCollision(); // check if snake hit something
        checkFood(); // check if snake ate food
        repaint(); // redraw the screen
    }

    /**
     * this moves the snake one spot in its direction
     */
    private void move() {
        // make a new head based on the old head
        Point head = new Point(snake.getFirst());
        // change the new head's position based on direction
        switch (direction) {
            case 0 -> head.y--; // Up
            case 1 -> head.y++; // Down
            case 2 -> head.x--; // Left
            case 3 -> head.x++; // Right
        }
        // add the new head to the front of the snake
        snake.addFirst(head);
        // remove the tail so it looks like its moving
        snake.removeLast();
    }

    /**
     * this checks if the snake ate the food
     */
    private void checkFood() {
        // if the head is on the food
        if (snake.getFirst().equals(food)) {
            score++; // get a point
            // make the snake longer. we put it at a fake spot for now
            snake.addLast(new Point(-1, -1)); // Grow snake, temporary position
            spawnFood(); // make new food
            updateStatusLabel();
            // if u get enough points u win
            if (score >= WIN_SCORE) {
                gameOver(true, "TARGET SCORE REACHED! ACCESS GRANTED!");
            }
        }
    }

    /**
     * checks if the snake ran into a wall or itself
     */
    private void checkCollision() {
        Point head = snake.getFirst();
        // Wall collision
        // check if it hit a wall
        if (head.x < 0 || head.x >= GRID_WIDTH || head.y < 0 || head.y >= GRID_HEIGHT) {
            gameOver(false, "WALL COLLISION! ACCESS DENIED!");
            return;
        }
        // Self collision
        // check if it hit its own body
        for (int i = 1; i < snake.size() -1; i++) {
            if (head.equals(snake.get(i))) {
                gameOver(false, "SELF COLLISION! ACCESS DENIED!");
                return;
            }
        }
    }

    /**
     * puts a new piece of food in a random spot
     */
    private void spawnFood() {
        // keep trying to find a spot for food until its not on the snake
        do {
            food = new Point((int) (Math.random() * GRID_WIDTH), (int) (Math.random() * GRID_HEIGHT));
        } while (snake.contains(food));
    }

    /**
     * this handles what to do when a key is pressed to change direction
     * @param keyCode the code for the key that was pressed
     */
    private void handleKeyPress(int keyCode) {
        switch (keyCode) {
            // cant go down if ur going up
            case KeyEvent.VK_UP: if (direction != 1) direction = 0; break;
            case KeyEvent.VK_DOWN: if (direction != 0) direction = 1; break;
            case KeyEvent.VK_LEFT: if (direction != 3) direction = 2; break;
            case KeyEvent.VK_RIGHT: if (direction != 2) direction = 3; break;
        }
    }
    
    /**
     * updates the text at the bottom of the screen with score and time
     */
    private void updateStatusLabel() {
        statusLabel.setText(String.format("Score: %d / %d | Time: %d", score, WIN_SCORE, timeLeft));
    }

    /**
     * ends the game and shows a message
     * @param success if the player won
     * @param message the message to show
     */
    private void gameOver(boolean success, String message) {
        isGameOver = true;
        gameTimer.stop(); // stop the snake
        countdownTimer.stop(); // stop the time
        statusLabel.setText(message); // show the final message
        statusLabel.setForeground(success ? Color.CYAN : Color.RED); // change color
        // wait a bit then close the game
        Timer exitTimer = new Timer(2000, e -> gameEndCallback.accept(success));
        exitTimer.setRepeats(false);
        exitTimer.start();
    }

    /**
     * this draws everything on the screen
     * @param g the graphics thingy
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Draw Food
        // draw the food as a green circle
        g2.setColor(Color.GREEN);
        g2.fillOval(food.x * TILE_SIZE, food.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

        // Draw Snake
        // draw the snake as blue squares
        for (Point p : snake) {
            g2.setColor(Color.CYAN);
            g2.fillRect(p.x * TILE_SIZE, p.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }
    }
}