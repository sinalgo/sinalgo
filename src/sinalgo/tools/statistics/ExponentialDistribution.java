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
 * An exponential distribution random number generator with parameter lambda.
 * (The expected value is 1/lambda)
 * 
 * If the distribution is specified in the xml configuration file, an entry like
 * <pre>
   &lt;mainTagName distribution="Exponential" lambda="xxx"/&gt;
 </pre> 
 * is expected. 
 */
public class ExponentialDistribution extends Distribution {
	private double lambda;
	
	/**
	 * Creates a new exponential distribution sample generator with parameter lambda.
	 * @param lambda The parameter of the exponential distribution. The expectation is 1/lambda.
	 */
	public ExponentialDistribution(double lambda) {
		this.lambda = lambda;
	}
	
	/**
	 * Creates a new uniform distribution and initializes it from the XML configuration file.
	 * @param mainTagPath The entry-path which points to the entry in the XML configuration 
	 * file which contains the specifications for this distribution.
	 * @throws CorruptConfigurationEntryException If the configuration file is corrupt.
	 */
	public ExponentialDistribution(String mainTagPath) throws CorruptConfigurationEntryException {
		lambda = Configuration.getDoubleParameter(mainTagPath + "/lambda");
	}
	
	@Override
	public double nextSample() {
		return - Math.log(randomGenerator.nextDouble()) / lambda; 
	}
	
	/**
	 * Creates a random sample drawn from an exponential distribution with parameter lambda. 
	 * 
	 * <ul>
	 *  <li>PDF: lambda * e ^ (-lambda * x)</li>
	 *  <li>CDF: 1 - e^(-lambda * x)</li>
	 *  <li>x > 0</li>
	 *  <li>lambda > 0</li>
	 *  <li>E(X) = 1/lambda</li>
	 *  <li>Var(X) = 1/(lambda^2)</li>
	 * </ul>
	 * 
	 * @param lambda
	 * @return A random sample drawn from an exponential distribution with parameter lambda.
	 */
	public static double nextExponential(double lambda) {
		Random r = Distribution.getRandom();
		return - Math.log(r.nextDouble()) / lambda; 
	}
}
