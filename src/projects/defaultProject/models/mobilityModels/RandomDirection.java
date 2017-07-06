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
 * Random Direction Mobility Model
 * <p>
 * The node selects an arbitrary direction and moves for a certain time in this 
 * direction. If it encounters the border of the deployment area, it is reflected
 * just as a perfect billard ball.  
 * <p>
 * The mobility consists of two phases: moving or waiting. The node decides to move 
 * for a certain time in a given direction. Afterwards, it waits for a certain time before
 * it restarts moving. 
 * <p>
 * The time during which the node moves, the waiting time, and the speed
 * at which the node moves can be described using distributions in the configuration 
 * file. An entry in the configuration file may look as following:
 * <pre>
		&lt;RandomDirection&gt;
		&nbsp;&lt;WaitingTime distribution="Poisson" lambda=10/&gt;
		&nbsp;&lt;MoveTime distribution="Constant" constant="13"/&gt;
	  &nbsp;&lt;NodeSpeed distribution="Gaussian" mean="20" variance="20"/&gt;
		&lt;/RandomDirection&gt;
 * </pre>
 * <b>Note:</b> The speed is measured in distance-units per round, the waiting and moving time 
 * in rounds. For all distributions, the absolute value of the generated samples is taken. 
 * <p>
 * The random direction mobility model has the advantage over the random waypoint that it
 * does not concentrate the nodes in the center of the deployment area, but keeps a uniformly
 * random distribution of the nodes.
 * <p>
 * <b>SmoothStart:</b> This RandomDirection mobility model implements a smooth start feature,  
 * allowing the nodes to start in the stationary case. I.e. it avoids that at the beginning, all nodes start 
 * with a new mobility phase, by randomly choosing for each node at the beginning whether it is waiting
 * or moving. 
 * <p>
 * Further implementation notes:
 * <ul>
 *   <li>If the waiting time expires between two rounds, the node continues waiting until the next round starts.</li>
 *   <li>The time a node moves is rounded up to the next integer value.</li>
 *   <li>This implementation assumes that all nodes move according to the same speed and waiting time distributions (it stores the distribution generators in static fields used by all instances).</li>
 *   <li>If the node is moved (either through the gui or any other means) between two successive calls to getNextPos(), this mobility model asks for a new position to walk to.</li>
 *   <li>This mobility model works for 2D as well as for 3D.</li>
 * </ul>
 */
public class RandomDirection extends MobilityModel {
	// we assume that these distributions are the same for all nodes
	private static Distribution speedDistribution; // how fast the nodes move
	private static Distribution waitingTimeDistribution; // how long nodes wait before starting the next mobility phase
	private static Distribution moveTimeDistribution; // for how long the node moves when it moves
	
	protected static boolean initialized = false; // a flag set to true after initialization of the static vars of this class has been done.
	protected static Random random = Distribution.getRandom(); // a random generator of the framework 
	
	private Position moveVector; // The vector that is added in each step to the current position of this node
	protected Position currentPosition = null; // the current position, to detect if the node has been moved by other means than this mobility model between successive calls to getNextPos()
	private int remaining_hops = 0; // the remaining hops until a new path has to be determined
	private int remaining_waitingTime = 0;
	
	private boolean initialize = true; // to detect very first time to start smoothly

	/**
	 * Initializes the next move by determining the next point to move to.
	 * This new destination may be outside the deployment area. This method also
	 * determines the vector along which the nodes move in each step and the number
	 * of rounds that the move will take.
	 * 
	 * @param node The node for which the new destination is determined.
	 * @param moveSpeed The speed at which the node will move.
	 * @parm moveTime The time during which the node is supposed to move
	 */
	private void initializeNextMove(Node node, double moveSpeed, double moveTime) {
		double angleXY = 2 * Math.PI * random.nextDouble(); // 0 .. 360
		double angleZ = Math.PI * (0.5 - random.nextDouble()); // -90 .. 90
		if(Main.getRuntime().getTransformator().getNumberOfDimensions() == 2) {
			angleZ = 0; // remain in the XY-plane
		}
		double distance = moveTime * moveSpeed; // the distance to move
		
		// the relative dislocation
		double dx = distance * Math.cos(angleXY) * Math.cos(angleZ);
		double dy = distance * Math.sin(angleXY) * Math.cos(angleZ);
		double dz = distance * Math.sin(angleZ);

		// determine the number of rounds needed to reach the target
		remaining_hops = (int) Math.ceil(moveTime);
		// determine the moveVector which is added in each round to the position of this node
		moveVector = new Position(dx / moveTime, dy / moveTime, dz / moveTime);
	}
	
