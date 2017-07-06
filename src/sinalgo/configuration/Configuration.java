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
package sinalgo.configuration;


import java.awt.Color;
import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.naming.ConfigurationException;

import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.tools.statistics.Distribution;
import sinalgo.tools.storage.SortableVector;


/**
 * This class provides globally visible constants and access to the custom settings from the
 * configuration file. 
 * <p>
 * Most of these constants are project specific and are set through the configuration file
 * of the project when the framework loads the project. They are defined in the <i>framework</i> 
 * section of the configuration file. 
 * <p><b>You should not modify the members of this class directly. Instead change the corresponding 
 * values in the configuration file of your project.</b> The values in this class are only used 
 * as default values if there is no configuration file.
 * <p> 
 * This class also holds the project specific <i>custom</i> fields of the configuration file. They are stored 
 * in a lookup table. The key is the concatenation of the enclosing tag-names, separated by slashes.              
 */
public class Configuration {
	 
	/**
	 * The version of the release - this is needed especially for bug-tracking 
	 */
	public static final String versionString = "0.75.3"; // also change the value in the build.xml file
	
	/**
	 * The annotation to be used for fields that are included by default
	 * in the configuration file. 
	 * The description contains a brief description of this field.
	 */
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface DefaultInConfigFile {
		/** */
		String value();
	}
	
	/**
	 * The annotation to be used for fields that included optionally 
	 * in the configuration file. 
	 * The description contains a brief description of this field.
	 */
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface OptionalInConfigFile {
		/** */
		String value();
	}
	
	/**
	 * An annotation to be attached to the first member in a section
	 * such that other applications may order the annotated members
	 * of this class according to these section info.  
	 * The description contains the title of this section.
	 */
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface SectionInConfigFile {
		/** */
		String value();
	}
	
	/**
	 * Annotation used for private settings in this config file that
	 * are set through setters and getters (i.e. the edgeType)
	 */
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface PrivateInConfigFile {
		/** */
		String value();
	}
	
	
	/**
	 * Annotation used for settings in this config file that
	 * contain the name of an implementation. They are displayed
	 * as drop down choices in the project selector, containing
	 * the implementations in the given folder, e.g. 'nodes/edges'
	 * for the edges, or 'models/mobilityModels' for the mobility 
	 * models.
	 */
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ImplementationChoiceInConfigFile {
		/** */
		String value();
	}
	
	/**
	 * BEGIN of FRAMEWORK-BLOCK
	 * 
	 * ATTENTION: DO NOT CHANGE THE VALUES HERE. THESE ARE JUST THE INTERNAL INITIALISAZION VALUES
	 * IF YOU CHANGE THEM, THEY GET OVERWRITTEN BY THE VALUES IN THE SPECIFIED CONFIGURATION XML 
	 * FILE. 
	 * The project specific Config.xml is the place where you are supposed to change
	 * the values annotated with OverwriteInConfigFile.
	 */
	
	//-------------------------------------------------------------------------
	// Simulation Area
	//-------------------------------------------------------------------------
	@SectionInConfigFile("Simulation Area")
	@DefaultInConfigFile("Number of dimensions (2 for 2D, 3 for 3D)")
	public static int dimensions = 2;
	
	/** */
	@DefaultInConfigFile("Length of the deployment field along the x-axis.")
	public static int dimX = 500;
	
	/** */
	@DefaultInConfigFile("Length of the deployment field along the y-axis.")
	public static int dimY = 500;
	
	/** */
	@DefaultInConfigFile("Length of the deployment field along the z-axis.")
	public static int dimZ = 500;
	
	///** */
	//@OptionalInConfigFile("True if the simulation area is connected like a torus.")
	//public static boolean isTorus = false; // this is not yet implemented
	
	//-------------------------------------------------------------------------
	// Simulation
	//-------------------------------------------------------------------------
	

	/** */
	@SectionInConfigFile("Simulation")
	@DefaultInConfigFile("Switches between synchronous and asynchronous mode.")
	public static boolean asynchronousMode = false;

	/** */
	@DefaultInConfigFile("If set to true, the runtime obtains for each node a new\n" +
	                       "position from the mobility model at the beginning of each\n" +
	                       "round. This flag needs to be turned on if the chosen \n" +
	                       "mobility model of any node may change the node's position.\n" +
	                       "Set this flag to FALSE for static graphs whose nodes do never\n" +
	                       "change their position to increase performance.")
	public static boolean mobility = true;
	
	/** */
	@DefaultInConfigFile("If set to true, the chosen interference model is called at the\n" +
	                       "end of every round to test for interferring packets.\n" +
	                       "To increase performance, set this flag to FALSE if you do not\n" +
                         "consider interference.")
	public static boolean interference = true;
	
	/** */
	@DefaultInConfigFile("Set this flag to true if interference only decreases if\n" +
	                     "fewer messages are being sent and increases if more messages\n" +
	                     "are being sent.\n" +
	                     "If this flag is NOT set, interference for all messages currently\n" +
	                     "being sent is reevaluated whenever a new message is being sent, and\n" +
	                     "whenever a message stops being sent. When this flag is set,\n" +
	                     "interference tests are reduced to a minimum, using the additivity\n" +
	                     "property.\n" +
	                     "This flag only affects the asynchronous mode. In synchronous mode,\n" +
	                     "interference is checked exactly once for every message in every round.")
	public static boolean interferenceIsAdditive = true;
	
