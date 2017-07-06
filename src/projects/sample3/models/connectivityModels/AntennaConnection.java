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
package projects.sample3.models.connectivityModels;


import projects.sample3.nodes.nodeImplementations.Antenna;
import projects.sample3.nodes.nodeImplementations.MobileNode;
import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.models.ConnectivityModelHelper;
import sinalgo.nodes.Node;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;

/**
 * Implements a connection from a node to the antenna.
 */
public class AntennaConnection extends ConnectivityModelHelper {

	private static boolean initialized = false; // indicates whether the static fields of this class have already been initialized 
	private static double rMaxSquare; // we reuse the rMax value from the GeometricNodeCollection.
	
	/**
	 * The constructor reads the antenna-config settings from the config file.
	 * @throws CorruptConfigurationEntryException When there is a missing entry in the 
	 * config file.
	 */
	public AntennaConnection() throws CorruptConfigurationEntryException {
		if(! initialized) { // only initialize once
			double geomNodeRMax = Configuration.getDoubleParameter("GeometricNodeCollection/rMax");
			try {
				rMaxSquare = Configuration.getDoubleParameter("UDG/rMax");
			} catch(CorruptConfigurationEntryException e) {
				Global.log.logln("\nWARNING: Did not find an entry for UDG/rMax in the XML configuration file. Using GeometricNodeCollection/rMax.\n");
				rMaxSquare = geomNodeRMax;
			}
			if(rMaxSquare > geomNodeRMax) { // dangerous! This is probably not what the user wants!
				Main.minorError("WARNING: The maximum transmission range used for the UDG connectivity model is larger than the maximum transmission range specified for the GeometricNodeCollection.\nAs a result, not all connections will be found! Either fix the problem in the project-specific configuration file or the '-overwrite' command line argument.");
			}
			rMaxSquare = rMaxSquare * rMaxSquare;
			initialized = true;
		}
	}

	protected boolean isConnected(Node from, Node to) {
		// Antennas are hardwired - we exclude links between pairs of antennas.
		// MobileNodes are not connected among themselves
		if(from instanceof Antenna && to instanceof MobileNode ||
				to instanceof Antenna && from instanceof MobileNode) {
			double dist = from.getPosition().squareDistanceTo(to.getPosition());
			return dist < rMaxSquare;
		}
		return false;
	}

}
