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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration;
import sinalgo.gui.GuiHelper;
import sinalgo.io.versionTest.VersionTester;


/**
 * Dialog that shows the global settings of the current simulation. 
 */
@SuppressWarnings("serial")
public class GlobalSettingsDialog extends JDialog implements ActionListener {

	private JButton close = new JButton("Close");
	private JButton versionTest = new JButton("Test now");
	private JCheckBox testForUpdatesAtStartup = new JCheckBox("Test for a more recent version of Sinalgo at startup (once per day)");
	
	/**
	 * The constructor for the GlobalSettingsDialog class.
	 *
	 * @param parent The parent Frame to attach the dialog to.
	 */
	public GlobalSettingsDialog(JFrame parent){
		super(parent, "Global Settings", true);
		GuiHelper.setWindowIcon(this);
		
		this.setLayout(new BorderLayout());
		this.setPreferredSize(new Dimension(650, 500));
		
		JTextArea text = new JTextArea();
		text.setEditable(false);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Configuration.printConfiguration(new PrintStream(os));
		text.setText(os.toString());
		text.setCaretPosition(0); // ensure that the top of the text is shown 
		JScrollPane spane = new JScrollPane(text);
		this.add(spane);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BorderLayout());
		
		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout(new BorderLayout());

		testForUpdatesAtStartup.setSelected(AppConfig.getAppConfig().checkForSinalgoUpdate);
		testForUpdatesAtStartup.addActionListener(this);
		settingsPanel.add(BorderLayout.LINE_START, testForUpdatesAtStartup);

		versionTest.addActionListener(this);
		settingsPanel.add(BorderLayout.EAST, versionTest);

		buttonPanel.add(settingsPanel);

		close.addActionListener(this);
		JPanel closePanel = new JPanel();
		closePanel.add(close);
		buttonPanel.add(BorderLayout.SOUTH, closePanel);
		
		this.add(BorderLayout.SOUTH, buttonPanel);
	
		// Detect ESCAPE button
		KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		focusManager.addKeyEventPostProcessor(new KeyEventPostProcessor() {
			public boolean postProcessKeyEvent(KeyEvent e) {
				if(!e.isConsumed() && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					GlobalSettingsDialog.this.setVisible(false);
				}
				return false;
			}
		});

		this.getRootPane().setDefaultButton(close);
		
		this.pack();
		this.setLocationRelativeTo(parent);
		this.setVisible(true);
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event) {	
		if(event.getSource().equals(close)) {
			this.setVisible(false);
		} else if(event.getSource().equals(versionTest)) {
			VersionTester.testVersion(false, true);
		} else if(event.getSource().equals(testForUpdatesAtStartup)) {
			AppConfig.getAppConfig().checkForSinalgoUpdate = testForUpdatesAtStartup.isSelected();
			AppConfig.getAppConfig().writeConfig();
		}
	}
}

