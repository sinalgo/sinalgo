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
package sinalgo.gui;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;

import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration.ImplementationChoiceInConfigFile;
import sinalgo.gui.helper.NonRegularGridLayout;
import sinalgo.gui.helper.UnborderedJTextField;
import sinalgo.gui.multiLineTooltip.MultiLineToolTipJComboBox;
import sinalgo.gui.multiLineTooltip.MultiLineToolTipJTextArea;
import sinalgo.gui.multiLineTooltip.MultiLineToolTipJTextField;
import sinalgo.io.xml.XMLParser;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;


/**
 * This class provides a dialog to let the user select a project.
 */
@SuppressWarnings("serial")
public class ProjectSelector extends JFrame implements ActionListener, ListSelectionListener{
	private JPanel listPanel = new JPanel();
	private JList selection = new JList();
	
	private JPanel buttonPanel = new JPanel();
	private JButton ok = new JButton("Ok");
	private JButton cancel = new JButton("Cancel");
	
	private JTextArea descriptionText = new JTextArea();
	private JScrollPane scrollableDescriptionPane = null;
	
	private JPanel configuration = new JPanel();
	private JPanel frameworkConfigurationPanel = new JPanel();
	private JPanel customConfigurationPanel = new JPanel();
	private JScrollPane scrollableConfigurationPane = null;
	private JButton saveConfig = new JButton("Save Config");
	private JButton saveConfig2 = new JButton("Save Config");
	private JButton expand, collapse;
	JTextArea customParameters = new JTextArea();
	
	JScrollPane listScroller;
	JTabbedPane right = new JTabbedPane();
	private int projectListWidth = 160;
	
	private AppConfig appConfig = AppConfig.getAppConfig();

	boolean showOptionalFields = false;
	
	private Vector<ConfigEntry> projectEntries;
	
	//the instance to invoke after having finished
	private Object main = null;
	
	private int defaultTooltipDismissDelay = 0;
	private int myTooltipDismissDelay = 15000;
	
	// true if the user has modified the currently shown configuration
	private UserInputListener userInputListener = new UserInputListener();
	private String selectedProjectName = "";
	
	/**
	 * The constructor for the project selection frame.
	 */
	public ProjectSelector(){
		super("Select a Project");
		GuiHelper.setWindowIcon(this);
		//show all the tooltips for 15000 mili-seconds
		defaultTooltipDismissDelay = ToolTipManager.sharedInstance().getDismissDelay();
		ToolTipManager.sharedInstance().setDismissDelay(myTooltipDismissDelay);
	}
	
	
	/**
	 * @return The names of all projects, sorted alphabetically (ascending).
	 */
	public static String[] getAllProjectNames() {
		String[] list = Configuration.nonUserProjectDirNames.split(";");
		final Vector<String> blocklist = new Vector<String>();
		for(String s : list) {
			blocklist.add(s);
		}

		File file = new File(Configuration.sourceDirPrefix+"/"+Configuration.projectDirInSourceFolder);
		String[] projects = file.list(new FilenameFilter() {
			public boolean accept(File dir, String name){
				//only allow projects not calles CVS and only the ones that have a compiled version in the binaries folder.
				if(blocklist.contains(name)){
					return false;
				}
				File compiledFolder = new File(Configuration.binaryDir+"/"+Configuration.projectDirInSourceFolder+"/"+name);
				if(compiledFolder.exists() && compiledFolder.isDirectory()) {
					return true;
				}
				return false;
			}
		});
		// sort alphabetically
		java.util.Arrays.sort(projects);
		return projects;
	}
	