	/* (non-Javadoc)
	 * @see mobilityModels.MobilityModelInterface#getNextPos(nodes.Node)
	 */
	public Position getNextPos(Node n) {
		if(initialize) { // called the very first time such that not all nodes start moving in the first round of the simulation.
			// use a sample to determine in which phase we are.
			double wt = Math.abs(waitingTimeDistribution.nextSample());
			double mt = Math.abs(moveTimeDistribution.nextSample());
			double fraction = random.nextDouble() * (wt + mt);
			if(fraction < wt) {
				// the node starts waiting, but depending on fraction, may already have waited some time
				remaining_waitingTime = (int) Math.ceil(wt - fraction); // the remaining rounds to wait
				remaining_hops = 0;
			} else {
				// the node starts moving
				double speed = Math.abs(speedDistribution.nextSample()); // units per round
				initializeNextMove(n, speed, mt + wt - fraction);
			}
			currentPosition = n.getPosition(); // initially, currentPos is null
			initialize = false;
		}
		
		// restart a new move to a new destination if the node was moved by another means than this mobility model
		if(currentPosition != null) {
			if(!currentPosition.equals(n.getPosition())) {
				remaining_waitingTime = 0;
				remaining_hops = 0;
			}
		} else {
			currentPosition = new Position(0, 0, 0);
		}
		
		// execute the waiting loop
		if(remaining_waitingTime > 0) {
			remaining_waitingTime --;
			return n.getPosition();
		}
		// move
		if(remaining_hops == 0) { // we start to move, determine next random target
			// determine the next point to which this node moves to
			double speed = Math.abs(speedDistribution.nextSample()); // units per round
			double time = Math.abs(moveTimeDistribution.nextSample()); // rounds
			initializeNextMove(n, speed, time);
		}
		double newx = n.getPosition().xCoord + moveVector.xCoord; 
		double newy = n.getPosition().yCoord + moveVector.yCoord;
		double newz = n.getPosition().zCoord + moveVector.zCoord;
		
		// test that it is not outside the deployment area, otherwise reflect
		// We need to repeat the test for special cases where the node moves in really long
		// steps and is reflected more than once at the same border.
		boolean reflected = false;
		do {
			reflected = false;
			
			if(newx < 0) {
				newx *= -1;
				moveVector.xCoord *= -1;
				reflected = true;
			}
			if(newy < 0) {
				newy *= -1;
				moveVector.yCoord *= -1;
				reflected = true;
			}
			if(newz < 0) {
				newz *= -1;
				moveVector.zCoord *= -1;
				reflected = true;
			}
			if(newx > Configuration.dimX) {
				newx = 2*Configuration.dimX - newx;
				moveVector.xCoord *= -1;
				reflected = true;
			}
			if(newy > Configuration.dimY) {
				newy = 2*Configuration.dimY - newy;
				moveVector.yCoord *= -1;
				reflected = true;
			}
			if(newz > Configuration.dimZ) {
				newz = 2*Configuration.dimZ - newz;
				moveVector.zCoord *= -1;
				reflected = true;
			}
		} while(reflected);
		
		Position result = new Position(newx, newy, newz);

		if(remaining_hops <= 1) { // was last round of mobility
			// set the next waiting time that executes after this mobility phase
			remaining_waitingTime = (int) Math.ceil(Math.abs(waitingTimeDistribution.nextSample()));
			remaining_hops = 0;
		} else {
			remaining_hops --;
		}

		currentPosition.assign(result);
		return result;
	}
	
	/**
	 * The default constructor
	 * @see RandomWayPoint
	 * @throws CorruptConfigurationEntryException
	 */
	public RandomDirection() throws CorruptConfigurationEntryException {
		if(!initialized) {
			moveTimeDistribution = Distribution.getDistributionFromConfigFile("RandomDirection/MoveTime");
			speedDistribution = Distribution.getDistributionFromConfigFile("RandomDirection/NodeSpeed");
			waitingTimeDistribution = Distribution.getDistributionFromConfigFile("RandomDirection/WaitingTime");
			initialized = true;
		}
	}
}
