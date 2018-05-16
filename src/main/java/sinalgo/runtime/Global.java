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
package sinalgo.runtime;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import lombok.Getter;
import lombok.Setter;
import sinalgo.configuration.Configuration;
import sinalgo.configuration.Configuration.ImplementationChoiceInConfigFile.ImplementationType;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.io.IOUtils;
import sinalgo.models.MessageTransmissionModel;
import sinalgo.runtime.AbstractCustomGlobal.GlobalMethod;
import sinalgo.tools.Tools;
import sinalgo.tools.logging.Logging;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is the class, where the global information is stored. Do not mistake it
 * for the Configuration.
 * <p>
 * Do not add or change in this class bacause it is a part of the framework. For
 * GUI_Methods and other custom global information write a CustomGlobal class
 * and put it in the your project folder. It is then accessible by the
 * customGlobal variable in this class.
 */
public class Global {

    /**
     * Stores the names of all projects in a list.
     * <br/><br/>
     * WARNING: This is internal to the framework and should not be changed or used by regular users.
     */
    private static List<String> projectNames;

    /**
     * A map used to store the implementation classes of each type for each project.
     * <br/><br/>
     * WARNING: This is internal to the framework and should not be changed or used by regular users.
     */
    private static Map<ImplementationType, Map<String, List<String>>> implementationMap;

    /**
     * A boolean flag indicating whether the simulation is runing or not. This flag
     * is used to block mouse input (like tooltip...) and zooming during the
     * simulation.
     */
    @Getter
    @Setter
    private static boolean isRunning;

    /**
     * This is the date of the last start of a simulation. This means this is the
     * time the user started the last number of rounds. This time is particularly
     * interesting in the batchmode where the user just starts one serie of rounds.
     */
    @Getter
    @Setter
    private static Date startTime;

    /**
     * This is the date of the start of the last round started. Only really
     * significant in the synchronous mode.
     */
    @Getter
    @Setter
    private static Date startTimeOfRound;

    /**
     * The default log file generated for each run. You may add your own log output,
     * or create a separete log file.
     * <p>
     * Note: A log FILE is only created if outputToConsole is set to false in the
     * config file of the proejct. Otherwise, the text written to this logger is
     * printed to the console.
     */
    @Getter
    @Setter
    private static Logging log; // only install after logging has been activated.

    /*
     * Some Information about the global state of the simulation. You can add other
     * variables here to collect the information
     */

    /**
     * Global information about the number of messages sent in this round.
     */
    @Getter
    @Setter
    private static int numberOfMessagesInThisRound;

    /**
     * Global information about the number of messages sent in all previous rounds.
     */
    @Getter
    @Setter
    private static int numberOfMessagesOverAll;

    /**
     * The current time of the simulation.
     * <p>
     * In synchronous simulation, this time is incremented by 1 at the end of every
     * round, in asynchronous mode, this time is set to be the time of the current
     * event.
     */
    @Getter
    @Setter
    private static double currentTime;

    /**
     * A boolean whose value changes in every round s.t. in every second round, this
     * value is the same. This member may only be used in synchronous simulation
     * mode.
     */
    @Getter
    @Setter
    private static boolean isEvenRound = true;

    /**
     * The Message Transmission Model. This Model indicates how long it takes for a
     * message to go from one node to another. This model is global for all nodes.
     *
     * @see MessageTransmissionModel
     */
    @Getter
    @Setter
    private static MessageTransmissionModel messageTransmissionModel;

    /**
     * This is the instance of the custom global class. It is initialized by default
     * with defaultCustomGlobal and if the user uses a project and has a custom
     * global, it sets the customGlobal to an instance of the appropriate class.
     */
    @Getter
    @Setter
    private static AbstractCustomGlobal customGlobal = new DefaultCustomGlobal();

    /**
     * A boolean to indicate whether the user wanted to use a specific project or
     * not.
     */
    @Getter
    @Setter
    private static boolean useProject;

    /**
     * The name of the actual Project. It is specified by the command line.
     */
    @Getter
    @Setter
    private static String projectName = "";

    /**
     * An atomic boolean used to indicate whether or not the framework has been initialized.
     */
    private static AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Method used to initialize the framework's project scanning logic.
     */
    public synchronized static void init() {
        if (initialized.getAndSet(true)) {
            return;
        }
        try {
            Pattern projectPattern = Pattern.compile("^(" + Configuration.getUserProjectsPackage() + "\\.\\w+).*$");

            Map<String, List<String>> allImplementations = new FastClasspathScanner("-sinalgo", Configuration.getUserProjectsPackage())
                    .scan(Math.min(Math.max(Runtime.getRuntime().availableProcessors(), 4), 1))
                    .getNamesOfAllStandardClasses().parallelStream()
                    .map(projectPattern::matcher)
                    .filter(Matcher::matches)
                    .collect(Collectors.groupingBy(m -> m.group(1), Collectors.mapping(Matcher::group, Collectors.toList())));

            Set<String> blackList = Arrays.stream(Configuration.getNonUserProjectNames().split(";"))
                    .collect(Collectors.toSet());
            projectNames = allImplementations.keySet().parallelStream()
                    .map(Global::getLastName)
                    .filter(s -> !blackList.contains(s))
                    .sorted()
                    .distinct()
                    .collect(Collectors.toList());

            implementationMap = Arrays.stream(ImplementationType.values()).parallel()
                    .collect(Collectors.toMap(Function.identity(), type -> Stream.concat(Stream.of(Configuration.getDefaultProjectName()), projectNames.stream())
                            .collect(Collectors.toMap(Function.identity(), pn -> {
                                String projectPackage = IOUtils.getAsPackage(Configuration.getUserProjectsPackage(), pn);
                                String implPackage = IOUtils.getAsPackage(projectPackage, type.getPkg());
                                Stream<String> implStream = allImplementations.get(projectPackage).stream()
                                        .filter(impl -> impl.matches("^" + implPackage + "\\.\\w+$"))
                                        .map(Global::getLastName)
                                        .sorted()
                                        .distinct()
                                        .map(s -> pn.equals(Configuration.getDefaultProjectName()) ? s : pn + ":" + s);
                                if (ImplementationType.NODES_EDGES.equals(type)) {
                                    implStream = Stream.concat(Stream.of("Edge", "BidirectionalEdge"), implStream);
                                }
                                return implStream.collect(Collectors.toList());
                            }))));
        } catch (Exception e) {
            throw new SinalgoFatalException("Fatal exception. Could not read projects in the user projects package.", e);
        }
    }

