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
package projects.defaultProject.models.mobilityModels;


import java.util.Random;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.models.MobilityModel;
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;
import sinalgo.runtime.Main;
import sinalgo.tools.statistics.Distribution;


/**
 * Random Way Point Mobility Model
 * <p>
 * The node selects a random point in the simulation area and moves there with constant speed. 
 * When arrived at the position, the node waits for a predefined amount of time and then selects
 * the next point to move to.
 * <p>
 * Both, the speed and the waiting time can be described using a random-number generator that
 * returns values according to a given distribution probability.
 * The distribions must be specified in the XML configuration file, and may look as following: 
 * <pre>
		&lt;RandomWayPoint&gt;
		&nbsp; &lt;Speed distribution="Gaussian" mean="40" variance="10"/&gt;
		&nbsp; &lt;WaitingTime distribution="Poisson" lambda="10"/&gt;
		&lt;/RandomWayPoint&gt;
	</pre>
 * 
 * <p>
 * <b>Note:</b> The speed is measured in distance-units per round, the waiting time in rounds. 
 * For the speed, the absolute value of the sample produced by the speed-distribution is used.
 * <p>
 * Note that with the random way point mobility model, nodes tend to gather in the center of the
 * deployment area. I.e. the node densitity changes over time, even if you have an initial random
 * placement of the nodes. To circumvent this problem, consider using the random direction mobiltiy
 * model.
 * <p>
 * Further implementation notes:
 * <ul>
 *   <li>If the waiting time expires between two rounds, the node continues waiting until the next round starts.</li>
 *   <li>If a node arrives at its destination between two rounds, it starts waiting, but this time is not counted towards the waiting time.</li>
 *   <li>This implementation assumes that all nodes move according to the same speed and waiting time distributions (it stores the distribution generators in static fields used by all instances).</li>
 *   <li>If the node is moved (either through the gui or any other means) between two successive calls to getNextPos(), this mobility model asks for a new position to walk to.</li>
 *   <li>This mobility model works for 2D as well as for 3D.</li>
 * </ul>
 * 
 * @see projects.defaultProject.models.mobilityModels.PerfectRWP For a perfect random waypoint mobility model which ensures that the simulation only performs in the stationary regime, starting from the first round.
 */
public class RandomWayPoint extends MobilityModel {

	// we assume that these distributions are the same for all nodes
	protected static Distribution speedDistribution;
	protected static Distribution waitingTimeDistribution;

	private static boolean initialized = false; // a flag set to true after initialization of the static vars of this class has been done.
	protected static Random random = Distribution.getRandom(); // a random generator of the framework 
	
	protected Position nextDestination = new Position(); // The point where this node is moving to
	protected Position moveVector = new Position(); // The vector that is added in each step to the current position of this node
	protected Position currentPosition = null; // the current position, to detect if the node has been moved by other means than this mobility model between successive calls to getNextPos()
	protected int remaining_hops = 0; // the remaining hops until a new path has to be determined
	protected int remaining_waitingTime = 0;
	
	/* (non-Javadoc)
	 * @see mobilityModels.MobilityModelInterface#getNextPos(nodes.Node)
	 */
	public Position getNextPos(Node n) {
		// restart a new move to a new destination if the node was moved by another means than this mobility model
		if(currentPosition != null) {
			if(!currentPosition.equals(n.getPosition())) {
				remaining_waitingTime = 0;
				remaining_hops = 0;
			}
		} else {
			currentPosition = new Position(0, 0, 0);
		}
		
		Position nextPosition = new Position();
		
		// execute the waiting loop
		if(remaining_waitingTime > 0) {
			remaining_waitingTime --;
			return n.getPosition();
		}

		if(remaining_hops == 0) {
			// determine the speed at which this node moves
			double speed = Math.abs(speedDistribution.nextSample()); // units per round

			// determine the next point where this node moves to
			nextDestination = getNextWayPoint();
			
			// determine the number of rounds needed to reach the target
			double dist = nextDestination.distanceTo(n.getPosition());
			double rounds = dist / speed;
			remaining_hops = (int) Math.ceil(rounds);
			// determine the moveVector which is added in each round to the position of this node
			double dx = nextDestination.xCoord - n.getPosition().xCoord;
			double dy = nextDestination.yCoord - n.getPosition().yCoord;
			double dz = nextDestination.zCoord - n.getPosition().zCoord;
			moveVector.xCoord = dx / rounds;
			moveVector.yCoord = dy / rounds;
			moveVector.zCoord = dz / rounds;
		}
		if(remaining_hops <= 1) { // don't add the moveVector, as this may move over the destination.
			nextPosition.xCoord = nextDestination.xCoord;
			nextPosition.yCoord = nextDestination.yCoord;
			nextPosition.zCoord = nextDestination.zCoord;
			// set the next waiting time that executes after this mobility phase
			remaining_waitingTime = (int) Math.ceil(waitingTimeDistribution.nextSample());
			remaining_hops = 0;
		} else {
			double newx = n.getPosition().xCoord + moveVector.xCoord; 
			double newy = n.getPosition().yCoord + moveVector.yCoord; 
			double newz = n.getPosition().zCoord + moveVector.zCoord; 
			nextPosition.xCoord = newx;
			nextPosition.yCoord = newy;
			nextPosition.zCoord = newz;
			remaining_hops --;
		}
		currentPosition.assign(nextPosition);
		return nextPosition;
	}
	
	/**
	 * Determines the next waypoint where this node moves after having waited.
	 * The position is expected to be within the deployment area.
	 * @return the next waypoint where this node moves after having waited. 
	 */
	protected Position getNextWayPoint() {
		double randx = random.nextDouble() * Configuration.dimX;
		double randy = random.nextDouble() * Configuration.dimY;
		double randz = 0;
		if(Main.getRuntime().getTransformator().getNumberOfDimensions() == 3) {
			randz = random.nextDouble() * Configuration.dimZ;
		}
		return new Position(randx, randy, randz);
	}
	
	/**
	 * Creates a new random way point object, and reads the speed distribution and 
	 * waiting time distribution configuration from the XML config file.
	 * @throws CorruptConfigurationEntryException When a needed configuration entry is missing.
	 */
	public RandomWayPoint() throws CorruptConfigurationEntryException {
		if(!initialized) {
			speedDistribution = Distribution.getDistributionFromConfigFile("RandomWayPoint/Speed");
			waitingTimeDistribution = Distribution.getDistributionFromConfigFile("RandomWayPoint/WaitingTime");
			initialized = true;
		}
	}
}
