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
package projects.defaultProject.models.interferenceModels;

import sinalgo.configuration.Configuration;
import sinalgo.models.InterferenceModel;
import sinalgo.nodes.messages.Packet;
import sinalgo.runtime.Main;

/**
 * A dummy interference model that does not consider intereference. 
 */
public class NoInterference extends InterferenceModel {
	private static boolean firstTime = true;
	
	/* (non-Javadoc)
	 * @see models.InterferenceModel#isDisturbed(nodes.messages.Packet)
	 */
	public boolean isDisturbed(Packet p) {
		return false;
	}
	
	/**
	 * Constructor that prints a warning if interference is turned on 
	 */
	public NoInterference() {
		super(false);
		if(firstTime && Configuration.interference && Configuration.showOptimizationHints) {
			Main.warning("At least some nodes use the 'NoInterference' interfernce model. " +
			             "If you do not consider interference at all in your project, you can " +
			             "considerably improve performance by turning off interference in the " +
			             "XML configuration file."
			);
			firstTime = false; // important to only have one message. 
		}
	}
}
