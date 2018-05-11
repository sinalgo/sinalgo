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
import sinalgo.exception.SinalgoFatalException;
import sinalgo.gui.GUI;
import sinalgo.gui.multiLineTooltip.MultiLineToolTipJList;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.gui.transformation.Transformation3D;
import sinalgo.io.IOUtils;
import sinalgo.runtime.AbstractCustomGlobal;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.runtime.SinalgoRuntime;
import sinalgo.runtime.events.Event;
import sinalgo.tools.Tuple;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Vector;

/**
 * The Panel with the buttons to control the simulation. This is the panel on
 * the right hand side of the simulation. It is used to change settings about
 * the simulation, to let it run and to exit.
 */
@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
public abstract class ControlPanel extends JPanel implements ActionListener, MouseListener {

    private static final long serialVersionUID = 8395288187533107606L;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private static JTextField roundsToPerform = new JTextField(5); // number of rounds to perform

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private static JLabel roundsToPerformLabel = new JLabel();

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private static JTextField refreshRate = new JTextField(5);

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private static String currentEventString = "No event";

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private static String currentEventToolTip = "No event executed until now.";

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private static JButton start; // reused to keep the picture

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private static JTextArea textField = new JTextArea();

    static { // static initialization
        getRoundsToPerform().setText(String.valueOf(Configuration.getDefaultRoundNumber()));
        getRoundsToPerform().setEditable(AppConfig.getAppConfig().isGuiRunOperationIsLimited());
        getRoundsToPerformLabel().setEnabled(AppConfig.getAppConfig().isGuiRunOperationIsLimited());
    }

    /**
     * The background color of the control panel.
     */
    @Getter
    @Setter
    private Color bgColor = new Color(this.getBackground().getRed(), this.getBackground().getGreen(), this.getBackground().getBlue());

    private GUI parentGUI;

    private JTextField roundsPerformed = new JTextField(0);
    private JTextField timePerformed = new JTextField(0);
    private JTextField mousePositionField = new JTextField(8);
    private JPanel info = new JPanel();
    private MultiLineToolTipJList eventJList = new MultiLineToolTipJList();
    private JButton abort;
    private JButton runMenuButton;
    private JButton exit = new JButton("Exit");
    private ZoomPanel zoomPanel;

    // A list of all buttons that are disabled while a simulation runs
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private Vector<JButton> disabledButtonList = new Vector<>();

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private Vector<Tuple<JButton, Method>> customButtons = new Vector<>();

    public ControlPanel() {
        setStart(this.createFrameworkIconButton("Start", this.getRunButtonImageName(), "Run"));
    }

    public void addToDisabledButtonList(JButton b) {
        if (!this.getDisabledButtonList().contains(b)) {
            this.getDisabledButtonList().add(b);
        }
    }

    /**
     * Adds a button to a list of buttons that are only active when the simulation
     * is not running. Avoids duplicates.
     *
     * @param button The button to add
     */
    public void includeIdleOnlyButton(JButton button) {
        // if(buttonList)
    }

    /**
     * Creates a new icon button where the icon is supposed to be stored in the
     * framework
     *
     * @param actionCommand Name of the action that is performed when this button is pressed
     * @param imageName     The name of the image file, which is stored in the directory
     *                      specified by Configuration.sinalgoImageDir
     * @param toolTip       Tooltip text to be shown for this button
     * @return A new JButton with an icon
     */
    protected JButton createFrameworkIconButton(String actionCommand, String imageName, String toolTip) {
        // To support jar files, we cannot access the file directly
        ClassLoader cldr = Thread.currentThread().getContextClassLoader();
        String path = IOUtils.getAsPath(Configuration.getSinalgoImageDir(), imageName);
        try {
            URL url = cldr.getResource(path);
            ImageIcon icon = new ImageIcon(url);
            JButton b = new JButton(icon);
            b.setPreferredSize(new Dimension(29, 29));
            return this.finishButton(b, actionCommand, toolTip);
        } catch (NullPointerException e) {
            throw new SinalgoFatalException("Cannot access the application icon " + imageName
                    + ", which should be stored in\n" + path + ".");
        }
    }

