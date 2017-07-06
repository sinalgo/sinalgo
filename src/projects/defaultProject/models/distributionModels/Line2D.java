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
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.models.DistributionModel;
import sinalgo.nodes.Position;

/**
 * Aligns the nodes on a line. 
 * 
 * By default, the nodes are placed on a horizontal line equally 
 * distributed over the entire simulation. Optionally, the orientation of 
 * the line can be specified in the configuration file with an entry as following: 
<pre>
&lt;DistributionModel&gt;
	&lt;Line FromX="100" FromY="80" ToX="800" ToY="450"/&gt;
&lt;/DistributionModel&gt;
</pre>
 * where the nodes are placed on a line from (FromX,FromY) to (ToX,ToY). If only 
 * one node is placed, it placed in the middle of the line.
 */
public class Line2D extends DistributionModel {
	private int i = 0; // counts number of nodes already returned
	private double dx;
	private double dy; 
	private double previousPositionX;
	private double previousPositionY;
	
	/* (non-Javadoc)
	 * @see sinalgo.models.DistributionModel#initialize()
	 */
	public void initialize() {
		if(Configuration.hasParameter("DistributionModel/Line/FromX") && 
				Configuration.hasParameter("DistributionModel/Line/FromY") &&
				Configuration.hasParameter("DistributionModel/Line/ToX") &&
				Configuration.hasParameter("DistributionModel/Line/ToY")) {
			try {
				previousPositionX = Configuration.getDoubleParameter("DistributionModel/Line/FromX");
				previousPositionY = Configuration.getDoubleParameter("DistributionModel/Line/FromY");
				dx = Configuration.getDoubleParameter("DistributionModel/Line/ToX") - previousPositionX;
				dy = Configuration.getDoubleParameter("DistributionModel/Line/ToY") - previousPositionY;
			} catch(CorruptConfigurationEntryException e) {
				sinalgo.runtime.Main.fatalError(e);
			}
			if(numberOfNodes <= 1) { // place the single node in the middle
				dx /= 2;
				dy /= 2;
			} else { 
				dx /= (numberOfNodes -1);
				dy /= (numberOfNodes -1);
				previousPositionX -= dx;
				previousPositionY -= dy;
			}
		} else { // default horizontal line
			dy = 0;
			dx = ((double) Configuration.dimX) / (this.numberOfNodes + 1);
			previousPositionX = 0;
			previousPositionY = Configuration.dimY / 2;
		}
	}
	
	/* (non-Javadoc)
	 * @see models.DistributionModel#getNextPosition()
	 */
	public Position getNextPosition() {
		i++;
		previousPositionX += dx;
		previousPositionY += dy;
		return new Position(previousPositionX, previousPositionY, 0);
	}
}