	/** */
	@DefaultInConfigFile("Set this flag to true if a node can receive messages while\n" +
	                     "it is sending messages itself, otherwise to false. This flag\n" +
	                     "is only relevant if interference is turned on, and it must be\n" +
	                     "handled properly in the used interference model.")
	public static boolean canReceiveWhileSending = true;
	
	/** */
	@DefaultInConfigFile("Set this flag to true if a node can receive multiple messages\n" +
	                     "in parallel, otherwise to false. When set to false, concurrent\n" +
	                     "packets are all dropped. This flag is only relevant if\n" +
	                     "interference is turned on, and it must be handled properly in\n" +
	                     "the used interference model.")
	public static boolean canReceiveMultiplePacketsInParallel = true;
	
	/**
	 * The type of the edge to be created in the edge factory.
	 * This field is private, but has a setter and getter method. 
	 */
	@ImplementationChoiceInConfigFile("nodes/edges")
	@PrivateInConfigFile("The type of the edges with which nodes are connected.")
	private static String edgeType = "Edge";
	
	/** */
	@DefaultInConfigFile("If set to true, the application exits as soon as the\n" +
	                       "termination criteria is met. This flag only affects\n" +
	                       "the GUI mode.")
	public static boolean exitOnTerminationInGUI = false; 
	
	/** */
	@DefaultInConfigFile("If set true, in asynchronous mode the connections are initialized\n" +
			"before the first event executes. Note that this flag is useless in synchronous mode\n" +
			"as the connections are updated in every step anyway.")
	public static boolean initializeConnectionsOnStartup = false;
	
	/** */
	@DefaultInConfigFile("Defines how often the GUI is updated. The GUI is\n" +
                       "redrawn after every refreshRate-th round.")
	public static int refreshRate = 1;
	
	/** */
	@DefaultInConfigFile("If set to true, the framework will inform a sender whenever \n" +
	                     "a unicast message is dropped. In synchronous mode, the sender \n" +
	                     "is informed in the round after the message should have arrived, and \n" +
	                     "immediately upon arrival in asynchronous mode.")
	public static boolean generateNAckMessages = false;
	
	/** */
	@DefaultInConfigFile("This flag only affects the asynchronous simulation mode. \n" +
	                     "When set to true, the framework calls handleEmptyEventQueue \n" +
	                     "on the project specific CustomGlobal whenever the event queue \n" +
	                     "becomes empty.")
	public static boolean handleEmptyEventQueue = true;

	/** */
	@DefaultInConfigFile("The java-command used to start the simulation process.\n" +
	                     "E.g. 'java', 'nice -n 19 java', 'time java'\n" +
	                     "This command should NOT contain the -Xmx flag, nor set\n" +
	                     "the classpath of java.")
	public static String javaCmd = "java";
	
	/** */
	@DefaultInConfigFile("Maximum memory the Java VM is allowed to use (in MB)")
	public static int javaVMmaxMem = 500;
	
	//-------------------------------------------------------------------------
	// Seed for random number generator
	//-------------------------------------------------------------------------

	/** */
	@SectionInConfigFile("Random number generators")
	@DefaultInConfigFile("If set to true, the random number generators of the\n" +
                       "framework use the same seed as in the previous run.")
  public static boolean useSameSeedAsInPreviousRun = false;

	@DefaultInConfigFile("If set to true, and useSameSeedAsInPreviousRun is set to false, \n" +
	                     "the random number generators of the\n" +
	                     "framework uses the specified fixed seed.")
	public static boolean useFixedSeed = false;

	/** */
	@DefaultInConfigFile("The seed to be used by the random number generators\n" +
		                   "if useFixedSeed is set to true.")
	public static long fixedSeed = 77654767;
	
	//-------------------------------------------------------------------------
	// Logging
	//-------------------------------------------------------------------------
	
	/** */
	@SectionInConfigFile("Logging")
	@DefaultInConfigFile("Name of the default log file, used by the system,\n" +
	                       "but also for use by the end-user. (This log file\n" +
	                       "is stored under sinalgo.runtime.Global.log.)")
	public static String logFileName = "logfile.txt";
	
	/** */
	@DefaultInConfigFile("Redirects the default log file to the console.\n" +
	                       "No logfile will be created if set to true.")
	public static boolean outputToConsole = true;
	
	/** */
	@DefaultInConfigFile("Indicates whether all log-files of the current simulation \n" +
	                       "are stored in a new directory. The name of the new directory\n" +
	                       "is given by the string-representation of the date\n" +
	                       "when the simulation starts.")
	public static boolean logToTimeDirectory = true;
	
	/** */
	@DefaultInConfigFile("If set to true, the system configuration is written to\n" +
	                       "the default log file after the application has been started.")
	public static boolean logConfiguration = true;
	
