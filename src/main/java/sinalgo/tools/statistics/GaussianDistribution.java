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
package sinalgo.tools.statistics;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.configuration.Configuration;
import sinalgo.exception.CorruptConfigurationEntryException;

import java.util.Random;

/**
 * A gaussian distribution sample generator.
 * <p>
 * If the distribution is specified in the xml configuration file, an entry like
 *
 * <pre>
 * &lt;mainTagName distribution="Gaussian" mean="xxx" variance="yyy"/&gt;
 * </pre>
 * <p>
 * is expected.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class GaussianDistribution extends Distribution {

    private double mean; // the mean of the distribution
    private double var; // the variance of the distribution

    /**
     * Creates a new gaussian distribution sample generator.
     *
     * @param mean The mean of the distribution.
     * @param var  The variance of the distribution.
     */
    public GaussianDistribution(double mean, double var) {
        this.setMean(mean);
        this.setVar(var);
    }

    /**
     * Creates a new Gaussian distribution and initializes it from the XML
     * configuration file.
     *
     * @param mainTagPath The entry-path which points to the entry in the XML configuration
     *                    file which contains the specifications for this distribution.
     * @throws CorruptConfigurationEntryException If the configuration file is corrupt.
     */
    public GaussianDistribution(String mainTagPath) throws CorruptConfigurationEntryException {
        this.setMean(Configuration.getDoubleParameter(mainTagPath + "/mean"));
        this.setVar(Configuration.getDoubleParameter(mainTagPath + "/variance"));
    }

    @Override
    public double nextSample() {
        return this.getMean() + getRandomGenerator().nextGaussian() * Math.sqrt(this.getVar());
    }

    /**
     * Creates a random sample drawn from an gaussian distribution with given mean
     * and variance.
     *
     * @param mean     The mean of the gaussian distribution
     * @param variance The variance of the gaussian distribution
     * @return a random sample drawn from an gaussian distribution with given mean
     * and variance.
     */
    public static double nextGaussian(double mean, double variance) {
        Random r = Distribution.getRandom();
        return mean + r.nextGaussian() * Math.sqrt(variance);
    }

}
