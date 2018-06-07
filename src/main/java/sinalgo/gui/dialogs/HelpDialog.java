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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.configuration.AppConfig;
import sinalgo.gui.GuiHelper;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.URL;

@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class HelpDialog extends JFrame implements ActionListener, WindowListener {

    private static final long serialVersionUID = 5648555963120786571L;

    private JEditorPane html;
    private JButton menuButton = new JButton("Menu");

    private URL currentURL;
    private URL defaultURL;

    private HelpDialog(JFrame parent) { // is private, use showHelp() to create it in a new thread
        this.setTitle("Sinalgo Help  (source: https://github.com/Sinalgo/sinalgo)");
        GuiHelper.setWindowIcon(this);
        this.addWindowListener(this);
        this.restoreWindowState();

        this.setLayout(new BorderLayout());
        this.setResizable(true);

        JPanel topPanel = new JPanel();
        this.add(topPanel, BorderLayout.NORTH);
        topPanel.setLayout(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        topPanel.add(this.getMenuButton(), BorderLayout.WEST);
        this.getMenuButton().addActionListener(this);

        this.setHtml(new JEditorPane());
        JScrollPane scroller = new JScrollPane();
        JViewport vp = scroller.getViewport();
        vp.add(this.getHtml());
        this.add(scroller, BorderLayout.CENTER);

        try {
            this.setDefaultURL(new URL("https://sinalgo.github.io"));
            this.setCurrentURL(this.getDefaultURL());
            this.getHtml().setPage(this.getCurrentURL());
            this.getHtml().setEditable(false);
            this.getHtml().addHyperlinkListener(this.getLinkListener());
        } catch (IOException e1) {
            this.getHtml().setText("Cannot display the page.\n" + e1.getMessage());
        }

        // Detect ESCAPE button
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventPostProcessor(e -> {
            if (!e.isConsumed() && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                HelpDialog.this.setVisible(false);
            }
            return false;
        });

        this.pack();
        this.setVisible(true);
    }

    private HyperlinkListener getLinkListener() {
        return e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (e instanceof HTMLFrameHyperlinkEvent) {
                    ((HTMLDocument) this.getHtml().getDocument())
                            .processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) e);
                } else {
                    try {
                        this.setCurrentURL(e.getURL());
                        String s = this.getCurrentURL().toString();
                        int offset = s.indexOf(".html");
                        if (offset > 0) { // .html is in the string
                            s = s.substring(0, offset + 5);
                            s += "?help";
                            if (this.getCurrentURL().getRef() != null) {
                                s += "#" + this.getCurrentURL().getRef();
                            }
                            this.setCurrentURL(new URL(s));
                            HelpDialog.this.setEnabled(true);
                            if (this.getMenuDlg() != null) {
                                this.getMenuDlg().setVisible(false);
                                this.setMenuDlg(null);
                            }
                        }
                        this.getHtml().setPage(this.getCurrentURL());
                    } catch (IOException e1) {
                        this.getHtml().setText("Cannot display the page.\n" + e1.getMessage());
                    }
                }
            }
        };
    }

    private MenuDialog menuDlg; // The menu dialog if its currently shown, otherwise null

    private void showMenu() {
        Point p = this.getMenuButton().getLocationOnScreen();
        this.setMenuDlg(new MenuDialog(this, p));
        this.setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(this.getMenuButton())) {
            this.showMenu();
        }
    }

    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    class MenuDialog extends JWindow implements ActionListener {

        private static final long serialVersionUID = -950395591867596455L;

        private JFrame parentFrame;

        private JButton closeButton = new JButton("Close");
        private JButton resetButton = new JButton("Reset");
        private JEditorPane ePane;
        private String defaultMenuURL = "https://github.com/Sinalgo/sinalgo/";

        MenuDialog(JFrame owner, Point pos) {
            super(owner);
            this.setParentFrame(owner);
            this.setLayout(new BorderLayout());

            this.setEPane(new JEditorPane());
            // ePane.getEditorKit().
            this.getEPane().setPreferredSize(new Dimension(250, 400));
            this.getEPane().setEditable(false);
            JScrollPane scroller = new JScrollPane();
            JViewport vp = scroller.getViewport();
            vp.add(this.getEPane());
            this.add(scroller, BorderLayout.CENTER);

            try {
                // create the URL for the menu (ensure that the url still points to a Sinalgo
                // page
                String s = (HelpDialog.this.getCurrentURL() == null ? this.getDefaultMenuURL() : HelpDialog.this.getCurrentURL().toString());
                URL myURL;
                int offset = s.indexOf(".html");
                if (offset > 0) { // .html is in the string
                    if (!s.contains("github.com/andrebrait/sinalgo/")) { // went to a different site
                        myURL = new URL(this.getDefaultMenuURL());
                    } else { // add the ?menu option
                        s = s.substring(0, offset + 5);
                        s += "?menu";
                        myURL = new URL(s);
                    }
                } else {
                    myURL = new URL(this.getDefaultMenuURL());
                }
                this.getEPane().setPage(myURL); // load the page
                this.getEPane().addHyperlinkListener(HelpDialog.this.getLinkListener());
            } catch (IOException e1) {
                this.getEPane().setText("Cannot display the page.\n" + e1.getMessage());
            }

            JPanel menuPanel = new JPanel();
            menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.X_AXIS));
            this.add(menuPanel, BorderLayout.NORTH);
            this.getCloseButton().addActionListener(this);
            menuPanel.add(this.getCloseButton());
            this.getResetButton().addActionListener(this);
            menuPanel.add(this.getResetButton());

            this.setLocation(pos);
            this.pack();
            this.setVisible(true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(this.getCloseButton())) {
                this.setVisible(false);
                this.getParentFrame().setEnabled(true);
                HelpDialog.this.setMenuDlg(null);
            }
            if (e.getSource().equals(this.getResetButton())) {
                this.setVisible(false);
                this.getParentFrame().setEnabled(true);
                HelpDialog.this.setMenuDlg(null);
                try {
                    HelpDialog.this.setCurrentURL(HelpDialog.this.getDefaultURL());
                    HelpDialog.this.getHtml().setPage(HelpDialog.this.getCurrentURL());
                } catch (IOException e1) {
                    HelpDialog.this.getHtml().setText("Cannot display the page.\n" + e1.getMessage());
                }
            }
        }

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

        this.setPreferredSize(new Dimension(AppConfig.getAppConfig().getHelpWindowWidth(), AppConfig.getAppConfig().getHelpWindowHeight()));
        this.setLocation(new Point(AppConfig.getAppConfig().getHelpWindowPosX(), AppConfig.getAppConfig().getHelpWindowPosY()));
        if (AppConfig.getAppConfig().isHelpWindowIsMaximized()) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
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
        Runner r = new Runner(parent);
        r.start();
    }

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    static class Runner extends Thread {

        private JFrame p;

        Runner(JFrame parent) {
            this.setP(parent);
        }

        @Override
        public void run() {
            new HelpDialog(this.getP());
        }

    }

}
