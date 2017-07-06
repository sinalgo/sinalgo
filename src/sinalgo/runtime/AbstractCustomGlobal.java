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


import java.awt.Graphics;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.TreeSet;

import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.nodes.Node;
import sinalgo.nodes.timers.Timer;

/**
 * Implementations of this class provide a global view of your simulation and is the
 * user-definable counterpart to sinalgo.runtime.Global. Each project is suposed to have
 * an implemenation of this abstract class called 'CustomGlobal' in the main folder of
 * the project.   
 */
public abstract class AbstractCustomGlobal {

	/**
	 * This annotation is used to mark methods that should be accessible
	 * through the global menu in the GUI.
	 * <p>
	 * Note: The method to which this annotation is attached must be parameter-less.  
	 */
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface GlobalMethod{
		/** The text shown in the menu */
		String menuText();
		/** The sub-menu to put in this method. 
		 * If not set, the method is placed in the main menu.*/
		String subMenu() default "";
		int order() default 0;
	}
	
	/**
	 * Every time the user opens the 'Global' menu, 
	 * the menu is assembled and includes methods annotated
	 * with the {@link GlobalMethod} annotation. Before including such a
	 * method in the list, this method is called, to allow the project
	 * to decide at runtime whether the method should be included or not, and,
	 * if necessary, change the default menu text.  
	 * @param m The method of this CustomGlobal class, which is annotated with {@link GlobalMethod}
	 * @param defaultText The default menu text specified in the annotation.
	 * @return The text to be displayed for the given method in the menu, 
	 * <code>null</code> if the method should not be included in the menu.
	 */
	public String includeGlobalMethodInMenu(Method m, String defaultText) {
		return defaultText; // The default implementation uses the default text.
		// Do NOT modify this code. To obtain a different behavior, overwrite
		// this method in the project specific CustomGlobal file. 
	}
	
	
	/**
	 * The annotation needed for custom buttons which can be added to the
	 * control panel through special method declarations in the CustomGlobal.java
	 * file.
	 * These method need to be annotated with a CustomButton tag, which specifies the
	 * text or image to be used to draw the button. If the image name is empty, 
	 * the button will contain the given buttonText, otherwise, the button will 
	 * contain the specified image.
	 */
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface CustomButton {
		/** The text shown on the button, if no image is set */
		String buttonText() default "";
		/** The file name of an image to be shown. The image should be a gif, of 21x21 pixels. */
		String imageName() default "";
		/** The tool tip text to be shown when the mouse moves over the button */
		String toolTipText() default "";
	}

	/**
	 * After every round, SINALGO executes this method to determine whether the simulation
	 * should be stopped. This is the preferred way to properly terminate a simulation 
	 * running in batch mode.
	 * <p>
	 * This method is only called in synchronous simulation mode. 
	 * 
	 * @return True if the simulation should terminate, false otherwise. 
	 */
	public abstract boolean hasTerminated();
	

	/**
	 * This paint method is called after the graph has been drawn onto 
	 * the graphics object. It allows for customizing the drawing of the
	 * graph by painting additional information onto the graphics.    
	 * @param g The graphics object onto which the graph has already been painted
	 * @param pt Transformation object to translate between logic and GUI coordinates
	 */
	public void customPaint(Graphics g, PositionTransformation pt) {
		// No implementation here! Add your code to the CustomGlobal.java 
		// file in your project. 
	}
	
	/**
	 * Called by the runtime system when running in asynchronous mode and
	 * when there are no more events in the queue.
	 * 
	 * The user may react to this event by generating another event by overwriting 
	 * this mehtod in the project specific CustomGlobal file.
	 * 
	 * By default, if the queue is still empty after this call, and the application
	 * runs in batch mode, the application terminates. In GUI mode, control goes back 
	 * to the user, who may trigger new events manually with the mouse.  
	 */
	public void handleEmptyEventQueue() {
		// No implementation here! Add your code to the CustomGlobal.java 
		// file in your project. 
	}
	
	
	/**
	 * This method is called when the user clicks to exit the
	 * application in GUI mode, or when sinalgo.runtime.Runtime#exitApplication()
	 * is called directly from within your code.
	 * <p>  
	 * Override this method in the project specific CustomGlobal class to perform 
	 * some cleanup operations, if needed. 
	 */
	public void onExit() {
		// No implementation here! Add your code to the CustomGlobal.java 
		// file in your project. 
	}
	
	/**
	 * This method is called when the framework crashes with a fatal-error.
	 * It executes after the fatal error displays and can be used to 
	 * perform any kind of analysis of the error, or storing recovery information.   
	 */
	public void onFatalErrorExit() {
		// No implementation here! Add your code to the CustomGlobal.java 
		// file in your project. 
	}
	
	
	/**
	 * The framework calls this method after starting the application and 
	 * before executing the first round. 
	 * <p>
	 * This method may be used to perform any task that needs to be executed
	 * before the simulation starts, e.g. initialize some datastructures.
	 * <p>
	 * By default, this method does nothing.
	 */
	public void preRun() {
		// No implementation here! Add your code to the CustomGlobal.java 
		// file in your project. 
	}

	/**
	 * The framework calls this method before each round. I.e. before the positions 
	 * of the nodes are updated or any node performs its step. 
	 */
	public void preRound() {
		// No implementation here! Add your code to the CustomGlobal.java 
		// file in your project.
	}

	/**
	 * The framework calls this method after each round. I.e. after each node has 
	 * executed its step, and the interference has been checked, but before the
	 * graph is redrawn (in GUI mode).   
	 */
	public void postRound() {
		// No implementation here! Add your code to the CustomGlobal.java 
		// file in your project.
	}
	
	/**
	 * The framework calls this method at startup after having selected a project
	 * to check whether the necessary requirements for this project are given.
	 * For algorithms that only work correctly in synchronous mode it could check
	 * that the user didn't try to execute it in asynchronous mode.
	 */
	public void checkProjectRequirements(){
		// No implementation here! Add your code to the CustomGlobal.java 
		// file in your project.
	}
	
	/**
	 * The framework calls this method whenever a node is added to the
	 * framework. (The method is called after addition.)
	 * @param n The node that was added  
	 */
	public void nodeAddedEvent(Node n) {
		// No implementation here! Add your code to the CustomGlobal.java 
		// file in your project.
	}
	
	/**
	 * The framework calls this method whenever a single node is removed from the
	 * framework. (The method is called after removal.)
	 * The method is NOT called when all nodes are removed from the framework
	 * using the 'Clear Graph' method.
	 * @param n
	 */
	public void nodeRemovedEvent(Node n) {
		// No implementation here! Add your code to the CustomGlobal.java 
		// file in your project.
	}
	
	/**
	 * List of all global timers for a synchronous simulation. 
	 * (In asynchronous mode, the global timers are also handled
	 * as events.) 
	 */
	public TreeSet<Timer> globalTimers = new TreeSet<Timer>();

	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b>
	 * <p>Handles all global timers that were scheduled to execute 
	 * prior to (or exactly at) the current time.
	 * <p>
	 * The framework calls this method at the beginning of each round, after
	 * incrementing the global time, and after calling {@link #preRound()}.  
	 */
	public void handleGlobalTimers() {
		if(globalTimers.isEmpty()) {
			return;
		}
		Timer t = globalTimers.first();
		while(t.getFireTime() <= Global.currentTime) {
			globalTimers.remove(t);
			t.fire();
			if(globalTimers.isEmpty()) {
				break;
			}
			t = globalTimers.first(); // go to the next timer
		}
	}
	
}