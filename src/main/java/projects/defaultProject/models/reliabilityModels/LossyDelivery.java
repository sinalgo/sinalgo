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
package projects.defaultProject.models.reliabilityModels;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.configuration.Configuration;
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.models.ReliabilityModel;
import sinalgo.nodes.messages.Packet;
import sinalgo.tools.statistics.Distribution;

import java.util.Random;

/**
 * A loossy reliability model that drops messages with a constant probability.
 * <p>
 * The percentage of dropped messages has to be specified in the configuration
 * file:
 * <p>
 * &lt;LossyDelivery dropRate="..."/&gt;
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class LossyDelivery extends ReliabilityModel {

    private Random rand = Distribution.getRandom();
    private double dropRate; // default is 0

    @Override
    public boolean reachesDestination(Packet p) {
        double r = this.getRand().nextDouble();
        return (r > this.getDropRate());
    }

    /**
     * Creates a new Drop Rate Reliability Model instance.
     */
    public LossyDelivery() {
        try {
            this.setDropRate(Configuration.getDoubleParameter("LossyDelivery/dropRate"));
        } catch (CorruptConfigurationEntryException e) {
            throw new SinalgoFatalException("Missing configuration entry for the Message Transmission Model:\n" + e.getMessage());
        }
    }

}
