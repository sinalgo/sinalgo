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


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;

import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration;
import sinalgo.gui.GUI;
import sinalgo.gui.multiLineTooltip.MultilineToolTipJList;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.gui.transformation.Transformation3D;
import sinalgo.runtime.AbstractCustomGlobal;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.runtime.Runtime;
import sinalgo.runtime.events.Event;
import sinalgo.tools.Tuple;


/**
 * The Panel with the buttons to control the simulation. This is the panel on the right hand side
 * of the simulation. It is used to change settings about the simulation, to let it run and to exit.
 */
public abstract class ControlPanel extends JPanel implements ActionListener, MouseListener {
	protected GUI parent = null;
	protected static JTextField roundsToPerform = new JTextField(5); // number of rounds to perform
	protected static JLabel roundsToPerformLabel = new JLabel();
	
	protected static JTextField refreshRate = new JTextField(5);
	protected JTextField roundsPerformed = new JTextField(0);
	protected JTextField timePerformed = new JTextField(0);
	protected JTextField mousePositionField = new JTextField(8);
	protected JPanel info = new JPanel();
	
	protected MultilineToolTipJList eventJList = new MultilineToolTipJList();
	protected static String currentEventString = "No event";
	protected static String currentEventToolTip = "No event executed until now.";
	
	protected static JButton start = null; // reused to keep the picture
	protected JButton abort = null;
	protected JButton runMenuButton = null;
	protected JButton exit = new JButton("Exit");
	protected ZoomPanel zoomPanel = null; 
	
	protected static JTextArea textField = new JTextArea();
	
	static AppConfig appConfig = AppConfig.getAppConfig(); 

	public ControlPanel() {
		start = createFrameworkIconButton("Start", getRunButtonImageName(), "Run");
	}
	
	/**
	 * The background color of the control panel.
	 */
	public Color bgColor = new Color(getBackground().getRed(), 
	                                 getBackground().getGreen(), 
	                                 getBackground().getBlue()); 
	
	// A list of all buttons that are disabled while a simulation runs
	private Vector<JButton> disabledButtonList = new Vector<JButton>();
	
	public void addToDisabledButtonList(JButton b) {
		if(!disabledButtonList.contains(b)) {
			disabledButtonList.add(b);
		}
	}
	
	
	static { // static initialization 
		roundsToPerform.setText(String.valueOf(Configuration.defaultRoundNumber));
		roundsToPerform.setEditable(appConfig.guiRunOperationIsLimited);
		roundsToPerformLabel.setEnabled(appConfig.guiRunOperationIsLimited);
	}
	
	/**
	 * Adds a button to a list of buttons that are only active
	 * when the simulation is not running.
	 * Avoids duplicates.
	 * @param button The button to add
	 */
	public void includeIdleOnlyButton(JButton button) {
		//if(buttonList)
	}
	
	/**
	 * Creates a new icon button where the icon is supposed to be stored in the framework
	 * @param actionCommand Name of the action that is performed when this button is pressed
	 * @param imageName The name of the image file, which is stored in the directory specified by Configuration.imageDir
	 * @param toolTip Tooltip text to be shown for this button
	 * @return A new JButton with an icon
	 */
	protected JButton createFrameworkIconButton(String actionCommand, String imageName, String toolTip) {
		// To support jar files, we cannot access the file directly
		ClassLoader cldr = this.getClass().getClassLoader();
		JButton b = null;
		try {
			URL url = cldr.getResource(Configuration.imageDir + imageName);
			ImageIcon icon = new ImageIcon(url);
			b = new JButton(icon);
		} catch(NullPointerException e) {
			Main.fatalError("Cannot access the application icon " + imageName + ", which should be stored in\n" +
			                Configuration.binaryDir + "/" + Configuration.imageDir + imageName + ".");
			return null;
		}
		b.setPreferredSize(new Dimension(29, 29));
		return finishButton(b, actionCommand, toolTip);
	}
	
