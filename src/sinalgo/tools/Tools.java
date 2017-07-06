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
package sinalgo.tools;


import javax.swing.JOptionPane;

import sinalgo.configuration.WrongConfigurationException;
import sinalgo.gui.GUI;
import sinalgo.gui.GraphPanel;
import sinalgo.gui.helper.NodeSelectionHandler;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.io.mapIO.Map;
import sinalgo.models.ConnectivityModel;
import sinalgo.models.DistributionModel;
import sinalgo.models.InterferenceModel;
import sinalgo.models.MessageTransmissionModel;
import sinalgo.models.MobilityModel;
import sinalgo.models.Model;
import sinalgo.models.ModelType;
import sinalgo.models.ReliabilityModel;
import sinalgo.nodes.Node;
import sinalgo.nodes.edges.Edge;
import sinalgo.nodes.edges.EdgePool;
import sinalgo.nodes.messages.Packet;
import sinalgo.runtime.AbstractCustomGlobal;
import sinalgo.runtime.BatchRuntime;
import sinalgo.runtime.GUIRuntime;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.runtime.NotInBatchModeException;
import sinalgo.runtime.NotInGUIModeException;
import sinalgo.runtime.Runtime;
import sinalgo.runtime.events.EventQueue;
import sinalgo.runtime.events.PacketEvent;
import sinalgo.runtime.events.TimerEvent;
import sinalgo.runtime.nodeCollection.NodeCollectionInterface;
import sinalgo.runtime.packetsInTheAir.PacketsInTheAirBuffer;
import sinalgo.tools.logging.Logging;
import sinalgo.tools.statistics.Distribution;


import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

/**
 * This class holds wrappers to often used method calls into the framework
 * to ease quick development and prevent the project-programmer from searching
 * through the framework implementation to get a certain functionality. 
 */
public class Tools {
	
	//**************************************************************************************
	// Error handling
	//**************************************************************************************
	
	/**
	 * Shows a warning to the user.
	 * <p>
	 * The warning-message is printed to the log-file, only.
	 * Additionally, in GUI-mode, a pop-up message informs the user about the problem.
	 * 
	 * @param message The message containing the warning.
	 */
	public static void warning(String message) {
		Main.warning(message);
	}
	
	/**
	 * Handles an error which does not require termination of the application, 
	 * but that needs to be propagated to the user.
	 * <p>
	 * The error-message is printed to the log-file and the System.err output.
	 * Additionally, in GUI-mode, a pop-up message informs the user about the problem.
	 * 
	 * @param message The message containing the error description.
	 */
	public static void minorError(String message) {
		Main.minorError(message);
	}
	
	/**
	 * Exits the application due to a fatal error.
	 *
	 * Before exiting, an error-message is diplayed if in GUI-mode. In any case, the error is written to System.err. 
	 * 
	 * @param message The message containing the error description.
	 */
	public static void fatalError(String message) {
		Main.fatalError(message);
	}
	
	//**************************************************************************************
	// Information about the algorithm like the number of sent messages and the current time
	//**************************************************************************************
	
	/**
	 * @return The global time of the simulation.
	 */
	public static double getGlobalTime(){
		return Global.currentTime;
	}
	
	/**
	 * @return The time the last synchronous round executed. Note that this method will 
	 * exit with a fatal error when called in asynchronous mode.
	 */
	public static Date getStartTimeOfRound(){
		if(!Global.isAsynchronousMode){
			return Global.startTime;
		}
		else{
			Main.fatalError("Cannot get the startTime of the round in asynchronous mode");
//			will never reach this code, but for the compiler...
			return null;
		}
	}
	
	/**
	 * @return The number of messages that have been sent in this round. Note that this 
	 * method will exit with a fatal error when called in asynchronous mode.
	 */
	public static int getNumberOfMessagesSentInThisRound(){
		if(!Global.isAsynchronousMode){
			return Global.numberOfMessagesInThisRound;
		}
		else{
			Main.fatalError("Cannot get the startTime of the round in asynchronous mode");
//			will never reach this code, but for the compiler...
			return 0;
		}
	}
	
