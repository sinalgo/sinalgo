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
package sinalgo.models;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.tools.Tuple;

/**
 * This model class provides some static functionalities to create models. 
 */
public abstract class Model {

	private String paramString = ""; // The parameter string passed either on the console or by the gui

	/**
	 * Sets the parameter string that was passed to the constructor of this model on the console or through the GUI.
	 * @param params The String to set the parameter string to.
	 */
	public void setParamString(String params) {
		paramString = params;
	}
	
	/**
	 * Returns the parameter string pased to the constructor of this model on the console or through the GUI. 
	 * @return The parameter string pased to the constructor of this model on the console or through the GUI. 
	 */
	public String getParamString() {
		return paramString;
	}
	
	/**
	 * Returns the type of this model.
	 * @return The type of this model.
	 */
	public abstract ModelType getType();
	
	/**
	 * Returns the class-object of a given model. 
	 * 
	 * @param type The type of the model
	 * @param className The name of the model. If the model is stored 
	 * in a project, it must be prefixed with the proejct-name and a 
	 * colon. E.g. looks like 'project-name':'class-name'. 
	 * Otherwise, to take the default model, only give the class-name.
	 * If className contains '.' characters, it is assumed that the fully qualified
	 * class-name is provided.
	 * 
	 * @return A class object of the required model.
	 * @throws WrongConfigurationException If the specified class cannot be found or created.
	 */
	public static Class<?> getModelClass(ModelType type, String className) throws WrongConfigurationException {
		String modelName = type.name();
	  // replace first letter with lower-case version and append an 's'
		modelName = modelName.substring(0,1).toLowerCase() + modelName.substring(1) + "s"; 
		
		Class<?> result = null;
		try {
			String name = "";
			if(className.contains(".")) {
				name = className; // kind of a hack: the name is already absolute.
			} else if(className.contains(":")){ // the name is composed as 'project-name':'class-name'
				String[] parts = className.split(":",2);
				name =  Configuration.userProjectsPath + "." + parts[0] + ".models." + modelName + "." + parts[1]; 
				
			}	else{
				name = Configuration.defaultProjectPath + ".models." + modelName + "." + className;
			}
			result = Class.forName(name);
		} catch (ClassNotFoundException e) {
			throw new WrongConfigurationException(e, "Cannot generate class for the " +
			                                      modelName + ". The class " +  className + 
			                                      " cannot be found. ("+ e.getMessage() + ")");
		}
		
		return result;
	}

	/**
	 * Creates an instance of a model given the name and type of the model. 
	 * 
	 * @param type The type of the model
	 * @param className The name of the model. If the model is stored 
	 * in a project, it must be prefixed with the proejct-name and a 
	 * colon. E.g. looks like 'project-name':'class-name'.
	 * Otherwise, to take the default model, only give the class-name.
	 * If className contains '.' characters, it is assumed that the fully qualified
	 * class-name is provided.
	 * 
	 * @param parameters The parameters for the constructor, no further parameters if no parameters should be passed on to the constructor. 
	 * @return An instance of the specified model.
	 * @throws WrongConfigurationException If the constructor cannot be created or the instanciation fails. Note: 
	 * call <code>getCause()</code> to obtain the original exception.
	 */
	public static Model getModelInstance(ModelType type, String className, Object ...parameters) throws WrongConfigurationException {
		Class<?> c = getModelClass(type, className);
		return getModelInstance(c, className, parameters);
	}
	
	/**
	 * Creates an instance of a connectivity model given the name of the model. 
	 * 
	 * @param className The name of the model. If the model is stored 
	 * in a project, it must be prefixed with the proejct-name and a 
	 * colon. E.g. looks like 'project-name':'class-name'.
	 * Otherwise, to take the default model, only give the class-name.
	 * If className contains '.' characters, it is assumed that the fully qualified
	 * class-name is provided.
	 * @param parameters The parameters for the constructor, no further parameters if no parameters should be passed on to the constructor. 
	 * @return An instance of the specified model.
	 * @throws WrongConfigurationException If the constructor cannot be created or the instanciation fails. Note: 
	 * call <code>getCause()</code> to obtain the original exception.
	 */
	public static ConnectivityModel getConnectivityModelInstance(String className, Object ...parameters) throws WrongConfigurationException {
		return (ConnectivityModel) getModelInstance(ModelType.ConnectivityModel, className, parameters);
	}

