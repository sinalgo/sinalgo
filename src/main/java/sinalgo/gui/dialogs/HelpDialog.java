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

import sinalgo.configuration.AppConfig;
import sinalgo.gui.GuiHelper;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URL;

public class HelpDialog extends JFrame implements ActionListener, WindowListener {

    private static final long serialVersionUID = 5648555963120786571L;

    private JEditorPane html;
    private JButton menuButton = new JButton("Menu");

    private URL currentURL = null;
    private URL defaultURL = null;

    private HelpDialog(JFrame parent) { // is private, use showHelp() to create it in a new thread
        this.setTitle("Sinalgo Help  (source: https://github.com/andrebrait/sinalgo)");
        GuiHelper.setWindowIcon(this);
        this.addWindowListener(this);
        restoreWindowState();

        this.setLayout(new BorderLayout());
        this.setResizable(true);

        JPanel topPanel = new JPanel();
        this.add(topPanel, BorderLayout.NORTH);
        topPanel.setLayout(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        topPanel.add(menuButton, BorderLayout.WEST);
        menuButton.addActionListener(this);

        html = new JEditorPane();
        JScrollPane scroller = new JScrollPane();
        JViewport vp = scroller.getViewport();
        vp.add(html);
        this.add(scroller, BorderLayout.CENTER);

        try {
            defaultURL = new URL("https://github.com/andrebrait/sinalgo/raw/master/MANUAL.pdf");
            currentURL = defaultURL;
            html.setPage(currentURL);
            html.setEditable(false);
            html.addHyperlinkListener(getLinkListener());
        } catch (IOException e1) {
            html.setText("Cannot display the page.\n" + e1.getMessage());
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
                    ((HTMLDocument) html.getDocument()).processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) e);
                } else {
                    try {
                        currentURL = e.getURL();
                        String s = currentURL.toString();
                        int offset = s.indexOf(".html");
                        if (offset > 0) { // .html is in the string
                            s = s.substring(0, offset + 5);
                            s += "?help";
                            if (currentURL.getRef() != null) {
                                s += "#" + currentURL.getRef();
                            }
                            currentURL = new URL(s);
                            HelpDialog.this.setEnabled(true);
                            if (menuDlg != null) {
                                menuDlg.setVisible(false);
                                menuDlg = null;
                            }
                        }
                        html.setPage(currentURL);
                    } catch (IOException e1) {
                        html.setText("Cannot display the page.\n" + e1.getMessage());
                    }
                }
            }
        };
    }

    private MenuDialog menuDlg = null; // The menu dialog if its currently shown, otherwise null

    private void showMenu() {
        Point p = menuButton.getLocationOnScreen();
        menuDlg = new MenuDialog(this, p);
        this.setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(menuButton)) {
            showMenu();
        }
    }

    class MenuDialog extends JWindow implements ActionListener {

        private static final long serialVersionUID = -950395591867596455L;

        JFrame parent;

        JButton closeButton = new JButton("Close");
        JButton resetButton = new JButton("Reset");
        JEditorPane ePane;
        String defaultMenuURL = "https://github.com/andrebrait/sinalgo/";

        MenuDialog(JFrame owner, Point pos) {
            super(owner);
            parent = owner;
            this.setLayout(new BorderLayout());

            ePane = new JEditorPane();
            // ePane.getEditorKit().
            ePane.setPreferredSize(new Dimension(250, 400));
            ePane.setEditable(false);
            JScrollPane scroller = new JScrollPane();
            JViewport vp = scroller.getViewport();
            vp.add(ePane);
            this.add(scroller, BorderLayout.CENTER);

            try {
                // create the URL for the menu (ensure that the url still points to a Sinalgo
                // page
                String s = (currentURL == null ? defaultMenuURL : currentURL.toString());
                URL myURL;
                int offset = s.indexOf(".html");
                if (offset > 0) { // .html is in the string
                    if (!s.contains("github.com/andrebrait/sinalgo/")) { // went to a different site
                        myURL = new URL(defaultMenuURL);
                    } else { // add the ?menu option
                        s = s.substring(0, offset + 5);
                        s += "?menu";
                        myURL = new URL(s);
                    }
                } else {
                    myURL = new URL(defaultMenuURL);
                }
                ePane.setPage(myURL); // load the page
                ePane.addHyperlinkListener(getLinkListener());
            } catch (IOException e1) {
                ePane.setText("Cannot display the page.\n" + e1.getMessage());
            }

            JPanel menuPanel = new JPanel();
            menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.X_AXIS));
            this.add(menuPanel, BorderLayout.NORTH);
            closeButton.addActionListener(this);
            menuPanel.add(closeButton);
            resetButton.addActionListener(this);
            menuPanel.add(resetButton);

            this.setLocation(pos);
            this.pack();
            this.setVisible(true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(closeButton)) {
                this.setVisible(false);
                parent.setEnabled(true);
                menuDlg = null;
            }
            if (e.getSource().equals(resetButton)) {
                this.setVisible(false);
                parent.setEnabled(true);
                menuDlg = null;
                try {
                    currentURL = defaultURL;
                    html.setPage(currentURL);
                } catch (IOException e1) {
                    html.setText("Cannot display the page.\n" + e1.getMessage());
                }
            }
        }

    }

    private void saveWindowState() {
        AppConfig ac = AppConfig.getAppConfig();
        ac.helpWindowHeight = this.getHeight();
        ac.helpWindowWidth = this.getWidth();
        ac.helpWindowPosX = this.getLocation().x;
        ac.helpWindowPosY = this.getLocation().y;
        ac.helpWindowIsMaximized = (this.getExtendedState() == Frame.MAXIMIZED_BOTH);
        ac.writeConfig();
    }

    private void restoreWindowState() {
        AppConfig ac = AppConfig.getAppConfig();
        this.setPreferredSize(new Dimension(ac.helpWindowWidth, ac.helpWindowHeight));
        this.setLocation(new Point(ac.helpWindowPosX, ac.helpWindowPosY));
        if (ac.helpWindowIsMaximized) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {
        saveWindowState();
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        saveWindowState();
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        saveWindowState();
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

    static class Runner extends Thread {

        private JFrame p;

        Runner(JFrame parent) {
            p = parent;
        }

        @Override
        public void run() {
            new HelpDialog(p);
        }

    }

}
