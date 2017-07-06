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
package projects.sample2.models.mobilityModels;

import projects.defaultProject.models.mobilityModels.RandomWayPoint;
import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.io.mapIO.Map;
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;
import sinalgo.runtime.Main;
import sinalgo.tools.Tools;

/**
 * A MobilityModel to select a random target and walk to this point. But if there is a value on
 * the map in front of the node, that is not 0 the node selects a new target.
 */
public class LakeAvoid extends RandomWayPoint{
	
	/**
	 * The one and only constructor.
	 *
	 * @throws CorruptConfigurationEntryException When a needed configuration entry is missing.
	 */
	public LakeAvoid() throws CorruptConfigurationEntryException{
		super();
	}
	
	public Position getNextPos(Node n){
		Map map = Tools.getBackgroundMap();
		
		Position newPos = new Position();
		
		boolean inLake = false;
		if(Configuration.useMap){
			inLake = !map.isWhite(n.getPosition());  //we are already standing in the lake
		}
		
		if(inLake){
			Main.fatalError("A node is standing in a lake. Cannot find a step outside.");
		}
		
		do{
			inLake = false;
			newPos = super.getNextPos(n);
			if(Configuration.useMap){
				if(!map.isWhite(newPos)) {
					inLake = true;
					super.remaining_hops = 0;//this foces the node to search for an other target...
				}
			}
		}	while(inLake);
		
		return newPos;
	}
}