    protected ImageIcon getFrameworkIcon(String imageName) {
        // To support jar files, we cannot access the file directly
        ClassLoader cldr = Thread.currentThread().getContextClassLoader();
        String path = IOUtils.getAsPath(Configuration.getSinalgoImageDir(), imageName);
        try {
            URL url = cldr.getResource(path);
            return new ImageIcon(url);
        } catch (NullPointerException e) {
            throw new SinalgoFatalException("Cannot access the application icon " + imageName
                    + ", which should be stored in\n" + path + ".");
        }
    }

    /**
     * Creates a new icon button, the icon is supposed to be stored in a folder
     * 'images' in the current user project.
     *
     * @param actionCommand Name of the action that is performed when this button is pressed
     * @param imageName     The name of the image file, which is stored in the directory
     *                      specified by Configuration.sinalgoImageDir
     * @param toolTip       Tooltip text to be shown for this button
     * @return A new JButton with an icon
     */
    protected JButton createCustomIconButton(String actionCommand, String imageName, String toolTip) {
        ClassLoader cldr = Thread.currentThread().getContextClassLoader();
        String path = IOUtils.getAsPath(Global.getProjectResourceDir(), "images", imageName);
        URL url = cldr.getResource(path);
        if (url == null) {
            throw new SinalgoFatalException("Cannot access the project specific icon " + imageName
                    + ", which should be stored in\n" + path + ".");
        }
        ImageIcon icon = new ImageIcon(url);
        JButton b = new JButton(icon);
        b.setPreferredSize(new Dimension(29, 29));
        return this.finishButton(b, actionCommand, toolTip);
    }

    protected JButton createTextButton(String actionCommand, String buttonText, String toolTip) {
        JButton b = new JButton(buttonText);
        b.setFont(b.getFont().deriveFont(Font.PLAIN));
        return this.finishButton(b, actionCommand, toolTip);
    }

    private JButton finishButton(JButton b, String actionCommand, String toolTip) {
        b.setActionCommand(actionCommand);
        b.setFocusable(false);
        b.setBorderPainted(false);
        b.setBorder(new SoftBevelBorder(BevelBorder.RAISED));
        b.setBackground(this.getBgColor());
        b.addActionListener(this);
        b.addMouseListener(this); // move over the button => draw border
        b.setToolTipText(toolTip);
        return b;
    }

    /**
     * Rotates a 3D graph such that the z-axis becomes a single point. Nothing
     * happens when called on a 2D graph.
     */
    public void defaultViewXY() {
        PositionTransformation pt = this.getParentGUI().getTransformator();
        if (pt instanceof Transformation3D) {
            ((Transformation3D) pt).defaultViewXY(this.getParentGUI().getGraphPanel().getWidth(),
                    this.getParentGUI().getGraphPanel().getHeight());
            this.getParentGUI().setZoomFactor(pt.getZoomFactor());
        }
    }

    /**
     * Rotates a 3D graph such that the y-axis becomes a single point. Nothing
     * happens when called on a 2D graph.
     */
    public void defaultViewXZ() {
        PositionTransformation pt = this.getParentGUI().getTransformator();
        if (pt instanceof Transformation3D) {
            ((Transformation3D) pt).defaultViewXZ(this.getParentGUI().getGraphPanel().getWidth(),
                    this.getParentGUI().getGraphPanel().getHeight());
            this.getParentGUI().setZoomFactor(pt.getZoomFactor());
        }
    }

    /**
     * Rotates a 3D graph such that the x-axis becomes a single point. Nothing
     * happens when called on a 2D graph.
     */
    public void defaultViewYZ() {
        PositionTransformation pt = this.getParentGUI().getTransformator();
        if (pt instanceof Transformation3D) {
            ((Transformation3D) pt).defaultViewYZ(this.getParentGUI().getGraphPanel().getWidth(),
                    this.getParentGUI().getGraphPanel().getHeight());
            this.getParentGUI().setZoomFactor(pt.getZoomFactor());
        }
    }

