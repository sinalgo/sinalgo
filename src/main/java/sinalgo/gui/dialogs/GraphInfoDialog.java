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
import sinalgo.gui.GuiHelper;
import sinalgo.gui.helper.NonRegularGridLayout;
import sinalgo.gui.helper.UnborderedJTextField;
import sinalgo.nodes.Node;
import sinalgo.runtime.SinalgoRuntime;
import sinalgo.tools.Tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Enumeration;

/**
 * The Dialog to show the informations about the actual Graph. It is showing the
 * number of nodes, the number of edges and the type of the created edges.
 */
public class GraphInfoDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = -6814163227290018871L;

    /**
     * The constructor for the GraphInfoDialog class.
     *
     * @param parent The parentGUI frame to attach the Dialog to.
     */
    public GraphInfoDialog(JFrame parent) {
        super(parent, "Info about the current network", true);
        GuiHelper.setWindowIcon(this);

        // determine the number of nodes and edges
        Enumeration<Node> nodeEnumer = SinalgoRuntime.getNodes().getNodeEnumeration();
        int numNodes = 0;
        int numEdges = 0;

        while (nodeEnumer.hasMoreElements()) {
            numNodes++;
            Node node = nodeEnumer.nextElement();
            numEdges += node.getOutgoingConnections().size();
        }

        JPanel cp = new JPanel();
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        cp.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        JPanel info = new JPanel();
        NonRegularGridLayout nrgl = new NonRegularGridLayout(5, 2, 10, 5);
        nrgl.setAlignToLeft(true);
        info.setLayout(nrgl);

        UnborderedJTextField numSelLabel = new UnborderedJTextField("  Number of Nodes in this Graph:", Font.BOLD);
        JTextField numberOfNodes = new JTextField(6);
        numberOfNodes.setEditable(false);
        numberOfNodes.setText(String.valueOf(numNodes));
        numberOfNodes.setBorder(null);
        info.add(numSelLabel);
        info.add(numberOfNodes);

        UnborderedJTextField edgeSelLabel = new UnborderedJTextField(
                "  Number of (unidirectional) Edges in this Graph:", Font.BOLD);
        JTextField numberOfEdges = new JTextField(6);
        numberOfEdges.setEditable(false);
        numberOfEdges.setText(String.valueOf(numEdges));
        numberOfEdges.setBorder(null);
        info.add(edgeSelLabel);
        info.add(numberOfEdges);

        if (Tools.isSimulationInAsynchroneMode()) {
            UnborderedJTextField eventLabel = new UnborderedJTextField("  Number of outstanding events:", Font.BOLD);
            JTextField numEvents = new JTextField(6);
            numEvents.setEditable(false);
            numEvents.setText(Integer.toString(Tools.getEventQueue().size()));
            numEvents.setBorder(null);
            info.add(eventLabel);
            info.add(numEvents);
        } else {
            UnborderedJTextField label = new UnborderedJTextField("  Number of messages sent in this round:",
                    Font.BOLD);
            JTextField field = new JTextField(6);
            field.setEditable(false);
            field.setBorder(null);
            field.setText(Integer.toString(Tools.getNumberOfMessagesSentInThisRound()));
            info.add(label);
            info.add(field);
        }

        if (Configuration.isInterference()) { // this is only know if interference is turned on
            UnborderedJTextField label = new UnborderedJTextField("  Number of messages currently being sent:",
                    Font.BOLD);
            JTextField field = new JTextField(6);
            field.setEditable(false);
            field.setBorder(null);
            field.setText(Integer.toString(Tools.getPacketsInTheAir().size()));
            info.add(label);
            info.add(field);
        }

        UnborderedJTextField label = new UnborderedJTextField("  Number of messages sent so far:", Font.BOLD);
        JTextField field = new JTextField(6);
        field.setEditable(false);
        field.setBorder(null);
        field.setText(Integer.toString(Tools.getNumberOfSentMessages()));
        info.add(label);
        info.add(field);

        cp.add(info);

        JPanel buttons = new JPanel();

        JButton close = new JButton("Close");
        close.addActionListener(this);
        buttons.add(close);

        cp.add(buttons);

        this.setContentPane(cp);

        // Detect ESCAPE button
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventPostProcessor(e -> {
            if (!e.isConsumed() && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                GraphInfoDialog.this.setVisible(false);
            }
            return false;
        });

        // this.setResizable(false);
        this.getRootPane().setDefaultButton(close);
        this.pack();
        this.setLocationRelativeTo(parent);
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // if(e.getActionCommand().equals(close.getActionCommand())){
        // }
        this.setVisible(false);
    }

}
