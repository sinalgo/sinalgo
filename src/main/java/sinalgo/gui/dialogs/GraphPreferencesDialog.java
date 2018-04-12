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

/**
 * The Class for the dialog for the Graph preferences.
 */
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

    private GUI parent;

    /**
     * Generates a dialog that shows information about the current graph.
     *
     * @param parent The Gui instance that created the dialog.
     */
    public GraphPreferencesDialog(GUI parent) {
        super(parent, "Preferences", true);
        GuiHelper.setWindowIcon(this);
        this.parent = parent;

        JPanel cp = new JPanel();
        cp.setLayout(new BorderLayout());
        cp.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        JPanel visualDetails = new JPanel();
        visualDetails.setBorder(BorderFactory.createTitledBorder("Visual Details:"));
        visualDetails.setLayout(new BoxLayout(visualDetails, BoxLayout.Y_AXIS));

        drawArrowsCB.setSelected(Configuration.drawArrows);
        visualDetails.add(drawArrowsCB);

        // Feature not yet implemented
        // drawRulerCB.setSelected(Configuration.drawRulers);
        // visualDetails.add(drawRulerCB);

        drawNodesCB.setSelected(Configuration.drawNodes);
        visualDetails.add(drawNodesCB);

        drawEdgesCB.setSelected(Configuration.drawEdges);
        visualDetails.add(drawEdgesCB);

        usePerspectiveCB.setSelected(Configuration.usePerspectiveView);
        if (Configuration.dimensions == 3) { // only show in 3D
            visualDetails.add(usePerspectiveCB);
        }

        cp.add(visualDetails, BorderLayout.NORTH);

        JPanel simulationDetails = new JPanel();
        simulationDetails.setLayout(new GridLayout(3, 2, 3, 3));
        simulationDetails.setBorder(BorderFactory.createTitledBorder("Simulation Details:"));

        // Edges implemenations

        Font f = typeOfEdges.getFont().deriveFont(Font.PLAIN);
        typeOfEdges.setFont(f);
        fillTypesOfEdges();
        simulationDetails.add(new JLabel("Type of Edges: "));
        simulationDetails.add(typeOfEdges);

        // Transmission model
        selectedTransmissionModel.setFont(f);
        fillTransmissionModel();
        simulationDetails.add(new JLabel("Transmission Model: "));
        simulationDetails.add(selectedTransmissionModel);

        simulationDetails.add(new JLabel(""));
        allModelsCheckBox = new JCheckBox("Show all implementations");
        allModelsCheckBox.setSelected(Configuration.showModelsOfAllProjects);
        allModelsCheckBox.addChangeListener(e -> {
            if (Configuration.showModelsOfAllProjects != allModelsCheckBox.isSelected()) {
                Configuration.showModelsOfAllProjects = allModelsCheckBox.isSelected();
                fillTypesOfEdges();
                fillTransmissionModel();
            }
        });
        simulationDetails.add(allModelsCheckBox);
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

        ok.addActionListener(this);
        buttons.add(ok);

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

        this.getRootPane().setDefaultButton(ok);
        this.pack();
        this.setLocationRelativeTo(parent);
        this.setVisible(true);
    }

    private void fillTypesOfEdges() {
        // default + current project
        Vector<String> names = new Vector<>(Global.getImplementations("nodes/edges"));
        if (!names.contains(Configuration.getEdgeTypeShortName())) {
            names.add(Configuration.getEdgeTypeShortName());
        }
        typeOfEdges.removeAllItems();
        for (String s : names) {
            typeOfEdges.addItem(s);
        }

        typeOfEdges.setSelectedItem(Configuration.getEdgeTypeShortName());
    }

    private void fillTransmissionModel() {
        // default project && current
        // project
        Vector<String> names = new Vector<>(Global.getImplementations("models/messageTransmissionModels"));
        if (!names.contains(Configuration.DefaultMessageTransmissionModel)) {
            names.add(Configuration.DefaultMessageTransmissionModel);
        }
        selectedTransmissionModel.removeAllItems();
        for (String s : names) {
            selectedTransmissionModel.addItem(s);
        }
        selectedTransmissionModel.setSelectedItem(Configuration.DefaultMessageTransmissionModel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(ok.getActionCommand())) {
            try {
                String selectedType = (String) typeOfEdges.getSelectedItem();
                Configuration.setEdgeType(selectedType);

                String selectedTransModel = (String) selectedTransmissionModel.getSelectedItem();
                if (!Configuration.DefaultMessageTransmissionModel.equals(selectedTransModel)) {
                    Configuration.DefaultMessageTransmissionModel = selectedTransModel;
                    Global.messageTransmissionModel = Model.getMessageTransmissionModelInstance(
                            Configuration.DefaultMessageTransmissionModel);
                }

                if (drawRulerCB.isSelected() != Configuration.drawRulers) {
                    Configuration.drawRulers = drawRulerCB.isSelected();
                    parent.getGraphPanel().forceDrawInNextPaint();
                }
                if (drawArrowsCB.isSelected() != Configuration.drawArrows) {
                    Configuration.drawArrows = drawArrowsCB.isSelected();
                    parent.getGraphPanel().forceDrawInNextPaint();
                }
                if (drawEdgesCB.isSelected() != Configuration.drawEdges) {
                    Configuration.drawEdges = drawEdgesCB.isSelected();
                    parent.getGraphPanel().forceDrawInNextPaint();
                }
                if (drawNodesCB.isSelected() != Configuration.drawNodes) {
                    Configuration.drawNodes = drawNodesCB.isSelected();
                    parent.getGraphPanel().forceDrawInNextPaint();
                }
                if (usePerspectiveCB.isSelected() != Configuration.usePerspectiveView) {
                    Configuration.usePerspectiveView = !Configuration.usePerspectiveView;
                    parent.getGraphPanel().forceDrawInNextPaint();
                }

            } catch (WrongConfigurationException ex) {
                throw new SinalgoWrappedException(ex);
            }
        }
        this.setVisible(false);
    }
}