    /**
     * The Method to set the start-Button enabled or not. This method is
     * synchronized, because it is called out of the Thread and thus should only be
     * accessed once a time. This method aditionally guarantees, that only one (but
     * certanly one of them) button is set enabled.
     *
     * @param b The boolean to indicate if the Start-Button is set true or false.
     */
    public synchronized void setStartButtonEnabled(boolean b) {
        if (b) {
            this.getAbort().setBorderPainted(false);
        } else {
            getStart().setBorderPainted(false);
        }

        this.getAbort().setEnabled(!b);
        getStart().setEnabled(b);
        getRoundsToPerform().setEnabled(b);
        getRefreshRate().setEnabled(b);

        for (JButton button : this.getDisabledButtonList()) {
            button.setEnabled(b);
        }
    }

    /**
     * This Method returns the default Button of the Control Panel.
     *
     * @return The Default Button.
     */
    public JButton getDefaultButton() {
        return getStart();
    }

    /**
     * Appends some text to the output text field.
     *
     * @param s The text to append.
     */
    public void appendTextToOutput(String s) {
        getTextField().append(s);
        getTextField().setCaretPosition(getTextField().getText().length());
    }

    /**
     * @return A print stream that prints to the output text field
     */
    public TextOutputPrintStream getTextOutputPrintStream() {
        return new TextOutputPrintStream(new OutputStream() {

            @Override
            public void write(int b) {
                getTextField().append(Character.toString((char) b));
            }
        });
    }

    /**
     * Removes all text from the text field.
     */
    public void clearOutput() {
        getTextField().setText("");
    }

    /**
     * Set the current time and the event number.
     *
     * @param time        The current time.
     * @param eventNumber The number of events that have been executed until now.
     */
    public abstract void setRoundsPerformed(double time, long eventNumber);

    /**
     * This Method changes the number of rounds already performed. This number is
     * shown on the top of the control Panel.
     *
     * @param i The new Number of Steps already Performed.
     */
    public abstract void setRoundsPerformed(long i);

    /**
     * Sets the event that was executed last.
     *
     * @param e The event that was last processed, null if there was no event.
     */
    public abstract void setCurrentEvent(Event e);

    /**
     * Sets the current mouse position
     *
     * @param s A string representation of the position
     */
    public void setMousePosition(String s) {
        this.getMousePositionField().setText(s);
    }

    /**
     * Starts the simulation by first reading from the input fields the refresh rate
     * and the number of rounds to perform.
     */
    public void startSimulation() {
        try {
            int rr = Integer.parseInt(getRefreshRate().getText());
            if (rr <= 0) {
                Main.minorError("Invalid input: '" + getRefreshRate().getText()
                        + "' is not a positive integer.\nThe refresh rate has to be a positive integer.");
                return;
            }
            Configuration.setRefreshRate(rr);
        } catch (java.lang.NumberFormatException nFE) {
            Main.minorError("Invalid input: '" + getRefreshRate().getText() + "' is not a valid integer.");
            return;
        }
        try {
            int rounds = Integer.parseInt(getRoundsToPerform().getText());
            if (rounds <= 0) {
                Main.minorError("Invalid input: '" + getRoundsToPerform().getText()
                        + "' is not a positive integer.\nThe number of rounds has to be a positive integer.");
                return;
            }
            this.getParentGUI().setStartButtonEnabled(false);
            this.getParentGUI().getRuntime().run(rounds, true);
        } catch (java.lang.NumberFormatException nFE) {
            Main.minorError("Invalid input: '" + getRoundsToPerform().getText() + "' is not a valid integer.");
        }
    }

