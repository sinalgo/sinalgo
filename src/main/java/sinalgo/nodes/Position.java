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

import java.util.Objects;

/**
 * A simple vector implementation that describes the position of the nodes on
 * the deployment area.
 */
public class Position {

    /**
     * A tiny amount that may be added or substracted from a node's position to
     * avoid rounding effects.
     * <p>
     * Do always use this variable for such purposes, such that backtracking where
     * such rounding error corrections were made is possible.
     */
    public static double epsilonPosition = 10e-8;

    /**
     * The x coordinate of the position.
     */
    public double xCoord;

    /**
     * The y coordinate of the position.
     */
    public double yCoord;

    /**
     * The z coordinate of the position.
     */
    public double zCoord;

    /**
     * The constructor for the Position class.
     */
    public Position() {
        this(0, 0, 0);
    }

    /**
     * The constructor for the Position class.
     *
     * @param x The x coordinate of the node to create.
     * @param y The y coordinate of the node to create.
     * @param z The z coordinate of the node to create.
     */
    public Position(double x, double y, double z) {
        assign(x, y, z);
    }

    /**
     * Assigns this position the values of anohter position.
     *
     * @param p The position object from which to copy the x,y and z coordinates.
     */
    public void assign(Position p) {
        assign(p.xCoord, p.yCoord, p.zCoord);
    }

    /**
     * Assigns this position the values of anohter position.
     */
    public void assign(double x, double y, double z) {
        xCoord = x;
        yCoord = y;
        zCoord = z;
    }

    /**
     * This method returns the distance of a Position to another one.
     *
     * @param pos The other position to calculate the distance to.
     * @return The distance between the two positions.
     */
    public double distanceTo(Position pos) {
        return Math.sqrt(squareDistanceTo(pos));
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
        return ((xCoord - pos.xCoord) * (xCoord - pos.xCoord)) + ((yCoord - pos.yCoord) * (yCoord - pos.yCoord))
                + ((zCoord - pos.zCoord) * (zCoord - pos.zCoord));
    }

    @Override
    public String toString() {
        return "[" + xCoord + ", " + yCoord + ", " + zCoord + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Position position = (Position) o;
        return Double.compare(position.xCoord, xCoord) == 0 &&
                Double.compare(position.yCoord, yCoord) == 0 &&
                Double.compare(position.zCoord, zCoord) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(xCoord, yCoord, zCoord);
    }
}