	/** */
	@DefaultInConfigFile("If set to true, the log files are flushed every time\n" +
	                       "a new log is added.")
	public static boolean eagerFlush = false;

	//-------------------------------------------------------------------------
	// GUI
	//-------------------------------------------------------------------------
	
	/** */
	@SectionInConfigFile("GUI")
	@DefaultInConfigFile("If true, the application shows an extended control panel.")
	public static boolean extendedControl = true;
	
	/** */
	@DefaultInConfigFile("If true, the graph edges are drawn as directed arrows,\n otherwise simple lines.")
	public static boolean drawArrows = false;
	
	/** */
	// @OverwriteInConfigFile("If true, draw ruler along the axes of the graph")
	public static boolean drawRulers = true;
	
	/** */
	@OptionalInConfigFile("Fraction of the old and new zoom values for a zoom step.")
	public static double zoomStep = 1.2;
	
	/** */
	@OptionalInConfigFile("Fraction of the old and new zoom values for a zoom \n" +
	                      "step when zooming with the mouse wheel.")
	public static double wheelZoomStep = 1.05;

	/** */
	@OptionalInConfigFile("The minimum required zoom")
	public static double minZoomFactor = 0.05;

	/** */
	@OptionalInConfigFile("If set to true, the nodes are ordered according to their \n" +
	                      "elevation before drawing, such that nodes closer to the \n" +
	                      "viewer are drawn on top. This setting only applies to 3D.")
	public static boolean draw3DGraphNodesInProperOrder = true;

	/** */
	@OptionalInConfigFile("If set to true and in 3D mode, the cube is drawn\n" +
	                      "with perspective.")
	public static boolean usePerspectiveView = true;
	
	/** */
	@OptionalInConfigFile("Factor that defines the distance of the observer from the cube\n" +
			                  "when useing the perspective view in 3D. Default: 30")
	public static int perspectiveViewDistance = 40;
	
  //-------------------------------------------------------------------------
	// Background Map for GUI 
	//-------------------------------------------------------------------------
	
	/** */
	@SectionInConfigFile("Background map in 2D")
	@DefaultInConfigFile("If set to true, the background of a 2D simulation is colored\n" +
	                       "according to a map, specified in a map-file, specified\n" +
	                       "by the field map")
	public static boolean useMap = false;

	/** */
	@DefaultInConfigFile("In 2D, the background can be colored depending on a map file.\n" +
	                        "This field contains the file name for this map, which is supposed\n" +
	                        "to be located in the source folder of the current project.\n" +
	                        "The map is only painted if useMap is set to true.")
	public static String map = "Map.mp";

	//-------------------------------------------------------------------------
	// The models that are selected by default.  
	//-------------------------------------------------------------------------
	@SectionInConfigFile("Models")
	@ImplementationChoiceInConfigFile("models/messageTransmissionModels")
	@OptionalInConfigFile("The message transmission model used when none is specified")
	public static String DefaultMessageTransmissionModel = "ConstantTime";

	/** */
	@ImplementationChoiceInConfigFile("models/connectivityModels")
	@OptionalInConfigFile("Default connectivity model used when none is specified")
	public static String DefaultConnectivityModel = "UDG";  

	/** */
	@ImplementationChoiceInConfigFile("models/distributionModels")
	@OptionalInConfigFile("Default distribution model used when none is specified")
	public static String DefaultDistributionModel = "Random";

	/** */
	@ImplementationChoiceInConfigFile("models/interferenceModels")
	@OptionalInConfigFile("Default interference model used when none is specified")
	public static String DefaultInterferenceModel = "NoInterference";

	/** */
	@ImplementationChoiceInConfigFile("models/mobilityModels")
	@OptionalInConfigFile("Default mobility model used when none is specified")
	public static String DefaultMobilityModel = "NoMobility";
	
	/** */
	@ImplementationChoiceInConfigFile("models/reliabilityModels")
	@OptionalInConfigFile("Default reliability model used when none is specified")
	public static String DefaultReliabilityModel = "ReliableDelivery";
	
	/** */
	@ImplementationChoiceInConfigFile("nodes/nodeImplementations")
	@OptionalInConfigFile("Default node implementation used when none is specified")
	public static String DefaultNodeImplementation = "DummyNode";
	
	/** */
	@OptionalInConfigFile("Show the models implemented by all projects in the drop\n" +
	                      "down options. When set to false, only the models by the\n" +
	                      "selected project and the default project are shown.")
	public static boolean showModelsOfAllProjects = false;

	//-------------------------------------------------------------------------
	// The default transformation and node collection implementations for the 2D / 3D case
	//-------------------------------------------------------------------------
	
	/** */
	@SectionInConfigFile("Node storage, position transformation")	
	@OptionalInConfigFile("Transformation implementation for 2D. (This is\n" +
	                      "used to translate between the logic positions used by\n" +
	                      "the simulation to the 2D coordinate system used by the\n" +
	                      "GUI to display the graph)")
	public static String guiPositionTransformation2D = "sinalgo.gui.transformation.Transformation2D";

