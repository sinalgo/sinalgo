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
package sinalgo.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.tools.statistics.Distribution;
import sinalgo.tools.storage.SortableVector;

import javax.naming.ConfigurationException;
import java.awt.*;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import static sinalgo.configuration.Configuration.ImplementationChoiceInConfigFile.ImplementationType.*;

/**
 * This class provides globally visible constants and access to the custom
 * settings from the configuration file.
 * <p>
 * Most of these constants are project specific and are set through the
 * configuration file of the project when the framework loads the project. They
 * are defined in the <i>framework</i> section of the configuration file.
 *
 * <b>You should not modify the members of this class directly. Instead change
 * the corresponding values in the configuration file of your project.</b> The
 * values in this class are only used as default values if there is no
 * configuration file.
 * <p>
 * This class also holds the project specific <i>custom</i> fields of the
 * configuration file. They are stored in a lookup table. The key is the
 * concatenation of the enclosing tag-names, separated by slashes.
 */
public class Configuration {

    /**
     * The version of the release - this is needed especially for bug-tracking
     */
    public static final String VERSION_STRING = getVersionString();

    private static String getVersionString() {
        try {
            return new Scanner(Thread.currentThread().getContextClassLoader().getResourceAsStream("VERSION"),
                    StandardCharsets.UTF_8.displayName()).useDelimiter("\\A").next().trim();
        } catch (Exception e) {
            throw new SinalgoFatalException("Could not read version information from the VERSION file.\n\n" + e);
        }
    }

    /**
     * The repository which contains the latest code changes for Sinalgo
     */
    public static final String SINALGO_REPO = getRepoString();

    private static String getRepoString() {
        try {
            return new Scanner(Thread.currentThread().getContextClassLoader().getResourceAsStream("WEB_REPOSITORY_URL"),
                    StandardCharsets.UTF_8.displayName()).useDelimiter("\\A").next().trim();
        } catch (Exception e) {
            throw new SinalgoFatalException("Could not read version information from the WEB_REPOSITORY_URL file.\n\n" + e);
        }
    }

    /**
     * The webpage that contains Sinalgo's tutorial and help files
     */
    public static final String SINALGO_WEB_PAGE = getWebPageString();

    private static String getWebPageString() {
        try {
            return new Scanner(Thread.currentThread().getContextClassLoader().getResourceAsStream("WEB_PAGE_URL"),
                    StandardCharsets.UTF_8.displayName()).useDelimiter("\\A").next().trim();
        } catch (Exception e) {
            throw new SinalgoFatalException("Could not read version information from the WEB_PAGE_URL file.\n\n" + e);
        }
    }