	/**
	 * @return The number of message that have been sent so far since the start of the 
	 * framework.
	 */
	public static int getNumberOfSentMessages(){
		return Global.numberOfMessagesOverAll;
	}
	
	//**************************************************************************************
	// Information about settings of the framework.
	//**************************************************************************************

	/**
	 * @return true if the simulation is running in GUI-mode, false if it is running in batch
	 * mode.
	 */
	public static boolean isSimulationInGuiMode(){
		return Global.isGuiMode;
	}

	/**
	 * @return true if the simulation runs in asynchronous mode, otherwise false.
	 */
	public static boolean isSimulationInAsynchroneMode() {
		return Global.isAsynchronousMode;
	}

	/**
	 * @return true if the simulation runs in synchronous mode, otherwise false.
	 */
	public static boolean isSimulationInSynchroneMode() {
		return !Global.isAsynchronousMode;
	}

	
	/**
	 * @return true if the simulation is running in batch-mode, false if it is running in GUI
	 * mode.
	 */
	public static boolean isSimulationInBatchMode(){
		return !Global.isGuiMode;
	}
	
	/**
	 * @return true if the simulation is currently running, false otherwise. Running means that
	 * it is currently executing simulation steps (or performing events). 
	 */
	public static boolean isSimulationRunning(){
		return Global.isRunning;
	}
	
	//**************************************************************************************
	// Accessing simulation parameters
	//**************************************************************************************

	/**
	 * Gets the random number generator to be used for this simulation, which may be
	 * generated with a seed. Use only this simulation-owned random number generator
	 * such that the simulation may be repeated.   
	 * @return The random number generator to be used for this simulation. 
	 */
	public static Random getRandomNumberGenerator() {
		return Distribution.getRandom();
	}
	
	/**
	 * @return the graph panel of the current GUI. Note that this method exits with a fatal error
	 * when the simulation is not in GUI mode.
	 */
	public static GraphPanel getGraphPanel() {
		return getGuiRuntime().getGUI().getGraphPanel();
	}
	
	/**
	 * @return returns the GUI instance. Note that this method exists with a fatal error when the 
	 * simulation is not in GUI mode.
	 */
	public static GUI getGUI(){
		return getGuiRuntime().getGUI();
	}
	
	/**
	 * @return The position transformation object used to display the 
	 * network connectivity graph.
	 */
	public static PositionTransformation getPositionTransformation() {
		return Main.getRuntime().getTransformator();
	}
	
		/**
	 * Stops a running simulation. 
	 * <p> 
	 * In synchronous mode, the simulation stops before handling the next event, 
	 * in asynchronous mode, the simulation stops before starting a new round.
	 * <p>
	 * When called in batch mode, this method exits the simulation. In GUI mode, 
	 * a call to this method is equivalent to pressing the abort button, the user 
	 * may continue by pressing again the run button.  
	 */
	public static void stopSimulation() {
		if(Global.isGuiMode) {
			Main.getRuntime().abort();
		} else {
			Main.exitApplication();
		}
	}
	
	/**
	 * Exits the simulation and closes the application. 
	 */
	public static void exit() {
		Main.exitApplication();
	}
	
	/**
	 * @return The default logger instance.
	 */
	public static Logging getDefaultLogger(){
		return Global.log;
	}
	
	/**
	 * @return The name of the currently selected project.
	 */
	public static String getProjectName(){
		return Global.projectName;
	}
	
	/**
	 * @return The Buffer of all messages that are currently being sent. This buffer is
	 * only filled when interference is turned on. Otherwise it is empty. Do not change
	 * values of packets in this buffer as this will influence the simulation. Note that
	 * this buffer contains packets and not messages. Packets are wrapper classes around
	 * the messages and add information about the sender, the intensity...
	 */
	public static PacketsInTheAirBuffer getPacketsInTheAir(){
		return Runtime.packetsInTheAir;
	}
	
