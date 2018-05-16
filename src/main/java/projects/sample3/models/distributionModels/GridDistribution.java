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
package projects.sample3.models.distributionModels;

import sinalgo.configuration.Configuration;
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.models.DistributionModel;
import sinalgo.nodes.Position;
import sinalgo.tools.statistics.Distribution;

import java.util.Random;
import java.util.Vector;

/**
 * A distribution model that distributes the nodes on a regular grid depending
 * on the geometric node collections rMax. It choses the postitions so that the
 * field is optimally covered by the nodes. This means that the grid distances
 * are sqrt(2)*rMax. This completely covers the area between the nodes as there
 * is no point between the four nodes having a distance to all the nodes that is
 * bigger than rMax. If there are nodes left after having a node on every grid
 * position the model distributes the other nodes randomly on the field.
 */
public class GridDistribution extends DistributionModel {

    private Random rand = Distribution.getRandom();

    private double radius;

    private Vector<Position> positions = new Vector<>();
    private int returnNum;

    @Override
    public void initialize() {
        try {
            this.radius = Configuration.getDoubleParameter("GeometricNodeCollection/rMax");
        } catch (CorruptConfigurationEntryException e) {
            e.printStackTrace();
        }
        double horizontalFactor = (Configuration.getDimX() - 2 * this.radius) / (this.radius * 1.414);
        double verticalFactor = (Configuration.getDimY() - 2 * this.radius) / (this.radius * 1.414);

        int ihF = (int) horizontalFactor;
        int ivF = (int) verticalFactor;

        int number = 0;

        for (int i = 0; i < ihF + 1; i++) {
            for (int j = 0; j < ivF + 1; j++) {
                if (number < this.numberOfNodes) {
                    this.positions.add(new Position(this.radius + i * (this.radius * 1.414), this.radius + j * (this.radius * 1.414), 0));
                }
            }
        }
    }

    @Override
    public Position getNextPosition() {
        if (this.returnNum < this.positions.size()) {
            return this.positions.elementAt(this.returnNum++);
        } else {
            double randomPosX = this.rand.nextDouble() * Configuration.getDimX();
            double randomPosY = this.rand.nextDouble() * Configuration.getDimY();
            return new Position(randomPosX, randomPosY, 0);
        }
    }

    @Override
    public void setParamString(String s) {
    }

}
