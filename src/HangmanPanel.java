import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

// this whole thing is the hangman game panel. it holds all the stuff for the game
public class HangmanPanel extends JPanel {
    // these r the words the player has to guess. all secret like
    private final String[] WORDS = {"PROTOCOL", "ENCRYPTION", "FIREWALL", "ALGORITHM", "DATABASE", "KEYLOGGER", "MALWARE", "NETWORK", "SERVER", "KERNEL", "OVERRIDE", "GLITCH", "VECTOR", "CYPHER", "NEON", "APEX", "JOLT","GRID", "ECHO", "PULSE", "REVERB", "VOLT", "SPIKE", "ROGUE", "NOVA","ORION", "HELIX", "FUSE", "BLAZE", "FLUX", "WARDEN", "KILO", "BYTE","AXON", "CATALYST", "ZENITH", "VORTEX", "PROXY", "PHANTOM", "STATIC","ROOT", "HEX", "NOMAD", "GHOST", "CIPHER", "DAEMON", "MAINFRAME","PHREAK", "ZERO-DAY", "KILLSWITCH", "BACKDOOR", "TRACE", "SYNAPSE","JYNX", "RAZOR", "SHANK", "NULL", "SWITCH"};
    // this is the secret word we pick from the list
    private String secretWord;
    // this is the word shown to the player with the blanks like _ _ _
    private StringBuilder displayedWord;
    // this is how many guesses u have left
    private int remainingTries;
    // this thingy tells the main game if u won or lost
    private final Consumer<Boolean> gameEndCallback;

    // labels to show stuff on the screen
    private JLabel wordLabel; // shows the word with blanks
    private JLabel triesLabel; // shows how many tries r left
    private JLabel statusLabel; // says if u won or lost
    // this panel holds all the letter buttons
    private JPanel keyboardPanel;

    public HangmanPanel(Consumer<Boolean> callback) {
        this.gameEndCallback = callback;
        initGame(); // start the game stuff
        initUI(); // make the screen look pretty
    }

    /**
     * this starts the game. it pics a secret word and sets up the tries
     */
    private void initGame() {
        // pick a random word from our list
        secretWord = WORDS[(int) (Math.random() * WORDS.length)];
        // make the blank word thingy with underscores
        displayedWord = new StringBuilder("_".repeat(secretWord.length()));
        // u get more tries for longer words so its fair
        remainingTries = secretWord.length();
    }

    /**
     * this sets up all the buttons and text on the screen. the user interface
     */
    private void initUI() {
        // how big the panel is
        setPreferredSize(new Dimension(600, 400));
        // cool hacker colors
        setBackground(Color.BLACK);
        setForeground(Color.GREEN);
        // how to arrange stuff
        setLayout(new BorderLayout(10, 10));
        // some space around the edges
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // this panel is for the stuff at the top. the word and tries
        JPanel topPanel = new JPanel(new GridLayout(3, 1));
        topPanel.setBackground(Color.BLACK); // same black color

        // the label for the secret word
        wordLabel = new JLabel(getSpacedWord(displayedWord.toString()), SwingConstants.CENTER);
        wordLabel.setFont(new Font("Monospaced", Font.BOLD, 36)); // big font
        wordLabel.setForeground(Color.GREEN);
        topPanel.add(wordLabel);

        // the label for how many tries u got left
        triesLabel = new JLabel("Tries Left: " + remainingTries, SwingConstants.CENTER);
        triesLabel.setFont(new Font("Monospaced", Font.PLAIN, 18));
        triesLabel.setForeground(Color.GREEN);
        topPanel.add(triesLabel);
        
        // this label is for winning or losing message
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        statusLabel.setForeground(Color.YELLOW);
        topPanel.add(statusLabel);

        add(topPanel, BorderLayout.NORTH); // put the top panel at the top

        // this is for the keyboard buttons
        keyboardPanel = new JPanel(new GridLayout(3, 9, 5, 5)); // arrange buttons in a grid
        keyboardPanel.setBackground(Color.BLACK);
        String keyboardLayout = "QWERTYUIOPASDFGHJKLZXCVBNM"; // all the letters
        // make a button for each letter
        for (char c : keyboardLayout.toCharArray()) {
            JButton keyButton = new JButton(String.valueOf(c));
            keyButton.setFont(new Font("Monospaced", Font.BOLD, 16));
            keyButton.setBackground(new Color(20, 20, 20));
            keyButton.setForeground(Color.GREEN);
            keyButton.setFocusPainted(false); // no weird box around the letter
            keyButton.setBorder(BorderFactory.createLineBorder(Color.GREEN));
            // what to do when u click a button
            keyButton.addActionListener(e -> handleGuess(keyButton));
            keyboardPanel.add(keyButton);
        }
        add(keyboardPanel, BorderLayout.CENTER); // put the keyboard in the middle
    }

    /**
     * this takes a word and puts spaces between the letters so it looks nice
     * @param word the word to put spaces in
     * @return the word with spaces
     */
    private String getSpacedWord(String word) {
        return String.join(" ", word.split(""));
    }

    /**
     * this happens when a player clicks a letter button. it checks if the letter is good
     * @param button the button that was clicked
     */
    private void handleGuess(JButton button) {
        char guess = button.getText().charAt(0); // get the letter from the button
        button.setEnabled(false); // turn the button off so u cant click it again
        button.setBackground(Color.DARK_GRAY); // make it look off

        // check if the letter is in the secret word
        if (secretWord.indexOf(guess) >= 0) {
            // if it is go through the word and show the letter
            for (int i = 0; i < secretWord.length(); i++) {
                if (secretWord.charAt(i) == guess) {
                    displayedWord.setCharAt(i, guess);
                }
            }
        } else {
            // if not u lose a try
            remainingTries--;
        }

        updateLabels(); // update the screen text
        checkGameState(); // see if the games over
    }

    /**
     * this updates the word and the tries left on the screen
     */
    private void updateLabels() {
        wordLabel.setText(getSpacedWord(displayedWord.toString()));
        triesLabel.setText("Tries Left: " + remainingTries);
    }
    
    /**
     * this makes all the letter buttons not work anymore. like when the game is over
     */
    private void disableKeyboard(){
        // go through all the things on the keyboard panel
        for(Component comp : keyboardPanel.getComponents()){
            // if its a button make it not clickable
            if(comp instanceof JButton){
                comp.setEnabled(false);
            }
        }
    }

    /**
     * this checks if the player won or lost the game
     */
    private void checkGameState() {
        // if the displayed word is the same as the secret word u won
        if (displayedWord.toString().equals(secretWord)) {
            statusLabel.setText("ACCESS GRANTED!");
            statusLabel.setForeground(Color.CYAN);
            disableKeyboard(); // turn off the buttons
            endGame(true); // end the game with a win
        } else if (remainingTries <= 0) {
            // if ur out of tries u lost
            statusLabel.setText("ACCESS DENIED! Word was: " + secretWord);
            statusLabel.setForeground(Color.RED);
            disableKeyboard(); // turn off the buttons
            endGame(false); // end the game with a loss
        }
    }

    /**
     * this ends the game and tells the main window if it was a win or loss
     * @param success true if they won, false if they lost
     */
    private void endGame(boolean success) {
        // wait 2 seconds then close the game
        Timer timer = new Timer(2000, e -> gameEndCallback.accept(success));
        timer.setRepeats(false); // only run one time
        timer.start();
    }
}