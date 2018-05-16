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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
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
import java.util.function.Supplier;

/**
 * A config file that stores application wide settings for the user, such as the
 * size of the windows and the selected project.
 */
@Getter
@Setter
public class AppConfig {

    private final String configFileName = "appConfig.xml";

    private int projectSelectorWindowWidth = 600;
    private int projectSelectorWindowHeight = 400;
    private int projectSelectorWindowPosX = 50;
    private int projectSelectorWindowPosY = 50;
    private boolean projectSelectorIsMaximized;
    private int projectSelectorSelectedTab = 1; // 1 = description, 2 = config
    private String lastChosenProject = "";
    private int guiWindowWidth = 800;
    private int guiWindowHeight = 600;
    private int guiWindowPosX = 50;
    private int guiWindowPosY = 50;
    private boolean guiIsMaximized;
    private long seedFromLastRun;

    private int helpWindowWidth = 500;
    private int helpWindowHeight = 500;
    private int helpWindowPosX = 200;
    private int helpWindowPosY = 200;
    private boolean helpWindowIsMaximized;

    private boolean guiControlPanelExpandSimulation = true;
    private boolean guiControlPanelShowFullViewPanel = true;
    private boolean guiControlPanelShowTextPanel = true;
    private boolean guiControlPanelShowProjectControl = true;

    private boolean guiRunOperationIsLimited = true; // infinite many (rounds/events) or the specified amount?

    private String lastSelectedFileDirectory = ""; // where the user pointed last to open a file

    private boolean checkForSinalgoUpdate = true; // check for updates
    private long timeStampOfLastUpdateCheck; // machine time when Sinalgo checked last for an update

    private int generateNodesDlgNumNodes = 100; // # of nodes to generate

    private String previousRunCmdLineArgs = ""; // cmd line args of the previous call to 'Run'

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private static AppConfig singletonInstance;

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private static Supplier<AppConfig> appConfigProvider = () -> {
        setSingletonInstance(new AppConfig());
        setAppConfigProvider(AppConfig::getSingletonInstance);
        return getAppConfig();
    };

    /**
     * @return The singleton instance of AppConfig.
     */
    public static AppConfig getAppConfig() {
        return getAppConfigProvider().get();
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
        Path configFilePath = Paths.get(Configuration.getAppConfigDir(), this.configFileName);
        InputStream configInputStream;
        try {
            configInputStream = Files.newInputStream(configFilePath);
        } catch (Exception e) {
            ClassLoader cldr = Thread.currentThread().getContextClassLoader();
            configInputStream = cldr.getResourceAsStream(
                    IOUtils.getAsPath(Configuration.getSinalgoResourceDirPrefix(), this.configFileName));
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
                        this.setProjectSelectorWindowWidth(Integer.parseInt(v));
                    } catch (NumberFormatException ignored) {
                    }
                }

