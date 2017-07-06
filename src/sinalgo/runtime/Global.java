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


import java.io.File;
import java.util.Date;
import java.util.Vector;

import sinalgo.configuration.Configuration;
import sinalgo.gui.ProjectSelector;
import sinalgo.models.MessageTransmissionModel;
import sinalgo.runtime.AbstractCustomGlobal.GlobalMethod;
import sinalgo.tools.Tools;
import sinalgo.tools.logging.Logging;

/**
 * This is the class, where the global information is stored. Do not mistake it for the Configuration.
 * 
 * Do not add or change in this class bacause it is a part of the framework. For GUI_Methods and other custom global
 * information write a CustomGlobal class and put it in the your project folder. It is then accessible by the 
 * customGlobal variable in this class.
 */
public class Global {
	
	/**
	 * A boolean flag indicating whether the simulation is runing or not. This flag is used to block mouse input (like tooltip...) and 
	 * zooming during the simulation.
	 */
	public static boolean isRunning = false;
	
	/**
	 * This is the date of the last start of a simulation. This means this is the time the user started
	 * the last number of rounds. This time is particularly interesting in the batchmode where the user
	 * just starts one serie of rounds.
	 */
	public static Date startTime = null;
	
	/**
	 * This is the date of the start of the last round started. Only really significant in the synchronous
	 * mode.
	 */
	public static Date startTimeOfRound = null;
	
	/**
	 * The default log file generated for each run. You may add your own log output, 
	 * or create a separete log file.
	 * <p>
	 * Note: A log FILE is only created if outputToConsole is set to false in the 
	 * config file of the proejct. Otherwise, the text written to this logger is printed
	 * to the console.
	 */
	public static Logging log = null; // only install after logging has been activated. 
	
	
	/**
	 * Some Information about the global state of the simulation. You can add other variables here
	 * to collect the information 
	 */
	
	/**
	 * Global information about the number of messages sent in this round.
	 */
	public static int numberOfMessagesInThisRound = 0;
	
	/**
	 * Global information about the number of messages sent in all previous rounds.
	 */	
	public static int numberOfMessagesOverAll = 0;
	
	/**
	 * The current time of the simulation.
	 * <p>
	 * In synchronous simulation, this time is incremented by 1 at the end of every round, in
	 * asynchronous mode, this time is set to be the time of the current event. 
	 */
	public static double currentTime = 0; 
	
	
	/**
 	 * A boolean whose value changes in every round s.t. in every second
 	 * round, this value is the same. This member may only be used in
 	 * synchronous simulation mode. 
	 */
	public static boolean isEvenRound = true;
	
	/**
	 * The Message Transmission Model. This Model indicates how long it takes for a message to go
	 * from one node to another. This model is global for all nodes.
	 * @see MessageTransmissionModel
	 */
	public static MessageTransmissionModel messageTransmissionModel = null;
	
	/**
	 * This is the instance of the custom global class. It is initialized by default with defaultCustomGlobal and if 
	 * the user uses a project and has a custom global, it sets the customGlobal to an instance of the appropriate
	 * class.
	 */
	public static AbstractCustomGlobal customGlobal = new DefaultCustomGlobal();

	/**
	 * A boolean to indicate whether the user wanted to use a specific project or not.
	 */
	public static boolean useProject = false;

	/**
	 * The name of the actual Project. It is specified by the command line.
	 */
	public static String projectName = "";

	/**
	 * @return The base-directory of the source-files of the currently used project.
	 */
	public static String getProjectSrcDir() {
		if(useProject) {
			return Configuration.sourceDirPrefix + "/" + Configuration.userProjectsPath.replace('.','/') + "/" + projectName;
		} else {
			return Configuration.sourceDirPrefix + "/" + Configuration.defaultProjectPath.replace('.','/');
		}
	}

	/**
	 * @return The base-path (separated by '.') of the currently used project.
	 */
	public static String getProjectBinPath() {
		if(useProject) {
			return Configuration.userProjectsPath + "." + projectName;
		} else {
			return Configuration.defaultProjectPath;
		}
	}
	
	/**
	 * True if started in GUI mode, otherwise false.
	 */
	public static boolean isGuiMode = false;
	
	/**
	 * True if runing in asynchronousMode, false otherwise.
	 */
	public static boolean isAsynchronousMode = true;
	
