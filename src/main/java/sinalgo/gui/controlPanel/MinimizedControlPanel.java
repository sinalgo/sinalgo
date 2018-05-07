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
package sinalgo.gui.controlPanel;

import sinalgo.configuration.Configuration;
import sinalgo.gui.GUI;
import sinalgo.gui.transformation.Transformation3D;
import sinalgo.runtime.Global;
import sinalgo.runtime.events.Event;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

/**
 * The minimized version of the control panel.
 */
public class MinimizedControlPanel extends ControlPanel {

    private static final long serialVersionUID = 6500416722163812886L;

    private JPanel buttonPanel;

    /**
     * Creates a MinimizedControlPanel for the specified GUI instance.
     *
     * @param p The Gui instance to create the MinimizedControlPanel for.
     */
    public MinimizedControlPanel(GUI p) {
        this.parent = p;
        int controlPanelHeight = 25;
        this.setMaximumSize(new Dimension(20000, controlPanelHeight));
        this.setMinimumSize(new Dimension(20000, controlPanelHeight));
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createRaisedBevelBorder());

        this.buttonPanel = new JPanel();
        FlowLayout buttonLayout = new FlowLayout(FlowLayout.LEFT, 2, 0);
        this.buttonPanel.setLayout(buttonLayout);

        JButton button = this.createFrameworkIconButton("clearGraph", "cleargraph.gif", "Clear Graph");
        this.buttonPanel.add(button);
        this.addToDisabledButtonList(button);

        button = this.createFrameworkIconButton("addNodes", "addnodes.gif", "Add Nodes");
        this.buttonPanel.add(button);
        this.addToDisabledButtonList(button);

        button = this.createFrameworkIconButton("connectNodes", "connectnodes.gif", "Reevaluate Connections");
        this.buttonPanel.add(button);
        this.addToDisabledButtonList(button);

        this.addSeparator(this.buttonPanel);

        button = this.createFrameworkIconButton("zoomIn", "zoominimage.png", "Zoom In");
        this.buttonPanel.add(button);
        this.addToDisabledButtonList(button);

        button = this.createFrameworkIconButton("zoomOut", "zoomoutimage.png", "Zoom Out");
        this.buttonPanel.add(button);
        this.addToDisabledButtonList(button);

        button = this.createFrameworkIconButton("zoomToFit", "zoomtofit.gif", "Zoom To Fit");
        this.buttonPanel.add(button);
        this.addToDisabledButtonList(button);

        if (this.parent.getTransformator() instanceof Transformation3D) {
            button = this.createFrameworkIconButton("zoomToFit3D", "zoomtofit3d.gif", "Default View");
            this.buttonPanel.add(button);
            this.addToDisabledButtonList(button);
        }

        this.addSeparator(this.buttonPanel);
        this.addSpacer(this.buttonPanel, 5);

        // The two text fields to enter number of rounds and refresh rate
        // roundNumber.setText(String.valueOf(Configuration.defaultRoundNumber));
        if (Configuration.asynchronousMode) {
            roundsToPerform.setToolTipText("Number of Events to perform");
        } else {
            roundsToPerform.setToolTipText("Number of Rounds to perform");
        }
        this.buttonPanel.add(roundsToPerform);

        refreshRate.setText(String.valueOf(Configuration.refreshRate));
        refreshRate.setToolTipText("Refresh Rate");
        this.buttonPanel.add(refreshRate);

        JPanel startButtonPanel = new JPanel();
        startButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        startButtonPanel.add(start);

        // the run-selection button
        this.runMenuButton = this.createFrameworkIconButton("RunMenu", "maximize.gif", "Run Options");
        this.runMenuButton.setPreferredSize(new Dimension(13, 29));
        this.addToDisabledButtonList(this.runMenuButton);
        startButtonPanel.add(this.runMenuButton);
        // raise the 'run' menu whenever the mouse idles over this button
        this.runMenuButton.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (MinimizedControlPanel.this.runMenuButton.isEnabled()) {
                    start.setBorderPainted(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (MinimizedControlPanel.this.runMenuButton.isEnabled()) {
                    start.setBorderPainted(false);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        });
        this.buttonPanel.add(startButtonPanel);

        this.abort = this.createFrameworkIconButton("Abort", "abort.gif", "Abort Simulation");
        this.abort.setEnabled(false);
        this.buttonPanel.add(this.abort);

        this.addSpacer(this.buttonPanel, 5);

        JLabel doneRoundsLabel;
        if (Global.isAsynchronousMode) {
            doneRoundsLabel = new JLabel("Time: ");
            this.roundsPerformed.setText(String.valueOf(this.round(sinalgo.runtime.Global.currentTime, 2)));
        } else {
            doneRoundsLabel = new JLabel("Round: ");
            this.roundsPerformed.setText(String.valueOf((int) this.round(sinalgo.runtime.Global.currentTime, 2)));
        }
        this.buttonPanel.add(doneRoundsLabel);
        this.roundsPerformed.setEditable(false);
        this.roundsPerformed.setBorder(BorderFactory.createEmptyBorder());
        this.roundsPerformed.setToolTipText("Number of rounds performed so far");
        this.buttonPanel.add(this.roundsPerformed);

        // Add the user-defined buttons
        Vector<JButton> customButtons = super.createCustomButtons();
        if (customButtons.size() > 0) {
            this.addSpacer(this.buttonPanel, 5);
            this.addSeparator(this.buttonPanel);
            this.addSpacer(this.buttonPanel, 5);

            for (JButton b : customButtons) {
                this.buttonPanel.add(b);
                this.addToDisabledButtonList(b);
            }
            this.addSpacer(this.buttonPanel, 4); // strange, but the last button is sometimes not painted...
        }

        JButton changeButton = this.createFrameworkIconButton("extendPanel", "maximize.gif", "Extend");
        changeButton.setPreferredSize(new Dimension(13, 29));
        this.addToDisabledButtonList(changeButton);
        this.add(BorderLayout.EAST, changeButton);
        this.add(BorderLayout.WEST, this.buttonPanel);

        this.setVisible(true);
    }

    @Override
    public void setRoundsPerformed(double time, int eventNumber) {
        this.roundsPerformed.setText(String.valueOf(this.round(time, 2)));
        this.buttonPanel.doLayout();
    }

    @Override
    public void setRoundsPerformed(int i) {
        this.roundsPerformed.setText(String.valueOf(i));
        this.buttonPanel.doLayout();
    }

    @Override
    public void setCurrentEvent(Event e) {
        this.setStringsForCurrentEvent(e);
        Vector<String> v = new Vector<>(1);
        v.add(currentEventString);
        this.eventJList.setListData(v);
    }

    private void addSeparator(JPanel buttonPanel) {
        JPanel separator = new JPanel();
        separator.setPreferredSize(new Dimension(1, 23));
        separator.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        buttonPanel.add(separator);
    }

    private void addSpacer(JPanel buttonPanel, int width) {
        JPanel spacer = new JPanel();
        spacer.setPreferredSize(new Dimension(width, 20));
        buttonPanel.add(spacer);
    }

}
