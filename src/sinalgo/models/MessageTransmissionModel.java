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
package sinalgo.models;

import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;

/**
 * The interface for all TransmissionModels.
 */
public abstract class MessageTransmissionModel extends Model {

	/**
	 * Determines the time a message takes to arrive at its destination.
	 * <p>
	 * When the message is sent via broadcast, and interference is turned on, and the sender
	 * node has no neighbor, the sender sends the message to itself. In that case, the 
	 * startNode and the endNode are identical.
	 * <p>
	 * For most simulation scenarios, this method should return a constant time.   
	 * @param startNode The start node of the time calculation
	 * @param endNode The end node of the time calculation. 
	 * @param msg The message to send
	 * @return The time it takes the message to travel from the source to the destination.
	 */
	public abstract double timeToReach(Node startNode, Node endNode, Message msg);

	/* (non-Javadoc)
	 * @see models.Model#getType()
	 */
	public final ModelType getType() {
		return ModelType.MessageTransmissionModel;
	}

}