	/**
	 * @return The globally set instance of the messageTransmissionModel. This model
	 * determines how long it takes a message to travel from its origin to its destination.
	 */
	public static MessageTransmissionModel getMessageTransmissionModel(){
		return Global.messageTransmissionModel;
	}
	
	/**
	 * @return The CustomGlobal instance of the currently selected project.
	 */
	public static AbstractCustomGlobal getCustomGlobal(){
		return Global.customGlobal;
	}
	
	/**
	 * @return The GUIRuntime instance. With this method you can avoid catching the 
	 * NotInGUIModeException. Note that this method will exit with a fatal error when
	 * the simulation is not in GUI-Mode. Only call it if you are sure that you are in
	 * gui mode (call Tools.isSimulationInGuiMode to find out).
	 */
	public static GUIRuntime getGuiRuntime(){
		try {
			return Main.getGuiRuntime();
		} catch (NotInGUIModeException e) {
			Main.fatalError(e);
			//will never reach this code, but for the compiler...
			return null;
		}
	}
	
	/**
	 * @return The BatchRuntime instance. With this method you can avoid catching the 
	 * NotInBatchModeException. Note that this method will exit with a fatal error when
	 * the simulation is not in batch-Mode. Only call it if you are sure that you are in
	 * batch mode (call Tools.isSimulationInBatchMode to find out).
	 */
	public static BatchRuntime getBatchRuntime(){
		try {
			return Main.getBatchRuntime();
		} catch (NotInBatchModeException e) {
			Main.fatalError(e);
			//will never reach this code, but for the compiler...
			return null;
		}
	}
	
	/**
	 * @return The runtime instance. Note that the returned Runtime is either a BatchRuntime
	 * or a GUIRuntime object. If you want to avoid casting the Runtime to a Batch- resp. GUI-
	 * Runtime use the getGuiRuntime or the getBatchRuntime methods.
	 */
	public static Runtime getRuntime(){
		return Main.getRuntime();
	}
	
	/**
	 * @return The event queue that holds all scheduled events in the asynchronous simulation mode.
	 */
	public static EventQueue getEventQueue() {
		return Runtime.eventQueue;
	}
	
	/**
	 * @return The map describing the background of the simulation, null
	 * if no map is set. 
	 */
	public static Map getBackgroundMap() {
		return Runtime.map;
	}

	/**
	 * @return A list of all nodes currently added to the framework.
	 */
	public static NodeCollectionInterface getNodeList() {
		return Runtime.nodes;
	}
	
	/**
	 * Returns the node with the given ID, null if there is no such node.
	 * Note: This method is rather expensive, as it loops over all
	 * nodes until it finds the suitable node.  
	 * @param id The ID of the node to return.
	 * @return The node with the given ID, null if there is no such node.
	 */
	public static Node getNodeByID(int id) {
		for(Node n : Runtime.nodes) {
			if(n.ID == id) {
				return n;
			}
		}
		return null;
	}
	
	/**
	 * @return A randomly chosen node from all nodes currently deployed in the framework.
	 */
	public static Node getRandomNode() {
		return Runtime.nodes.getRandomNode();
	}
	
	/**
	 * Removes all nodes 
	 */
	public static void removeAllNodes() {
		sinalgo.runtime.Runtime.clearAllNodes();
	}
	
	/**
	 * Removes a node from the framework, drops all messages
	 * sent over edges of this node.
	 * @param n 
	 */
	public static void removeNode(Node n) {
		sinalgo.runtime.Runtime.removeNode(n);
	}
	
