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
package sinalgo.tools.logging;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import sinalgo.configuration.Configuration;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;


/**
Provides methods to create log-files and add logging statements
to an existing log-file.<br>
<br>
The creation of a log-file is straight forward: To create a log-file
with the name 'myLog.txt', write<br>
<code>
Logging myLog = Logging.getLogger("myLog.txt");
</code>
<p>
To put the log-file in a sub-directory, write<br>
<code>
 Logging myLog = Logging.getLogger("dir1/dir2/myLog.txt");
</code>
<p>
Then, to add log-statements, use the methods log(String) and logln(String). E.g.<br>
<code>
myLog.log("Test");<br>
myLog.logln("Test"); // appends a new-line to the given string
</code>
</p>
Subsequent calls to <i>Logging.getLogger("myLog.txt")</i> will return the same
singleton Logging object. I.e. to access the same log-file from several
classes, you need not make the logging object public or accessible, but
can access it directly with the Logging.getLogger(String) method.
<p>
The framework already provides one global log-file, which may be used
for logging, especially logging of errors. The file name of this framework
log-file is specified in the Config.xml file of each project. For this
framework log-file (and only for this log-file), you can specify in the
Config.xml file, whether a file should be created, or whether its content
should be printed onto the standard output. You can access this
framework log-file by calling <i>Logging.getLogger()</i> or through
<i>sinalgo.runtime.Global.log</i>.
<p><p>

<b><u>Advanced logging features:</u></b>
<p>
<b>a) Log to time directory</b><br>
By default, the log files are created in a folder 'logs' of the root
directory. I.e. subsequent runs of a simulation will overwrite the
log-files. As this overwriting may be undesirable, the log-files may be
placed in a unique folder, which gets created for each simulation. The
name of this unique folder is composed of the project-name and the time
when the simulation started, and is located in the 'logs' directory. You
can turn on this feature in the Config.xml file, by setting the entry
'logToTimeDirectory' to 'true'.
<p><p>

<b>b) Logging with levels</b><br>
Logging statements may be used to debug a system. But after debugging,
these statements are often not needed anymore. However, removing
manually the log statements may be time consuming and often not
desirable, as they need to be re-inserted if the problem occurs again.
To avoid the removal of log-statements in the code, we support the
concept of logging with levels. I.e. each log-statement takes as
optional parameter a boolean indicating whether it should be printed or
not. Collecting all of these boolean variables in a single file lets you
quickly turn on or off different sets of log-statements. Therefore, when
adding log-statements for a certain topic, assign to all of them the
same boolean flag, such that all of them can be enabled or disabled by
this flag (at compile-time).<br>
In theory, this flag can be stored anywhere. We suggest that you collect
all of these flags and store them in the class LogL in the root
directory of your project.<br>
<br>
The file LogL.java may look as following:<br>
<code>
public class LogL extends sinalgo.tools.logging.LogL {<br>
    public static final boolean testLog = false;<br>
    public static final boolean nodeSpeed = true;<br>
}
</code>
<br>
and the log-statements now look as following:<br>

<code>
Logging myLog = Logging.getLogger("myLog.txt");<br>
myLog.log(LogL.testLog, "Test");<br>
myLog.logln(LogL.nodeSpeed, "Test");<br>
</code>
<br>
whereas the first one won't be printed, as LogL.testLog is set to false.
<p><p>

<b>c) Appending to Log Files</b><br> 

 The logging class allows to append to an existing log-file from a
 previous run.  To do so, call the <i>getLogger()</i> method with the
 second optional parameter set to <b>true</b>. Note that log-files
 created with the append flag set to true are always placed in the
 '<i>logs</i>' folder and ignore the <i>'logToTimeDirectory'</i> flag.

<br><br>
<b><u>Remarks:</u></b></br>
<b>a) Runtime:</b><br>
In order to change the log-levels at runtime, you need to remove
the 'final' modifier for the corresponding log-levels in the LogL.java file.
<p><p>

<b>b) Performance:</b><br>
Turning off logging by setting the corresponding flag
to false still triggers the method to be called. Even more costly is
often the composition of the string that is passed to the log-method.
[The composition of the string is not only costly in time, but also
allocates new memory cells, which need to be reclaimed by the garbage
collector later on.]<br>
Most of the time, this is no problem. E.g. when the log-statement is
placed in a part of the code that does not execute often. But when the
log-statement is located in a piece of code that executes very often,
e.g. in every step of every round, this may decrease simulation
performance noticeably.<br>
A possible work-around for such exposed log-statements is to not use the
log-level flag in the method-call, but surround the log-statements with
an if() clause, that only executes if the corresponding log-level is set
to true. E.g.<br>
<code>
if(LogL.testLog) { <br>
myLog.log("Test"); //we don't need the log-level anymore<br>
}
</code>
 */
