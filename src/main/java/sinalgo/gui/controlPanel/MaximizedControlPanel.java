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
import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration;
import sinalgo.gui.GUI;
import sinalgo.gui.helper.MultiLineFlowLayout;
import sinalgo.gui.helper.NonRegularGridLayout;
import sinalgo.gui.multiLineTooltip.MultiLineToolTip;
import sinalgo.gui.multiLineTooltip.MultiLineToolTipJList;
import sinalgo.gui.popups.EventPopupMenu;
import sinalgo.gui.transformation.Transformation3D;
import sinalgo.runtime.Global;
import sinalgo.runtime.SinalgoRuntime;
import sinalgo.runtime.events.Event;
import sinalgo.runtime.events.EventQueue;
import sinalgo.runtime.events.EventQueueListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.Vector;

/**
 * The maximized version of the control panel.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class MaximizedControlPanel extends ControlPanel implements EventQueueListener {

    private static final long serialVersionUID = -4478176658450671857L;

    private EventQueueElement[] queueElements = new EventQueueElement[Configuration.getShownEventQueueSize()];

    private int controlPanelWidth = 200;

    private EventQueueList eventList;
    private JLayeredPane viewContent; // view panel, with button
    private JLayeredPane textContent; // text panel, with button
    private JLayeredPane projectControlContent; // project specific buttons, max/minimizable
    private JLayeredPane simulationPane; // the simulation panel, with button

    private JPanel events = new JPanel();

    private NonColoringNonBorderingCellRenderer nCNBCR = new NonColoringNonBorderingCellRenderer();
    private DefaultListCellRenderer dLCR = new DefaultListCellRenderer();

    private class MyScrollPane extends JPanel implements Scrollable {

        private static final long serialVersionUID = -7907252727026293260L;

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(MaximizedControlPanel.this.getControlPanelWidth(), MaximizedControlPanel.this.getParentGUI().getHeight() - 60); // hand-crafted :(
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 0;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 0;
        }

        MyScrollPane() {
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); // new BorderLayout());
            this.setBorder(BorderFactory.createEmptyBorder(-8, 0, 0, 0));

            // The button to change to the minimized view
            // ------------------------------------------------------------------------
            JPanel mPanel = new JPanel();
            JButton minimize = MaximizedControlPanel.this.createFrameworkIconButton("minimizedPanel", "minimize.gif", "Minimize");
            minimize.setPreferredSize(new Dimension(MaximizedControlPanel.this.getControlPanelWidth(), 11));
            MaximizedControlPanel.this.addToDisabledButtonList(minimize);
            mPanel.add(minimize);
            this.add(mPanel);

            // Simulation Control
            // ------------------------------------------------------------------------
            MaximizedControlPanel.this.setSimulationPane(new JLayeredPane());
            MaximizedControlPanel.this.createSimulationPanel();
            this.add(MaximizedControlPanel.this.getSimulationPane());

            // Customized Buttons
            // ------------------------------------------------------------------------
            MaximizedControlPanel.this.setProjectControlContent(new JLayeredPane()); // a layered panel for the minimize button
            MaximizedControlPanel.this.createProjectControlPanel();
            this.add(MaximizedControlPanel.this.getProjectControlContent());

            // VIEW Panel
            // ------------------------------------------------------------------------
            MaximizedControlPanel.this.setViewContent(new JLayeredPane()); // a layered panel for the minimize button
            MaximizedControlPanel.this.createViewPanel();
            this.add(MaximizedControlPanel.this.getViewContent());

            // TEXT Panel
            // ------------------------------------------------------------------------
            MaximizedControlPanel.this.setTextContent(new JLayeredPane());
            MaximizedControlPanel.this.createTextPanel();
            this.add(MaximizedControlPanel.this.getTextContent());
        }
    } // end of class MyScrollPane

    /**
     * Creates the content of the text panel
     */
    private void createTextPanel() {
        this.getTextContent().removeAll();

        JButton textPanelMinimizeButton;
        if (AppConfig.getAppConfig().isGuiControlPanelShowTextPanel()) {
            textPanelMinimizeButton = this.createFrameworkIconButton("minimizeText", "minimize.gif", "Minimize");
        } else {
            textPanelMinimizeButton = this.createFrameworkIconButton("maximizeText", "maximize.gif", "Maximize");
        }
        textPanelMinimizeButton.setPreferredSize(new Dimension(21, 11));
        this.getTextContent().add(textPanelMinimizeButton, JLayeredPane.PALETTE_LAYER);
        textPanelMinimizeButton.setBounds(this.getControlPanelWidth() - 26, 3, 21, 11);
        this.addToDisabledButtonList(textPanelMinimizeButton); // disable while simulating

        JPanel textPanel = new JPanel();
        textPanel.setBorder(BorderFactory.createTitledBorder("Output"));
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        this.getTextContent().add(textPanel, JLayeredPane.DEFAULT_LAYER);

        if (AppConfig.getAppConfig().isGuiControlPanelShowTextPanel()) {
            JScrollPane sp = new JScrollPane(getTextField(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            sp.setPreferredSize(new Dimension(this.getControlPanelWidth(), Configuration.getOutputTextFieldHeight()));
            getTextField().setEditable(false);
            getTextField().setLineWrap(true);
            textPanel.add(sp);
            JButton clearText = super.createTextButton("ClearOutputText", "Clear", "Remove all output");
            clearText.setPreferredSize(new Dimension(60, 12));
            clearText.setFont(new Font("", Font.PLAIN, 11));
            this.addToDisabledButtonList(clearText); // disable while simulating
            JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            p.add(clearText, BorderLayout.EAST);
            textPanel.add(p);
        }

        // Finally set the size of the textPanel
        Dimension dim = textPanel.getPreferredSize();
        textPanel.setBounds(0, 0, this.getControlPanelWidth(), dim.height);
        this.getTextContent().setPreferredSize(dim);
    }

    private void createSimulationPanel() {
        this.getSimulationPane().removeAll(); // restart from scratch
        boolean isMax = AppConfig.getAppConfig().isGuiControlPanelExpandSimulation();

        if (Global.isAsynchronousMode()) { // the minimization button is only needed in async mode.
            JButton simulationPanelMinimizeButton;
            if (isMax) {
                simulationPanelMinimizeButton = this.createFrameworkIconButton("minimizeSimControl", "minimize.gif",
                        "Minimize");
            } else {
                simulationPanelMinimizeButton = this.createFrameworkIconButton("maximizeSimControl", "maximize.gif",
                        "Maximize");
            }
            simulationPanelMinimizeButton.setPreferredSize(new Dimension(21, 11));
            this.getSimulationPane().add(simulationPanelMinimizeButton, JLayeredPane.PALETTE_LAYER);
            simulationPanelMinimizeButton.setBounds(this.getControlPanelWidth() - 26, 3, 21, 11);
            this.addToDisabledButtonList(simulationPanelMinimizeButton); // disable while simulating
        }

        JPanel roundControl = new JPanel();
        roundControl.setBorder(BorderFactory.createTitledBorder("Simulation Control"));
        roundControl.setLayout(new BoxLayout(roundControl, BoxLayout.Y_AXIS));
        this.getSimulationPane().add(roundControl, JLayeredPane.DEFAULT_LAYER);

        this.setInfo(new JPanel());

        Font labelFont = this.getInfo().getFont();
        JLabel passedTimeLabel;
        JLabel eventNumberLabel;
        if (Global.isAsynchronousMode()) {
            passedTimeLabel = new JLabel("Time: ");
            passedTimeLabel.setFont(labelFont);
            this.getTimePerformed().setText(String.valueOf(this.round(Global.getCurrentTime(), 4)));
            this.getTimePerformed().setEditable(false);
            this.getTimePerformed().setBorder(BorderFactory.createEmptyBorder());
            this.getInfo().add(passedTimeLabel);
            this.getInfo().add(this.getTimePerformed());

            if (isMax) {
                eventNumberLabel = new JLabel("Events: ");
                eventNumberLabel.setFont(labelFont);
                this.getRoundsPerformed().setText(String.valueOf(EventQueue.getEventNumber()));
                this.getRoundsPerformed().setEditable(false);
                this.getRoundsPerformed().setBorder(BorderFactory.createEmptyBorder());
                this.getInfo().add(eventNumberLabel);
                this.getInfo().add(this.getRoundsPerformed());

                this.getInfo().add(new JPanel()); // add some space between the 'performed rounds' and the 'rounds to perform'
                this.getInfo().add(new JPanel());
            }

            // roundNumber.setText(String.valueOf(Configuration.defaultRoundNumber));
            getRoundsToPerformLabel().setText("Events to do:    ");
            getRoundsToPerformLabel().setFont(labelFont);
            this.getInfo().add(getRoundsToPerformLabel());
            this.getInfo().add(getRoundsToPerform());

        } else { // Synchronous mode
            passedTimeLabel = new JLabel("Round: ");
            passedTimeLabel.setFont(labelFont);
            this.getTimePerformed().setText(String.valueOf((int) this.round(Global.getCurrentTime(), 4)));

            this.getTimePerformed().setEditable(false);
            this.getTimePerformed().setBorder(BorderFactory.createEmptyBorder());

            this.getInfo().add(passedTimeLabel);
            this.getInfo().add(this.getTimePerformed());

            this.getInfo().add(new JPanel()); // add some space between the 'performed rounds' and the 'rounds to perform'
            this.getInfo().add(new JPanel());

            getRoundsToPerformLabel().setText("Rounds to do:  ");
            getRoundsToPerformLabel().setFont(labelFont);
            this.getInfo().add(getRoundsToPerformLabel());
            this.getInfo().add(getRoundsToPerform());
        }

        getRefreshRate().setText(String.valueOf(Configuration.getRefreshRate()));
        JLabel refreshLabel = new JLabel("Refresh rate: ");
        refreshLabel.setFont(labelFont);
        this.getInfo().add(refreshLabel);
        this.getInfo().add(getRefreshRate());

        NonRegularGridLayout nrgl = new NonRegularGridLayout(this.getInfo().getComponentCount() / 2, 2, 1, 2);
        nrgl.setAlignToLeft(true);
        this.getInfo().setLayout(nrgl);
        roundControl.add(this.getInfo());

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 5));
        buttons.add(getStart());

        // the run-selection button
        this.setRunMenuButton(this.createFrameworkIconButton("RunMenu", "maximize.gif", "Run Options"));
        this.getRunMenuButton().setPreferredSize(new Dimension(13, 29));
        buttons.add(this.getRunMenuButton());
        // raise the 'run' menu whenever the mouse idles over this button
        this.getRunMenuButton().addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (MaximizedControlPanel.this.getRunMenuButton().isEnabled()) {
                    getStart().setBorderPainted(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (MaximizedControlPanel.this.getRunMenuButton().isEnabled()) {
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
        this.addToDisabledButtonList(this.getRunMenuButton()); // disable while running

        this.setAbort(this.createFrameworkIconButton("Abort", "abort.gif", "Abort Simulation"));
        this.getAbort().setEnabled(false);
        buttons.add(this.getAbort());

        roundControl.add(buttons);

        // Async mode - list of events
        // ------------------------------------------------------------------------
        // if there is an actual Event: add the panel.
        if (Global.isAsynchronousMode()) {
            this.getEvents().setLayout(new BorderLayout());

            String[] elements = {getCurrentEventString()};
            this.setEventJList(new MultiLineToolTipJList());
            this.getEventJList().setListData(elements);
            this.getEventJList().setToolTipText(
                    "The last Event that has been executed.\nDouble click the event to get more information.");
            this.getEventJList().setCellRenderer(new NonColoringNonBorderingCellRenderer());
            MouseListener mouseListener = new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        if (e.getClickCount() == 2) {
                            MaximizedControlPanel.this.getEventJList().setCellRenderer(MaximizedControlPanel.this.getDLCR());
                            JOptionPane.showMessageDialog(null, getCurrentEventString() + "\n" + getCurrentEventToolTip(),
                                    "Information about an Event", JOptionPane.INFORMATION_MESSAGE);
                            MaximizedControlPanel.this.getEventJList().setCellRenderer(MaximizedControlPanel.this.getNCNBCR());
                        }
                    }
                }
            };
            this.getEventJList().addMouseListener(mouseListener);
            int fixedCellHeight = 12;
            this.getEventJList().setFixedCellHeight(fixedCellHeight);
            int fixedCellWidth = 180;
            this.getEventJList().setFixedCellWidth(fixedCellWidth);
            this.getEventJList().setPreferredSize(new Dimension(this.getControlPanelWidth(), fixedCellHeight + 6));
            this.getEventJList().setBorder(javax.swing.plaf.metal.MetalBorders.getTextFieldBorder());
            this.getEventJList().setFont(this.getEventJList().getFont().deriveFont(Font.PLAIN));

            this.getEvents().add(BorderLayout.NORTH, this.getEventJList());

            for (int i = 0; i < this.getQueueElements().length; i++) {
                this.getQueueElements()[i] = new EventQueueElement(null, null);
            }

            this.composeEventList();
            this.setEventList(new EventQueueList(this.getQueueElements()));
            SinalgoRuntime.getEventQueue().addEventQueueListener(MaximizedControlPanel.this);
            this.getEventList().setCellRenderer(new NonColoringNonBorderingCellRenderer());
            this.getEventList().setFixedCellHeight(fixedCellHeight);
            this.getEventList().setFixedCellWidth(fixedCellWidth);
            this.getEventList().setFont(this.getEventList().getFont().deriveFont(Font.PLAIN));
            JScrollPane scrollableEventList = new JScrollPane(this.getEventList());

            int height = Configuration.getShownEventQueueSize() * fixedCellHeight + 4;
            scrollableEventList.setPreferredSize(new Dimension(this.getControlPanelWidth(), height));

            this.getEvents().add(BorderLayout.SOUTH, scrollableEventList);
            if (isMax) {
                roundControl.add(this.getEvents());
            }
        }

        // Finally set the size of the viewPanel
        Dimension dim = roundControl.getPreferredSize();
        roundControl.setBounds(0, 0, this.getControlPanelWidth(), dim.height);
        this.getSimulationPane().setPreferredSize(dim);
    }

    /**
     * Creates the content of the project-specific control panel, which contains the
     * project specific buttons
     */
    private void createProjectControlPanel() {
        Vector<JButton> cb = this.createCustomButtons();
        if (cb.size() == 0) {
            return; // no buttons to be displayed
        }
        this.getProjectControlContent().removeAll();

        JButton minimizeButton;
        if (AppConfig.getAppConfig().isGuiControlPanelShowProjectControl()) {
            minimizeButton = this.createFrameworkIconButton("minimizeProjectControl", "minimize.gif", "Minimize");
        } else {
            minimizeButton = this.createFrameworkIconButton("maximizeProjectControl", "maximize.gif", "Maximize");
        }
        minimizeButton.setPreferredSize(new Dimension(21, 11));
        this.getProjectControlContent().add(minimizeButton, JLayeredPane.PALETTE_LAYER);
        minimizeButton.setBounds(this.getControlPanelWidth() - 26, 3, 21, 11);
        this.addToDisabledButtonList(minimizeButton); // disable while simulating

        JPanel customButtons = new JPanel();
        customButtons.setBorder(BorderFactory.createTitledBorder("Project Control"));

        if (AppConfig.getAppConfig().isGuiControlPanelShowProjectControl()) {
            customButtons.setPreferredSize(new Dimension(this.getControlPanelWidth(), 3000));
            customButtons.setLayout(new MultiLineFlowLayout(this.getControlPanelWidth(), 0, 0));

            for (JButton b : cb) {
                customButtons.add(b);
                this.addToDisabledButtonList(b);
            }
            customButtons.doLayout();
            // adjust the size of the
            Dimension d = customButtons.getLayout().preferredLayoutSize(customButtons);
            d.width = this.getControlPanelWidth(); // enforce the width
            customButtons.setPreferredSize(d);
        } else {
            customButtons.setLayout(new BoxLayout(customButtons, BoxLayout.Y_AXIS));
        }

        this.getProjectControlContent().add(customButtons);

        // Finally set the size of the viewPanel
        Dimension dim = customButtons.getPreferredSize();
        customButtons.setBounds(0, 0, this.getControlPanelWidth(), dim.height);
        this.getProjectControlContent().setPreferredSize(dim);
        this.getProjectControlContent().invalidate();
    }

    /**
     * Creates the content of the view panel
     */
    private void createViewPanel() {
        this.getViewContent().removeAll();
        JPanel viewPanel = new JPanel();
        viewPanel.setBorder(BorderFactory.createTitledBorder("View"));
        viewPanel.setLayout(new BoxLayout(viewPanel, BoxLayout.Y_AXIS));
        this.getViewContent().add(viewPanel, JLayeredPane.DEFAULT_LAYER);

        JButton viewPanelMinimizeButton;
        if (AppConfig.getAppConfig().isGuiControlPanelShowFullViewPanel()) {
            viewPanelMinimizeButton = this.createFrameworkIconButton("minimizeView", "minimize.gif", "Minimize");
        } else {
            viewPanelMinimizeButton = this.createFrameworkIconButton("maximizeView", "maximize.gif", "Maximize");
        }
        viewPanelMinimizeButton.setPreferredSize(new Dimension(21, 11));
        this.getViewContent().add(viewPanelMinimizeButton, JLayeredPane.PALETTE_LAYER);
        viewPanelMinimizeButton.setBounds(this.getControlPanelWidth() - 26, 3, 21, 11);
        this.addToDisabledButtonList(viewPanelMinimizeButton); // disable while simulating

        // .... add zoom view
        if (AppConfig.getAppConfig().isGuiControlPanelShowFullViewPanel()) {
            if (this.getParentGUI().getTransformator().supportReverseTranslation()) {
                // only show the coordinate if it can be mapped from GUI to logic coordinates
                JPanel mousePos = new JPanel();
                JLabel mousePosLabel = new JLabel("Mouse Position:");
                mousePosLabel.setFont(this.getMousePositionField().getFont());
                mousePos.add(mousePosLabel);
                mousePos.add(this.getMousePositionField());
                this.getMousePositionField().setText("");
                this.getMousePositionField().setEditable(false);
                this.getMousePositionField().setBorder(BorderFactory.createEmptyBorder());

                viewPanel.add(mousePos);
            }

            this.setZoomPanel(new ZoomPanel(this.getParentGUI(), this.getParentGUI().getTransformator()));
            this.getZoomPanel().setPreferredSize(
                    new Dimension(this.getControlPanelWidth(), this.getZoomPanel().getPreferredHeight(this.getControlPanelWidth())));
            viewPanel.add(this.getZoomPanel());
        }

        JPanel buttonPanel = new JPanel();
        FlowLayout buttonLayout = new FlowLayout(FlowLayout.CENTER, 2, 0);
        buttonPanel.setLayout(buttonLayout);
        // create the buttons

        JButton button = this.createFrameworkIconButton("zoomIn", "zoominimage.png", "Zoom In");
        buttonPanel.add(button);
        this.addToDisabledButtonList(button);

        button = this.createFrameworkIconButton("zoomOut", "zoomoutimage.png", "Zoom Out");
        buttonPanel.add(button);
        this.addToDisabledButtonList(button);

        button = this.createFrameworkIconButton("zoomToFit", "zoomtofit.gif", "Zoom To Fit");
        buttonPanel.add(button);
        this.addToDisabledButtonList(button);

        if (this.getParentGUI().getTransformator() instanceof Transformation3D) {
            button = this.createFrameworkIconButton("zoomToFit3D", "zoomtofit3d.gif", "Default View");
            buttonPanel.add(button);
            this.addToDisabledButtonList(button);
        }

        viewPanel.add(buttonPanel);

        // Finally set the size of the viewPanel
        Dimension dim = viewPanel.getPreferredSize();
        viewPanel.setBounds(0, 0, this.getControlPanelWidth(), dim.height);
        this.getViewContent().setPreferredSize(dim);
    }

    /**
     * Handle some actions unique to this maximized control panel
     *
     * @see sinalgo.gui.controlPanel.ControlPanel#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case "minimizeView":
                AppConfig.getAppConfig().setGuiControlPanelShowFullViewPanel(false);
                this.createViewPanel();
                break;
            case "maximizeView":
                AppConfig.getAppConfig().setGuiControlPanelShowFullViewPanel(true);
                this.createViewPanel();
                break;
            case "minimizeText":
                AppConfig.getAppConfig().setGuiControlPanelShowTextPanel(false);
                this.createTextPanel();
                break;
            case "maximizeText":
                AppConfig.getAppConfig().setGuiControlPanelShowTextPanel(true);
                this.createTextPanel();
                break;
            case "minimizeProjectControl":
                AppConfig.getAppConfig().setGuiControlPanelShowProjectControl(false);
                this.createProjectControlPanel();
                break;
            case "maximizeProjectControl":
                AppConfig.getAppConfig().setGuiControlPanelShowProjectControl(true);
                this.createProjectControlPanel();
                break;
            case "maximizeSimControl":
                AppConfig.getAppConfig().setGuiControlPanelExpandSimulation(true);
                this.createSimulationPanel();
                break;
            case "minimizeSimControl":
                AppConfig.getAppConfig().setGuiControlPanelExpandSimulation(false);
                this.createSimulationPanel();
                break;
            case "ClearOutputText":
                this.clearOutput();
                break;
            default:
                super.actionPerformed(e);
                break;
        }
    }

    /**
     * Creates a MaximizedControlPanel for the specified GUI instance.
     *
     * @param p The Gui instance to create the MaximizedControlPanel for.
     */
    public MaximizedControlPanel(GUI p) {
        this.setParentGUI(p);
        this.setBorder(BorderFactory.createRaisedBevelBorder());
        this.setLayout(new BorderLayout());

        MyScrollPane msp = new MyScrollPane();
        JScrollPane scrollPane = new JScrollPane(msp);
        scrollPane.setBorder(null);

        this.setMaximumSize(new Dimension(this.getControlPanelWidth(), 2000));
        this.setMinimumSize(new Dimension(this.getControlPanelWidth(), 2000));

        this.add(BorderLayout.CENTER, scrollPane);
        this.setVisible(true);
    }

    @Override
    public void setRoundsPerformed(double time, long eventNumber) {
        this.getTimePerformed().setText(String.valueOf(this.round(time, 4)));
        this.getRoundsPerformed().setText(String.valueOf(eventNumber));
    }

    @Override
    public void setRoundsPerformed(long i) {
        this.getTimePerformed().setText(String.valueOf(i));
    }

    @Override
    public void setCurrentEvent(Event e) {
        this.setStringsForCurrentEvent(e);
        String[] v = {getCurrentEventString()};
        this.getEventJList().setListData(v);

        this.composeEventList();
        this.getEventList().setListData(this.getQueueElements());
        // remove the focus from the list, for cases when the wrong one is installed
        // (which happens if one presses the ESC button)
        this.getEventList().setCellRenderer(this.getNCNBCR());
    }

    @Override
    public void eventQueueChanged() {
        if (!Global.isRunning()) {
            this.composeEventList();
            this.getEventList().setListData(this.getQueueElements());
            // remove the focus from the list, for cases when the wrong one is installed
            // (which happens if one presses the ESC button)
            this.getEventList().setCellRenderer(this.getNCNBCR());
        }
    }

    private void composeEventList() {
        Iterator<Event> eventIter = SinalgoRuntime.getEventQueue().iterator();
        for (EventQueueElement queueElement : this.getQueueElements()) {
            if (eventIter.hasNext()) {
                Event e = eventIter.next();
                queueElement.setText(e.getEventListText(false));
                queueElement.setToolTipText(e.getEventListToolTipText(false));
                queueElement.setEvent(e);
            } else {
                queueElement.setText(null);
                queueElement.setToolTipText(null);
                queueElement.setEvent(null);
            }
        }
    }

    private class NonColoringNonBorderingCellRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = -5717256666978390866L;

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            this.setBackground(new Color(255, 255, 255));
            this.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
            return this;
        }
    }

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private class EventQueueElement extends JComponent {

        private static final long serialVersionUID = 2348221129614192757L;

        private String displayableText;
        private String tooltip = "";

        /**
         * The event associated w/ this element, null if there is no event
         * assoc. w/ this element
         */
        @Getter
        @Setter
        private Event event;

        private EventQueueElement(String displayableText, String tooltip) {
            this.setDisplayableText(displayableText);
            this.setTooltip(tooltip);
        }

        /**
         * Sets the text for this element. To get this text call the toString method.
         *
         * @param displayableText The text to be set for this element.
         */
        public void setText(String displayableText) {
            this.setDisplayableText(displayableText);
        }

        @Override
        public void setToolTipText(String tooltip) {
            this.setTooltip(tooltip);
        }

        @Override
        public String toString() {
            return this.getDisplayableText();
        }

        @Override
        public String getToolTipText() {
            return this.getTooltip();
        }
    }

    public class EventQueueList extends JList {

        private static final long serialVersionUID = 7394423151110879133L;

        /**
         * Creates an instance of EventQueueList for the given Array of elements.
         *
         * @param data The Array of elements to be contained in the list.
         */
        private EventQueueList(Object[] data) {
            super(data);
            MouseListener mouseListener = new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                        int index = EventQueueList.this.locationToIndex(e.getPoint());
                        if (MaximizedControlPanel.this.getQueueElements()[index] != null
                                && MaximizedControlPanel.this.getQueueElements()[index].toString() != null) {
                            EventQueueElement selElem = MaximizedControlPanel.this.getQueueElements()[index];
                            EventQueueList.this.setSelectedIndex(index);
                            EventQueueList.this.setCellRenderer(MaximizedControlPanel.this.getDLCR()); // mark the element
                            JOptionPane.showMessageDialog(null, selElem.toString() + "\n" + selElem.getToolTipText(),
                                    "Information about an Event", JOptionPane.INFORMATION_MESSAGE);
                            EventQueueList.this.setCellRenderer(MaximizedControlPanel.this.getNCNBCR()); // unmark it
                        }
                    }
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        int index = EventQueueList.this.locationToIndex(e.getPoint());
                        if (index >= 0 && MaximizedControlPanel.this.getQueueElements()[index] != null
                                && MaximizedControlPanel.this.getQueueElements()[index].getEvent() != null) {
                            Event event = MaximizedControlPanel.this.getQueueElements()[index].getEvent();
                            EventQueueList.this.setSelectedIndex(index);
                            EventQueueList.this.setCellRenderer(MaximizedControlPanel.this.getDLCR()); // mark the element
                            EventPopupMenu epm = new EventPopupMenu(event, EventQueueList.this, MaximizedControlPanel.this.getNCNBCR());
                            epm.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                }
            };
            this.addMouseListener(mouseListener);
        }

        @Override
        public JToolTip createToolTip() {
            return new MultiLineToolTip();
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            if (this.getCellBounds(0, this.getModel().getSize() - 1).contains(event.getPoint())) {
                int index = this.locationToIndex(event.getPoint());
                EventQueueElement element = (EventQueueElement) this.getModel().getElementAt(index);
                return element.getToolTipText();
            } else {
                if (((EventQueueElement) this.getModel().getElementAt(0)).getDisplayableText() == null) {
                    return "No event scheduled";
                } else {
                    return null;
                }
            }
        }
    }
}