    /**
     * The annotation to be used for fields that are included by default in the
     * configuration file. The description contains a brief description of this
     * field.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DefaultInConfigFile {

        /** */
        String value();
    }

    /**
     * The annotation to be used for fields that included optionally in the
     * configuration file. The description contains a brief description of this
     * field.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OptionalInConfigFile {

        /** */
        String value();
    }

    /**
     * An annotation to be attached to the first member in a section such that other
     * applications may order the annotated members of this class according to these
     * section info. The description contains the title of this section.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SectionInConfigFile {

        /** */
        String value();
    }

    /**
     * Annotation used for private settings in this config file that are set through
     * setters and getters (i.e. the edgeType)
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PrivateInConfigFile {

        /** */
        String value();
    }

    /**
     * Annotation used for settings in this config file that contain the name of an
     * implementation. They are displayed as drop down choices in the project
     * selector, containing the implementations in the given package, e.g.
     * 'nodes.edges' for the edges, or 'models.mobilityModels' for the mobility
     * models.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ImplementationChoiceInConfigFile {

        enum ImplementationType {

            NODES_EDGES("nodes.edges"),
            NODES_IMPLEMENTATIONS("nodes.nodeImplementations"),
            MODELS_MESSAGE_TRANSMISSION("models.messageTransmissionModels"),
            MODELS_CONNECTIVITY("models.connectivityModels"),
            MODELS_DISTRIBUTION("models.distributionModels"),
            MODELS_INTERFERENCE("models.interferenceModels"),
            MODELS_MOBILITY("models.mobilityModels"),
            MODELS_RELIABILITY("models.reliabilityModels");

            private String pkg;

            ImplementationType(String pkg) {
                this.pkg = pkg;
            }

            /**
             * @return The directory used for this implementation type
             */
            public String getPkg() {
                return this.pkg;
            }
        }

        /** */
        ImplementationType value();
    }

    /**
     * BEGIN of FRAMEWORK-BLOCK
     * <p>
     * ATTENTION: DO NOT CHANGE THE VALUES HERE. THESE ARE JUST THE INTERNAL
     * INITIALISAZION VALUES IF YOU CHANGE THEM, THEY GET OVERWRITTEN BY THE VALUES
     * IN THE SPECIFIED CONFIGURATION XML FILE. The project specific Config.xml is
     * the place where you are supposed to change the values annotated with
     * OverwriteInConfigFile.
     */

    // -------------------------------------------------------------------------
    // Simulation Area
    // -------------------------------------------------------------------------
    @Getter
    @Setter
    @SectionInConfigFile("Simulation Area")
    @DefaultInConfigFile("Number of dimensions (2 for 2D, 3 for 3D)")
    private static int dimensions = 2;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("Length of the deployment field along the x-axis.")
    private static int dimX = 500;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("Length of the deployment field along the y-axis.")
    private static int dimY = 500;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("Length of the deployment field along the z-axis.")
    private static int dimZ = 500;

    /// ** */
    // @OptionalInConfigFile("True if the simulation area is connected like a
    /// torus.")
    // public static boolean isTorus = false; // this is not yet implemented

    // -------------------------------------------------------------------------
    // Simulation
    // -------------------------------------------------------------------------

    /** */
    @Getter
    @Setter
    @SectionInConfigFile("Simulation")
    @DefaultInConfigFile("Switches between synchronous and asynchronous mode.")
    private static boolean asynchronousMode;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("If set to true, the runtime obtains for each node a new\n"
            + "position from the mobility model at the beginning of each\n"
            + "round. This flag needs to be turned on if the chosen \n"
            + "mobility model of any node may change the node's position.\n"
            + "Set this flag to FALSE for static graphs whose nodes do never\n"
            + "change their position to increase performance.")
    private static boolean mobility = true;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("If set to true, the chosen interference model is called at the\n"
            + "end of every round to test for interferring packets.\n"
            + "To increase performance, set this flag to FALSE if you do not\n" + "consider interference.")
    private static boolean interference = true;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("Set this flag to true if interference only decreases if\n"
            + "fewer messages are being sent and increases if more messages\n" + "are being sent.\n"
            + "If this flag is NOT set, interference for all messages currently\n"
            + "being sent is reevaluated whenever a new message is being sent, and\n"
            + "whenever a message stops being sent. When this flag is set,\n"
            + "interference tests are reduced to a minimum, using the additivity\n" + "property.\n"
            + "This flag only affects the asynchronous mode. In synchronous mode,\n"
            + "interference is checked exactly once for every message in every round.")
    private static boolean interferenceIsAdditive = true;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("Set this flag to true if a node can receive messages while\n"
            + "it is sending messages itself, otherwise to false. This flag\n"
            + "is only relevant if interference is turned on, and it must be\n"
            + "handled properly in the used interference model.")
    private static boolean canReceiveWhileSending = true;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("Set this flag to true if a node can receive multiple messages\n"
            + "in parallel, otherwise to false. When set to false, concurrent\n"
            + "packets are all dropped. This flag is only relevant if\n"
            + "interference is turned on, and it must be handled properly in\n" + "the used interference model.")
    private static boolean canReceiveMultiplePacketsInParallel = true;

    /**
     * The type of the edge to be created in the edge factory. This field is
     * private, but has a setter and getter method.
     *
     * @return The type of the Edges
     */
    @Getter
    @ImplementationChoiceInConfigFile(NODES_EDGES)
    @PrivateInConfigFile("The type of the edges with which nodes are connected.")
    private static String edgeType = "Edge";

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("If set to true, the application exits as soon as the\n"
            + "termination criteria is met. This flag only affects\n" + "the GUI mode.")
    private static boolean exitOnTerminationInGUI;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("If set true, in asynchronous mode the connections are initialized\n"
            + "before the first event executes. Note that this flag is useless in synchronous mode\n"
            + "as the connections are updated in every step anyway.")
    private static boolean initializeConnectionsOnStartup;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("Defines how often the GUI is updated. The GUI is\n"
            + "redrawn after every refreshRate-th round.")
    private static int refreshRate = 1;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("If set to true, the framework will inform a sender whenever \n"
            + "a unicast message is dropped. In synchronous mode, the sender \n"
            + "is informed in the round after the message should have arrived, and \n"
            + "immediately upon arrival in asynchronous mode.")
    private static boolean generateNAckMessages;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("This flag only affects the asynchronous simulation mode. \n"
            + "When set to true, the framework calls handleEmptyEventQueue \n"
            + "on the project specific CustomGlobal whenever the event queue \n" + "becomes empty.")
    private static boolean handleEmptyEventQueue = true;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("The java-command used to start the simulation process.\n"
            + "E.g. 'java', 'nice -n 19 java', 'time java'\n"
            + "This command should NOT contain the -Xmx flag, nor set\n" + "the classpath of java.")
    private static String javaCmd = "java";

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("Maximum memory the Java VM is allowed to use (in MB)")
    private static int javaVMmaxMem = 500;

    // -------------------------------------------------------------------------
    // Seed for random number generator
    // -------------------------------------------------------------------------

    /** */
    @Getter
    @Setter
    @SectionInConfigFile("Random number generators")
    @DefaultInConfigFile("If set to true, the random number generators of the\n"
            + "framework use the same seed as in the previous run.")
    private static boolean useSameSeedAsInPreviousRun;

    @Getter
    @Setter
    @DefaultInConfigFile("If set to true, and useSameSeedAsInPreviousRun is set to false, \n"
            + "the random number generators of the\n" + "framework uses the specified fixed seed.")
    private static boolean useFixedSeed;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("The seed to be used by the random number generators\n" + "if useFixedSeed is set to true.")
    private static long fixedSeed = 77654767;

    // -------------------------------------------------------------------------
    // Logging
    // -------------------------------------------------------------------------

    /** */
    @Getter
    @Setter
    @SectionInConfigFile("Logging")
    @DefaultInConfigFile("Name of the default log file, used by the system,\n"
            + "but also for use by the end-user. (This log file\n" + "is stored under sinalgo.runtime.Global.log.)")
    private static String logFileName = "logfile.txt";

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("Redirects the default log file to the console.\n"
            + "No logfile will be created if set to true.")
    private static boolean outputToConsole = true;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("Indicates whether all log-files of the current simulation \n"
            + "are stored in a new directory. The name of the new directory\n"
            + "is given by the string-representation of the date\n" + "when the simulation starts.")
    private static boolean logToTimeDirectory = true;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("If set to true, the system configuration is written to\n"
            + "the default log file after the application has been started.")
    private static boolean logConfiguration = true;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("If set to true, the log files are flushed every time\n" + "a new log is added.")
    private static boolean eagerFlush;

    // -------------------------------------------------------------------------
    // GUI
    // -------------------------------------------------------------------------

    /** */
    @Getter
    @Setter
    @SectionInConfigFile("GUI")
    @DefaultInConfigFile("If true, the application shows an extended control panel.")
    private static boolean extendedControl = true;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("If true, the graph edges are drawn as directed arrows,\n otherwise simple lines.")
    private static boolean drawArrows;

    /** */
    @Getter
    @Setter
    // @OverwriteInConfigFile("If true, draw ruler along the axes of the graph")
    private static boolean drawRulers = true;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("Fraction of the old and new zoom values for a zoom step.")
    private static double zoomStep = 1.2;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("Fraction of the old and new zoom values for a zoom \n"
            + "step when zooming with the mouse wheel.")
    private static double wheelZoomStep = 1.05;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("The minimum required zoom")
    private static double minZoomFactor = 0.05;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("If set to true, the nodes are ordered according to their \n"
            + "elevation before drawing, such that nodes closer to the \n"
            + "viewer are drawn on top. This setting only applies to 3D.")
    private static boolean draw3DGraphNodesInProperOrder = true;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("If set to true and in 3D mode, the cube is drawn\n" + "with perspective.")
    private static boolean usePerspectiveView = true;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("Factor that defines the distance of the observer from the cube\n"
            + "when useing the perspective view in 3D. Default: 30")
    private static int perspectiveViewDistance = 40;

    // -------------------------------------------------------------------------
    // Background Map for GUI
    // -------------------------------------------------------------------------

    /** */
    @Getter
    @Setter
    @SectionInConfigFile("Background map in 2D")
    @DefaultInConfigFile("If set to true, the background of a 2D simulation is colored\n"
            + "according to a map, specified in a map-file, specified\n" + "by the field map")
    private static boolean useMap;

    /** */
    @Getter
    @Setter
    @DefaultInConfigFile("In 2D, the background can be colored depending on a map file.\n"
            + "This field contains the file name for this map, which is supposed\n"
            + "to be located in the resource folder of the current project.\n"
            + "The map is only painted if useMap is set to true.")
    private static String map = "Map.mp";

    // -------------------------------------------------------------------------
    // The models that are selected by default.
    // -------------------------------------------------------------------------
    @Getter
    @Setter
    @SectionInConfigFile("Models")
    @ImplementationChoiceInConfigFile(MODELS_MESSAGE_TRANSMISSION)
    @OptionalInConfigFile("The message transmission model used when none is specified")
    private static String defaultMessageTransmissionModel = "ConstantTime";

    /** */
    @Getter
    @Setter
    @ImplementationChoiceInConfigFile(MODELS_CONNECTIVITY)
    @OptionalInConfigFile("Default connectivity model used when none is specified")
    private static String defaultConnectivityModel = "UDG";

    /** */
    @Getter
    @Setter
    @ImplementationChoiceInConfigFile(MODELS_DISTRIBUTION)
    @OptionalInConfigFile("Default distribution model used when none is specified")
    private static String defaultDistributionModel = "Random";

    /** */
    @Getter
    @Setter
    @ImplementationChoiceInConfigFile(MODELS_INTERFERENCE)
    @OptionalInConfigFile("Default interference model used when none is specified")
    private static String defaultInterferenceModel = "NoInterference";

    /** */
    @Getter
    @Setter
    @ImplementationChoiceInConfigFile(MODELS_MOBILITY)
    @OptionalInConfigFile("Default mobility model used when none is specified")
    private static String defaultMobilityModel = "NoMobility";

    /** */
    @Getter
    @Setter
    @ImplementationChoiceInConfigFile(MODELS_RELIABILITY)
    @OptionalInConfigFile("Default reliability model used when none is specified")
    private static String defaultReliabilityModel = "ReliableDelivery";

    /** */
    @Getter
    @Setter
    @ImplementationChoiceInConfigFile(NODES_IMPLEMENTATIONS)
    @OptionalInConfigFile("Default node implementation used when none is specified")
    private static String defaultNodeImplementation = "DummyNode";

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("Show the models implemented by all projects in the drop\n"
            + "down options. When set to false, only the models by the\n"
            + "selected project and the default project are shown.")
    private static boolean showModelsOfAllProjects;

    // -------------------------------------------------------------------------
    // The default transformation and node collection implementations for the 2D /
    // 3D case
    // -------------------------------------------------------------------------

    /** */
    @Getter
    @Setter
    @SectionInConfigFile("Node storage, position transformation")
    @OptionalInConfigFile("Transformation implementation for 2D. (This is\n"
            + "used to translate between the logic positions used by\n"
            + "the simulation to the 2D coordinate system used by the\n" + "GUI to display the graph)")
    private static String guiPositionTransformation2D = "sinalgo.gui.transformation.Transformation2D";

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("Transformation implementation for 3D. (This is\n"
            + "used to translate between the logic positions used by\n"
            + "the simulation to the 2D coordinate system used by the\n" + "GUI to display the graph)")
    private static String guiPositionTransformation3D = "sinalgo.gui.transformation.Transformation3D";

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("Node collection implementation for 2D.")
    private static String nodeCollection2D = "sinalgo.runtime.nodeCollection.Geometric2DNodeCollection";

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("Node collection implementation for 3D.")
    private static String nodeCollection3D = "sinalgo.runtime.nodeCollection.Geometric3DNodeCollection";

    // -------------------------------------------------------------------------
    // Export Settings
    // -------------------------------------------------------------------------
    /** */
    @Getter
    @Setter
    @SectionInConfigFile("Export Settings")
    @OptionalInConfigFile("EPS 2 PDF command:\n" + "This is the command that is used to convert an EPS file \n"
            + "into a PDF file. You can use the following parameters:\n"
            + "  %s is the complete path from the root folder of the\n"
            + "     framework to the SOURCE file (the eps)\n"
            + "  %t is the complete path from the root folder of the\n"
            + "     framework to the TARGET file (the pdf)\n" + "These placeholders are set by the framework.\n"
            + "Example:\n" + "  'epstopdf %s')")
    private static String epsToPdfCommand = "epstopdf %s";

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("Enables the drawing of the bounding box of the deployment to EPS/PDF.")
    private static boolean epsDrawDeploymentAreaBoundingBox = true;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("Indicates whether the background in the ps should be\n " + "white or gray.\n "
            + "The gray version is easier to understand (especially in 3D)\n"
            + "but the white one should be more useful to be imported in reports." + "")
    private static boolean epsDrawBackgroundWhite = true;

    // -------------------------------------------------------------------------
    // Diverse
    // -------------------------------------------------------------------------
    /** */
    @Getter
    @Setter
    @SectionInConfigFile("Animation Settings")
    @OptionalInConfigFile("Draw an envelope for each message that is being sent")
    private static boolean showMessageAnimations;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("Width of the envelope (when the message animation is enabled)")
    private static double messageAnimationEnvelopeWidth = 30;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("Height of the envelope (when the message animation is enabled)")
    private static double messageAnimationEnvelopeHeight = 20;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("Color of the envelope (when the message animation is enabled)")
    private static Color messageAnimationEnvelopeColor = Color.YELLOW;

    // -------------------------------------------------------------------------
    // Diverse
    // -------------------------------------------------------------------------
    /** */
    @Getter
    @Setter
    @SectionInConfigFile("Diverse Settings")
    @OptionalInConfigFile("Show hints on how to further optimize the simulation when\n"
            + "some parameters seem not to be set optimally.")
    private static boolean showOptimizationHints = true;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("Indicates whether the edges are drawn in the default\n"
            + "draw implementation for the graph.")
    private static boolean drawEdges = true;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("Indicates whether the nodes are drawn in the default\n"
            + "draw implementation for the graph.")
    private static boolean drawNodes = true;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("The number of future events that are shown in the control\n" + "panel")
    private static int shownEventQueueSize = 10;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("Height of the output text field in pixels.")
    private static int outputTextFieldHeight = 200;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("The length of the arrows. This length is multiplied by the current " + "zoomLevel.")
    private static int arrowLength = 8;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("The width of the arrows. This width is multiplied by the current " + "zoomLevel.")
    private static int arrowWidth = 2;

    /** */
    @Getter
    @Setter
    @OptionalInConfigFile("The dsfault value of the rounds field.")
    private static int defaultRoundNumber = 1;

    ///**
    // * Indicates whether the three plotted frame lines are also exported to the eps
    // * as a dotted line.
    // */
    // public static final boolean DRAW_FRAME_DOTTED = false;

    /*-***********************************************************************************************************
     * END of FRAMEWORK-BLOCK
     *************************************************************************************************************/

    // -------------------------------------------------------------------------
    // Edge pool stuff
    // -------------------------------------------------------------------------

    /**
     * A boolean indicating whether the type of the edges has changed, but
     * the edge factory has not yet reacted to the change.. This is used
     * that we can get rid of the reflection, if the same type of edges has to be
     * created again.
     * <p>
     * <p>
     * Note that this member MUST NOT have any annotations!
     *
     * @param changed True if the edge type changed.
     * @return Whether the edge type has changed.
     */
    @Getter
    @Setter
    private static boolean edgeTypeChanged = true;

    /**
     * The short name of the edge (e.g. Edge, myProject:MyEdge)
     *
     * @return The short name for the current edge type.
     */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private static String edgeTypeShortName = "";

    /**
     * This method sets the type of the edge to create. It also sets the
     * edgeTypeChanged flag.
     *
     * @param selectedType The name of the type to set the type to.
     */
    public static void setEdgeType(String selectedType) {
        setEdgeTypeShortName(selectedType);
        if (selectedType.compareTo("Edge") == 0) { // the default edge from the framework
            edgeType = "sinalgo.nodes.edges.Edge";
        } else if (selectedType.compareTo("BidirectionalEdge") == 0) { // the bidirectional edge from the framework
            edgeType = "sinalgo.nodes.edges.BidirectionalEdge";
        } else if (selectedType.contains(":")) { // edge specification of the form project:edgeName
            String[] modelParts = selectedType.split(":");
            edgeType = Configuration.getUserProjectsPackage() + "." + modelParts[0] + ".nodes.edges." + modelParts[1];
        } else if (!selectedType.contains(".")) { // just the name of an edge (without '.') -> from the default project
            edgeType = Configuration.getDefaultProjectPackage() + ".nodes.edges." + selectedType;
        } else { // the edge is given already in explicit form
            edgeType = selectedType;
            setEdgeTypeShortName(Global.toShortName(edgeType));
        }
        setEdgeTypeChanged(true);
    }

    // -------------------------------------------------------------------------
    // Setters and getters for objects
    // There is an assign() and a getText() method that needs to be implemented
    // for each object (except the String)
    // -------------------------------------------------------------------------

    private static Object textToObject(Class<?> c, String text) throws ConfigurationException {
        if (c.equals(String.class)) {
            return text;
        }
        if (c.equals(Color.class)) {
            // try if it's the name of a system color, e.g. yellow, red, black, ...
            try {
                Field f = Color.class.getDeclaredField(text.toLowerCase());
                return f.get(null);
            } catch (Throwable ignored) {
            }
            // otherwise, assume that its of the form r=RR,g=GG,b=BB
            String[] list = text.split("[^0-9]");
            String[] colors = new String[3];
            // move the non-empty entries to the beginning of the list
            int offset = 0;
            for (String s : list) {
                if (!s.equals("")) {
                    colors[offset++] = s;
                    if (offset > 2) {
                        break;
                    }
                }
            }
            if (offset < 3) {
                throw new ConfigurationException(
                        "Invalid color description (" + text + ") : The description is expected to"
                                + " be the name of a color (which is a member of java.awt.Color), or of the form"
                                + " r=255,g=255,b=0");
            }

            try {
                int r = Integer.parseInt(colors[0]);
                int g = Integer.parseInt(colors[1]);
                int b = Integer.parseInt(colors[2]);
                return new Color(r, g, b);
            } catch (NumberFormatException e) {
                throw new ConfigurationException(
                        "Invalid color description (" + text + ") : The description is expected to"
                                + " be the name of a color (which is a member of java.awt.Color), or of the form"
                                + " r=255,g=255,b=0");
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException(
                        "Invalid color description (" + text + ") : The description is expected to"
                                + " be the name of a color (which is a member of java.awt.Color), or of the form"
                                + " r=255,g=255,b=0. The values for each color must be in the range 0..255.");
            }
        }

        throw new ConfigurationException("In order to use configuration entries of type " + c.getClass().getName()
                + "\n" + "you need to implement the Configuration.assign(...) method for this class.");
    }

    /**
     * This method may also be called with the primitive types
     *
     * @param o The object to be converted
     * @return A textual representation for the given object
     */
    public static String getConfigurationText(Object o) {
        if (o instanceof String) {
            return (String) o;
        }
        if (o instanceof Color) {
            Color c = (Color) o;
            return "r=" + c.getRed() + ",g=" + c.getGreen() + ",b=" + c.getBlue();
        }
        return o.toString();
    }

    // -------------------------------------------------------------------------

    /**
     * The hash map, where the parameters from the specified xml file are stored.
     * They can be accessed by the get method of the HashMap. The key is the name
     * together with all the nested tags from custom to the value itself. For
     * example, if you have the following XML entries: <br>
     * &lt;UDG&gt;&lt;Sample&gt;&lt;Factor
     * value=&quot;7&quot;&gt;&lt;/Factor&gt;&lt;/Sample&gt;&lt;/UDG&gt; <!-- This
     * is the above text in plain:
     * <UDG><Sample><Factor value="7"></Factor></Sample><UDG> --> <br>
     * Then you can access it by calling 'String value =
     * paramters.get("UDG/Sample/Factor");'. This will result in the String 'value'
     * to be "7".
     */
    private static HashMap<String, String> parameters = new HashMap<>();

    /**
     * Adds a property entry to the list of properties.
     *
     * @param key      The key of the property, which is converted to lower-case.
     * @param property The value of the property
     */
    public static void putPropertyEntry(String key, String property) {
        parameters.put(key.toLowerCase(), property);
    }

    /**
     * Retrieves an entry of the configuration file given the corresponding key.
     * <p>
     * The key is first converted to lower-case.
     *
     * @param key The key to the configuration file entry, which is
     *            case-insensitive.
     * @return The configuration file entry corresponding to the entry.
     * @throws CorruptConfigurationEntryException If no entry is associated with the key.
     */
    public static String getStringParameter(String key) throws CorruptConfigurationEntryException {
        key = key.toLowerCase();
        if (parameters.containsKey(key)) {
            return parameters.get(key);
        } else {
            throw new CorruptConfigurationEntryException(
                    "Missing entry in the configuration file: An entry for the key '" + key
                            + "' is missing in the config file of project '" + Global.getProjectName() + "'.");
        }
    }

    /**
     * Tests whether the configuration file had a custom entry with the given key.
     * <p>
     * Note: The method also returns true if the user has assigned a value to the
     * key using the overwrite flag.
     *
     * @param key The key of the entry, which is first converted to lower case.
     * @return true if there is an entry for this key, otherwise false.
     */
    public static boolean hasParameter(String key) {
        return parameters.containsKey(key.toLowerCase());
    }

    /**
     * Retrieves an entry of the configuration file corresponding to the given key
     * and converts it to a double value.
     *
     * @param key The key of the configuration entry, which is first converted to
     *            lower case.
     * @return The entry in the configuration file corresponding to the key,
     * converted to a double.
     * @throws CorruptConfigurationEntryException If no entry is associated with the key or the entry cannot be
     *                                            converted to a double.
     */
    public static double getDoubleParameter(String key) throws CorruptConfigurationEntryException {
        key = key.toLowerCase();
        if (parameters.containsKey(key)) {
            try {
                return Double.parseDouble(parameters.get(key));
            } catch (NumberFormatException e) {
                throw new CorruptConfigurationEntryException(
                        "The entry '" + key + "' in the configuration file cannot be converted to a double value.");
            }
        } else {
            throw new CorruptConfigurationEntryException(
                    "Missing entry in the configuration file: An entry for the key '" + key + "' is missing.");
        }
    }

    /**
     * Retrieves an entry of the configuration file corresponding to the given key
     * and converts it to an integer value.
     *
     * @param key The key of the configuration entry, which is first converted to
     *            lower case.
     * @return The entry in the configuration file corresponding to the key,
     * converted to a integer.
     * @throws CorruptConfigurationEntryException If no entry is associated with the key or the entry cannot be
     *                                            converted to a integer.
     */
    public static int getIntegerParameter(String key) throws CorruptConfigurationEntryException {
        key = key.toLowerCase();
        if (parameters.containsKey(key)) {
            try {
                return Integer.parseInt(parameters.get(key));
            } catch (NumberFormatException e) {
                throw new CorruptConfigurationEntryException(
                        "The entry '" + key + "' in the configuration file cannot be converted to a integer value.");
            }
        } else {
            throw new CorruptConfigurationEntryException(
                    "Missing entry in the configuration file: An entry for the key '" + key + "' is missing.");
        }
    }

    /**
     * Retrieves an entry of the configuration file corresponding to the given key
     * and converts it to an boolean value.
     *
     * @param key The key of the configuration entry, which is first converted to
     *            lower case.
     * @return The entry in the configuration file corresponding to the key,
     * converted to a boolean.
     * @throws CorruptConfigurationEntryException If no entry is associated with the key.
     */
    public static boolean getBooleanParameter(String key) throws CorruptConfigurationEntryException {
        key = key.toLowerCase();
        if (parameters.containsKey(key)) {
            return Boolean.parseBoolean(parameters.get(key));
        } else {
            throw new CorruptConfigurationEntryException(
                    "Missing entry in the configuration file: An entry for the key '" + key + "' is missing.");
        }
    }

    /**
     * Retrieves an entry of the configuration file corresponding to the given key
     * and converts it to a Color object.
     *
     * @param key The key of the configuration entry, which is first converted to
     *            lower case.
     * @return The entry in the configuration file corresponding to the key,
     * converted to a Color object.
     * @throws CorruptConfigurationEntryException If no entry is associated with the key or the entry is not a
     *                                            valid color name (a static Color member of the java.awt.Color
     *                                            class).
     */
    public static Color getColorParameter(String key) throws CorruptConfigurationEntryException {
        key = key.toLowerCase();
        if (!parameters.containsKey(key)) {
            throw new CorruptConfigurationEntryException(
                    "Missing entry in the configuration file: An entry for the key '" + key + "' is missing.");
        }
        String color = parameters.get(key);
        try {
            Field f = Color.class.getDeclaredField(color.toLowerCase());
            return (Color) f.get(null);
        } catch (Throwable t) {
            throw new CorruptConfigurationEntryException(
                    "Invalid color: '" + color + "' specified by the configuration entry" + key
                            + "\nValid color names are the static color members of the java.awt.Color class.");
        }
    }

    /*-**********************************************************************************************
     * BEGIN of ADDITIONAL SETTINGS
     *
     * These settings affect the behavior of the simulation framework and are not
     * contained in the XML-configuration-file. If you want to change these
     * settings, you can do it directly in this source-file (or add them yourself to
     * the XML config file.)
     ************************************************************************************************/

    /**
     * The name of this application.
     */
    @Getter
    @Setter
    private static String appName = "Sinalgo";

    /**
     * The folder where configurations, logs, etc. will be stored.
     */
    @Getter
    @Setter
    private static String appConfigDir = System.getProperty("user.home", "") + "/." + getAppName().toLowerCase();

    /**
     * The folder where the temporary files are generated
     */
    @Getter
    @Setter
    private static String appTmpFolder = getTemporaryFolder();

    private static String getTemporaryFolder() {
        try {
            return Files.createTempDirectory(getAppName().toLowerCase()).toString().replace(File.separatorChar, '/');
        } catch (Exception e) {
            throw new SinalgoFatalException("Could not create a temporary working directory:\n\n" + e);
        }
    }

    /**
     * The directory where the logfiles are stored.
     */
    @Getter
    @Setter
    private static String logFileDirectory = getAppConfigDir() + "/logs";

    /**
     * The path where user-specific projects are stored. This path has to be
     * postfixed with the users project name.
     */
    @Getter
    @Setter
    private static String userProjectsPackage = "projects";

    /**
     * The default project's name
     */
    @Getter
    @Setter
    private static String defaultProjectName = "defaultProject";

    /**
     * The path where the default project is stored.
     */
    @Getter
    @Setter
    private static String defaultProjectPackage = getUserProjectsPackage() + "." + getDefaultProjectName();

    /**
     * The name of the description file in the project folder.
     */
    @Getter
    @Setter
    private static String descriptionFileName = "description.txt";

    /**
     * The name of the description file in the project folder.
     */
    @Getter
    @Setter
    private static String configfileFileName = "Config.xml";

    /**
     * The directory where the resources for sinalgo are stored;
     */
    @Getter
    @Setter
    private static String sinalgoResourceDirPrefix = "sinalgo";

    /**
     * The directory where the images are stored. Remember to use the
     * ClassLoader.getResource() method to map the file name to a url, such that the
     * images can be accessed when they are stored in a jar file.
     */
    @Getter
    @Setter
    private static String sinalgoImageDir = getSinalgoResourceDirPrefix() + "/images";

    /**
     * The directory where the resources for the projects are stored
     */
    @Getter
    @Setter
    private static String projectResourceDirPrefix = getUserProjectsPackage();

    /**
     * A semicolon separated list of project names that should not be considered as
     * user-projects
     */
    @Getter
    @Setter
    private static String nonUserProjectNames = "defaultProject;template";

    /**
     * Assigns a value to the configuration file. This method should only be called
     * during initialization of the framework.
     * <p>
     * This method terminates with a fatal error upon any failure.
     *
     * @param fieldName The name (case sensitive) of the field to be assigned
     * @param value     The value (in textual string format) to assign to the field
     */
    public static void setFrameworkConfigurationEntry(String fieldName, String value) {
        if (fieldName.equals("edgeType")) { // special case for the 'edgeType'
            Configuration.setEdgeType(value);
        } else {
            String type = null;
            try {
                Method setter = null;
                Field field = Configuration.class.getDeclaredField(fieldName);
                type = field.getType().getTypeName();
                try {
                    setter = new PropertyDescriptor(fieldName, Configuration.class).getWriteMethod();
                } catch (IntrospectionException ignore) {
                }

                boolean useSetter = !Modifier.isPublic(field.getModifiers()) && setter != null;

                if (!Modifier.isPublic(field.getModifiers()) && setter == null) {
                    throw new SinalgoFatalException("Error while parsing the configuration file: The entry '" + fieldName
                            + "' in Configuration.java is not public or has a setter method.");
                }
                if (!Modifier.isStatic(field.getModifiers())) {
                    throw new SinalgoFatalException("Error while parsing the configuration file: The entry '" + fieldName
                            + "' in Configuration.java is not static.");
                }

                // Integer
                if (field.getType() == int.class) {
                    try {
                        int intValue = Integer.parseInt(value);
                        if (useSetter) {
                            setter.invoke(null, intValue);
                        } else {
                            field.setInt(null, intValue);
                        }
                    } catch (NumberFormatException ex) {
                        throw new SinalgoFatalException("Error while parsing the specified parameters: Cannot convert '" + value
                                + "' to an integer value for the configuration entry '" + fieldName + "'.");
                    }
                }
                // Boolean
                else if (field.getType() == boolean.class) {
                    // Parse boolean manually, as Boolean.parseBoolean(String) converts anything not
                    // 'false' to true.
                    if (value.compareTo("true") == 0) {
                        if (useSetter) {
                            setter.invoke(null, true);
                        } else {
                            field.setBoolean(null, true);
                        }
                    } else if (value.compareTo("false") == 0) {
                        if (useSetter) {
                            setter.invoke(null, false);
                        } else {
                            field.setBoolean(null, false);
                        }
                    } else {
                        throw new SinalgoFatalException("Error while parsing the specified parameters: Cannot convert '" + value
                                + "' to a boolean value for the configuration entry '" + fieldName + "'.");
                    }
                }
                // Long
                else if (field.getType() == long.class) {
                    try {
                        long longValue = Long.parseLong(value);
                        if (useSetter) {
                            setter.invoke(null, longValue);
                        } else {
                            field.setLong(null, longValue);
                        }
                    } catch (NumberFormatException ex) {
                        throw new SinalgoFatalException("Error while parsing the specified parameters: Cannot convert '" + value
                                + "' to a long value for the configuration entry '" + fieldName + "'.");
                    }
                }
                // double
                else if (field.getType() == double.class) {
                    try {
                        double doubleValue = Double.parseDouble(value);
                        if (useSetter) {
                            setter.invoke(null, doubleValue);
                        } else {
                            field.setDouble(null, doubleValue);
                        }
                    } catch (NumberFormatException ex) {
                        throw new SinalgoFatalException("Error while parsing the specified parameters: Cannot convert '" + value
                                + "' to a double value for the configuration entry '" + fieldName + "'.");
                    }
                } else {
                    try {
                        Object objectValue = textToObject(field.getType(), value);
                        if (useSetter) {
                            setter.invoke(null, objectValue);
                        } else {
                            field.set(null, objectValue);
                        }
                    } catch (Exception e) {
                        throw new SinalgoFatalException("Error while parsing the configuration file: Cannot set the field '" + fieldName
                                + "' of type '" + type + "' to '" + value + "'." + "\n\n"
                                + e.getMessage());
                    }
                }
            } catch (NumberFormatException e) {
                throw new SinalgoFatalException("Error while parsing the configuration file: Cannot set the field '" + fieldName
                        + "' of type '" + type + "' to '" + value
                        + "'. Cannot convert the given value to the desired type:\n" + e);
            } catch (SecurityException | IllegalAccessException | IllegalArgumentException e) {
                throw new SinalgoFatalException("Error while parsing the configuration file: Cannot set the field '" + fieldName
                        + "' to '" + value + "':\n" + e);
            } catch (NoSuchFieldException | InvocationTargetException e) {
                throw new SinalgoFatalException("Invalid configuration file: " + "The field '" + fieldName
                        + "' is not a valid framework entry as it is not " + "contained in Configuration.java, "
                        + "or there isn't a setter method for it. " + "Check the spelling of this field or "
                        + "move it to the custom entries.");

            }
        }
    }

    /**
     * Prints the entire configuration of the framework, including the custom-fields
     * of the xml configuration file to the given stream.
     *
     * @param ps A print-stream to print the configuration to
     */
    public static void printConfiguration(PrintStream ps) {

        // Print the system environment settings
        ps.println("\n------------------------------------------------------\n" + "General Config\n"
                + "------------------------------------------------------");
        // Command Line Args
        ps.print("Command Line arguments: ");
        if (Main.getCmdLineArgs() != null) {
            for (String entry : Main.getCmdLineArgs()) {
                ps.print(entry + " ");
            }
        }
        ps.println();
        // The VM arguments
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        List<String> list = bean.getInputArguments();
        ps.print("Java VM arguments: ");
        for (String entry : list) {
            ps.print(entry + " ");
        }
        ps.println();
        // other VM data
        ps.println("Class path: " + bean.getClassPath());
        // ps.println("Library path: " + bean.getLibraryPath());

        ps.println("------------------------------------------------------\n" + "Configuration settings\n"
                + "------------------------------------------------------");
        Field[] fields = Configuration.class.getDeclaredFields();
        for (Field f : fields) {
            try {
                DefaultInConfigFile dan = f.getAnnotation(DefaultInConfigFile.class);
                OptionalInConfigFile oan = f.getAnnotation(OptionalInConfigFile.class);
                SectionInConfigFile san = f.getAnnotation(SectionInConfigFile.class);
                PrivateInConfigFile pan = f.getAnnotation(PrivateInConfigFile.class);
                if (dan != null || oan != null || pan != null) {
                    if (san != null) { // print section
                        ps.println(" " + san.value());
                    }
                    ps.println("    " + f.getName() + " = " + getConfigurationText(f.get(null)));
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                ps.println(f.getName() + "      ERROR - CANNOT GET THE VALUE OF THIS FIELD.");
            } catch (NullPointerException e) {
                ps.println(
                        f.getName() + "      ERROR - CANNOT GET THE VALUE OF THIS FIELD. It is probably not static.");
            }
        }
        ps.println("------------------------------------------------------\n" + "Custom settings\n"
                + "------------------------------------------------------");
        // sort the custom settings
        SortableVector<String> sv = new SortableVector<>();
        for (Entry<String, String> e : parameters.entrySet()) {
            sv.add(" " + e.getKey() + " = " + e.getValue());
        }
        sv.sort(); // sorts ascending
        for (String s : sv) {
            ps.println(s);
        }

        ps.println("------------------------------------------------------\n" + "Seed for Random Number Generators\n"
                + "------------------------------------------------------");

        if (Configuration.isUseSameSeedAsInPreviousRun()) {
            ps.println(" The same seed as for the previous run: " + Distribution.getSeed());
        } else if (Configuration.isUseFixedSeed()) {
            ps.println(" Fixed seed: " + Distribution.getSeed());
        } else {
            ps.println(" Randomly selected seed: " + Distribution.getSeed());
        }
        ps.println("------------------------------------------------------\n" + "End of settings\n"
                + "------------------------------------------------------\n");
    }
}
