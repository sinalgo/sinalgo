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
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;

/**
 * Perfect simulation with random way point - this mobility model starts in the
 * stationary distribution of the RWP. Therefore, the initial placement of the
 * nodes is not considered and the nodes may start in the waiting phase or
 * mobility phase.
 * <p>
 * In order to obtain the stationary distribution, the following steps are
 * performed the very first time the node is asked to return its position, that
 * is, when the first round starts.
 * <ul>
 * <li>Each node picks two random (uniformely distributed) points p1 and p2 in
 * the deployment area.</li>
 * <li>We determine a sample of a waiting time (wt) and the time to move from p1
 * to p2 (mt).</li>
 * <li>We pick a random time f in the interval [0, wt+mt].</li>
 * <li>If f < wt, the node starts waiting for (wt - f) rounds. Its position is a
 * random point on the line p1-p2.</li>
 * <li>If f > wt, the node is placed to p1 + (p2 - p1)* a, where a = (f-wt)/mt,
 * which denotes the fraction of the distance p1-p2 already moved during time
 * (f-wt). In the following rounds, the node continues moving towards p2.</li>
 * </ul>
 *
 * <b>Note:</b> This special step only happens in the first round. Afterwards,
 * the node moves just as with the normal random way point mobility model.
 * <p>
 * To use <code>PerfectRWP</code>, you may use any initial distribution model,
 * as the initial placement of the nodes is not considered.
 *
 * @see projects.defaultProject.models.mobilityModels.RandomWayPoint For any
 * further details of the implementation of the underlying random way point
 * mobility model.
 */
public class PerfectRWP extends RandomWayPoint {

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private boolean initialize = true; // smooth start

    @Override
    public Position getNextPos(Node n) {
        if (this.isInitialize()) {
            this.setInitialize(false);

            double speed = Math.abs(getSpeedDistribution().nextSample()); // units per round
            double wt = Math.ceil(getWaitingTimeDistribution().nextSample()); // potential waiting time
            Position startPos = this.getNextWayPoint();
            this.setNextDestination(this.getNextWayPoint());
            double mt = startPos.distanceTo(this.getNextDestination()) / speed; // time of the move
            double fraction = (wt + mt) * getRandom().nextDouble();
            double dx = this.getNextDestination().getXCoord() - startPos.getXCoord();
            double dy = this.getNextDestination().getYCoord() - startPos.getYCoord();
            if (fraction < wt) {
                // start waiting
                this.setRemaining_waitingTime((int) Math.ceil(wt - fraction));
                this.setRemaining_hops(0);

                double movedFraction = getRandom().nextDouble();
                startPos.setXCoord(startPos.getXCoord() + dx * movedFraction);
                startPos.setYCoord(startPos.getYCoord() + dy * movedFraction);
                return startPos; // don't consider initial distribution
            } else {
                double movedFraction = (fraction - wt) / mt; // how far the node has already moved on the line [0..1]
                // the current position
                startPos.setXCoord(startPos.getXCoord() + dx * movedFraction);
                startPos.setYCoord(startPos.getYCoord() + dy * movedFraction);

                // remaining vector to move
                dx *= (1 - movedFraction);
                dy *= (1 - movedFraction);

                // determine the number of rounds needed to reach the target
                double dist = this.getNextDestination().distanceTo(startPos);
                double rounds = dist / speed;
                this.setRemaining_hops((int) Math.ceil(rounds));
                // determine the moveVector which is added in each round to the position of this
                // node
                this.setMoveVector(new Position(dx / rounds, dy / rounds, 0));
                this.setRemaining_waitingTime(0);
                return startPos;
            }
        }
        return super.getNextPos(n);
    }

    /**
     * Creates a new perfect random way point object, and reads the speed
     * distribution and waiting time distribution configuration from the XML config
     * file.
     *
     * @throws CorruptConfigurationEntryException if the configuration is corrupt.
     */
    public PerfectRWP() throws CorruptConfigurationEntryException {
        super();
    }

}
