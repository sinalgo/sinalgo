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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;

/**
 * The class for the dialog displaying information about a node.
 */
public class NodeInfoDialog extends JDialog implements ActionListener, PropertyChangeListener {

    private static final long serialVersionUID = 5403782066445725367L;

    private GUI parent;
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
     * @param p The parent gui where this dialog is attached to.
     * @param n The node the information is about.
     */
    public NodeInfoDialog(GUI p, Node n) {
        super(p, "Edit Node " + n.getID(), true);
        GuiHelper.setWindowIcon(this);
        this.parent = p;
        this.node = n;

        this.node.highlight(true);

        this.setLayout(new BorderLayout());

        JPanel nodeSelAndPosition = new JPanel();
        nodeSelAndPosition.setLayout(new BorderLayout());

        JPanel nodeSel = new JPanel();

        // evaluate whether a node is the first or the last in the enumeration and thus
        // has no
        // previous respective next element.
        boolean hasPrev = false;
        Enumeration<Node> nodesEnumer = SinalgoRuntime.nodes.getNodeEnumeration();
        while (nodesEnumer.hasMoreElements()) {
            Node nd = nodesEnumer.nextElement();
            if (nd.getID() == n.getID()) {
                if (!nodesEnumer.hasMoreElements()) {
                    this.nextNode.setEnabled(false);
                }
                this.prevNode.setEnabled(hasPrev);
                break;
            }
            hasPrev = true;
        }

        this.prevNode.addActionListener(this);
        this.nodeNumber.setColumns(6);
        this.nodeNumber.setValue(this.node.getID());
        this.nodeNumber.addPropertyChangeListener("value", this);
        this.nextNode.addActionListener(this);
        nodeSel.add(this.prevNode);
        nodeSel.add(this.nodeNumber);
        nodeSel.add(this.nextNode);
        this.add(nodeSel);

        Position pos = this.node.getPosition();

        this.positionX.setText(String.valueOf(pos.getXCoord()));
        this.positionY.setText(String.valueOf(pos.getYCoord()));
        this.positionZ.setText(String.valueOf(pos.getZCoord()));

        JPanel position = new JPanel();
        position.setBorder(BorderFactory.createTitledBorder("Position"));
        position.setLayout(new BoxLayout(position, BoxLayout.Y_AXIS));
        position.setPreferredSize(new Dimension(80, 80));

        position.add(this.positionX);
        position.add(this.positionY);
        if (Configuration.dimensions == 3) {
            position.add(this.positionZ);
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
        this.implementationText.setText(Global.toShortName(this.node.getClass().getName()));
        this.implementationText.setEditable(false);
        infoPanel.add(typeLabel);
        infoPanel.add(this.implementationText);

        UnborderedJTextField connectivityLabel = new UnborderedJTextField("Node Connectivity:", Font.BOLD);
        this.connectivityText.setText(Global.toShortName(this.node.getConnectivityModel().getClass().getName()));
        this.connectivityText.setEditable(false);
        infoPanel.add(connectivityLabel);
        infoPanel.add(this.connectivityText);

        UnborderedJTextField interferenceLabel = new UnborderedJTextField("Node Interference:", Font.BOLD);
        this.interferenceText.setText(Global.toShortName(this.node.getInterferenceModel().getClass().getName()));
        this.interferenceText.setEditable(false);
        infoPanel.add(interferenceLabel);
        infoPanel.add(this.interferenceText);

        UnborderedJTextField mobilityLabel = new UnborderedJTextField("Node Mobility:", Font.BOLD);
        this.mobilityText.setText(Global.toShortName(this.node.getMobilityModel().getClass().getName()));
        this.mobilityText.setEditable(false);
        infoPanel.add(mobilityLabel);
        infoPanel.add(this.mobilityText);

        UnborderedJTextField reliabilityLabel = new UnborderedJTextField("Node Reliability:", Font.BOLD);
        this.reliabilityText.setText(Global.toShortName(this.node.getReliabilityModel().getClass().getName()));
        this.reliabilityText.setEditable(false);
        infoPanel.add(reliabilityLabel);
        infoPanel.add(this.reliabilityText);

        this.infoText.setText(this.node.toString());
        JLabel infoTextLabel = new JLabel("Info Text:");
        this.infoText.setEditable(false);
        this.infoText.setBackground(infoTextLabel.getBackground());
        infoPanel.add(infoTextLabel);
        infoPanel.add(this.infoText);

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
                NodeInfoDialog.this.node.highlight(false);
                NodeInfoDialog.this.parent.redrawGUI();
            }
        };
        this.addWindowListener(listener);

