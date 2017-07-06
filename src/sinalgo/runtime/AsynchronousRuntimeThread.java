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

import sinalgo.configuration.Configuration;
import sinalgo.nodes.Node;
import sinalgo.runtime.events.Event;
import sinalgo.runtime.events.EventQueue;
import sinalgo.tools.logging.LogL;

/**
 * This is the asynchronous Runtime Thread that executes the simulation in the Asynchronous case.
 * It handles the global event-queue and takes one event after the other out of it and handles it.
 */
public class AsynchronousRuntimeThread extends Thread{
	
	/**
	 * The number of events to be executed in this run. Has to be set before the thread is started.
	 */
	public long numberOfEvents = 0;
	
	/**
	 * Indicates whether the connectivity is initialized or not. In Asynchronous mode the
	 * connectivity is generated once at startup and then it does not change anymore.
	 */
	public static boolean connectivityInitialized = false;
	
	/**
	 * The number events the gui will be redrawn after.
	 */
	public long refreshRate = 0;
	
	private GUIRuntime runtime = null;
	
	private static Node lastEventNode = null;
	
	/**
	 * The Condtructor for the AsynchronousRuntimeThread creating an instancs with a given GUIRuntime.
	 *
	 * @param runtime The GUIRuntime instance this thread is created for.
	 */
	public AsynchronousRuntimeThread(GUIRuntime runtime){
		this.runtime = runtime;
	}
	
	/**
	 * The Condtructor for the AsynchronousRuntimeThread creating an instance of without a given runtime.
	 * This call is absolutely the same as calling AsynchronousRuntimeThread(null)
	 */
	public AsynchronousRuntimeThread(){
		runtime = null;
	}
	
	/**
	 * Determines which nodes are connected according to the connectivity model. 
	 */
	public static void initializeConnectivity() {
		connectivityInitialized = true;
		for(Node n: Runtime.nodes){
			n.getConnectivityModel().updateConnections(n);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run(){
		Global.isRunning = true;
		
		Event event = null;

		if(!connectivityInitialized && Configuration.initializeConnectionsOnStartup){
			initializeConnectivity();
		}

		for(long i = 0; i < numberOfEvents; i++) {
			// In GUI-mode, check whether ABORT was pressed.
			if(runtime != null && runtime.abort){
				runtime.abort = false;
				break;
			}
			if(event != null) {
				event.free(); // free the previous event
				event = null;
			}
			event = Runtime.eventQueue.getNextEvent(); // returns null if there is no further event
			
			if(event == null && Configuration.handleEmptyEventQueue){
				Global.customGlobal.handleEmptyEventQueue();
				// and try again
				event = Runtime.eventQueue.getNextEvent(); // returns null if there is no further event
			}
			if(event == null){
				Global.log.logln(LogL.EVENT_QUEUE_DETAILS, "There is no event to be executed. Generate an event manually.");
				if(Global.isGuiMode){
					break;
				}	else {
					Main.exitApplication(); // we're in batch mode and there are no more events -> exit 
				}
			}
			
			Global.currentTime = event.time;
			
			event.handle(); // does not yet free the event
			
			if(Global.isGuiMode){
				if(i%refreshRate == refreshRate -1 && i+1 < numberOfEvents){ // only perform if we continue with more events
					if(lastEventNode != null){
						lastEventNode.highlight(false);
					}
					if(event.isNodeEvent()) {
						event.getEventNode().highlight(true);
					}
					lastEventNode = event.getEventNode();// may be null, if the event does not execute on a node
					runtime.getGUI().setRoundsPerformed((Global.currentTime), EventQueue.eventNumber);
					runtime.getGUI().setCurrentlyProcessedEvent(event); // does not store the event
					runtime.getGUI().redrawGUINow();
				}
			}
		}
		
		if(Global.isGuiMode){
			runtime.getGUI().setRoundsPerformed((Global.currentTime), EventQueue.eventNumber);
			if(event != null) {
				runtime.getGUI().setCurrentlyProcessedEvent(event);
		
				if(lastEventNode != null){
					lastEventNode.highlight(false);
				}
				if(event.isNodeEvent()) {
					event.getEventNode().highlight(true);
				}
				lastEventNode = event.getEventNode();// may be null, if the event does not execute on a node
			} else {
				runtime.getGUI().setCurrentlyProcessedEvent(null);
				if(lastEventNode != null) {
					lastEventNode.highlight(false);
				}
			}
			runtime.getGUI().redrawGUINow();
			runtime.getGUI().setStartButtonEnabled(true);
		} else { // Batch mode 
			Main.exitApplication(); // we're in batch mode and the required number of events have been handled -> exit			
		}
		if(event != null) {
			event.free();
			event = null;
		}
		Global.isRunning = false;
	}
}
