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
import sinalgo.configuration.Configuration;
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.models.MobilityModel;
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;
import sinalgo.runtime.Main;
import sinalgo.tools.statistics.Distribution;

import java.util.Random;

/**
 * Random Direction Mobility Model
 * <p>
 * The node selects an arbitrary direction and moves for a certain time in this
 * direction. If it encounters the border of the deployment area, it is
 * reflected just as a perfect billard ball.
 * <p>
 * The mobility consists of two phases: moving or waiting. The node decides to
 * move for a certain time in a given direction. Afterwards, it waits for a
 * certain time before it restarts moving.
 * <p>
 * The time during which the node moves, the waiting time, and the speed at
 * which the node moves can be described using distributions in the
 * configuration file. An entry in the configuration file may look as following:
 *
 * <pre>
 * &lt;RandomDirection&gt;
 * &nbsp;&lt;WaitingTime distribution="Poisson" lambda=10/&gt;
 * &nbsp;&lt;MoveTime distribution="Constant" constant="13"/&gt;
 * &nbsp;&lt;NodeSpeed distribution="Gaussian" mean="20" variance="20"/&gt;
 * &lt;/RandomDirection&gt;
 * </pre>
 *
 * <b>Note:</b> The speed is measured in distance-units per round, the waiting
 * and moving time in rounds. For all distributions, the absolute value of the
 * generated samples is taken.
 * <p>
 * The random direction mobility model has the advantage over the random
 * waypoint that it does not concentrate the nodes in the center of the
 * deployment area, but keeps a uniformly random distribution of the nodes.
 *
 * <b>SmoothStart:</b> This RandomDirection mobility model implements a smooth
 * start feature, allowing the nodes to start in the stationary case. I.e. it
 * avoids that at the beginning, all nodes start with a new mobility phase, by
 * randomly choosing for each node at the beginning whether it is waiting or
 * moving.
 * <p>
 * Further implementation notes:
 * <ul>
 * <li>If the waiting time expires between two rounds, the node continues
 * waiting until the next round starts.</li>
 * <li>The time a node moves is rounded up to the next integer value.</li>
 * <li>This implementation assumes that all nodes move according to the same
 * speed and waiting time distributions (it stores the distribution generators
 * in static fields used by all instances).</li>
 * <li>If the node is moved (either through the gui or any other means) between
 * two successive calls to getNextPos(), this mobility model asks for a new
 * position to walk to.</li>
 * <li>This mobility model works for 2D as well as for 3D.</li>
 * </ul>
 */
@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
public class RandomDirection extends MobilityModel {

    // we assume that these distributions are the same for all nodes
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private static Distribution speedDistribution; // how fast the nodes move

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private static Distribution waitingTimeDistribution; // how long nodes wait before starting the next mobility phase

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private static Distribution moveTimeDistribution; // for how long the node moves when it moves

    // a flag set to true after initialization of the static vars of this class has been done.
    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private static boolean initialized;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private static Random random = Distribution.getRandom(); // a random generator of the framework

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    // The vector that is added in each step to the current position of this node
    private Position moveVector;

    // the current position, to detect if the node has been moved
    // by other means than this mobility model between successive calls to getNextPos()
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private Position currentPosition;

    private int remaining_hops; // the remaining hops until a new path has to be determined
    private int remaining_waitingTime;

    private boolean initialize = true; // to detect very first time to start smoothly

    /**
     * Initializes the next move by determining the next point to move to. This new
     * destination may be outside the deployment area. This method also determines
     * the vector along which the nodes move in each step and the number of rounds
     * that the move will take.
     *
     * @param moveSpeed The speed at which the node will move.
     * @param moveTime  The time during which the node is supposed to move
     */
    private void initializeNextMove(double moveSpeed, double moveTime) {
        double angleXY = 2 * Math.PI * getRandom().nextDouble(); // 0 .. 360
        double angleZ = Math.PI * (0.5 - getRandom().nextDouble()); // -90 .. 90
        if (Main.getRuntime().getTransformator().getNumberOfDimensions() == 2) {
            angleZ = 0; // remain in the XY-plane
        }
        double distance = moveTime * moveSpeed; // the distance to move

        // the relative dislocation
        double dx = distance * Math.cos(angleXY) * Math.cos(angleZ);
        double dy = distance * Math.sin(angleXY) * Math.cos(angleZ);
        double dz = distance * Math.sin(angleZ);

        // determine the number of rounds needed to reach the target
        this.setRemaining_hops((int) Math.ceil(moveTime));
        // determine the moveVector which is added in each round to the position of this
        // node
        this.setMoveVector(new Position(dx / moveTime, dy / moveTime, dz / moveTime));
    }

