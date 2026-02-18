import javax.swing.*;
import java.awt.*;

public class ByteRunnerGame {
	public static void main(String[] args) {
		// create the game window
		JFrame window = new JFrame("ByteRunner");
		// stop player from resizing window
		window.setResizable(false);
		// make program close when window closes
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// create the main game panel
		GamePanel panel = new GamePanel();
		
		// put the panel inside the window
		window.setContentPane(panel);
		// make window fit the panel size
		window.pack();
		// open window in center of screen
		window.setLocationRelativeTo(null);
		// show the window
		window.setVisible(true);

		// start the game
		panel.startGameThread();
	}
}