	/** */
	@OptionalInConfigFile("Transformation implementation for 3D. (This is\n" +
	                      "used to translate between the logic positions used by\n" +
	                      "the simulation to the 2D coordinate system used by the\n" +
	                      "GUI to display the graph)")
	public static String guiPositionTransformation3D = "sinalgo.gui.transformation.Transformation3D";
	
	/** */
	@OptionalInConfigFile("Node collection implementation for 2D.") 
	public static String nodeCollection2D = "sinalgo.runtime.nodeCollection.Geometric2DNodeCollection";

	/** */
	@OptionalInConfigFile("Node collection implementation for 3D.") 
	public static String nodeCollection3D = "sinalgo.runtime.nodeCollection.Geometric3DNodeCollection";

	//-------------------------------------------------------------------------
	// Export Settings
	//-------------------------------------------------------------------------
	/** */
	@SectionInConfigFile("Export Settings")	
	@OptionalInConfigFile("EPS 2 PDF command:\n" + 
	                      "This is the command that is used to convert an EPS file \n" +
	                      "into a PDF file. You can use the following parameters:\n" + 
	                      "  %s is the complete path from the root folder of the\n" +
	                      "     framework to the SOURCE file (the eps)\n" + 
	                      "  %t is the complete path from the root folder of the\n" +
	                      "     framework to the TARGET file (the pdf)\n" + 
	                      "These placeholders are set by the framework.\n" + 
	                      "Example:\n" +
	                      "  'epstopdf %s')")
	public static String epsToPdfCommand = "epstopdf %s";
	
	/** */
	@OptionalInConfigFile("Enables the drawing of the bounding box of the deployment to EPS/PDF.")
	public static boolean epsDrawDeploymentAreaBoundingBox = true; 

	/** */
	@OptionalInConfigFile("Indicates whether the background in the ps should be\n " +
	                      "white or gray.\n " +
	                      "The gray version is easier to understand (especially in 3D)\n" +
	                      "but the white one should be more useful to be imported in reports." + "") 
	public static boolean epsDrawBackgroundWhite = true;

	//-------------------------------------------------------------------------
	// Diverse
	//-------------------------------------------------------------------------
	/** */
	@SectionInConfigFile("Animation Settings")	
	@OptionalInConfigFile("Draw an envelope for each message that is being sent")
	public static boolean showMessageAnimations = false;
	
	/** */
	@OptionalInConfigFile("Width of the envelope (when the message animation is enabled)")
	public static double messageAnimationEnvelopeWidth = 30;

	/** */
	@OptionalInConfigFile("Height of the envelope (when the message animation is enabled)")
	public static double messageAnimationEnvelopeHeight = 20;

	/** */
	@OptionalInConfigFile("Color of the envelope (when the message animation is enabled)")
	public static Color messageAnimationEnvelopeColor = Color.YELLOW;
	
	
	//-------------------------------------------------------------------------
	// Diverse
	//-------------------------------------------------------------------------
	/** */
	@SectionInConfigFile("Diverse Settings")	
	@OptionalInConfigFile("Show hints on how to further optimize the simulation when\n" +
	                      "some parameters seem not to be set optimally.")
	public static boolean showOptimizationHints = true;
	
	/** */
	@OptionalInConfigFile("Indicates whether the edges are drawn in the default\n" +
	                      "draw implementation for the graph.")
	public static boolean drawEdges = true;
	
	/** */
	@OptionalInConfigFile("Indicates whether the nodes are drawn in the default\n" +
	                      "draw implementation for the graph.")
	public static boolean drawNodes = true;
	
	/** */
	@OptionalInConfigFile("The number of future events that are shown in the control\n" +
			"panel")
	public static int shownEventQueueSize = 10;
	
	
	/** */
	@OptionalInConfigFile("Height of the output text field in pixels.")
	public static int outputTextFieldHeight = 200;
	
	/** */
	@OptionalInConfigFile("The length of the arrows. This length is multiplied by the current " +
	                      "zoomLevel.")
	public static int arrowLength = 8;

	/** */
	@OptionalInConfigFile("The width of the arrows. This width is multiplied by the current " +
	                      "zoomLevel.")
	public static int arrowWidth = 2;
	
	/** */
	@OptionalInConfigFile("The dsfault value of the rounds field.")
	public static int defaultRoundNumber = 1;

	
	/**
	 * Indicates whether the three plotted frame lines are also exported to the eps as a dotted line.
	 */
	//public static final boolean drawFrameDotted = false;
	
	

	
	

	
	
	
	
	
	
	
	/*************************************************************************************************************
	 * END of FRAMEWORK-BLOCK
	 *************************************************************************************************************/
	
	//-------------------------------------------------------------------------
	// Edge pool stuff
	//-------------------------------------------------------------------------
	
	/**
	 * A boolean indicating whether the type of the edges has changed. This is used that we can get rid
	 * of the reflection, if the same type of edges has to be created again.
	 * <p>
	 * Note that this member MUST NOT have any annotations! 
	 */
	private static boolean edgeTypeChanged = true;
	
	
	/**
	 * The short name of the edge (e.g. Edge, myProject:MyEdge) 
	 */
	private static String edgeTypeShortName = "";
	