	/**
	 * Creates an instance of a distribution model given the name of the model. 
	 * 
	 * @param className The name of the model. If the model is stored 
	 * in a project, it must be prefixed with the proejct-name and a 
	 * colon. E.g. looks like 'project-name':'class-name'.
	 * Otherwise, to take the default model, only give the class-name.
	 * If className contains '.' characters, it is assumed that the fully qualified
	 * class-name is provided.
	 * @param parameters The parameters for the constructor, no further parameters if no parameters should be passed on to the constructor. 
	 * @return An instance of the specified model.
	 * @throws WrongConfigurationException If the constructor cannot be created or the instanciation fails. Note: 
	 * call <code>getCause()</code> to obtain the original exception.
	 */
	public static DistributionModel getDistributionModelInstance(String className, Object ...parameters) throws WrongConfigurationException {
		return (DistributionModel) getModelInstance(ModelType.DistributionModel, className, parameters);
	}

	/**
	 * Creates an instance of a interference model given the name of the model. 
	 * 
	 * @param className The name of the model. If the model is stored 
	 * in a project, it must be prefixed with the proejct-name and a 
	 * colon. E.g. looks like 'project-name':'class-name'.
	 * Otherwise, to take the default model, only give the class-name.
	 * If className contains '.' characters, it is assumed that the fully qualified
	 * class-name is provided.
	 * @param parameters The parameters for the constructor, no further parameters if no parameters should be passed on to the constructor. 
	 * @return An instance of the specified model.
	 * @throws WrongConfigurationException If the constructor cannot be created or the instanciation fails. Note: 
	 * call <code>getCause()</code> to obtain the original exception.
	 */
	public static InterferenceModel getInterferenceModelInstance(String className, Object ...parameters) throws WrongConfigurationException {
		return (InterferenceModel) getModelInstance(ModelType.InterferenceModel, className, parameters);
	}
	
	/**
	 * Creates an instance of a message transmission model given the name of the model. 
	 * 
	 * @param className The name of the model. If the model is stored 
	 * in a project, it must be prefixed with the proejct-name and a 
	 * colon. E.g. looks like 'project-name':'class-name'.
	 * Otherwise, to take the default model, only give the class-name.
	 * If className contains '.' characters, it is assumed that the fully qualified
	 * class-name is provided.
	 * @param parameters The parameters for the constructor, no further parameters if no parameters should be passed on to the constructor. 
	 * @return An instance of the specified model.
	 * @throws WrongConfigurationException If the constructor cannot be created or the instanciation fails. Note: 
	 * call <code>getCause()</code> to obtain the original exception.
	 */
	public static MessageTransmissionModel getMessageTransmissionModelInstance(String className, Object ...parameters) throws WrongConfigurationException {
		return (MessageTransmissionModel) getModelInstance(ModelType.MessageTransmissionModel, className, parameters);
	}
	
	/**
	 * Creates an instance of a mobility model given the name of the model. 
	 * 
	 * @param className The name of the model. If the model is stored 
	 * in a project, it must be prefixed with the proejct-name and a 
	 * colon. E.g. looks like 'project-name':'class-name'.
	 * Otherwise, to take the default model, only give the class-name.
	 * If className contains '.' characters, it is assumed that the fully qualified
	 * class-name is provided.
	 * @param parameters The parameters for the constructor, no further parameters if no parameters should be passed on to the constructor. 
	 * @return An instance of the specified model.
	 * @throws WrongConfigurationException If the constructor cannot be created or the instanciation fails. Note: 
	 * call <code>getCause()</code> to obtain the original exception.
	 */
	public static MobilityModel getMobilityModelInstance(String className, Object ...parameters) throws WrongConfigurationException {
		return (MobilityModel) getModelInstance(ModelType.MobilityModel, className, parameters);
	}
	
	/**
	 * Creates an instance of a reliabitliy model given the name of the model. 
	 * 
	 * @param className The name of the model. If the model is stored 
	 * in a project, it must be prefixed with the proejct-name and a 
	 * colon. E.g. looks like 'project-name':'class-name'.
	 * Otherwise, to take the default model, only give the class-name.
	 * If className contains '.' characters, it is assumed that the fully qualified
	 * class-name is provided.
	 * @param parameters The parameters for the constructor, no further parameters if no parameters should be passed on to the constructor. 
	 * @return An instance of the specified model.
	 * @throws WrongConfigurationException If the constructor cannot be created or the instanciation fails. Note: 
	 * call <code>getCause()</code> to obtain the original exception.
	 */
	public static ReliabilityModel getReliabilityModelInstance(String className, Object ...parameters) throws WrongConfigurationException {
		return (ReliabilityModel) getModelInstance(ModelType.ReliabilityModel, className, parameters);
	}
	
