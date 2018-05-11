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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
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
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class GenerateNodesDialog extends JDialog implements ActionListener, ProgressBarUser {

    private static final long serialVersionUID = -8080111497886244380L;

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private static JTextField number = new JTextField(6);

    // static: keep the value for subsequent calls
    static {
        getNumber().setText(Integer.toString(AppConfig.getAppConfig().getGenerateNodesDlgNumNodes()));
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

    private PercentualProgressDialog pf;
    private boolean canceled;

    private GUI parentGUI;

    private Position singleNodePosition; // null if the dialgo was created for several nodes

    /**
     * The constructor for the GenerateNodesDialog class.
     *
     * @param p The parentGUI Frame to add the Dialog to.
     */
    public GenerateNodesDialog(GUI p) {
        super(p, "Create new Nodes", true);
        GuiHelper.setWindowIcon(this);
        this.getCancel().addActionListener(this);
        this.getOk().addActionListener(this);

        Font f = this.getDistributionModelComboBox().getFont().deriveFont(Font.PLAIN);
        this.getDistributionModelComboBox().setFont(f);
        this.getNodeTypeComboBox().setFont(f);
        this.getConnectivityModelComboBox().setFont(f);
        this.getInterferenceModelComboBox().setFont(f);
        this.getMobilityModelComboBox().setFont(f);
        this.getReliabilityModelComboBox().setFont(f);

        // Detect ESCAPE button
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventPostProcessor(e -> {
            if (!e.isConsumed() && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                GenerateNodesDialog.this.setVisible(false);
            }
            return false;
        });

        this.setLocationRelativeTo(p);
        this.setParentGUI(p);
    }

    /**
     * The method to generate an GenerateNodesDialog.
     *
     * @param singleNodePos If the dialog is for a single node, this parameter contains the
     *                      position where the node should be placed. For displaying the
     *                      dialog for several nodes, set this parameter to null.
     */
    public void compose(Position singleNodePos) {
        this.setSingleNodePosition(singleNodePos);
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
        distSel.add(getNumber());
        JTextField dummy = new JTextField();
        dummy.setVisible(false);
        distSel.add(dummy);

        // the available distrubutions
        // ----------------------------
        UnborderedJTextField distSelLabel = new UnborderedJTextField("Distribution Model:", Font.BOLD);
        distSel.add(distSelLabel);
        this.fillChoice(this.getDistributionModelComboBox(), MODELS_DISTRIBUTION, this.getDistributionSel());
        distSel.add(this.getDistributionModelComboBox());
        this.getDistributionParam().setText(this.getDistributionParamDefString());
        distSel.add(this.getDistributionParam());
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
        this.fillChoice(this.getNodeTypeComboBox(), NODES_IMPLEMENTATIONS, this.getNodeTypeSel());
        props.add(this.getNodeTypeComboBox());
        this.getNodeTypeParam().setEditable(false);
        this.getNodeTypeParam().setVisible(false);
        props.add(this.getNodeTypeParam());

        // the available connectivities
        // ----------------------------
        UnborderedJTextField connSelLabel = new UnborderedJTextField("Connectivity Model:", Font.BOLD);
        props.add(connSelLabel);
        this.fillChoice(this.getConnectivityModelComboBox(), MODELS_CONNECTIVITY, this.getConnectivitySel());
        props.add(this.getConnectivityModelComboBox());
        this.getConnectivityParam().setText(this.getConnectivityDefString());
        props.add(this.getConnectivityParam());

        // the available interferences
        // ----------------------------
        UnborderedJTextField interSelLabel = new UnborderedJTextField("Interference Model:", Font.BOLD);
        props.add(interSelLabel);
        this.fillChoice(this.getInterferenceModelComboBox(), MODELS_INTERFERENCE, this.getInterferenceSel());
        props.add(this.getInterferenceModelComboBox());
        this.getInterferenceParam().setText(this.getInterferenceDefString());
        props.add(this.getInterferenceParam());

        // the available mobility
        // ----------------------------
        UnborderedJTextField mobSelLabel = new UnborderedJTextField("Mobility Model:", Font.BOLD);
        props.add(mobSelLabel);
        this.fillChoice(this.getMobilityModelComboBox(), MODELS_MOBILITY, this.getMobilitySel());
        props.add(this.getMobilityModelComboBox());
        this.getMobilityParam().setText(this.getMobilityDefString());
        props.add(this.getMobilityParam());

        // the available reliability models
        // ----------------------------
        UnborderedJTextField reliSelLabel = new UnborderedJTextField("Reliability Model:", Font.BOLD);
        props.add(reliSelLabel);
        this.fillChoice(this.getReliabilityModelComboBox(), MODELS_RELIABILITY, this.getReliabilitySel());
        props.add(this.getReliabilityModelComboBox());
        this.getReliabilityParam().setText(this.getReliabilityDefString());
        props.add(this.getReliabilityParam());

        // add a button to change whether all implementations are contained in the drop
        // down fields
        JPanel allModelsPanel = new JPanel();
        allModelsPanel.setLayout(new BorderLayout());
        this.setAllModelsCheckBox(new JCheckBox("Show all implementations"));
        this.getAllModelsCheckBox().setSelected(Configuration.isShowModelsOfAllProjects());
        this.getAllModelsCheckBox().addChangeListener(e -> {
            if (Configuration.isShowModelsOfAllProjects() != this.getAllModelsCheckBox().isSelected()) {
                Configuration.setShowModelsOfAllProjects(this.getAllModelsCheckBox().isSelected());
                // reload the contents of the drop down fields
                this.fillChoice(this.getDistributionModelComboBox(), MODELS_DISTRIBUTION, this.getDistributionSel());
                this.fillChoice(this.getNodeTypeComboBox(), NODES_IMPLEMENTATIONS, this.getNodeTypeSel());
                this.fillChoice(this.getConnectivityModelComboBox(), MODELS_CONNECTIVITY, this.getConnectivitySel());
                this.fillChoice(this.getInterferenceModelComboBox(), MODELS_INTERFERENCE, this.getInterferenceSel());
                this.fillChoice(this.getMobilityModelComboBox(), MODELS_MOBILITY, this.getMobilitySel());
                this.fillChoice(this.getReliabilityModelComboBox(), MODELS_RELIABILITY, this.getReliabilitySel());
                GenerateNodesDialog.this.pack();
            }
        });
        allModelsPanel.add(this.getAllModelsCheckBox());
        cp.add(allModelsPanel);

        // the buttons OK / CANCEL
        JPanel buttons = new JPanel();
        this.getOk().setMnemonic(KeyEvent.VK_O);
        buttons.add(this.getOk());
        this.getCancel().setMnemonic(KeyEvent.VK_C);
        buttons.add(this.getCancel());

        cp.add(buttons);

        this.setContentPane(cp);

        // this.setResizable(false);
        this.getRootPane().setDefaultButton(this.getOk());
        this.pack();
        this.setLocationRelativeTo(this.getParentGUI());
        getNumber().grabFocus();
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
        if (event.getActionCommand().equals(this.getOk().getActionCommand())) {
            if (this.getSingleNodePosition() == null) {
                try {
                    int num = Integer.parseInt(getNumber().getText());
                    if (num <= 0) {
                        throw new NumberFormatException();
                    }
                    AppConfig.getAppConfig().setGenerateNodesDlgNumNodes(num);
                    this.setPf(new PercentualProgressDialog(this, this, "Creating new Nodes..."));
                    this.setCanceled(false);
                    this.getPf().init();
                } catch (NumberFormatException nfE) {
                    Main.minorError(
                            "Please specify a correct number of nodes to generate. This number has to be an integer greater than zero.");
                }
            } else { // only for a single node
                this.readSelection();
                Node n = this.generateNode(this.getSingleNodePosition());
                n.finishInitializationWithDefaultModels(true);
                // redraw
                Tools.getGraphPanel().forceDrawInNextPaint();
                Tools.getGraphPanel().repaint();
                this.setVisible(false);
            }
        } else if (event.getActionCommand().equals(this.getCancel().getActionCommand())) {
            this.setVisible(false);
        }
    }

    @Override
    public void cancelClicked() {
        this.setCanceled(true);
    }

    /**
     * Reads and stores the selction the user made
     */
    private void readSelection() {
        this.setNumberOfNodes(Integer.parseInt(getNumber().getText()));
        this.setDistributionSel(this.getDistributionModelComboBox().getSelectedItem().toString());
        this.setNodeTypeSel(this.getNodeTypeComboBox().getSelectedItem().toString());
        this.setInterferenceSel(this.getInterferenceModelComboBox().getSelectedItem().toString());
        this.setMobilitySel(this.getMobilityModelComboBox().getSelectedItem().toString());
        this.setReliabilitySel(this.getReliabilityModelComboBox().getSelectedItem().toString());
        this.setConnectivitySel(this.getConnectivityModelComboBox().getSelectedItem().toString());

        this.setDistributionParamDefString(this.getDistributionParam().getText());
        this.setInterferenceDefString(this.getInterferenceParam().getText());
        this.setMobilityDefString(this.getMobilityParam().getText());
        this.setReliabilityDefString(this.getReliabilityParam().getText());
        this.setConnectivityDefString(this.getConnectivityParam().getText());
    }

    private Node generateNode(Position pos) {
        Node node = Node.createNodeByClassname(this.getNodeTypeSel());
        node.setPosition(pos);

        InterferenceModel im = Model.getInterferenceModelInstance(this.getInterferenceSel());
        im.setParamString(this.getInterferenceDefString());
        node.setInterferenceModel(im);

        MobilityModel mm = Model.getMobilityModelInstance(this.getMobilitySel());
        mm.setParamString(this.getMobilityDefString());
        node.setMobilityModel(mm);

        ReliabilityModel rm = Model.getReliabilityModelInstance(this.getReliabilitySel());
        rm.setParamString(this.getReliabilityDefString());
        node.setReliabilityModel(rm);

        ConnectivityModel cm = Model.getConnectivityModelInstance(this.getConnectivitySel());
        cm.setParamString(this.getConnectivityDefString());
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
            DistributionModel distribution = Model.getDistributionModelInstance(this.getDistributionSel());
            distribution.setParamString(this.getDistributionParamDefString());
            distribution.setNumberOfNodes(this.getNumberOfNodes());
            distribution.initialize();

            Vector<Node> addedNodes = new Vector<>();

            for (int i = 0; i < this.getNumberOfNodes(); i++) {
                this.getPf().setPercentage(100.0d * ((double) i / (double) this.getNumberOfNodes()));

                Node node = this.generateNode(distribution.getNextPosition());

                if (this.isCanceled()) {
                    for (Node n : addedNodes) {
                        SinalgoRuntime.getNodes().removeNode(n);
                        i--;
                        this.getPf().setPercentage(100.0d * ((double) i / (double) this.getNumberOfNodes()));
                    }
                    this.getPf().finish();
                    addedNodes.clear();
                    return;
                }

                node.finishInitializationWithDefaultModels(true);
                addedNodes.add(node);
            }
        } catch (WrongConfigurationException e) {
            Main.minorError("There was an error while generating the nodes.\n" + e.getMessage());
        }
        this.getPf().finish();
        Tools.getGraphPanel().forceDrawInNextPaint();
        Tools.getGraphPanel().repaint();
        this.setVisible(false);
    }
}
