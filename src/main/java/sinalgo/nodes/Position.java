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
package sinalgo.nodes;

import lombok.*;

/**
 * A simple vector implementation that describes the position of the nodes on
 * the deployment area.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Position {

    /**
     * A tiny amount that may be added or substracted from a node's position to
     * avoid rounding effects.
     * <p>
     * Do always use this variable for such purposes, such that backtracking where
     * such rounding error corrections were made is possible.
     */
    @Getter
    @Setter
    private static double epsilonPosition = 10e-8;

    /**
     * The x coordinate of the position.
     *
     * @param xCoord new x coordinate for this position.
     * @return The x coordinate of the position.
     */
    private double xCoord;

    /**
     * The y coordinate of the position.
     *
     * @param yCoord new y coordinate for this position.
     * @return The y coordinate of the position.
     */
    private double yCoord;

    /**
     * The z coordinate of the position.
     *
     * @param zCoord new z coordinate for this position.
     * @return The z coordinate of the position.
     */
    private double zCoord;

    /**
     * Assigns this position the values of anohter position.
     *
     * @param p The position object from which to copy the x,y and z coordinates.
     */
    public void assign(Position p) {
        this.assign(p.getXCoord(), p.getYCoord(), p.getZCoord());
    }

    /**
     * Assigns this position the values of anohter position.
     */
    public void assign(double x, double y, double z) {
        this.setXCoord(x);
        this.setYCoord(y);
        this.setZCoord(z);
    }

    /**
     * This method returns the distance of a Position to another one.
     *
     * @param pos The other position to calculate the distance to.
     * @return The distance between the two positions.
     */
    public double distanceTo(Position pos) {
        return Math.sqrt(this.squareDistanceTo(pos));
    }

    /**
     * Determines the squared distance from this position to another position
     * <p>
     * Use this method for comparison, as it is faster because it does not take the
     * square-root.
     *
     * @param pos The other position
     * @return The squared distance between this point and another point.
     */
    public double squareDistanceTo(Position pos) {
        return ((this.getXCoord() - pos.getXCoord()) * (this.getXCoord() - pos.getXCoord()))
                + ((this.getYCoord() - pos.getYCoord()) * (this.getYCoord() - pos.getYCoord()))
                + ((this.getZCoord() - pos.getZCoord()) * (this.getZCoord() - pos.getZCoord()));
    }

    @Override
    public String toString() {
        return "[" + this.getXCoord() + ", " + this.getYCoord() + ", " + this.getZCoord() + "]";
    }

}