	/**
	 * Creates an instance of a model given the class.
	 * 
	 * @param c The class of the model.
	 * @param className The name of the model - only used to produce meaningful error-texts.
	 * @param parameters The parameters for the constructor, no further parameters if no parameters should be passed on to the constructor. 
	 * @return An instance of the specified model.
	 * @throws WrongConfigurationException If the constructor cannot be created or the instanciation fails. Note: 
	 * call <code>getCause()</code> to obtain the original exception.
	 */
	private static Model getModelInstance(Class<?> c, String className, Object ...parameters) throws WrongConfigurationException { 
		Class<?>[] types = new Class[parameters.length];
		for(int i=0; i < parameters.length; i++) {
			types[1] = parameters[i].getClass();
		}
		Model result = null;
		try {
			Constructor<?> constructor = c.getConstructor(types);
			result = (Model) constructor.newInstance(parameters);
		} catch (SecurityException e) {
			throw new WrongConfigurationException("Cannot generate constructor of the model '" + className + "' due to a SecurityException: " + e.getMessage());
		} catch (NoSuchMethodException e) {
			String paramTypes = "";
			for(Class<?> cl : types) {
				paramTypes += cl.getSimpleName() + ", ";
			}
			throw new WrongConfigurationException(e, "Cannot generate constructor of the model '" + className + "'. There is no constructor that takes the parameters: (" + paramTypes + ").");
		} catch (IllegalArgumentException e) {
			throw new WrongConfigurationException(e, "Cannot generate instance of the model '" + className + "' due to wrong arguments.");
		} catch (InstantiationException e) {
			String cause = e.getCause() == null ? "" : e.getCause().getMessage();
			throw new WrongConfigurationException(e, "Cannot generate instance of the model '" + className + "' : " + cause);
		} catch (IllegalAccessException e) {
			throw new WrongConfigurationException(e, "Cannot generate instance of the model '" + className + "'");
		} catch (InvocationTargetException e) {
			String cause = e.getCause() == null ? "" : e.getCause().getMessage();
			throw new WrongConfigurationException(e, "Cannot generate instance of the model '" + className + "' :  " + cause);
		}
		
		return result;
	}
	
	/**
	 * The model can be prefix with the type of the model in an abreviated manner in the form <code>X=model_name</code>
	 * where X is the prefix of one character and model_name the name of the model. X is one of the
	 * characters in {C|D|I|T|M|R} and maps to a model as defined below in the initializer.
	 * <p>
	 * The prefix X is used to disambiguate different models if different several models have the same name. 
	 */
	private static HashMap<String, ModelType> modelEnumPrefix = new HashMap<String, ModelType>();
	static {
		// initialize the mapping from prefix to ModelEnum
		modelEnumPrefix.put("C", ModelType.ConnectivityModel);
		modelEnumPrefix.put("D", ModelType.DistributionModel);
		modelEnumPrefix.put("I", ModelType.InterferenceModel);
		modelEnumPrefix.put("T", ModelType.MessageTransmissionModel);
		modelEnumPrefix.put("M", ModelType.MobilityModel);
		modelEnumPrefix.put("R", ModelType.ReliabilityModel);
	}
	
