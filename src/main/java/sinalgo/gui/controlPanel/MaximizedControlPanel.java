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
public class MaximizedControlPanel extends ControlPanel implements EventQueueListener {

    private static final long serialVersionUID = -4478176658450671857L;

    private AppConfig appConfig = AppConfig.getAppConfig();

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
            return new Dimension(MaximizedControlPanel.this.controlPanelWidth, MaximizedControlPanel.this.parent.getHeight() - 60); // hand-crafted :(
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
            minimize.setPreferredSize(new Dimension(MaximizedControlPanel.this.controlPanelWidth, 11));
            MaximizedControlPanel.this.addToDisabledButtonList(minimize);
            mPanel.add(minimize);
            this.add(mPanel);

            // Simulation Control
            // ------------------------------------------------------------------------
            MaximizedControlPanel.this.simulationPane = new JLayeredPane();
            MaximizedControlPanel.this.createSimulationPanel();
            this.add(MaximizedControlPanel.this.simulationPane);

            // Customized Buttons
            // ------------------------------------------------------------------------
            MaximizedControlPanel.this.projectControlContent = new JLayeredPane(); // a layered panel for the minimize button
            MaximizedControlPanel.this.createProjectControlPanel();
            this.add(MaximizedControlPanel.this.projectControlContent);

            // VIEW Panel
            // ------------------------------------------------------------------------
            MaximizedControlPanel.this.viewContent = new JLayeredPane(); // a layered panel for the minimize button
            MaximizedControlPanel.this.createViewPanel();
            this.add(MaximizedControlPanel.this.viewContent);

