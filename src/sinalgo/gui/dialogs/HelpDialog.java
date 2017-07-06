/*
 Copyright (c) 2007, Distributed Computing Group (DCG)
                    ETH Zurich
                    Switzerland
                    dcg.ethz.ch

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the
   distribution.

 - Neither the name 'Sinalgo' nor the names of its contributors may be
   used to endorse or promote products derived from this software
   without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package sinalgo.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.JWindow;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import sinalgo.configuration.AppConfig;
import sinalgo.gui.GuiHelper;

@SuppressWarnings("serial")
public class HelpDialog extends JFrame implements ActionListener, WindowListener {

  JEditorPane html;
  JButton menuButton = new JButton("Menu");
  
  URL currentURL = null;
  URL defaultURL = null;
	
  HelpDialog(JFrame parent) { // is private, use showHelp() to create it in a new thread
		this.setTitle("SINALGO Help  (source: http://dcg.ethz.ch/projects/sinalgo)");
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
			defaultURL = new URL("http://dcg.ethz.ch/projects/sinalgo/tutorial/Documentation.html?menu"); 
			currentURL = defaultURL;
			html.setPage(currentURL);
	    html.setEditable(false); 
	    html.addHyperlinkListener(getLinkListener());
		} catch (MalformedURLException e1) {
			html.setText("Cannot display the page.\n" + e1.getMessage());
		} catch (IOException e1) {
			html.setText("Cannot display the page.\n" + e1.getMessage());
		}	
		
		// Detect ESCAPE button
		KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		focusManager.addKeyEventPostProcessor(new KeyEventPostProcessor() {
			public boolean postProcessKeyEvent(KeyEvent e) {
				if(!e.isConsumed() && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					HelpDialog.this.setVisible(false);
				}
				return false;
			}
		});
		
		this.pack();
		this.setVisible(true);
	}

	
	private HyperlinkListener getLinkListener() {
		return new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) { 
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) { 
					if (e instanceof HTMLFrameHyperlinkEvent) { 
						((HTMLDocument)html.getDocument()).processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent)e);  
					} else { 
						try { 
							currentURL = e.getURL();
							String s = currentURL.toString();
							int offset = s.indexOf(".html");
							if(offset > 0) { // .html is in the string
								s = s.substring(0, offset + 5);
								s += "?help";
								if(currentURL.getRef() != null) {
									s += "#" + currentURL.getRef();
								}
								currentURL = new URL(s);
								HelpDialog.this.setEnabled(true);
								if(menuDlg != null) {
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
			}
		};
	}
	
	MenuDialog menuDlg = null; // The menu dialog if its currently shown, otherwise null

	private void showMenu() {
		Point p = menuButton.getLocationOnScreen();
		menuDlg = new MenuDialog(this, p);
		this.setEnabled(false);
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getSource().equals(menuButton)) {
			showMenu();
		}
	}
	

	class MenuDialog extends JWindow implements ActionListener {
		JFrame parent;

		JButton closeButton = new JButton("Close");
		JButton resetButton = new JButton("Reset");
		JEditorPane ePane;
		String defaultMenuURL = "http://dcg.ethz.ch/projects/sinalgo/index.html?menu";
		
		public MenuDialog(JFrame owner, Point pos) {
			super(owner);
			parent = owner;
			this.setLayout(new BorderLayout());

			ePane = new JEditorPane();
			//ePane.getEditorKit().
			ePane.setPreferredSize(new Dimension(250, 400));
	    ePane.setEditable(false);
	    JScrollPane scroller = new JScrollPane(); 
	    JViewport vp = scroller.getViewport(); 
	    vp.add(ePane); 
	    this.add(scroller, BorderLayout.CENTER); 

			try {
				// create the URL for the menu (ensure that the url still points to a Sinalgo page
				String s = (currentURL == null ? defaultMenuURL : currentURL.toString());
				URL myURL = null;
				int offset = s.indexOf(".html");
				if(offset > 0) { // .html is in the string
					if(s.indexOf("dcg.ethz.ch/projects/sinalgo/") < 0) { // went to a different site
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
			} catch (MalformedURLException e1) {
				ePane.setText("Cannot display the page.\n" + e1.getMessage());
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

		public void actionPerformed(ActionEvent e) {
			if(e.getSource().equals(closeButton)) {
				this.setVisible(false);
				parent.setEnabled(true);
				menuDlg = null;
			}
			if(e.getSource().equals(resetButton)) {
				this.setVisible(false);
				parent.setEnabled(true);
				menuDlg = null;
				try {
					currentURL = defaultURL;
					html.setPage(currentURL);
				} catch (MalformedURLException e1) {
					html.setText("Cannot display the page.\n" + e1.getMessage());
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
		ac.helpWindowIsMaximized = (this.getExtendedState() == JFrame.MAXIMIZED_BOTH);
		ac.writeConfig();
	}
	
	private void restoreWindowState() {
		AppConfig ac = AppConfig.getAppConfig();
		this.setPreferredSize(new Dimension(ac.helpWindowWidth, ac.helpWindowHeight));
		this.setLocation(new Point(ac.helpWindowPosX, ac.helpWindowPosY));
		if(ac.helpWindowIsMaximized) {
			this.setExtendedState(JFrame.MAXIMIZED_BOTH);
		}
	}

	public void windowClosed(WindowEvent e) {
		saveWindowState();
	}

	public void windowActivated(WindowEvent e) {
	}
	public void windowClosing(WindowEvent e) {
		saveWindowState();
	}
	public void windowDeactivated(WindowEvent e) {
		saveWindowState();
	}
	public void windowDeiconified(WindowEvent e) {
	}
	public void windowIconified(WindowEvent e) {
	}
	public void windowOpened(WindowEvent e) {
	}


	public static void showHelp(JFrame parent) {
		Runner r = new Runner(parent);
		r.start();
	}
	
	static class Runner extends Thread  {
		private JFrame p;
		public Runner(JFrame parent) {
			p = parent;
		}
		public void run() {
			new HelpDialog(p);
		}
		
	}
	
}
