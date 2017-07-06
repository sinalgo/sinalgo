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
package sinalgo.configuration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import sinalgo.runtime.Global;
import sinalgo.tools.statistics.Distribution;

/**
 * A config file that stores application wide settings for the user, such as
 * the size of the windows and the selected project. 
 */
public class AppConfig {
	String configFile = Configuration.sourceDirPrefix+"/"+Configuration.projectDirInSourceFolder+"/defaultProject/appConfig.xml";
	boolean configExists = false;

	public int projectSelectorWindowWidth = 600;
	public int projectSelectorWindowHeight= 400;
	public int projectSelectorWindowPosX= 50;
	public int projectSelectorWindowPosY= 50;
	public boolean projectSelectorIsMaximized = false;
	public int projectSelectorSelectedTab = 1; // 1 = description, 2 = config
	public String lastChosenProject = "";
	public int guiWindowWidth = 800;
	public int guiWindowHeight = 600;
	public int guiWindowPosX = 50;
	public int guiWindowPosY = 50;
	public boolean guiIsMaximized = false;
	public long seedFromLastRun = 0;

	public int helpWindowWidth = 500;
	public int helpWindowHeight = 500;
	public int helpWindowPosX = 200;
	public int helpWindowPosY = 200;
	public boolean helpWindowIsMaximized = false;
	
	public boolean guiControlPanelExpandSimulation = true;
	public boolean guiControlPanelShowFullViewPanel = true;
	public boolean guiControlPanelShowTextPanel = true;
	public boolean guiControlPanelShowProjectControl = true;

	
	public boolean guiRunOperationIsLimited = true; // infinite many (rounds/events) or the specified amount?
	
	public String lastSelectedFileDirectory = ""; // where the user pointed last to open a file
	
	public boolean checkForSinalgoUpdate = true; // check for updates
	public long timeStampOfLastUpdateCheck = 0; // machine time when Sinalgo checked last for an update
	
	public int generateNodesDlgNumNodes = 100; // # of nodes to generate
	
	public String previousRunCmdLineArgs = ""; // cmd line args of the previous call to 'Run'
	
	
	private static AppConfig singletonInstance = null; // the singleton instance
	
	/**
	 * @return The singleton instance of AppConfig.
	 */
	public static AppConfig getAppConfig() {
		if(singletonInstance == null) {
			singletonInstance = new AppConfig();
		}
		return singletonInstance;
	}

	/**
	 * @return A file describing the directory that was chosen last, 
	 * if this directory does not exist anymore, the directory of the
	 * current project.
	 */
	public File getLastSelectedFileDirectory() {
		File f = new File(lastSelectedFileDirectory);
		if(!f.exists()) {
			f = new File(Global.getProjectSrcDir());
		}
		return f;
	}
	