            // TEXT Panel
            // ------------------------------------------------------------------------
            MaximizedControlPanel.this.textContent = new JLayeredPane();
            MaximizedControlPanel.this.createTextPanel();
            this.add(MaximizedControlPanel.this.textContent);
        }
    } // end of class MyScrollPane

    /**
     * Creates the content of the text panel
     */
    private void createTextPanel() {
        this.textContent.removeAll();

        JButton textPanelMinimizeButton;
        if (this.appConfig.isGuiControlPanelShowTextPanel()) {
            textPanelMinimizeButton = this.createFrameworkIconButton("minimizeText", "minimize.gif", "Minimize");
        } else {
            textPanelMinimizeButton = this.createFrameworkIconButton("maximizeText", "maximize.gif", "Maximize");
        }
        textPanelMinimizeButton.setPreferredSize(new Dimension(21, 11));
        this.textContent.add(textPanelMinimizeButton, JLayeredPane.PALETTE_LAYER);
        textPanelMinimizeButton.setBounds(this.controlPanelWidth - 26, 3, 21, 11);
        this.addToDisabledButtonList(textPanelMinimizeButton); // disable while simulating

        JPanel textPanel = new JPanel();
        textPanel.setBorder(BorderFactory.createTitledBorder("Output"));
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        this.textContent.add(textPanel, JLayeredPane.DEFAULT_LAYER);

        if (this.appConfig.isGuiControlPanelShowTextPanel()) {
            JScrollPane sp = new JScrollPane(textField, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            sp.setPreferredSize(new Dimension(this.controlPanelWidth, Configuration.getOutputTextFieldHeight()));
            textField.setEditable(false);
            textField.setLineWrap(true);
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
        textPanel.setBounds(0, 0, this.controlPanelWidth, dim.height);
        this.textContent.setPreferredSize(dim);
    }

    private void createSimulationPanel() {
        this.simulationPane.removeAll(); // restart from scratch
        boolean isMax = this.appConfig.isGuiControlPanelExpandSimulation();

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
            this.simulationPane.add(simulationPanelMinimizeButton, JLayeredPane.PALETTE_LAYER);
            simulationPanelMinimizeButton.setBounds(this.controlPanelWidth - 26, 3, 21, 11);
            this.addToDisabledButtonList(simulationPanelMinimizeButton); // disable while simulating
        }

        JPanel roundControl = new JPanel();
        roundControl.setBorder(BorderFactory.createTitledBorder("Simulation Control"));
        roundControl.setLayout(new BoxLayout(roundControl, BoxLayout.Y_AXIS));
        this.simulationPane.add(roundControl, JLayeredPane.DEFAULT_LAYER);

        this.info = new JPanel();

        Font labelFont = this.info.getFont();
        JLabel passedTimeLabel;
        JLabel eventNumberLabel;
        if (Global.isAsynchronousMode()) {
            passedTimeLabel = new JLabel("Time: ");
            passedTimeLabel.setFont(labelFont);
            this.timePerformed.setText(String.valueOf(this.round(Global.getCurrentTime(), 4)));
            this.timePerformed.setEditable(false);
            this.timePerformed.setBorder(BorderFactory.createEmptyBorder());
            this.info.add(passedTimeLabel);
            this.info.add(this.timePerformed);

            if (isMax) {
                eventNumberLabel = new JLabel("Events: ");
                eventNumberLabel.setFont(labelFont);
                this.roundsPerformed.setText(String.valueOf(EventQueue.getEventNumber()));
                this.roundsPerformed.setEditable(false);
                this.roundsPerformed.setBorder(BorderFactory.createEmptyBorder());
                this.info.add(eventNumberLabel);
                this.info.add(this.roundsPerformed);

                this.info.add(new JPanel()); // add some space between the 'performed rounds' and the 'rounds to perform'
                this.info.add(new JPanel());
            }

            // roundNumber.setText(String.valueOf(Configuration.defaultRoundNumber));
            roundsToPerformLabel.setText("Events to do:    ");
            roundsToPerformLabel.setFont(labelFont);
            this.info.add(roundsToPerformLabel);
            this.info.add(roundsToPerform);

        } else { // Synchronous mode
            passedTimeLabel = new JLabel("Round: ");
            passedTimeLabel.setFont(labelFont);
            this.timePerformed.setText(String.valueOf((int) this.round(Global.getCurrentTime(), 4)));

            this.timePerformed.setEditable(false);
            this.timePerformed.setBorder(BorderFactory.createEmptyBorder());

            this.info.add(passedTimeLabel);
            this.info.add(this.timePerformed);

            this.info.add(new JPanel()); // add some space between the 'performed rounds' and the 'rounds to perform'
            this.info.add(new JPanel());

            roundsToPerformLabel.setText("Rounds to do:  ");
            roundsToPerformLabel.setFont(labelFont);
            this.info.add(roundsToPerformLabel);
            this.info.add(roundsToPerform);
        }

        refreshRate.setText(String.valueOf(Configuration.getRefreshRate()));
        JLabel refreshLabel = new JLabel("Refresh rate: ");
        refreshLabel.setFont(labelFont);
        this.info.add(refreshLabel);
        this.info.add(refreshRate);

        NonRegularGridLayout nrgl = new NonRegularGridLayout(this.info.getComponentCount() / 2, 2, 1, 2);
        nrgl.setAlignToLeft(true);
        this.info.setLayout(nrgl);
        roundControl.add(this.info);

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 5));
        buttons.add(start);

        // the run-selection button
        this.runMenuButton = this.createFrameworkIconButton("RunMenu", "maximize.gif", "Run Options");
        this.runMenuButton.setPreferredSize(new Dimension(13, 29));
        buttons.add(this.runMenuButton);
        // raise the 'run' menu whenever the mouse idles over this button
        this.runMenuButton.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (MaximizedControlPanel.this.runMenuButton.isEnabled()) {
                    start.setBorderPainted(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (MaximizedControlPanel.this.runMenuButton.isEnabled()) {
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
        this.addToDisabledButtonList(this.runMenuButton); // disable while running

        this.abort = this.createFrameworkIconButton("Abort", "abort.gif", "Abort Simulation");
        this.abort.setEnabled(false);
        buttons.add(this.abort);

        roundControl.add(buttons);

        // Async mode - list of events
        // ------------------------------------------------------------------------
        // if there is an actual Event: add the panel.
        if (Global.isAsynchronousMode()) {
            this.events.setLayout(new BorderLayout());

            String[] elements = {currentEventString};
            this.eventJList = new MultiLineToolTipJList();
            this.eventJList.setListData(elements);
            this.eventJList.setToolTipText(
                    "The last Event that has been executed.\nDouble click the event to get more information.");
            this.eventJList.setCellRenderer(new NonColoringNonBorderingCellRenderer());
            MouseListener mouseListener = new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        if (e.getClickCount() == 2) {
                            MaximizedControlPanel.this.eventJList.setCellRenderer(MaximizedControlPanel.this.dLCR);
                            JOptionPane.showMessageDialog(null, currentEventString + "\n" + currentEventToolTip,
                                    "Information about an Event", JOptionPane.INFORMATION_MESSAGE);
                            MaximizedControlPanel.this.eventJList.setCellRenderer(MaximizedControlPanel.this.nCNBCR);
                        }
                    }
                }
            };
            this.eventJList.addMouseListener(mouseListener);
            int fixedCellHeight = 12;
            this.eventJList.setFixedCellHeight(fixedCellHeight);
            int fixedCellWidth = 180;
            this.eventJList.setFixedCellWidth(fixedCellWidth);
            this.eventJList.setPreferredSize(new Dimension(this.controlPanelWidth, fixedCellHeight + 6));
            this.eventJList.setBorder(javax.swing.plaf.metal.MetalBorders.getTextFieldBorder());
            this.eventJList.setFont(this.eventJList.getFont().deriveFont(Font.PLAIN));

            this.events.add(BorderLayout.NORTH, this.eventJList);

            for (int i = 0; i < this.queueElements.length; i++) {
                this.queueElements[i] = new EventQueueElement(null, null);
            }

            this.composeEventList();
            this.eventList = new EventQueueList(this.queueElements);
            SinalgoRuntime.eventQueue.addEventQueueListener(MaximizedControlPanel.this);
            this.eventList.setCellRenderer(new NonColoringNonBorderingCellRenderer());
            this.eventList.setFixedCellHeight(fixedCellHeight);
            this.eventList.setFixedCellWidth(fixedCellWidth);
            this.eventList.setFont(this.eventList.getFont().deriveFont(Font.PLAIN));
            JScrollPane scrollableEventList = new JScrollPane(this.eventList);

            int height = Configuration.getShownEventQueueSize() * fixedCellHeight + 4;
            scrollableEventList.setPreferredSize(new Dimension(this.controlPanelWidth, height));

            this.events.add(BorderLayout.SOUTH, scrollableEventList);
            if (isMax) {
                roundControl.add(this.events);
            }
        }

        // Finally set the size of the viewPanel
        Dimension dim = roundControl.getPreferredSize();
        roundControl.setBounds(0, 0, this.controlPanelWidth, dim.height);
        this.simulationPane.setPreferredSize(dim);
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
        this.projectControlContent.removeAll();

        JButton minimizeButton;
        if (this.appConfig.isGuiControlPanelShowProjectControl()) {
            minimizeButton = this.createFrameworkIconButton("minimizeProjectControl", "minimize.gif", "Minimize");
        } else {
            minimizeButton = this.createFrameworkIconButton("maximizeProjectControl", "maximize.gif", "Maximize");
        }
        minimizeButton.setPreferredSize(new Dimension(21, 11));
        this.projectControlContent.add(minimizeButton, JLayeredPane.PALETTE_LAYER);
        minimizeButton.setBounds(this.controlPanelWidth - 26, 3, 21, 11);
        this.addToDisabledButtonList(minimizeButton); // disable while simulating

        JPanel customButtons = new JPanel();
        customButtons.setBorder(BorderFactory.createTitledBorder("Project Control"));

        if (this.appConfig.isGuiControlPanelShowProjectControl()) {
            customButtons.setPreferredSize(new Dimension(this.controlPanelWidth, 3000));
            customButtons.setLayout(new MultiLineFlowLayout(this.controlPanelWidth, 0, 0));

            for (JButton b : cb) {
                customButtons.add(b);
                this.addToDisabledButtonList(b);
            }
            customButtons.doLayout();
            // adjust the size of the
            Dimension d = customButtons.getLayout().preferredLayoutSize(customButtons);
            d.width = this.controlPanelWidth; // enforce the width
            customButtons.setPreferredSize(d);
        } else {
            customButtons.setLayout(new BoxLayout(customButtons, BoxLayout.Y_AXIS));
        }

        this.projectControlContent.add(customButtons);

        // Finally set the size of the viewPanel
        Dimension dim = customButtons.getPreferredSize();
        customButtons.setBounds(0, 0, this.controlPanelWidth, dim.height);
        this.projectControlContent.setPreferredSize(dim);
        this.projectControlContent.invalidate();
    }

    /**
     * Creates the content of the view panel
     */
    private void createViewPanel() {
        this.viewContent.removeAll();
        JPanel viewPanel = new JPanel();
        viewPanel.setBorder(BorderFactory.createTitledBorder("View"));
        viewPanel.setLayout(new BoxLayout(viewPanel, BoxLayout.Y_AXIS));
        this.viewContent.add(viewPanel, JLayeredPane.DEFAULT_LAYER);

        JButton viewPanelMinimizeButton;
        if (this.appConfig.isGuiControlPanelShowFullViewPanel()) {
            viewPanelMinimizeButton = this.createFrameworkIconButton("minimizeView", "minimize.gif", "Minimize");
        } else {
            viewPanelMinimizeButton = this.createFrameworkIconButton("maximizeView", "maximize.gif", "Maximize");
        }
        viewPanelMinimizeButton.setPreferredSize(new Dimension(21, 11));
        this.viewContent.add(viewPanelMinimizeButton, JLayeredPane.PALETTE_LAYER);
        viewPanelMinimizeButton.setBounds(this.controlPanelWidth - 26, 3, 21, 11);
        this.addToDisabledButtonList(viewPanelMinimizeButton); // disable while simulating

        // .... add zoom view
        if (this.appConfig.isGuiControlPanelShowFullViewPanel()) {
            if (this.parent.getTransformator().supportReverseTranslation()) {
                // only show the coordinate if it can be mapped from GUI to logic coordinates
                JPanel mousePos = new JPanel();
                JLabel mousePosLabel = new JLabel("Mouse Position:");
                mousePosLabel.setFont(this.mousePositionField.getFont());
                mousePos.add(mousePosLabel);
                mousePos.add(this.mousePositionField);
                this.mousePositionField.setText("");
                this.mousePositionField.setEditable(false);
                this.mousePositionField.setBorder(BorderFactory.createEmptyBorder());

                viewPanel.add(mousePos);
            }

            this.zoomPanel = new ZoomPanel(this.parent, this.parent.getTransformator());
            this.zoomPanel.setPreferredSize(
                    new Dimension(this.controlPanelWidth, this.zoomPanel.getPreferredHeight(this.controlPanelWidth)));
            viewPanel.add(this.zoomPanel);
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

        if (this.parent.getTransformator() instanceof Transformation3D) {
            button = this.createFrameworkIconButton("zoomToFit3D", "zoomtofit3d.gif", "Default View");
            buttonPanel.add(button);
            this.addToDisabledButtonList(button);
        }

        viewPanel.add(buttonPanel);

        // Finally set the size of the viewPanel
        Dimension dim = viewPanel.getPreferredSize();
        viewPanel.setBounds(0, 0, this.controlPanelWidth, dim.height);
        this.viewContent.setPreferredSize(dim);
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
                this.appConfig.setGuiControlPanelShowFullViewPanel(false);
                this.createViewPanel();
                break;
            case "maximizeView":
                this.appConfig.setGuiControlPanelShowFullViewPanel(true);
                this.createViewPanel();
                break;
            case "minimizeText":
                this.appConfig.setGuiControlPanelShowTextPanel(false);
                this.createTextPanel();
                break;
            case "maximizeText":
                this.appConfig.setGuiControlPanelShowTextPanel(true);
                this.createTextPanel();
                break;
            case "minimizeProjectControl":
                this.appConfig.setGuiControlPanelShowProjectControl(false);
                this.createProjectControlPanel();
                break;
            case "maximizeProjectControl":
                this.appConfig.setGuiControlPanelShowProjectControl(true);
                this.createProjectControlPanel();
                break;
            case "maximizeSimControl":
                this.appConfig.setGuiControlPanelExpandSimulation(true);
                this.createSimulationPanel();
                break;
            case "minimizeSimControl":
                this.appConfig.setGuiControlPanelExpandSimulation(false);
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
        this.parent = p;
        this.setBorder(BorderFactory.createRaisedBevelBorder());
        this.setLayout(new BorderLayout());

        MyScrollPane msp = new MyScrollPane();
        JScrollPane scrollPane = new JScrollPane(msp);
        scrollPane.setBorder(null);

        this.setMaximumSize(new Dimension(this.controlPanelWidth, 2000));
        this.setMinimumSize(new Dimension(this.controlPanelWidth, 2000));

        this.add(BorderLayout.CENTER, scrollPane);
        this.setVisible(true);
    }

    @Override
    public void setRoundsPerformed(double time, long eventNumber) {
        this.timePerformed.setText(String.valueOf(this.round(time, 4)));
        this.roundsPerformed.setText(String.valueOf(eventNumber));
    }

    @Override
    public void setRoundsPerformed(long i) {
        this.timePerformed.setText(String.valueOf(i));
    }

    @Override
    public void setCurrentEvent(Event e) {
        this.setStringsForCurrentEvent(e);
        String[] v = {currentEventString};
        this.eventJList.setListData(v);

        this.composeEventList();
        this.eventList.setListData(this.queueElements);
        // remove the focus from the list, for cases when the wrong one is installed
        // (which happens if one presses the ESC button)
        this.eventList.setCellRenderer(this.nCNBCR);
    }

    @Override
    public void eventQueueChanged() {
        if (!Global.isRunning()) {
            this.composeEventList();
            this.eventList.setListData(this.queueElements);
            // remove the focus from the list, for cases when the wrong one is installed
            // (which happens if one presses the ESC button)
            this.eventList.setCellRenderer(this.nCNBCR);
        }
    }

    private void composeEventList() {
        Iterator<Event> eventIter = SinalgoRuntime.eventQueue.iterator();
        for (EventQueueElement queueElement : this.queueElements) {
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
        private Event event = null;

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
                        if (MaximizedControlPanel.this.queueElements[index] != null && MaximizedControlPanel.this.queueElements[index].toString() != null) {
                            EventQueueElement selElem = MaximizedControlPanel.this.queueElements[index];
                            EventQueueList.this.setSelectedIndex(index);
                            EventQueueList.this.setCellRenderer(MaximizedControlPanel.this.dLCR); // mark the element
                            JOptionPane.showMessageDialog(null, selElem.toString() + "\n" + selElem.getToolTipText(),
                                    "Information about an Event", JOptionPane.INFORMATION_MESSAGE);
                            EventQueueList.this.setCellRenderer(MaximizedControlPanel.this.nCNBCR); // unmark it
                        }
                    }
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        int index = EventQueueList.this.locationToIndex(e.getPoint());
                        if (index >= 0 && MaximizedControlPanel.this.queueElements[index] != null && MaximizedControlPanel.this.queueElements[index].getEvent() != null) {
                            Event event = MaximizedControlPanel.this.queueElements[index].getEvent();
                            EventQueueList.this.setSelectedIndex(index);
                            EventQueueList.this.setCellRenderer(MaximizedControlPanel.this.dLCR); // mark the element
                            EventPopupMenu epm = new EventPopupMenu(event, EventQueueList.this, MaximizedControlPanel.this.nCNBCR);
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
