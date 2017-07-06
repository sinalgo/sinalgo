/*
 Copyright (c) 2007, Distributed Computing Group (DCG)
                    ETH Zurich
                    Switzerland
                    dcg.ethz.ch

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the
   distribution.

 - Neither the name 'Sinalgo' nor the names of its contributors may be
   used to endorse or promote products derived from this software
   without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package sinalgo.tools.statistics;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * A statistics tool that allows to determine simple statistic properties such as the 
 * mean and standard deviation of a series of measurements.
 * 
 * For each series you want to have a statistical analysis on, create a new object 
 * of this class and add the samples using the <code>addSample</code> method.
 */
public class DataSeries implements Externalizable {
	private static final long serialVersionUID = 2822762510760348852L;
	
	private double sum = 0; // The sum of all samples
	private double squared_sum = 0; // the sum of the square of all samples
	private int num_samples = 0; // The number of samples added so far
	private double min = Double.MAX_VALUE, max = Double.MIN_VALUE; // the min. and max. values added 
	

	/**
	 * Default constructor, creates a new statisitc object.  
	 */
	public DataSeries() {
	}
	
	/**
	 * Resets this data series object by removing all added samples. After calling this
	 * method, the object is as when it was newly allocated. 
	 */
	public void reset() {
		sum = 0;
		squared_sum = 0;
		num_samples = 0;
		min = Double.MAX_VALUE;
		max = Double.MIN_VALUE;
	}
	
	/**
	 * Adds a new sample to this series.  
	 * @param value The new value to be added to this series. 
	 */
	public void addSample(double value) {
		if(value < min) {
			min = value;
		}
		if(value > max) {
			max = value;
		}
		sum += value;
		squared_sum += value * value;
		num_samples++;
	}
	
	/**
	 * Adds all samples added to a DataSeries also to this
	 * DataSeries.
	 * @param ds
	 */
	public void addSamples(DataSeries ds) {
		sum += ds.sum;
		squared_sum += ds.squared_sum;
		num_samples += ds.num_samples;
		min = Math.min(min, ds.min);
		max = Math.max(max, ds.max);
	}
	
	/**
	 * Returns the mean of the values added so far to this series.
	 * @return The mean of the values added so far to this series, 0 if no samples were added.
	 */
	public double getMean() {
		// The mean of n samples is \sum{samples} / n
		if(num_samples > 0) {
			return sum / num_samples;
		} else {
			return 0;
		}
	}
	
	/**
	 * Returns the variance of the values added so far to this series. 
	 * @return The variance of the values added so far to this series, 0 if no samples were added. 
	 */
	public double getVariance() {
		// The variance of n samples equals to E( (X - E(X))^2) = E(X^2) - (E(X))^2
		if(num_samples > 0) {
			return squared_sum / num_samples - (getMean()*getMean());
		} else {
			return 0;
		}
	}
	
	/**
	 * Returns the standard deviation of the values added so far to this series. 
	 * @return The standard deviation of the values added so far to this series. 
	 */
	public double getStandardDeviation() {
		return Math.sqrt(getVariance());
	}
	
	/**
	 * Returns the number of samples added to this data series.
	 * @return the number of samples added to this data series.
	 */
	public int getNumberOfSamples() {
		return num_samples;
	}
	
	/**
	 * Returns the sum of all samples added to this data series.
	 * @return The sum of all samples added to this data series.
	 */
	public double getSum() {
		return sum;
	}
	
	/**
	 * @return The value of the smallest sample, 0 if no sample was added.
	 */
	public double getMinimum() {
		return num_samples == 0 ? 0 : min;
	}
	
	/**
	 * @return The value of the largest sample, 0 if no sample was added.
	 */
	public double getMaximum() {
		return num_samples == 0 ? 0 : max;
	}

	/* (non-Javadoc)
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sum = in.readDouble();
		squared_sum = in.readDouble();
		num_samples = in.readInt();
		min = in.readDouble();
		max = in.readDouble();
	}

	/* (non-Javadoc)
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeDouble(sum);
		out.writeDouble(squared_sum);
		out.writeInt(num_samples);
		out.writeDouble(min);
		out.writeDouble(max);
	}
}
