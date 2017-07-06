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
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JOptionPane;

import sinalgo.Run;
import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.configuration.AppConfig;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.gui.GUI;
import sinalgo.gui.ProjectSelector;
import sinalgo.io.versionTest.VersionTester;
import sinalgo.io.xml.XMLParser;
import sinalgo.models.Model;
import sinalgo.tools.Tools;
import sinalgo.tools.logging.LogL;
import sinalgo.tools.logging.Logging;
import sinalgo.tools.statistics.Distribution;

/**
 * The main class to start with.
 */
public class Main{
	
	private static Runtime runtime; 
	public static String[] cmdLineArgs; // the command line arguments
	
	/**
	 * This method is the one to start with. It starts the whole simulation.
	 *
	 * @param args The parameters to start the simulation with.
	 */
	public static void main(String[] args){		
		cmdLineArgs = args; // store for later use
		Run.addJDOMtoClassPath(); // add the jdom library to the classpath, if it's not already there.
		Main main = new Main();
		main.go(args);
	}
	
	//just an internal method to not have it static...
	private void go(String[] args){
		UncaughtExceptionHandler mUEH = new MyUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(mUEH);
		
		for(String s : args) { // any argument '-help' triggers the help to be printed
			if(s.equals("-help")) {
				usage(false);
				cleanup();
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
		if(!Global.useProject){
			if(guiBatch <= 1){
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
			}
		}
		
		//read in the XML-File and save it in the lookup-table
		// if there is a Config.xml.run file, use this temporary file
		String path = Global.getProjectSrcDir() + "/" + Configuration.configfileFileName;
		if(new File(path + ".run").exists()) {
			XMLParser.parse(path + ".run");
		} else {
			XMLParser.parse(path);
		}
		
		// parse the -overwrite parameters
		parseOverwriteParameters(args, true);

		// activate the logging (after all overwrite parameters have been handled)
		Logging.activate();
		
		//sets the Async/Sync flag
		Global.isAsynchronousMode = Configuration.asynchronousMode;
		
		//initialize the chosen runtime system
		if(guiBatch <= 1){ // GUI MODE 
			Global.isGuiMode = true;
			Global.log.logln(LogL.ALWAYS, "> Starting " + Configuration.appName + " in GUI-Mode" +
			               (Global.useProject ? " for project "+Global.projectName+"." : "."));
			
			runtime = new GUIRuntime();
		}	else { // BATCH MODE 
			Global.log.log(LogL.ALWAYS, "> Starting " + Configuration.appName + " in BATCH-Mode" + 
			               (Global.useProject ? " for project "+Global.projectName+"." : "."));
			runtime = new BatchRuntime(); 	
		}
		
		// initialize the DefaultMessageTransmissionModel (only after the runtime exists, s.t. we can output error messages, if needed
		Global.messageTransmissionModel = Model.getMessageTransmissionModelInstance(Configuration.DefaultMessageTransmissionModel, new Object[0]);
		
		if(Global.useProject){
			//Try to initalize the gustomGlobal. This is done after the parsing of the override parameter to not disturb the initialisazion with
			//the static initialisazion of the Global class (initializes the Logger...)
			try {
				// NOTE: we could also call newInstance() on the class-object. But this would not encapsulate 
				// exceptions that may be thrown in the constructor.
				Class<?> custGlob = Class.forName(Global.getProjectBinPath() + ".CustomGlobal");
				Constructor<?> constructor = custGlob.getConstructor();
				Global.customGlobal = (AbstractCustomGlobal) constructor.newInstance();
			} catch (ClassNotFoundException e) {
				Global.log.logln(LogL.WARNING, "There is no CustomGlobal in the project '" + 
				                 Global.projectName +	"'. Using the DefaultCustomGlobal.");
			} catch (InstantiationException e) {
				Main.fatalError("Cannot instanciate the project specific CustomGlobal object:\n" + e);
			} catch (IllegalAccessException e) {
				Main.fatalError("Cannot instanciate the project specific CustomGlobal object:\n" + e);
			} catch (SecurityException e) {
				Main.fatalError("Cannot instanciate the project specific CustomGlobal object:\n" + e);
			} catch (NoSuchMethodException e) {
				Main.fatalError("Cannot instanciate the project specific CustomGlobal object:\n" + e);
			} catch (IllegalArgumentException e) {
				Main.fatalError("Cannot instanciate the project specific CustomGlobal object:\n" + e);
			} catch (InvocationTargetException e) {
				Main.fatalError("Cannot instanciate the project specific CustomGlobal object:\n" + e.getCause().getMessage() + "\n\nMore Info:\n" + e);
			}
		} else{
			Global.log.logln(LogL.WARNING, 
			                 "WARNING: You did not specify a project and thus are using the default project.\n" +
			                 "         Select a project with \"-project Projectname\"."
			);
		}
		
		//Check the projects requirements
		Global.customGlobal.checkProjectRequirements(); // note that the runtime is not yet initialized at this point of time!

		// Test whether this is the latest version
		VersionTester.testVersion(true, false);
		
		try{
			//initialize the appropriate runtime environment
			runtime.initializeRuntimeSystem(args);
			
			Global.log.logln(LogL.ALWAYS, "> Initialisation terminated.");
			if(Configuration.logConfiguration) {
				Configuration.printConfiguration(Global.log.getOutputStream());
			} else {
				Global.log.logln(LogL.ALWAYS, "> The seed for the random number generator is "+ Distribution.getSeed());
			}
			Global.log.logln(LogL.ALWAYS, "> Starting the Simulation.\n");
			
			runtime.preRun();

			runtime.run(runtime.getNumberOfRounds(), false); // possibly call it with 0 (in batch mode, we run until the stopping criteria is met in this case.
		}
		catch(CorruptConfigurationEntryException e){
			Main.fatalError(e);
		}
		catch(WrongConfigurationException e){
			Main.fatalError(e);
		}
	}
	
	/**
	 * Retrieves the GUI-runtime of the application.
	 * @return The GUI runtime environment
	 * @throws NotInGUIModeException if the application is not started in GUI mode.
	 */
	public static GUIRuntime getGuiRuntime() throws NotInGUIModeException {
		if(runtime instanceof GUIRuntime) {
			return (GUIRuntime) runtime;
		} else {
			throw new NotInGUIModeException("Application was started in batch mode, but some code expects it to be running in GUI mode.");
		}
	}

	/**
	 * Retrieves the batch-runtime of the application.
	 * @return The batch runtime environment
	 * @throws NotInBatchModeException if the application is not started in batch mode.
	 */
	public static BatchRuntime getBatchRuntime() throws NotInBatchModeException {
		if(runtime instanceof BatchRuntime) {
			return (BatchRuntime) runtime;
		}
		else{
			throw new NotInBatchModeException("Application was started in gui mode, but some code expects it to be running in batch mode.");
		}
	}
	
	/**
	 * Returns the runtime of the simulation.
	 * @return the runtime of the simulation.
	 */
	public static Runtime getRuntime() {
		if(runtime == null){
			Main.fatalError("Call to Main.getRuntime() before the runtime has been created.");
		}
		return runtime;
	}
	
	/**
	 * Exits the application due to a fatal error.
	 *
	 * Before exiting, an error-message is diplayed if in GUI-mode. In any case, the error is written to System.err. 
	 * 
	 * @param message The message containing the error description.
	 */
	public static void fatalError(String message) {
		if(Global.isGuiMode) {
			if(runtime != null){
				JOptionPane.showMessageDialog(((GUIRuntime)runtime).getGUI(), Tools.wrapAndCutToLines(message, 30), "Fatal Error", JOptionPane.ERROR_MESSAGE);
			}
			else{
				JOptionPane.showMessageDialog(null, Tools.wrapAndCutToLines(message, 30), "Fatal Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		if(Logging.isActivated()) {
			Global.log.logln(LogL.ALWAYS, "\n" + message + "\n\n" + Logging.getStackTrace());
		} else {
			System.err.println("\n" +
			                   "-------------------------------------------------------\n" + 
			                   "Fatal Error\n" +
			                   "-------------------------------------------------------\n" + 
			                   message + "\n" +
			                   "-------------------------------------------------------\n" +
			                   "Stack Trace\n" + 
			                   "-------------------------------------------------------\n" +
			                   Logging.getStackTrace() + 
			                   "-------------------------------------------------------\n"
			);
		}

		try {
			// The main thread has an exception handler installed, that would again call fatalError, and possibly cause an infinite loop  
			Global.customGlobal.onFatalErrorExit();
		} catch(Throwable t) {
			System.err.println("\n\n Furthermore, an exception was thrown in CustomGlobal.onFatalErrorExit():\n" + t.getMessage());
		}
		cleanup();
		System.exit(1);
	}

	/**
	 * Handles an error which does not require termination of the application, 
	 * but that needs to be propagated to the user.
	 * <p>
	 * The error-message is printed to the log-file and the System.err output.
	 * Additionaly, in GUI-mode, a pop-up message informs the user about the problem.
	 * 
	 * @param message The message containing the error description.
	 */
	public static void minorError(String message) {
		if(Global.isGuiMode) {
			JOptionPane.showMessageDialog(null, Tools.wrapAndCutToLines(message,30), "Minor Error", JOptionPane.ERROR_MESSAGE);
		}
		if(Logging.isActivated()) {
			Global.log.logln(LogL.ALWAYS, "\nMinor Error: " + message);
		} else {
			System.err.println("\nMinor Error: " + message + "\n");
		}
	}

	/**
	 * Shows a warning to the user.
	 * <p>
	 * The warning-message is printed to the log-file, only.
	 * Additionaly, in GUI-mode, a pop-up message informs the user about the problem.
	 * 
	 * @param message The message containing the warning.
	 */
	public static void warning(String message) {
		if(runtime instanceof GUIRuntime) {
			GUI gui = ((GUIRuntime) runtime).getGUI();
			JOptionPane.showMessageDialog(gui, Tools.wrapAndCutToLines(message,30), 
			                              "Warning", JOptionPane.WARNING_MESSAGE);
		}
		if(Logging.isActivated()) {
			Global.log.logln(LogL.WARNING, "Warning: " + message);
		} else {
			System.err.println("Warning: " + message);
		}
	}

	/**
	 * Shows a info to the user.
	 * <p>
	 * The info-message is printed to the log-file, only.
	 * Additionaly, in GUI-mode, a pop-up message informs the user about the problem.
	 * 
	 * @param message The message containing the info.
	 */
	public static void info(String message) {
		if(runtime instanceof GUIRuntime) {
			GUI gui = ((GUIRuntime) runtime).getGUI();
			JOptionPane.showMessageDialog(gui, Tools.wrapAndCutToLines(message,30), 
			                              "Information", JOptionPane.INFORMATION_MESSAGE);
		}
		if(Logging.isActivated()) {
			Global.log.logln(LogL.INFO, "INFO: " + message);
		} else {
			System.err.println("INFO: " + message);
		}
	}
	
	/**
	 * Handles an exception which does not require termination of the application, 
	 * but that needs to be propagated to the user.
	 * <p>
	 * The error-message is printed to the log-file and the System.err output.
	 * Additionaly, in GUI-mode, a pop-up message informs the user about the problem.
	 * 
	 * @param t The causing exception.
	 */
	public static void minorError(Throwable t) {
		if(t.getCause() != null) {
			minorError(t + "\nMessage:\n" + t.getMessage() + "\nCause:\n" + t.getCause());
		} else {
			String message = t.toString()+"\n";
			StackTraceElement[] sT = t.getStackTrace();
			for(int i = 0; i < sT.length; i++){
				message+="\tat "+sT[i].toString()+"\n";
			}
			minorError(message);
		}
	}
	
	/**
	 * Exits the application due to a fatal error.
	 *
	 * Before exiting, an error-message is diplayed if in GUI-mode. In any case, the error is written to System.err. 
	 * 
	 * @param t The exception causing the error.
	 */
	public static void fatalError(Throwable t) {
		if(t.getCause() != null) {
			fatalError("----------------------------------------------\n" +
			           t + "\n" +
			           "----------------------------------------------\n" +
			           "Message:\n" + t.getMessage() + "\n" +  
			           "----------------------------------------------\n" +
			           "Cause:\n" +
			           "----------------------------------------------\n" +
			           t.getCause() + "\n" + 
			           "----------------------------------------------\n");
		} else {
			String message = t.toString()+"\n\n" + "Message: " + t.getMessage() + "\n\n";
//			StackTraceElement[] sT = t.getStackTrace();
//			for(int i = 0; i < sT.length; i++){
//				message+="\tat "+sT[i].toString()+"\n";
//			}
			fatalError(message);
		}
	}
	
	
	/**
	 * This method parses the overwrite parameters. This cannot be done later with the other parameters like
	 * -gen... because it can overwrite the parameter for the logging. Thus it is important, that this happens
	 * before the first usage of the Global class, because otherwise the logger would be instantiated. On
	 * the other hand this has to be done after the parsing of the xml-File, because we want to overwrite the
	 * parameters passed in the xml-file. This implies, that the parsing of the xml-file cannot use the logging
	 * mechanism but has to output directly on the console with System.out or System.err.
	 *
	 * @param args The parameters passed to the application by the user.
	 * @param printHints True if the method should print warnings for overwrite commands that
	 * do not overwrite an existing parameter.
	 */
	public static void parseOverwriteParameters(String[] args, boolean printHints){
		int numberOfParameters = args.length;
		for(int i = 0; i < numberOfParameters; i++){

			if(args[i].startsWith("-overwrite")){
				if(args.length == i+1||args[i+1].startsWith("-")){
					if(printHints) {
						System.out.println("You did not specify a parameter to overwrite.");
					}
				}
					
				String paramString = "";
				
				int c = i+1;
				while((numberOfParameters > c)&&(!args[c].startsWith("-"))){
					paramString += args[c]+" ";
					c++;
				}
				
				String[] params = paramString.split(" ");
				for(int j = 0; j < params.length; j++){
					if(!params[j].equals("")){
						String[] nameVal = params[j].split("=");
						
						if(nameVal.length != 2){
							System.err.println("The overwrite-parameter "+params[j]+" is not formated correctly. Use paramName=paramValue");
							cleanup();
							System.exit(1);
						}
						
						if(nameVal[0].equals("edgeType")){
							Configuration.setEdgeType(nameVal[1]);
						}
						else{
							try {
								Field field = Configuration.class.getDeclaredField(nameVal[0]);
								if(field.getType() == int.class){
									field.setInt(null, Integer.parseInt(nameVal[1]));
								}
								else if(field.getType().equals(boolean.class)){
									//Make the parsing by hand and not with
									//field.setBoolean(null, Boolean.parseBoolean(child.getAttributeValue("value")));
									//to avoid an arbitrary string to be taken as true
									//like this, only "true" sets the value to true and only "false" sets it to false
									//all other strings just let the value unchanged (=default)
									
									if(nameVal[1].compareTo("true") == 0){
										field.setBoolean(null, true);
									}
									else if(nameVal[1].compareTo("false") == 0){
										field.setBoolean(null, false);
									}
									else{
										if(printHints) {
											System.err.println("Illegal value \""+nameVal[1]+"\" for a boolean for field "+field.getName()+".\n" +
											                   " Taking the default value of '"+field.get(null)+"'");
										}
									}
								}
								else if(field.getType() == long.class){
									field.setLong(null, Long.parseLong(nameVal[1]));
								}
								else{
									try{
										field.set(Configuration.class, nameVal[1]);
									}
									catch(RuntimeException ex){
										if(printHints) {
											System.err.println("Not supported Framework-Field-Type. Could not overwrite its content.\n" +
											                   " Taking the default value of '"+field.get(null)+"'");
										}
									}
								}
							} 
							catch (SecurityException e) {
								System.err.println("Could not access the Field "+nameVal[0]+" because of: "+e);
							} 
							catch (NoSuchFieldException e) {
								if(!Configuration.hasParameter(nameVal[0]) && printHints) {
									System.err.println("The key '" + nameVal[0]+ "' is not known. Adding it as a new parameter to the framework."); 
									// get rid of this message by putting a dummy entry in the config file
									// We cannot log this problem yet, as we don't know where the log-files will be stored. (This itself
									// is a property specified in the parameters currently being parsed.)
								}
								Configuration.putPropertyEntry(nameVal[0], nameVal[1]);
							} 
							catch (IllegalArgumentException e) {
								Main.fatalError(e);
							} 
							catch (IllegalAccessException e) {
								Main.fatalError(e);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * To quit the application, call this method. It may perform some 
	 * cleanup operations before exiting, as specified in the current 
	 * instance of the customGlobal, in the method onExit().
	 */
	public static void exitApplication() {
		cleanup();
		Global.customGlobal.onExit(); // may perform some cleanup ops
		System.exit(0);
	}
	
	/**
	 * Framework specific cleanup; 
	 */
	private static void cleanup() {
		// write the app config
		AppConfig.getAppConfig().writeConfig();
		// delete the temporary config file (with suffix .run)
		String path = Global.getProjectSrcDir() + "/" + Configuration.configfileFileName;
		File f = new File(path + ".run");
		if(f.exists()) { // delete the temporary .run config file
			f.delete();
		}
	}
	
	/**
	 * This method prints out the usage information.
	 *
	 * @param error A boolean to indicating whether this usage is a error message or not.
	 */
	public static void usage(boolean error){
		if(error){
			System.err.println("Parameters not formatted correctly.\n");
			usage(System.err);
		}
		else{
			usage(System.out);
		}
	}
	
	private static void usage(PrintStream ps){
		ps.println("Usage: {-help|-project|-gui|-batch|-gen|-refreshRate|-rounds|-overwrite}*\n" +
		           "\n-help   Prints this help\n" +
		           "\n-project name\n" +
		           "        Initializes the simulation with the project 'name'\n" +
		           "\n-gui    Runs the simulation in GUI-mode (default)\n" +
		           "\n-batch  Runs the simulation in batch-mode\n" +
		           "\n-gen #n T D {(params)} {CIMR {(params)}}*\n" +
		           "        Generates an initial node placement with\n" +
		           "        #n the number of nodes\n" + 
		           "        T  the node type\n"+
		           "        D  the distribution model\n"+
		           "        CIMR is one of the 4 following models for the node:\n" +
		           "              C  the connectivity model\n"+
		           "              I  the interference model\n"+
		           "              M  the mobility model\n"+
		           "              R  the reliability model\n" +
		           "        All models may be postfixed with a parameter-string\n" +
		           "        params in parentheses\n" +
		           "        The CIMR models may come in any order. If not specified,\n" +
		           "        the corresponding default model is used.\n" +
		           "        \n" +
		           "        For disambiguation, you may prefix any model-name with 'X='\n" +
		           "        where X is {D|C|I|M|R} as used above for the corresponding model.\n"+
		           "\n-rounds x\n" +
		           "        Immediately performs x rounds\n" +
		           "\n-refreshRate x\n" +
		           "        Redraw the GUI only every x-th round\n" + 
		           "\n-overwrite key=value {key=value}*\n" +
		           "        Overwrite settings from the XML configuration file\n" +
		           "          key is the composed entry-name in the XML file\n" +
		           "          value is the new value\n" +
		           "\n"      
		);
	}	
}