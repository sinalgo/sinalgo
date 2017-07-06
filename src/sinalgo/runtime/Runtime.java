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
package sinalgo.runtime;



import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Vector;

import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.gui.GUI;
import sinalgo.gui.GraphPanel;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.io.mapIO.Map;
import sinalgo.models.ConnectivityModel;
import sinalgo.models.DistributionModel;
import sinalgo.models.InterferenceModel;
import sinalgo.models.MobilityModel;
import sinalgo.models.Model;
import sinalgo.models.ModelType;
import sinalgo.models.ReliabilityModel;
import sinalgo.nodes.Node;
import sinalgo.nodes.edges.Edge;
import sinalgo.runtime.events.Event;
import sinalgo.runtime.events.EventQueue;
import sinalgo.runtime.nodeCollection.NodeCollectionInterface;
import sinalgo.runtime.packetsInTheAir.PacketsInTheAirBuffer;
import sinalgo.tools.Tools;
import sinalgo.tools.Tuple;


/**
 * This is the superclass of GuiRuntime and BatchRuntime. It is the core piece of the framework
 * and handles the simulation. It is extended by the gui- and the batch-runtime which handle the
 * simulation in the particular case.
 */
public abstract class Runtime{
	
	/**
	 * The collection of nodes stored in a slever way to fast retrieve the possible neighbors.
	 */
	public static NodeCollectionInterface nodes = createNewNodeCollection(); 
	
	/**
	 * The datastructure for all the messages, that are in the air in this moment. This is important 
	 * for the interference.
	 */
	public static PacketsInTheAirBuffer packetsInTheAir = new PacketsInTheAirBuffer();
	
	/**
	 * The global event queue that stores the events scheduled. This queue is always empty in the synchronous mode.
	 */
	public static EventQueue eventQueue = new EventQueue();
	
	/**
	 * The instance of the background map.
	 */
	public static Map map = null;
	
	protected static AppConfig appConfig = AppConfig.getAppConfig();
	
	//some information on the rounds
	
	//the number of rounds the simulation makes. It is used to store the number of rounds it makes
	//at the start of the simulation when a -rounds parameter is provided.
	protected int numberOfRounds = 0;
	
	public int getNumberOfRounds() {
		return numberOfRounds;
	}
	
	//true if a running simulation should be stopped at the end of the current round, otherwise false
	//set by the abort button in the gui
	protected boolean abort = false;
	
	//these are local variables to ensure the 'communication' between the parsing -gen parameters (where these variables are written
	//and the generation of the nodes.
	private int numNodes;
	private String nodeTypeName;
	private DistributionModel nodeDistribution;
	private int numSpecifiedModels;
	private Vector<Tuple<ModelType, Class<?>>> models;
	private String[] modelParams = new String[4];
	private String[] modelNames;
	
	protected boolean nodeCreationFinished = true;
	
	/**
	 * The constructor for the Runtime class. It initializes some basic variables. (like the map)
	 *
	 */
	public Runtime(){
		if(Configuration.useMap){
			try {
				map = new Map("src/"+Configuration.userProjectDir+"/"+Global.projectName+"/"+Configuration.map);
			} catch (FileNotFoundException e) {
				Main.fatalError(e);
			}
		}
	}
	
	/**
	 * This method is to perform a given number of simulation-rounds. 
	 * The method is overwritten in the concrete runtime system.
	 *
	 * @param rounds The number of rounds / events to simulate, may be 0.
	 * @param considerInfiniteRunFlag True, if the framework may run for infinitely many
	 * rounds / events, if the application is configured this way
	 */
	public abstract void run(long rounds, boolean considerInfiniteRunFlag);
	
	/**
	 * Called exactly once just before the <code>run</code> is called the first time.
	 */
	public void preRun() {
		// call the preRun() method of the CustomGlobal, if there was a project specified.
		Global.customGlobal.preRun();
	}
	
	
	// The transformation instance that knows how to translate between the
	// logic coordinate system used by the simulation and the corresponding
	// coordinates on the GUI.
	private PositionTransformation transformator = PositionTransformation.loadFieldTransformator();
	
	/**
	 * Returns the transformation instance that knows how to translate
	 * between the logic coordinate system used by the simulation and the
	 * corresponding coordinates on the GUI.
	 * @return The transformation instance.
	 */
	public PositionTransformation getTransformator() {
		return transformator;
	}

		
	/**
	 * This method aborts the simulation after the current round has terminated
	 */
	public void abort(){
		abort = true;
	}
	
