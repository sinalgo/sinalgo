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
 * A uniform distribution which returns random values uniformly distributed in
 * the range [min, max].
 * <p>
 * If the distribution is specified in the xml configuration file, an entry like
 *
 * <pre>
 * &lt;mainTagName distribution="Uniform" min="xxx" max="yyy"/&gt;
 * </pre>
 * <p>
 * is expected.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class UniformDistribution extends Distribution {

    private double min; // the min value of the range to choose a value from
    private double range; // the size of the range.

    /**
     * Creates a new Uniform sample generator which chooses the samples from the
     * range [min, max].
     *
     * @param min The minimum value of the range to choose a value from.
     * @param max The maximum value of the range to choose a value from.
     * @throws NumberFormatException If min > max.
     */
    public UniformDistribution(double min, double max) throws NumberFormatException {
        this.setMin(min);
        this.setRange(max - min);
        if (this.getRange() < 0) {
            throw new NumberFormatException(
                    "Invalid arguments to create a uniform distribution. The upper bound of the range must be at least as big as the lower bound.");
        }
    }

    /**
     * Creates a new uniform distribution and initializes it from the XML
     * configuration file.
     *
     * @param mainTagPath The entry-path which points to the entry in the XML configuration
     *                    file which contains the specifications for this distribution.
     * @throws CorruptConfigurationEntryException If the configuration file is corrupt.
     */
    public UniformDistribution(String mainTagPath) throws CorruptConfigurationEntryException {
        this.setMin(Configuration.getDoubleParameter(mainTagPath + "/min"));
        this.setRange(Configuration.getDoubleParameter(mainTagPath + "/max") - this.getMin());
        if (this.getRange() < 0) {
            throw new CorruptConfigurationEntryException(
                    "Invalid arguments to create a uniform distribution. The upper bound of the range must be at least as big as the lower bound.");
        }
    }

    @Override
    public double nextSample() {
        return this.getMin() + this.getRange() * getRandomGenerator().nextDouble();
    }

    /**
     * Creates a random sample drawn from a uniform distribution of a given range.
     *
     * @param minRange The minimum value of the interval the sample is drawn from
     * @param maxRange The maximum value of the interval the sample is drawn from
     * @return a random sample drawn from a uniform distribution of a given range.
     */
    public static double nextUniform(double minRange, double maxRange) {
        Random r = Distribution.getRandom();
        return minRange + r.nextDouble() * (maxRange - minRange);
    }

}
