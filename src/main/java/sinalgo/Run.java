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
package sinalgo;

import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.exception.SinalgoWrappedException;
import sinalgo.gui.ProjectSelector;
import sinalgo.io.IOUtils;
import sinalgo.io.xml.XMLParser;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.runtime.SinalgoUncaughtExceptionHandler;
import sinalgo.tools.Tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper function to start the simulator.
 * Execute './gradlew run' (or 'gradlew.bat run' on Windows) to start the simulator.
 */
public class Run {

    public static void main(String args[]) {
        Thread.currentThread().setUncaughtExceptionHandler(new SinalgoUncaughtExceptionHandler());
        Global.init();

        testJavaVersion();

        StringBuilder command = new StringBuilder(); // the entire command
        try {
            { // Store the cmd line args s.t. we could restart Sinalgo
                AppConfig.getAppConfig().setPreviousRunCmdLineArgs(String.join(" ", args));
                AppConfig.getAppConfig().writeConfig();
            }

            // ensure that there is a project selected
            String projectName = new Run().projectSelector(args); // may be null

            // read in the XML-File and save it in the lookup-table
            XMLParser.parse(IOUtils.getProjectConfigurationAsReader(projectName));

            // parse the -overwrite parameters
            Main.parseOverwriteParameters(args, false);

            // assemble the cmd-line args to start the simulator
            // The simulator is started in a new process s.t. we can
            // - dynamically set the max memory usage
            // - define the priority of the application, e.g. with nice w/o typing it on the
            // cmd line each time
            // add the command string as specified in the config file
            Vector<String> cmds = new Vector<>(Arrays.asList(Configuration.getJavaCmd().split(" ")));

            /*
             * Workaround for enabling the debugger when running this from
             * an IDE or other environments that feature graphical debuggers.
             * This will set a port for debugging that will be either random
             * or derived from the port set by the original debug command,
             * if the random port selection fails. The application will NOT be
             * suspended until the debugger is attached and it'll be started in
             * server mode because it crashes if not in server mode.
             */
            Pattern debugPattern = Pattern.compile("^-Xdebug$|^-Xrunjdwp.*$|^-agentlib:jdwp.*$");
            Pattern portPattern = Pattern.compile("address=\\d+");
            ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                    .map(debugPattern::matcher)
                    .filter(Matcher::matches)
                    .map(Matcher::group)
                    .map(s -> {
                        Matcher portMatcher = portPattern.matcher(s);
                        if (portMatcher.find()) {
                            int port;
                            // Trying to find a random available port
                            try (ServerSocket socket = new ServerSocket(0)) {
                                port = socket.getLocalPort();
                            } catch (IOException e) {
                                port = Integer.parseInt(portMatcher.group(1));
                                port += (port < 65535) ? 10 : -10;
                            }
                            // Setting the new port
                            s = portMatcher.replaceFirst("address=" + port);
                            // Setting it to server mode
                            s = s.replaceFirst("server=n", "server=y");
                            // Do NOT block until the debugger has attached
                            s = s.replaceFirst("suspend=y", "suspend=n");
                        }
                        return s;
                    })
                    .forEachOrdered(cmds::add);

            String cp = System.getProperty("user.dir");
            cmds.add("-Xmx" + Configuration.getJavaVMmaxMem() + "m");
            cmds.add("-cp");
            // Uses the old Class Path as its set by Gradle
            cmds.add(System.getProperty("java.class.path"));
            cmds.add("sinalgo.runtime.Main");

            // the project was selected through the projectSelector GUI, add it to the cmd line args
            if (projectName != null) {
                cmds.add("-project");
                cmds.add(projectName);
            }
            // add the given cmd-line args
            cmds.addAll(Arrays.asList(args));

            // reassemble the entire command for error-messages
            for (String s : cmds) {
                command.append(s).append(" ");
            }

            // create & start the procecss
            ProcessBuilder pb = new ProcessBuilder(cmds);
            pb.directory(new File(cp));
            pb.redirectErrorStream(true);
            mainProcess = pb.start();
            // mainProcess = SinalgoRuntime.getRuntime().exec(command); // alternative

            Runtime.getRuntime().addShutdownHook(new ShutdownThread()); // catch shutdown of this process through user

            // forward all output to this process's standard output (remains in this while
            // loop until
            // the other process finishes)
            BufferedReader osr = new BufferedReader(new InputStreamReader(mainProcess.getInputStream()));
            String line;
            while ((line = osr.readLine()) != null) {
                System.out.println(line);
            }
            int exitValue;
            if ((exitValue = mainProcess.waitFor()) != 0) {
                System.out.println("\n\nThe simulation terminated with exit value " + exitValue + "\n");
                System.out.println("Command: " + command); // print the command for error-checking
            }
            mainProcess = null; // the simulation process stopped

            System.exit(0); // important, otherwise, this process does not terminate
        } catch (IOException | SecurityException | InterruptedException | IllegalArgumentException | UnsupportedOperationException e) {
            throw new SinalgoFatalException(
                    "Failed to create the simulation process with the following command:\n" + command + "\n\n" + e.getMessage());
        }
    }

    /**
     * Test that the java VM version is not below 1.8
     */
    private static void testJavaVersion() {
        // Test that java version is OK (must be >= 1.8)
        String version = System.getProperty("java.version");
        try {
            if (version.matches("^[0-9]\\.[0-9].*$")) {
                version = version.substring(0, version.indexOf(".") + 2);
            }
            double v = Double.parseDouble(version);
            if (v < 1.8) {
                printInvalidJavaError(version);
            }
        } catch (NumberFormatException e) {
            printInvalidJavaError(version);
        }
    }

    private static void printInvalidJavaError(String version) {
        System.err.println("You may have an invalid Java version: " + version
                + ". The application requires version 1.8 or more recent.");
    }

    /**
     * Ensures that the user selected a project. If not done so on the command line
     * with the '-project' flag, this method launches the project selector dialgo,
     * which lets the user select a project.
     *
     * @param args The cmd-line arguments
     * @return The name of the selected project if the project selector was
     * launched, null otherwise.
     */
    private String projectSelector(String[] args) {
        // most of the following code-parts are copied from sinalgo.runtime.Main.main()
        for (String s : args) { // any argument '-help' triggers the help to be printed
            if (s.equals("-help")) {
                Main.usage(false);
                System.exit(1);
            }
        }

        int guiBatch = Tools.parseGuiBatch(args);

        Tools.parseProject(args);

        // start the project selector GUI if no project was selected.
        if (!Global.isUseProject()) {
            if (guiBatch == 2) { // in batch mode
                throw new SinalgoFatalException(
                        "Missing project: In batch mode, you need to specify a project on the command line using the -project flag.");
            }

            Global.setGuiMode(true);
            // we are in gui mode, but no project was selected
            ProjectSelector pane = new ProjectSelector(this);
            pane.populate();

            try {
                // wait for the user to press ok in the ProjectSelector.
                synchronized (this) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                throw new SinalgoWrappedException(e);
            }
        }
        return Global.getProjectName();
    }

    private static Process mainProcess; // the simulation process, may be null

    /**
     * A shutdown hook to kill the simulation process when this process is killed.
     */
    public static class ShutdownThread extends Thread {

        @Override
        public void run() {
            if (mainProcess != null) {
                mainProcess.destroy(); // kill the simulation process
            }
        }
    }

}
