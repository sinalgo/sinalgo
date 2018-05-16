/*
BSD 3-Clause License

Copyright (c) 2007-2013, Distributed Computing Group (DCG)
                         ETH Zurich
                         Switzerland
                         dcg.ethz.ch
              2017-2018, André Brait

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the copyright holder nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package projects.defaultProject.models.mobilityModels;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.models.MobilityModel;
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;
import sinalgo.tools.Tools;
import sinalgo.tools.statistics.Distribution;

import java.util.Random;

/**
 * Random Way Point Mobility Model
 * <p>
 * The node selects a random point in the simulation area and moves there with
 * constant speed. When arrived at the position, the node waits for a predefined
 * amount of time and then selects the next point to move to.
 * <p>
 * Both, the speed and the waiting time can be described using a random-number
 * generator that returns values according to a given distribution probability.
 * The distribions must be specified in the XML configuration file, and may look
 * as following:
 *
 * <pre>
 * &lt;RandomWayPoint&gt;
 * &nbsp; &lt;Speed distribution="Gaussian" mean="40" variance="10"/&gt;
 * &nbsp; &lt;WaitingTime distribution="Poisson" lambda="10"/&gt;
 * &lt;/RandomWayPoint&gt;
 * </pre>
 *
 *
 * <b>Note:</b> The speed is measured in distance-units per round, the waiting
 * time in rounds. For the speed, the absolute value of the sample produced by
 * the speed-distribution is used.
 * <p>
 * Note that with the random way point mobility model, nodes tend to gather in
 * the center of the deployment area. I.e. the node densitity changes over time,
 * even if you have an initial random placement of the nodes. To circumvent this
 * problem, consider using the random direction mobiltiy model.
 * <p>
 * Further implementation notes:
 * <ul>
 * <li>If the waiting time expires between two rounds, the node continues
 * waiting until the next round starts.</li>
 * <li>If a node arrives at its destination between two rounds, it starts
 * waiting, but this time is not counted towards the waiting time.</li>
 * <li>This implementation assumes that all nodes move according to the same
 * speed and waiting time distributions (it stores the distribution generators
 * in static fields used by all instances).</li>
 * <li>If the node is moved (either through the gui or any other means) between
 * two successive calls to getNextPos(), this mobility model asks for a new
 * position to walk to.</li>
 * <li>This mobility model works for 2D as well as for 3D.</li>
 * </ul>
 *
 * @see projects.defaultProject.models.mobilityModels.PerfectRWP For a perfect
 * random waypoint mobility model which ensures that the simulation only
 * performs in the stationary regime, starting from the first round.
 */
@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
public class RandomWayPoint extends MobilityModel {

    // we assume that these distributions are the same for all nodes
    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private static Distribution speedDistribution;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private static Distribution waitingTimeDistribution;

    // a flag set to true after initialization of the static vars of this class has been done.
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private static boolean initialized;

    // a random generator of the framework
    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private static Random random = Distribution.getRandom();

    // The point where this node is moving to
    private Position nextDestination = new Position();

    // The vector that is added in each step to the current position of this node
    private Position moveVector = new Position();

    // the current position, to detect if the node has been moved by other means than this mobility model between successive calls to getNextPos()
    private Position currentPosition;

    // the remaining hops until a new path has to be determined
    private int remaining_hops;

    private int remaining_waitingTime;

    @Override
    public Position getNextPos(Node n) {
        // restart a new move to a new destination if the node was moved by another
        // means than this mobility model
        if (this.getCurrentPosition() != null) {
            if (!this.getCurrentPosition().equals(n.getPosition())) {
                this.setRemaining_waitingTime(0);
                this.setRemaining_hops(0);
            }
        } else {
            this.setCurrentPosition(new Position(0, 0, 0));
        }

        Position nextPosition = new Position();

        // execute the waiting loop
        if (this.remaining_waitingTime > 0) {
            this.remaining_waitingTime--;
            return n.getPosition();
        }

        if (this.remaining_hops == 0) {
            // determine the speed at which this node moves
            double speed = Math.abs(getSpeedDistribution().nextSample()); // units per round

            // determine the next point where this node moves to
            this.nextDestination = this.getNextWayPoint();

            // determine the number of rounds needed to reach the target
            double dist = this.nextDestination.distanceTo(n.getPosition());
            double rounds = dist / speed;
            this.remaining_hops = (int) Math.ceil(rounds);
            // determine the moveVector which is added in each round to the position of this
            // node
            double newx = (this.nextDestination.getXCoord() - n.getPosition().getXCoord()) / rounds;
            double newy = (this.nextDestination.getYCoord() - n.getPosition().getYCoord()) / rounds;
            double newz = (this.nextDestination.getZCoord() - n.getPosition().getZCoord()) / rounds;
            this.moveVector.assign(newx, newy, newz);
        }
        if (this.remaining_hops <= 1) { // don't add the moveVector, as this may move over the destination.
            nextPosition.assign(this.nextDestination);
            // set the next waiting time that executes after this mobility phase
            this.remaining_waitingTime = (int) Math.ceil(getWaitingTimeDistribution().nextSample());
            this.remaining_hops = 0;
        } else {
            double newx = n.getPosition().getXCoord() + this.moveVector.getXCoord();
            double newy = n.getPosition().getYCoord() + this.moveVector.getYCoord();
            double newz = n.getPosition().getZCoord() + this.moveVector.getZCoord();
            nextPosition.assign(newx, newy, newz);
            this.remaining_hops--;
        }
        this.currentPosition.assign(nextPosition);
        return nextPosition;
    }

    /**
     * Determines the next waypoint where this node moves after having waited. The
     * position is expected to be within the deployment area.
     *
     * @return the next waypoint where this node moves after having waited.
     */
    protected Position getNextWayPoint() {
        return Tools.getRandomPosition(getRandom());
    }

    /**
     * Creates a new random way point object, and reads the speed distribution and
     * waiting time distribution configuration from the XML config file.
     *
     * @throws CorruptConfigurationEntryException When a needed configuration entry is missing.
     */
    public RandomWayPoint() throws CorruptConfigurationEntryException {
        if (!isInitialized()) {
            setSpeedDistribution(Distribution.getDistributionFromConfigFile("RandomWayPoint/Speed"));
            setWaitingTimeDistribution(Distribution.getDistributionFromConfigFile("RandomWayPoint/WaitingTime"));
            setInitialized(true);
        }
    }
}