	/**
	 * Gathers all implementations contained in the project-folder and the default folder.
	 * e.g. to get all mobility-models, set path to models/mobilityModels.
	 * 
	 * @param subDir
	 * @return A list of all class-names that are contained in the project or default folder.
	 */
	public static Vector<String> getImplementations(String subDir) {
		return getImplementations(subDir, Configuration.showModelsOfAllProjects);
	}
	
	/**
	 * @see Global#getImplementations(String)
	 * @param allProjects If set to true, the implementations from all projects are included
	 * @return A list of all class-names that are contained in the project or default folder.
	 */
	public static Vector<String> getImplementations(String subDir, boolean allProjects) {
		Vector<String> result = new Vector<String>();
		if(subDir.equals("nodes/edges")) { // special case for the edges: the base implementaions are stored in the framework 
			result.add("Edge");
			result.add("BidirectionalEdge");
		}
		if(allProjects) {
			// default project before the user implementations
			includeDirForImplementations(Configuration.binaryDir + "/" + Configuration.defaultProjectDir + "/" + subDir, "defaultProject", result);
			for(String projectName : ProjectSelector.getAllProjectNames()) {
				includeDirForImplementations(Configuration.binaryDir + "/" + Configuration.userProjectDir + "/" + projectName + "/" + subDir, 
				                             projectName, result);
			}
		} else { 
			if(useProject) {
				includeDirForImplementations(Configuration.binaryDir + "/" + Configuration.userProjectDir + "/" + projectName + "/" + subDir, 
				                             projectName, result);
			}
			// default project after the user implementations
			includeDirForImplementations(Configuration.binaryDir + "/" + Configuration.defaultProjectDir + "/" + subDir, "defaultProject", result);
		}
		return result;
	}
	
	/**
	 * Helper method to include implementations contained in a given folder
	 * @param dirName The folder name to search
	 * @param projectName The name of the project in which the implementations are contained
	 * @param result A vector to which the found implementaions are added in the form projectName:implName (for the default project just the implName)
	 */
	private static void includeDirForImplementations(String dirName, String projectName, Vector<String> result) {
		File dir = new File(dirName);
		String[] list = dir.list();
		if(list != null) {
			for(String s : list) {
				// cut off the '.class', but prefix with the project name and a colon.
				if(s.endsWith(".class") && !s.contains("$")) {
					if(projectName.equals("defaultProject")) {
						result.add(s.substring(0, s.lastIndexOf('.'))); // cut off the '.class'	
					} else {
						result.add(projectName + ":" + s.substring(0, s.lastIndexOf('.'))); // prefix with the project name
					}
				}
			}
		}
	}
	
	/**
	 * Determine the short name. The class may be 
	 * a) from the framework => e.g. Edge, BidirectionalEdge
	 * b) from a project => projectName:ClassName (except for the defaultProject, where only the class name is used)
	 * c) an inner class => the given name is returned w/o modifications.
	 * d) For any other class name format that is not expected, the given name is returned w/o modification.
	 * @param name The name in the form 'projects.sample1.nodes.edges.MyEdge'  
	 * @return The user-friendly name for this class of the form sample1:MyEdge
	 */
	public static String toShortName(String name) {
		if(name.contains("$")) { // its an inner class, display the original name
			return name;
		}
		String[] list = name.replace(".", "#").split("#");
		if(name.startsWith("sinalgo")) { // its from the framework
			if(list.length < 1) {
				return name;
			}
			return list[list.length -1];
		} else {
			if(list.length < 4) {
				return name;
			}
			if(list[list.length - 4].equals("defaultProject")) { // it's from the default project
				return list[list.length-1];
			} else {
				return list[list.length - 4] + ":" + list[list.length-1]; // it's from a user-project
			}
		}
	}
	
	/*************************************************************
	 * Methods always shown in the 'Global' Menu 
	 *************************************************************/
	
	@GlobalMethod(menuText = "Print Memory Stats",subMenu="Sinalgo Memory", order=1)
	public static void printSinalgoMemoryStats() {
		Tools.printSinalgoMemoryStats(Tools.getTextOutputPrintStream());
	}
	
	@GlobalMethod(menuText = "Run GC", subMenu="Sinalgo Memory", order=2)
	public static void runGC() {
		Tools.runGC(Tools.getTextOutputPrintStream());
	}
	
	@GlobalMethod(menuText = "Clear Recycled Objects", subMenu="Sinalgo Memory", order=3)
	public static void disposeRecycledObjects() {
		Tools.disposeRecycledObjects(Tools.getTextOutputPrintStream()); 	
	}


}