	/**
	 * Creates the class of a model given the model-name, if the model-name uniquely defines the model.
	 * <p>
	 * This method does not require the model-type. I.e. this method tests all possible model-folders
	 * and returns the model that matches this name. If there is an ambiguity, this method throws an exception.
	 * <p> 
	 * To disambiguate a model-name, it can be prefixed to indicate the model-kind. 
	 * The prefix looks like X=model-name where X is {C|D|I|T|M|R}. The mapping is as following:
	 * <ul>
	 * <li>C-ConnectivityModel</li>
	 * <li>D-DistributionModel</li>
	 * <li>I-InterferenceModel</li>
	 * <li>T-MessageTransmissionModel</li>
	 * <li>M-MobilityModel</li>
	 * <li>R-ReliabilityModel</li>
	 * </ul>
	 * 
	 * @param modelName The name of the model. If the model is stored 
	 * in a project, it must be prefixed with the proejct-name and a 
	 * colon. E.g. looks like 'project-name':'class-name'.
	 * Otherwise, to take the default model, only give the class-name.
	 * If className contains '.' characters, it is assumed that the fully qualified
	 * class-name is provided.
	 * @return A class object of the required model.
	 * @throws WrongConfigurationException If the specified class cannot be found or created, or if the modelName does
	 * not uniquely define a model.
	 */
	public static Tuple<ModelType, Class<?>> getModelClass(String modelName) throws WrongConfigurationException {
		if(modelName.length() > 2 && modelName.charAt(1) == '=') {
			// the modelName is prefixed with a disambiguation prefix that indicates the model type
			if(!modelEnumPrefix.containsKey(modelName.substring(0,1))) {
				throw new WrongConfigurationException("Cannot create an instance of the model " + modelName +
				                                      ". The prefix '" + modelName.substring(0,1) + 
				                                      "' does not specify a model. Valid prefixes are {C|D|I|T|M|R}."
				);
			}
			ModelType model = modelEnumPrefix.get(modelName.substring(0,1));
			return new Tuple<ModelType, Class<?>>(model , getModelClass(model, modelName.substring(2))); 
		} else {
			Class<?> modelClass = null;
			ModelType type = null;
			int succeeded = 0;
			String modelNames = ""; // the list of possible models (for exception text)
			// test each model, exactly one should succeed
			for(ModelType mt : ModelType.values()) {
				try {
					modelClass = getModelClass(mt, modelName);
					type = mt;
					succeeded ++;
					modelNames += mt.name() + " ";
				} catch(WrongConfigurationException e) {} // ok to fail
			}
			if(succeeded == 0) {
				throw new WrongConfigurationException("Cannot create an instance of the model " + modelName + ". The class is not found.");
			} else if(succeeded > 1){
				throw new WrongConfigurationException("Cannot create an instance of the model '" + modelName + 
				                                      "'. The model is not uniquely defined - there exists a model with the name '" +
				                                      modelName + "' for [" + modelNames + "]. To disambiguate the situaion, you may " +
				                                      "prefix the model to indicate the model-kind. The prefix looks like X=model-name, " +
				                                      "where X is {C|D|I|T|M|R}. The mapping is as following: \n" +
				                                      "C-ConnectivityModel\n" +
				                                      "D-DistributionModel\n" +
				                                      "I-InterferenceModel\n" +
				                                      "T-MessageTransmissionModel\n" +
				                                      "M-MobilityModel\n" +
				                                      "R-ReliabilityModel"
				);
			} else {
				return new Tuple<ModelType, Class<?>>(type, modelClass);
			}
		}
	}
	
	/**
	 * Creates an instance of a model given the model-name, if the model-name uniquely defines the model.
	 * <p>
	 * This method does not require the model-type. I.e. this method tests all possible model-folders
	 * and returns the model that matches this name. If there is an ambiguity, this method throws an exception.
	 * <p> 
	 * To disambiguate a model-name, it can be prefixed to indicate the model-kind. 
	 * The prefix looks like X=model-name where X is {C|D|I|T|M|R}. The mapping is as following:
	 * <ul>
	 * <li>C-ConnectivityModel</li>
	 * <li>D-DistributionModel</li>
	 * <li>I-InterferenceModel</li>
	 * <li>T-MessageTransmissionModel</li>
	 * <li>M-MobilityModel</li>
	 * <li>R-ReliabilityModel</li>
	 * </ul>
	 * 
	 * @param className The name of the model. If the model is stored 
	 * in a project, it must be prefixed with the proejct-name and a 
	 * colon. E.g. looks like 'project-name':'class-name'.
	 * Otherwise, to take the default model, only give the class-name.
	 * If className contains '.' characters, it is assumed that the fully qualified
	 * class-name is provided.
	 * @param parameters The parameters passed to the constructor.
	 * @return A class object of the required model.
	 * @throws WrongConfigurationException If the specified class cannot be found or created, or if the modelName does
	 * not uniquely define a model.
	 */
	public static Model getModelInstance(String className, Object ...parameters) throws WrongConfigurationException {
		Tuple<ModelType, Class<?>> tmp = getModelClass(className); 
		return getModelInstance(tmp.second, className, parameters);
	}
}
