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
package projects.defaultProject.models.reliabilityModels;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.models.ReliabilityModel;
import sinalgo.nodes.messages.Packet;
import sinalgo.runtime.Main;
import sinalgo.tools.statistics.Distribution;

/**
 * A loossy reliability model that drops messages with a constant probability.
 * <p>
 * The percentage of dropped messages has to be specified in the configuration file:
 * <p>
 * &lt;LossyDelivery dropRate="..."/&gt;
 */
public class LossyDelivery extends ReliabilityModel {
	java.util.Random rand = Distribution.getRandom();
	private double dropRate = 0;
	
	
	/* (non-Javadoc)
	 * @see sinalgo.models.ReliabilityModel#reachesDestination(sinalgo.nodes.messages.Packet)
	 */
	public boolean reachesDestination(Packet p){ 
		double r = rand.nextDouble();
		return(r > dropRate);
	}
	
	/**
	 * Creates a new Drop Rate Reliability Model instance.
	 */
	public LossyDelivery() {
		try {
			dropRate = Configuration.getDoubleParameter("LossyDelivery/dropRate");
		} catch (CorruptConfigurationEntryException e) {
			Main.fatalError("Missing configuration entry for the Message Transmission Model:\n" + e.getMessage());
		}
	}
}
