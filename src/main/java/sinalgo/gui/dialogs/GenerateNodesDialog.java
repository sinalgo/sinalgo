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
package sinalgo.gui.dialogs;

import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration;
import sinalgo.configuration.Configuration.ImplementationChoiceInConfigFile.ImplementationType;
import sinalgo.exception.WrongConfigurationException;
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
import sinalgo.runtime.SinalgoRuntime;
import sinalgo.tools.Tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import static sinalgo.configuration.Configuration.ImplementationChoiceInConfigFile.ImplementationType.MODELS_CONNECTIVITY;
import static sinalgo.configuration.Configuration.ImplementationChoiceInConfigFile.ImplementationType.MODELS_DISTRIBUTION;
import static sinalgo.configuration.Configuration.ImplementationChoiceInConfigFile.ImplementationType.MODELS_INTERFERENCE;
import static sinalgo.configuration.Configuration.ImplementationChoiceInConfigFile.ImplementationType.MODELS_MOBILITY;
import static sinalgo.configuration.Configuration.ImplementationChoiceInConfigFile.ImplementationType.MODELS_RELIABILITY;
import static sinalgo.configuration.Configuration.ImplementationChoiceInConfigFile.ImplementationType.NODES_IMPLEMENTATIONS;

/**
 * The Dialog to generate a number of new Nodes.
 */
public class GenerateNodesDialog extends JDialog implements ActionListener, ProgressBarUser {

    private static final long serialVersionUID = -8080111497886244380L;

    private static JTextField number = new JTextField(6);

    // static: keep the value for subsequent calls
    static {
        number.setText(Integer.toString(AppConfig.getAppConfig().getGenerateNodesDlgNumNodes()));
    }

    private int numberOfNodes; // 'number' field translated to an int, set after a call to readSelection()
    private JButton ok = new JButton("Ok");
    private JButton cancel = new JButton("Cancel");

    private JComboBox distributionModelComboBox = new JComboBox();
    private String distributionSel = Configuration.getDefaultDistributionModel();
    private JTextField distributionParam = new JTextField(20);
    private String distributionParamDefString = "";

    private JComboBox nodeTypeComboBox = new JComboBox();
    private String nodeTypeSel = Configuration.getDefaultNodeImplementation();
    private JTextField nodeTypeParam = new JTextField(20);

    private JComboBox connectivityModelComboBox = new JComboBox();
    private String connectivitySel = Configuration.getDefaultConnectivityModel();
    private JTextField connectivityParam = new JTextField(20);
    private String connectivityDefString = "";

    private JComboBox interferenceModelComboBox = new JComboBox();
    private String interferenceSel = Configuration.getDefaultInterferenceModel();
    private String interferenceDefString = "";
    private JTextField interferenceParam = new JTextField(20);

    private JComboBox mobilityModelComboBox = new JComboBox();
    private String mobilitySel = Configuration.getDefaultMobilityModel();
    private String mobilityDefString = "";
    private JTextField mobilityParam = new JTextField(20);

    private JComboBox reliabilityModelComboBox = new JComboBox();
    private String reliabilitySel = Configuration.getDefaultReliabilityModel();
    private String reliabilityDefString = "";
    private JTextField reliabilityParam = new JTextField(20);

    private JCheckBox allModelsCheckBox;

    private PercentualProgressDialog pf = null;
    private boolean canceled = false;

    private GUI parent;

    private Position singleNodePosition; // null if the dialgo was created for several nodes

    /**
     * The constructor for the GenerateNodesDialog class.
     *
     * @param p The parent Frame to add the Dialog to.
     */
    public GenerateNodesDialog(GUI p) {
        super(p, "Create new Nodes", true);
        GuiHelper.setWindowIcon(this);
        this.cancel.addActionListener(this);
        this.ok.addActionListener(this);

        Font f = this.distributionModelComboBox.getFont().deriveFont(Font.PLAIN);
        this.distributionModelComboBox.setFont(f);
        this.nodeTypeComboBox.setFont(f);
        this.connectivityModelComboBox.setFont(f);
        this.interferenceModelComboBox.setFont(f);
        this.mobilityModelComboBox.setFont(f);
        this.reliabilityModelComboBox.setFont(f);

        // Detect ESCAPE button
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventPostProcessor(e -> {
            if (!e.isConsumed() && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                GenerateNodesDialog.this.setVisible(false);
            }
            return false;
        });

        this.setLocationRelativeTo(p);
        this.parent = p;
    }

