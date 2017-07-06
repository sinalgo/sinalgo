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
import sinalgo.gui.GUI;
import sinalgo.gui.GraphPanel;
import sinalgo.gui.dialogs.PercentualProgressDialog;
import sinalgo.gui.dialogs.ProgressBarUser;

/**
 * The runtime handling the runtime in the gui mode.
 */
public class GUIRuntime extends Runtime implements ProgressBarUser {
	private GUI gui = new GUI(this);
	
	/**
	 * This method returns the gui instance.
	 *
	 * @return The one and only instance of the gui. 
	 */
	public GUI getGUI(){ return gui; }
	
	private PercentualProgressDialog pf = new PercentualProgressDialog(this, "Initialising the Nodes");

	/* (non-Javadoc)
	 * @see runtime.Runtime#initConcreteRuntime()
	 */
	public void initConcreteRuntime(){
		
		//at this point the system has to wait for the initialisation of the nodes to be finished.
		synchronized(this){
			try {
				if(!nodeCreationFinished){
					this.wait();
				}
			} catch (InterruptedException e) {
				Main.fatalError(e);
			}
		}
		
		pf.finish();
		
		// In async mode, the user may specify to evaluate the connections immediately at startup
		if(Global.isAsynchronousMode && Configuration.initializeConnectionsOnStartup) {
			if(Runtime.nodes.size() > 0) {
				// when there are no nodes created yet, perform the initialization
				// only during the first step.
				AsynchronousRuntimeThread.initializeConnectivity();
			}
		}
		
		//init the gui
		gui.init();
		
		if(this.numberOfRounds != 0){
			gui.setStartButtonEnabled(false);
		}
		
		// wait until the the GUI has been painted at least once
		// this ensures that the the entire GUI has been drawn nicely
		// before any simulation starts
		while(!GraphPanel.firstTimePainted){
			try {
				synchronized(this){
					wait(100);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see runtime.Runtime#run(int)
	 */
	public void run(long rounds, boolean considerInfiniteRunFlag){
		if(Global.isRunning) {
			return; // a simulation thread is still running - don't start a second one! 
		}
		if(rounds <= 0) {
			return;// nothing to do
		}
		if(considerInfiniteRunFlag && !appConfig.guiRunOperationIsLimited) {
			rounds = Long.MAX_VALUE;
		}
		if(Configuration.asynchronousMode){
			AsynchronousRuntimeThread arT = new AsynchronousRuntimeThread(this);
			arT.numberOfEvents = rounds;
			arT.refreshRate = Configuration.refreshRate;
			
			Global.isRunning = true;
			//start the thread
			arT.start();
		}
		else{
			SynchronousRuntimeThread gRT = new SynchronousRuntimeThread(this);
			gRT.numberOfRounds = rounds;
			gRT.refreshRate = Configuration.refreshRate;
			
			Global.isRunning = true;
			//start the thread
			gRT.start();
		}
	}
	
	public void initProgress(){
		pf.init();
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.runtime.Runtime#setProgress(double)
	 */
	public void setProgress(double percent) {
		pf.setPercentage(percent);
	}

	public void cancelClicked() {
		System.exit(1);
	}

	/* (non-Javadoc)
	 * @see sinalgo.gui.ProgressBarUser#performMethod()
	 */
	public void performMethod() {
		this.createNodes();
	}
}
