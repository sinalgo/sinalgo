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
package projects.defaultProject.models.messageTransmissionModels;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;
import sinalgo.tools.statistics.Distribution;

/**
 * Dummy message transmission model whose delivery time is defined through a
 * distribution.
 * <p>
 * This class expects an entry in the configuration file that describes the
 * distribution of the delivery time.
 *
 * <pre>
 * &lt;RandomMessageTransmission distribution="Uniform" min="0.1" max="0.8"/&gt;
 * </pre>
 * <p>
 * If the distribution returns a value smaller or equal to 0, the transmission
 * time is set to the small positive value 1e-9.
 */
public class RandomTime extends sinalgo.models.MessageTransmissionModel {

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private Distribution dist;

    /**
     * Creates a new RandomTime transmission model instance and reads the config for
     * this object from the config file.
     *
     * @throws CorruptConfigurationEntryException if the configuration is corrupt.
     */
    public RandomTime() throws CorruptConfigurationEntryException {
        this.setDist(Distribution.getDistributionFromConfigFile("RandomMessageTransmission"));
    }

    @Override
    public double timeToReach(Node startNode, Node endNode, Message msg) {
        double time = this.getDist().nextSample();
        if (time <= 0) {
            time = 1e-9;
        }
        return time;
    }

}