	/**
	 * Create a set of nodes
	 * @param numNodes # of nodes
	 * @param nodeTypeName The class-name of the node (as on the cmd-line with the -gen flag)
	 * @param distributionModelName Name of the distribution model (as on the cmd-line with the -gen flag)
	 * @param strings optional strings to define the remaining models (as on the cmd-line with the -gen flag)
	 * each model is in a new string, the optional model parameters in parentheses as well.
	 */
	public static void generateNodes(int numNodes, 
	                                 String nodeTypeName, 
	                                 String distributionModelName, 
	                                 String...strings) {
		
		Vector<Tuple<ModelType, Class<?>>> models;
		String[] modelParams = new String[4]; // the optional parameter-strings to the optional models.
		String[] modelNames = new String[4]; // the optional models (the params are stored at same offset in modelParams)
		int numSpecifiedModels = 0;
		
		String distModelParam = "";
		int i = 0;
		// detect 
		if(strings.length > 0 && strings[0].startsWith("(") && strings[0].endsWith(")")) {
			distModelParam = strings[0].substring(1, strings[0].length() - 1);
			i++;
		} 
		
		for(; i<strings.length; i++) {
			if(numSpecifiedModels >= 4) { // too many models specified
				Main.fatalError("Invalid command-line argument: The -gen flag takes at most 4 models\n" +
				                "after the distribution model: (in arbitrary order)\n" +
				                "Connectivity, Interference, Mobility, Reliability\n" +
				                "each of which may be post-fixed with one optional parameter placed in \n" +
				                "parameters.\n" +
				                "Note: The MessageTransmissionModel is global and is set in the config-file.\n\n" +
				                "The arguments for the -gen flag have to be formatted as following:\n" +
				                "-gen #nodes nodeType DistModel [(params)] [{M [(params)]}*]" +
				                "where each model appears AT MOST once. (if you don't specify the model,\n" +
				                "the default model is taken.)  (The MessageTransmissionModel must not be used,\n" +
				                "it is set in the configuration file.)"
				                );
			}
			modelNames[numSpecifiedModels] = strings[i];
			// Detect the parameters!			
			if(strings.length > i+1) {
				String s = strings[i+1];
				if(s.startsWith("(") && s.endsWith(")")) {
					modelParams[numSpecifiedModels] = s.substring(1, s.length() - 1);
					i++; // skip this entry
				}
			} 
			numSpecifiedModels ++;
		}

		// initialize the distribution model
		DistributionModel nodeDistribution = Model.getDistributionModelInstance(distributionModelName);
		nodeDistribution.setParamString(distModelParam);
		nodeDistribution.setNumberOfNodes(numNodes);
		nodeDistribution.initialize();
		
		models = new Vector<Tuple<ModelType, Class<?>>>(4);
		for(int j=0; j < numSpecifiedModels; j++) {
			Tuple<ModelType, Class<?>> tmp = Model.getModelClass(modelNames[j]);
			
			if(tmp.first == ModelType.DistributionModel) {
				Main.fatalError("Invalid command-line argument for the -gen flag:\n" +
				                "The optional parameters to specify the models for the nodes contains\n" +
				                "a DistributionModel.\n\n" +
				                "The arguments for the -gen flag have to be formatted as following:\n" +
				                "-gen #nodes nodeType DistModel [(params)] [{M [(params)]}*]" +
				                "where each model appears AT MOST once. (if you don't specify the model,\n" +
				                "the default model is taken.)  (The MessageTransmissionModel must not be used,\n" +
				                "it is set in the configuration file.)"
				);
			} 
			if(tmp.first == ModelType.MessageTransmissionModel) {
				Main.fatalError("Invalid command-line argument for the -gen flag:\n" +
				                "The optional parameters to specify the models for the nodes contained\n" +
				                "MessageTransmissionModel. This model is globally unique and set through\n" +
				                "the configuration file."
				);
			}
			// test that no duplicate
			for(int k=0; k<j; k++) {
				if(models.elementAt(k).first == tmp.first) {
					Main.fatalError("Invalid command-line argument for the -gen flag:\n" +
					                "The optional parameters to specify the models for the nodes contains\n" +
					                "more than one '" + tmp.first.name() + "' \n\n" + 
					                "The arguments for the -gen flag have to be formatted as following:\n" +
					                "-gen #nodes nodeType DistModel [(params)] [{M [(params)]}*]\n" +
					                "where each model appears AT MOST once. (if you don't specify the model,\n" +
					                "the default model is taken.)  (The MessageTransmissionModel must not be used,\n" +
					                "it is set in the configuration file.)"
					);
				}
			}
			models.add(tmp);
		}

		// Create the nodes
		for(int j=0; j< numNodes; j++) {
			Node node = null;
			try{
				node = Node.createNodeByClassname(nodeTypeName);
			}
			catch(WrongConfigurationException e){
				Main.fatalError(e);
			}
			node.setPosition(nodeDistribution.getNextPosition());
			
			// set the models 
			for(int k=0; k<numSpecifiedModels; k++) {
				Tuple<ModelType, Class<?>> tmp = models.elementAt(k);
				try {
					// NOTE: we could also call newInstance() on the class-object. But this would not encapsulate 
					// exceptions that may be thrown in the constructor.
					Constructor<?> constructor = tmp.second.getConstructor();
					Model m = (Model) constructor.newInstance();
					m.setParamString(modelParams[k]); // set the parameter string for this model
					switch(tmp.first) {
					case ConnectivityModel: {
						node.setConnectivityModel((ConnectivityModel) m);
					} break;
					case MobilityModel: {
						node.setMobilityModel((MobilityModel) m);
					} break;
					case InterferenceModel: {
						node.setInterferenceModel((InterferenceModel) m);
					} break;
					case ReliabilityModel: {
						node.setReliabilityModel((ReliabilityModel) m);
					} break;
					default: {
						assert false; // bug if here
					} break;
					}
				} catch(IllegalAccessException e) {
					Main.fatalError("Cannot generate instance of the model '" + modelNames[k] + "' due to illegal access. " +
					                "(The model needs a public constructor w/o parameters.):\n" + e);
				} catch(InstantiationException e) {
					Main.fatalError("Cannot generate instance of the model '" + modelNames[k] + "' " +
					                "(The model needs a public constructor w/o parameters.):\n" + e);
				} catch (SecurityException e) {
					Main.fatalError("Cannot generate instance of the model'" + modelNames[k] + "' " +
					                "(Probably not sufficient security permissions.):\n" + e);
				} catch (NoSuchMethodException e) {
					Main.fatalError("Cannot generate instance of the model '" + modelNames[k] + "' " +
					                "(The model needs a public constructor w/o parameters.):\n" + e);
				} catch (IllegalArgumentException e) {
					Main.fatalError("Cannot generate instance of the model '" + modelNames[k] + "' " +
					                "(The model needs a public constructor w/o parameters.):\n" + e);
				} catch (InvocationTargetException e) {
					// The constructor has thrown an exception
					Main.fatalError(e.getCause().getMessage() + "\n\nMore Info:\n" + e.getCause());
				}
			}
			// set default models
			node.finishInitializationWithDefaultModels(true);
		}
	}
	
	
	
