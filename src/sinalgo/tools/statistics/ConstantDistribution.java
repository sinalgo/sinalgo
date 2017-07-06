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

import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;

/**
 * A constant distribution that always returns the same value.
 * <p>
 * If the distribution is specified in the xml configuration file, an entry like
 * <pre>
   &lt;mainTagName distribution="Constant" constant="xxx"/&gt;
 </pre> 
 * is expected. 
 */
public class ConstantDistribution extends Distribution {
	private double value; // the value of this distribution 
	
	/**
	 * Constructs a new ConstantDistribution object that returns always a constant.
	 * @param value The value to be returned by this distribution.
	 */
	public ConstantDistribution(double value) {
		this.value = value;
	}
	
	/**
	 * Creates a new constant distribution and initializes it from the XML configuration file.
	 * @param mainTagPath The entry-path which points to the entry in the XML configuration 
	 * file which contains the specifications for this distribution.
	 * @throws CorruptConfigurationEntryException If the configuration file is corrupt.
	 */
	public ConstantDistribution(String mainTagPath) throws CorruptConfigurationEntryException {
		value = Configuration.getDoubleParameter(mainTagPath + "/constant");
	}
	
	/* (non-Javadoc)
	 * @see tools.statistics.Distribution#nextSample()
	 */
	public double nextSample() {
		return value;
	}
}