public class Logging {

	/**
	 * Singleton Constructor for the default logger object. Depending on the configuration
	 * of the framework, this logger may print to the console or to the default log-file. If
	 * printed to the console, no log-file will be created.   
	 * @return The logging instance
	 */
	public static Logging getLogger() {
		if(instance == null) {
			if(activated){
				if(Configuration.outputToConsole) {
					instance = new Logging(System.out);
				} else {
					return getLogger(Configuration.logFileName);
				}
			} else {
				Main.fatalError("You tried to instantiate the logging mechanism before you are allowed to." +
				                "Most probable you instantiated runtime.Global or accessed a static member/function" +
				                "before parsing of the 	-overwrite parameters."
				);
			}
		}
		return instance;
	}

	/**
	 * Returns an instance of the specified logger object. 
	 * The logger object is identified by its name, which is the name 
	 * of the file to which the log statements are written.
	 * <p>
	 * Upon calling this method the first time with a given logFileName, a
	 * new logger object is created. All subsequent calls with the same logFileName
	 * will return the corresponding logger object created before.
	 * <p>
	 * By default, the log-file is stored in the directory 'logs'. The logFileName 
	 * may contain sub-directories, which will be created if necessary.
	 * 
	 * If the flag logToTimeDirectory is set to true in the configuration file
	 * of the current project, the log-file is placed in the directory 'logs/XXX', where
	 * XXX is a unique name composed of the project name and the time when the current
	 * simulation started. Use this feature to not overwrite log-files in subsequent runs.
	 * @param aName The file name that identifies the logger object to be returned. 
	 * This name may also contain sub-directories, in which the corresponding log file is
	 * placed.
	 * @return A logging object for the given log file name.  
	 */
	public static Logging getLogger(String aName) {
		return getLogger(aName, false);
	}

	/**
	 * Returns an instance of the specified logger object. 
	 * The logger object is identified by its name, which is the name 
	 * of the file to which the log statements are written.
	 * <p>
	 * Upon calling this method the first time with a given logFileName, a
	 * new logger object is created. All subsequent calls with the same logFileName
	 * will return the corresponding logger object created before.
	 * <p>
	 * By default, the log-file is stored in the directory 'logs'. The logFileName 
	 * may contain sub-directories, which will be created if necessary.
	 * 
	 * If the flag logToTimeDirectory is set to true in the configuration file
	 * of the current project, the log-file is placed in the directory 'logs/XXX', where
	 * XXX is a unique name composed of the project name and the time when the current
	 * simulation started. Use this feature to not overwrite log-files in subsequent runs.
	 * <p>
	 * If append is set to true, the corresponding log-file is placed in the
	 * directory 'logs', ignoring the logToTimeDirectory flag of the configuration 
	 * file.
	 * If the specified file already exists, this logger object appends to it, 
	 * otherwise, a new file is created.
	 * @param logFileName The file name that identifies the logger object to be returned. 
	 * This name may also contain sub-directories, in which the corresponding log file is
	 * placed.
	 * @param append True to append to an already exising log file. If the file does 
	 * not yet exist, a new file is created.
	 * @return A logging object for the given log file name.  
	 */
	public static Logging getLogger(String logFileName, boolean append) {
		if(activated){
			if(loggers.containsKey(logFileName)) {
				return loggers.get(logFileName);
			} else {
				Logging l = new Logging(logFileName, append);
				loggers.put(logFileName, l);
				return l;
			}
		}
		else{
			Main.fatalError("You tried to instantiate the logging mechanism before you are allowed to." +
			                "Most probable you instantiated runtime.Global or accessed a static member/function" +
			                "before parsing of the 	-overwrite parameters."
			);
		}
		return null;
	}
	
	/**
	 * Adds a log-message to the log file, if the logFlag is set.
	 * @param logFlag Flag to enable/disable ths log-message
	 * @param txt
	 */
	public void log(boolean logFlag, String txt) {
		if(logFlag) {
			out.print(txt);
			if(Configuration.eagerFlush) {
				out.flush();
			}
		}
	}