    /**
     * The method to generate an GenerateNodesDialog.
     *
     * @param singleNodePos If the dialog is for a single node, this parameter contains the
     *                      position where the node should be placed. For displaying the
     *                      dialog for several nodes, set this parameter to null.
     */
    public void compose(Position singleNodePos) {
        this.singleNodePosition = singleNodePos;
        JPanel cp = new JPanel();

        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        cp.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        JPanel dist = new JPanel();
        dist.setBorder(BorderFactory.createTitledBorder("Node Distribution"));
        dist.setLayout(new BoxLayout(dist, BoxLayout.Y_AXIS));
        if (singleNodePos == null) {
            cp.add(dist);
        }
        JPanel distSel = new JPanel();
        distSel.setBorder(BorderFactory.createEmptyBorder(0, 3, 3, 3));
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
        this.fillChoice(this.distributionModelComboBox, MODELS_DISTRIBUTION, this.distributionSel);
        distSel.add(this.distributionModelComboBox);
        this.distributionParam.setText(this.distributionParamDefString);
        distSel.add(this.distributionParam);
        dist.add(distSel);

        JPanel propertyPanel = new JPanel();
        propertyPanel.setBorder(BorderFactory.createTitledBorder("Node Properties"));
        propertyPanel.setLayout(new BoxLayout(propertyPanel, BoxLayout.Y_AXIS));
        cp.add(propertyPanel);
        JPanel props = new JPanel();
        props.setBorder(BorderFactory.createEmptyBorder(0, 3, 3, 3));
        props.setLayout(new NonRegularGridLayout(5, 3, 5, 2));
        propertyPanel.add(props);

        // the available node implementations
        // ----------------------------
        UnborderedJTextField propSelLabel = new UnborderedJTextField("Node Implementation:", Font.BOLD);
        props.add(propSelLabel);
        this.fillChoice(this.nodeTypeComboBox, NODES_IMPLEMENTATIONS, this.nodeTypeSel);
        props.add(this.nodeTypeComboBox);
        this.nodeTypeParam.setEditable(false);
        this.nodeTypeParam.setVisible(false);
        props.add(this.nodeTypeParam);

        // the available connectivities
        // ----------------------------
        UnborderedJTextField connSelLabel = new UnborderedJTextField("Connectivity Model:", Font.BOLD);
        props.add(connSelLabel);
        this.fillChoice(this.connectivityModelComboBox, MODELS_CONNECTIVITY, this.connectivitySel);
        props.add(this.connectivityModelComboBox);
        this.connectivityParam.setText(this.connectivityDefString);
        props.add(this.connectivityParam);

        // the available interferences
        // ----------------------------
        UnborderedJTextField interSelLabel = new UnborderedJTextField("Interference Model:", Font.BOLD);
        props.add(interSelLabel);
        this.fillChoice(this.interferenceModelComboBox, MODELS_INTERFERENCE, this.interferenceSel);
        props.add(this.interferenceModelComboBox);
        this.interferenceParam.setText(this.interferenceDefString);
        props.add(this.interferenceParam);

        // the available mobility
        // ----------------------------
        UnborderedJTextField mobSelLabel = new UnborderedJTextField("Mobility Model:", Font.BOLD);
        props.add(mobSelLabel);
        this.fillChoice(this.mobilityModelComboBox, MODELS_MOBILITY, this.mobilitySel);
        props.add(this.mobilityModelComboBox);
        this.mobilityParam.setText(this.mobilityDefString);
        props.add(this.mobilityParam);

        // the available reliability models
        // ----------------------------
        UnborderedJTextField reliSelLabel = new UnborderedJTextField("Reliability Model:", Font.BOLD);
        props.add(reliSelLabel);
        this.fillChoice(this.reliabilityModelComboBox, MODELS_RELIABILITY, this.reliabilitySel);
        props.add(this.reliabilityModelComboBox);
        this.reliabilityParam.setText(this.reliabilityDefString);
        props.add(this.reliabilityParam);

        // add a button to change whether all implementations are contained in the drop
        // down fields
        JPanel allModelsPanel = new JPanel();
        allModelsPanel.setLayout(new BorderLayout());
        this.allModelsCheckBox = new JCheckBox("Show all implementations");
        this.allModelsCheckBox.setSelected(Configuration.isShowModelsOfAllProjects());
        this.allModelsCheckBox.addChangeListener(e -> {
            if (Configuration.isShowModelsOfAllProjects() != this.allModelsCheckBox.isSelected()) {
                Configuration.setShowModelsOfAllProjects(this.allModelsCheckBox.isSelected());
                // reload the contents of the drop down fields
                this.fillChoice(this.distributionModelComboBox, MODELS_DISTRIBUTION, this.distributionSel);
                this.fillChoice(this.nodeTypeComboBox, NODES_IMPLEMENTATIONS, this.nodeTypeSel);
                this.fillChoice(this.connectivityModelComboBox, MODELS_CONNECTIVITY, this.connectivitySel);
                this.fillChoice(this.interferenceModelComboBox, MODELS_INTERFERENCE, this.interferenceSel);
                this.fillChoice(this.mobilityModelComboBox, MODELS_MOBILITY, this.mobilitySel);
                this.fillChoice(this.reliabilityModelComboBox, MODELS_RELIABILITY, this.reliabilitySel);
                GenerateNodesDialog.this.pack();
            }
        });
        allModelsPanel.add(this.allModelsCheckBox);
        cp.add(allModelsPanel);

        // the buttons OK / CANCEL
        JPanel buttons = new JPanel();
        this.ok.setMnemonic(KeyEvent.VK_O);
        buttons.add(this.ok);
        this.cancel.setMnemonic(KeyEvent.VK_C);
        buttons.add(this.cancel);

        cp.add(buttons);

        this.setContentPane(cp);

        // this.setResizable(false);
        this.getRootPane().setDefaultButton(this.ok);
        this.pack();
        this.setLocationRelativeTo(this.parent);
        number.grabFocus();
        this.setVisible(true);
    }

