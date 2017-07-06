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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolTip;
import javax.swing.Scrollable;

import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration;
import sinalgo.gui.GUI;
import sinalgo.gui.helper.MultiLineFlowLayout;
import sinalgo.gui.helper.NonRegularGridLayout;
import sinalgo.gui.multiLineTooltip.MultiLineToolTip;
import sinalgo.gui.multiLineTooltip.MultilineToolTipJList;
import sinalgo.gui.popups.EventPopupMenu;
import sinalgo.gui.transformation.Transformation3D;
import sinalgo.runtime.Global;
import sinalgo.runtime.Runtime;
import sinalgo.runtime.events.Event;
import sinalgo.runtime.events.EventQueue;
import sinalgo.runtime.events.EventQueueListener;

/**
 * The maximized version of the control panel.
 */
@SuppressWarnings("serial")
public class MaximizedControlPanel extends ControlPanel implements EventQueueListener{
	
	private AppConfig appConfig = AppConfig.getAppConfig();
	
	private EventQueueElement[] queueElements = new EventQueueElement[Configuration.shownEventQueueSize];
	
	private int controlPanelWidth = 200;
	
	private EventQueueList eventList;
	private JScrollPane scrollableEventList;
	JLayeredPane viewContent; // view panel, with button
	JLayeredPane textContent; // text panel, with button
	JLayeredPane projectControlContent; // project specific buttons, max/minimizable
	JLayeredPane simulationPane; // the simulation panel, with button
	
	private JPanel events = new JPanel();
	
	private int fixedCellHeight = 12;
	private int fixedCellWidth = 180;
	
	private NonColoringNonBorderingCellRenderer nCNBCR = new NonColoringNonBorderingCellRenderer();
	private DefaultListCellRenderer dLCR = new DefaultListCellRenderer();
	
	private class MyScrollPane extends JPanel implements Scrollable {
		
		/* (non-Javadoc)
		 * @see javax.swing.Scrollable#getPreferredScrollableViewportSize()
		 */
		public Dimension getPreferredScrollableViewportSize() {
			return new Dimension(controlPanelWidth, parent.getHeight() - 60); // hand-crafted :(
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.Scrollable#getScrollableBlockIncrement(java.awt.Rectangle, int, int)
		 */
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 0;
		}

		/* (non-Javadoc)
		 * @see javax.swing.Scrollable#getScrollableTracksViewportHeight()
		 */
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}