	/**
	 * @return Whether the edge type has changed.
	 */
	public static boolean hasEdgeTypeChanged() {
		return edgeTypeChanged;
	}
	
	/**
	 * Sets the member edgeTypeChanged, which indicates whether the edge type
	 * has been changed, but the edge factory has not yet reacted to the change.
	 * @param changed 
	 */
	public static void setEdgeTypeChanged(boolean changed) {
		edgeTypeChanged = changed;
	}
	
	/**
	 * This method sets the type of the edge to create. It also sets the edgeTypeChanged flag.
	 *
	 * @param selectedType The name of the type to set the type to.
	 */
	public static void setEdgeType(String selectedType){
		edgeTypeShortName = selectedType;
		if(selectedType.compareTo("Edge") == 0) { // the default edge from the framework
			edgeType = "sinalgo.nodes.edges.Edge";
		}	
		else if(selectedType.compareTo("BidirectionalEdge") == 0) { // the bidirectional edge from the framework
			edgeType = "sinalgo.nodes.edges.BidirectionalEdge";
		}	
		else if(selectedType.contains(":")) { // edge specification of the form project:edgeName
			String[] modelParts = selectedType.split(":");
			edgeType = Configuration.userProjectsPath+"."+modelParts[0]+".nodes.edges."+modelParts[1];
		}	
		else if(!selectedType.contains(".")){ // just the name of an edge (without '.') -> from the default project
			edgeType = Configuration.defaultProjectPath+".nodes.edges."+selectedType;
		} 
		else { // the edge is given already in explicit form 
			edgeType = selectedType;
			edgeTypeShortName = Global.toShortName(edgeType);
		}
		edgeTypeChanged = true;
	}
	
	/**
	 * This method returns the current type of the edges.
	 *
	 * @return The type of the Edges.
	 */
	public static String getEdgeType(){
		return edgeType;
	}

	/**
	 * @return
	 */
	public static String getEdgeTypeShortName() {
		return edgeTypeShortName;
	}
	

	//-------------------------------------------------------------------------
	// Setters and getters for objects
	// There is an assign() and a getText() method that needs to be implemented
	// for each object (except the String)
	//-------------------------------------------------------------------------
	
	private static Object textToObject(Class<?> c, String text) throws ConfigurationException {
		if(c.equals(String.class)) {
			return text;
		}
		if(c.equals(java.awt.Color.class)) {
			// try if it's the name of a system color, e.g. yellow, red, black, ...
			try {
				Field f = Color.class.getDeclaredField(text.toLowerCase());
				return (Color) f.get(null);
			} catch(Throwable t) {
			}
			// otherwise, assume that its of the form r=RR,g=GG,b=BB 
			String[] list = text.split("[^0123456789]");
			String[] colors = new String[3];
			// move the non-empty entries to the beginning of the list
			int offset = 0;
			for(String s : list) {
				if(!s.equals("")) {
					colors[offset++] = s;
					if(offset > 2) {
						break;
					}
				}
			}
			if(offset < 3) {
				throw new ConfigurationException("Invalid color description (" + text + ") : The description is expected to" +
						  " be the name of a color (which is a member of java.awt.Color), or of the form" +
						  " r=255,g=255,b=0");
			}
			
			try {
				int r = Integer.parseInt(colors[0]);
				int g = Integer.parseInt(colors[1]);
				int b = Integer.parseInt(colors[2]);
				return new Color(r,g,b);
			} catch(NumberFormatException e) {
				throw new ConfigurationException("Invalid color description (" + text + ") : The description is expected to" +
						  " be the name of a color (which is a member of java.awt.Color), or of the form" +
						  " r=255,g=255,b=0");
			} catch(IllegalArgumentException e) {
				throw new ConfigurationException("Invalid color description (" + text + ") : The description is expected to" +
						  " be the name of a color (which is a member of java.awt.Color), or of the form" +
						  " r=255,g=255,b=0. The values for each color must be in the range 0..255.");
			}
		}
		
		throw new ConfigurationException("In order to use configuration entries of type " + c.getClass().getName() + 
				"\n" + "you need to implement the Configuration.assign(...) method for this class.");
	}
	
	/**
	 * This method may also be called with the primitive types
	 * @param o
	 * @return A textual representation for the given object
	 */
	public static String getConfigurationText(Object o) {
		if(o instanceof String) {
			return o.toString();
		}
		if(o instanceof java.awt.Color) {
			Color c = (Color) o;
			return "r=" + c.getRed() + ",g=" + c.getGreen() + ",b=" + c.getBlue(); 
		}
		return o.toString();
	}
	

	
	

	
	