                v = e.getAttributeValue("windowHeight");
                if (v != null) {
                    try {
                        this.setProjectSelectorWindowHeight(Integer.parseInt(v));
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("windowPosX");
                if (v != null) {
                    try {
                        this.setProjectSelectorWindowPosX(Integer.parseInt(v));
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("windowPosY");
                if (v != null) {
                    try {
                        this.setProjectSelectorWindowPosY(Integer.parseInt(v));
                    } catch (NumberFormatException ignored) {
                    }
                }

                v = e.getAttributeValue("lastChosenProject");
                if (v != null) {
                    this.setLastChosenProject(v);
                }

                v = e.getAttributeValue("isMaximized");
                if (v != null) {
                    this.setProjectSelectorIsMaximized(v.equals("true"));
                }

                v = e.getAttributeValue("selectedTab");
                if (v != null) {
                    try {
                        this.setProjectSelectorSelectedTab(Integer.parseInt(v));
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
                        this.setGuiWindowWidth(Integer.parseInt(v));
                    } catch (NumberFormatException ignored) {
                    }
                }

                v = e.getAttributeValue("windowHeight");
                if (v != null) {
                    try {
                        this.setGuiWindowHeight(Integer.parseInt(v));
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("windowPosX");
                if (v != null) {
                    try {
                        this.setGuiWindowPosX(Integer.parseInt(v));
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("windowPosY");
                if (v != null) {
                    try {
                        this.setGuiWindowPosY(Integer.parseInt(v));
                    } catch (NumberFormatException ignored) {
                    }
                }

                v = e.getAttributeValue("isMaximized");
                if (v != null) {
                    this.setGuiIsMaximized(v.equals("true"));
                }

                v = e.getAttributeValue("ControlPanelExpandSimulation");
                if (v != null) {
                    this.setGuiControlPanelExpandSimulation(v.equals("true"));
                }

                v = e.getAttributeValue("ControlPanelShowFullViewPanel");
                if (v != null) {
                    this.setGuiControlPanelShowFullViewPanel(v.equals("true"));
                }

                v = e.getAttributeValue("ControlPanelShowTextPanel");
                if (v != null) {
                    this.setGuiControlPanelShowTextPanel(v.equals("true"));
                }

                v = e.getAttributeValue("ControlPanelShowProjectControl");
                if (v != null) {
                    this.setGuiControlPanelShowProjectControl(v.equals("true"));
                }

                v = e.getAttributeValue("RunOperationIsLimited");
                if (v != null) {
                    this.setGuiRunOperationIsLimited(v.equals("true"));
                }

                v = e.getAttributeValue("helpWindowWidth");
                if (v != null) {
                    try {
                        this.setHelpWindowWidth(Integer.parseInt(v));
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("helpWindowHeight");
                if (v != null) {
                    try {
                        this.setHelpWindowHeight(Integer.parseInt(v));
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("helpWindowPosX");
                if (v != null) {
                    try {
                        this.setHelpWindowPosX(Integer.parseInt(v));
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("helpWindowPosY");
                if (v != null) {
                    try {
                        this.setHelpWindowPosY(Integer.parseInt(v));
                    } catch (NumberFormatException ignored) {
                    }
                }
                v = e.getAttributeValue("helpWindowIsMaximized");
                if (v != null) {
                    try {
                        this.setHelpWindowIsMaximized(v.equals("true"));
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
                        this.setSeedFromLastRun(Long.parseLong(s));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Diverse App specific configs
            e = root.getChild("App");
            if (e != null) {
                String s = e.getAttributeValue("lastSelectedFileDirectory");
                if (s != null) {
                    this.setLastSelectedFileDirectory(s);
                }

                s = e.getAttributeValue("checkForUpdatesAtStartup");
                if (s != null) {
                    this.setCheckForSinalgoUpdate(s.equals("true"));
                }

                s = e.getAttributeValue("updateCheckTimeStamp");
                if (s != null) {
                    try {
                        this.setTimeStampOfLastUpdateCheck(Long.parseLong(s));
                    } catch (NumberFormatException ignored) {
                    }
                }

                s = e.getAttributeValue("runCmdLineArgs");
                if (s != null) {
                    this.setPreviousRunCmdLineArgs(s);
                }
            }

            // Generate Nodes Dlg
            e = root.getChild("GenNodesDlg");
            if (e != null) {
                String s = e.getAttributeValue("numNodes");
                if (s != null) {
                    try {
                        this.setGenerateNodesDlgNumNodes(Integer.parseInt(s));
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
        String dir = Configuration.getAppConfigDir();
        if (!Objects.equals("", dir)) {
            IOUtils.createDir(dir);
        }

        Document doc = new Document();
        Element root = new Element("sinalgo");
        doc.setRootElement(root);

        Element ps = new Element("ProjectSelector");
        root.addContent(ps);
        ps.setAttribute("windowWidth", Integer.toString(this.getProjectSelectorWindowWidth()));
        ps.setAttribute("windowHeight", Integer.toString(this.getProjectSelectorWindowHeight()));
        ps.setAttribute("lastChosenProject", this.getLastChosenProject());
        ps.setAttribute("windowPosX", Integer.toString(this.getProjectSelectorWindowPosX()));
        ps.setAttribute("windowPosY", Integer.toString(this.getProjectSelectorWindowPosY()));
        ps.setAttribute("isMaximized", Boolean.toString(this.isProjectSelectorIsMaximized()));
        ps.setAttribute("selectedTab", Integer.toString(this.getProjectSelectorSelectedTab()));

        Element gui = new Element("GUI");
        root.addContent(gui);
        gui.setAttribute("windowWidth", Integer.toString(this.getGuiWindowWidth()));
        gui.setAttribute("windowHeight", Integer.toString(this.getGuiWindowHeight()));
        gui.setAttribute("windowPosX", Integer.toString(this.getGuiWindowPosX()));
        gui.setAttribute("windowPosY", Integer.toString(this.getGuiWindowPosY()));
        gui.setAttribute("isMaximized", Boolean.toString(this.isGuiIsMaximized()));
        gui.setAttribute("ControlPanelExpandSimulation", Boolean.toString(this.isGuiControlPanelExpandSimulation()));
        gui.setAttribute("ControlPanelShowFullViewPanel", Boolean.toString(this.isGuiControlPanelShowFullViewPanel()));
        gui.setAttribute("ControlPanelShowTextPanel", Boolean.toString(this.isGuiControlPanelShowTextPanel()));
        gui.setAttribute("ControlPanelShowProjectControl", Boolean.toString(this.isGuiControlPanelShowProjectControl()));
        gui.setAttribute("RunOperationIsLimited", Boolean.toString(this.isGuiRunOperationIsLimited()));
        gui.setAttribute("helpWindowWidth", Integer.toString(this.getHelpWindowWidth()));
        gui.setAttribute("helpWindowHeight", Integer.toString(this.getHelpWindowHeight()));
        gui.setAttribute("helpWindowPosX", Integer.toString(this.getHelpWindowPosX()));
        gui.setAttribute("helpWindowPosY", Integer.toString(this.getHelpWindowPosY()));
        gui.setAttribute("helpWindowIsMaximized", Boolean.toString(this.isHelpWindowIsMaximized()));

        Element seed = new Element("RandomSeed");
        root.addContent(seed);
        seed.setAttribute("seedFromPreviousRun", Long.toString(Distribution.getSeed()));

        Element app = new Element("App");
        root.addContent(app);
        app.setAttribute("lastSelectedFileDirectory", this.lastSelectedFileDirectory);
        app.setAttribute("checkForUpdatesAtStartup", Boolean.toString(this.isCheckForSinalgoUpdate()));
        app.setAttribute("updateCheckTimeStamp", Long.toString(this.getTimeStampOfLastUpdateCheck()));
        app.setAttribute("runCmdLineArgs", this.getPreviousRunCmdLineArgs());

        Element genNodes = new Element("GenNodesDlg");
        root.addContent(genNodes);
        genNodes.setAttribute("numNodes", Integer.toString(this.getGenerateNodesDlgNumNodes()));

        // write the xml
        XMLOutputter outputter = new XMLOutputter();
        Format f = Format.getPrettyFormat();
        f.setIndent("\t");
        outputter.setFormat(f);

        try {
            FileWriter fW = new FileWriter(new File(IOUtils.getAsPath(Configuration.getAppConfigDir(), this.configFileName)));
            outputter.output(doc, fW);
        } catch (IOException ignored) {
        }
    }
}
