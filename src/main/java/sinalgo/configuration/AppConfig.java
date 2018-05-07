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
package sinalgo.configuration;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import sinalgo.io.IOUtils;
import sinalgo.tools.statistics.Distribution;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * A config file that stores application wide settings for the user, such as the
 * size of the windows and the selected project.
 */
public class AppConfig {

    private final String configFileName = "appConfig.xml";

    public int projectSelectorWindowWidth = 600;
    public int projectSelectorWindowHeight = 400;
    public int projectSelectorWindowPosX = 50;
    public int projectSelectorWindowPosY = 50;
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
        if (singletonInstance == null) {
            singletonInstance = new AppConfig();
        }
        return singletonInstance;
    }

    /**
     * @return A file describing the directory that was chosen last, if this
     * directory does not exist anymore, the default directory.
     */
    public File getLastSelectedFileDirectory() {
        File f = new File(this.lastSelectedFileDirectory);
        if (!f.exists()) {
            f = FileSystemView.getFileSystemView().getDefaultDirectory();
        }
        return f;
    }

    /**
     * Singleton constructor
     */
    private AppConfig() {
        Path configFilePath = Paths.get(Configuration.appConfigDir, this.configFileName);
        InputStream configInputStream;
        try {
            configInputStream = Files.newInputStream(configFilePath);
        } catch (Exception e) {
            ClassLoader cldr = Thread.currentThread().getContextClassLoader();
            configInputStream = cldr.getResourceAsStream(
                    IOUtils.getAsPath(Configuration.sinalgoResourceDirPrefix, this.configFileName));
        }

        if (configInputStream == null) {
            return;
        }

        try {
            Document doc = new SAXBuilder().build(configInputStream);
            Element root = doc.getRootElement();

            // Read the entries for the Project Selector
            Element e = root.getChild("ProjectSelector");
            if (e != null) {
                String v = e.getAttributeValue("windowWidth");
                if (v != null) {
                    try {
                        this.projectSelectorWindowWidth = Integer.parseInt(v);
                    } catch (NumberFormatException ignored) {
                    }
                }

                v = e.getAttributeValue("windowHeight");
                if (v != null) {
                    try {
                        this.projectSelectorWindowHeight = Integer.parseInt(v);
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("windowPosX");
                if (v != null) {
                    try {
                        this.projectSelectorWindowPosX = Integer.parseInt(v);
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("windowPosY");
                if (v != null) {
                    try {
                        this.projectSelectorWindowPosY = Integer.parseInt(v);
                    } catch (NumberFormatException ignored) {
                    }
                }

                v = e.getAttributeValue("lastChosenProject");
                if (v != null) {
                    this.lastChosenProject = v;
                }

                v = e.getAttributeValue("isMaximized");
                if (v != null) {
                    this.projectSelectorIsMaximized = v.equals("true");
                }

                v = e.getAttributeValue("selectedTab");
                if (v != null) {
                    try {
                        this.projectSelectorSelectedTab = Integer.parseInt(v);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Read the entries for the GUI
            e = root.getChild("GUI");
            if (e != null) {
                String v = e.getAttributeValue("windowWidth");
                if (v != null) {
                    try {
                        this.guiWindowWidth = Integer.parseInt(v);
                    } catch (NumberFormatException ignored) {
                    }
                }

                v = e.getAttributeValue("windowHeight");
                if (v != null) {
                    try {
                        this.guiWindowHeight = Integer.parseInt(v);
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("windowPosX");
                if (v != null) {
                    try {
                        this.guiWindowPosX = Integer.parseInt(v);
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("windowPosY");
                if (v != null) {
                    try {
                        this.guiWindowPosY = Integer.parseInt(v);
                    } catch (NumberFormatException ignored) {
                    }
                }

                v = e.getAttributeValue("isMaximized");
                if (v != null) {
                    this.guiIsMaximized = v.equals("true");
                }

                v = e.getAttributeValue("ControlPanelExpandSimulation");
                if (v != null) {
                    this.guiControlPanelExpandSimulation = v.equals("true");
                }

                v = e.getAttributeValue("ControlPanelShowFullViewPanel");
                if (v != null) {
                    this.guiControlPanelShowFullViewPanel = v.equals("true");
                }

                v = e.getAttributeValue("ControlPanelShowTextPanel");
                if (v != null) {
                    this.guiControlPanelShowTextPanel = v.equals("true");
                }

                v = e.getAttributeValue("ControlPanelShowProjectControl");
                if (v != null) {
                    this.guiControlPanelShowProjectControl = v.equals("true");
                }

                v = e.getAttributeValue("RunOperationIsLimited");
                if (v != null) {
                    this.guiRunOperationIsLimited = v.equals("true");
                }

                v = e.getAttributeValue("helpWindowWidth");
                if (v != null) {
                    try {
                        this.helpWindowWidth = Integer.parseInt(v);
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("helpWindowHeight");
                if (v != null) {
                    try {
                        this.helpWindowHeight = Integer.parseInt(v);
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("helpWindowPosX");
                if (v != null) {
                    try {
                        this.helpWindowPosX = Integer.parseInt(v);
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("helpWindowPosY");
                if (v != null) {
                    try {
                        this.helpWindowPosY = Integer.parseInt(v);
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("helpWindowIsMaximized");
                if (v != null) {
                    try {
                        this.helpWindowIsMaximized = v.equals("true");
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // read the seed from the last run
            e = root.getChild("RandomSeed");
            if (e != null) {
                String s = e.getAttributeValue("seedFromPreviousRun");
                if (s != null) {
                    try {
                        this.seedFromLastRun = Long.parseLong(s);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Diverse App specific configs
            e = root.getChild("App");
            if (e != null) {
                String s = e.getAttributeValue("lastSelectedFileDirectory");
                if (s != null) {
                    this.lastSelectedFileDirectory = s;
                }

                s = e.getAttributeValue("checkForUpdatesAtStartup");
                if (s != null) {
                    this.checkForSinalgoUpdate = s.equals("true");
                }

                s = e.getAttributeValue("updateCheckTimeStamp");
                if (s != null) {
                    try {
                        this.timeStampOfLastUpdateCheck = Long.parseLong(s);
                    } catch (NumberFormatException ignored) {
                    }
                }

                s = e.getAttributeValue("runCmdLineArgs");
                if (s != null) {
                    this.previousRunCmdLineArgs = s;
                }
            }

            // Generate Nodes Dlg
            e = root.getChild("GenNodesDlg");
            if (e != null) {
                String s = e.getAttributeValue("numNodes");
                if (s != null) {
                    try {
                        this.generateNodesDlgNumNodes = Integer.parseInt(s);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

        } catch (JDOMException | IOException ignored) {
        }
    } // end of constructor

    /**
     * Writes the application wide config
     */
    public void writeConfig() {
        String dir = Configuration.appConfigDir;
        if (!Objects.equals("", dir)) {
            IOUtils.createDir(dir);
        }

        Document doc = new Document();
        Element root = new Element("sinalgo");
        doc.setRootElement(root);

        Element ps = new Element("ProjectSelector");
        root.addContent(ps);
        ps.setAttribute("windowWidth", Integer.toString(this.projectSelectorWindowWidth));
        ps.setAttribute("windowHeight", Integer.toString(this.projectSelectorWindowHeight));
        ps.setAttribute("lastChosenProject", this.lastChosenProject);
        ps.setAttribute("windowPosX", Integer.toString(this.projectSelectorWindowPosX));
        ps.setAttribute("windowPosY", Integer.toString(this.projectSelectorWindowPosY));
        ps.setAttribute("isMaximized", Boolean.toString(this.projectSelectorIsMaximized));
        ps.setAttribute("selectedTab", Integer.toString(this.projectSelectorSelectedTab));

        Element gui = new Element("GUI");
        root.addContent(gui);
        gui.setAttribute("windowWidth", Integer.toString(this.guiWindowWidth));
        gui.setAttribute("windowHeight", Integer.toString(this.guiWindowHeight));
        gui.setAttribute("windowPosX", Integer.toString(this.guiWindowPosX));
        gui.setAttribute("windowPosY", Integer.toString(this.guiWindowPosY));
        gui.setAttribute("isMaximized", Boolean.toString(this.guiIsMaximized));
        gui.setAttribute("ControlPanelExpandSimulation", Boolean.toString(this.guiControlPanelExpandSimulation));
        gui.setAttribute("ControlPanelShowFullViewPanel", Boolean.toString(this.guiControlPanelShowFullViewPanel));
        gui.setAttribute("ControlPanelShowTextPanel", Boolean.toString(this.guiControlPanelShowTextPanel));
        gui.setAttribute("ControlPanelShowProjectControl", Boolean.toString(this.guiControlPanelShowProjectControl));
        gui.setAttribute("RunOperationIsLimited", Boolean.toString(this.guiRunOperationIsLimited));
        gui.setAttribute("helpWindowWidth", Integer.toString(this.helpWindowWidth));
        gui.setAttribute("helpWindowHeight", Integer.toString(this.helpWindowHeight));
        gui.setAttribute("helpWindowPosX", Integer.toString(this.helpWindowPosX));
        gui.setAttribute("helpWindowPosY", Integer.toString(this.helpWindowPosY));
        gui.setAttribute("helpWindowIsMaximized", Boolean.toString(this.helpWindowIsMaximized));

        Element seed = new Element("RandomSeed");
        root.addContent(seed);
        seed.setAttribute("seedFromPreviousRun", Long.toString(Distribution.getSeed()));

        Element app = new Element("App");
        root.addContent(app);
        app.setAttribute("lastSelectedFileDirectory", this.lastSelectedFileDirectory);
        app.setAttribute("checkForUpdatesAtStartup", Boolean.toString(this.checkForSinalgoUpdate));
        app.setAttribute("updateCheckTimeStamp", Long.toString(this.timeStampOfLastUpdateCheck));
        app.setAttribute("runCmdLineArgs", this.previousRunCmdLineArgs);

        Element genNodes = new Element("GenNodesDlg");
        root.addContent(genNodes);
        genNodes.setAttribute("numNodes", Integer.toString(this.generateNodesDlgNumNodes));

        // write the xml
        XMLOutputter outputter = new XMLOutputter();
        Format f = Format.getPrettyFormat();
        f.setIndent("\t");
        outputter.setFormat(f);

        try {
            FileWriter fW = new FileWriter(new File(IOUtils.getAsPath(Configuration.appConfigDir, this.configFileName)));
            outputter.output(doc, fW);
        } catch (IOException ignored) {
        }
    }
}
