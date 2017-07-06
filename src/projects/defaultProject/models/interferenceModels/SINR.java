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
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.models.InterferenceModel;
import sinalgo.nodes.Position;
import sinalgo.nodes.messages.Packet;
import sinalgo.runtime.Global;
import sinalgo.runtime.Runtime;
import sinalgo.tools.Tools;
import sinalgo.tools.logging.LogL;

/**
 * This interference model determines a quotient q = s / (i+n) between the received
 * signal and the sum of the ambient background noise n and the interference caused
 * by all concurrent transmissions. The transmission succeeds if q > beta, where beta
 * is a small constant. 
 * <br>
 * This model assumes that the intensity of an electric signal decays exponentially 
 * with the distance from the sender. This decrease is parameterized by the path-loss 
 * exponent alpha: Intensity(r) = sendPower/r^alpha. The value of alpha is often chosen
 * in the range between 2 and 6.
 * <br>
 * To the interference caused by concurrent transmissions, we add an ambient noise 
 * level N.
 * <br><br>
 * This model requires the following entry in the configuration file: 
 * <br>
 * &lt;SINR alpha="..." beta="..." noise="..."/&gt;
 * <br>
 * where alpha, beta, and noise are three floating point values.
 */
public class SINR extends InterferenceModel {
	private int alpha = 2; // the path-loss exponent
	private double beta = 0.5; // the threshold 
	private double ambientNoise = 0; // the ambient noise 
	
	/**
	 * The constructor for the SignalToInterference class.
	 */
	public SINR() {
		try{
			alpha = Configuration.getIntegerParameter("SINR/alpha");
		} catch(CorruptConfigurationEntryException e) {
			Tools.fatalError("The configuration entry SINR/alpha is not a valid double:\n\n" + e.getMessage());
		}
		try {
			beta = Configuration.getDoubleParameter("SINR/beta");
		} catch(CorruptConfigurationEntryException e) {
			Tools.fatalError("The configuration entry SINR/beta is not a valid double:\n\n" + e.getMessage());
		}
		try {
			ambientNoise = Configuration.getDoubleParameter("SINR/noise");
		} catch(CorruptConfigurationEntryException e) {
			Tools.fatalError("The configuration entry SINR/noise is not a valid double:\n\n" + e.getMessage());
		}
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.models.InterferenceModel#isDisturbed(sinalgo.nodes.messages.Packet)
	 */
	public boolean isDisturbed(Packet p) {
		Position receiverPos = p.destination.getPosition();
		double distanceFromSource = p.origin.getPosition().distanceTo(receiverPos);
		double poweredDistanceFromSource = Math.pow(distanceFromSource, alpha);
		
		double signal = p.intensity/poweredDistanceFromSource;
		
		double noise = ambientNoise;

		
		for(Packet pack : Runtime.packetsInTheAir) { // iterate over all active packets
			if(pack == p) {
				continue; // that's the packet we want
			}
			if(pack.origin.ID == p.destination.ID) {
				// the receiver node of p is sending a packet itself
				if(!Configuration.canReceiveWhileSending) {
					return true;
				}
				continue; // the interference created from this sender is not considered
			}
			// Detect multiple packets that want to arrive in parallel at the same destination. 
			if(!Configuration.canReceiveMultiplePacketsInParallel && pack.destination.ID == p.destination.ID ) {
				return true;
			}
			
			Position pos = pack.origin.getPosition();
			double distance = pos.distanceTo(receiverPos);
			double poweredDistance = Math.pow(distance, alpha);
			noise += pack.intensity / poweredDistance;
		}
		
		boolean disturbed = signal < beta * noise;
		
		if(LogL.INTERFERENCE_DETAIL) {
			Global.log.logln("Node "+p.destination.ID+" is checking a packet from "+p.origin.ID);
			if(disturbed){
				Global.log.logln("Dropped the message due to too much interference.");
			}
		}
		
		return disturbed;
	}
}
