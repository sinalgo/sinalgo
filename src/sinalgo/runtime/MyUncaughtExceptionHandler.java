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



import java.lang.Thread.UncaughtExceptionHandler;

import sinalgo.tools.Tools;
import sinalgo.tools.logging.Logging;


/**
 * This class implements a UncoughtExceptionHandler. It is used to catch all the uncaught exceptions
 * and forward it to the Main as a fatal error.
 */
public class MyUncaughtExceptionHandler implements UncaughtExceptionHandler{

	public void uncaughtException(Thread t, Throwable e) {
		
		if(e.getClass().equals(java.lang.OutOfMemoryError.class)){
			Runtime.nodes = null;
			Tools.disposeRecycledObjects(Logging.getLogger().getOutputStream());
			System.gc();
			java.lang.Runtime r = java.lang.Runtime.getRuntime();
			long maxMem = r.maxMemory() / 1048576;
			Main.fatalError("Sinalgo ran out of memory. (" + maxMem + " MB is not enough). \n" +
			                "To allow the VM to use more memory, modify the javaVMmaxMem entry of the config file."
			                );
			return;
		}
		
		String st = "    ";
		StackTraceElement[] ste = e.getStackTrace();
		for(StackTraceElement element : ste){
			st+= element.toString()+"\n    ";
		}
		Main.fatalError("There was an Exception in Thread "+t+" \n\n" + 
		                "Exception: " + e + ": \n\n" + 
		                "Message: " + e.getMessage() + "\n\n" +
		                "Cause: " + e.getCause() + "\n\n" + 
		                "StackTrace: " + st);
	}
	
}
