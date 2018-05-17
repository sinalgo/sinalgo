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
package sinalgo.gui;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jdom2.Comment;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration;
import sinalgo.configuration.Configuration.ImplementationChoiceInConfigFile;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.exception.SinalgoWrappedException;
import sinalgo.gui.helper.NonRegularGridLayout;
import sinalgo.gui.helper.UnborderedJTextField;
import sinalgo.gui.multiLineTooltip.MultiLineToolTipJComboBox;
import sinalgo.gui.multiLineTooltip.MultiLineToolTipJTextArea;
import sinalgo.gui.multiLineTooltip.MultiLineToolTipJTextField;
import sinalgo.io.IOUtils;
import sinalgo.io.xml.XMLParser;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.tools.Tuple;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;

/**
 * This class provides a dialog to let the user select a project.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class ProjectSelector extends JFrame implements ActionListener, ListSelectionListener {

    private static final long serialVersionUID = -6312902319966899446L;

    private JPanel listPanel = new JPanel();
    private JList selection = new JList();

    private JPanel buttonPanel = new JPanel();
    private JButton ok = new JButton("Ok");
    private JButton cancel = new JButton("Cancel");

    private JTextArea descriptionText = new JTextArea();

    private JPanel configuration = new JPanel();
    private JPanel frameworkConfigurationPanel = new JPanel();
    private JPanel customConfigurationPanel = new JPanel();
    private JButton saveConfig = new JButton("Save Config");
    private JButton saveConfig2 = new JButton("Save Config");
    private JButton expand;
    private JButton collapse;
    private JTextArea customParameters = new JTextArea();

    private JTabbedPane right = new JTabbedPane();

    private boolean showOptionalFields;

    private Vector<ConfigEntry> projectEntries;

    // the instance to invoke after having finished
    private final Object main;

    private int defaultTooltipDismissDelay;

    // true if the user has modified the currently shown configuration
    private UserInputListener userInputListener = new UserInputListener();
    private String selectedProjectName = "";

    /**
     * The constructor for the project selection frame.
     *
     * @param main The object to notify when the ok button is pressed.
     */
    public ProjectSelector(Object main) {
        super("Select a Project");
        GuiHelper.setWindowIcon(this);
        // show all the tooltips for 15000 mili-seconds
        this.setDefaultTooltipDismissDelay(ToolTipManager.sharedInstance().getDismissDelay());
        int myTooltipDismissDelay = 15000;
        ToolTipManager.sharedInstance().setDismissDelay(myTooltipDismissDelay);
        this.main = main;
    }

    /**
     * This method populates the ProjectSelector with all the subpanels and buttons
     */
    public void populate() {
        // gather all projects
        Vector<String> projects = Global.getProjectNames();

        this.addComponentListener(new ComponentListener() {

            int oldX = AppConfig.getAppConfig().getProjectSelectorWindowPosX(), oldY = AppConfig.getAppConfig().getProjectSelectorWindowPosY();

            @Override
            public void componentResized(ComponentEvent e) {
                if (ProjectSelector.this.getExtendedState() == Frame.MAXIMIZED_BOTH) {
                    AppConfig.getAppConfig().setProjectSelectorIsMaximized(true);
                    AppConfig.getAppConfig().setProjectSelectorWindowPosX(this.oldX);
                    AppConfig.getAppConfig().setProjectSelectorWindowPosY(this.oldY);
                } else {
                    AppConfig.getAppConfig().setProjectSelectorIsMaximized(false);
                    AppConfig.getAppConfig().setProjectSelectorWindowWidth(ProjectSelector.this.getWidth());
                    AppConfig.getAppConfig().setProjectSelectorWindowHeight(ProjectSelector.this.getHeight());
                }
                ProjectSelector.this.getCustomParameters().setSize(100, ProjectSelector.this.getCustomParameters().getHeight());
                // needed to ensure that the text field shrinks as well
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                this.oldX = AppConfig.getAppConfig().getProjectSelectorWindowPosX();
                this.oldY = AppConfig.getAppConfig().getProjectSelectorWindowPosY();
                AppConfig.getAppConfig().setProjectSelectorWindowPosX(ProjectSelector.this.getX());
                AppConfig.getAppConfig().setProjectSelectorWindowPosY(ProjectSelector.this.getY());
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });

        // safe the overal config file when the project selector closes
        this.addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                AppConfig.getAppConfig().setProjectSelectorSelectedTab(1 + ProjectSelector.this.getRight().getSelectedIndex());
                AppConfig.getAppConfig().writeConfig();
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        });

        this.setLayout(new BorderLayout());
        this.setResizable(true);
        if (AppConfig.getAppConfig().isProjectSelectorIsMaximized()) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
        this.setSize(new Dimension(AppConfig.getAppConfig().getProjectSelectorWindowWidth(), AppConfig.getAppConfig().getProjectSelectorWindowHeight()));
        this.setLocation(AppConfig.getAppConfig().getProjectSelectorWindowPosX(), AppConfig.getAppConfig().getProjectSelectorWindowPosY());

        JPanel left = new JPanel();
        left.setLayout(new BorderLayout());
        // List of all projects
        this.getSelection().setListData(projects);
        this.getSelection().setSelectedValue(AppConfig.getAppConfig().getLastChosenProject(), true);
        if (!this.getSelection().isSelectionEmpty()) {
            this.setSelectedProjectName((String) this.getSelection().getSelectedValue());
        } else {
            this.setSelectedProjectName("");
        }
        this.getSelection().addListSelectionListener(this);
        this.getSelection().setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 0));
        this.getSelection().setBackground(this.getListPanel().getBackground());
        JScrollPane listScroller = new JScrollPane(this.getSelection());
        this.getListPanel().setLayout(new BorderLayout());
        listScroller.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        this.getListPanel().add(listScroller, BorderLayout.CENTER);
        this.getListPanel().setBorder(BorderFactory.createTitledBorder("Available Projects"));
        left.add(this.getListPanel(), BorderLayout.CENTER);

        // OK / Cancel buttons
        this.getButtonPanel().add(this.getOk());
        this.getButtonPanel().add(this.getCancel());
        this.getOk().addActionListener(this);
        this.getCancel().addActionListener(this);
        int projectListWidth = 160;
        this.getButtonPanel().setPreferredSize(new Dimension(projectListWidth, 40));
        left.add(this.getButtonPanel(), BorderLayout.SOUTH);

        this.add(left, BorderLayout.WEST);

        // right.setBorder(BorderFactory.createTitledBorder("Project Description &
        // Configuration"));

        // The tab containing the description of the selected project
        JPanel description = new JPanel();
        description.setLayout(new BorderLayout());
        JScrollPane scrollableDescriptionPane = new JScrollPane(this.getDescriptionText());
        description.add(scrollableDescriptionPane);
        this.getDescriptionText().setEditable(false);
        this.getDescriptionText().setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        int i = this.getSelection().getSelectedIndex();
        if (i == -1) {
            // there was no defaultProject
            this.getDescriptionText().setText("Please select a project.");
        } else {
            this.generateGUIDescription(this.getSelectedProjectName());
        }
        this.getRight().addTab("Description", description);

        // The tab containing the config-file entries
        this.getConfiguration().setLayout(new BoxLayout(this.getConfiguration(), BoxLayout.Y_AXIS));

        JScrollPane scrollableConfigurationPane = new JScrollPane(this.getFrameworkConfigurationPanel());
        // increment the scroll speed (for some reason the speed for the
        // scrollableConfigurationPane is very low)
        scrollableConfigurationPane.getVerticalScrollBar().setUnitIncrement(15);

        this.getFrameworkConfigurationPanel().setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.getConfiguration().add(scrollableConfigurationPane);
        JPanel bp = new JPanel();
        bp.add(this.getSaveConfig());
        this.getSaveConfig().addActionListener(this);
        this.getSaveConfig().setMnemonic(java.awt.event.KeyEvent.VK_S);
        this.getConfiguration().add(bp);

        this.setExpand(this.createFrameworkIconButton("expand", "expand.gif", "Show advanced settings"));
        this.setCollapse(this.createFrameworkIconButton("collapse", "collapse.gif", "Hide advanced settings"));

        this.getCustomConfigurationPanel().setLayout(new BorderLayout());

        JPanel mainCustomConfigurationPanel = new JPanel();
        mainCustomConfigurationPanel.setLayout(new BoxLayout(mainCustomConfigurationPanel, BoxLayout.Y_AXIS));
        mainCustomConfigurationPanel.add(this.getCustomConfigurationPanel());
        // and the save button
        JPanel bp2 = new JPanel();
        bp2.add(this.getSaveConfig2());
        this.getSaveConfig2().addActionListener(this);
        this.getSaveConfig2().setMnemonic(java.awt.event.KeyEvent.VK_S);
        mainCustomConfigurationPanel.add(bp2);

        this.getRight().addTab("Framework Config", this.getConfiguration());
        this.getRight().addTab("Project Config", mainCustomConfigurationPanel);
        this.getRight().setMnemonicAt(0, java.awt.event.KeyEvent.VK_D);
        this.getRight().setMnemonicAt(1, java.awt.event.KeyEvent.VK_F);
        this.getRight().setMnemonicAt(2, java.awt.event.KeyEvent.VK_P);
        this.getRight().setSelectedIndex(AppConfig.getAppConfig().getProjectSelectorSelectedTab() - 1);

        if (i == -1) {
            JTextField msg = new JTextField("Please select a project.");
            msg.setEditable(false);
            this.getFrameworkConfigurationPanel().add(msg);
        } else {
            this.generateGUIGonfiguration(this.getSelectedProjectName());
        }

        this.add(this.getRight(), BorderLayout.CENTER);

        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        this.getRootPane().setDefaultButton(this.getOk());

        this.setVisible(true);

        // this.setUndecorated(true);
        // GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
    }

    /**
     * Creates a new icon button where the icon is supposed to be stored in the
     * framework
     *
     * @param actionCommand Name of the action that is performed when this button is pressed
     * @param imageName     The name of the image file, which is stored in the directory
     *                      specified by Configuration.sinalgoImageDir
     * @param toolTip       Tooltip text to be shown for this button
     * @return A new JButton with an icon
     */
    protected JButton createFrameworkIconButton(String actionCommand, String imageName, String toolTip) {
        // To support jar files, we cannot access the file directly
        String path = IOUtils.getAsPath(Configuration.getSinalgoImageDir(), imageName);
        try {
            InputStream is = IOUtils.getResourceAsStream(path);
            ImageIcon icon = new ImageIcon(ImageIO.read(is));
            JButton b = new JButton(icon);
            // b.setPreferredSize(new Dimension(29, 29));
            b.setPreferredSize(new Dimension(0, 9));
            b.setActionCommand(actionCommand);
            b.setFocusable(false);
            b.setBorderPainted(false);
            // b.setBackground(bgColor);
            b.addActionListener(this);
            // b.addMouseListener(this); // move over the button => draw border
            b.setToolTipText(toolTip);
            return b;
        } catch (IOException e) {
            throw new SinalgoFatalException("Cannot access the application icon " + imageName
                    + ", which should be stored in\n" + "resources" + path + ".");
        }
    }

    /**
     * Show the description for a given project
     *
     * @param projectName The project name
     */
    private void generateGUIDescription(String projectName) {
        ClassLoader cldr = Thread.currentThread().getContextClassLoader();
        InputStream proj = cldr.getResourceAsStream(IOUtils.getAsPath(Configuration.getProjectResourceDirPrefix(), projectName, Configuration.getDescriptionFileName()));
        try {
            if (proj == null) {
                this.getDescriptionText().setText("There is no description-file in the currently selected project.");
            } else {
                LineNumberReader r = new LineNumberReader(new InputStreamReader(proj));
                StringBuilder description = new StringBuilder();
                String tmp;
                while ((tmp = r.readLine()) != null) {
                    description.append(tmp).append("\n");
                }
                this.getDescriptionText().setText(description.toString());
                this.getDescriptionText().setCaretPosition(0);
            }
        } catch (FileNotFoundException e) {
            this.getDescriptionText().setText("There is no description-file in the currently selected project.");
        } catch (IOException e) {
            Main.minorError(e);
            this.getDescriptionText().setText("There is no description-file in the currently selected project.");
        }
    }

    /**
     * Extracts the custom-text from the config file, without the surrounding
     * <custom></custom> flag
     *
     * @param reader The file loaded as a reader
     * @return The custom text, an empty string upon any failure
     */
    private String getCustomText(LineNumberReader reader) {
        StringBuilder result = new StringBuilder();
        boolean inCustom = false;
        String line;
        try {
            line = reader.readLine();
            while (line != null) {
                if (!inCustom) {
                    if (!line.contains("<Custom>")) {
                        line = reader.readLine();
                        continue;
                    }
                    int offset = line.indexOf("<Custom>");
                    result = new StringBuilder(line.substring(offset + 8)); // skip the tag <Custom>
                    inCustom = true;
                } else {
                    result.append(line).append("\n");
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            return "";
        }
        int offset = result.lastIndexOf("</Custom>");
        if (offset >= 0) {
            result = new StringBuilder(result.substring(0, offset));
        }
        return result.toString();
    }

    private MultiLineToolTipJComboBox asynchronousSimulationCB;
    private MultiLineToolTipJComboBox mobilityCB;

    /**
     * Generate the GUI components to show the config for a given project
     *
     * @param projectName The Project's name
     */
    private void generateGUIGonfiguration(String projectName) {
        boolean configExists = true;

        Element frameworkElement = null;

        LineNumberReader configFile;
        try {
            configFile = IOUtils.getProjectConfigurationAsReader(projectName);
        } catch (SinalgoFatalException e) {
            configFile = null;
        }
        if (configFile == null) {
            configExists = false;
        } else {
            try {
                Document doc = new SAXBuilder().build(configFile);
                Element root = doc.getRootElement();
                frameworkElement = root.getChild("Framework");
                Element custom = root.getChild("Custom");
                if (custom == null) {
                    throw new SinalgoFatalException("Invalid configuration file: A Custom entry is missing.\n"
                            + "The file needs to be of the following form: \n"
                            + "<Document>\n  <Framework>...</Framework>\n  <Custom></Custom>\n</Document>");
                }
                if (frameworkElement == null) {
                    throw new SinalgoFatalException("Invalid configuration file: A 'framework' entry is missing.\n"
                            + "The file needs to be of the following form: \n"
                            + "<Document>\n  <Framework>...</Framework>\n  <Custom></Custom>\n</Document>");
                }
            } catch (JDOMException e1) {
                throw new SinalgoFatalException("Invalid configuration file:\n\n" + e1.getMessage());
            } catch (IOException e1) {
                throw new SinalgoFatalException("Cannot open or read from configuration file:\n\n" + e1.getMessage());
            }
        }

        this.setProjectEntries(new Vector<>());

        Class<?> configClass = Configuration.class;
        // We assume here that the fields are returned in the order they are listed in
        // the java file!

        boolean finalConfigExists = configExists;
        Element finalFrameworkElement = frameworkElement;
        Arrays.stream(configClass.getDeclaredFields()).map(f -> {
            try {
                return new Tuple<>(f, new PropertyDescriptor(f.getName(), configClass));
            } catch (IntrospectionException e) {
                return new Tuple<Field, PropertyDescriptor>(f, null);
            }
        }).forEach(pd -> {
            try {
                // read the annotations for this field
                Field field = pd.getFirst();
                Method getter = Optional.ofNullable(pd.getSecond()).map(PropertyDescriptor::getReadMethod).orElse(null);
                boolean useGetter = !Modifier.isPublic(field.getModifiers()) && getter != null;
                Configuration.DefaultInConfigFile dan = field.getAnnotation(Configuration.DefaultInConfigFile.class);
                Configuration.OptionalInConfigFile oan = field.getAnnotation(Configuration.OptionalInConfigFile.class);
                Configuration.SectionInConfigFile san = field.getAnnotation(Configuration.SectionInConfigFile.class);
                String sectionName;
                if (dan != null || oan != null) {
                    if (san != null) { // get the title
                        sectionName = san.value();
                        this.getProjectEntries().add(new ConfigEntry(sectionName, "", Configuration.SectionInConfigFile.class, "",
                                false, field));
                    }
                    String description = dan != null ? dan.value() : oan.value(); // the description text
                    // test whether the XML file contains an entry for this field
                    String value = null;
                    if (finalConfigExists) {
                        Element e = finalFrameworkElement.getChild(field.getName());
                        if (e != null) {
                            value = e.getAttributeValue("value"); // null if not there
                        }
                    }
                    if (value == null) {
                        Object fieldValue = useGetter ? getter.invoke(null) : field.get(null);
                        // there was no entry in the config-file. Take the default value.
                        this.getProjectEntries().add(
                                new ConfigEntry(field.getName(), Configuration.getConfigurationText(fieldValue),
                                        field.getType(), description, oan != null, field));
                    } else { // there is an entry in the XML file
                        this.getProjectEntries().add(new ConfigEntry(field.getName(), value, field.getType(), description,
                                oan != null, field)); // elem.comment
                    }
                } else if (field.getName().equals("edgeType")) {
                    // NOTE: the edgeType member does not carry any annotations (exception)
                    String comment = "The default type of edges to be used";
                    String value = null;
                    if (finalConfigExists) {
                        Element e = finalFrameworkElement.getChild(field.getName());
                        if (e != null) {
                            value = e.getAttributeValue("value"); // null if not there
                        }
                    }
                    if (value == null) {
                        // there was no entry in the config-file. Take the default value.
                        this.getProjectEntries().add(new ConfigEntry(field.getName(), Configuration.getEdgeType(),
                                field.getType(), comment, oan != null, field));
                    } else {
                        this.getProjectEntries().add(new ConfigEntry(field.getName(), value,
                                field.getType(), comment, oan != null, field));
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                throw new SinalgoWrappedException(e);
            }
        });


        // for each entry, create the corresponding GUI components

        this.setAsynchronousSimulationCB(null);
        this.setMobilityCB(null);
        for (ConfigEntry e : this.getProjectEntries()) {
            String ttt = e.getComment().equals("") ? null : e.getComment(); // the tool tip text, don't show at all, if no text to
            // display

            // creating the text field
            UnborderedJTextField label;
            if (e.getEntryClass() == Configuration.SectionInConfigFile.class) {
                label = new UnborderedJTextField(e.getKey(), Font.BOLD);
            } else {
                label = new UnborderedJTextField("         " + e.getKey(), Font.PLAIN);
            }
            label.setToolTipText(ttt);
            e.setTextComponent(label);

            if (e.getEntryClass() == boolean.class) {
                String[] ch = {"true", "false"};
                MultiLineToolTipJComboBox booleanChoice = new MultiLineToolTipJComboBox(ch);
                if ((e.getValue()).compareTo("true") != 0) {
                    booleanChoice.setSelectedItem("false");
                } else {
                    booleanChoice.setSelectedItem("true");
                }
                booleanChoice.addActionListener(this.getUserInputListener()); // ! add this listener AFTER setting the value!
                booleanChoice.setToolTipText(ttt);
                e.setValueComponent(booleanChoice);
                // special case: mobility can only be changed if simulation is in sync mode.
                if (e.getKey().equals("asynchronousMode")) {
                    this.setAsynchronousSimulationCB(booleanChoice);
                    if (this.getMobilityCB() != null && (e.getValue()).equals("true")) {
                        this.getMobilityCB().setSelectedItem("false");
                        this.getMobilityCB().setEnabled(false);
                    }
                }
                if (e.getKey().equals("mobility")) {
                    this.setMobilityCB(booleanChoice);
                    if (this.getAsynchronousSimulationCB() != null && "true".equals(this.getAsynchronousSimulationCB().getSelectedItem())) {
                        this.getMobilityCB().setSelectedItem("false");
                        this.getMobilityCB().setEnabled(false);
                    }
                }
            } else if (e.getEntryClass() == Configuration.SectionInConfigFile.class) {
                e.setValueComponent(null); // there's no component for the section
            } else {
                // special case for some text fields that expect the name of an implementation.
                // They should show the available implementations in a drop down field
                ImplementationChoiceInConfigFile ian = e.getField().getAnnotation(ImplementationChoiceInConfigFile.class);
                if (ian != null) {
                    Vector<String> ch = Global.getImplementations(ian.value(), true);
                    MultiLineToolTipJComboBox choice = new MultiLineToolTipJComboBox(ch);
                    choice.setEditable(true); // let the user the freedom to enter other stuff (which is likely to be
                    // wrong...)
                    choice.setSelectedItem(e.getValue());
                    choice.addActionListener(this.getUserInputListener()); // ! add this listener AFTER setting the value!
                    choice.setToolTipText(ttt);
                    e.setValueComponent(choice);
                } else {
                    if (e.getKey().equals("javaCmd")) { // special case - this field may contain a lot of text
                        JTextArea textArea = new MultiLineToolTipJTextArea(e.getValue());
                        textArea.setToolTipText(ttt);
                        textArea.setBorder((new JTextField()).getBorder()); // copy the border
                        textArea.setLineWrap(true);
                        // textArea.setPreferredSize(new Dimension(50, 30));
                        textArea.addKeyListener(this.getUserInputListener());
                        e.setValueComponent(textArea);
                    } else {
                        MultiLineToolTipJTextField textField = new MultiLineToolTipJTextField(e.getValue());
                        textField.setToolTipText(ttt);
                        textField.addKeyListener(this.getUserInputListener());
                        e.setValueComponent(textField);
                    }
                }
            }
        }
        // and finally add all the entries
        this.insertProjectEntries();

        this.getCustomConfigurationPanel().removeAll();

        // And add the custom entries

        // this code snipped was used to redirect the mouse wheel applied on this
        // text field to the entire tab when the custom config was below the framework
        // config
        // // remove all mouse wheel listeners from the text input
        // for(MouseWheelListener mwl : customParameters.getMouseWheelListeners()) {
        // customParameters.removeMouseWheelListener(mwl);
        // }
        // // and add the 'global' one
        // customParameters.addMouseWheelListener(new
        // MouseWheelForwarder(scrollableConfigurationPane.getMouseWheelListeners()));

        this.getCustomParameters().setTabSize(3);
        this.getCustomParameters().setLineWrap(true);
        this.getCustomParameters().setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        if (configExists) {
            this.getCustomParameters().setText(this.getCustomText(IOUtils.getProjectConfigurationAsReader(projectName)));
        } else {
            this.getCustomParameters().setText("");
        }

        this.getCustomParameters().addKeyListener(this.getUserInputListener()); // ! add this listener AFTER setting the text !

        JScrollPane customScroll = new JScrollPane(this.getCustomParameters(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        customScroll.setWheelScrollingEnabled(true);
        this.getCustomConfigurationPanel().add(customScroll);

        this.getUserInputListener().reset();

        super.repaint();
    }

    /**
     * Adds the default entries to the GUI
     * <p>
     * Thanks to this method, the user can show and hide the optional settings and
     * keep what he has already entered.
     */
    private void insertProjectEntries() {
        this.getFrameworkConfigurationPanel().removeAll();
        this.getFrameworkConfigurationPanel().setLayout(new BorderLayout());
        JPanel entryTable = new JPanel();
        this.getFrameworkConfigurationPanel().add(entryTable, BorderLayout.CENTER);
        if (this.isShowOptionalFields()) {
            this.getFrameworkConfigurationPanel().add(this.getCollapse(), BorderLayout.SOUTH); // add the 'expand' button
        } else {
            this.getFrameworkConfigurationPanel().add(this.getExpand(), BorderLayout.SOUTH); // add the 'expand' button
        }

        entryTable.removeAll();
        ConfigEntry title = null; // only print titles if there are entries shown for this title

        int numEntryTableLines = 0; // count number of rows added to entryTable

        for (ConfigEntry e : this.getProjectEntries()) {
            if (e.getValueComponent() == null) { // it's a title
                title = e;
                continue;
            }
            if (e.isOptional() && !this.isShowOptionalFields()) {
                continue;
            }
            if (title != null) { // first print the title
                entryTable.add(title.getTextComponent());
                MultiLineToolTipJTextField textField = new MultiLineToolTipJTextField(e.getValue());
                textField.setVisible(false); // add a place-holder
                entryTable.add(textField);
                title = null;
                numEntryTableLines++;
            }
            entryTable.add(e.getTextComponent());
            entryTable.add(e.getValueComponent());
            numEntryTableLines++;
        }
        NonRegularGridLayout nrgl = new NonRegularGridLayout(numEntryTableLines, 2, 5, 2);
        nrgl.setAlignToLeft(true);
        entryTable.setLayout(nrgl);
    }

    /**
     * Validates the custom configuration and displays, if necessary, error
     * messages.
     *
     * @return The XML document describing the custom configuration, null if the
     * configuration did not parse properly.
     */
    private Document validateCustomFields() {
        Document doc;
        try {
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Document><Custom>" + this.getCustomParameters().getText()
                    + "</Custom></Document>";
            doc = new SAXBuilder().build(new StringReader(xml));
        } catch (JDOMException e) {
            Main.minorError("Invalid XML in the custom configuration:\n\n" + e.getMessage());
            return null;
        } catch (IOException e) {
            Main.minorError("Cannot parse custom configuration due to I/O exception:\n\n" + e.getMessage());
            return null;
        }
        return doc;
    }

    private void storeConfig(boolean isTemporary) {
        // Test that the custom config is OK
        Document customConfig = this.validateCustomFields();
        if (customConfig == null) {
            return;
        }

        // Build the framework config XML tree
        Document doc = new Document();
        Element root = new Element("Document");
        doc.setRootElement(root);
        Element framework = new Element("Framework");
        root.addContent(framework);
        Element custom = new Element("Custom");
        root.addContent(custom);
        custom.addContent(new Element("_xml_custom_")); // this tag will be replaced by the config text in a second step

        for (ConfigEntry e : this.getProjectEntries()) {
            if (e.getValueComponent() != null) { // there is a value field in the GUI
                if (!Objects.equals("", e.getComment())) { // the comment is not "", add it
                    framework.addContent(new Comment(e.getComment().replace("\n", " "))); // without the newline chars
                }
                // get the value of this entry from the GUI
                String value = "";
                if (e.getValueComponent() instanceof JComboBox) {
                    value = (String) ((JComboBox) e.getValueComponent()).getSelectedItem();
                } else if (e.getValueComponent() instanceof JTextComponent) {
                    value = ((JTextComponent) e.getValueComponent()).getText();
                }
                // create and add a new entry in the XML file
                Element elem = new Element(e.getKey());
                elem.setAttribute("value", value);
                framework.addContent(elem);
                framework.addContent(new Element("_xml_NL_")); // after each entry, we would like a new-line in the
                // document - these tags are replaced in a second step
            } else {
                // this is a section entry, which will be inserted as comment
                // String line = " - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                // - - - - ";
                String line = "***********************************************************************";
                framework.addContent(new Comment(line));
                StringBuilder name = new StringBuilder("  " + e.getKey());
                while (name.length() < line.length()) { // fill the name with spaces s.t. the '-->' are aligned
                    name.append(" ");
                }
                framework.addContent(new Comment(name.toString()));
                framework.addContent(new Comment(line));
            }
        }

        String outputPath = IOUtils.getAsPath(
                (isTemporary ? Configuration.getAppTmpFolder() : Configuration.getAppConfigDir()),
                Configuration.getUserProjectsPackage(), this.getSelectedProjectName());
        IOUtils.createDir(outputPath);
        File outputFile = new File(IOUtils.getAsPath(outputPath, Configuration.getConfigfileFileName() + (isTemporary ? ".run" : "")));

        // And write the xml tree to the file
        XMLOutputter outputter = new XMLOutputter();
        Format f = Format.getPrettyFormat();
        f.setIndent("\t");
        outputter.setFormat(f);
        String tempOutputFolder = IOUtils.getAsPath(Configuration.getAppTmpFolder(),
                Configuration.getUserProjectsPackage(), this.getSelectedProjectName());
        IOUtils.createDir(tempOutputFolder);
        File tempOutputFile = new File(IOUtils.getAsPath(tempOutputFolder, Configuration.getConfigfileFileName() + ".temp"));

        try (FileWriter fW = new FileWriter(tempOutputFile)) {
            outputter.output(doc, fW);
        } catch (IOException e) {
            Main.minorError("Could not write a temporary configuration file!\n\n" + e.getMessage());
            return;
        }

        // in a second step, parse the temp file, replace the _xml_nl_ by new-lines and
        // the _xml_custom_ by the custom text

        try (BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));
             LineNumberReader input = new LineNumberReader(new FileReader(tempOutputFile))) {
            String line = input.readLine();
            while (line != null) {
                if (line.contains("<_xml_NL_")) {
                    output.newLine();
                } else if (line.contains("<_xml_custom_")) {
                    output.write(this.getCustomParameters().getText());
                } else {
                    output.write(line);
                    output.newLine();
                }
                line = input.readLine();
            }
            tempOutputFile.delete();
        } catch (IOException e1) {
            Main.minorError("Could not write the configuration file!\n\n" + e1.getMessage());
        }

        this.getUserInputListener().reset(); // finally reset the 'modified' flag
    }

    /*-**********************************************************************************/
    /* WRITING BACK */
    /*-**********************************************************************************/

    /**
     * This method overwrites the values in the config file by the ones set in the
     * project selector configuration.
     */
    private void setFrameworkConfig() {
        for (ConfigEntry e : this.getProjectEntries()) {
            if (e.getValueComponent() == null) {
                continue; // this entry does not have a value - its probably a section header
            }
            String value;
            if (e.getValueComponent() instanceof JComboBox) {
                value = (String) ((JComboBox) e.getValueComponent()).getSelectedItem();
            } else {
                value = ((JTextComponent) e.getValueComponent()).getText();
            }
            Configuration.setFrameworkConfigurationEntry(e.getKey(), value);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(this.getSaveConfig()) || e.getSource().equals(this.getSaveConfig2())) { // --------------------------------------------------------------------
            if (this.getSelection().getSelectedValue() == null) {
                JOptionPane.showMessageDialog(this, "Please select a project from the selection.",
                        "No project selected.", JOptionPane.ERROR_MESSAGE);
            } else {
                try {
                    this.storeConfig(false);
                } catch (Exception ex) {
                    throw new SinalgoWrappedException(ex);
                }
            }
        } else if (e.getSource().equals(this.getOk())) { // --------------------------------------------------------------------
            if (this.getSelection().getSelectedValue() == null) {
                JOptionPane.showMessageDialog(this, "Please select a project from the selection.",
                        "No project selected.", JOptionPane.ERROR_MESSAGE);
            } else {
                if (this.getUserInputListener().isModified()) {
                    // the user has modified the config, but not stored it
                    int decision = JOptionPane.showConfirmDialog(this,
                            "The modifications to this configuration have not yet been saved.\n\nDo you want to store this configuration, such that it is also available for subsequent runs?",
                            "Unsaved modifications", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (decision == JOptionPane.YES_OPTION) { // store
                        try {
                            this.storeConfig(false);
                        } catch (Exception ex) {
                            throw new SinalgoWrappedException(ex);
                        }
                    }
                    if (decision == JOptionPane.CANCEL_OPTION) {
                        return; // don't do anything
                    }
                }

                Document customDoc = this.validateCustomFields();
                if (customDoc == null) { // there is invalid xml in the custom entry
                    return;
                }

                if (!this.getSelectedProjectName().equals("defaultProject")) {
                    Global.setProjectName(this.getSelectedProjectName());
                    Global.setUseProject(true);
                    AppConfig.getAppConfig().setLastChosenProject(Global.getProjectName());
                }

                Element customEle = customDoc.getRootElement().getChild("Custom");
                XMLParser.parseCustom(customEle, "");

                this.setFrameworkConfig();

                // Block the overwriting of the now set values.
                XMLParser.setBlockParse(true);

                this.setVisible(false);

                // reset the tooltip dismiss delay to the default value
                ToolTipManager.sharedInstance().setDismissDelay(this.getDefaultTooltipDismissDelay());

                AppConfig.getAppConfig().setProjectSelectorSelectedTab(1 + this.getRight().getSelectedIndex());
                AppConfig.getAppConfig().writeConfig(); // write the config, s.t. when the main application crashes, at least the
                // project selector config is preserved
                this.storeConfig(true); // store the config to a file s.t. the simulation process can read it

                // wake up the waiting object.
                synchronized (this.getMain()) {
                    this.getMain().notify();
                }
            }
        } else if (e.getSource().equals(this.getCancel())) { // --------------------------------------------------------------------
            if (this.getUserInputListener().isModified()) {
                // the user has modified the config, but not stored it
                int decision = JOptionPane.showConfirmDialog(this,
                        "The configuration for project '" + this.getSelectedProjectName()
                                + "' has unsaved changes. Do you wish to save them?",
                        "Save Changes?", JOptionPane.YES_NO_CANCEL_OPTION);
                if (decision == JOptionPane.YES_OPTION) { // store
                    try {
                        this.storeConfig(false);
                    } catch (Exception ex) {
                        throw new SinalgoWrappedException(ex);
                    }
                }
                if (decision == JOptionPane.CANCEL_OPTION) {
                    return;
                }
            }
            AppConfig.getAppConfig().setProjectSelectorSelectedTab(1 + this.getRight().getSelectedIndex());
            AppConfig.getAppConfig().writeConfig();
            System.exit(0);
        } else if (e.getSource().equals(this.getCollapse())) { // --------------------------------------------------------------------
            this.setShowOptionalFields(false);
            this.insertProjectEntries();
            this.repaint();
        } else if (e.getSource().equals(this.getExpand())) { // --------------------------------------------------------------------
            this.setShowOptionalFields(true);
            this.insertProjectEntries();
            this.repaint();
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            if (this.getUserInputListener().isModified()) {
                // the user has modified the config, but not stored it
                int decision = JOptionPane.showConfirmDialog(this,
                        "The configuration for project '" + this.getSelectedProjectName()
                                + "' has unsaved changes. Do you wish to save them?",
                        "Save Changes?", JOptionPane.YES_NO_CANCEL_OPTION);
                if (decision == JOptionPane.YES_OPTION) { // store
                    try {
                        this.storeConfig(false);
                    } catch (Exception ex) {
                        throw new SinalgoWrappedException(ex);
                    }
                }
                if (decision == JOptionPane.CANCEL_OPTION) {
                    this.getSelection().removeListSelectionListener(this);
                    this.getSelection().setSelectedValue(this.getSelectedProjectName(), true);
                    this.getSelection().addListSelectionListener(this);
                    return;
                }
            }
            this.setSelectedProjectName((String) this.getSelection().getSelectedValue());
            this.generateGUIGonfiguration(this.getSelectedProjectName());
            this.generateGUIDescription(this.getSelectedProjectName());
        }
    }

    /**
     * Input listener that listens to all user-input fields to decide whether the
     * user has modified the configuration somehow.
     */
    private class UserInputListener implements KeyListener, ActionListener {

        @Getter
        private boolean isModified;

        public void reset() {
            ProjectSelector.this.getSaveConfig().setEnabled(false);
            ProjectSelector.this.getSaveConfig2().setEnabled(false);
            this.isModified = false;
        }

        public void setModified() {
            ProjectSelector.this.getSaveConfig().setEnabled(true);
            ProjectSelector.this.getSaveConfig2().setEnabled(true);
            this.isModified = true;
        }

        @Override
        public void keyTyped(KeyEvent e) {
            this.setModified();
        }

        @Override
        public void keyPressed(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            this.setModified();
            this.test(e);
        }

        private void test(ActionEvent e) {
            if (e.getSource() == ProjectSelector.this.getAsynchronousSimulationCB()) {
                if (ProjectSelector.this.getMobilityCB() != null) {
                    if ("true".equals(ProjectSelector.this.getAsynchronousSimulationCB().getSelectedItem())) {
                        ProjectSelector.this.getMobilityCB().setSelectedItem("false");
                        ProjectSelector.this.getMobilityCB().setEnabled(false);
                    } else {
                        ProjectSelector.this.getMobilityCB().setEnabled(true);
                    }
                }
            }
        }
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    private class ConfigEntry {

        /**
         * The key of the Pair.
         */
        private final String key;

        /**
         * The value of the Pair.
         */
        private final String value;

        /**
         * The class of the ConfigEntry.
         */
        private final Class<?> entryClass;

        /**
         * The comment for the entry.
         */
        private final String comment;

        /**
         * True if the entry is only shown in the extended settings
         */
        private final boolean isOptional;

        /**
         * The variable-field of the config file (to access the annotations)
         */
        private final Field field;

        /**
         * The GUI component for this entry that holds the value for this entry. This
         * member is set only when the GUI is created, and may remain NULL when this
         * entry does not have a value field (e.g. a comment, or section header)
         */
        private JComponent valueComponent;

        /**
         * The GUI component for this entry that holds the text for this entry. This
         * member is set only when the GUI is created.
         */
        private JComponent textComponent;

    }

    // private class MouseWheelForwarder implements MouseWheelListener{
    //
    // private MouseWheelListener[] mwlArray = null;
    //
    // private MouseWheelForwarder(MouseWheelListener[] mwlArray){
    // this.mwlArray = mwlArray;
    // }
    //
    // public void mouseWheelMoved(MouseWheelEvent arg0) {
    // for(int i = 0; i < mwlArray.length; i++){
    // mwlArray[i].mouseWheelMoved(arg0);
    // }
    // }
    //
    // }
}
