import javax.swing.*;
import java.awt.*;
import java.util.Random;
import java.util.function.Consumer;

// this is the popup window for the mini games
public class MiniGameDialog extends JDialog {
    // this thingy is to tell the main game if the player won
    private final Consumer<Boolean> callback;
    // this remembers if the last game was snake or not
    public static boolean lastGameWasSnake = false;

    public MiniGameDialog(Frame owner, Consumer<Boolean> callback) {
        super(owner, "Bypassing Security...", true);
        this.callback = callback;

        // Choose a random game
        // this will hold the game panel
        JPanel gamePanel;
        // to pick a random game
        Random rand = new Random();
        int gameChoice = rand.nextInt(3); // pick 0, 1, or 2

        // pick which game to show
        switch (gameChoice) {
            case 0 -> {
                // play hangman
                gamePanel = new HangmanPanel(this::onGameEnd);
                lastGameWasSnake = false;
            }
            case 1 -> {
                // play snake
                gamePanel = new SnakePanel(this::onGameEnd);
                lastGameWasSnake = true;
            }
            default -> {
                // play memory match
                gamePanel = new MemoryMatchPanel(this::onGameEnd);
                lastGameWasSnake = false;
            }
        }

        add(gamePanel); // put the game in the window
        pack(); // make the window the right size for the game
        setLocationRelativeTo(owner); // show it in the middle of the screen
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // Close when 'X' is clicked
        setResizable(false); // u cant change the size of the window
    }

    /**
     * this runs when the mini game is over. it tells the main game and closes the window
     * @param success if the player won or not
     */
    private void onGameEnd(boolean success) {
        callback.accept(success); // tell the main game what happened
        dispose(); // close this popup window
    }
}