	//**************************************************************************************
	// GUI Operations
	//**************************************************************************************
	
	/**
	 * Shows a message dialog.
	 * @param infoText Text to display.
	 */
	public static void showMessageDialog(String infoText) {
		JOptionPane.showMessageDialog(null, infoText);
	}
	
	
	/**
	 * Shows a question-message dialog requesting input from the user. 
	 * @param queryText The text to display on the dialog 
	 * @return The text entered by the user, null if the user canceled the process. 
	 */
	public static String showQueryDialog(String queryText) {
		return JOptionPane.showInputDialog(null, queryText);
	}
	
	/**
	 * Asynchronous call to repaint the GUI. The AWT-library will eventually 
	 * schedule the GUI to be repainted. During this repaint cycle, also the
	 * connectivity graph is repainted.
	 * <p>
	 * This method should only be called when running in GUI mode.
	 */
	public static void repaintGUI() {
		try {
			Main.getGuiRuntime().getGUI().redrawGUI();
		} catch (NotInGUIModeException e) {
			// ignore
		}
	}

	/**
	 * Asks the user to select a node with the mouse.
	 * <p>
	 * This method may only be called when running in GUI mode. 
	 * @param handler The handler to invoke when a node is selected
	 * @param text Text to display to the user
	 */
	public static void getNodeSelectedByUser(NodeSelectionHandler handler, String text) {
		try {
			Main.getGuiRuntime().getGUI().getGraphPanel().getNodeSelectedByUser(handler, text);
		} catch (NotInGUIModeException e) {
			Main.fatalError("");
		}
	}