	//-------------------------------------------------------------------------
	
	
	
	
	/**
	 * The hash map, where the parameters from the specified xml file are stored. They can be accessed by the
	 * get method of the HashMap. The key is the name together with all the nested tags from custom to the
	 * value itself. 
	 * For example, if you have the following XML entries: <br>
	 * &lt;UDG&gt;&lt;Sample&gt;&lt;Factor value=&quot;7&quot;&gt;&lt;/Factor&gt;&lt;/Sample&gt;&lt;/UDG&gt;
	 * <!-- This is the above text in plain: <UDG><Sample><Factor value="7"></Factor></Sample><UDG> --> <br>
	 * Then you can access it by calling 'String value = paramters.get("UDG/Sample/Factor");'. This will result
	 * in the String 'value' to be "7".
	*/ 
	private static HashMap<String, String> parameters = new HashMap<String, String>();
	
	
	/**
	 * Adds a property entry to the list of properties. 
	 * @param key The key of the property, which is converted to lower-case.
	 * @param property The value of the property 
	 */
	public static void putPropertyEntry(String key, String property) {
		parameters.put(key.toLowerCase(), property);
	}
	
	/**
	 * Retrieves an entry of the configuration file given the corresponding key.
	 * <p>
	 * The key is first converted to lower-case.
	 * @param key The key to the configuration file entry, which is case-insensitive.
	 * @return The configuration file entry corresponding to the entry.
	 * @throws CorruptConfigurationEntryException If no entry is associated with the key.
	 */
	public static String getStringParameter(String key) throws CorruptConfigurationEntryException {
		key = key.toLowerCase();
		if(parameters.containsKey(key)) {
			return parameters.get(key);
		} else {
			throw new CorruptConfigurationEntryException("Missing entry in the configuration file: An entry for the key '" 
			                                             + key + "' is missing in the config file of project '"+Global.projectName+"'.");
		}
	}
	
	
	/**
	 * Tests whether the configuration file had a custom entry with the given key.
	 * <p>
	 * Note: The method also returns true if the user has assigned a value to the key 
	 * using the overwrite flag.
	 * @param key The key of the entry, which is first converted to lower case.
	 * @return true if there is an entry for this key, otherwise false.
	 */
	public static boolean hasParameter(String key) {
		return parameters.containsKey(key.toLowerCase());
	}

	/**
	 * Retrieves an entry of the configuration file corresponding to the given key and converts it to
	 * a double value.
	 * @param key The key of the configuration entry, which is first converted to lower case.
	 * @return The entry in the configuration file corresponding to the key, converted to a double.
	 * @throws CorruptConfigurationEntryException If no entry is associated with the key or the entry cannot be converted to a double.
	 */
	public static double getDoubleParameter(String key) throws CorruptConfigurationEntryException {
		key = key.toLowerCase();
		if(parameters.containsKey(key)) {
			try {
				return Double.parseDouble(parameters.get(key));
			} catch(NumberFormatException e) {
				throw new CorruptConfigurationEntryException("The entry '" + key + "' in the configuration file cannot be converted to a double value."); 
			}
		} else {
			throw new CorruptConfigurationEntryException("Missing entry in the configuration file: An entry for the key '" 
					+ key + "' is missing.");
		}
	}
	
	/**
	 * Retrieves an entry of the configuration file corresponding to the given key and converts it to
	 * an integer value.
	 * @param key The key of the configuration entry, which is first converted to lower case.
	 * @return The entry in the configuration file corresponding to the key, converted to a integer.
	 * @throws CorruptConfigurationEntryException If no entry is associated with the key or the entry cannot be converted to a integer.
	 */
	public static int getIntegerParameter(String key) throws CorruptConfigurationEntryException {
		key = key.toLowerCase();
		if(parameters.containsKey(key)) {
			try {
				return Integer.parseInt(parameters.get(key));
			} catch(NumberFormatException e) {
				throw new CorruptConfigurationEntryException("The entry '" + key + "' in the configuration file cannot be converted to a integer value."); 
			}
		} else {
			throw new CorruptConfigurationEntryException("Missing entry in the configuration file: An entry for the key '" 
					+ key + "' is missing.");
		}
	}
	
	/**
	 * Retrieves an entry of the configuration file corresponding to the given key and converts it to
	 * an boolean value.
	 * @param key The key of the configuration entry, which is first converted to lower case.
	 * @return The entry in the configuration file corresponding to the key, converted to a boolean.
	 * @throws CorruptConfigurationEntryException If no entry is associated with the key.
	 */
	public static boolean getBooleanParameter(String key) throws CorruptConfigurationEntryException {
		key = key.toLowerCase();
		if(parameters.containsKey(key)) {
			return Boolean.parseBoolean(parameters.get(key));
		} else {
			throw new CorruptConfigurationEntryException("Missing entry in the configuration file: An entry for the key '" 
					+ key + "' is missing.");
		}
	}
	
