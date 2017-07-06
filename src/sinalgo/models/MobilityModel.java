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
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;
import sinalgo.runtime.Main;

/**
 * The superclass for all the MobilityModels. Extend it to implement a concrete mobility model.
 */
public abstract class MobilityModel extends Model {
	private static boolean firstTime = true;
	
	/**
	 * This method returns the next position of a node. It is called from the system to update the
	 * position of the nodes during the update pass of a round.
	 *
	 * @param n The node to get the next position for.
	 * @return The next position oth the given node.
	 */
	public abstract Position getNextPos(Node n); 

	/* (non-Javadoc)
	 * @see models.Model#getType()
	 */
	public final ModelType getType() {
		return ModelType.MobilityModel;
	}
	
	/**
	 * The default constructor tests that mobility is enabled. 
	 */
	protected MobilityModel() {
		if(firstTime && !Configuration.mobility) {
			Main.warning("Some nodes are using a mobility model even though mobility is explicitly turned off in the XML Configuration file.");
			firstTime = false; // important to only have one message. 
		}
	}
	
	/**
	 * This constructor only tests whether mobility is enabled if check is set to true. 
	 * @param check Check that mobility is turned on if true. No check is performed if false.
	 */
	protected MobilityModel(boolean check) {
		if(check && firstTime && !Configuration.mobility) {
			Main.warning("Some nodes are using an mobility model even though mobility is explicitly turned off in the XML Configuration file.");
			firstTime = false;
		}
	}
}
