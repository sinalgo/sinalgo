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
package projects.sample1;


import java.lang.reflect.Method;
import java.util.Enumeration;

import javax.swing.JOptionPane;

import projects.sample1.nodes.nodeImplementations.S1Node;
import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.runtime.AbstractCustomGlobal;
import sinalgo.runtime.GUIRuntime;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.tools.Tools;
import sinalgo.tools.logging.Logging;

/**
 * This class holds customized global state and methods for the framework. 
 * The only mandatory method to overwrite is 
 * <code>hasTerminated</code>
 * <br>
 * Optional methods to override are
 * <ul>
 * <li><code>customPaint</code></li>
 * <li><code>handleEmptyEventQueue</code></li>
 * <li><code>onExit</code></li>
 * <li><code>preRun</code></li>
 * <li><code>preRound</code></li>
 * <li><code>postRound</code></li>
 * <li><code>checkProjectRequirements</code></li>
 * </ul>
 * @see sinalgo.runtime.AbstractCustomGlobal for more details.
 * <br>
 * In addition, this class also provides the possibility to extend the framework with
 * custom methods that can be called either through the menu or via a button that is
 * added to the GUI. 
 */
public class CustomGlobal extends AbstractCustomGlobal{
	
	Logging log = Logging.getLogger("s1_log.txt");
	
	// The user can optionally specify exitAfter in the config file to indicate after how many rounds the simulation should stop. 
	boolean exitAfterFixedRounds = false;
	int exitAfterNumRounds;
	{
		if(Configuration.hasParameter("exitAfter")) {
			try {
				exitAfterFixedRounds = Configuration.getBooleanParameter("exitAfter");
			} catch (CorruptConfigurationEntryException e1) {
				Tools.fatalError("The 'exitAfter' needs to be a valid boolean.");
			}
			if(exitAfterFixedRounds) {
				try {
					exitAfterNumRounds = Configuration.getIntegerParameter("exitAfter/rounds");
				} catch (CorruptConfigurationEntryException e) {
					Tools.fatalError("The 'exitAfter/rounds' parameter specifies the maximum time the simulation runs. It needs to be a valid integer.");
				}
			}
		} else {
			exitAfterFixedRounds = false;
		}
	}
	
	
	/* (non-Javadoc)
	 * @see runtime.AbstractCustomGlobal#hasTerminated()
	 */
	public boolean hasTerminated(){
		if(exitAfterFixedRounds) {
			return exitAfterNumRounds <= Global.currentTime;
		}

		if(Tools.isSimulationInGuiMode()) {
			return false; // in GUI mode, have the user decide when to stop.
		} else {
			return Global.currentTime > 100000; // stop after x rounds 
		}
	}

	/**
	 * An example of a method that will be available through the menu of the GUI.
	 */
	@AbstractCustomGlobal.GlobalMethod(menuText="Echo", order=1)
	public void echo() {
		// Query the user for an input
		String answer = JOptionPane.showInputDialog(null, "This is an example.\nType in any text to echo.");
		// Show an information message 
		JOptionPane.showMessageDialog(null, "You typed '" + answer + "'", "Example Echo", JOptionPane.INFORMATION_MESSAGE);
	}
	
	/* (non-Javadoc)
	 * @see runtime.AbstractCustomGlobal#preRun()
	 */
	public void preRun() {
		// start the communication automatically if the AutoStart flag is set.
		try {
			if(Configuration.hasParameter("AutoStart") && Configuration.getBooleanParameter("AutoStart")) {
				S1Node n = (S1Node) Tools.getNodeList().getRandomNode();
				n.start(); // start from a random node
			}
		} catch (CorruptConfigurationEntryException e) {
			Tools.fatalError("The 'AutoStart' option in the configuration file specifies whether a node" +
			                 "should be automatically selected to start the communication process. This flag needs to be" +
			                 "of type boolean (true|false).");
		}
	}
	
	public void postRound() {
		double dt = System.currentTimeMillis() - Global.startTimeOfRound.getTime();
		log.logln("Round " + (int)(Global.currentTime) + " time: "  + dt + " Msg/Round: " + Global.numberOfMessagesInThisRound);
	}
	
	
	/**
	 * Custom button to generate a infomation Dialog to show the node with the maximum sent messages.
	 */
	@CustomButton(buttonText="OKButton", imageName="OK.gif", toolTipText="Prints out the maximum sent messages of all nodes.")
	public void printMaxMsgSent(){
		S1Node max = null;
		Enumeration<?> nodeEnumer = Tools.getNodeList().getNodeEnumeration();
		while(nodeEnumer.hasMoreElements()){
			S1Node s1Node = (S1Node)nodeEnumer.nextElement();
			if(max == null){
				max = s1Node;
			}
			else{
				if(max.msgSent < s1Node.msgSent){
					max = s1Node;
				}
			}
		}
		if(Global.isGuiMode){
			if(max != null){
				JOptionPane.showMessageDialog(((GUIRuntime)Main.getRuntime()).getGUI(), "The node with the maximum sent number of messages is the node with id "+max.ID+". \nIt sent "+max.msgSent+" messages until now.");
			}
			else{
				JOptionPane.showMessageDialog(((GUIRuntime)Main.getRuntime()).getGUI(), "There is no node.");
			}
		}
	}
	
	/* The method stopSending can be called through the 'Global' menu of Sinalgo.
	 * The menu-item is placed in a sub-menu 'Node Control', order='2' guarantees
	 * that it is placed after the 'Echo' menu.
	 * Note the use of the method includeGlobalMethodInMenu which lets you specify
	 * at each time the menu pops up, what menu-text should be displayed (or no menu
	 * at all, if the method returns null.) 
	 */
	
	@GlobalMethod(menuText="...", subMenu="Node Control", order=2)
	public void stopSending() {
		S1Node.isSending = !S1Node.isSending;
	}

	/* (non-Javadoc)
	 * @see sinalgo.runtime.AbstractCustomGlobal#includeGlobalMethodInMenu(java.lang.reflect.Method, java.lang.String)
	 */
	public String includeGlobalMethodInMenu(Method m, String defaultText) {
		if(m.getName().equals("stopSending")) {
			if(Tools.getNodeList().size() == 0) {
				return null; // don't display this menu option
			}
			return S1Node.isSending ? "Stop Sending" : "Continue Sending";
		}
		return defaultText;
	}
	
	
	public void checkProjectRequirements(){
		if(Global.isAsynchronousMode){
			Main.fatalError("SampleProject1 is written to be executed in synchronous mode. It doesn't work in asynchronous mode.");
		}
	}
	
	public void onExit() {
		// perform some cleanup operations here
	}
	
}