	/**
	 * Rotates the view in 3D such that we see the X-Y plane. This method 
	 * has no effect when running a 2D simulation. 
	 * This method may only be called when running in GUI mode. 
	 */
	public static void XY_View() {
		try {
			sinalgo.runtime.Main.getGuiRuntime().getGUI().getControlPanel().defaultViewXY();
		} catch (NotInGUIModeException e) {
			// ignore
		}
	}

	/**
	 * Rotates the view in 3D such that we see the Y-Z plane. This method 
	 * has no effect when running a 2D simulation. 
	 * This method may only be called when running in GUI mode. 
	 */
	public static void YZ_View() {
		try {
			sinalgo.runtime.Main.getGuiRuntime().getGUI().getControlPanel().defaultViewYZ();
		} catch (NotInGUIModeException e) {
			// ignore
		}
	}

	/**
	 * Rotates the view in 3D such that we see the X-Z plane. This method 
	 * has no effect when running a 2D simulation. 
	 * This method may only be called when running in GUI mode. 
	 */
	public static void XZ_View() {
		try {
			sinalgo.runtime.Main.getGuiRuntime().getGUI().getControlPanel().defaultViewXZ();
		} catch (NotInGUIModeException e) {
			// ignore
		}
	}

	/**
	 * Determines for each node the neighboring nodes and adds / removes edges accordingly.
	 * <p>
	 * This method is intended to be used only in asynchronous simulation node. In synchronous
	 * simulation mode, the connections are automatically evaluated in every round.  
	 */
	public static void reevaluateConnections() {
		Runtime.reevaluateConnections();
	}


	/**
	 * Appends some text to the output text field, nothing happens when not in GUI mode.
	 * @param text The text to append.
	 */
	public static void appendToOutput(String text) {
		if(!Global.isGuiMode) {
			return;
		}
		try {
			sinalgo.runtime.Main.getGuiRuntime().getGUI().getControlPanel().appendTextToOutput(text);
		} catch (NotInGUIModeException e) {
			// ignore
		}
	}
	
	/**
	 * @return A print stream that prints to the GUI output text field when in GUI mode, 
	 * and to the standard log-file when running in batch mode.
	 * <p>
	 * Note that you should only use print(String) and println(String) on this printStream
	 * as the caret-position of the output text field is only updated for these methods.
	 */
	public static PrintStream getTextOutputPrintStream() {
		if(!Global.isGuiMode) {
			return Logging.getLogger().getOutputStream();
		}
		try {
			return sinalgo.runtime.Main.getGuiRuntime().getGUI().getControlPanel().getTextOutputPrintStream();
		} catch(NotInGUIModeException e) {
			// ignore
		}
		return Logging.getLogger().getOutputStream();
	}
	
	/**
	 * Removes all text from the output text field, nothing happens when not in GUI mode. 
	 */
	public static void clearOutput() {
		if(!Global.isGuiMode) {
			return;
		}
		try {
			sinalgo.runtime.Main.getGuiRuntime().getGUI().getControlPanel().clearOutput();
		} catch (NotInGUIModeException e) {
			// ignore
		}
	}
	
	
	//**************************************************************************************
	// Text Operations
	//**************************************************************************************
	
	/**
	 * Adds new-lines to a given string if it is longer than 80 characters.
	 * <p>
	 * A new-line is added such that each line is no longer than 80 characters.
	 * @param s The string to wrap.
	 * @return A wrapped string that contains no more than 80 characters per line.
	 */
	public static String wrapToLines(String s) {
		return Tools.wrapToLinesConsideringWS(s, 80);
	}

