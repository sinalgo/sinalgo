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
package sinalgo.gui.dialogs;


import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.gui.GUI;
import sinalgo.gui.GuiHelper;
import sinalgo.gui.helper.NonRegularGridLayout;
import sinalgo.gui.helper.UnborderedJTextField;
import sinalgo.models.ConnectivityModel;
import sinalgo.models.DistributionModel;
import sinalgo.models.InterferenceModel;
import sinalgo.models.MobilityModel;
import sinalgo.models.Model;
import sinalgo.models.ReliabilityModel;
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.runtime.Runtime;
import sinalgo.tools.Tools;


/**
 * The Dialog to generate a number of new Nodes.
 */
@SuppressWarnings("serial")
public class GenerateNodesDialog extends JDialog implements ActionListener, ProgressBarUser{

	private static JTextField number = new JTextField(6); { // static: keep the value for subsequent calls
		number.setText(Integer.toString(AppConfig.getAppConfig().generateNodesDlgNumNodes));
	} 
	int numberOfNodes; // 'number' field translated to an int, set after a call to readSelection() 
	private JButton ok = new JButton("Ok");
	private JButton cancel = new JButton("Cancel");
	
	private JComboBox distributionModelComboBox = new JComboBox();
	private String distributionSel = Configuration.DefaultDistributionModel;
	private JTextField distributionParam = new JTextField(20);
	private String distributionParamDefString = "";
	
	private JComboBox nodeTypeComboBox = new JComboBox();
	private String nodeTypeSel = Configuration.DefaultNodeImplementation;
	private JTextField nodeTypeParam = new JTextField(20);
	
	private JComboBox connectivityModelComboBox = new JComboBox();
	private String connectivitySel = Configuration.DefaultConnectivityModel;
	private JTextField connectivityParam = new JTextField(20);
	private String connectivityDefString = "";
	
	private JComboBox interferenceModelComboBox = new JComboBox();
	private String interferenceSel = Configuration.DefaultInterferenceModel;
	private String interferenceDefString = "";
	private JTextField interferenceParam = new JTextField(20);
	
	private JComboBox mobilityModelComboBox = new JComboBox();
	private String mobilitySel = Configuration.DefaultMobilityModel;
	private String mobilityDefString = "";
	private JTextField mobilityParam = new JTextField(20);
	
	private JComboBox reliabilityModelComboBox = new JComboBox();
	private String reliabilitySel = Configuration.DefaultReliabilityModel;
	private String reliabilityDefString = "";
	private JTextField reliabilityParam = new JTextField(20);
	
	JCheckBox allModelsCheckBox;
	
	private PercentualProgressDialog pf = null;
	private Vector<Node> addedNodes = new Vector<Node>();
	private boolean canceled = false;
	
	private GUI parent;
	
	private Position singleNodePosition; // null if the dialgo was created for several nodes
	
	/**
	 * The constructor for the GenerateNodesDialog class.
	 *
	 * @param p The parent Frame to add the Dialog to.
	 */
	public GenerateNodesDialog(GUI p){
		super(p, "Create new Nodes", true);
		GuiHelper.setWindowIcon(this);
		cancel.addActionListener(this);
		ok.addActionListener(this);
		
		Font f = distributionModelComboBox.getFont().deriveFont(Font.PLAIN);
		distributionModelComboBox.setFont(f);
		nodeTypeComboBox.setFont(f);
		connectivityModelComboBox.setFont(f);
		interferenceModelComboBox.setFont(f);
		mobilityModelComboBox.setFont(f);
		reliabilityModelComboBox.setFont(f);

		// Detect ESCAPE button
		KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		focusManager.addKeyEventPostProcessor(new KeyEventPostProcessor() {
			public boolean postProcessKeyEvent(KeyEvent e) {
				if(!e.isConsumed() && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					GenerateNodesDialog.this.setVisible(false);
				}
				return false;
			}
		});
		
		this.setLocationRelativeTo(p);
		this.parent = p;
	}
	
