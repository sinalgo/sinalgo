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
import sinalgo.configuration.Configuration;
import sinalgo.exception.SinalgoWrappedException;
import sinalgo.exception.WrongConfigurationException;
import sinalgo.gui.GUI;
import sinalgo.gui.GuiHelper;
import sinalgo.models.Model;
import sinalgo.runtime.Global;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import static sinalgo.configuration.Configuration.ImplementationChoiceInConfigFile.ImplementationType.MODELS_MESSAGE_TRANSMISSION;
import static sinalgo.configuration.Configuration.ImplementationChoiceInConfigFile.ImplementationType.NODES_EDGES;

/**
 * The Class for the dialog for the Graph preferences.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class GraphPreferencesDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 7895237565849731280L;

    private JCheckBox drawArrowsCB = new JCheckBox("Draw the links as arrows");
    private JCheckBox drawRulerCB = new JCheckBox("Draw the ruler");
    private JCheckBox drawEdgesCB = new JCheckBox("Draw edges");
    private JCheckBox drawNodesCB = new JCheckBox("Draw nodes");
    private JCheckBox usePerspectiveCB = new JCheckBox("Draw 3D with perspective");

    private JComboBox typeOfEdges = new JComboBox();
    private JComboBox selectedTransmissionModel = new JComboBox();
    private JCheckBox allModelsCheckBox;

    private JButton ok = new JButton("Ok");

    private GUI parentGUI;

    /**
     * Generates a dialog that shows information about the current graph.
     *
     * @param parentGUI The Gui instance that created the dialog.
     */
    public GraphPreferencesDialog(GUI parentGUI) {
        super(parentGUI, "Preferences", true);
        GuiHelper.setWindowIcon(this);
        this.setParentGUI(parentGUI);

        JPanel cp = new JPanel();
        cp.setLayout(new BorderLayout());
        cp.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        JPanel visualDetails = new JPanel();
        visualDetails.setBorder(BorderFactory.createTitledBorder("Visual Details:"));
        visualDetails.setLayout(new BoxLayout(visualDetails, BoxLayout.Y_AXIS));

        this.getDrawArrowsCB().setSelected(Configuration.isDrawArrows());
        visualDetails.add(this.getDrawArrowsCB());

        // Feature not yet implemented
        // drawRulerCB.setSelected(Configuration.drawRulers);
        // visualDetails.add(drawRulerCB);

        this.getDrawNodesCB().setSelected(Configuration.isDrawNodes());
        visualDetails.add(this.getDrawNodesCB());

        this.getDrawEdgesCB().setSelected(Configuration.isDrawEdges());
        visualDetails.add(this.getDrawEdgesCB());

        this.getUsePerspectiveCB().setSelected(Configuration.isUsePerspectiveView());
        if (Configuration.getDimensions() == 3) { // only show in 3D
            visualDetails.add(this.getUsePerspectiveCB());
        }

        cp.add(visualDetails, BorderLayout.NORTH);

        JPanel simulationDetails = new JPanel();
        simulationDetails.setLayout(new GridLayout(3, 2, 3, 3));
        simulationDetails.setBorder(BorderFactory.createTitledBorder("Simulation Details:"));

        // Edges implemenations

        Font f = this.getTypeOfEdges().getFont().deriveFont(Font.PLAIN);
        this.getTypeOfEdges().setFont(f);
        this.fillTypesOfEdges();
        simulationDetails.add(new JLabel("Type of Edges: "));
        simulationDetails.add(this.getTypeOfEdges());

        // Transmission model
        this.getSelectedTransmissionModel().setFont(f);
        this.fillTransmissionModel();
        simulationDetails.add(new JLabel("Transmission Model: "));
        simulationDetails.add(this.getSelectedTransmissionModel());

        simulationDetails.add(new JLabel(""));
        this.setAllModelsCheckBox(new JCheckBox("Show all implementations"));
        this.getAllModelsCheckBox().setSelected(Configuration.isShowModelsOfAllProjects());
        this.getAllModelsCheckBox().addChangeListener(e -> {
            if (Configuration.isShowModelsOfAllProjects() != this.getAllModelsCheckBox().isSelected()) {
                Configuration.setShowModelsOfAllProjects(this.getAllModelsCheckBox().isSelected());
                this.fillTypesOfEdges();
                this.fillTransmissionModel();
            }
        });
        simulationDetails.add(this.getAllModelsCheckBox());
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder());
        centerPanel.add(simulationDetails, BorderLayout.NORTH);

        JLabel label = new JLabel(
                "<html><table><tr valign='top'><td><b>Note:</b></td><td> These settings affect only this simulation; they are not stored <br>in the config file for further runs.</td></tr></table></html>");
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11));
        centerPanel.add(label, BorderLayout.SOUTH);

        cp.add(centerPanel, BorderLayout.CENTER);

        JPanel buttons = new JPanel();

        this.getOk().addActionListener(this);
        buttons.add(this.getOk());

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(this);
        buttons.add(cancel);

        cp.add(buttons, BorderLayout.SOUTH);

        this.setContentPane(cp);

        // Detect ESCAPE button
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventPostProcessor(e -> {
            if (!e.isConsumed() && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                GraphPreferencesDialog.this.setVisible(false);
            }
            return false;
        });

        this.getRootPane().setDefaultButton(this.getOk());
        this.pack();
        this.setLocationRelativeTo(parentGUI);
        this.setVisible(true);
    }

    private void fillTypesOfEdges() {
        // default + current project
        Vector<String> names = new Vector<>(Global.getImplementations(NODES_EDGES));
        if (!names.contains(Configuration.getEdgeTypeShortName())) {
            names.add(Configuration.getEdgeTypeShortName());
        }
        this.getTypeOfEdges().removeAllItems();
        for (String s : names) {
            this.getTypeOfEdges().addItem(s);
        }

        this.getTypeOfEdges().setSelectedItem(Configuration.getEdgeTypeShortName());
    }

    private void fillTransmissionModel() {
        // default project && current
        // project
        Vector<String> names = new Vector<>(Global.getImplementations(MODELS_MESSAGE_TRANSMISSION));
        if (!names.contains(Configuration.getDefaultMessageTransmissionModel())) {
            names.add(Configuration.getDefaultMessageTransmissionModel());
        }
        this.getSelectedTransmissionModel().removeAllItems();
        for (String s : names) {
            this.getSelectedTransmissionModel().addItem(s);
        }
        this.getSelectedTransmissionModel().setSelectedItem(Configuration.getDefaultMessageTransmissionModel());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(this.getOk().getActionCommand())) {
            try {
                String selectedType = (String) this.getTypeOfEdges().getSelectedItem();
                Configuration.setEdgeType(selectedType);

                String selectedTransModel = (String) this.getSelectedTransmissionModel().getSelectedItem();
                if (!Configuration.getDefaultMessageTransmissionModel().equals(selectedTransModel)) {
                    Configuration.setDefaultMessageTransmissionModel(selectedTransModel);
                    Global.setMessageTransmissionModel(Model.getMessageTransmissionModelInstance(
                            Configuration.getDefaultMessageTransmissionModel()));
                }

                if (this.getDrawRulerCB().isSelected() != Configuration.isDrawRulers()) {
                    Configuration.setDrawRulers(this.getDrawRulerCB().isSelected());
                    this.getParentGUI().getGraphPanel().forceDrawInNextPaint();
                }
                if (this.getDrawArrowsCB().isSelected() != Configuration.isDrawArrows()) {
                    Configuration.setDrawArrows(this.getDrawArrowsCB().isSelected());
                    this.getParentGUI().getGraphPanel().forceDrawInNextPaint();
                }
                if (this.getDrawEdgesCB().isSelected() != Configuration.isDrawEdges()) {
                    Configuration.setDrawEdges(this.getDrawEdgesCB().isSelected());
                    this.getParentGUI().getGraphPanel().forceDrawInNextPaint();
                }
                if (this.getDrawNodesCB().isSelected() != Configuration.isDrawNodes()) {
                    Configuration.setDrawNodes(this.getDrawNodesCB().isSelected());
                    this.getParentGUI().getGraphPanel().forceDrawInNextPaint();
                }
                if (this.getUsePerspectiveCB().isSelected() != Configuration.isUsePerspectiveView()) {
                    Configuration.setUsePerspectiveView(!Configuration.isUsePerspectiveView());
                    this.getParentGUI().getGraphPanel().forceDrawInNextPaint();
                }

            } catch (WrongConfigurationException ex) {
                throw new SinalgoWrappedException(ex);
            }
        }
        this.setVisible(false);
    }
}
