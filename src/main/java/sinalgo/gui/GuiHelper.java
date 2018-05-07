package sinalgo.gui;

import com.apple.eawt.Application;
import sinalgo.configuration.Configuration;
import sinalgo.io.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class GuiHelper {

    public static ImageIcon getIcon(String fileName) {
        ClassLoader cldr = Thread.currentThread().getContextClassLoader();
        URL url = cldr.getResource(IOUtils.getAsPath(Configuration.getSinalgoImageDir(), fileName));
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
        ClassLoader cldr = Thread.currentThread().getContextClassLoader();
        URL url = cldr.getResource(IOUtils.getAsPath(Configuration.getSinalgoImageDir(), "sinalgo.png"));
        if (url != null) {
            ImageIcon ii = new ImageIcon(url);
            try {
                Application.getApplication().setDockIconImage(ii.getImage());
            } catch (RuntimeException ignore) {

            }
            frame.setIconImage(ii.getImage());
        }
    }

    /**
     * Sets the icon image of Sinalgo to a given window
     *
     * @param w The window to add the icon to
     */
    public static void setWindowIcon(Window w) {
        ClassLoader cldr = Thread.currentThread().getContextClassLoader();
        URL url = cldr.getResource(IOUtils.getAsPath(Configuration.getSinalgoImageDir(), "sinalgo.png"));
        if (url != null) {
            ImageIcon ii = new ImageIcon(url);
            if (w instanceof JFrame) {
                try {
                    Application.getApplication().setDockIconImage(ii.getImage());
                } catch (RuntimeException ignore) {

                }
                w.setIconImage(ii.getImage());
            }
        }
    }

}