	/**
	 * Retrieves an entry of the configuration file corresponding to the given key and converts it to
	 * a Color object. 
	 * @param key The key of the configuration entry, which is first converted to lower case.
	 * @return The entry in the configuration file corresponding to the key, converted to a Color object.
	 * @throws CorruptConfigurationEntryException If no entry is associated with the key or the entry
	 * is not a valid color name (a static Color member of the java.awt.Color class). 
	 */
	public static Color getColorParameter(String key) throws CorruptConfigurationEntryException {
		key = key.toLowerCase();
		if(!parameters.containsKey(key)) {
			throw new CorruptConfigurationEntryException("Missing entry in the configuration file: An entry for the key '" 
					+ key + "' is missing.");
		}
		String color = parameters.get(key);
		try {
			Field f = Color.class.getDeclaredField(color.toLowerCase());
			return (Color) f.get(null);
		} catch(Throwable t) {
			throw new CorruptConfigurationEntryException("Invalid color: '" + color + "' specified by the configuration entry" + key + 
			"\nValid color names are the static color members of the java.awt.Color class.");  
		}
	}
	
	
	/************************************************************************************************
	 * BEGIN of ADDITIONAL SETTINGS 
	 * 
	 * These settings affect the behavior of the simulation framework and 
	 * are not contained in the XML-configuration-file. If you want to change 
	 * these settings, you can do it directly in this source-file (or add them 
	 * yourself to the XML config file.)   
	 ************************************************************************************************/
	
	/**
	 * The directory where the logfiles are stored.
	 */
	public static final String logFileDirectory = "logs";
	
	/**
	 * The name of this application.
	 */
	public final static String appName = "SINALGO";
	
	/**
	 * The path where the default project is stored.
	 */
	public static final String defaultProjectPath = "projects.defaultProject";

	/**
	 * The directory where the default project is stored.
	 */
	public static final String defaultProjectDir = "projects/defaultProject";
	
	/**
	 * The path where user-specific projects are stored. This path has to be postfixed with
	 * the users project name.   
	 */
	public static final String userProjectsPath = "projects";
	/**
	 * The directory where user-specific projects are stored. This path has to be postfixed with
	 * the users project name.   
	 */
	public static final String userProjectDir = "projects";
	
	/**
	 * The directory where the source tree starts. 
	 */
	public static final String sourceDirPrefix = "src";
	
	/**
	 * The directory where the project tree starts in the source dir. 
	 */
	public static final String projectDirInSourceFolder = "projects";
	
	/**
	 * The name of the description file in the project folder.
	 */
	public static final String descriptionFileName = "description.txt";
	
	/**
	 * The name of the description file in the project folder.
	 */
	public static final String configfileFileName = "Config.xml";
	
	/**
	 * The directory where the class files are located. 
	 */
	public static final String binaryDir = "binaries/bin";
	
	/**
	 * The directory where the images are stored.
	 * Remember to use the ClassLoader.getResource() method to 
	 * map the file name to a url, such that the images can be
	 * accessed when they are stored in a jar file. 
	 */
	public static final String imageDir = "sinalgo/images/";
	
	/**
	 * A semicolon separated list of folder-names that should not
	 * be considered as user-projects
	 */
	public static final String nonUserProjectDirNames = "defaultProject;template;CVS";
	
	
	/**
	 * Assigns a value to the configuration file. This method should only be called
	 * during initialization of the framework.
	 * <p>
	 * This method terminates with a fatal error upon any failure.
	 * @param fieldName The name (case sensitive) of the field to be assigned
	 * @param value The value (in textual string format) to assign to the field
	 */
	public static void setFrameworkConfigurationEntry(String fieldName, String value) {
		if(fieldName.equals("edgeType")) { // special case for the 'edgeType'
			Configuration.setEdgeType(value);
		}	else {
			Field field = null;
			try{
				field = Configuration.class.getField(fieldName);

				if(!Modifier.isPublic(field.getModifiers())) {
					Main.fatalError("Error while parsing the configuration file: The entry '" + 
					                fieldName + "' in Configuration.java is not public.");
				}
				if(!Modifier.isStatic(field.getModifiers())) {
					Main.fatalError("Error while parsing the configuration file: The entry '" + 
					                fieldName +	"' in Configuration.java is not static.");
				}
				

				// Integer
				if(field.getType() == int.class){
					try {
						field.setInt(null, Integer.parseInt(value));
					} catch(NumberFormatException ex) {
						Main.fatalError("Error while parsing the specified parameters: Cannot convert '" + 
						                value+ "' to an integer value for the configuration entry '" + fieldName + "'."); 
					}
				}
				// Boolean
				else if(field.getType() == boolean.class){
					// Parse boolean manually, as Boolean.parseBoolean(String) converts anything not 'false' to true.
					if(value.compareTo("true") == 0){
						field.setBoolean(null, true);
					}	else if(value.compareTo("false") == 0){
						field.setBoolean(null, false);
					} else {
						Main.fatalError("Error while parsing the specified parameters: Cannot convert '" + value + 
						                "' to a boolean value for the configuration entry '" + fieldName + "'.");
					}
				}
				// Long
				else if(field.getType() == long.class) {
					try {
						field.setLong(null, Long.parseLong(value));
					} catch(NumberFormatException ex) {
						Main.fatalError("Error while parsing the specified parameters: Cannot convert '" + 
						                value+ "' to a long value for the configuration entry '" + fieldName + "'."); 
					}
				}
				//double
				else if(field.getType() == double.class){
					try {
						field.setDouble(null, Double.parseDouble(value));
					} catch(NumberFormatException ex) {
						Main.fatalError("Error while parsing the specified parameters: Cannot convert '" + 
						                value+ "' to a double value for the configuration entry '" + fieldName + "'."); 
					}
				}
				else { 
					try {
						field.set(null, textToObject(field.getType(), value));
					} catch(Exception e){
						Main.fatalError("Error while parsing the configuration file: Cannot set the field '" + 
								fieldName + "' of type '" + field.getType().getName() + "' to '" + 
								value + "'." + "\n\n" + e.getMessage());
					}
				}
			} catch (NumberFormatException e){
				Main.fatalError("Error while parsing the configuration file: Cannot set the field '" + 
				                fieldName + "' of type '" + field.getType().getName() + "' to '" + 
				                value + "'. Cannot convert the given value to the desired type:\n" + e);
			}	catch (SecurityException e) { 
				Main.fatalError("Error while parsing the configuration file: Cannot set the field '" + 
				                fieldName + "' to '" + value + "':\n" + e);
			} catch (NoSuchFieldException e) {
				Main.fatalError("Invalid configuration file: " + 
				                "The field '" + fieldName + "' is not a valid framework entry as it is not " +
				                "contained in Configuration.java. " +
				                "Check the spelling of this field or " + "move it to the custom entries.");
				
			} catch (IllegalArgumentException e) { 
				Main.fatalError("Error while parsing the configuration file: Cannot set the field '" + 
				                fieldName + "' to '" + value + "':\n" + e);
			}	catch (IllegalAccessException e) { 
				Main.fatalError("Error while parsing the configuration file: Cannot set the field '" + 
				                fieldName + "' to '" + value + "':\n" + e);
			}
		}
	}

	
	