	/**
	 * Adds a log-message to the log file.
	 * @param txt The text to log.
	 */
	public void log(String txt) {
		out.print(txt);
		if(Configuration.eagerFlush) {
			out.flush();
		}
	}
	
	/**
	 * Adds a log-message with line-break to the log file, if the logFlag 
	 * is set.
	 * @param logFlag Flag to enable/disable ths log-message
	 * @param txt The log message to be printed.
	 */
	public void logln(boolean logFlag, String txt) {
		if(logFlag) {
			out.println(txt);
			if(Configuration.eagerFlush) {
				out.flush();
			}
		}
	}

	/**
	 * Adds a log-message with line-break to the log file.
	 * @param txt The log message to be printed.
	 */
	public void logln(String txt) {
		out.println(txt);
		if(Configuration.eagerFlush) {
			out.flush();
		}
	}
	
	/**
	 * Adds a line-break to the log-file. 
	 */
	public void logln() {
		out.println();
		if(Configuration.eagerFlush) {
			out.flush();
		}
	}
	
	
	/**
	 * Prefixes the log-message with the code position of the
	 * method caller and prints the text to the log file. 
	 * @param txt The log message to be printed.
	 */
	public void logPos(String txt) {
		out.print(getCodePosition(1));
		out.print(" ");
		out.print(txt);
		if(Configuration.eagerFlush) {
			out.flush();
		}
	}

	/**
	 * If the logFlag is set, adds a log-message prefixed with the code
	 * position of the caller to the log file.
	 * @param logFlag Flag to enable/disable ths log-message   
	 * @param txt The log message to be printed.
	 */
	public void logPos(boolean logFlag, String txt) {
		if(logFlag) {
			out.print(getCodePosition(1));
			out.print(" ");
			out.print(txt);
			if(Configuration.eagerFlush) {
				out.flush();
			}
		}
	}
	
	/**
	 * Prefixes the log-message with the code position of the
	 * method caller and prints the text to the log file, adding
	 * a new-line. 
	 * @param txt The log message to be printed.
	 */
	public void logPosln(String txt) {
		out.print(getCodePosition(1));
		out.print(" ");
		out.println(txt);
		if(Configuration.eagerFlush) {
			out.flush();
		}
	}

	/**
	 * If the logFlag is set, prefixes the log-message with the code 
	 * position of the method caller and prints the text to the log
	 * file, adding a new-line.
	 * @param logFlag Flag to enable/disable ths log-message    
	 * @param txt The log message to be printed.
	 */
	public void logPosln(boolean logFlag, String txt) {
		if(logFlag) {
			out.print(getCodePosition(1));
			out.print(" ");
			out.println(txt);
			if(Configuration.eagerFlush) {
				out.flush();
			}
		}
	}

	/** 
	 * Returns the print stream where this logger logs to.
	 * @return The print stream where this logger logs to.
	 */
	public PrintStream getOutputStream() {
		return out;
	}
	
	/**
	 * @return The time-prefix used for the directories when
	 * logToTimeDirectory in the config file is enabled.
	 */
	public static String getTimePrefix() {
		return timePrefix;
	}
	
	public static String getTimeDirectoryName() {
		return Global.projectName + "_" + timePrefix;
	}

	/**
	 * Returns a string representation of the code position
	 * of the caller of this method.
	 * <p>
	 * The method returns a string consisting of the form
	 * className.methodName:lineNumber
	 * where className is the fully qualified class name of the
	 * class in which the code of the caller of this method is located, 
	 * methodName is the name of the method in which the
	 * code of the caller of this method is located,
	 * and lineNumber indicates the line number in the source-file
	 * at which this method is being called.
	 * @return A human friendly description of the position in code
	 * of the calling method. 
	 */
	public static String getCodePosition() {
		return getCodePosition(1);
	}
	
