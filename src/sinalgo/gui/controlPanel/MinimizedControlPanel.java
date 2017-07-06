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
package sinalgo.gui.controlPanel;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import sinalgo.configuration.Configuration;
import sinalgo.gui.GUI;
import sinalgo.gui.transformation.Transformation3D;
import sinalgo.runtime.Global;
import sinalgo.runtime.events.Event;

/**
 * The minimized version of the control panel.
 */
@SuppressWarnings("serial")
public class MinimizedControlPanel extends ControlPanel {
	JPanel buttonPanel = null;
	
	/**
	 * Creates a MinimizedControlPanel for the specified GUI instance.
	 *
	 * @param p The Gui instance to create the MinimizedControlPanel for.
	 */
	public MinimizedControlPanel(GUI p){
		parent = p;
		int controlPanelHeight= 25;
		this.setMaximumSize(new Dimension(20000, controlPanelHeight));
		this.setMinimumSize(new Dimension(20000, controlPanelHeight));
		this.setLayout(new BorderLayout());
		this.setBorder(BorderFactory.createRaisedBevelBorder());
		
		buttonPanel = new JPanel();
		FlowLayout buttonLayout = new FlowLayout(FlowLayout.LEFT, 2, 0);
		buttonPanel.setLayout(buttonLayout);

		JButton button = createFrameworkIconButton("clearGraph", "cleargraph.gif", "Clear Graph");
		buttonPanel.add(button);
		addToDisabledButtonList(button);
		
		button = createFrameworkIconButton("addNodes", "addnodes.gif", "Add Nodes");
		buttonPanel.add(button);
		addToDisabledButtonList(button);

		button = createFrameworkIconButton("connectNodes", "connectnodes.gif", "Reevaluate Connections");
		buttonPanel.add(button);
		addToDisabledButtonList(button);

		
		addSeparator(buttonPanel);

		button = createFrameworkIconButton("zoomIn", "zoominimage.png", "Zoom In");
		buttonPanel.add(button);
		addToDisabledButtonList(button);

		button = createFrameworkIconButton("zoomOut", "zoomoutimage.png", "Zoom Out");
		buttonPanel.add(button);
		addToDisabledButtonList(button);

		button = createFrameworkIconButton("zoomToFit", "zoomtofit.gif", "Zoom To Fit");
		buttonPanel.add(button);
		addToDisabledButtonList(button);
		
		if(parent.getTransformator() instanceof Transformation3D) {
			button = createFrameworkIconButton("zoomToFit3D", "zoomtofit3d.gif", "Default View");
			buttonPanel.add(button);
			addToDisabledButtonList(button);
		}
		
		addSeparator(buttonPanel);
		addSpacer(buttonPanel, 5);
		
		// The two text fields to enter number of rounds and refresh rate
		//roundNumber.setText(String.valueOf(Configuration.defaultRoundNumber));
		if(Configuration.asynchronousMode){
			roundsToPerform.setToolTipText("Number of Events to perform");
		}
		else{
			roundsToPerform.setToolTipText("Number of Rounds to perform");
		}
		buttonPanel.add(roundsToPerform);
		
		refreshRate.setText(String.valueOf(Configuration.refreshRate));
		refreshRate.setToolTipText("Refresh Rate");
		buttonPanel.add(refreshRate);

		JPanel startButtonPanel = new JPanel();
		startButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		startButtonPanel.add(start);
			
		// the run-selection button
		runMenuButton = createFrameworkIconButton("RunMenu", "maximize.gif", "Run Options");
		runMenuButton.setPreferredSize(new Dimension(13, 29));
		addToDisabledButtonList(runMenuButton);
		startButtonPanel.add(runMenuButton);
		// raise the 'run' menu whenever the mouse idles over this button
		runMenuButton.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {
				if(runMenuButton.isEnabled()) {
					start.setBorderPainted(true);
				}
			}
			public void mouseExited(MouseEvent e) {
				if(runMenuButton.isEnabled()) {
					start.setBorderPainted(false);
				}
			}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
		});
		buttonPanel.add(startButtonPanel);
		
		
		abort = createFrameworkIconButton("Abort", "abort.gif", "Abort Simulation");
		abort.setEnabled(false);
		buttonPanel.add(abort);
		
		addSpacer(buttonPanel, 5);
		
		JLabel doneRoundsLabel;
		if(Global.isAsynchronousMode){
			doneRoundsLabel = new JLabel("Time: ");
			roundsPerformed.setText(String.valueOf(round(sinalgo.runtime.Global.currentTime, 2)));
		}
		else{
			doneRoundsLabel = new JLabel("Round: ");
			roundsPerformed.setText(String.valueOf((int)round(sinalgo.runtime.Global.currentTime, 2)));
		}
		buttonPanel.add(doneRoundsLabel);
		roundsPerformed.setEditable(false);
		roundsPerformed.setBorder(BorderFactory.createEmptyBorder());
		roundsPerformed.setToolTipText("Number of rounds performed so far");
		buttonPanel.add(roundsPerformed);

		// Add the user-defined buttons
		Vector<JButton> customButtons = super.createCustomButtons();
		if(customButtons.size() > 0) {
			addSpacer(buttonPanel, 5);
			addSeparator(buttonPanel);
			addSpacer(buttonPanel, 5);

			for(JButton b : customButtons) {
				buttonPanel.add(b);
				addToDisabledButtonList(b);
			}
			addSpacer(buttonPanel, 4); // strange, but the last button is sometimes not painted...
		}
		
		JButton changeButton = createFrameworkIconButton("extendPanel", "maximize.gif", "Extend");
		changeButton.setPreferredSize(new Dimension(13, 29));
		addToDisabledButtonList(changeButton);
		this.add(BorderLayout.EAST, changeButton);
		this.add(BorderLayout.WEST, buttonPanel);

		this.setVisible(true);
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.gui.controlPanel.ControlPanel#setRoundsPerformed(double)
	 */
	public void setRoundsPerformed(double time, int eventNumber){
		roundsPerformed.setText(String.valueOf(round(time, 2)));
		buttonPanel.doLayout();
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.gui.controlPanel.ControlPanel#setRoundsPerformed(double)
	 */
	public void setRoundsPerformed(int i){
		roundsPerformed.setText(String.valueOf(i));
		buttonPanel.doLayout();
	}
	
	public void setCurrentEvent(Event e){
		setStringsForCurrentEvent(e);
		Vector<String> v = new Vector<String>(1);
		v.add(currentEventString);
		eventJList.setListData(v);
	}
	
	private void addSeparator(JPanel buttonPanel) {
		JPanel separator = new JPanel();
		separator.setPreferredSize(new Dimension(1, 23));
		separator.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		buttonPanel.add(separator);
	}
	
	private void addSpacer(JPanel buttonPanel, int width) {
		JPanel spacer = new JPanel();
		spacer.setPreferredSize(new Dimension(width, 20));
		buttonPanel.add(spacer);
	}
	
}
