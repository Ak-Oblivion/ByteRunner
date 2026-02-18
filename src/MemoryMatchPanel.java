import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

// this is the memory match game panel. where u match the cards
public class MemoryMatchPanel extends JPanel {
    // how big the grid of cards is. 4x4
    private final int GRID_SIZE = 4;
    // how many pairs of cards there are
    private final int NUM_PAIRS = (GRID_SIZE * GRID_SIZE) / 2;
    // the pictures on the cards
    private final String[] SYMBOLS = {"@", "#", "$", "%", "&", "*", "?", "!"};
    // how many seconds u have to finish
    private final int TIME_LIMIT = 30; // seconds

    // tells the main game if u won or not
    private final Consumer<Boolean> gameEndCallback;
    // a list of all the card buttons
    private final List<JButton> cards = new ArrayList<>();
    // a list of the pictures for the cards
    private final List<String> cardSymbols = new ArrayList<>();
    // the first card u click on
    private JButton firstCard = null;
    // how many pairs u found
    private int pairsFound = 0;
    // how much time is left
    private int timeLeft;
    
    // the label that shows the time
    private JLabel statusLabel;
    // the timer that counts down
    private Timer countdownTimer;

    public MemoryMatchPanel(Consumer<Boolean> callback) {
        this.gameEndCallback = callback;
        // make the panel the right size
        setPreferredSize(new Dimension(500, 500));
        // cool hacker color
        setBackground(Color.BLACK);
        // how to put stuff on the panel
        setLayout(new BorderLayout(10, 10));
        
        // get the game ready
        initGame();
    }

    /**
     * this starts the game. makes the cards and the timer
     */
    private void initGame() {
        // Setup card symbols
        // put the symbols in the list. two of each
        for (int i = 0; i < NUM_PAIRS; i++) {
            cardSymbols.add(SYMBOLS[i]);
            cardSymbols.add(SYMBOLS[i]);
        }
        // mix up the cards so they r random
        Collections.shuffle(cardSymbols);

        // Setup UI
        // make the grid for the cards
        JPanel cardGrid = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE, 5, 5));
        cardGrid.setBackground(Color.BLACK);

        // create all the card buttons
        for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
            JButton card = new JButton();
            card.setFont(new Font("Monospaced", Font.BOLD, 36)); // big text
            card.setBackground(new Color(20, 20, 20));
            card.setForeground(Color.CYAN);
            card.setFocusPainted(false); // no ugly box
            card.setBorder(BorderFactory.createLineBorder(Color.GREEN));
            // hide the symbol on the card
            card.putClientProperty("symbol", cardSymbols.get(i));
            // what happens when u click a card
            card.addActionListener(new CardClickListener());
            cards.add(card); // add it to our list
            cardGrid.add(card); // add it to the screen
        }
        add(cardGrid, BorderLayout.CENTER); // put the grid in the middle
        
        // the text at the bottom for the time
        statusLabel = new JLabel("Time left: " + TIME_LIMIT, SwingConstants.CENTER);
        statusLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        statusLabel.setForeground(Color.GREEN);
        add(statusLabel, BorderLayout.SOUTH);
        
        // the timer for the countdown
        timeLeft = TIME_LIMIT;
        countdownTimer = new Timer(1000, e -> {
            timeLeft--; // one second less
            if (timeLeft >= 0) {
                statusLabel.setText("Time left: " + timeLeft); // update the time
            } else {
                // if time is up u lose
                gameOver(false, "TIME'S UP! ACCESS DENIED!");
            }
        });
        countdownTimer.start(); // start the timer
    }

    /**
     * this is what happens when the game is over. win or lose
     * @param success if you won or not
     * @param message the message to show the player
     */
    private void gameOver(boolean success, String message) {
        countdownTimer.stop(); // stop the clock
        for(JButton card : cards) card.setEnabled(false); // turn off all the cards
        statusLabel.setText(message); // show the win or lose message
        statusLabel.setForeground(success ? Color.CYAN : Color.RED); // change color for win or lose
        // wait a bit then close the game
        Timer exitTimer = new Timer(2000, e -> gameEndCallback.accept(success));
        exitTimer.setRepeats(false); // just one time
        exitTimer.start();
    }

    // this class listens for when u click a card
    private class CardClickListener implements ActionListener {
        /**
         * this runs when a card is clicked
         * @param e the click event
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            JButton clickedCard = (JButton) e.getSource();

            // if this is the first card u flipped
            if (firstCard == null) { // First card flipped
                firstCard = clickedCard;
                // show the symbol on the card
                firstCard.setText((String) firstCard.getClientProperty("symbol"));
            } else { // Second card flipped
                // if u click the same card twice dont do anything
                if (clickedCard == firstCard) return; // Clicked the same card twice

                // show the symbol on the second card
                clickedCard.setText((String) clickedCard.getClientProperty("symbol"));
                
                // Check for match
                // check if the two cards match
                if (firstCard.getClientProperty("symbol").equals(clickedCard.getClientProperty("symbol"))) {
                    // its a match!
                    firstCard.setEnabled(false); // turn them off
                    clickedCard.setEnabled(false);
                    firstCard.setBackground(Color.DARK_GRAY); // change color
                    clickedCard.setBackground(Color.DARK_GRAY);
                    pairsFound++; // one more pair found
                    firstCard = null; // get ready for the next pair
                    // if u found all the pairs u win
                    if(pairsFound == NUM_PAIRS) {
                        gameOver(true, "ALL PAIRS FOUND! ACCESS GRANTED!");
                    }
                } else {
                    // Not a match, flip them back after a delay
                    // not a match so flip them back over
                    JButton card1 = firstCard;
                    JButton card2 = clickedCard;
                    firstCard = null; // so u cant click more cards
                    
                    // a timer to wait a little bit before flipping back
                    Timer flipBackTimer = new Timer(500, ae -> {
                        card1.setText(""); // hide the symbol
                        card2.setText("");
                    });
                    flipBackTimer.setRepeats(false); // only one time
                    flipBackTimer.start();
                }
            }
        }
    }
}