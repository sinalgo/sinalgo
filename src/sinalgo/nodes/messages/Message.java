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
package sinalgo.nodes.messages;

import java.awt.Color;

/**
 * The superclass of all messages. Extend this class to get your own 
 * custom message type.
 */
public abstract class Message {

	/**
	 * Implement this method in your subclass to return an absolutely identical 
	 * copy (a clone) of this message.
	 * <p>
	 * Whenever a node sends a message, the framework delivers a copy of the message
	 * to the destination to ensure that no other references are hold on the message 
	 * and the message may be modified by somebody else than the receiver. 
	 * <p>
	 * <b>Important:</b> Remember to update this method whenever you modify the members of
	 * your message implementation.
	 * <p> 
	 * If <b>and only if</b> your project ensures that instances of of your message-subclass
	 * are never modified after they have been sent, you may may return this message object
	 * by writing <code>return this;</code> (and thus not return a copy.) This improves 
	 * simulation performance considerably, especially for broadcast messages, but requires 
	 * that you do not modify the message anymore. (The message may not be modified, but it 
	 * is OK if a receiver of this message forwards it. Sending a message does <i>not</i> modify
	 * the message object. The message is encapsulated in a <code>packet</code> that stores the
	 * meta information for the transmission.) 
	 */
	public abstract Message clone();
	
	
	/**
	 * @return The color in which the envelope for this message should be drawn, null
	 * if the default color (specified in the configuration file) should be used.
	 */
	public Color getEnvelopeColor() {
		return null; // use the default color
	}
}
