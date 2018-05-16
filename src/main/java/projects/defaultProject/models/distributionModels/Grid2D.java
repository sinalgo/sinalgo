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
package projects.defaultProject.models.distributionModels;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.configuration.Configuration;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.models.DistributionModel;
import sinalgo.nodes.Position;

/**
 * Aligns the nodes about equally spaced on a gird covering the entire
 * deployment area.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class Grid2D extends DistributionModel {

    private double size; // the cell-size of the gird
    private int numNodesPerLine; // number of nodes on the x-axis
    private int i;
    private int j; // loop counters

    @Override
    public void initialize() {
        double a = 1 - this.numberOfNodes;
        double b = -(Configuration.getDimX() + Configuration.getDimY()); // kind of a hack
        double c = Configuration.getDimX() * Configuration.getDimY();
        double tmp = b * b - 4 * a * c;
        if (tmp < 0) {
            throw new SinalgoFatalException("negative sqrt");
        }
        this.setSize((-b - Math.sqrt(tmp)) / (2 * a));
        this.setNumNodesPerLine((int) Math.round(Configuration.getDimX() / this.getSize()) - 1);
        this.setI(0);
        this.setJ(1);
    }

    @Override
    public Position getNextPosition() {
        this.setI(this.getI() + 1);
        if (this.getI() > this.getNumNodesPerLine()) {
            this.setI(1);
            this.setJ(this.getJ() + 1);
        }
        return new Position(this.getI() * this.getSize(), this.getJ() * this.getSize(), 0);
    }

}