	/**
	 * Prints the entire configuration of the framework, including the custom-fields of the
	 * xml configuration file to the given stream.
	 * @param ps A print-stream to print the configuration to
	 */
	public static void printConfiguration(PrintStream ps) {

		// Print the system environment settings
		ps.println("\n------------------------------------------------------\n" + 
		           "General Config\n" + 
		           "------------------------------------------------------"
		           );
		// Command Line Args
		ps.print("Command Line arguments: ");
		if(Main.cmdLineArgs != null) {
			for(String entry : Main.cmdLineArgs) {
				ps.print(entry + " ");
			}
		}
		ps.println();
		// The VM arguments
		RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
		List<String> list = bean.getInputArguments();
		ps.print("Java VM arguments: ");
		for(String entry : list) {
			ps.print(entry + " ");
		}
		ps.println();
		// other VM data
		ps.println("Class path: " + bean.getClassPath());
		// ps.println("Library path: " + bean.getLibraryPath());
		
		ps.println("------------------------------------------------------\n" + 
		           "Configuration settings\n" +
		           "------------------------------------------------------"
		           );
		Field[] fields = Configuration.class.getDeclaredFields();
		for(Field f : fields) {
			try {
				DefaultInConfigFile dan = f.getAnnotation(DefaultInConfigFile.class);
				OptionalInConfigFile oan = f.getAnnotation(OptionalInConfigFile.class);
				SectionInConfigFile san = f.getAnnotation(SectionInConfigFile.class);
				PrivateInConfigFile pan = f.getAnnotation(PrivateInConfigFile.class);
				if(dan != null || oan != null || pan != null) {
					if(san != null) { // print section
						ps.println(" " + san.value());
					}
					ps.println("    " + f.getName() + " = " + getConfigurationText(f.get(null)));
				} 
			} catch (IllegalArgumentException e) {
				ps.println(f.getName() + "      ERROR - CANNOT GET THE VALUE OF THIS FIELD.");
			} catch (IllegalAccessException e) {
				ps.println(f.getName() + "      ERROR - CANNOT GET THE VALUE OF THIS FIELD.");
			} catch (NullPointerException e) {
				ps.println(f.getName() + "      ERROR - CANNOT GET THE VALUE OF THIS FIELD. It is probably not static.");
			}
		}
		ps.println("------------------------------------------------------\n" +
		           "Custom settings\n" +
		           "------------------------------------------------------"
		);
		// sort the custom settings
		SortableVector<String> sv = new SortableVector<String>();
		for(Entry<String, String> e :parameters.entrySet()) {
			sv.add(" " + e.getKey() + " = " + e.getValue());
		}
		sv.sort(); // sorts ascending
		for(String s : sv) {
			ps.println(s);
		}
		
		ps.println("------------------------------------------------------\n" +
		           "Seed for Random Number Generators\n" +
		           "------------------------------------------------------"
		);

		if(Configuration.useSameSeedAsInPreviousRun) {
			ps.println(" The same seed as for the previous run: " + Distribution.getSeed());
		} else if(Configuration.useFixedSeed) {
			ps.println(" Fixed seed: " + Distribution.getSeed());
		} else {
			ps.println(" Randomly selected seed: " + Distribution.getSeed());
		}
		ps.println("------------------------------------------------------\n" +
		           "End of settings\n" +
		           "------------------------------------------------------\n"
		);
	}
}
