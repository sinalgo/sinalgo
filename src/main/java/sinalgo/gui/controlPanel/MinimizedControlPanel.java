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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
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

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private JPanel buttonPanel;

    /**
     * Creates a MinimizedControlPanel for the specified GUI instance.
     *
     * @param p The Gui instance to create the MinimizedControlPanel for.
     */
    public MinimizedControlPanel(GUI p) {
        this.setParentGUI(p);
        int controlPanelHeight = 25;
        this.setMaximumSize(new Dimension(20000, controlPanelHeight));
        this.setMinimumSize(new Dimension(20000, controlPanelHeight));
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createRaisedBevelBorder());

        this.setButtonPanel(new JPanel());
        FlowLayout buttonLayout = new FlowLayout(FlowLayout.LEFT, 2, 0);
        this.getButtonPanel().setLayout(buttonLayout);

        JButton button = this.createFrameworkIconButton("clearGraph", "cleargraph.gif", "Clear Graph");
        this.getButtonPanel().add(button);
        this.addToDisabledButtonList(button);

        button = this.createFrameworkIconButton("addNodes", "addnodes.gif", "Add Nodes");
        this.getButtonPanel().add(button);
        this.addToDisabledButtonList(button);

        button = this.createFrameworkIconButton("connectNodes", "connectnodes.gif", "Reevaluate Connections");
        this.getButtonPanel().add(button);
        this.addToDisabledButtonList(button);

        this.addSeparator(this.getButtonPanel());

        button = this.createFrameworkIconButton("zoomIn", "zoominimage.png", "Zoom In");
        this.getButtonPanel().add(button);
        this.addToDisabledButtonList(button);

        button = this.createFrameworkIconButton("zoomOut", "zoomoutimage.png", "Zoom Out");
        this.getButtonPanel().add(button);
        this.addToDisabledButtonList(button);

        button = this.createFrameworkIconButton("zoomToFit", "zoomtofit.gif", "Zoom To Fit");
        this.getButtonPanel().add(button);
        this.addToDisabledButtonList(button);

        if (this.getParentGUI().getTransformator() instanceof Transformation3D) {
            button = this.createFrameworkIconButton("zoomToFit3D", "zoomtofit3d.gif", "Default View");
            this.getButtonPanel().add(button);
            this.addToDisabledButtonList(button);
        }

        this.addSeparator(this.getButtonPanel());
        this.addSpacer(this.getButtonPanel(), 5);

        // The two text fields to enter number of rounds and refresh rate
        // roundNumber.setText(String.valueOf(Configuration.defaultRoundNumber));
        if (Configuration.isAsynchronousMode()) {
            getRoundsToPerform().setToolTipText("Number of Events to perform");
        } else {
            getRoundsToPerform().setToolTipText("Number of Rounds to perform");
        }
        this.getButtonPanel().add(getRoundsToPerform());

        getRefreshRate().setText(String.valueOf(Configuration.getRefreshRate()));
        getRefreshRate().setToolTipText("Refresh Rate");
        this.getButtonPanel().add(getRefreshRate());

        JPanel startButtonPanel = new JPanel();
        startButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        startButtonPanel.add(getStart());

        // the run-selection button
        this.setRunMenuButton(this.createFrameworkIconButton("RunMenu", "maximize.gif", "Run Options"));
        this.getRunMenuButton().setPreferredSize(new Dimension(13, 29));
        this.addToDisabledButtonList(this.getRunMenuButton());
        startButtonPanel.add(this.getRunMenuButton());
        // raise the 'run' menu whenever the mouse idles over this button
        this.getRunMenuButton().addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (MinimizedControlPanel.this.getRunMenuButton().isEnabled()) {
                    getStart().setBorderPainted(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (MinimizedControlPanel.this.getRunMenuButton().isEnabled()) {
                    getStart().setBorderPainted(false);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        });
        this.getButtonPanel().add(startButtonPanel);

        this.setAbort(this.createFrameworkIconButton("Abort", "abort.gif", "Abort Simulation"));
        this.getAbort().setEnabled(false);
        this.getButtonPanel().add(this.getAbort());

        this.addSpacer(this.getButtonPanel(), 5);

        JLabel doneRoundsLabel;
        if (Global.isAsynchronousMode()) {
            doneRoundsLabel = new JLabel("Time: ");
            this.getRoundsPerformed().setText(String.valueOf(this.round(Global.getCurrentTime(), 2)));
        } else {
            doneRoundsLabel = new JLabel("Round: ");
            this.getRoundsPerformed().setText(String.valueOf((int) this.round(Global.getCurrentTime(), 2)));
        }
        this.getButtonPanel().add(doneRoundsLabel);
        this.getRoundsPerformed().setEditable(false);
        this.getRoundsPerformed().setBorder(BorderFactory.createEmptyBorder());
        this.getRoundsPerformed().setToolTipText("Number of rounds performed so far");
        this.getButtonPanel().add(this.getRoundsPerformed());

        // Add the user-defined buttons
        Vector<JButton> customButtons = super.createCustomButtons();
        if (customButtons.size() > 0) {
            this.addSpacer(this.getButtonPanel(), 5);
            this.addSeparator(this.getButtonPanel());
            this.addSpacer(this.getButtonPanel(), 5);

            for (JButton b : customButtons) {
                this.getButtonPanel().add(b);
                this.addToDisabledButtonList(b);
            }
            this.addSpacer(this.getButtonPanel(), 4); // strange, but the last button is sometimes not painted...
        }

        JButton changeButton = this.createFrameworkIconButton("extendPanel", "maximize.gif", "Extend");
        changeButton.setPreferredSize(new Dimension(13, 29));
        this.addToDisabledButtonList(changeButton);
        this.add(BorderLayout.EAST, changeButton);
        this.add(BorderLayout.WEST, this.getButtonPanel());

        this.setVisible(true);
    }

    @Override
    public void setRoundsPerformed(double time, long eventNumber) {
        this.getRoundsPerformed().setText(String.valueOf(this.round(time, 2)));
        this.getButtonPanel().doLayout();
    }

    @Override
    public void setRoundsPerformed(long i) {
        this.getRoundsPerformed().setText(String.valueOf(i));
        this.getButtonPanel().doLayout();
    }

    @Override
    public void setCurrentEvent(Event e) {
        this.setStringsForCurrentEvent(e);
        Vector<String> v = new Vector<>(1);
        v.add(getCurrentEventString());
        this.getEventJList().setListData(v);
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
