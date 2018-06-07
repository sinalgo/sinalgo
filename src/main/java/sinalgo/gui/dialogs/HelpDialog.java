/*
BSD 3-Clause License

Copyright (c) 2007-2013, Distributed Computing Group (DCG)
                         ETH Zurich
                         Switzerland
                         dcg.ethz.ch
              2017-2018, André Brait

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the copyright holder nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package sinalgo.gui.dialogs;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class HelpDialog extends JFrame implements ActionListener, WindowListener {

    private static final long serialVersionUID = 5648555963120786571L;

    private JFXPanel fxPanel;

    private HelpDialog(JFrame parent) { // is private, use showHelp() to create it in a new thread
        super("Sinalgo Help  (source: " + Configuration.SINALGO_WEB_PAGE + ")");
        fxPanel = new JFXPanel();
        Platform.runLater(() -> {
            WebView wv = new WebView();
            wv.getEngine().load(Configuration.SINALGO_WEB_PAGE);
            wv.setContextMenuEnabled(false);
            getFxPanel().setScene(new Scene(wv, 500, 500));
            HelpDialog.this.setMinimumSize(new Dimension(500, 500));
            HelpDialog.this.setIconImage(parent.getIconImage());
            HelpDialog.this.add(new JScrollPane(fxPanel));
            HelpDialog.this.addWindowListener(this);
            HelpDialog.this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            HelpDialog.this.restoreWindowState();
            wv.setOnKeyPressed(e -> {
                if (!e.isConsumed()) {
                    if (e.getCode() == KeyCode.LEFT && e.isAltDown()) {
                        WebHistory h = wv.getEngine().getHistory();
                        if (h.getCurrentIndex() > 0) {
                            h.go(-1);
                        }
                    } else if (e.getCode() == KeyCode.RIGHT && e.isAltDown()) {
                        WebHistory h = wv.getEngine().getHistory();
                        if (h.getCurrentIndex() < h.getEntries().size() - 1) {
                            h.go(1);
                        }
                    }
                }
            });
            HelpDialog.this.setVisible(true);
            HelpDialog.this.pack();
        });

        // Detect ESCAPE button
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventPostProcessor(e -> {
            if (!e.isConsumed() && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                HelpDialog.this.dispose();
            }
            return false;
        });

    }

    private void saveWindowState() {
        AppConfig.getAppConfig().setHelpWindowHeight(this.getHeight());
        AppConfig.getAppConfig().setHelpWindowWidth(this.getWidth());
        AppConfig.getAppConfig().setHelpWindowPosX(this.getLocation().x);
        AppConfig.getAppConfig().setHelpWindowPosY(this.getLocation().y);
        AppConfig.getAppConfig().setHelpWindowIsMaximized((this.getExtendedState() == Frame.MAXIMIZED_BOTH));
        AppConfig.getAppConfig().writeConfig();
    }

    private void restoreWindowState() {
        this.setPreferredSize(new Dimension(AppConfig.getAppConfig().getHelpWindowWidth(),
                AppConfig.getAppConfig().getHelpWindowHeight()));
        this.setLocation(new Point(AppConfig.getAppConfig().getHelpWindowPosX(),
                AppConfig.getAppConfig().getHelpWindowPosY()));
        if (AppConfig.getAppConfig().isHelpWindowIsMaximized()) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void windowClosed(WindowEvent e) {
        this.saveWindowState();
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        this.saveWindowState();
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        this.saveWindowState();
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    public static void showHelp(JFrame parent) {
        new HelpDialog(parent);
    }

}
