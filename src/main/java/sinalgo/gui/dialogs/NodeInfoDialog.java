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
import sinalgo.gui.GUI;
import sinalgo.gui.GuiHelper;
import sinalgo.gui.helper.NonRegularGridLayout;
import sinalgo.gui.helper.UnborderedJTextField;
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;
import sinalgo.runtime.Global;
import sinalgo.runtime.SinalgoRuntime;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;

/**
 * The class for the dialog displaying information about a node.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class NodeInfoDialog extends JDialog implements ActionListener, PropertyChangeListener {

    private static final long serialVersionUID = 5403782066445725367L;

    private GUI parentGUI;
    private Node node;

    private JFormattedTextField nodeNumber = new JFormattedTextField();

    private JTextField positionX = new JTextField();
    private JTextField positionY = new JTextField();
    private JTextField positionZ = new JTextField();

    private JTextArea infoText = new JTextArea();

    private UnborderedJTextField implementationText = new UnborderedJTextField();
    private UnborderedJTextField connectivityText = new UnborderedJTextField();
    private UnborderedJTextField interferenceText = new UnborderedJTextField();
    private UnborderedJTextField mobilityText = new UnborderedJTextField();
    private UnborderedJTextField reliabilityText = new UnborderedJTextField();

    private JButton prevNode = new JButton("Previous Node");
    private JButton nextNode = new JButton("Next Node");

    /**
     * The constructor for the NodeInfoDialog class.
     *
     * @param p The parentGUI gui where this dialog is attached to.
     * @param n The node the information is about.
     */
    public NodeInfoDialog(GUI p, Node n) {
        super(p, "Edit Node " + n.getID(), true);
        GuiHelper.setWindowIcon(this);
        this.setParentGUI(p);
        this.setNode(n);

        this.getNode().highlight(true);

        this.setLayout(new BorderLayout());

        JPanel nodeSelAndPosition = new JPanel();
        nodeSelAndPosition.setLayout(new BorderLayout());

        JPanel nodeSel = new JPanel();

        // evaluate whether a node is the first or the last in the enumeration and thus
        // has no
        // previous respective next element.
        boolean hasPrev = false;
        Enumeration<Node> nodesEnumer = SinalgoRuntime.getNodes().getNodeEnumeration();
        while (nodesEnumer.hasMoreElements()) {
            Node nd = nodesEnumer.nextElement();
            if (nd.getID() == n.getID()) {
                if (!nodesEnumer.hasMoreElements()) {
                    this.getNextNode().setEnabled(false);
                }
                this.getPrevNode().setEnabled(hasPrev);
                break;
            }
            hasPrev = true;
        }

        this.getPrevNode().addActionListener(this);
        this.getNodeNumber().setColumns(6);
        this.getNodeNumber().setValue(this.getNode().getID());
        this.getNodeNumber().addPropertyChangeListener("value", this);
        this.getNextNode().addActionListener(this);
        nodeSel.add(this.getPrevNode());
        nodeSel.add(this.getNodeNumber());
        nodeSel.add(this.getNextNode());
        this.add(nodeSel);

        Position pos = this.getNode().getPosition();

        this.getPositionX().setText(String.valueOf(pos.getXCoord()));
        this.getPositionY().setText(String.valueOf(pos.getYCoord()));
        this.getPositionZ().setText(String.valueOf(pos.getZCoord()));

        JPanel position = new JPanel();
        position.setBorder(BorderFactory.createTitledBorder("Position"));
        position.setLayout(new BoxLayout(position, BoxLayout.Y_AXIS));
        position.setPreferredSize(new Dimension(80, 80));

        position.add(this.getPositionX());
        position.add(this.getPositionY());
        if (Configuration.getDimensions() == 3) {
            position.add(this.getPositionZ());
        }

        nodeSelAndPosition.add(BorderLayout.NORTH, nodeSel);
        nodeSelAndPosition.add(BorderLayout.SOUTH, position);

        this.add(BorderLayout.NORTH, nodeSelAndPosition);

        JPanel info = new JPanel();
        info.setBorder(BorderFactory.createTitledBorder("Node Info"));
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        this.add(BorderLayout.CENTER, info);

        JPanel infoPanel = new JPanel();
        NonRegularGridLayout nrgl = new NonRegularGridLayout(6, 2, 5, 5);
        nrgl.setAlignToLeft(true);
        infoPanel.setLayout(nrgl);
        info.add(infoPanel);

        UnborderedJTextField typeLabel = new UnborderedJTextField("Node Implementation:", Font.BOLD);
        this.getImplementationText().setText(Global.toShortName(this.getNode().getClass().getName()));
        this.getImplementationText().setEditable(false);
        infoPanel.add(typeLabel);
        infoPanel.add(this.getImplementationText());

        UnborderedJTextField connectivityLabel = new UnborderedJTextField("Node Connectivity:", Font.BOLD);
        this.getConnectivityText().setText(Global.toShortName(this.getNode().getConnectivityModel().getClass().getName()));
        this.getConnectivityText().setEditable(false);
        infoPanel.add(connectivityLabel);
        infoPanel.add(this.getConnectivityText());

        UnborderedJTextField interferenceLabel = new UnborderedJTextField("Node Interference:", Font.BOLD);
        this.getInterferenceText().setText(Global.toShortName(this.getNode().getInterferenceModel().getClass().getName()));
        this.getInterferenceText().setEditable(false);
        infoPanel.add(interferenceLabel);
        infoPanel.add(this.getInterferenceText());

        UnborderedJTextField mobilityLabel = new UnborderedJTextField("Node Mobility:", Font.BOLD);
        this.getMobilityText().setText(Global.toShortName(this.getNode().getMobilityModel().getClass().getName()));
        this.getMobilityText().setEditable(false);
        infoPanel.add(mobilityLabel);
        infoPanel.add(this.getMobilityText());

        UnborderedJTextField reliabilityLabel = new UnborderedJTextField("Node Reliability:", Font.BOLD);
        this.getReliabilityText().setText(Global.toShortName(this.getNode().getReliabilityModel().getClass().getName()));
        this.getReliabilityText().setEditable(false);
        infoPanel.add(reliabilityLabel);
        infoPanel.add(this.getReliabilityText());

        this.getInfoText().setText(this.getNode().toString());
        JLabel infoTextLabel = new JLabel("Info Text:");
        this.getInfoText().setEditable(false);
        this.getInfoText().setBackground(infoTextLabel.getBackground());
        infoPanel.add(infoTextLabel);
        infoPanel.add(this.getInfoText());

        JPanel buttons = new JPanel();

        JButton ok = new JButton("OK");
        ok.addActionListener(this);
        buttons.add(ok);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(this);
        buttons.add(cancel);

        this.add(BorderLayout.SOUTH, buttons);

        WindowListener listener = new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent event) {
                NodeInfoDialog.this.getNode().highlight(false);
                NodeInfoDialog.this.getParentGUI().redrawGUI();
            }
        };
        this.addWindowListener(listener);

        this.setResizable(true);
        this.getRootPane().setDefaultButton(ok);

        // Detect ESCAPE button
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventPostProcessor(e -> {
            if (!e.isConsumed() && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                this.getNode().highlight(false);
                NodeInfoDialog.this.setVisible(false);
                this.getParentGUI().redrawGUINow(); // needs blocking redrawing
            }
            return false;
        });

        // Redraw the graph to have the selected node painted in the right color.
        this.getParentGUI().redrawGUI();
        this.pack();
        this.setLocationRelativeTo(p);
        this.setVisible(true);
    }

    private void revalidate(Node n, boolean hasPrev, boolean hasNext) {
        this.setTitle("Edit Node " + n.getID());

        this.getNode().highlight(false);

        this.setNode(n);

        this.getNode().highlight(true);

        this.getPrevNode().setEnabled(hasPrev);
        this.getNextNode().setEnabled(hasNext);

        Position pos = this.getNode().getPosition();

        this.getPositionX().setText(String.valueOf(pos.getXCoord()));
        this.getPositionY().setText(String.valueOf(pos.getYCoord()));
        this.getPositionZ().setText(String.valueOf(pos.getZCoord()));

        this.getImplementationText().setText(Global.toShortName(this.getNode().getClass().getName()));

        this.getConnectivityText().setText(Global.toShortName(this.getNode().getConnectivityModel().getClass().getName()));
        this.getInterferenceText().setText(Global.toShortName(this.getNode().getInterferenceModel().getClass().getName()));
        this.getMobilityText().setText(Global.toShortName(this.getNode().getMobilityModel().getClass().getName()));
        this.getReliabilityText().setText(Global.toShortName(this.getNode().getReliabilityModel().getClass().getName()));

        this.getInfoText().setText(this.getNode().toString());
        this.getParentGUI().redrawGUI();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        switch (event.getActionCommand()) {
            case "Cancel":
                this.getNode().highlight(false);
                this.setVisible(false);
                this.getParentGUI().redrawGUINow(); // needs blocking redrawing
                break;
            case "OK":
                try {
                    this.getNode().setPosition(new Position(Double.parseDouble(this.getPositionX().getText()),
                            Double.parseDouble(this.getPositionY().getText()), Double.parseDouble(this.getPositionZ().getText())));
                } catch (NumberFormatException nFE) {
                    throw new SinalgoWrappedException(nFE);
                }
                this.getNode().highlight(false);
                this.setVisible(false);
                this.getParentGUI().redrawGUINow(); // needs blocking redrawing
                break;
            case "Next Node": {
                Enumeration<Node> nodesEnumer = SinalgoRuntime.getNodes().getNodeEnumeration();
                while (nodesEnumer.hasMoreElements()) {
                    Node nd = nodesEnumer.nextElement();
                    if (nd.getID() == this.getNode().getID() + 1) {
                        this.getNodeNumber().setValue(nd.getID());
                        // this triggers a property change event.
                        break;
                    }
                }
                break;
            }
            case "Previous Node": {
                Enumeration<Node> nodesEnumer = SinalgoRuntime.getNodes().getNodeEnumeration();
                while (nodesEnumer.hasMoreElements()) {
                    Node nd = nodesEnumer.nextElement();
                    if (nd.getID() == this.getNode().getID() - 1) {
                        this.getNodeNumber().setValue(nd.getID());
                        // this triggers a property change event.
                        break;
                    }
                }
                break;
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        int newId = (Integer) evt.getNewValue();

        boolean hasPrev = false;
        Enumeration<Node> nodesEnumer = SinalgoRuntime.getNodes().getNodeEnumeration();
        while (nodesEnumer.hasMoreElements()) {
            Node nd = nodesEnumer.nextElement();
            if (nd.getID() == newId) {
                boolean hasNext = true;
                if (!nodesEnumer.hasMoreElements()) {
                    hasNext = false;
                }
                this.revalidate(nd, hasPrev, hasNext);
                break;
            }
            hasPrev = true;
        }
    }
}
