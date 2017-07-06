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
package projects.defaultProject.models.messageTransmissionModels;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.models.MessageTransmissionModel;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;
import sinalgo.runtime.Main;

/**
 * A message transmission model implementation that delivers all messages in the following round, 
 * which corresponds to a constant time delay of 1. 
 * <br>
 * This model expects a configuration entry of the form 
 * <code>&lt;MessageTransmission ConstantTime="..."&gt;</code>
 * where ConstantTime specifies the time a message needs to arrive.
 */
public class ConstantTime extends MessageTransmissionModel {

	private double time = 1.0;
	
	/**
	 * Creates a new Constant time DefaultMessageTransmissionModel. It tries to read the parameter called
	 * ConstantTime/ConstantValue from the configuration file. You can vary the value there.
	 */
	public ConstantTime(){
		try {
			time = Configuration.getDoubleParameter("MessageTransmission/ConstantTime");
		} catch (CorruptConfigurationEntryException e) {
			Main.warning("Missing or wrong entry in the configuration file for the ConstantTime DefaultMessageTransmissionModel:\n" 
			             + e.getMessage() +	"\n\nDefaulting to constant transmission time of 1 time unit.");
		}
	}
	
	/* (non-Javadoc)
	 * @see models.MessageTransmissionModel#timeToReach(nodes.Node, nodes.Node)
	 */
	public double timeToReach(Node startNode, Node endNode, Message msg) {
		return time;
	}
}