    @Override
    public Position getNextPos(Node n) {
        if (this.initialize) { // called the very first time such that not all nodes start moving in the first
            // round of the simulation.
            // use a sample to determine in which phase we are.
            double wt = Math.abs(getWaitingTimeDistribution().nextSample());
            double mt = Math.abs(getMoveTimeDistribution().nextSample());
            double fraction = getRandom().nextDouble() * (wt + mt);
            if (fraction < wt) {
                // the node starts waiting, but depending on fraction, may already have waited
                // some time
                this.setRemaining_waitingTime((int) Math.ceil(wt - fraction)); // the remaining rounds to wait
                this.setRemaining_hops(0);
            } else {
                // the node starts moving
                double speed = Math.abs(getSpeedDistribution().nextSample()); // units per round
                this.initializeNextMove(speed, mt + wt - fraction);
            }
            this.setCurrentPosition(n.getPosition()); // initially, currentPos is null
            this.setInitialize(false);
        }

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

        // execute the waiting loop
        if (this.getRemaining_waitingTime() > 0) {
            this.setRemaining_waitingTime(this.getRemaining_hops() - 1);
            return n.getPosition();
        }
        // move
        if (this.remaining_hops == 0) { // we start to move, determine next random target
            // determine the next point to which this node moves to
            double speed = Math.abs(getSpeedDistribution().nextSample()); // units per round
            double time = Math.abs(getMoveTimeDistribution().nextSample()); // rounds
            this.initializeNextMove(speed, time);
        }
        double newx = n.getPosition().getXCoord() + this.moveVector.getXCoord();
        double newy = n.getPosition().getYCoord() + this.moveVector.getYCoord();
        double newz = n.getPosition().getZCoord() + this.moveVector.getZCoord();

        // test that it is not outside the deployment area, otherwise reflect
        // We need to repeat the test for special cases where the node moves in really
        // long
        // steps and is reflected more than once at the same border.
        boolean reflected;
        do {
            reflected = false;

            if (newx < 0) {
                newx *= -1;
                this.moveVector.setXCoord(this.moveVector.getXCoord() * -1);
                reflected = true;
            }
            if (newy < 0) {
                newy *= -1;
                this.moveVector.setYCoord(this.moveVector.getYCoord() * -1);
                reflected = true;
            }
            if (newz < 0) {
                newz *= -1;
                this.moveVector.setZCoord(this.moveVector.getZCoord() * -1);
                reflected = true;
            }
            if (newx > Configuration.getDimX()) {
                newx = 2 * Configuration.getDimX() - newx;
                this.moveVector.setXCoord(this.moveVector.getXCoord() * -1);
                reflected = true;
            }
            if (newy > Configuration.getDimY()) {
                newy = 2 * Configuration.getDimY() - newy;
                this.moveVector.setYCoord(this.moveVector.getYCoord() * -1);
                reflected = true;
            }
            if (newz > Configuration.getDimZ()) {
                newz = 2 * Configuration.getDimZ() - newz;
                this.moveVector.setZCoord(this.moveVector.getZCoord() * -1);
                reflected = true;
            }
        } while (reflected);

        Position result = new Position(newx, newy, newz);

        if (this.remaining_hops <= 1) { // was last round of mobility
            // set the next waiting time that executes after this mobility phase
            this.remaining_waitingTime = (int) Math.ceil(Math.abs(getWaitingTimeDistribution().nextSample()));
            this.remaining_hops = 0;
        } else {
            this.remaining_hops--;
        }

        this.currentPosition.assign(result);
        return result;
    }

    /**
     * The default constructor
     *
     * @throws CorruptConfigurationEntryException if the configuratin is corrupt.
     * @see RandomWayPoint
     */
    public RandomDirection() throws CorruptConfigurationEntryException {
        if (!isInitialized()) {
            setMoveTimeDistribution(Distribution.getDistributionFromConfigFile("RandomDirection/MoveTime"));
            setSpeedDistribution(Distribution.getDistributionFromConfigFile("RandomDirection/NodeSpeed"));
            setWaitingTimeDistribution(Distribution.getDistributionFromConfigFile("RandomDirection/WaitingTime"));
            setInitialized(true);
        }
    }
}
