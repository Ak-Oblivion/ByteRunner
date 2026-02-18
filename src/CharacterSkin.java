import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

public class CharacterSkin {
	// skin id name and price
	final String id;
	final String name;
	final int price;
	// is skin unlocked
	boolean isUnlocked;
	// path to skin image file
	final String imagePath;
	// holds the loaded skin image
	private Image skinPreviewImage;

	public CharacterSkin(String id, String name, int price, String imagePath, boolean isUnlocked) {
		this.id = id;
		this.name = name;
		this.price = price;
		this.imagePath = imagePath;
		this.isUnlocked = isUnlocked;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getPrice() {
		return price;
	}

	public String getImagePath() {
		return imagePath;
	}

	public boolean isUnlocked() {
		return isUnlocked;
	}

	public Color getColor() {
		return Color.WHITE;
	}

	/**
	 * loads the skin image when needed
	 * tries to load from inside the game files
	 * if that fails it tries to load from the computer
	 */
	public Image getPreviewImage() {
		// if image is not loaded yet
		if (skinPreviewImage == null) {
			try {
				// try loading from game resources
				skinPreviewImage = ImageIO.read(getClass().getResource(imagePath));
			} catch (IOException | IllegalArgumentException e) {
				// if that fails try loading from a normal file path
				try {
					skinPreviewImage = ImageIO.read(new File(imagePath));
				} catch (IOException ex) {
					// if it still fails do nothing
				}
			}
		}
		// return the loaded image
		return skinPreviewImage;
	}

	public void setUnlocked(boolean unlocked) {
		this.isUnlocked = unlocked;
	}
}