	/**
	 * Singleton constructor 
	 */
	private AppConfig() {
		File file= new File(configFile);
		if(file.exists()){
			configExists = true;
		} else {
			return;
		}
		
		try {
			Document doc = new SAXBuilder().build(configFile);
			Element root = doc.getRootElement();

			// Read the entries for the Project Selector 
			Element e = root.getChild("ProjectSelector");
			if(e != null) {
				String v = e.getAttributeValue("windowWidth");
				if(v != null) {
					try {
						projectSelectorWindowWidth = Integer.parseInt(v);
					} catch(NumberFormatException ex) {
					}
				}
				
				v = e.getAttributeValue("windowHeight");
				if(v != null) {
					try {
						projectSelectorWindowHeight= Integer.parseInt(v);
					} catch(NumberFormatException ex) {
					}
				}
				v = e.getAttributeValue("windowPosX");
				if(v != null) {
					try {
						projectSelectorWindowPosX= Integer.parseInt(v);
					} catch(NumberFormatException ex) {
					}
				}
				v = e.getAttributeValue("windowPosY");
				if(v != null) {
					try {
						projectSelectorWindowPosY= Integer.parseInt(v);
					} catch(NumberFormatException ex) {
					}
				}
				
				v = e.getAttributeValue("lastChosenProject");
				if(v != null) {
					lastChosenProject = v;
				}
				
				v = e.getAttributeValue("isMaximized");
				if(v != null) {
					projectSelectorIsMaximized = v.equals("true");
				}
				
				v = e.getAttributeValue("selectedTab");
				if(v != null) {
					try {
						projectSelectorSelectedTab = Integer.parseInt(v);
					} catch(NumberFormatException ex) {
					}
				}
			}

			// Read the entries for the GUI
			e = root.getChild("GUI");
			if(e != null) {
				String v = e.getAttributeValue("windowWidth");
				if(v != null) {
					try {
						guiWindowWidth = Integer.parseInt(v);
					} catch(NumberFormatException ex) {
					}
				}
				
				v = e.getAttributeValue("windowHeight");
				if(v != null) {
					try {
						guiWindowHeight = Integer.parseInt(v);
					} catch(NumberFormatException ex) {
					}
				}
				v = e.getAttributeValue("windowPosX");
				if(v != null) {
					try {
						guiWindowPosX = Integer.parseInt(v);
					} catch(NumberFormatException ex) {
					}
				}
				v = e.getAttributeValue("windowPosY");
				if(v != null) {
					try {
						guiWindowPosY = Integer.parseInt(v);
					} catch(NumberFormatException ex) {
					}
				}
				
				v = e.getAttributeValue("isMaximized");
				if(v != null) {
					guiIsMaximized = v.equals("true");
				}
				
				v = e.getAttributeValue("ControlPanelExpandSimulation");
				if(v != null) {
					guiControlPanelExpandSimulation = v.equals("true");
				}
				
				v = e.getAttributeValue("ControlPanelShowFullViewPanel");
				if(v != null) {
					guiControlPanelShowFullViewPanel = v.equals("true");
				}

				v = e.getAttributeValue("ControlPanelShowTextPanel");
				if(v != null) {				
					guiControlPanelShowTextPanel = v.equals("true");
				}
				
				v = e.getAttributeValue("ControlPanelShowProjectControl");
				if(v != null) {
					guiControlPanelShowProjectControl = v.equals("true");
				}

				v = e.getAttributeValue("RunOperationIsLimited");
				if(v != null) {
					guiRunOperationIsLimited = v.equals("true");
				}
				
				v = e.getAttributeValue("helpWindowWidth");
				if(v != null) {
					try {
						helpWindowWidth = Integer.parseInt(v);
					} catch(NumberFormatException ex) {
					}
				}
				v = e.getAttributeValue("helpWindowHeight");
				if(v != null) {
					try {
						helpWindowHeight= Integer.parseInt(v);
					} catch(NumberFormatException ex) {
					}
				}
				v = e.getAttributeValue("helpWindowPosX");
				if(v != null) {
					try {
						helpWindowPosX = Integer.parseInt(v);
					} catch(NumberFormatException ex) {
					}
				}
				v = e.getAttributeValue("helpWindowPosY");
				if(v != null) {
					try {
						helpWindowPosY = Integer.parseInt(v);
					} catch(NumberFormatException ex) {
					}
				}
				v = e.getAttributeValue("helpWindowIsMaximized");
				if(v != null) {
					try {
						helpWindowIsMaximized = v.equals("true");
					} catch(NumberFormatException ex) {
					}
				}
			}
			
			// read the seed from the last run
			e = root.getChild("RandomSeed");
			if(e != null) {
				String s = e.getAttributeValue("seedFromPreviousRun");
				if(s != null) {
					try {
						seedFromLastRun = Long.parseLong(s);
					} catch(NumberFormatException ex) {
					}
				}
			}
			
			// Diverse App specific configs
			e = root.getChild("App");
			if(e != null) {
				String s = e.getAttributeValue("lastSelectedFileDirectory");
				if(s != null) {
					lastSelectedFileDirectory = s;
				}
				
				s = e.getAttributeValue("checkForUpdatesAtStartup");
				if(s != null) {
					checkForSinalgoUpdate = s.equals("true");
				}

				s = e.getAttributeValue("updateCheckTimeStamp");
				if(s != null) {
					try {
						timeStampOfLastUpdateCheck = Long.parseLong(s);
					} catch(NumberFormatException ex) {
					}
				}

				
				s = e.getAttributeValue("runCmdLineArgs");
				if(s != null) {
					previousRunCmdLineArgs = s;
				}
			}

			// Generate Nodes Dlg
			e = root.getChild("GenNodesDlg");
			if(e != null) {
				String s = e.getAttributeValue("numNodes");
				if(s != null) {
					try {
						generateNodesDlgNumNodes = Integer.parseInt(s);
					} catch(NumberFormatException ex) {
					}
				}
			}

		} catch (JDOMException e1) {
		} catch (IOException e1) {
		}
	} // end of constructor

