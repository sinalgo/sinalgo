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

/**
 * The runtime handling the runtime in the batch mode.
 */
public class BatchRuntime extends Runtime{	
	
	public void initConcreteRuntime(){
	}
	
	/* (non-Javadoc)
	 * @see runtime.Runtime#run(int)
	 */
	public void run(long rounds, boolean considerInfiniteRunFlag) {
		if(Global.isRunning) {
			return; // a simulation thread is still running - don't start a second one! 
		}
		// if the -rounds flag is not set or negative, run as long as possible
		if(rounds <= 0) {
			rounds = Long.MAX_VALUE;
		}
		
		if(Configuration.asynchronousMode){
			AsynchronousRuntimeThread arT = new AsynchronousRuntimeThread();
			arT.numberOfEvents = rounds;
			Global.isRunning = true;
			arT.start();
		}	else {
			SynchronousRuntimeThread bRT = new SynchronousRuntimeThread();
			bRT.numberOfRounds = rounds;
			Global.isRunning = true;
			bRT.start();
		}
	}
	

	/* (non-Javadoc)
	 * @see sinalgo.runtime.Runtime#setProgress(double)
	 */
	public void setProgress(double percent) {
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.runtime.Runtime#initProgress()
	 */
	public void initProgress(){
		createNodes();
	}
}