        this.setResizable(true);
        this.getRootPane().setDefaultButton(ok);

        // Detect ESCAPE button
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventPostProcessor(e -> {
            if (!e.isConsumed() && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                this.node.highlight(false);
                NodeInfoDialog.this.setVisible(false);
                this.parent.redrawGUINow(); // needs blocking redrawing
            }
            return false;
        });

        // Redraw the graph to have the selected node painted in the right color.
        this.parent.redrawGUI();
        this.pack();
        this.setLocationRelativeTo(p);
        this.setVisible(true);
    }

    private void revalidate(Node n, boolean hasPrev, boolean hasNext) {
        this.setTitle("Edit Node " + n.getID());

        this.node.highlight(false);

        this.node = n;

        this.node.highlight(true);

        this.prevNode.setEnabled(hasPrev);
        this.nextNode.setEnabled(hasNext);

        Position pos = this.node.getPosition();

        this.positionX.setText(String.valueOf(pos.getXCoord()));
        this.positionY.setText(String.valueOf(pos.getYCoord()));
        this.positionZ.setText(String.valueOf(pos.getZCoord()));

        this.implementationText.setText(Global.toShortName(this.node.getClass().getName()));

        this.connectivityText.setText(Global.toShortName(this.node.getConnectivityModel().getClass().getName()));
        this.interferenceText.setText(Global.toShortName(this.node.getInterferenceModel().getClass().getName()));
        this.mobilityText.setText(Global.toShortName(this.node.getMobilityModel().getClass().getName()));
        this.reliabilityText.setText(Global.toShortName(this.node.getReliabilityModel().getClass().getName()));

        this.infoText.setText(this.node.toString());
        this.parent.redrawGUI();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        switch (event.getActionCommand()) {
            case "Cancel":
                this.node.highlight(false);
                this.setVisible(false);
                this.parent.redrawGUINow(); // needs blocking redrawing
                break;
            case "OK":
                try {
                    this.node.setPosition(new Position(Double.parseDouble(this.positionX.getText()),
                            Double.parseDouble(this.positionY.getText()), Double.parseDouble(this.positionZ.getText())));
                } catch (NumberFormatException nFE) {
                    throw new SinalgoWrappedException(nFE);
                }
                this.node.highlight(false);
                this.setVisible(false);
                this.parent.redrawGUINow(); // needs blocking redrawing
                break;
            case "Next Node": {
                Enumeration<Node> nodesEnumer = SinalgoRuntime.nodes.getNodeEnumeration();
                while (nodesEnumer.hasMoreElements()) {
                    Node nd = nodesEnumer.nextElement();
                    if (nd.getID() == this.node.getID() + 1) {
                        this.nodeNumber.setValue(nd.getID());
                        // this triggers a property change event.
                        break;
                    }
                }
                break;
            }
            case "Previous Node": {
                Enumeration<Node> nodesEnumer = SinalgoRuntime.nodes.getNodeEnumeration();
                while (nodesEnumer.hasMoreElements()) {
                    Node nd = nodesEnumer.nextElement();
                    if (nd.getID() == this.node.getID() - 1) {
                        this.nodeNumber.setValue(nd.getID());
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
        Enumeration<Node> nodesEnumer = SinalgoRuntime.nodes.getNodeEnumeration();
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