	/**
	 * This method initializes the Runtime. It parses the input from the command line. This is 
	 * the part of the initialisazion which both runtime systems need.
	 *
	 * @param args The String Array the user passed on the command line.
	 * @throws CorruptConfigurationEntryException The exception, that is thrown if there is no 
	 * field in the configuration with the name you specified.
	 * @throws WrongConfigurationException Thrown if the user misconfigured the simulation somehow.
	 */
	public void initializeRuntimeSystem(String [] args) throws CorruptConfigurationEntryException, WrongConfigurationException{

		if(Configuration.mobility && Configuration.asynchronousMode){
			Main.fatalError("You tried to run the simulation in the asynchronous mode and mobility is turned on. " +
					"In the asynchronous mode mobility is not allowed.");
			Configuration.mobility = false;
		}
		
		int numberOfParameters = args.length;
		for(int i = 0; i < numberOfParameters; i++) { 

			if(args[i].equals("-rounds")){
				if(i+1 >= args.length) {
					Main.fatalError("Missing parameter: The command-line flag '-rounds' must " +
					"be followed by the number of rounds to execute."); 
				}
				try {
					numberOfRounds = Integer.parseInt(args[i+1]);
					i++; // don't have to look at args[i+1] anymore
				} catch(NumberFormatException e) {
					Main.fatalError("Cannot convert the number of rounds to execute (" + args[i+1] + ") " +
					                "to an integer: The '-rounds' flag must be followed by an integer value.\n " + e);
				}
			}
			
			else if(args[i].equals("-refreshRate")){
				if(i+1 >= args.length) {
					Main.fatalError("Missing parameter: The command-line flag '-refreshRate' must " +
					"be followed by the number of rounds between consecutive screen refreshs."); 
				}
				try{
					Configuration.refreshRate = Integer.parseInt(args[i+1]);
					i++; // don't have to look at args[i+1] anymore
				} catch(NumberFormatException e) {
					Main.fatalError("Cannot convert the refreshrate (" + args[i+1] + ") " +
					                "to an integer: The '-refreshRate' flag must be followed by an integer value.\n " + e);
				}
			}
			else if(args[i].equals("-gen")){
				nodeCreationFinished = false;
				
				// format: #nodes nodeType DistModel [(params)] [{M [(params)]}*]
				if(args.length <= i + 3) {
					Main.fatalError("Invalid parameters for the flag '-gen', which takes at least 3 parameters:\n" +
					                "-gen #nodes nodeType DistModel [(params)] [{M [(params)]}*]" +
					                "where each model appears at most once. (if you don't specify the model,\n" +
					                "the default model is taken. (The MessageTransmissionModel must not be used,\n" +
					                "it is set in the configuration file.)"
					);
				}
				
				numNodes = 0; 
				try {
					i++;
					numNodes = Integer.parseInt(args[i]); 
				} catch(NumberFormatException e) {
					Main.fatalError("Invalid parameters for the flag '-gen', which takes at least 3 parameters:\n" +
					                "-gen #nodes nodeType DistModel [(params)] [{M [(params)]}*]" +
					                "where each model appears at most once. (if you don't specify the model,\n" +
					                "the default model is taken. (The MessageTransmissionModel must not be used,\n" +
					                "it is set in the configuration file.)\n" +
					                "Reason: Cannot convert '" + args[i] + "' (which should correspond to the " +
					                "number of nodes) to an integer." 
					);
				}
				
				nodeTypeName = args[++i]; // note: pre-increments i
				String distributionModelName = args[++i]; // note: pre-increments i
				
				Tuple<String, Integer> optParam;
				optParam = getOptionalParameters(args, i+1); 
				String distributionModelParameters = optParam.first;
				i = optParam.second.intValue(); // i now points to the next arg to consider
				
				modelNames = new String[4]; // the optional models (the params are stored at same offset in modelParams)
				modelParams = new String[4]; // the optional parameter-strings to the optoinal models.
				numSpecifiedModels = 0;
				
				while(i < args.length) {
					if(args[i].startsWith("-")) {
						break; // there are no further models or optional parameters for the models.
					}
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
					modelNames[numSpecifiedModels] = args[i++]; // note: post-incremented i
					optParam = getOptionalParameters(args, i); //
					modelParams[numSpecifiedModels] = optParam.first;
					i = optParam.second.intValue(); // i now points to the next arg to consider
					numSpecifiedModels ++;
				}

				// initialize the distribution model
				nodeDistribution = Model.getDistributionModelInstance(distributionModelName);
				nodeDistribution.setParamString(distributionModelParameters);
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
				
				this.initProgress();

				try{
					synchronized(this){
						if(!nodeCreationFinished){
							//wait for the end of the initialisazion progress
							wait();
						}
					}
				}
				catch(InterruptedException e){
					Main.fatalError(e);
				}
				
				i--; // point to last processed entry, for-loop increments i afterwards
				
			}
			else if(args[i].equals("-project")){
				// omitting -project as is was already used in the main class.
			}
			else if(args[i].equals("-gui")){
				// omitting -gui as is was already used in the main class.
			}
			else if(args[i].equals("-batch")){
				// omitting -batch as is was already used in the main class.
			}
			else if(args[i].equals("-overwrite")){
				// omitting -overwrite as is was already used in the main class.
			}
			else if(args[i].startsWith("-")){
				Main.fatalError("Unknown modifier " + args[i]);
			}
		}
		
		initConcreteRuntime();
	}
	
