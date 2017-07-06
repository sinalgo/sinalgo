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


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;

/**
 * Abstract class that provides the basis for distribution models, which are
 * random number generators able to return random numbers according to a certain
 * distribution probability.
 * <p>
 * Distributions can be initialized using a configuration from the xml config file. In
 * order to allow automatic initialization, your sub-class must provide a constructor that
 * takes a string with the mainTagPath, which points to the configuration file xml-entry that
 * contains the information for the specific distribution.
 * <p>
 * <b>Conventions for sub-classing this class.</b><p>
 * Naming convention: All sub-classes must have a name that ends with 'Distribution.java'.<p>
 * Implementation: All sub-classes must implement a constructor which takes the entry-path of 
 * the configuration file from where the settings for the distribution is initialized.
 */
public abstract class Distribution {
	protected static Random randomGenerator; // the singleton instance of the random object. Be sure to initialize before using the first time! 
	private static long randomSeed; // the seed used for the random object
	
	/**
	 * Returns the seed value that was used for the singleton random object. 
	 * @return the seed value that was used for the singleton random object.
	 */
	public static long getSeed() {
		getRandom(); // initialize the random generator if it's not already done
		return randomSeed;
	}

	/**
	 * The super-class for all distributions, ensures that the random generator instance exists 
	 */
	protected Distribution(){
		getRandom(); // initialize the random generator if it's not already done
	}
	
	/**
	 * Returns the singleton random generator object of this simulation. You should only use this
	 * random number generator in this project to ensure that the simulatoin can be repeated by
	 * using a fixed seed. (The usage of a fixed seed can be enforced in the XML configuration file.)  
	 *
	 * @return the singleton random generator object of this simulation
	 */
	public static Random getRandom() {
		// construct the singleton random object if it does not yet exist
		if(randomGenerator == null) {
			if(Configuration.useSameSeedAsInPreviousRun) {
				randomSeed = AppConfig.getAppConfig().seedFromLastRun;
			} else {
				if(Configuration.useFixedSeed){
					randomSeed = Configuration.fixedSeed;
				} else {
					randomSeed = (new java.util.Random()).nextLong();
					Configuration.fixedSeed = randomSeed;
				}
			}
			randomGenerator = new Random(randomSeed); // use a random seed
		}
		return randomGenerator;
	}
	
	/**
	 * Constructs a distribution that was specified in the XML configuration file.
	 * The entry in the configuration file is supposed to look as following:
	 * <pre>
	  &lt;mainTagName distribution="X" .../&gt;
	 * </pre>
	 * where <cod>mainTagName</code> is an arbitrary tag name which contains at least one attribute
	 * called <code>distribution</code>. This attribute specifies the name of the distribution, 
	 * here denoted by <cod>X</code>. 
	 * The tag may have further attributes which are distribution specific, e.g. the 
	 * Guassian distribution expects a tag like
	 * <pre>
	  &lt;mainTagName distribution="Gaussian" mean="10" variance="20"/>
	  </pre>
	 * <p>
	 * <b>Note</b> that the name of the acutal implementation of the distribution 
	 * is called XDistribution.java, where X may be 'Gaussian'.  
	 * @param mainTagPath The tag-path under which the mainTagName can be found. 
	 * @return The distribution described by the configuration file entry. 
	 * @throws CorruptConfigurationEntryException If the configuration file is does not contain the expected entries.
	 */
	public static Distribution getDistributionFromConfigFile(String mainTagPath) throws CorruptConfigurationEntryException {
		getRandom(); // initialize the randomGenerator
		String distributionName = Configuration.getStringParameter(mainTagPath + "/distribution");
		
		try {
			Class<?> c = Class.forName("sinalgo.tools.statistics." + distributionName + "Distribution");
			// construct the array of class-types of the objects
			Class<?>[] parameterTypes = {String.class};
			// find the corresponding constructor ...
			Constructor<?> constructor = c.getConstructor(parameterTypes);
			// ... and create an instance of the object
			return (Distribution) constructor.newInstance(mainTagPath);
		} catch(ClassNotFoundException e) {
			throw new IllegalArgumentException("Cannot find an implementation of 'tools.statistics." + distributionName + "'Distribution.java' to create a distribution given its name. (" + e.getMessage() + ")");
		} catch(NoSuchMethodException e) {
			throw new IllegalArgumentException("Cannot find the constructor of 'tools.statistics." + distributionName + "'Distribution.java' to create a distribution given a configuration-file entry name. (" + e.getMessage() + ")");
		} catch(InvocationTargetException e) {
			throw new IllegalArgumentException("Cannot create an instance of 'tools.statistics." + distributionName + "'Distribution.java' (" + e.getCause().getMessage() + ")");
		} catch(IllegalAccessException e) {
			throw new IllegalArgumentException("Cannot create an instance of 'tools.statistics." + distributionName + "'Distribution.java' (" + e.getMessage() + ")");
		} catch(InstantiationException e) {
			throw new IllegalArgumentException("Cannot create an instance of 'tools.statistics." + distributionName + "'Distribution.java' (" + e.getMessage() + ")");
		} 
	}
	
	/**
	 * Returns the next random sample of this distribution. 
	 * 
	 * This method must be implemented in all proper subclasses.
	 * @return the next random sample of this distribution.
	 */
	public abstract double nextSample();
}