    /**
     * Stops a running simulation.
     */
    public void stopSimulation() {
        this.getParentGUI().getRuntime().abort();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(this.getExit().getActionCommand())) {
            Main.exitApplication();
        } else if (e.getActionCommand().equals(getStart().getActionCommand())) {
            this.startSimulation();
        } else if (e.getActionCommand().equals(this.getAbort().getActionCommand())) {
            this.stopSimulation();
        } else if (e.getActionCommand().equals(this.getRunMenuButton().getActionCommand())) {
            // Show the menu containing the run options
            RunPopupMenu rp = new RunPopupMenu();
            Point p = this.getRunMenuButton().getLocationOnScreen();
            Point guiP = this.getLocationOnScreen();
            rp.show(this, p.x - guiP.x - 29, p.y - guiP.y + 29);
        } else if (e.getActionCommand().equals("zoomIn")) {
            this.getParentGUI().zoomIn();
        } else if (e.getActionCommand().equals("zoomOut")) {
            this.getParentGUI().zoomOut();
        } else if (e.getActionCommand().equals("zoomToFit")) {
            this.getParentGUI().getTransformator().zoomToFit(this.getParentGUI().getGraphPanel().getWidth(), this.getParentGUI().getGraphPanel().getHeight());
            this.getParentGUI().setZoomFactor(this.getParentGUI().getTransformator().getZoomFactor());
        } else if (e.getActionCommand().equals("zoomToFit3D")) {
            this.getParentGUI().getTransformator().defaultView(this.getParentGUI().getGraphPanel().getWidth(),
                    this.getParentGUI().getGraphPanel().getHeight());
            this.getParentGUI().setZoomFactor(this.getParentGUI().getTransformator().getZoomFactor());
        } else if (e.getActionCommand().equals("extendPanel")) {
            this.getParentGUI().changePanel(true);
        } else if (e.getActionCommand().equals("minimizedPanel")) {
            this.getParentGUI().changePanel(false);
        } else if (e.getActionCommand().equals("clearGraph")) {
            this.getParentGUI().clearAllNodes();
        } else if (e.getActionCommand().equals("addNodes")) {
            this.getParentGUI().addNodes();
        } else if (e.getActionCommand().equals("connectNodes")) {
            SinalgoRuntime.reevaluateConnections(); // could ask...
            this.getParentGUI().redrawGUI();
        } else {
            // test whether its a custom button
            for (Tuple<JButton, Method> t : this.getCustomButtons()) {
                if (t.getFirst() == e.getSource()) {
                    try {
                        synchronized (this.getParentGUI().getTransformator()) {
                            // synchronize it on the transformator to grant not to be concurrent with
                            // any drawing or modifying action
                            t.getSecond().invoke(Global.getCustomGlobal());
                        }
                    } catch (IllegalArgumentException | IllegalAccessException e1) {
                        throw new SinalgoFatalException("Error while invoking custom method, triggered through button:\n"
                                + e1.getMessage() + "\n\n" + e1);
                    } catch (InvocationTargetException e1) {
                        if (e1.getCause() != null) {
                            Main.minorError("Exception thrown while executing '" + t.getSecond().getName() + "'.\n"
                                    + e1.getCause().getMessage() + "\n\n" + e1.getCause());
                        } else {
                            throw new SinalgoFatalException("Exception thrown while executing '" + t.getSecond().getName() + "'.\n"
                                    + e1.getMessage() + "\n\n" + e1);
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a set of custom buttons defined in the CustomGlobal of the current
     * project.
     *
     * @return A vector of the buttons, which should not be modified.
     */
    protected Vector<JButton> createCustomButtons() {
        Vector<JButton> buttons = new Vector<>();
        Method[] f = Global.getCustomGlobal().getClass().getMethods();
        for (Method aF : f) {
            AbstractCustomGlobal.CustomButton info = aF.getAnnotation(AbstractCustomGlobal.CustomButton.class);
            if (info != null) {
                Class<?>[] params = aF.getParameterTypes();
                if (params.length != 0) { // we only accept methods with no parameters
                    continue;
                }
                String command = "GLOBAL_BUTTON_" + aF.getName();
                JButton b;
                if (!info.imageName().equals("")) {
                    b = this.createCustomIconButton(command, info.imageName(), info.toolTipText());
                } else {
                    b = this.createTextButton(command, info.buttonText(), info.toolTipText());
                }
                buttons.add(b);
                this.getCustomButtons().add(new Tuple<>(b, aF));
            }
        }
        return buttons;
    }

    /**
     * Set text (and tooltip-text) to be displayed for the event that executed last.
     *
     * @param e The current event, null if there is no event.
     */
    protected void setStringsForCurrentEvent(Event e) {
        if (e != null) {
            setCurrentEventString(e.getEventListText(true));
            setCurrentEventToolTip(e.getEventListToolTipText(true));
        } else {
            setCurrentEventString("No event");
            setCurrentEventToolTip("No event executed");
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    protected double round(double value, int places) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, BigDecimal.ROUND_HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (e.getSource() instanceof JButton) {
            if (((JButton) e.getSource()).isEnabled()) {
                ((JButton) e.getSource()).setBorderPainted(true);
            }
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (e.getSource() instanceof JButton) {
            ((JButton) e.getSource()).setBorderPainted(false);
        }
    }

    /**
     * Called whenever the type of RUN-operation is changed.
     *
     * @param isLimited True if the RUN operation should stop after the indicated # of
     *                  nodes, false if the RUN operation should perform as many
     *                  steps/events as possible.
     */
    public void setRunType(boolean isLimited) {
        getRoundsToPerform().setEditable(isLimited);
        getRoundsToPerformLabel().setEnabled(isLimited);
        AppConfig.getAppConfig().setGuiRunOperationIsLimited(isLimited);
        getStart().setIcon(this.getFrameworkIcon(this.getRunButtonImageName()));
    }

    // -----------------------------------------------------------------
    // Code for the RUN button
    // -----------------------------------------------------------------

    /**
     * @return The name of the Icon to use for the run button, according to the
     * current settings.
     */
    public String getRunButtonImageName() {
        if (Configuration.isHandleEmptyEventQueue() && Configuration.isAsynchronousMode()) {
            if (AppConfig.getAppConfig().isGuiRunOperationIsLimited()) {
                return "refillrun.gif";
            } else {
                return "refillrunforever.gif";
            }
        } else {
            if (AppConfig.getAppConfig().isGuiRunOperationIsLimited()) {
                return "run.gif";
            } else {
                return "runforever.gif";
            }
        }
    }

    /**
     * A simple output streamer for the output text field.
     */
    public class TextOutputPrintStream extends PrintStream {

        public TextOutputPrintStream(OutputStream out) {
            super(out);
        }

        public void setCaretPosition() {
            getTextField().setCaretPosition(getTextField().getText().length());
        }

        @Override
        public void println(String s) {
            getTextField().append(s);
            getTextField().append("\n");
            getTextField().setCaretPosition(getTextField().getText().length());
        }

        @Override
        public void print(String s) {
            getTextField().append(s);
            getTextField().setCaretPosition(getTextField().getText().length());
        }
    }

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    public class RunPopupMenu extends JPopupMenu implements ActionListener {

        private static final long serialVersionUID = 7390719200202452018L;

        private JMenuItem runForever = new JMenuItem("Run Forever", ControlPanel.this.getFrameworkIcon("runforever.gif"));
        private JMenuItem runCount = new JMenuItem(
                "Run Specified # of " + (Global.isAsynchronousMode() ? "Events" : "Rounds"), ControlPanel.this.getFrameworkIcon("run.gif"));
        private JCheckBoxMenuItem refillEventQueueMenuItem = new JCheckBoxMenuItem("Refill Event Queue",
                Configuration.isHandleEmptyEventQueue());

        protected RunPopupMenu() {
            // if(appConfig.guiRunOperationIsLimited) {
            this.add(this.getRunForever());
            // } else {
            this.add(this.getRunCount());
            // }
            if (Configuration.isAsynchronousMode()) {
                this.addSeparator();
                this.add(this.getRefillEventQueueMenuItem());
            }
            this.getRunForever().addActionListener(this);
            this.getRunCount().addActionListener(this);
            this.getRefillEventQueueMenuItem().addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals(this.getRunForever().getActionCommand())) {
                ControlPanel.this.setRunType(false);
            } else if (e.getActionCommand().equals(this.getRunCount().getActionCommand())) {
                ControlPanel.this.setRunType(true);
            } else if (e.getActionCommand().equals(this.getRefillEventQueueMenuItem().getActionCommand())) {
                Configuration.setHandleEmptyEventQueue(this.getRefillEventQueueMenuItem().isSelected());
                getStart().setIcon(ControlPanel.this.getFrameworkIcon(ControlPanel.this.getRunButtonImageName()));
            }
        }
    }


}