		/* (non-Javadoc)
		 * @see javax.swing.Scrollable#getScrollableTracksViewportWidth()
		 */
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.Scrollable#getScrollableUnitIncrement(java.awt.Rectangle, int, int)
		 */
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 0;
		}
		
		public MyScrollPane() {
			this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); //new BorderLayout());
			this.setBorder(BorderFactory.createEmptyBorder(-8, 0,0,0));

			// The button to change to the minimized view
			// ------------------------------------------------------------------------
			JPanel mPanel = new JPanel();
			JButton minimize = createFrameworkIconButton("minimizedPanel", "minimize.gif", "Minimize");
			minimize.setPreferredSize(new Dimension(controlPanelWidth, 11));
			addToDisabledButtonList(minimize);
			mPanel.add(minimize);
			this.add(mPanel);

			// Simulation Control
			// ------------------------------------------------------------------------
			simulationPane = new JLayeredPane();
			createSimulationPanel();
			this.add(simulationPane);
			
			// Customized Buttons
			// ------------------------------------------------------------------------
			projectControlContent = new JLayeredPane(); // a layered panel for the minimize button
			createProjectControlPanel();
			this.add(projectControlContent);
			
			// VIEW Panel
			// ------------------------------------------------------------------------
			viewContent = new JLayeredPane(); // a layered panel for the minimize button
			createViewPanel();
			this.add(viewContent);

			// TEXT Panel
   	  //------------------------------------------------------------------------
			textContent = new JLayeredPane();
			createTextPanel();
			this.add(textContent);
		}
	} // end of class MyScrollPane
	
	
	/**
	 * Creates the content of the text panel
	 */
	private void createTextPanel() {
		textContent.removeAll();
		
		JButton textPanelMinimizeButton;
		if(appConfig.guiControlPanelShowTextPanel) {
			textPanelMinimizeButton = createFrameworkIconButton("minimizeText", "minimize.gif", "Minimize");
		} else {
			textPanelMinimizeButton = createFrameworkIconButton("maximizeText", "maximize.gif", "Maximize");
		}
		textPanelMinimizeButton.setPreferredSize(new Dimension(21, 11));
		textContent.add(textPanelMinimizeButton, JLayeredPane.PALETTE_LAYER);
		textPanelMinimizeButton.setBounds(controlPanelWidth - 26, 3, 21, 11);
		addToDisabledButtonList(textPanelMinimizeButton); // disable while simulating

		JPanel textPanel = new JPanel();						
		textPanel.setBorder(BorderFactory.createTitledBorder("Output"));
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		textContent.add(textPanel, JLayeredPane.DEFAULT_LAYER);

		if(appConfig.guiControlPanelShowTextPanel) {
			JScrollPane sp = new JScrollPane(textField, 
			                                 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			                                 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			sp.setPreferredSize(new Dimension(controlPanelWidth, Configuration.outputTextFieldHeight));
			textField.setEditable(false);
			textField.setLineWrap(true);
			textPanel.add(sp);
			JButton clearText = super.createTextButton("ClearOutputText", "Clear", "Remove all output");
			clearText.setPreferredSize(new Dimension(60, 12));
			clearText.setFont(new Font("", 0, 11));
			addToDisabledButtonList(clearText); // disable while simulating
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.add(clearText, BorderLayout.EAST);
			textPanel.add(p);
		}
		
		// Finally set the size of the textPanel
		Dimension dim = textPanel.getPreferredSize();
		textPanel.setBounds(0, 0, controlPanelWidth, dim.height);
		textContent.setPreferredSize(dim);
	}
	
	private void createSimulationPanel() {
		simulationPane.removeAll(); // restart from scratch
		boolean isMax = appConfig.guiControlPanelExpandSimulation; 	

		if(Global.isAsynchronousMode) { // the minimization button is only needed in async mode.
			JButton simulationPanelMinimizeButton;
			if(isMax) {
				simulationPanelMinimizeButton = createFrameworkIconButton("minimizeSimControl", "minimize.gif", "Minimize");
			} else {
				simulationPanelMinimizeButton = createFrameworkIconButton("maximizeSimControl", "maximize.gif", "Maximize");
			}
			simulationPanelMinimizeButton.setPreferredSize(new Dimension(21, 11));
			simulationPane.add(simulationPanelMinimizeButton, JLayeredPane.PALETTE_LAYER);
			simulationPanelMinimizeButton.setBounds(controlPanelWidth - 26, 3, 21, 11);
			addToDisabledButtonList(simulationPanelMinimizeButton); // disable while simulating
		}
		
		JPanel roundControl = new JPanel();						
		roundControl.setBorder(BorderFactory.createTitledBorder("Simulation Control"));
		roundControl.setLayout(new BoxLayout(roundControl, BoxLayout.Y_AXIS));
		simulationPane.add(roundControl, JLayeredPane.DEFAULT_LAYER);
		
		info = new JPanel();

		Font labelFont = info.getFont();
		JLabel passedTimeLabel;
		JLabel eventNumberLabel;
		if(Global.isAsynchronousMode){
			passedTimeLabel = new JLabel("Time: ");
			passedTimeLabel.setFont(labelFont);
			timePerformed.setText(String.valueOf(round(sinalgo.runtime.Global.currentTime, 4)));
			timePerformed.setEditable(false);
			timePerformed.setBorder(BorderFactory.createEmptyBorder());
			info.add(passedTimeLabel);
			info.add(timePerformed);
		
			if(isMax) {
				eventNumberLabel = new JLabel("Events: ");
				eventNumberLabel.setFont(labelFont);
				roundsPerformed.setText(String.valueOf(EventQueue.eventNumber));
				roundsPerformed.setEditable(false);
				roundsPerformed.setBorder(BorderFactory.createEmptyBorder());
				info.add(eventNumberLabel);
				info.add(roundsPerformed);
			
				info.add(new JPanel()); // add some space between the 'performed rounds' and the 'rounds to perform' 
				info.add(new JPanel());
			}
			
			//roundNumber.setText(String.valueOf(Configuration.defaultRoundNumber));
			roundsToPerformLabel.setText("Events to do:    ");
			roundsToPerformLabel.setFont(labelFont);
			info.add(roundsToPerformLabel);
			info.add(roundsToPerform);
			
		}	else { // Synchronous mode
			passedTimeLabel = new JLabel("Round: ");
			passedTimeLabel.setFont(labelFont);
			timePerformed.setText(String.valueOf((int)round(sinalgo.runtime.Global.currentTime, 4)));
			
			timePerformed.setEditable(false);
			timePerformed.setBorder(BorderFactory.createEmptyBorder());
			
			info.add(passedTimeLabel);
			info.add(timePerformed);
			
			info.add(new JPanel()); // add some space between the 'performed rounds' and the 'rounds to perform' 
			info.add(new JPanel());
			
			roundsToPerformLabel.setText("Rounds to do:  ");
			roundsToPerformLabel.setFont(labelFont);
			info.add(roundsToPerformLabel);
			info.add(roundsToPerform);
		}
		
		refreshRate.setText(String.valueOf(Configuration.refreshRate));
		JLabel refreshLabel = new JLabel("Refresh rate: "); 
		refreshLabel.setFont(labelFont);
		info.add(refreshLabel);
		info.add(refreshRate);
		
		NonRegularGridLayout nrgl = new NonRegularGridLayout(info.getComponentCount() / 2, 2, 1, 2);
		nrgl.setAlignToLeft(true);
		info.setLayout(nrgl);
		roundControl.add(info);
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 5));
		buttons.add(start);
		
		// the run-selection button
		runMenuButton = createFrameworkIconButton("RunMenu", "maximize.gif", "Run Options");
		runMenuButton.setPreferredSize(new Dimension(13, 29));
		buttons.add(runMenuButton);
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
		addToDisabledButtonList(runMenuButton); // disable while running
		
		abort = createFrameworkIconButton("Abort", "abort.gif", "Abort Simulation");
		abort.setEnabled(false);
		buttons.add(abort);
		
		roundControl.add(buttons); 
		
		// Async mode - list of events 
		// ------------------------------------------------------------------------
		//if there is an actual Event: add the panel.
		if(Global.isAsynchronousMode){
			events.setLayout(new BorderLayout());
			
			String[] elements = {currentEventString};
			eventJList = new MultilineToolTipJList();
			eventJList.setListData(elements);
			eventJList.setToolTipText("The last Event that has been executed.\nDouble click the event to get more information.");
			eventJList.setCellRenderer(new NonColoringNonBorderingCellRenderer());
			MouseListener mouseListener = new MouseAdapter() {
			      public void mouseClicked(MouseEvent e) {
			          if (e.getButton() == MouseEvent.BUTTON1){
			        	  if(e.getClickCount() == 2) {
			        		  eventJList.setCellRenderer(dLCR);
			        		  JOptionPane.showMessageDialog(null, currentEventString+"\n"+currentEventToolTip, "Information about an Event", JOptionPane.NO_OPTION);
			        		  eventJList.setCellRenderer(nCNBCR);
			        	  }
			          }
			      }
			};
			eventJList.addMouseListener(mouseListener);
			eventJList.setFixedCellHeight(fixedCellHeight);
			eventJList.setFixedCellWidth(fixedCellWidth);
			eventJList.setPreferredSize(new Dimension(controlPanelWidth, fixedCellHeight+6));
			eventJList.setBorder(javax.swing.plaf.metal.MetalBorders.getTextFieldBorder());
			eventJList.setFont(eventJList.getFont().deriveFont(Font.PLAIN));
			
			events.add(BorderLayout.NORTH, eventJList);
			
			for(int i = 0; i < queueElements.length; i++){
				queueElements[i] = new EventQueueElement(null, null);
			}
			
			composeEventList();
			eventList = new EventQueueList(queueElements);
			Runtime.eventQueue.addEventQueueListener(MaximizedControlPanel.this);
			eventList.setCellRenderer(new NonColoringNonBorderingCellRenderer());
			eventList.setFixedCellHeight(fixedCellHeight);
			eventList.setFixedCellWidth(fixedCellWidth);
			eventList.setFont(eventList.getFont().deriveFont(Font.PLAIN));
			scrollableEventList = new JScrollPane(eventList);
			
			int height = Configuration.shownEventQueueSize * fixedCellHeight + 4;
			scrollableEventList.setPreferredSize(new Dimension(controlPanelWidth, height));
			
			events.add(BorderLayout.SOUTH, scrollableEventList);
			if(isMax) {
				roundControl.add(events);
			}
		}
		
		// Finally set the size of the viewPanel
		Dimension dim = roundControl.getPreferredSize();
		roundControl.setBounds(0, 0, controlPanelWidth, dim.height);
		simulationPane.setPreferredSize(dim);
	}
	
	
	/**
	 * Creates the content of the project-specific control panel, which
	 * contains the project specific buttons
	 */
	private void createProjectControlPanel() {
		Vector<JButton> cb = createCustomButtons();
		if(cb.size() == 0) {
			return; // no buttons to be displayed
		}
		projectControlContent.removeAll();
		
		JButton minimizeButton;
		if(appConfig.guiControlPanelShowProjectControl) { 
			minimizeButton = createFrameworkIconButton("minimizeProjectControl", "minimize.gif", "Minimize");
		} else {
			minimizeButton = createFrameworkIconButton("maximizeProjectControl", "maximize.gif", "Maximize");
		}
		minimizeButton.setPreferredSize(new Dimension(21, 11));
		projectControlContent.add(minimizeButton, JLayeredPane.PALETTE_LAYER);
		minimizeButton.setBounds(controlPanelWidth - 26, 3, 21, 11);
		addToDisabledButtonList(minimizeButton); // disable while simulating
		
		JPanel customButtons = new JPanel();
		customButtons.setBorder(BorderFactory.createTitledBorder("Project Control"));
		
		if(appConfig.guiControlPanelShowProjectControl) {
			customButtons.setPreferredSize(new Dimension(controlPanelWidth, 3000));
			customButtons.setLayout(new MultiLineFlowLayout(controlPanelWidth, 0, 0));

			for(JButton b : cb) {
				customButtons.add(b);
				addToDisabledButtonList(b);
			}
			customButtons.doLayout();
			// adjust the size of the 
			Dimension d = customButtons.getLayout().preferredLayoutSize(customButtons);
			d.width = controlPanelWidth; // enforce the width
			customButtons.setPreferredSize(d);
		} else {
			customButtons.setLayout(new BoxLayout(customButtons, BoxLayout.Y_AXIS));
		}
		
		projectControlContent.add(customButtons);
		
		// Finally set the size of the viewPanel
		Dimension dim = customButtons.getPreferredSize();
		customButtons.setBounds(0, 0, controlPanelWidth, dim.height);
		projectControlContent.setPreferredSize(dim);
		projectControlContent.invalidate();
	}
	
	
	/**
	 * Creates the content of the view panel 
	 */
	private void createViewPanel() {
		viewContent.removeAll();
		JPanel viewPanel = new JPanel();						
		viewPanel.setBorder(BorderFactory.createTitledBorder("View"));
		viewPanel.setLayout(new BoxLayout(viewPanel, BoxLayout.Y_AXIS));
		viewContent.add(viewPanel, JLayeredPane.DEFAULT_LAYER);

		JButton viewPanelMinimizeButton;
		if(appConfig.guiControlPanelShowFullViewPanel) {
			viewPanelMinimizeButton = createFrameworkIconButton("minimizeView", "minimize.gif", "Minimize");
		} else {
			viewPanelMinimizeButton = createFrameworkIconButton("maximizeView", "maximize.gif", "Maximize");
		}
		viewPanelMinimizeButton.setPreferredSize(new Dimension(21, 11));
		viewContent.add(viewPanelMinimizeButton, JLayeredPane.PALETTE_LAYER);
		viewPanelMinimizeButton.setBounds(controlPanelWidth - 26, 3, 21, 11);
		addToDisabledButtonList(viewPanelMinimizeButton); // disable while simulating
		
		// .... add zoom view
		if(appConfig.guiControlPanelShowFullViewPanel) {
			if(parent.getTransformator().supportReverseTranslation()) {
				// only show the coordinate if it can be mapped from GUI to logic coordinates 
				JPanel mousePos = new JPanel();
				JLabel mousePosLabel = new JLabel("Mouse Position:");
				mousePosLabel.setFont(mousePositionField.getFont());
				mousePos.add(mousePosLabel);
				mousePos.add(mousePositionField);
				mousePositionField.setText("");
				mousePositionField.setEditable(false);
				mousePositionField.setBorder(BorderFactory.createEmptyBorder());
				
				viewPanel.add(mousePos);
			}
			
			zoomPanel = new ZoomPanel(parent, parent.getTransformator());
			zoomPanel.setPreferredSize(new Dimension(controlPanelWidth, 
			                                         zoomPanel.getPreferredHeight(controlPanelWidth)));
			viewPanel.add(zoomPanel);
		}
		
		JPanel buttonPanel = new JPanel();
		FlowLayout buttonLayout = new FlowLayout(FlowLayout.CENTER, 2, 0); 
		buttonPanel.setLayout(buttonLayout);
		// create the buttons

		JButton button = createFrameworkIconButton("zoomIn", "zoominimage.png", "Zoom In");
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
		
		viewPanel.add(buttonPanel);
		
		// Finally set the size of the viewPanel
		Dimension dim = viewPanel.getPreferredSize();
		viewPanel.setBounds(0, 0, controlPanelWidth, dim.height);
		viewContent.setPreferredSize(dim);
	}
	
	
	/**
	 * Handle some actions unique to this maximized control panel
	 * @see sinalgo.gui.controlPanel.ControlPanel#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("minimizeView")) {
			appConfig.guiControlPanelShowFullViewPanel = false;
			createViewPanel();
		} else if(e.getActionCommand().equals("maximizeView")) {
			appConfig.guiControlPanelShowFullViewPanel = true;
			createViewPanel();
		} else if(e.getActionCommand().equals("minimizeText")) {
			appConfig.guiControlPanelShowTextPanel = false;
			createTextPanel();
		} else if(e.getActionCommand().equals("maximizeText")) {
			appConfig.guiControlPanelShowTextPanel = true;
			createTextPanel();
		} else if(e.getActionCommand().equals("minimizeProjectControl")) {
			appConfig.guiControlPanelShowProjectControl = false;
			createProjectControlPanel();
		} else if(e.getActionCommand().equals("maximizeProjectControl")) {
			appConfig.guiControlPanelShowProjectControl = true;
			createProjectControlPanel();
		} else if(e.getActionCommand().equals("maximizeSimControl")) {
			appConfig.guiControlPanelExpandSimulation = true;
			createSimulationPanel();
		} else if(e.getActionCommand().equals("minimizeSimControl")) {
			appConfig.guiControlPanelExpandSimulation = false;
			createSimulationPanel();
		} else if(e.getActionCommand().equals("ClearOutputText")) {
			this.clearOutput();
		} else {
			super.actionPerformed(e);
		}
	}
	
	
	/**
	 * Creates a MaximizedControlPanel for the specified GUI instance.
	 *
	 * @param p The Gui instance to create the MaximizedControlPanel for.
	 */
	public MaximizedControlPanel(GUI p){
		parent = p;
		this.setBorder(BorderFactory.createRaisedBevelBorder());
		this.setLayout(new BorderLayout());

		MyScrollPane msp = new MyScrollPane();
		JScrollPane scrollPane = new JScrollPane(msp);
		scrollPane.setBorder(null);
		
		this.setMaximumSize(new Dimension(controlPanelWidth, 2000));
		this.setMinimumSize(new Dimension(controlPanelWidth, 2000));
		
		this.add(BorderLayout.CENTER, scrollPane);
		this.setVisible(true);
	}
	
	public void setRoundsPerformed(double time, int eventNumber){
		timePerformed.setText(String.valueOf(round(time, 4)));
		roundsPerformed.setText(String.valueOf(eventNumber));
	}
	
	public void setRoundsPerformed(int i){
		timePerformed.setText(String.valueOf(i));
	}
	
	public void setCurrentEvent(Event e){
		setStringsForCurrentEvent(e);
		String[] v = {currentEventString};
		eventJList.setListData(v);
		
		composeEventList();
		eventList.setListData(queueElements);
		// remove the focus from the list, for cases when the wrong one is installed (which happens if one presses the ESC button)
		eventList.setCellRenderer(nCNBCR);
	}
	
	public void eventQueueChanged(){
		if(!Global.isRunning){
			composeEventList();
			eventList.setListData(queueElements);
			// remove the focus from the list, for cases when the wrong one is installed (which happens if one presses the ESC button)
			eventList.setCellRenderer(nCNBCR);
		}
	}
	
	private void composeEventList(){
		Iterator<Event> eventIter = Runtime.eventQueue.iterator();
		for(int i = 0; i < queueElements.length; i++){
			if(eventIter.hasNext()){
				Event e = eventIter.next();
				queueElements[i].setText(e.getEventListText(false));
				queueElements[i].setToolTipText(e.getEventListToolTipText(false));
				queueElements[i].setEvent(e);
			}	else {
				queueElements[i].setText(null);
				queueElements[i].setToolTipText(null);
				queueElements[i].setEvent(null);
			}
		}
	}
	
	private class NonColoringNonBorderingCellRenderer extends DefaultListCellRenderer {
		  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,  boolean cellHasFocus) {
		    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		    setBackground(new Color(255, 255, 255));
		    setBorder(BorderFactory.createEmptyBorder(0,1,0,0));
		    return this;
		  }  
	}
	
	private class EventQueueElement extends JComponent{
		private String displayableText = "";
		private String tooltip = "";
		private Event event = null;
		
		private EventQueueElement(String displayableText, String tooltip){
			this.displayableText = displayableText;
			this.tooltip = tooltip;
		}
		
		/**
		 * Sets the text for this element. To get this text call the toString method.
		 * 
		 * @param displayableText The text to be set for this element.
		 */
		public void setText(String displayableText){
			this.displayableText = displayableText;
		}
		
		public void setToolTipText(String tooltip){
			this.tooltip = tooltip;
		}
		
		public void setEvent(Event e) {
			event = e;
		}
		
		/**
		 * @return the event associated w/ this element, null if there is no event assoc. w/ this element
		 */
		public Event getEvent() {
			return event;
		}
		
		public String toString(){
			return displayableText;
		}
		
		public String getToolTipText(){
			return tooltip;
		}	
	}
	
	@SuppressWarnings("serial")
	public class EventQueueList extends JList{
		
		/**
		 * Creates an instance of EventQueueList for the given Array of elements.
		 *
		 * @param data The Array of elements to be contained in the list.
		 */
		private EventQueueList(Object[] data){
			super(data);
			MouseListener mouseListener = new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if(e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
						int index = locationToIndex(e.getPoint());
						if(queueElements[index] != null && queueElements[index].toString() != null){
							EventQueueElement selElem = queueElements[index];
							setSelectedIndex(index);
							setCellRenderer(dLCR); // mark the element
							JOptionPane.showMessageDialog(null, selElem.toString()+"\n"+selElem.getToolTipText(), "Information about an Event", JOptionPane.NO_OPTION);
							setCellRenderer(nCNBCR); // unmark it
						}
					}
					if(e.getButton() == MouseEvent.BUTTON3) {
						int index = locationToIndex(e.getPoint());
						if(index >=0 && queueElements[index] != null && queueElements[index].getEvent() != null) {
							Event event = queueElements[index].getEvent();
							setSelectedIndex(index);
							setCellRenderer(dLCR); // mark the element
							EventPopupMenu epm = new EventPopupMenu(event, EventQueueList.this, nCNBCR);
							epm.show(e.getComponent(), e.getX(), e.getY());
						}			          	
					}
				}
			};
			this.addMouseListener(mouseListener);
		}
		
		public JToolTip createToolTip(){
			return new MultiLineToolTip();
		}
		
		public String getToolTipText(MouseEvent event){
			if(this.getCellBounds(0, this.getModel().getSize()-1).contains(event.getPoint())){
				int index = this.locationToIndex(event.getPoint());
				EventQueueElement element = (EventQueueElement)this.getModel().getElementAt(index);
				return element.getToolTipText();
			}
			else{
				if(((EventQueueElement)this.getModel().getElementAt(0)).displayableText == null){
					return "No event scheduled";
				}
				else{
					return null;
				}
			}
		}
	}
}
