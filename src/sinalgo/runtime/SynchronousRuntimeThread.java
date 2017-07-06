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


import java.util.Date;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.nodes.Node;
import sinalgo.tools.logging.LogL;


/**
 * The runtime implementation for the synchronous simulation mode
 */
public class SynchronousRuntimeThread extends Thread {
	
	/**
	 * The number of rounds the thread has to perform.
	 */
	public long numberOfRounds = 0;
	
	private GUIRuntime runtime = null; // If in GUI-MODE, this member holds the the GUIRuntime

	/**
	 * The rate to refresh the graph. This means all how many steps the gui has to be
	 * redrawn.
	 */
	public long refreshRate = 1;
	
	/**
	 * The constructor for the RuntimeThread class. This constructor is used to create
	 * a RuntimeThread with a enabled GUI.
	 * @param r The instance of the GuiRuntime that has started this thread.
	 */
	public SynchronousRuntimeThread(GUIRuntime r){
		runtime = r;
	}
	
	/**
	 * Default constructor for the batch-mode. 
	 */
	public SynchronousRuntimeThread() {
		runtime = null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run(){
		Global.isRunning = true;
		
		Global.startTime = new Date();
		
		for(long i = 0; i < numberOfRounds; i++){
			// In GUI-mode, check whether ABORT was pressed.
			if(runtime != null && runtime.abort){
				runtime.abort = false;
				break;
			}

			// INCREMENT THE GLOBAL TIME by 1
			++Global.currentTime;
			Global.isEvenRound = !Global.isEvenRound; // flip the bit
			
			Global.startTimeOfRound = new Date();
			Global.numberOfMessagesInThisRound = 0;

			Global.customGlobal.preRound();
			Global.customGlobal.handleGlobalTimers();
			
			//Mobility is performed in a separate iteration over all nodes to avoid inconsistencies.			
			if(Configuration.mobility){
				for(Node n : Runtime.nodes) {
					n.setPosition(n.getMobilityModel().getNextPos(n));
				}
			}

			// Before the nodes perform their step, the entire network graph is updated
			// such that all nodes see the same network when they perform their step.
			for(Node n : Runtime.nodes) {
				n.updateConnections();
			}

			// Test all messages still being sent for interference
			if(Configuration.interference) {
				Runtime.packetsInTheAir.testForInterference();
			}
			
			// Perform the step for each node
			try{
				for(Node n : Runtime.nodes) {
					n.step();
				}
			} catch(WrongConfigurationException wCE){
				Main.minorError(wCE); // in gui, a popup is shown. in batch, exits.
				if(Global.isGuiMode) {
					runtime.getGUI().redrawGUINow();
					runtime.getGUI().setStartButtonEnabled(true);
				}
				Global.isRunning = false;
				return;
			}
			
			Global.customGlobal.postRound();
			
			if(Global.isGuiMode) { //redraw the graph all 'refreshRate' Steps except the last
				if((i%refreshRate) == (refreshRate-1)){
					if(i != numberOfRounds-1){
						runtime.getGUI().redrawGUINow(); // this is a SYNCHRONOUS call to redraw the graph! 
					}
				}
				runtime.getGUI().setRoundsPerformed((int)(Global.currentTime));
			}
	
			// test whether the application should exit
			if(Global.customGlobal.hasTerminated()){
				if(Global.isGuiMode && !Configuration.exitOnTerminationInGUI) { // do not quit GUI mode
					runtime.getGUI().redrawGUINow();
					runtime.getGUI().setStartButtonEnabled(true);
					
					Global.isRunning = false;
					return;
				}
				if(LogL.HINTS) {
					Date tem = new Date();
					long time = tem.getTime() - Global.startTime.getTime();
					Global.log.logln("Termination criteria fulfilled at round "+Global.currentTime+" after "+time+" ms");
					Global.log.logln("Hint: Sinalgo terminated because the function 'hasTerminated()' in CustomGlobal returned true.\n");
				}
				Main.exitApplication(); // exit the application
			}
			
			Global.numberOfMessagesOverAll += Global.numberOfMessagesInThisRound;
			
			if(LogL.ROUND_DETAIL){
				Global.log.logln("Round "+(Global.currentTime)+" finished");
				Global.log.logln("In this round "+Global.numberOfMessagesInThisRound+" Messages were sent");
				Global.log.logln("Overall "+Global.numberOfMessagesOverAll+" Messages were sent\n");
			}
		}
		
		if(Global.isGuiMode) {
			runtime.getGUI().redrawGUINow();
			runtime.getGUI().setStartButtonEnabled(true);
		} else { // we reached the end of a synchronous simulation in batch mode
			if(LogL.HINTS) {
				Date tem = new Date();
				long time = tem.getTime() - Global.startTime.getTime();
				Global.log.logln("Simulation stopped regularly after "+Global.currentTime+" rounds during "+time+" ms");
				Global.log.logln("Which makes "+(time/Global.currentTime)+" ms per round.\n");
			}
			Main.exitApplication(); // exit explicitely, s.t. CustomGlobal.onExit() is called
		}
		Global.isRunning = false;
	}
}