	protected ImageIcon getFrameworkIcon(String imageName) {
		// To support jar files, we cannot access the file directly
		ClassLoader cldr = this.getClass().getClassLoader();
		try {
			URL url = cldr.getResource(Configuration.imageDir + imageName);
			return new ImageIcon(url);
		} catch(NullPointerException e) {
			Main.fatalError("Cannot access the application icon " + imageName + ", which should be stored in\n" +
			                Configuration.binaryDir + "/" + Configuration.imageDir + imageName + ".");
		}
		return null;		
	}

	/**
	 * Creates a new icon button, the icon is supposed to be stored in a folder 'images' in the
	 * current user project.
	 * @param actionCommand Name of the action that is performed when this button is pressed
	 * @param imageName The name of the image file, which is stored in the directory specified by Configuration.imageDir
	 * @param toolTip Tooltip text to be shown for this button
	 * @return A new JButton with an icon
	 */
	protected JButton createCustomIconButton(String actionCommand, String imageName, String toolTip) {
		JButton b = null;
		File f = new File(Global.getProjectSrcDir() + "/images/"+ imageName);
		if(!f.exists()) {
			Main.fatalError("Cannot access the project specific icon " + imageName + ", which should be stored in\n" +
			                Global.getProjectSrcDir() + "/images/"+ imageName + ".");
		}
		ImageIcon icon = new ImageIcon(Global.getProjectSrcDir() + "/images/"+ imageName);
		b = new JButton(icon);
		b.setPreferredSize(new Dimension(29, 29));
		return finishButton(b, actionCommand, toolTip);
	}

	
	protected JButton createTextButton(String actionCommand, String buttonText, String toolTip) {
		JButton b = new JButton(buttonText);
		b.setFont(b.getFont().deriveFont(Font.PLAIN));
		return finishButton(b, actionCommand, toolTip);
	}
	
	private JButton finishButton(JButton b, String actionCommand, String toolTip) {
		b.setActionCommand(actionCommand);
		b.setFocusable(false);
		b.setBorderPainted(false);
		b.setBorder(new SoftBevelBorder(BevelBorder.RAISED));
		b.setBackground(bgColor);
		b.addActionListener(this);
		b.addMouseListener(this); // move over the button => draw border
		b.setToolTipText(toolTip);
		return b;
	}


	/**
	 * Rotates a 3D graph such that the z-axis becomes a single point.
	 * Nothing happens when called on a 2D graph. 
	 */
	public void defaultViewXY() {
		PositionTransformation pt = parent.getTransformator();
		if(pt instanceof Transformation3D) {
			((Transformation3D) pt).defaultViewXY(parent.getGraphPanel().getWidth(),
			                                      parent.getGraphPanel().getHeight());
			parent.setZoomFactor(pt.getZoomFactor());
		}
	}
	
	/**
	 * Rotates a 3D graph such that the y-axis becomes a single point.
	 * Nothing happens when called on a 2D graph.  
	 */
	public void defaultViewXZ() {
		PositionTransformation pt = parent.getTransformator();
		if(pt instanceof Transformation3D) {
			((Transformation3D) pt).defaultViewXZ(parent.getGraphPanel().getWidth(),
			                                      parent.getGraphPanel().getHeight());
			parent.setZoomFactor(pt.getZoomFactor());
		}
	}

	/**
	 * Rotates a 3D graph such that the x-axis becomes a single point. 
	 * Nothing happens when called on a 2D graph. 
	 */
	public void defaultViewYZ() {
		PositionTransformation pt = parent.getTransformator();
		if(pt instanceof Transformation3D) {
			((Transformation3D) pt).defaultViewYZ(parent.getGraphPanel().getWidth(),
			                                      parent.getGraphPanel().getHeight());
			parent.setZoomFactor(pt.getZoomFactor());
		}
	}
	
