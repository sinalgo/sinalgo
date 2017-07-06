package sinalgo.gui;

import java.awt.Window;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import sinalgo.configuration.Configuration;

public class GuiHelper {

	public static ImageIcon getIcon(String fileName) {
		ClassLoader cldr = ClassLoader.getSystemClassLoader();
		URL url = cldr.getResource(Configuration.imageDir + fileName);
		if(url != null) {
			ImageIcon ii = new ImageIcon(url);
			return ii;
		}
		return null; // not found
	}
	
	/**
	 * Set the application Icon to a given frame
	 * @param frame
	 */
	public static void setWindowIcon(JFrame frame) {
		// set the icon
		ClassLoader cldr = frame.getClass().getClassLoader();
		URL url = cldr.getResource(Configuration.imageDir + "appIcon.gif");
		if(url != null) {
			ImageIcon ii = new ImageIcon(url);
			frame.setIconImage(ii.getImage());
		}
	}

	/**
	 * Sets the icon image of Sinalgo (currently the DCG logo) to a given window 
	 * @param w The window to add the icon to
	 */
	public static void setWindowIcon(Window w) {
		ClassLoader cldr = w.getClass().getClassLoader();
		URL url = cldr.getResource(Configuration.imageDir + "appIcon.gif");
		if(url != null) {
			ImageIcon ii = new ImageIcon(url);
			if(w instanceof JFrame) { // TODO: only Java >= 6.0 supports Window.setIconImage() 
				((JFrame)w).setIconImage(ii.getImage());
			}
		}
	}
	
}
