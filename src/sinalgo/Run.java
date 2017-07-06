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
package sinalgo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Vector;

import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration;
import sinalgo.gui.ProjectSelector;
import sinalgo.io.xml.XMLParser;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;

/**
 * A helper function to start the simulator.
 * Execute 'java -cp binaries/bin sinalgo.Run' to start the simulator.
 */
public class Run {

	
	public static void main(String args[]) {
		String classPathSeparator = System.getProperty("path.separator");
		String dirSeparator = System.getProperty("file.separator");
		testJavaVersion();
		addJDOMtoClassPath();

		String command = ""; // the entire command
		try {
			{ // Store the cmd line args s.t. we could restart Sinalgo
				String cmdLineArgs = "";
				for(String s : args) {
					cmdLineArgs += s + " ";
				}
				AppConfig.getAppConfig().previousRunCmdLineArgs = cmdLineArgs;
				AppConfig.getAppConfig().writeConfig();
			}
			
			// ensure that there is a project selected
			String projectName = new Run().projectSelector(args); // may be null

			//read in the XML-File and save it in the lookup-table
			String tempConfigFileName = Global.getProjectSrcDir() + "/" + Configuration.configfileFileName; 
			XMLParser.parse(tempConfigFileName);
			// parse the -overwrite parameters
			Main.parseOverwriteParameters(args, false);
			
			// assemble the cmd-line args to start the simulator
			// The simulator is started in a new process s.t. we can 
			// - dynamically set the max memory usage
			// - define the priority of the application, e.g. with nice w/o typing it on the cmd line each time
			Vector<String> cmds = new Vector<String>();
			// add the command string as specified in the config file
			for(String s : Configuration.javaCmd.split(" ")) {
				cmds.add(s);
			}

			String cp = System.getProperty("user.dir");
			cmds.add("-Xmx" + Configuration.javaVMmaxMem + "m");
			cmds.add("-cp");
			// quite odd: the class path needs not be surrounded by hyphens "" - and it must not be for some OS...
			cmds.add("binaries" + dirSeparator + "bin" + classPathSeparator + "binaries" + dirSeparator +"jdom.jar");
			cmds.add("sinalgo.runtime.Main");
			
			if(projectName != null) { // the project was selected through the projectSelector GUI, add it to the cmd line args
				cmds.add("-project");
				cmds.add(projectName);
			}
			// add the given cmd-line args
			for(int i=0; i<args.length; i++) {
				cmds.add(args[i]);
			}
			
			// reassemble the entire command for error-messages
			for(String s : cmds) {
				command += s + " ";
			}
			
			// create & start the procecss
			ProcessBuilder pb = new ProcessBuilder(cmds);
			pb.directory(new File(cp));
			pb.redirectErrorStream(true);
			mainProcess = pb.start();
			//mainProcess = Runtime.getRuntime().exec(command); // alternative
			
			Runtime.getRuntime().addShutdownHook(new ShutdownThread()); // catch shutdown of this process through user
			
			// forward all output to this process's standard output (remains in this while loop until
			// the other process finishes)
			BufferedReader osr = new BufferedReader(new InputStreamReader(mainProcess.getInputStream()));
			String line = null;
			while((line = osr.readLine()) != null) {
				System.out.println(line);
			}
			int exitValue = 0;
			if((exitValue = mainProcess.waitFor()) != 0) {
				System.out.println("\n\nThe simulation terminated with exit value " + exitValue + "\n");
				System.out.println("Command: " + command); // print the command for error-checking
			}
			mainProcess = null; // the simulation process stopped
			
			// cleanup the Config.xml.run file
			if(projectName != null) { 
				File configFile = new File(tempConfigFileName + ".run");
				if(configFile.exists()) {
					configFile.delete();
				}
			}

			System.exit(0); // important, otherwise, this process does not terminate
		} catch (IOException e) {
			Main.fatalError("Failed to create the simulation process with the following command:\n" +
			                command + "\n\n" + e.getMessage());
		} catch (InterruptedException e) {
			Main.fatalError("Failed to create the simulation process with the following command:\n" +
			                command + "\n\n" + e.getMessage());
		}
	}
	
