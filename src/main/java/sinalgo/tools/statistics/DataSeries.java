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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * A statistics tool that allows to determine simple statistic properties such
 * as the mean and standard deviation of a series of measurements.
 * <p>
 * For each series you want to have a statistical analysis on, create a new
 * object of this class and add the samples using the <code>addSample</code>
 * method.
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class DataSeries implements Externalizable {

    private static final long serialVersionUID = 2822762510760348852L;

    /**
     * The sum of all samples added to this data series.
     *
     * @return The sum of all samples added to this data series.
     */
    private double sum; // The sum of all samples

    private double squaredSum; // the sum of the square of all samples

    /**
     * The number of samples added to this data series.
     *
     * @return the number of samples added to this data series.
     */
    private int numberOfSamples;

    @Getter(AccessLevel.PRIVATE)
    private double min = Double.MAX_VALUE, max = Double.MIN_VALUE; // the min. and max. values added

    /**
     * Default constructor, creates a new statisitc object.
     */
    public DataSeries() {
    }

    /**
     * Resets this data series object by removing all added samples. After calling
     * this method, the object is as when it was newly allocated.
     */
    public void reset() {
        this.setSum(0);
        this.setSquaredSum(0);
        this.setNumberOfSamples(0);
        this.setMin(Double.MAX_VALUE);
        this.setMax(Double.MIN_VALUE);
    }

    /**
     * Adds a new sample to this series.
     *
     * @param value The new value to be added to this series.
     */
    public void addSample(double value) {
        this.setMin(Math.min(this.getMin(), value));
        this.setMax(Math.max(this.getMax(), value));
        this.setSum(this.getSum() + value);
        this.setSquaredSum(this.getSquaredSum() + value * value);
        this.setNumberOfSamples(this.getNumberOfSamples() + 1);
    }

    /**
     * Adds all samples added to a DataSeries also to this DataSeries.
     *
     * @param ds DataSeries to which the samples will be added
     */
    public void addSamples(DataSeries ds) {
        this.setSum(this.getSum() + ds.getSum());
        this.setSquaredSum(this.getSquaredSum() + ds.getSquaredSum());
        this.setNumberOfSamples(this.getNumberOfSamples() + ds.getNumberOfSamples());
        this.setMin(Math.min(this.getMin(), ds.getMin()));
        this.setMax(Math.max(this.getMax(), ds.getMax()));
    }

    /**
     * Returns the mean of the values added so far to this series.
     *
     * @return The mean of the values added so far to this series, 0 if no samples
     * were added.
     */
    public double getMean() {
        // The mean of n samples is \sum{samples} / n
        if (this.getNumberOfSamples() > 0) {
            return this.getSum() / this.getNumberOfSamples();
        } else {
            return 0;
        }
    }

    /**
     * Returns the variance of the values added so far to this series.
     *
     * @return The variance of the values added so far to this series, 0 if no
     * samples were added.
     */
    public double getVariance() {
        // The variance of n samples equals to E( (X - E(X))^2) = E(X^2) - (E(X))^2
        if (this.getNumberOfSamples() > 0) {
            double currentMean = this.getMean();
            return this.getSquaredSum() / this.getNumberOfSamples() - (currentMean * currentMean);
        } else {
            return 0;
        }
    }

    /**
     * Returns the standard deviation of the values added so far to this series.
     *
     * @return The standard deviation of the values added so far to this series.
     */
    public double getStandardDeviation() {
        return Math.sqrt(this.getVariance());
    }

    /**
     * @return The value of the smallest sample, 0 if no sample was added.
     */
    public double getMinimum() {
        return this.getNumberOfSamples() == 0 ? 0 : this.getMin();
    }

    /**
     * @return The value of the largest sample, 0 if no sample was added.
     */
    public double getMaximum() {
        return this.getNumberOfSamples() == 0 ? 0 : this.getMax();
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        this.setSum(in.readDouble());
        this.setSquaredSum(in.readDouble());
        this.setNumberOfSamples(in.readInt());
        this.setMin(in.readDouble());
        this.setMax(in.readDouble());
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeDouble(this.sum);
        out.writeDouble(this.squaredSum);
        out.writeInt(this.numberOfSamples);
        out.writeDouble(this.min);
        out.writeDouble(this.max);
    }
}