	/**
	 * Scans the command-line arguments for optional-parameters which have to be 
	 * enclosed in parantheses '(', ')'. If no argument ends with a ')', the application exits with
	 * an error warning.
	 * 
	 * @param args The argument list of the command line
	 * @param i The offset into args from where the optional parameter may start. (i may be >= args.length) 
	 * @return A tuple containing the optional parameter and the new offset of i such that i points
	 * to the next entry in args after the optional parameter (which equals the initial value of i
	 * if there was no optional parameter).
	 */
	private Tuple<String, Integer> getOptionalParameters(String[] args, int i) {
		if(i >= args.length) {
			// no args left
			return new Tuple<String,Integer>("", new Integer(i));
		}
		if(!args[i].startsWith("(")) {
			// does not start with a '(' => no optional parameter.
			return new Tuple<String,Integer>("", new Integer(i));
		}
		String result = "";
		boolean found = true; // is the openening-paranthese closed?
		while(i < args.length) {
			result += " " + args[i]; // adds one initial space to the result string (and keeps the '(')
			i++;
			if(result.endsWith(")")) {
				found = true;
				break; // found the last entry
			}
		}
		if(!found) {
			Main.fatalError("Invalid optoinal parameters on the command-line: The optional \n" +
			                "parameter is supposed to be terminated with a ')': " + result);
		}
		result = result.substring(2, result.length()-1); // cut off the initial ' (' and the trailing ')'
		return new Tuple<String,Integer>(result, new Integer(i)); 
	}
	
	/**
	 * This method initializes the concrete runtime. I.e. the Gui- or the batchruntime.
	 */
	public abstract void initConcreteRuntime();
	
	/**
	 * This method adds a node to the collection of nodes.
	 *
	 * @param n The node to add.
	 */
	public static void addNode(Node n){
		nodes.addNode(n);
		Global.customGlobal.nodeAddedEvent(n);
		if(Global.isGuiMode){
			try {
				GraphPanel gp = Main.getGuiRuntime().getGUI().getGraphPanel();
				if(gp != null){
					//force the graph panel to redraw on next paint (do not call paintNow, because this draws the panel once more
					gp.forceDrawInNextPaint();
				}
			} catch (NotInGUIModeException e) {
				Main.fatalError(e);
			}
		}
	}
	
	/**
	 * This method removes a node from the collection and removes
	 * all edges incident to this node. This method is called by the 
	 * popup method from the node.
	 * <p>This method is quite expensive, as it loops over all nodes
	 * in the network to test for nodes that may maintain a connection
	 * to this node. 
	 *
	 * @param n The node to remove.
	 */
	public static void removeNode(Node n){
		// remove the outgoing connections from neighbor to this nodes
		// TODO: This is really expensive! Can't we have a better data structure?
		//       The problem so far is the 'getRandomNode' method
		Enumeration<Node> nodeEnumer = nodes.getNodeEnumeration();
		while(nodeEnumer.hasMoreElements()){
			Node node = nodeEnumer.nextElement();
			Edge e = node.outgoingConnections.remove(node, n); // does only remove it it really exists
			if(e != null) {
				e.free();
			}
		}
		n.outgoingConnections.removeAndFreeAllEdges();
		
		nodes.removeNode(n);
		eventQueue.removeAllEventsForThisNode(n); 
		if(Global.isGuiMode){
			// un highlight this node
			Tools.getGUI().getGraphPanel().setNodeHighlighted(n, false);
		}
		Global.customGlobal.nodeRemovedEvent(n);
	}
	