	/**
	 * Adds jdom.jar to the classpath if it's not already there.
	 * This method is clearly a hack (only works if the default 
	 * class loader is a URLClassLoader), and may not be portable 
	 * to more recent versions of java.  
	 */
	public static void addJDOMtoClassPath() {
		//add jdom.jar to the classpath, if it's not already there
		String cp = System.getProperty("java.class.path");
		
		if(!cp.contains("jdom.jar")) {
			try {
				if(!(ClassLoader.getSystemClassLoader() instanceof URLClassLoader)) {
					Main.fatalError("Cannot add 'binaries/jdom.jar' to the classpath. Add it manually on the command-line.");
				}
				URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
				Class<URLClassLoader> sysclass = URLClassLoader.class;
		    String fileSep = System.getProperty("file.separator");
				Method method = sysclass.getDeclaredMethod("addURL",URL.class);
				method.setAccessible(true);
				method.invoke(sysloader,new Object[]{ new File(System.getProperty("user.dir") + fileSep + "binaries"+fileSep+"jdom.jar").toURI().toURL()});
			} catch (Exception e) {
				Main.fatalError("Could not add 'binaries/jdom.jar' to the classpath. Add it manually on the command-line.");
			}
		}
	}
	
	/**
	 * Test that the java VM version is not below 1.5 
	 */
	private static void testJavaVersion() {
		// Test that java version is OK (must be >= 1.5)
		String javaVersion = System.getProperty("java.vm.version");
		javaVersion = javaVersion.replace('.', '#');
		String[] versionParts = javaVersion.split("#"); // can't split with '.', as regex 
		if(versionParts.length < 2) {
			System.err.println("You may have an invalid Java version: " + javaVersion);
		} else {
			try {
				int v = Integer.parseInt(versionParts[1]);
				if(v < 5) {
					System.err.println("You may have an invalid Java version: " + javaVersion + ". The application requires version 1.5 or more recent.");
				}
			} catch(NumberFormatException e) {
				System.err.println("You may have an invalid Java version: " + javaVersion + ". The application requires version 1.5 or more recent.");				
			}
		}
	}
	
	/**
	 * Ensures that the user selected a project. If not done so on the command
	 * line with the '-project' flag, this method launches the project selector 
	 * dialgo, which lets the user select a project. 
	 * @param args The cmd-line arguments
	 * @return The name of the selected project if the project selector was launched, null otherwise.
	 */
	private String projectSelector(String[] args) {
		// most of the following code-parts are copied from sinalgo.runtime.Main.main()
		for(String s : args) { // any argument '-help' triggers the help to be printed
			if(s.equals("-help")) {
				Main.usage(false);
				System.exit(1);
			}
		}
		
		// Parse whether in to start the framework in GUI or batch mode.
		int guiBatch = 0; // 0 = not seen (defaults to GUI), 1 = GUI, 2 = batch
		for(String s : args) {
			if(s.toLowerCase().equals("-batch")) {
				if(guiBatch == 1) { // conflict
					Main.fatalError("You may only specify the '-gui' xor the '-batch' flag.");
				}
				guiBatch = 2;
				Global.isGuiMode = false;
			} else if(s.toLowerCase().equals("-gui")) {
				if(guiBatch == 2) { // conflict
					Main.fatalError("You may only specify the '-gui' xor the '-batch' flag.");
				}
				guiBatch = 1;
				Global.isGuiMode = true;
			}
		}
		
		for(int i = 0; i < args.length; i++){
			if(args[i].equals("-project")) { // A specific project is specified
				if(i+1 >= args.length) {
					Main.fatalError("The flag '-project' must be preceeded by the name of a project");
				}
				// Test that the project folder exists (in the source)
				String path = Configuration.sourceDirPrefix + "/" + Configuration.userProjectsPath.replace('.', '/') + "/" + args[i+1]; //<<RF>> Why not simply call getProejctSrcDir for path? 
				Global.getProjectSrcDir();
				File testProj = new File(path);
				if(testProj.exists()){
					Global.useProject = true;
					Global.projectName = args[i+1];
				}	else{
					Main.fatalError("Cannot find the specified project '" + args[i+1] + "'.\n" +
					                "In order to create a project '" + args[i+1] + "', create a folder '" + 
					                path + "'");
				}
			}
		}
		
		// start the project selector GUI if no project was selected.
		if(!Global.useProject){
			if(guiBatch == 2) { // in batch mode
				Main.fatalError("Missing project: In batch mode, you need to specify a project on the command line using the -project flag.");
			}

			Global.isGuiMode = true;
			//we are in gui mode, but no project was selected
			ProjectSelector pane = new ProjectSelector();
			pane.populate(this);
			
			try {
				//wait for the user to press ok in the ProjectSelector.
				synchronized(this){
					wait();
				}
			}
			catch (InterruptedException e) {
				Main.fatalError(e);
			}
			return Global.projectName;
		} else {
			return null; // already specified
		}
	}
	
	private static Process mainProcess = null; // the simulation process, may be null

	/**
	 * A shutdown hook to kill the simulation process when this process is killed.
	 */
	public static class ShutdownThread extends Thread {
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			if(mainProcess != null) {
				mainProcess.destroy(); // kill the simulation process
			}
		}
	}
	
	
}