	/**
	 * Returns a string representation of the code position
	 * of the caller of this method or one of its parents.
	 * <p>
	 * To obtain the string representation of the caller of this 
	 * method, set offset to 0. This is equivalent to {@link #getCodePosition()}.
	 * To obtain the code position where this caller's method is being
	 * called, set offset to 1. 
	 * Generally, to get the code position of the method call n steps back
	 * in the calling sequence, set offset to n.
	 * @param offset The offset in the calling sequence to the method
	 * call that triggered the caller of this method to execute.
	 * @return A human friendly description of the position in code
	 * of the calling method or one of its parents, the empty string
	 * if offset does not specify an existing method in the calling 
	 * sequence.
	 * @see #getCodePosition()
	 */
	public static String getCodePosition(int offset) {
		String result = "<cannot determine code position>";
		Exception e = new Exception(); // a dummy exception to get the stack trace
		StackTraceElement trace[] = e.getStackTrace();
		offset++; // zero-based array 
		if(trace.length > offset && offset > 0) {
			result = trace[offset].getClassName() + "." + trace[offset].getMethodName() + ":" + trace[offset].getLineNumber();
		}
		return result;
	}	

	/**
	 * @return The current stacktrace as a string. 
	 */
	public static String getStackTrace() {
		String s = "";
		StackTraceElement[] list = Thread.currentThread().getStackTrace();
		if(list.length <= 2) {
			return ""; // no stack trace
		}
		for(int i=2; true; i++) {
			s += list[i].toString();
			if(i >= list.length - 1) {
				break;
			}
			s += "\n";
		}
		return s;
	}
	
	/**
	 * @return Returns a string representing the current time in the form
	 * Day.Month.Year-Hour:Minutes:Seconds.MilliSeconds
	 */
	public static String getTimeStamp() {
		SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss.SSS");
		return df.format(new Date());
	}
	
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	// Framework specific methods and member variables
	// => You should not need to modify/overwrite/call/use any of these members or methods
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------

	private static Logging instance = null;
	private static HashMap<String, Logging> loggers = new HashMap<String, Logging>();
	private PrintStream out;
	private static String timePrefix; // the time when the simulation started - can be prefixed to the log-files to distringish different rounds. 

	//a boolean, indicating whether the logging mechanism is already activated. This means that the -overwrite
	//parameters are already processed. (@see runtime.Main#parseOverwriteParameters for details)
	private static boolean activated = false;
	
	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * Creates a directory if it does not already exist.
	 * @param dir
	 */
	private void createDir(String dir) {
		File f = new File(dir);
		if(f.exists() && !f.isDirectory()) {
			Main.fatalError("Cannot create folder '" + dir + "'. There is a file called the same name.");
		} else if(!f.exists()){
			try {
				if(!f.mkdirs()) {
					Main.fatalError("Could not generate all of the directories '" + dir + "'.");	
				}
			} catch(SecurityException e) {
				Main.fatalError("Cannot create folder '" + dir + "':\n" + e);
			}
		}
	}
	
	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b>   
	 * Private constructor - this is a singleton implementation
	 * @param aFileName The file name of the file this logger should print to.
	 * @param append Set to true if this logger should append to an existing file. 
	 * If append is set to true, the file is never placed in the time-directory of
	 * the current simulation.
	 */
	private Logging(String aFileName, boolean append) {
		try {
			String dir = Configuration.logFileDirectory;
			if(dir != "") {
				createDir(dir);
				dir += "/";
			}
			
			if(!append) {
				if(Configuration.logToTimeDirectory) {
					dir = dir + getTimeDirectoryName();
					createDir(dir);
					dir = dir + "/";
				}
			} 
			int index = aFileName.lastIndexOf('/');
			if(index > 0) {
				String path = aFileName.substring(0, index);
				createDir(dir + path);
			}

			if(append) {
				out = new PrintStream(new FileOutputStream(dir + aFileName, true));	
			} else {
				out = new PrintStream(dir + aFileName);
			}
		} catch(FileNotFoundException e) {
			Main.fatalError("Could not open the logfile "+aFileName);
		}
	}

	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * Private constructor - this is a singleton implementation 
	 * @param aStream The stream this logger should print to.
	 */
	private Logging(PrintStream aStream) {
		out = aStream;
	}
	
	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * This method activates the logging mechanism. This is used to not let someone use the logging mechanism
	 * before it is reade to use. I.e. before the overwrite-parameters are parsed where the logfile could be
	 * redefined.
	 */
	public static void activate() {
		if(timePrefix == null) {
			SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy_HH.mm.ss.SSS");
			timePrefix = df.format(new Date());
		}
		activated = true;
		Global.log = Logging.getLogger(); // the default logger
	}
	
	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * Tests whether the framework has been configured to an extend that logging may be used. 
	 * Before this method returns true, logging must not be used. 
	 * @return Whether the logging has been activated - and therefore may be used.
	 */
	public static boolean isActivated() {
		return activated;
	}
}
