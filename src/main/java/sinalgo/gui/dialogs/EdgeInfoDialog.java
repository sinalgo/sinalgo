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

import sinalgo.gui.GuiHelper;
import sinalgo.gui.helper.NonRegularGridLayout;
import sinalgo.gui.helper.UnborderedJTextField;
import sinalgo.nodes.edges.Edge;
import sinalgo.runtime.Global;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * The Dialog to be shown, when someone is requesting information about an Edge.
 */
public class EdgeInfoDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = -4741386655977255004L;

    /**
     * The Constructor for the EdgeInfoDialog class.
     *
     * @param p The Parent Frame to add the Dialog to.
     * @param e The Edge to get the Information about.
     */
    public EdgeInfoDialog(JFrame p, Edge e) {
        super(p, "Edge from " + e.getStartNode().getID() + " to " + e.getEndNode().getID(), true);
        GuiHelper.setWindowIcon(this);

        this.setLayout(new BorderLayout());

        JPanel info = new JPanel();
        info.setBorder(BorderFactory.createTitledBorder("Edge Info"));
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        this.add(BorderLayout.CENTER, info);

        JPanel infoGrid = new JPanel();
        NonRegularGridLayout nrgl = new NonRegularGridLayout(4, 2, 5, 5);
        nrgl.setAlignToLeft(true);
        infoGrid.setLayout(nrgl);
        info.add(infoGrid);

        UnborderedJTextField startLabel = new UnborderedJTextField("Start Node:", Font.BOLD);
        infoGrid.add(startLabel);
        UnborderedJTextField startNode = new UnborderedJTextField(Long.toString(e.getStartNode().getID()), Font.PLAIN);
        startNode.setEditable(false);
        infoGrid.add(startNode);

        UnborderedJTextField endLabel = new UnborderedJTextField("End Node:", Font.BOLD);
        infoGrid.add(endLabel);
        UnborderedJTextField endNode = new UnborderedJTextField(Long.toString(e.getEndNode().getID()), Font.PLAIN);
        endNode.setEditable(false);
        infoGrid.add(endNode);

        UnborderedJTextField typeLabel = new UnborderedJTextField("Edge Type:", Font.BOLD);
        infoGrid.add(typeLabel);
        UnborderedJTextField typeField = new UnborderedJTextField(Global.toShortName(e.getClass().getName()),
                Font.PLAIN);
        typeField.setEditable(false);
        infoGrid.add(typeField);

        UnborderedJTextField toStringLabel = new UnborderedJTextField("Edge Info:", Font.BOLD);
        infoGrid.add(toStringLabel);
        JTextArea toStringArea = new JTextArea(e.toString());
        toStringArea.setEditable(false);
        toStringArea.setBackground(toStringLabel.getBackground());
        infoGrid.add(toStringArea);

        JPanel buttons = new JPanel();
        JButton ok = new JButton("OK");
        ok.addActionListener(this);
        buttons.add(ok);
        this.add(BorderLayout.SOUTH, buttons);

        // Detect ESCAPE button
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventPostProcessor(e1 -> {
            if (!e1.isConsumed() && e1.getID() == KeyEvent.KEY_PRESSED && e1.getKeyCode() == KeyEvent.VK_ESCAPE) {
                EdgeInfoDialog.this.setVisible(false);
            }
            return false;
        });

        this.getRootPane().setDefaultButton(ok);
        this.setResizable(true);
        this.pack();
        this.setLocationRelativeTo(p);
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        this.setVisible(false);
    }

}