	/**
	 * The Method to set the start-Button enabled or not. This method is synchronized, because it is called
	 * out of the Thread and thus should only be accessed once a time. This method aditionally
	 * guarantees, that only one (but certanly one of them) button is set enabled. 
	 *  
	 * @param b The boolean to indicate if the Start-Button is set true or false. 
	 */
	public synchronized void setStartButtonEnabled(boolean b){
		if(b){
			abort.setBorderPainted(false);
		}
		else{
			start.setBorderPainted(false);
		}
		
		abort.setEnabled(!b);
		start.setEnabled(b);
		roundsToPerform.setEnabled(b); 
		refreshRate.setEnabled(b);
		
		for(JButton button : disabledButtonList) {
			button.setEnabled(b);
		}
	}
	
	/**
	 * This Method returns the default Button of the Control Panel.
	 * @return The Default Button.
	 */
	public JButton getDefaultButton(){
		return start;
	}
	

	/**
	 * Appends some text to the output text field.
	 * @param s
	 */
	public void appendTextToOutput(String s) {
		textField.append(s);
		textField.setCaretPosition(textField.getText().length());
	}
	
	/**
	 * A simple output streamer for the output text field.
	 */
	public class TextOutputPrintStream extends PrintStream { 
		public TextOutputPrintStream(OutputStream out) {
			super(out);
		}
		public void setCaretPosition() {
			textField.setCaretPosition(textField.getText().length());
		}
		/* (non-Javadoc)
		 * @see java.io.PrintStream#println(java.lang.String)
		 */
		public void println(String s) {
			textField.append(s);
			textField.append("\n");
			textField.setCaretPosition(textField.getText().length());
		}
		/* (non-Javadoc)
		 * @see java.io.PrintStream#print(java.lang.String)
		 */
		public void print(String s) {
			textField.append(s);
			textField.setCaretPosition(textField.getText().length());
		}
	}
	
