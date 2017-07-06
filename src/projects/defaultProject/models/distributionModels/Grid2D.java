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
package projects.defaultProject.models.distributionModels;

import sinalgo.configuration.Configuration;
import sinalgo.models.DistributionModel;
import sinalgo.nodes.Position;
import sinalgo.runtime.Main;

/**
 * Aligns the nodes about equally spaced on a gird covering the entire deployment area.
 */
public class Grid2D extends DistributionModel {

	private double size; // the cell-size of the gird
	private int numNodesPerLine; // number of nodes on the x-axis
	private int i,j; // loop counters
	
	/* (non-Javadoc)
	 * @see sinalgo.models.DistributionModel#initialize()
	 */
	public void initialize() {
		double a = 1 - numberOfNodes;
		double b = - (Configuration.dimX + Configuration.dimY); // kind of a hack
		double c =  Configuration.dimX * Configuration.dimY;
		double tmp = b * b - 4 * a * c;
		if(tmp < 0) {
			Main.fatalError("negative sqrt");
		}
		size = (-b - Math.sqrt(tmp)) / (2*a);
		numNodesPerLine = (int) Math.round(Configuration.dimX / size) - 1;
		i=0; j=1;
	}
	
	/* (non-Javadoc)
	 * @see models.DistributionModel#getNextPosition()
	 */
	public Position getNextPosition() {
		i ++;
		if(i > numNodesPerLine) {
			i=1; j++;
		}
		return new Position(i * size, j * size, 0);
	}
}