	/**
	 *  The method to generate an GenerateNodesDialog.
	 *  @param singleNodePos If the dialog is for a single node, this parameter
	 *  contains the position where the node should be placed. For displaying the
	 *  dialog for several nodes, set this parameter to null.  
	 */
	public void compose(Position singleNodePos){
		singleNodePosition = singleNodePos;
		JPanel cp = new JPanel();
		
		cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
		cp.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		
		JPanel dist = new JPanel();
		dist.setBorder(BorderFactory.createTitledBorder("Node Distribution"));
		dist.setLayout(new BoxLayout(dist, BoxLayout.Y_AXIS));
		if(singleNodePos == null) {
			cp.add(dist);
		}
		JPanel distSel = new JPanel();
		distSel.setBorder(BorderFactory.createEmptyBorder(0,3,3,3));
		distSel.setLayout(new NonRegularGridLayout(2, 3, 5, 2));
		
		// Number of nodes
		// ----------------------------
		UnborderedJTextField numSelLabel = new UnborderedJTextField("Number of Nodes:", Font.BOLD);
		distSel.add(numSelLabel);
		distSel.add(number);
		JTextField dummy = new JTextField();
		dummy.setVisible(false);
		distSel.add(dummy);
		
		// the available distrubutions
		// ----------------------------
		UnborderedJTextField distSelLabel = new UnborderedJTextField("Distribution Model:", Font.BOLD);
		distSel.add(distSelLabel);
		fillChoice(distributionModelComboBox, "models/distributionModels", distributionSel);
		distSel.add(distributionModelComboBox);
		distributionParam.setText(distributionParamDefString);
		distSel.add(distributionParam);
		dist.add(distSel);		

		JPanel propertyPanel = new JPanel();
		propertyPanel.setBorder(BorderFactory.createTitledBorder("Node Properties"));
		propertyPanel.setLayout(new BoxLayout(propertyPanel, BoxLayout.Y_AXIS));
		cp.add(propertyPanel);
		JPanel props = new JPanel();
		props.setBorder(BorderFactory.createEmptyBorder(0,3,3,3));
		props.setLayout(new NonRegularGridLayout(5,3, 5, 2));
		propertyPanel.add(props);
		
		
		// the available node implementations
		// ----------------------------
		UnborderedJTextField propSelLabel = new UnborderedJTextField("Node Implementation:", Font.BOLD);
		props.add(propSelLabel);
		fillChoice(nodeTypeComboBox, "nodes/nodeImplementations", nodeTypeSel);
		props.add(nodeTypeComboBox);
		nodeTypeParam.setEditable(false);
		nodeTypeParam.setVisible(false);
		props.add(nodeTypeParam);

		//the available connectivities
		// ----------------------------
		UnborderedJTextField connSelLabel = new UnborderedJTextField("Connectivity Model:", Font.BOLD);
		props.add(connSelLabel);
		fillChoice(connectivityModelComboBox, "models/connectivityModels", connectivitySel);
		props.add(connectivityModelComboBox);
		connectivityParam.setText(connectivityDefString);
		props.add(connectivityParam);
		
		//the available interferences
		// ----------------------------
		UnborderedJTextField interSelLabel = new UnborderedJTextField("Interference Model:", Font.BOLD);
		props.add(interSelLabel);
		fillChoice(interferenceModelComboBox, "models/interferenceModels", interferenceSel);
		props.add(interferenceModelComboBox);	
		interferenceParam.setText(interferenceDefString);
		props.add(interferenceParam);
		
		//the available mobility
		// ----------------------------
		UnborderedJTextField mobSelLabel = new UnborderedJTextField("Mobility Model:", Font.BOLD);
		props.add(mobSelLabel);
		fillChoice(mobilityModelComboBox, "models/mobilityModels", mobilitySel);
		props.add(mobilityModelComboBox);
		mobilityParam.setText(mobilityDefString);
		props.add(mobilityParam);
		
		//the available reliability models
		// ----------------------------
		UnborderedJTextField reliSelLabel = new UnborderedJTextField("Reliability Model:", Font.BOLD);
		props.add(reliSelLabel);
		fillChoice(reliabilityModelComboBox, "models/reliabilityModels", reliabilitySel);
		props.add(reliabilityModelComboBox);
		reliabilityParam.setText(reliabilityDefString);
		props.add(reliabilityParam);
		

		// add a button to change whether all implementations are contained in the drop down fields
		JPanel allModelsPanel = new JPanel();
		allModelsPanel.setLayout(new BorderLayout());
		allModelsCheckBox = new JCheckBox("Show all implementations");
		allModelsCheckBox.setSelected(Configuration.showModelsOfAllProjects);
		allModelsCheckBox.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if(Configuration.showModelsOfAllProjects != allModelsCheckBox.isSelected()) {
					Configuration.showModelsOfAllProjects = allModelsCheckBox.isSelected();
					// reload the contents of the drop down fields
					fillChoice(distributionModelComboBox, "models/distributionModels", distributionSel);
					fillChoice(nodeTypeComboBox, "nodes/nodeImplementations", nodeTypeSel);			
					fillChoice(connectivityModelComboBox, "models/connectivityModels", connectivitySel);
					fillChoice(interferenceModelComboBox, "models/interferenceModels", interferenceSel);			
					fillChoice(mobilityModelComboBox, "models/mobilityModels", mobilitySel);			
					fillChoice(reliabilityModelComboBox, "models/reliabilityModels", reliabilitySel);		
					GenerateNodesDialog.this.pack();
				}
			}
		});
		allModelsPanel.add(allModelsCheckBox);
		cp.add(allModelsPanel);

		// the buttons OK / CANCEL
		JPanel buttons = new JPanel();
		ok.setMnemonic(java.awt.event.KeyEvent.VK_O);
		buttons.add(ok);		
		cancel.setMnemonic(java.awt.event.KeyEvent.VK_C);
		buttons.add(cancel);
		
		cp.add(buttons);
		
		this.setContentPane(cp);

		//this.setResizable(false);
		this.getRootPane().setDefaultButton(ok);
		this.pack();
		this.setLocationRelativeTo(parent);
		number.grabFocus();
		this.setVisible(true);
	}
	
	private void fillChoice(JComboBox c, String implDir, String selection) {
		c.removeAllItems();
		Vector<String> names = Global.getImplementations(implDir);
		if(!names.contains(selection)) {
			names.add(selection);
		}
		for(String s : names) {
			c.addItem(s);
		}
		c.setSelectedItem(selection);
	}
	
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event) {
		if(event.getActionCommand().equals(ok.getActionCommand())){
			if(singleNodePosition == null) try {
				int num = Integer.parseInt(number.getText());
				if(num <= 0){
					throw new NumberFormatException();
				}
				AppConfig.getAppConfig().generateNodesDlgNumNodes = num;
				pf = new PercentualProgressDialog(this, this, "Creating new Nodes...");
				canceled = false;
				pf.init();
			}	catch(NumberFormatException nfE){
				Main.minorError("Please specify a correct number of nodes to generate. This number has to be an integer greater than zero.");
			} 
			else { // only for a single node
				readSelection();
				Node n = generateNode(singleNodePosition);
				n.finishInitializationWithDefaultModels(true);
				// redraw
				Tools.getGraphPanel().forceDrawInNextPaint();
				Tools.getGraphPanel().repaint();
				this.setVisible(false);
			}
		}
		else if(event.getActionCommand().equals(cancel.getActionCommand())){
			this.setVisible(false);
		}
	}

	/* (non-Javadoc)
	 * @see sinalgo.gui.dialogs.ProgressBarUser#cancelClicked()
	 */
	public void cancelClicked() {
		this.canceled = true;
	}
	
	/**
	 * Reads and stores the selction the user made 
	 */
	private void readSelection() {
		numberOfNodes = Integer.parseInt(number.getText());
		distributionSel = distributionModelComboBox.getSelectedItem().toString();
		nodeTypeSel = nodeTypeComboBox.getSelectedItem().toString();
		interferenceSel = interferenceModelComboBox.getSelectedItem().toString();
		mobilitySel = mobilityModelComboBox.getSelectedItem().toString();
		reliabilitySel = reliabilityModelComboBox.getSelectedItem().toString();
		connectivitySel = connectivityModelComboBox.getSelectedItem().toString();
		
		distributionParamDefString = distributionParam.getText();
		interferenceDefString = interferenceParam.getText();
		mobilityDefString = mobilityParam.getText();
		reliabilityDefString = reliabilityParam.getText();
		connectivityDefString = connectivityParam.getText();
	}
	
	private Node generateNode(Position pos) {
		Node node = Node.createNodeByClassname(nodeTypeSel);
		node.setPosition(pos);

		InterferenceModel im = Model.getInterferenceModelInstance(interferenceSel);
		im.setParamString(interferenceDefString);
		node.setInterferenceModel(im);
		
		MobilityModel mm = Model.getMobilityModelInstance(mobilitySel);
		mm.setParamString(mobilityDefString);
		node.setMobilityModel(mm);
		
		ReliabilityModel rm = Model.getReliabilityModelInstance(reliabilitySel);
		rm.setParamString(reliabilityDefString);
		node.setReliabilityModel(rm);
		
		ConnectivityModel cm = Model.getConnectivityModelInstance(connectivitySel);
		cm.setParamString(connectivityDefString);
		node.setConnectivityModel(cm);
		return node;
	}
	
	/**
	 * Creates a node with the default settings.
	 * @param pos The position of the new node
	 * @return the node.
	 */
	public Node generateDefaultNode(Position pos) {
		Node n = generateNode(pos);
		n.finishInitializationWithDefaultModels(true);
		return n;
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.gui.dialogs.ProgressBarUser#performMethod()
	 */
	public void performMethod() {
		readSelection();
		try{
			DistributionModel distribution = Model.getDistributionModelInstance(distributionSel);
			distribution.setParamString(distributionParamDefString);
			distribution.setNumberOfNodes(numberOfNodes);
			distribution.initialize();
			
			addedNodes = new Vector<Node>();
			
			for(int i = 0; i < numberOfNodes; i++){
				pf.setPercentage(100.0d * ((double)i/(double)numberOfNodes));
				
				Node node = generateNode(distribution.getNextPosition());
				
				if(canceled){
					for(Node n:addedNodes){
						Runtime.nodes.removeNode(n);
						i--;
						pf.setPercentage(100.0d * ((double)i/(double)numberOfNodes));
					}
					pf.finish();
					addedNodes.clear();
					return;
				}

				node.finishInitializationWithDefaultModels(true);
				addedNodes.add(node);
			}
		} catch(WrongConfigurationException e){
			Main.minorError("There was an error while generating the nodes.\n" + e.getMessage());
		}
		pf.finish();
		Tools.getGraphPanel().forceDrawInNextPaint();
		Tools.getGraphPanel().repaint();
		this.setVisible(false);
	}

}