	/**
	 * Adds new-lines to a given string if it is longer than 80 characters, 
	 * and cuts the resulting text if it is longer than the given number of 
	 * lines. If some lines are cut, '....' is appended to the string to 
	 * indicate this fact.
	 * @param s The string to wrap and cut
	 * @param maxLines The max. number of lines the returned string may have
	 * @return The wrapped and cut string
	 */
	public static String wrapAndCutToLines(String s, int maxLines) {
		String str = Tools.wrapToLinesConsideringWS(s, 80);
		String list[] = str.split("\n", maxLines+1);
		if(list.length <= maxLines) {
			return str;
		} else { 
			String result = "";
			for(int i=0; i<maxLines-1; i++) {
				result += list[i] + "\n";
			}
			result += list[maxLines-1] + " ....";
			return result;
		}
	}

	/**
	 * Adds new-lines to a given string to wrap it after at most lineLength characters.
	 * <p>
	 * A new-line is added such that each line is no longer than lineLength characters. 
	 * The line breaks are added not considering white spaces.
	 * @param s The string to wrap.
	 * @param lineLength The maximum length of a line in characters.
	 * @return A wrapped string that contains no more than 80 characters per line.
	 */
	public static String wrapToLines(String s, int lineLength) {
		int max = s.length();
		int newLen = max + max / lineLength + 1;
		char[] list = new char[newLen]; // create a char-array that will store the new characters
		int offsetS = 0; // the offset into s
		int offsetL = 0; // the offset into the list
		int noNL = 0; // number of chars w/o newline
		for(; offsetS < max; ) {
			char tmp = s.charAt(offsetS++);
			if(noNL == lineLength) { // we have added line_length chars that were no new-lines. add a '\n'
				list[offsetL++] = '\n';
				noNL = 0;
			}
			list[offsetL++] = tmp; // copy the next character
			if(tmp == '\n') {
				noNL = 0;
			} else {
				noNL ++;
			}
		}
		return String.copyValueOf(list, 0, offsetL);
	}

	/**
	 * Adds new-lines to a given string to wrap it after at most lineLength characters.
	 * <p>
	 * A new-line is added such that each line is no longer than lineLength characters. 
	 * The line breaks are added considering white spaces. So line breaks are added either
	 * on the last space in the line or, if there is no space in the line, after the given
	 * number of characters.
	 * 
	 * @param s The string to wrap.
	 * @param lineLength The maximum length of a line in characters.
	 * @return A wrapped string that contains no more than 80 characters per line.
	 */	
	public static String wrapToLinesConsideringWS(String s, int lineLength){
		String rval = "";
		String currentString = s;
		while(currentString.length() > lineLength){
			// a line delimited by a new-line
			int firstNewLine = currentString.indexOf("\n");
			if(firstNewLine >= 0 && firstNewLine < lineLength){
				rval += currentString.substring(0, firstNewLine+1);
				currentString = currentString.substring(firstNewLine+1);
				continue;
			}
			// cut by spaces
			int lastSpaceInLine = currentString.lastIndexOf(" ", lineLength);
			if(lastSpaceInLine != -1){
				rval += currentString.substring(0, lastSpaceInLine)+"\n"; // replace the white-space with a newline
				currentString = currentString.substring(lastSpaceInLine+1);
				continue;
			}
			rval += currentString.substring(0, lineLength)+"\n";
			currentString = currentString.substring(lineLength);
		}
		if(currentString.length() != 0){
			rval += currentString;
		}
		return rval;
	}		

	
	//**************************************************************************************
	// Sinalgo System Operations
	//**************************************************************************************
	
