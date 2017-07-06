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

import sinalgo.configuration.Configuration;
import sinalgo.nodes.messages.Packet;
import sinalgo.runtime.Main;

/**
 * The Superclass for all interference models. Extend this class to implement a concrete Interference model.
 * <p>
 * The method <code>isDistrubed(Packet p)</code> is called to determine whether a given message
 * is not received due to interference. 
 */
public abstract class InterferenceModel extends Model {
	private static boolean firstTime = true; 
	
	/**
	 * The framework calls this method to determine whether a given packet 
	 * is disturbed (will not be received at the destination). 
	 * <p>
	 * In the synchronous setting, this test is called for each message not yet delivered
	 * at the end of each simulation round.
	 * <p>
	 * In the asynchronous setting, this test is only performed upon arrival of a message. 
	 * Note: For 'additive interference', the test is performed only if absolutely needed.
	 *
	 * @param p The packet to check.
	 * @return True if the message is disturbed, otherwise false. 
	 */
	public abstract boolean isDisturbed(Packet p);

	/* (non-Javadoc)
	 * @see models.Model#getType()
	 */
	public final ModelType getType() {
		return ModelType.InterferenceModel;
	}
	
	/**
	 * The default constructor tests that interference is enabled. 
	 */
	protected InterferenceModel() {
		if(firstTime && !Configuration.interference) {
			Main.warning("Some nodes are using an interference model even though interference is explicitly turned off in the XML Configuration file.");
			firstTime = false; // important to only have one message. 
		}
	}
	
	/**
	 * This constructor only tests whether interference is enabled if check is set to true. 
	 * @param check Check that interference is turned on if true. No check is performed if false.
	 */
	protected InterferenceModel(boolean check) {
		if(check && firstTime && !Configuration.interference) {
			Main.warning("Some nodes are using an interference model even though interference is explicitly turned off in the XML Configuration file.");
			firstTime = false;
		}
	}
}