	/**
	 * @return A print stream that prints to the output text field
	 */
	public TextOutputPrintStream getTextOutputPrintStream() {
		return new TextOutputPrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				textField.append(Character.toString((char) b));
			}
		});
	}
	
	/**
	 * Removes all text from the text field. 
	 */
	public void clearOutput() {
		textField.setText("");
	}
	
	/**
	 * Set the current time and the event number. 
	 * @param time The current time.
	 * @param eventNumber The number of events that have been executed until now.
	 */
	public abstract void setRoundsPerformed(double time, int eventNumber);

	/**
	 * This Method changes the number of rounds already performed. This number is shown on the
	 * top of the control Panel.
	 * @param i The new Number of Steps already Performed.
	 */
	public abstract void setRoundsPerformed(int i);
	
	/**
	 * Sets the event that was executed last. 
	 * @param e The event that was last processed, null if there was no event.
	 */
	public abstract void setCurrentEvent(Event e);
	
	/**
	 * Sets the current mouse position
	 * @param s A string representation of the position
	 */
	public void setMousePosition(String s) {
		mousePositionField.setText(s);
	}
	
	/**
	 * Starts the simulation by first reading from the input fields the refresh rate and 
	 * the number of rounds to perform.  
	 */
	public void startSimulation() {
		try{
			int rr = Integer.parseInt(refreshRate.getText());
			if(rr <= 0){
				Main.minorError("Invalid input: '" + refreshRate.getText() + "' is not a positive integer.\nThe refresh rate has to be a positive integer.");
				return;
			}
			Configuration.refreshRate = rr;
		}	catch(java.lang.NumberFormatException nFE){
			Main.minorError("Invalid input: '" + refreshRate.getText() + "' is not a valid integer.");
			return;
		}
		try {
			int rounds = Integer.parseInt(roundsToPerform.getText());
			if(rounds <= 0){
				Main.minorError("Invalid input: '" + roundsToPerform.getText() + "' is not a positive integer.\nThe number of rounds has to be a positive integer.");
				return;
			}
			parent.setStartButtonEnabled(false);
			parent.runtime.run(rounds, true);
		}
		catch(java.lang.NumberFormatException nFE){
			Main.minorError("Invalid input: '" + roundsToPerform.getText() + "' is not a valid integer.");
		}
	}
	
	/**
	 * Stops a running simulation. 
	 */
	public void stopSimulation() {
		parent.runtime.abort();
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e){
		if(e.getActionCommand().equals(exit.getActionCommand())) {
			Main.exitApplication();
		}
		else if(e.getActionCommand().equals(start.getActionCommand())){
			startSimulation();
		}
		else if(e.getActionCommand().equals(abort.getActionCommand())){
			stopSimulation();
		}
		else if(e.getActionCommand().equals(runMenuButton.getActionCommand())) {
			// Show the menu containing the run options
			RunPopupMenu rp = new RunPopupMenu();
			Point p = runMenuButton.getLocationOnScreen();
			Point guiP = this.getLocationOnScreen();
			rp.show(this, p.x - guiP.x - 29, p.y - guiP.y + 29);
		}
		else if(e.getActionCommand().equals("zoomIn")) {
			parent.zoomIn();
		} 
		else if(e.getActionCommand().equals("zoomOut")) {
			parent.zoomOut();
		} 
		else if(e.getActionCommand().equals("zoomToFit")) {
			parent.getTransformator().zoomToFit(parent.getGraphPanel().getWidth(),
			                                    parent.getGraphPanel().getHeight());
			parent.setZoomFactor(parent.getTransformator().getZoomFactor());
		} 
		else if(e.getActionCommand().equals("zoomToFit3D")) {
			parent.getTransformator().defaultView(parent.getGraphPanel().getWidth(),
			                                      parent.getGraphPanel().getHeight());
			parent.setZoomFactor(parent.getTransformator().getZoomFactor());
		} else if(e.getActionCommand().equals("extendPanel")) {
			parent.changePanel(true);
		} else if(e.getActionCommand().equals("minimizedPanel")) {
			parent.changePanel(false);
		} else if(e.getActionCommand().equals("clearGraph")) {
			parent.clearAllNodes();
		} else if(e.getActionCommand().equals("addNodes")) {
			parent.addNodes();
		} else if(e.getActionCommand().equals("connectNodes")) {
			Runtime.reevaluateConnections(); // could ask...
			parent.redrawGUI();
		} else {
			// test whether its a custom button
			for(Tuple<JButton, Method> t : customButtons) {
				if(t.first == e.getSource()) {
					try {
						synchronized(parent.getTransformator()){
							//synchronize it on the transformator to grant not to be concurrent with
							//any drawing or modifying action
							t.second.invoke(Global.customGlobal);
						}
					} catch (IllegalArgumentException e1) {
						Main.fatalError("Error while invoking custom method, triggered through button:\n" + e1.getMessage() + "\n\n" + e1);
					} catch (IllegalAccessException e1) {
						Main.fatalError("Error while invoking custom method, triggered through button:\n" + e1.getMessage() + "\n\n" + e1);
					} catch (InvocationTargetException e1) {
						if(e1.getCause() != null) {
							Main.minorError("Exception thrown while executing '" + t.second.getName() + "'.\n" + e1.getCause().getMessage() + "\n\n" + e1.getCause());
						} else {
							Main.fatalError("Exception thrown while executing '" + t.second.getName() + "'.\n" + e1.getMessage() + "\n\n" + e1);
						}
					}
				}
			}
		}
	}

	Vector<Tuple<JButton,Method>> customButtons = new Vector<Tuple<JButton,Method>>();
	
	/**
	 * Creates a set of custom buttons defined in the CustomGlobal 
	 * of the current project.
	 * @return A vector of the buttons, which should not be modified.
	 */
	protected Vector<JButton> createCustomButtons() {
		Vector<JButton> buttons = new Vector<JButton>();
		Method[] f = Global.customGlobal.getClass().getMethods();
		for(int i = 0; i < f.length; i++){
			AbstractCustomGlobal.CustomButton info = f[i].getAnnotation(AbstractCustomGlobal.CustomButton.class);
			if(info != null) {
				Class<?>[] params = f[i].getParameterTypes();
				if(params.length != 0) { // we only accept methods with no parameters
					continue;
				}
				String command = "GLOBAL_BUTTON_" + f[i].getName();
				JButton b = null;
				if(!info.imageName().equals("")) {
					b = createCustomIconButton(command, info.imageName(), info.toolTipText());
				} else { 
					b = createTextButton(command, info.buttonText(), info.toolTipText());
				}
				buttons.add(b);
				customButtons.add(new Tuple<JButton, Method>(b, f[i]));
			}
		}
		return buttons;
	}
	
	/**
	 * Set text (and tooltip-text) to be displayed for the event that executed last.
	 * @param e The current event, null if there is no event.
	 */
	protected void setStringsForCurrentEvent(Event e){ 
		if(e != null){
			currentEventString = e.getEventListText(true); 
			currentEventToolTip = e.getEventListToolTipText(true);
		} else {
			currentEventString = "No event";
			currentEventToolTip = "No event executed";
		}
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) {
	}

	protected double round(double value, int places){
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}
	
	public void mouseEntered(MouseEvent e) {
		if(e.getSource() instanceof JButton) {
			if(((JButton) e.getSource()).isEnabled()){
				((JButton) e.getSource()).setBorderPainted(true);
			}
		}
	}

	public void mouseExited(MouseEvent e) {
		if(e.getSource() instanceof JButton) {
			((JButton) e.getSource()).setBorderPainted(false);
		}
	}
	

	//-----------------------------------------------------------------
	// Code for the RUN button
	//-----------------------------------------------------------------
	
	/**
	 * Called whenever the type of RUN-operation is changed.
	 * @param isLimited True if the RUN operation should stop after
	 * the indicated # of nodes, false if the RUN operation should
	 * perform as many steps/events as possible.
	 */
	public void setRunType(boolean isLimited) {
		roundsToPerform.setEditable(isLimited);
		roundsToPerformLabel.setEnabled(isLimited);
		appConfig.guiRunOperationIsLimited = isLimited;
		start.setIcon(getFrameworkIcon(getRunButtonImageName()));
	}

	/**
	 * @return The name of the Icon to use for the run button, according
	 * to the current settings.
	 */
	public String getRunButtonImageName() {
		if(Configuration.handleEmptyEventQueue && Configuration.asynchronousMode) {
			if(appConfig.guiRunOperationIsLimited) {
				return "refillrun.gif";
			} else {
				return "refillrunforever.gif";
			}
		} else {
			if(appConfig.guiRunOperationIsLimited) {
				return "run.gif";
			} else {
				return "runforever.gif";
			}
		}
	}
	
	@SuppressWarnings("serial")
	public class RunPopupMenu extends JPopupMenu implements ActionListener {
		protected JMenuItem runForever = new JMenuItem("Run Forever", getFrameworkIcon("runforever.gif"));
		protected JMenuItem runCount = new JMenuItem("Run Specified # of " + (Global.isAsynchronousMode ? "Events" : "Rounds"), getFrameworkIcon("run.gif"));
		protected JCheckBoxMenuItem refillEventQueueMenuItem = new JCheckBoxMenuItem("Refill Event Queue", Configuration.handleEmptyEventQueue);

		protected RunPopupMenu() {
			//if(appConfig.guiRunOperationIsLimited) {
			this.add(runForever);
			//} else {
			this.add(runCount);
			//}
			if(Configuration.asynchronousMode) {
				this.addSeparator();
				this.add(refillEventQueueMenuItem);
			}
			runForever.addActionListener(this);
			runCount.addActionListener(this);
			refillEventQueueMenuItem.addActionListener(this);
		}

		public void actionPerformed(ActionEvent e) {
			if(e.getActionCommand().equals(runForever.getActionCommand())) {
				setRunType(false);
			}
			else if(e.getActionCommand().equals(runCount.getActionCommand())) {
				setRunType(true);
			}
			else if(e.getActionCommand().equals(refillEventQueueMenuItem.getActionCommand())) {
				Configuration.handleEmptyEventQueue = refillEventQueueMenuItem.isSelected();
				start.setIcon(getFrameworkIcon(getRunButtonImageName()));
			}
		}

	}
	
	
}