	/**
	 * Print the current memory usage of Sinalgo to a stream.
	 * @param ps The stream to write to.
	 */
	public static void printSinalgoMemoryStats(PrintStream ps) {
		ps.print("\nSinalgo Memory Stats:\nRecycling:  (used / recycled)\n");
		ps.print("  Packets \t(" + Packet.numPacketsOnTheFly + " / " + Packet.getNumFreedPackets() + ")\n");  
		if(Global.isAsynchronousMode) {
			ps.print("  PacketEvents \t(" + PacketEvent.numPacketEventsOnTheFly + " / " + PacketEvent.getNumFreedPacketEvents() + ")\n"); 
			ps.print("  TimerEvents \t(" + TimerEvent.numTimerEventsOnTheFly + " / " + TimerEvent.getNumFreedTimerEvents() + ")\n");
		}
		ps.print("  Edges \t(" + Edge.numEdgesOnTheFly + " / " + EdgePool.getNumFreedEdges() + ")\n");
		
		ps.print("General Memory:\n");
		java.lang.Runtime r = java.lang.Runtime.getRuntime();
		int usedP = Math.round(100 * (r.totalMemory() - r.freeMemory()) / r.maxMemory());
		ps.print("  Used: " + usedP + "%\t" + ((r.totalMemory() - r.freeMemory()) / 1048576) + " MB\n");
		ps.print("  Free: " + (100-usedP) + "%\t" + (r.freeMemory()/1048576) + " MB\n");
		ps.print("  Total Alloc.:\t" + (r.totalMemory()/1048576) + " MB\n");
		ps.print("  Max:  \t" + (r.maxMemory()/1048576) + " MB\n");
	}
	
	/**
	 * Calls the garbage collector to remove unused objects.
	 * Note that the Java VM itself calls the GC whenever it is necessary. This
	 * method just provides an insight in the memory usage of SINALGO.
	 * The old and new memory usage is printed to the given stream.
	 * @param ps The stream to print the memory usage to.
	 */
	public static void runGC(PrintStream ps) {
		java.lang.Runtime r = java.lang.Runtime.getRuntime();
		long used = r.totalMemory() - r.freeMemory();
		long free = r.freeMemory();
		long total = r.totalMemory();
		long max = r.maxMemory();
		System.runFinalization();
		System.gc();
		ps.print("\nGarbage Collected (in MB):\n");
		int usedP = Math.round(100 * (r.totalMemory() - r.freeMemory()) / r.maxMemory());
		ps.print("  Used:  "+ usedP + "%\t" + (used / 1048576) + " -> " + ((r.totalMemory() - r.freeMemory())/1048576) + "\n");
		ps.print("  Free:  " + (100 - usedP) + "%\t" + (free/1048576) + " -> " + (r.freeMemory()/1048576) + "\n");
		ps.print("  Total Alloc.:\t" + (total/1048576) + " -> " + (r.totalMemory()/1048576) + "\n");
		ps.print("  Max:\t" + (max/1048576) + " -> " + (r.maxMemory()/1048576) + "\n");
	}

	/**
	 * Sinalgo recycles certain objects held back for reuse by Sinalgo 
	 * to reduce the load on the garbage collector. This method removes 
	 * all recycled objects that are not needed at the moment.  
	 * @param ps The print stream to print the result to, null for no output
	 */
	public static void disposeRecycledObjects(PrintStream ps) {
		EdgePool.clear();
		PacketEvent.clearUnusedPacketEvents();
		TimerEvent.clearUnusedTimerEvents();
		Packet.clearUnusedPackets();
		if(ps != null) {
			ps.print("\nCleared Recycled Objects.\n");
		}
	}
	
	//**************************************************************************************
	// Div. Helpers 
	//**************************************************************************************
	
	
	/**
	 * Round a given number to have only the given number of fractional (decimal) digits.
	 * @param d The number to round
	 * @param digits The max. number of fractional digits
	 * @return The rounded number
	 */
	public static double round(double d, int digits) {
		double pow = 1; // Math.pow(10, digits);
		for(; digits > 0; digits--) {
			pow *= 10;
		}
		return Math.round(d * pow) / pow;
	}
	
	/**
	 * Returns a random neighbor of a given node, null if the node has no neighbors.
	 * @param n
	 * @return A random neighbor of a given node, null if the node has no neighbors.
	 */
	public static Node getRandomNeighbor(Node n) {
		Random rand = Tools.getRandomNumberGenerator();
		int offset = rand.nextInt(n.outgoingConnections.size());
		for(Edge e : n.outgoingConnections) {
			if(offset == 0) {
				return e.endNode;
			}
			offset --;
		}
		return null;
	}
}
