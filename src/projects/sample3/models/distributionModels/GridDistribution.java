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
package projects.sample3.models.distributionModels;


import java.util.Vector;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.models.DistributionModel;
import sinalgo.nodes.Position;
import sinalgo.tools.statistics.Distribution;

/**
 * A distribution model that distributes the nodes on a regular grid depending on the geometric node 
 * collections rMax. It choses the postitions so that the field is optimally covered by the nodes.
 * This means that the grid distances are sqrt(2)*rMax. This completely covers the area between the
 * nodes as there is no point between the four nodes having a distance to all the nodes that is bigger
 * than rMax. If there are nodes left after having a node on every grid position the model distributes
 * the other nodes randomly on the field.
 */
public class GridDistribution extends DistributionModel {

	private java.util.Random rand = Distribution.getRandom();
	
	double radius = 0;
	double horizontalFactor = 0;
	double verticalFactor = 0;
	
	private Vector<Position> positions = new Vector<Position>();
	private int returnNum = 0;
	
	public void initialize(){
		try {
			radius = Configuration.getDoubleParameter("GeometricNodeCollection/rMax");
		} catch (CorruptConfigurationEntryException e) {
			e.printStackTrace();
		}
		horizontalFactor = (Configuration.dimX - 2*radius)/(radius*1.414);
		verticalFactor = (Configuration.dimY - 2*radius)/(radius*1.414);
		
		int ihF = (int)horizontalFactor;
		int ivF = (int)verticalFactor;
		
		int number = 0;
		
		for(int i = 0; i < ihF+1; i++){
			for(int j = 0; j < ivF+1; j++){
				if(number < numberOfNodes){
					positions.add(new Position(radius + i*(radius*1.414), radius + j*(radius*1.414), 0));
				}
			}
		}
	}
	
	@Override
	public Position getNextPosition() {
		if(returnNum < positions.size()){
			return positions.elementAt(returnNum++);
		}
		else{
			double randomPosX = rand.nextDouble() * Configuration.dimX;
			double randomPosY = rand.nextDouble() * Configuration.dimY;
			return new Position(randomPosX, randomPosY, 0);
		}
	}
	
	public void setParamString(String s){}

}