    private void fillChoice(JComboBox c, ImplementationType implDir, String selection) {
        c.removeAllItems();
        Vector<String> names = Global.getImplementations(implDir);
        if (!names.contains(selection)) {
            names.add(selection);
        }
        for (String s : names) {
            c.addItem(s);
        }
        c.setSelectedItem(selection);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals(this.ok.getActionCommand())) {
            if (this.singleNodePosition == null) {
                try {
                    int num = Integer.parseInt(number.getText());
                    if (num <= 0) {
                        throw new NumberFormatException();
                    }
                    AppConfig.getAppConfig().setGenerateNodesDlgNumNodes(num);
                    this.pf = new PercentualProgressDialog(this, this, "Creating new Nodes...");
                    this.canceled = false;
                    this.pf.init();
                } catch (NumberFormatException nfE) {
                    Main.minorError(
                            "Please specify a correct number of nodes to generate. This number has to be an integer greater than zero.");
                }
            } else { // only for a single node
                this.readSelection();
                Node n = this.generateNode(this.singleNodePosition);
                n.finishInitializationWithDefaultModels(true);
                // redraw
                Tools.getGraphPanel().forceDrawInNextPaint();
                Tools.getGraphPanel().repaint();
                this.setVisible(false);
            }
        } else if (event.getActionCommand().equals(this.cancel.getActionCommand())) {
            this.setVisible(false);
        }
    }

    @Override
    public void cancelClicked() {
        this.canceled = true;
    }

    /**
     * Reads and stores the selction the user made
     */
    private void readSelection() {
        this.numberOfNodes = Integer.parseInt(number.getText());
        this.distributionSel = this.distributionModelComboBox.getSelectedItem().toString();
        this.nodeTypeSel = this.nodeTypeComboBox.getSelectedItem().toString();
        this.interferenceSel = this.interferenceModelComboBox.getSelectedItem().toString();
        this.mobilitySel = this.mobilityModelComboBox.getSelectedItem().toString();
        this.reliabilitySel = this.reliabilityModelComboBox.getSelectedItem().toString();
        this.connectivitySel = this.connectivityModelComboBox.getSelectedItem().toString();

        this.distributionParamDefString = this.distributionParam.getText();
        this.interferenceDefString = this.interferenceParam.getText();
        this.mobilityDefString = this.mobilityParam.getText();
        this.reliabilityDefString = this.reliabilityParam.getText();
        this.connectivityDefString = this.connectivityParam.getText();
    }

    private Node generateNode(Position pos) {
        Node node = Node.createNodeByClassname(this.nodeTypeSel);
        node.setPosition(pos);

        InterferenceModel im = Model.getInterferenceModelInstance(this.interferenceSel);
        im.setParamString(this.interferenceDefString);
        node.setInterferenceModel(im);

        MobilityModel mm = Model.getMobilityModelInstance(this.mobilitySel);
        mm.setParamString(this.mobilityDefString);
        node.setMobilityModel(mm);

        ReliabilityModel rm = Model.getReliabilityModelInstance(this.reliabilitySel);
        rm.setParamString(this.reliabilityDefString);
        node.setReliabilityModel(rm);

        ConnectivityModel cm = Model.getConnectivityModelInstance(this.connectivitySel);
        cm.setParamString(this.connectivityDefString);
        node.setConnectivityModel(cm);
        return node;
    }

    /**
     * Creates a node with the default settings.
     *
     * @param pos The position of the new node
     * @return the node.
     */
    public Node generateDefaultNode(Position pos) {
        Node n = this.generateNode(pos);
        n.finishInitializationWithDefaultModels(true);
        return n;
    }

    @Override
    public void performMethod() {
        this.readSelection();
        try {
            DistributionModel distribution = Model.getDistributionModelInstance(this.distributionSel);
            distribution.setParamString(this.distributionParamDefString);
            distribution.setNumberOfNodes(this.numberOfNodes);
            distribution.initialize();

            Vector<Node> addedNodes = new Vector<>();

            for (int i = 0; i < this.numberOfNodes; i++) {
                this.pf.setPercentage(100.0d * ((double) i / (double) this.numberOfNodes));

                Node node = this.generateNode(distribution.getNextPosition());

                if (this.canceled) {
                    for (Node n : addedNodes) {
                        SinalgoRuntime.nodes.removeNode(n);
                        i--;
                        this.pf.setPercentage(100.0d * ((double) i / (double) this.numberOfNodes));
                    }
                    this.pf.finish();
                    addedNodes.clear();
                    return;
                }

                node.finishInitializationWithDefaultModels(true);
                addedNodes.add(node);
            }
        } catch (WrongConfigurationException e) {
            Main.minorError("There was an error while generating the nodes.\n" + e.getMessage());
        }
        this.pf.finish();
        Tools.getGraphPanel().forceDrawInNextPaint();
        Tools.getGraphPanel().repaint();
        this.setVisible(false);
    }

}