	/**
	 * Removes the given edge from the system. This method is not used by the 
	 * connectivity model but by the event handler of the popupMenus in the GUI.
	 * <p>
	 * It invalidates all messages being sent over this edge. 
	 * 
	 * @param edge The edge to remove.
	 */
	public static void removeEdge(Edge edge){
		edge.removeEdgeFromGraph(); // invalidate, does not free the edge
		edge.startNode.outgoingConnections.remove(edge.startNode, edge.endNode); // remove the edge from the list of outgoing connections from this node
		edge.free();
	}
	
	/**
	 * In asynchronous mode, removes all events from the event queue
	 */
	public static void removeAllAsynchronousEvents() {
		eventQueue.dropAllEvents();
	}
	
	/**
	 * Removes a given event from the event queue
	 * @param e The event to remove
	 */
	public static void removeEvent(Event e) {
		eventQueue.dropEvent(e);
	}
	
	/**
	 * Creates a new node collection object. 
	 * Before replacing an existing node collection by a new one, you 
	 * should ensure that the existing node collection does not contain 
	 * any nodes anymore, such that they are recycled correctly.   
	 */
	private static NodeCollectionInterface createNewNodeCollection() {
		// load the node collection specified in the config file
		NodeCollectionInterface result = null;
		String name = null;
		if(Configuration.dimensions == 2){
			name = Configuration.nodeCollection2D;	
		} else if(Configuration.dimensions == 3) { 
			name = Configuration.nodeCollection3D;	
		} else {
			Main.fatalError("The 'dimensions' field in the configuration file is invalid. Valid values are either '2' for 2D or '3' for 3D. (Cannot create corresponding node collection.)");
		}
		try {
			Class<?> c = Class.forName(name);
			Constructor<?> cons = c.getConstructor();
			result = (NodeCollectionInterface) cons.newInstance();
		} catch (ClassNotFoundException e) {
			Main.fatalError("Cannot find the class " + name + " which contains the implementation for the node collection. Please check the nodeCollection field in the config file.");
		} catch (SecurityException e) {
			Main.fatalError("Cannot generate the node collection object due to a security exception:\n\n" + e.getMessage());
		} catch (NoSuchMethodException e) {
			Main.fatalError("The node collection " + name + " must provide a constructor taking no arguments.\n\n" + e.getMessage());
		} catch (IllegalArgumentException e) {
			Main.fatalError("The node collection " + name + " must provide a constructor taking no arguments.\n\n" + e.getMessage());
		} catch (InstantiationException e) {
			Main.fatalError("Classes usable as node collections must be instantiable classes, i.e. no interfaces and not abstract.\n\n" + e.getMessage()); 
		} catch (IllegalAccessException e) {
			Main.fatalError("Cannot generate the node collection object due to illegal access:\n\n" + e.getMessage());
		} catch (InvocationTargetException e) {
			Main.fatalError("Exception while instanciating " + name + ":\n\n" + e.getCause().getMessage());
		}
		return result;
	}
	
	/**
	 * Reevaluates all the connections (edges) between all the nodes currently in the graph.
	 */
	public static void reevaluateConnections(){
		for(Node n: nodes){
			n.getConnectivityModel().updateConnections(n);
		}
	}
	
	/**
	 * Removes all nodes, edges, messages and events from the simulation to 
	 * start over. 
	 * <p> 
	 * This method may be used to empty the node collection. 
	 */
	public static void clearAllNodes() {
		eventQueue.pruneAllNodeEvents();

		packetsInTheAir = new PacketsInTheAirBuffer();
		for(Node n : nodes) {
			n.outgoingConnections.removeAndFreeAllEdges();
		}
		nodes = createNewNodeCollection();
		Node.resetIDCounter(); // new nodes restart their ID with 1
		if(Global.isGuiMode){
			GUI gui = Tools.getGUI();
			gui.allNodesAreRemoved();
			gui.redrawGUINow();		
		}
	}
		
	/**
	 * This method initializes the progress bar.
	 */
	public abstract void initProgress();
	
	/**
	 * This method sets the progress of the initialisation. Depending whether the simulation is
	 * run in batch or in gui mode it is handled differently.
	 * 
	 * @param percent The percentage of the progress of initialisation.
	 */
	public abstract void setProgress(double percent);
	
	/**
	 * This method creates the nodes specified after the -gen parameter. I is called by the ProgressBar and
	 * it sets its percentage.
	 */
	public synchronized void createNodes() {
		// Create the nodes
		for(int j=0; j< numNodes; j++) {
			
			this.setProgress(100.0d* j/ numNodes);
			
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
		//the system nodes are initialized and thus the waiting thread (the main-Thread) can be invoked again. (If it is still waiting
		nodeCreationFinished = true;
		
		this.notifyAll();
	}
}