	/**
	 * Writes the application wide config  
	 */
	public void writeConfig() {
		File file= new File(configFile);		

		Document doc = new Document();
		Element root = new Element("sinalgo");
		doc.setRootElement(root);

		Element ps = new Element("ProjectSelector");
		root.addContent(ps);
		ps.setAttribute("windowWidth", Integer.toString(projectSelectorWindowWidth));
		ps.setAttribute("windowHeight", Integer.toString(projectSelectorWindowHeight));
		ps.setAttribute("lastChosenProject", lastChosenProject);
		ps.setAttribute("windowPosX", Integer.toString(projectSelectorWindowPosX));
		ps.setAttribute("windowPosY", Integer.toString(projectSelectorWindowPosY));
		ps.setAttribute("isMaximized", projectSelectorIsMaximized ? "true" : "false");
		ps.setAttribute("selectedTab", Integer.toString(projectSelectorSelectedTab));
		
		Element gui = new Element("GUI");
		root.addContent(gui);
		gui.setAttribute("windowWidth", Integer.toString(guiWindowWidth));
		gui.setAttribute("windowHeight", Integer.toString(guiWindowHeight));
		gui.setAttribute("windowPosX", Integer.toString(guiWindowPosX));
		gui.setAttribute("windowPosY", Integer.toString(guiWindowPosY));
		gui.setAttribute("isMaximized", guiIsMaximized ? "true" : "false");
		gui.setAttribute("ControlPanelExpandSimulation", guiControlPanelExpandSimulation ? "true" : "false");
		gui.setAttribute("ControlPanelShowFullViewPanel", guiControlPanelShowFullViewPanel ? "true" : "false");
		gui.setAttribute("ControlPanelShowTextPanel", guiControlPanelShowTextPanel ? "true" : "false");
		gui.setAttribute("ControlPanelShowProjectControl", guiControlPanelShowProjectControl ? "true" : "false");
		gui.setAttribute("RunOperationIsLimited", guiRunOperationIsLimited ? "true" : "false");
		gui.setAttribute("helpWindowWidth", Integer.toString(helpWindowWidth));
		gui.setAttribute("helpWindowHeight", Integer.toString(helpWindowHeight));
		gui.setAttribute("helpWindowPosX", Integer.toString(helpWindowPosX));
		gui.setAttribute("helpWindowPosY", Integer.toString(helpWindowPosY));
		gui.setAttribute("helpWindowIsMaximized", helpWindowIsMaximized ? "true" : "false");
		
		Element seed = new Element("RandomSeed");
		root.addContent(seed);
		seed.setAttribute("seedFromPreviousRun", Long.toString(Distribution.getSeed()));
		
		Element app= new Element("App");
		root.addContent(app);
		app.setAttribute("lastSelectedFileDirectory", lastSelectedFileDirectory);
		app.setAttribute("checkForUpdatesAtStartup", checkForSinalgoUpdate ? "true" : "false");
		app.setAttribute("updateCheckTimeStamp", Long.toString(timeStampOfLastUpdateCheck));
		app.setAttribute("runCmdLineArgs", previousRunCmdLineArgs);
		
		Element genNodes = new Element("GenNodesDlg");
		root.addContent(genNodes);
		genNodes.setAttribute("numNodes", Integer.toString(generateNodesDlgNumNodes));
		
		//write the xml
		XMLOutputter outputter = new XMLOutputter();
		Format f = Format.getPrettyFormat();
		f.setIndent("\t");
		outputter.setFormat(f);
		
		try {
			FileWriter fW = new FileWriter(file);
			outputter.output(doc, fW);
		} catch (IOException ex) {
		}
	}
}
