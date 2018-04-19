package sinalgo.gui;

import com.apple.eawt.Application;
import sinalgo.configuration.Configuration;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class GuiHelper {

    public static ImageIcon getIcon(String fileName) {
        ClassLoader cldr = ClassLoader.getSystemClassLoader();
        URL url = cldr.getResource(Configuration.sinalgoImageDir + "/" + fileName);
        if (url != null) {
            return new ImageIcon(url);
        }
        return null; // not found
    }

    /**
     * Set the application Icon to a given frame
     *
     * @param frame The frame
     */
    public static void setWindowIcon(JFrame frame) {
        // set the icon
        ClassLoader cldr = frame.getClass().getClassLoader();
        URL url = cldr.getResource(Configuration.sinalgoImageDir + "/" + "sinalgo.png");
        if (url != null) {
            ImageIcon ii = new ImageIcon(url);
            Application.getApplication().setDockIconImage(ii.getImage());
            frame.setIconImage(ii.getImage());
        }
    }

    /**
     * Sets the icon image of Sinalgo to a given window
     *
     * @param w The window to add the icon to
     */
    public static void setWindowIcon(Window w) {
        ClassLoader cldr = w.getClass().getClassLoader();
        URL url = cldr.getResource(Configuration.sinalgoImageDir + "/" + "sinalgo.png");
        if (url != null) {
            ImageIcon ii = new ImageIcon(url);
            if (w instanceof JFrame) {
                Application.getApplication().setDockIconImage(ii.getImage());
                w.setIconImage(ii.getImage());
            }
        }
    }

}
