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


import java.util.Random;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;

/**
 * A poisson distribution sample generator with parameter lambda.
 * (lambda is the mean and also the variance of the distribution.)
 * <p>
 * If the distribution is specified in the xml configuration file, an entry like
 * <pre>
   &lt;mainTagName distribution="Poisson" lambda="xxx"/&gt;
 </pre> 
 * is expected. 
 */
public class PoissonDistribution extends Distribution {
	private double expLambda; // e^-lambda
	
	/**
	 * Constructs a new poisson distribution sample generator.
	 * @param lambda The mean (and also variance) of the distribution.
	 */
	public PoissonDistribution(double lambda) {
		expLambda = Math.exp(- lambda);
	}
	
	/**
	 * Creates a new poisson distribution and initializes it from the XML configuration file.
	 * @param mainTagPath The entry-path which points to the entry in the XML configuration 
	 * file which contains the specifications for this distribution.
	 * @throws CorruptConfigurationEntryException If the configuration file is corrupt.
	 */
	public PoissonDistribution(String mainTagPath) throws CorruptConfigurationEntryException {
		double lambda = Configuration.getDoubleParameter(mainTagPath + "/lambda");
		expLambda = Math.exp(- lambda); 
	}
	
	@Override
	/**
	 * Returns the next sample of this poisson distribution sample generator.
	 * 
	 * In fact, the method returns an integer value casted to a double.
	 * @return The next sample of this poisson distribution sample generator casted to a double.
	 */
	public double nextSample() {
		double product = 1;
		int count =  0;
		int result = 0;
		while (product >= expLambda) {
			product *= randomGenerator.nextDouble();
			result = count;
			count++; // keep result one behind
		}
		return result;
	}
	
	/**
	 * Creates a random sample drawn from a poissson distribution with given lambda.
	 * 
	 * Note: for a poisson distribution, E(X) = Var(X) = lambda.
	 * 
	 * The value returned is an integer in the range from 0 to positive infinity (in theory)
	 * 
	 * @param lambda The expectation and variance of the distribution.
	 * @return a random sample drawn from a poissson distribution with given lambda.
	 */
	public static int nextPoisson(double lambda) {
		Random r = Distribution.getRandom();
		double elambda = Math.exp(- lambda);
		double product = 1;
		int count =  0;
		int result = 0;
		while (product >= elambda) {
			product *= r.nextDouble();
			result = count;
			count++; // keep result one behind
		}
		return result;
	}
}