	/**
	 * This method populates the ProjectSelector with all the subpanels and buttons
	 * 
	 * @param main The object to notify when the ok button is pressed.
	 */
	public void populate(Object main){
		this.main = main;
		
		// gather all projects
		String[] projects = getAllProjectNames();
		if(projects == null) {
			Main.fatalError("Cannot find the project folder. Please ensure that the framework is installed properly.");
		}
		Arrays.sort(projects); // sort the projects in ascending order
		
		this.addComponentListener(new ComponentListener() {
			int oldX = appConfig.projectSelectorWindowPosX, oldY = appConfig.projectSelectorWindowPosY;
			public void componentResized(ComponentEvent e) {
				if(ProjectSelector.this.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
					appConfig.projectSelectorIsMaximized = true;
					appConfig.projectSelectorWindowPosX = oldX;
					appConfig.projectSelectorWindowPosY = oldY;
				} else {
					appConfig.projectSelectorIsMaximized = false;
					appConfig.projectSelectorWindowWidth= ProjectSelector.this.getWidth();
					appConfig.projectSelectorWindowHeight = ProjectSelector.this.getHeight();
				}
				customParameters.setSize(100, customParameters.getHeight()); // needed to ensure that the text field shrinks as well
			}
			public void componentMoved(ComponentEvent e) {
				oldX = appConfig.projectSelectorWindowPosX;
				oldY = appConfig.projectSelectorWindowPosY;
				appConfig.projectSelectorWindowPosX = ProjectSelector.this.getX();
				appConfig.projectSelectorWindowPosY = ProjectSelector.this.getY();
			}
			public void componentShown(ComponentEvent e) {
			}
			public void componentHidden(ComponentEvent e) {
			}
		});
		
		// safe the overal config file when the project selector closes
		this.addWindowListener(new WindowListener() {
			public void windowOpened(WindowEvent e) {
			}
			public void windowClosing(WindowEvent e) {
				appConfig.projectSelectorSelectedTab = 1 + right.getSelectedIndex();
				appConfig.writeConfig();
			}
			public void windowClosed(WindowEvent e) {
			}
			public void windowIconified(WindowEvent e) {
			}
			public void windowDeiconified(WindowEvent e) {
			}
			public void windowActivated(WindowEvent e) {
			}
			public void windowDeactivated(WindowEvent e) {
			}
		});
		
		this.setLayout(new BorderLayout());
		this.setResizable(true);
		if(appConfig.projectSelectorIsMaximized) {
			setExtendedState(JFrame.MAXIMIZED_BOTH);
		}
		this.setSize(new Dimension(appConfig.projectSelectorWindowWidth, appConfig.projectSelectorWindowHeight));
		this.setLocation(appConfig.projectSelectorWindowPosX, appConfig.projectSelectorWindowPosY);
	
		JPanel left = new JPanel();
		left.setLayout(new BorderLayout());
		// List of all projects
		selection.setListData(projects);
		selection.setSelectedValue(appConfig.lastChosenProject, true);
		if(!selection.isSelectionEmpty()) {
			selectedProjectName = (String) selection.getSelectedValue();
		} else {
			selectedProjectName = "";
		}
		selection.addListSelectionListener(this);
		selection.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 0));
		selection.setBackground(listPanel.getBackground());
		listScroller = new JScrollPane(selection);
		listPanel.setLayout(new BorderLayout());
		listScroller.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		listPanel.add(listScroller, BorderLayout.CENTER);
		listPanel.setBorder(BorderFactory.createTitledBorder("Available Projects"));
		left.add(listPanel, BorderLayout.CENTER);
		
		// OK / Cancel buttons
		buttonPanel.add(ok);
		buttonPanel.add(cancel);
		ok.addActionListener(this);
		cancel.addActionListener(this);
		buttonPanel.setPreferredSize(new Dimension(projectListWidth, 40)); 
		left.add(buttonPanel, BorderLayout.SOUTH);
		
		this.add(left, BorderLayout.WEST);
		
		//right.setBorder(BorderFactory.createTitledBorder("Project Description & Configuration"));

		// The tab containing the description of the selected project
		JPanel description = new JPanel();
		description.setLayout(new BorderLayout());
		scrollableDescriptionPane = new JScrollPane(descriptionText);
		description.add(scrollableDescriptionPane);
		descriptionText.setEditable(false);
		descriptionText.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3)); 
		
		int i = selection.getSelectedIndex();
		if(i == -1){
			//there was no defaultProject
			descriptionText.setText("Please select a project.");
		} else {
			generateGUIDescription(selectedProjectName);
		}
		right.addTab("Description", description);
		
		// The tab containing the config-file entries
		configuration.setLayout(new BoxLayout(configuration, BoxLayout.Y_AXIS));
		
		scrollableConfigurationPane = new JScrollPane(frameworkConfigurationPanel);
		//increment the scroll speed (for some reason the speed for the scrollableConfigurationPane is very low)
		scrollableConfigurationPane.getVerticalScrollBar().setUnitIncrement(15);
		
		frameworkConfigurationPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3)); 

		configuration.add(scrollableConfigurationPane);
		JPanel bp = new JPanel();
		bp.add(saveConfig);
		saveConfig.addActionListener(this);
		saveConfig.setMnemonic(java.awt.event.KeyEvent.VK_S);
		configuration.add(bp);
		
		expand = createFrameworkIconButton("expand", "expand.gif", "Show advanced settings");
		collapse = createFrameworkIconButton("collapse", "collapse.gif", "Hide advanced settings");

		customConfigurationPanel.setLayout(new BorderLayout());
		
		JPanel mainCustomConfigurationPanel = new JPanel();
		mainCustomConfigurationPanel.setLayout(new BoxLayout(mainCustomConfigurationPanel, BoxLayout.Y_AXIS));
		mainCustomConfigurationPanel.add(customConfigurationPanel);
		// and the save button
		JPanel bp2 = new JPanel();
		bp2.add(saveConfig2);
		saveConfig2.addActionListener(this);
		saveConfig2.setMnemonic(java.awt.event.KeyEvent.VK_S);
		mainCustomConfigurationPanel.add(bp2);

		right.addTab("Framework Config", configuration);
		right.addTab("Project Config", mainCustomConfigurationPanel);
		right.setMnemonicAt(0, java.awt.event.KeyEvent.VK_D);
		right.setMnemonicAt(1, java.awt.event.KeyEvent.VK_F);
		right.setMnemonicAt(2, java.awt.event.KeyEvent.VK_P);
		right.setSelectedIndex(appConfig.projectSelectorSelectedTab - 1);

		if(i == -1){
			JTextField msg = new JTextField("Please select a project.");
			msg.setEditable(false);
			frameworkConfigurationPanel.add(msg);
		} else {
			generateGUIGonfiguration(selectedProjectName);
		}
		
		
		this.add(right, BorderLayout.CENTER);
	
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		this.getRootPane().setDefaultButton(ok);
		

		this.setVisible(true);
		
		//this.setUndecorated(true);
		//GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
	}

	
	/**
	 * Creates a new icon button where the icon is supposed to be stored in the framework
	 * @param actionCommand Name of the action that is performed when this button is pressed
	 * @param imageName The name of the image file, which is stored in the directory specified by Configuration.imageDir
	 * @param toolTip Tooltip text to be shown for this button
	 * @return A new JButton with an icon
	 */
	protected JButton createFrameworkIconButton(String actionCommand, String imageName, String toolTip) {
		// To support jar files, we cannot access the file directly
		ClassLoader cldr = this.getClass().getClassLoader();
		JButton b = null;
		try {
			URL url = cldr.getResource(Configuration.imageDir + imageName);
			ImageIcon icon = new ImageIcon(url);
			b = new JButton(icon);
		} catch(NullPointerException e) {
			Main.fatalError("Cannot access the application icon " + imageName + ", which should be stored in\n" +
			                Configuration.binaryDir + "/" + Configuration.imageDir + imageName + ".");
			return null;
		}
		//b.setPreferredSize(new Dimension(29, 29));
		b.setPreferredSize(new Dimension(0, 9));
		b.setActionCommand(actionCommand);
		b.setFocusable(false);
		b.setBorderPainted(false);
		//b.setBackground(bgColor);
		b.addActionListener(this);
		//b.addMouseListener(this); // move over the button => draw border
		b.setToolTipText(toolTip);
		return b;
	}

	
	/**
	 * Show the description for a given project
	 * @param projectName The project name
	 */
	private void generateGUIDescription(String projectName){
		File proj = new File(Configuration.sourceDirPrefix+"/"+Configuration.projectDirInSourceFolder+"/"+projectName+"/"+Configuration.descriptionFileName);
		try {
			if(!proj.exists()) {
				descriptionText.setText("There is no description-file in the currently selected project.");
			} else {
				LineNumberReader r = new LineNumberReader(new FileReader(proj));
				String description = "";
				String tmp = null;
				while((tmp = r.readLine()) != null) {
					description += tmp + "\n";
				}
				descriptionText.setText(description);
				descriptionText.setCaretPosition(0);
			}
		} catch (FileNotFoundException e) {
			descriptionText.setText("There is no description-file in the currently selected project.");
		} catch (IOException e) {
			Main.minorError(e);
			descriptionText.setText("There is no description-file in the currently selected project.");
		}
	}
	
	/**
	 * Extracts the custom-text from the config file, without the surrounding <custom></custom> flag
	 * @param aConfigFile The config file
	 * @return The custom text, an empty string upon any failure
	 */
	private String getCustomText(File aConfigFile) {
		LineNumberReader reader = null; 
		try {
			reader = new LineNumberReader(new FileReader(aConfigFile));
		} catch (FileNotFoundException e) {
			return "";
		}
		String result = "";
		boolean inCustom = false;
		String line = null;
		try {
			line = reader.readLine();
			while(line != null) {
				if(!inCustom) {
					if(!line.contains("<Custom>")) {
						line = reader.readLine();
						continue;
					}
					int offset = line.indexOf("<Custom>");
					result = line.substring(offset + 8); // skip the tag <Custom>
					inCustom = true;
				} else {
					result += line + "\n";
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			return "";
		} 
		int offset = result.lastIndexOf("</Custom>");
		if(offset >= 0) {
			result = result.substring(0, offset);
		}
		return result;
	}
	
	MultiLineToolTipJComboBox asynchronousSimulationCB = null;
	MultiLineToolTipJComboBox mobilityCB = null;
	
	/**
	 * Generate the GUI components to show the config for a given project
	 * @param projectName
	 */
	private void generateGUIGonfiguration(String projectName){
		boolean configExists = true;
		
		Element frameworkElement = null;
		
		String configFileName = Configuration.sourceDirPrefix+"/"+Configuration.projectDirInSourceFolder+"/"+projectName+"/"+Configuration.configfileFileName;
		File configFile = new File(configFileName);
		if(!configFile.exists()){
			configExists = false;
		} else try {
			Document doc = new SAXBuilder().build(configFile);
			Element root = doc.getRootElement();
			frameworkElement = root.getChild("Framework");
			Element custom = root.getChild("Custom");
			if(custom == null) {
				Main.fatalError("Invalid configuration file: A Custom entry is missing.\n" +
				                "The file needs to be of the following form: \n" +
				"<Document>\n  <Framework>...</Framework>\n  <Custom></Custom>\n</Document>");
			}
			if(frameworkElement == null) {
				Main.fatalError("Invalid configuration file: A 'framework' entry is missing.\n" +
				                "The file needs to be of the following form: \n" +
				"<Document>\n  <Framework>...</Framework>\n  <Custom></Custom>\n</Document>");
			}
		} catch (JDOMException e1) {
			Main.fatalError("Invalid configuration file:\n\n" + e1.getMessage());
		} catch (IOException e1) {
			Main.fatalError("Cannot open or read from configuration file:\n\n" + e1.getMessage());
		}
		
		projectEntries = new Vector<ConfigEntry>();
		
		String sectionName = "";
		
		Class<?> configClass = Configuration.class;
		// We assume here that the fields are returned in the order they are listed in the java file! 
		Field[] fields = configClass.getDeclaredFields();
		for(int i = 0; i < fields.length; i++){
			Field field = fields[i];
			try {
				// read the annotations for this field
				Configuration.DefaultInConfigFile dan = field.getAnnotation(Configuration.DefaultInConfigFile.class);
				Configuration.OptionalInConfigFile oan = field.getAnnotation(Configuration.OptionalInConfigFile.class);
				Configuration.SectionInConfigFile san = field.getAnnotation(Configuration.SectionInConfigFile.class);
				if(dan != null || oan != null) {
					if(san != null) { // get the title 
						sectionName = san.value();
						projectEntries.add(new ConfigEntry(sectionName, "", Configuration.SectionInConfigFile.class, "", false, field));
					}
					String description = dan != null ? dan.value() : oan.value(); // the description text
					// test whether the XML file contains an entry for this field
					String value = null;
					if(configExists){
						Element e = frameworkElement.getChild(field.getName());
						if(e != null) {
							value = e.getAttributeValue("value"); // null if not there
						}
					}
					if(value == null){
						//there was no entry in the config-file. Take the default value.
						projectEntries.add(new ConfigEntry(field.getName(), Configuration.getConfigurationText(field.get(null)), field.getType(), description, oan != null, field));
					} else { // there is an entry in the XML file
						projectEntries.add(new ConfigEntry(field.getName(), value , field.getType(), description, oan != null, field)); // elem.comment
					}
				}	else if(field.getName().equals("edgeType")) {
					// NOTE: the edgeType member does not carry any annotations (exception)
					String comment = "The default type of edges to be used";
					String value = null;
					if(configExists){
						Element e = frameworkElement.getChild(field.getName());
						if(e != null) {
							value = e.getAttributeValue("value"); // null if not there
						}
					}
					if(value == null){
						//there was no entry in the config-file. Take the default value.
						projectEntries.add(new ConfigEntry(field.getName(), Configuration.getEdgeType(), field.getType(), comment, oan != null, field));
					} else {
						projectEntries.add(new ConfigEntry(field.getName(), value , field.getType(), comment, oan != null, field));
					}	
				}
			} catch (IllegalArgumentException e) {
				Main.fatalError(e);
			} catch (IllegalAccessException e) {
				Main.fatalError(e);
			}
		}
		
		// for each entry, create the corresponding GUI components
		
		asynchronousSimulationCB = null;
		mobilityCB = null;
		for(ConfigEntry e : projectEntries) {
			String ttt = e.comment.equals("") ? null : e.comment; // the tool tip text, don't show at all, if no text to display
			
			// creating the text field
			UnborderedJTextField label;
			if(e.entryClass == Configuration.SectionInConfigFile.class){
				label = new UnborderedJTextField(e.key.toString(), Font.BOLD);
			} else {
				label = new UnborderedJTextField("         " + e.key.toString(), Font.PLAIN);
			}
			label.setToolTipText(ttt);
			e.textComponent = label;
			
			if(e.entryClass == boolean.class){
				String[] ch = {"true", "false"};
				MultiLineToolTipJComboBox booleanChoice = new MultiLineToolTipJComboBox(ch);
				if((e.value).compareTo("true") != 0){ 
					booleanChoice.setSelectedItem("false"); 
				} else {
					booleanChoice.setSelectedItem("true"); 
				}
				booleanChoice.addActionListener(userInputListener); // ! add this listener AFTER setting the value!
				booleanChoice.setToolTipText(ttt);
				e.valueComponent = booleanChoice;
				// special case: mobility can only be changed if simulation is in sync mode.
				if(e.key.equals("asynchronousMode")) {
					asynchronousSimulationCB = booleanChoice;
					if(mobilityCB != null && (e.value).equals("true")) {
						mobilityCB.setSelectedItem("false");
						mobilityCB.setEnabled(false);
					}
				}
				if(e.key.equals("mobility")) {
					mobilityCB = booleanChoice;
					if(asynchronousSimulationCB!= null && asynchronousSimulationCB.getSelectedItem().equals("true")) {
						mobilityCB.setSelectedItem("false");
						mobilityCB.setEnabled(false);
					}
				}
			}	else if(e.entryClass == Configuration.SectionInConfigFile.class){
				e.valueComponent = null; // there's no component for the section
			} else {
				// special case for some text fields that expect the name of an implementation. They 
				// should show the available implementations in a drop down field 
				ImplementationChoiceInConfigFile ian = e.field.getAnnotation(ImplementationChoiceInConfigFile.class); 
				if(ian != null) {
					Vector<String> ch = Global.getImplementations(ian.value(), true);
					MultiLineToolTipJComboBox choice = new MultiLineToolTipJComboBox(ch);
					choice.setEditable(true); // let the user the freedom to enter other stuff (which is likely to be wrong...)
					choice.setSelectedItem(e.value); 
					choice.addActionListener(userInputListener); // ! add this listener AFTER setting the value!
					choice.setToolTipText(ttt);
					e.valueComponent = choice;
				} else {
					if(e.key.equals("javaCmd")) { // special case - this field may contain a lot of text
						JTextArea textArea = new MultiLineToolTipJTextArea(e.value.toString());
						textArea.setToolTipText(ttt);
						textArea.setBorder((new JTextField()).getBorder()); // copy the border
						textArea.setLineWrap(true);
						//textArea.setPreferredSize(new Dimension(50, 30));
						textArea.addKeyListener(userInputListener);
						e.valueComponent = textArea;
					} else {
						MultiLineToolTipJTextField textField = new MultiLineToolTipJTextField(e.value.toString());
						textField.setToolTipText(ttt);
						textField.addKeyListener(userInputListener);
						e.valueComponent = textField;
					}
				}
			}
		}
		// and finally add all the entries
		insertProjectEntries();
		
		
		customConfigurationPanel.removeAll();
		
		// And add the custom entries
		
//   this code snipped was used to redirect the mouse wheel applied on this 
//   text field to the entire tab when the custom config was below the framework config
//		// remove all mouse wheel listeners from the text input
//		for(MouseWheelListener mwl : customParameters.getMouseWheelListeners()) {
//			customParameters.removeMouseWheelListener(mwl);
//		}
//		// and add the 'global' one
//		customParameters.addMouseWheelListener(new MouseWheelForwarder(scrollableConfigurationPane.getMouseWheelListeners()));

		customParameters.setTabSize(3);
		customParameters.setLineWrap(true);
		customParameters.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3)); 
		
		if(configExists) {
			customParameters.setText(getCustomText(configFile));
		} else {
			customParameters.setText("");
		}
		
		customParameters.addKeyListener(userInputListener); // ! add this listener AFTER setting the text ! 
		
		JScrollPane customScroll = new JScrollPane(customParameters, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		customScroll.setWheelScrollingEnabled(true);
		customConfigurationPanel.add(customScroll);
		
		userInputListener.reset();
		
		super.repaint();
	}
		
	/**
	 * Adds the default entries to the GUI
	 * 
	 * Thanks to this method, the user can show and hide the optional settings and keep what he has already entered.
	 */
	private void insertProjectEntries() {
		frameworkConfigurationPanel.removeAll();
		frameworkConfigurationPanel.setLayout(new BorderLayout());
		JPanel entryTable = new JPanel();
		frameworkConfigurationPanel.add(entryTable, BorderLayout.CENTER);
		if(showOptionalFields) {
			frameworkConfigurationPanel.add(collapse, BorderLayout.SOUTH); // add the 'expand' button
		} else {
			frameworkConfigurationPanel.add(expand, BorderLayout.SOUTH); // add the 'expand' button
		}

		entryTable.removeAll();
		ConfigEntry title = null; // only print titles if there are entries shown for this title
		
		int numEntryTableLines = 0; // count number of rows added to entryTable
		
		for(ConfigEntry e : projectEntries) {
			if(e.valueComponent == null) { // it's a title
				title = e;
				continue;
			}
			if(e.isOptional && !showOptionalFields) {
				continue;
			}
			if(title != null) { // first print the title
				entryTable.add(title.textComponent);
				MultiLineToolTipJTextField textField = new MultiLineToolTipJTextField(e.value.toString());
				textField.setVisible(false); // add a place-holder
				entryTable.add(textField);
				title = null;
				numEntryTableLines++;
			}
			entryTable.add(e.textComponent);
			entryTable.add(e.valueComponent);
			numEntryTableLines++;
		}
		NonRegularGridLayout nrgl = new NonRegularGridLayout(numEntryTableLines,2, 5, 2);
		nrgl.setAlignToLeft(true);
		entryTable.setLayout(nrgl);		
	}

	/**
	 * Validates the custom configuration and displays, if necessary, error messages. 
	 * @return The XML document describing the custom configuration, null if the configuration did not parse properly.
	 */
	private Document validateCustomFields() {
		Document doc = null;
		try {
			String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Document><Custom>" + customParameters.getText() + "</Custom></Document>"; 
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
		Document customConfig = validateCustomFields();
		if(customConfig == null) {
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
		
		for(ConfigEntry e : projectEntries) {
			if(e.valueComponent != null) { // there is a value field in the GUI
				if(e.comment != "") { // the comment is not "", add it
					framework.addContent(new Comment(e.comment.replace("\n", " "))); // without the newline chars
				}
				// get the value of this entry from the GUI
				String value = "";
				if(e.valueComponent instanceof JComboBox){
					value = (String)((JComboBox) e.valueComponent).getSelectedItem();
				} else if(e.valueComponent instanceof JTextComponent) {
					value = ((JTextComponent) e.valueComponent).getText();
				}	
				// create and add a new entry in the XML file				
				Element elem = new Element(e.key);
				elem.setAttribute("value", value);
				framework.addContent(elem);
				framework.addContent(new Element("_xml_NL_")); // after each entry, we would like a new-line in the document - these tags are replaced in a second step
			} else {
				// this is a section entry, which will be inserted as comment
				//String line = " - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ";
				String line = "***********************************************************************";
				framework.addContent(new Comment(line));
				String name = "  " + e.key;
				while(name.length() < line.length()) { // fill the name with spaces s.t. the '-->' are aligned
					name += " ";
				}
				framework.addContent(new Comment(name));
				framework.addContent(new Comment(line));
			}
		}
		
		String outputPath = Configuration.sourceDirPrefix+"/"+Configuration.projectDirInSourceFolder+"/"+ selectedProjectName;
		File outputFile = new File(outputPath+"/"+Configuration.configfileFileName + (isTemporary ? ".run" : ""));
		
		// And write the xml tree to the file
		XMLOutputter outputter = new XMLOutputter();
		Format f = Format.getPrettyFormat();
		f.setIndent("\t");
		outputter.setFormat(f);
		File tempOutputFile = new File(outputPath+"/"+Configuration.configfileFileName + ".temp");
		try {
			FileWriter fW = new FileWriter(tempOutputFile);
			outputter.output(doc, fW);
			fW.flush();
			fW.close();
		} catch (IOException e) {
			Main.minorError("Could not write a temporary configuration file!\n\n" + e.getMessage());
			return;
		}

		// in a second step, parse the temp file, replace the _xml_nl_ by new-lines and the _xml_custom_ by the custom text
		
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));
			LineNumberReader input = new LineNumberReader(new FileReader(tempOutputFile));
			String line = input.readLine();
			while(line != null) {
				if(line.contains("<_xml_NL_")) {
					output.newLine();
				} else if(line.contains("<_xml_custom_")) {
					output.write(customParameters.getText());
				} else {
					output.write(line);
					output.newLine();
				}
				line = input.readLine();
			}
			output.flush();
			output.close();
			input.close();
			tempOutputFile.delete();
		} catch (IOException e1) {
			Main.minorError("Could not write the configuration file!\n\n" + e1.getMessage());
		}

		userInputListener.reset(); // finally reset the 'modified' flag
	}
	
	
	/************************************************************************************/
	/* WRITING BACK */
	/************************************************************************************/
	
	/**
	 * This method overwrites the values in the config file by the ones set in the project selector configuration.
	 */
	private void setFrameworkConfig(){
		for(ConfigEntry e : projectEntries) {
			if(e.valueComponent == null){
				continue; // this entry does not have a value - its probably a section header
			}
			String value = "";
			if(e.valueComponent instanceof JComboBox){
				value = (String)((JComboBox)e.valueComponent).getSelectedItem();
			}	else {
				value = ((JTextComponent)e.valueComponent).getText();
			}
			Configuration.setFrameworkConfigurationEntry(e.key, value);
		}
	}
	
	

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource().equals(saveConfig) || e.getSource().equals(saveConfig2)){ // --------------------------------------------------------------------
			if(selection.getSelectedValue() == null){
				JOptionPane.showMessageDialog(this, "Please select a project from the selection.", "No project selected.", JOptionPane.ERROR_MESSAGE);
			} else try {
				storeConfig(false);
			} catch(Exception ex){
				Main.fatalError(ex);
			}
		} else if(e.getSource().equals(ok)) { // -------------------------------------------------------------------- 
			if(selection.getSelectedValue() == null){
				JOptionPane.showMessageDialog(this, "Please select a project from the selection.", "No project selected.", JOptionPane.ERROR_MESSAGE);
			}	else {
				if(userInputListener.isModified()) {
					// the user has modified the config, but not stored it
					int decision = JOptionPane.showConfirmDialog(this, "The modifications to this configuration have not yet been saved.\n\nDo you want to store this configuration, such that it is also available for subsequent runs?", "Unsaved modifications", JOptionPane.YES_NO_CANCEL_OPTION);
					if(decision == JOptionPane.YES_OPTION) { // store
						try {
							storeConfig(false);
						} catch(Exception ex){
							Main.fatalError(ex);
						}
					}
					if(decision == JOptionPane.CANCEL_OPTION) {
						return; // don't do anything
					}
				}
				
				Document customDoc = validateCustomFields();
				if(customDoc == null) { // there is invalid xml in the custom entry
					return;
				}
				
				if(!selectedProjectName.equals("defaultProject")){
					Global.projectName = selectedProjectName;
					Global.useProject = true;
					appConfig.lastChosenProject = Global.projectName;
				}

				Element customEle = customDoc.getRootElement().getChild("Custom");
				XMLParser.parseCustom(customEle, "");
				
				setFrameworkConfig();

				//Block the overwriting of the now set values.
				XMLParser.blockParse = true;

				this.setVisible(false);
				
				//reset the tooltip dismiss delay to the default value
				ToolTipManager.sharedInstance().setDismissDelay(defaultTooltipDismissDelay);
				
				appConfig.projectSelectorSelectedTab = 1 + right.getSelectedIndex();
				appConfig.writeConfig(); // write the config, s.t. when the main application crashes, at least the project selector config is preserved
				storeConfig(true); // store the config to a file s.t. the simulation process can read it
				
				//wake up the waiting object.
				synchronized(main){
					main.notify();
				}
			}
		}	else if(e.getSource().equals(cancel)){ // --------------------------------------------------------------------
			if(userInputListener.isModified()) {
				// the user has modified the config, but not stored it
				int decision = JOptionPane.showConfirmDialog(this, "The configuration for project '" + selectedProjectName + "' has unsaved changes. Do you wish to save them?", 
				                                             "Save Changes?", JOptionPane.YES_NO_CANCEL_OPTION);
				if(decision == JOptionPane.YES_OPTION) { // store
					try {
						storeConfig(false);
					} catch(Exception ex){
						Main.fatalError(ex);
					}
				}
				if(decision == JOptionPane.CANCEL_OPTION) {
					return;
				}
			}
			appConfig.projectSelectorSelectedTab = 1 + right.getSelectedIndex();
			appConfig.writeConfig();
			System.exit(1);
		} else if(e.getSource().equals(collapse)) { // --------------------------------------------------------------------
			showOptionalFields = false;
			insertProjectEntries();
			this.repaint();
		} else if(e.getSource().equals(expand)) { // --------------------------------------------------------------------
			showOptionalFields = true;
			insertProjectEntries();
			this.repaint();
		} 
	}

	public void valueChanged(ListSelectionEvent e) {
		if(!e.getValueIsAdjusting()){
			if(userInputListener.isModified()) {
				// the user has modified the config, but not stored it
				int decision = JOptionPane.showConfirmDialog(this, "The configuration for project '" + selectedProjectName + "' has unsaved changes. Do you wish to save them?", 
				                                             "Save Changes?", JOptionPane.YES_NO_CANCEL_OPTION);
				if(decision == JOptionPane.YES_OPTION) { // store
					try {
						storeConfig(false);
					} catch(Exception ex){
						Main.fatalError(ex);
					}
				}
				if(decision == JOptionPane.CANCEL_OPTION) {
					selection.removeListSelectionListener(this);
					selection.setSelectedValue(selectedProjectName, true);
					selection.addListSelectionListener(this);
					return;
				}
			}
			selectedProjectName = (String) selection.getSelectedValue();
			generateGUIGonfiguration(selectedProjectName);
			generateGUIDescription(selectedProjectName);
		}
	}
	
	/**
	 * Input listener that listens to all user-input fields to decide whether
	 * the user has modified the configuration somehow.
	 */
	private class UserInputListener implements KeyListener, ActionListener {
		private boolean isModified = false;
		
		public boolean isModified() {
			return isModified;
		}
		
		public void reset() {
			saveConfig.setEnabled(false);
			saveConfig2.setEnabled(false);
			isModified = false;
		}
		
		public void setModified() {
			saveConfig.setEnabled(true);
			saveConfig2.setEnabled(true);
			isModified = true;
		}
		
		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
		 */
		public void keyTyped(KeyEvent e) {
			setModified();
		}

		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
		 */
		public void keyPressed(KeyEvent e) {
		}

		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
		 */
		public void keyReleased(KeyEvent e) {
		}

		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent e) {
			setModified();
			test(e);
		}
		
		private void test(ActionEvent e) {
			if(e.getSource() == asynchronousSimulationCB) {
				if(mobilityCB != null && asynchronousSimulationCB.getSelectedItem().equals("true")) {
					mobilityCB.setSelectedItem("false");
					mobilityCB.setEnabled(false);
				} else {
					mobilityCB.setEnabled(true);
				}
			}
		}
	}
	
	private class ConfigEntry {
		/**
		 * The key of the Pair.
		 */
		public String key;
		/**
		 * The value of the Pair.
		 */
		public String value;
		
		/**
		 * The class of the ConfigEntry.
		 */
		public Class<?> entryClass;
		
		/**
		 * The comment for the entry.
		 */
		public String comment;
		
		/**
		 * True if the entry is only shown in the extended settings
		 */
		public boolean isOptional;
		
		/**
		 * The variable-field of the config file (to access the annotations)
		 */
		public Field field;
		
		/**
		 * The GUI component for this entry that holds the value
		 * for this entry.
		 * This member is set only when the GUI is created, and may remain
		 * NULL when this entry does not have a value field (e.g. a comment, 
		 * or section header) 
		 */
		public JComponent valueComponent = null;
		
		/**
		 * The GUI component for this entry that holds the text
		 * for this entry.
		 * This member is set only when the GUI is created.
		 */
		public JComponent textComponent = null;
		
		/**
		 * Constructor for the Pair-class.
		 *
		 * @param k The key to store.
		 * @param v The value to store.
		 * @param c The class of the entry.
		 * @param comment The comment for the entry.
		 */
		private ConfigEntry(String k, String v, Class<?> c, String comment, boolean isOptional, Field field){
			key = k;
			value = v;
			entryClass = c;
			this.comment = comment;
			this.isOptional = isOptional;
			this.field = field;
		}
	}
	
//	private class MouseWheelForwarder implements MouseWheelListener{
//
//		private MouseWheelListener[] mwlArray = null;
//		
//		private MouseWheelForwarder(MouseWheelListener[] mwlArray){
//			this.mwlArray = mwlArray;
//		}
//		
//		public void mouseWheelMoved(MouseWheelEvent arg0) {
//			for(int i = 0; i < mwlArray.length; i++){
//				mwlArray[i].mouseWheelMoved(arg0);
//			}
//		}
//		
//	}
}