    /**
     * Gets the last name of a package or class given a full name separated by dots
     *
     * @param fullName The full name.
     * @return The last name.
     */
    private static String getLastName(String fullName) {
        return fullName.substring(fullName.lastIndexOf('.') + 1, fullName.length());
    }

    /**
     * @return A vector containing all project names scanned in the classpath.
     */
    public static Vector<String> getProjectNames() {
        if (initialized.get()) {
            init();
        }
        return new Vector<>(projectNames);
    }

    /**
     * @return The base-directory of the resource-files of the currently used project.
     */
    public static String getProjectResourceDir() {
        if (isUseProject()) {
            return IOUtils.getAsPath(Configuration.getProjectResourceDirPrefix(), getProjectName());
        } else {
            return IOUtils.getAsPath(Configuration.getProjectResourceDirPrefix(), Configuration.getDefaultProjectName());
        }
    }

    /**
     * @return The currently used project.
     */
    public static String getProjecName() {
        return isUseProject() ? getProjectName() : Configuration.getDefaultProjectName();
    }

    /**
     * @return The base-package of the currently used project.
     */
    public static String getProjectPackage() {
        if (isUseProject()) {
            return IOUtils.getAsPackage(Configuration.getUserProjectsPackage(), getProjectName());
        } else {
            return Configuration.getDefaultProjectPackage();
        }
    }

    /**
     * True if started in GUI mode, otherwise false.
     */
    @Getter
    @Setter
    private static boolean isGuiMode;

    /**
     * True if runing in asynchronousMode, false otherwise.
     */
    @Getter
    @Setter
    private static boolean isAsynchronousMode = true;

    /**
     * Gathers all implementations contained in the project-folder and the default
     * folder. e.g. to get all mobility-models, set path to models/mobilityModels.
     *
     * @param type The name of the subdirectory for which to get the implementations
     * @return A list of all class-names that are contained in the project or
     * default folder.
     */
    public static Vector<String> getImplementations(ImplementationType type) {
        return getImplementations(type, Configuration.isShowModelsOfAllProjects());
    }

    /**
     * @param allProjects If set to true, the implementations from all projects are included
     * @return A list of all class-names that are contained in the project or
     * default folder.
     * @see Global#getImplementations(ImplementationType)
     */
    public static Vector<String> getImplementations(ImplementationType type, boolean allProjects) {
        if (!initialized.get()) {
            init();
        }
        Map<String, List<String>> implForType = implementationMap.getOrDefault(type, Collections.emptyMap());
        Stream<String> projectNameStream = Stream.of(Configuration.getDefaultProjectName());
        if (allProjects) {
            projectNameStream = Stream.concat(projectNameStream, projectNames.stream());
        } else if (isUseProject()) {
            projectNameStream = Stream.concat(Stream.of(getProjectName()), projectNameStream);
        }
        return projectNameStream
                .map(implForType::get)
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toCollection(Vector::new));
    }

    /**
     * Determine the short name. The class may be a) from the framework => e.g.
     * Edge, BidirectionalEdge b) from a project => projectName:ClassName (except
     * for the defaultProject, where only the class name is used) c) an inner class
     * => the given name is returned w/o modifications. d) For any other class name
     * format that is not expected, the given name is returned w/o modification.
     *
     * @param name The name in the form 'projects.sample1.nodes.edges.MyEdge'
     * @return The user-friendly name for this class of the form sample1:MyEdge
     */
    public static String toShortName(String name) {
        if (name.contains("$")) { // its an inner class, display the original name
            return name;
        }
        String[] list = name.replace(".", "#").split("#");
        if (name.startsWith("sinalgo")) { // its from the framework
            if (list.length < 1) {
                return name;
            }
            return list[list.length - 1];
        } else {
            if (list.length < 4) {
                return name;
            }
            if (list[list.length - 4].equals("defaultProject")) { // it's from the default project
                return list[list.length - 1];
            } else {
                return list[list.length - 4] + ":" + list[list.length - 1]; // it's from a user-project
            }
        }
    }

    /*************************************************************
     * Methods always shown in the 'Global' Menu
     *************************************************************/

    @GlobalMethod(menuText = "Print Memory Stats", subMenu = "Sinalgo Memory", order = 1)
    public static void printSinalgoMemoryStats() {
        Tools.printSinalgoMemoryStats(Tools.getTextOutputPrintStream());
    }

    @GlobalMethod(menuText = "Run GC", subMenu = "Sinalgo Memory", order = 2)
    public static void runGC() {
        Tools.runGC(Tools.getTextOutputPrintStream());
    }

    @GlobalMethod(menuText = "Clear Recycled Objects", subMenu = "Sinalgo Memory", order = 3)
    public static void disposeRecycledObjects() {
        Tools.disposeRecycledObjects(Tools.getTextOutputPrintStream());
    }

}
