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
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.exception.SinalgoWrappedException;
import sinalgo.models.DistributionModel;
import sinalgo.nodes.Position;

/**
 * Aligns the nodes on a line.
 * <p>
 * By default, the nodes are placed on a horizontal line equally distributed
 * over the entire simulation. Optionally, the orientation of the line can be
 * specified in the configuration file with an entry as following:
 *
 * <pre>
 * &lt;DistributionModel&gt;
 * &lt;Line FromX="100" FromY="80" ToX="800" ToY="450"/&gt;
 * &lt;/DistributionModel&gt;
 * </pre>
 * <p>
 * where the nodes are placed on a line from (FromX,FromY) to (ToX,ToY). If only
 * one node is placed, it placed in the middle of the line.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class Line2D extends DistributionModel {

    private double dx;
    private double dy;
    private double previousPositionX;
    private double previousPositionY;

    @Override
    public void initialize() {
        if (Configuration.hasParameter("DistributionModel/Line/FromX")
                && Configuration.hasParameter("DistributionModel/Line/FromY")
                && Configuration.hasParameter("DistributionModel/Line/ToX")
                && Configuration.hasParameter("DistributionModel/Line/ToY")) {
            try {
                this.setPreviousPositionX(Configuration.getDoubleParameter("DistributionModel/Line/FromX"));
                this.setPreviousPositionY(Configuration.getDoubleParameter("DistributionModel/Line/FromY"));
                this.setDx(Configuration.getDoubleParameter("DistributionModel/Line/ToX") - this.getPreviousPositionX());
                this.setDy(Configuration.getDoubleParameter("DistributionModel/Line/ToY") - this.getPreviousPositionY());
            } catch (CorruptConfigurationEntryException e) {
                throw new SinalgoWrappedException(e);
            }
            if (this.numberOfNodes <= 1) { // place the single node in the middle
                this.setDx(this.getDx() / 2);
                this.setDy(this.getDy() / 2);
            } else {
                this.setDx(this.getDx() / (this.numberOfNodes - 1));
                this.setDy(this.getDy() / (this.numberOfNodes - 1));
                this.setPreviousPositionX(this.getPreviousPositionX() - this.getDx());
                this.setPreviousPositionY(this.getPreviousPositionY() - this.getDy());
            }
        } else { // default horizontal line
            this.setDy(0);
            this.setDx(((double) Configuration.getDimX()) / (this.numberOfNodes + 1));
            this.setPreviousPositionX(0);
            this.setPreviousPositionY(Configuration.getDimY() / 2);
        }
    }

    @Override
    public Position getNextPosition() {
        this.setPreviousPositionX(this.getPreviousPositionX() + this.getDx());
        this.setPreviousPositionY(this.getPreviousPositionY() + this.getDy());
        return new Position(this.getPreviousPositionX(), this.getPreviousPositionY(), 0);
    }

}
