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
package sinalgo.gui;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.configuration.AppConfig;
import sinalgo.configuration.Configuration;
import sinalgo.exception.ExportException;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.gui.controlPanel.ControlPanel;
import sinalgo.gui.controlPanel.MaximizedControlPanel;
import sinalgo.gui.controlPanel.MinimizedControlPanel;
import sinalgo.gui.dialogs.AboutDialog;
import sinalgo.gui.dialogs.GenerateNodesDialog;
import sinalgo.gui.dialogs.GlobalSettingsDialog;
import sinalgo.gui.dialogs.GraphInfoDialog;
import sinalgo.gui.dialogs.GraphPreferencesDialog;
import sinalgo.gui.dialogs.HelpDialog;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.io.eps.Exporter;
import sinalgo.nodes.Position;
import sinalgo.runtime.AbstractCustomGlobal;
import sinalgo.runtime.AbstractCustomGlobal.GlobalMethod;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.runtime.SinalgoRuntime;
import sinalgo.runtime.events.Event;
import sinalgo.tools.storage.SortableVector;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Vector;

/**
 * The parentGUI frame for the whole gui. It contains two children: the graph panel
 * and the control panel.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class GUI extends JFrame implements ActionListener {

    private static final long serialVersionUID = -2301103668898732398L;

    private Font menuFont;
    private JMenu graphMenu;
    // private JMenuItem load;
    // private JMenuItem save;
    private JMenuItem exportMenuItem;
    private JMenuItem clearMenuItem;
    private JMenuItem reevaluateMenuItem;
    private JMenuItem generateMenuItem;
    private JMenuItem infoMenuItem;
    private JMenuItem preferencesMenuItem;
    private JMenuItem exitMenuItem;
    private JMenu globalMenu;
    private JMenu helpMenu;
    private JMenu viewMenu;
    private JMenuItem aboutMenuItem = new JMenuItem("About Sinalgo");
    private JMenuItem settingsMenuItem = new JMenuItem("Settings");
    private JMenuItem helpMenuItem = new JMenuItem("Help");
    private JMenuItem viewFullScreenMenuItem = new JMenuItem("Full Screen");
    private JMenuItem viewZoomInMenuItem = new JMenuItem("Zoom In");
    private JMenuItem viewZoomOutMenuItem = new JMenuItem("Zoom Out");
    private JMenuItem viewZoomFitMenuItem = new JMenuItem("Zoom To Fit");

    private GlobalInvoker globalInvoker = new GlobalInvoker();

    /**
     * @return The graph panel where the graph is drawn onto
     */
    @Getter
    private GraphPanel graphPanel;

    /**
     * The control panel of this GUI.
     *
     * @return The controlpanel of this GUI.
     */
    @Getter
    private ControlPanel controlPanel;

    private HashMap<MenuElement, Method> methodsAndNames = new HashMap<>();

    /**
     * The constructor for the GUI class.
     *
     * @param r The runtime instance for which the gui was created.
     */
    public GUI(SinalgoRuntime r) {
        super(Global.isUseProject() ? (Configuration.getAppName() + " - " + Global.getProjectName()) : (Configuration.getAppName()));
        GuiHelper.setWindowIcon(this);

        // load the buttons for the menu - these settings should be done only once
        this.getSettingsMenuItem().addActionListener(this);
        this.getSettingsMenuItem().setMnemonic(java.awt.event.KeyEvent.VK_S);
        this.getAboutMenuItem().addActionListener(this);
        this.getAboutMenuItem().setMnemonic(java.awt.event.KeyEvent.VK_A);
        this.getAboutMenuItem().setIcon(GuiHelper.getIcon("sinalgo_21.png"));
        this.getHelpMenuItem().addActionListener(this);
        this.getHelpMenuItem().setMnemonic(java.awt.event.KeyEvent.VK_H);
        this.getHelpMenuItem().setIcon(GuiHelper.getIcon("helpSmall.gif"));
        this.getHelpMenuItem().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));

        this.getViewFullScreenMenuItem().addActionListener(this);
        this.getViewFullScreenMenuItem().setMnemonic(java.awt.event.KeyEvent.VK_F);
        this.getViewFullScreenMenuItem().setIcon(GuiHelper.getIcon("zoomFullView.gif"));
        this.getViewFullScreenMenuItem().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));

        this.getViewZoomInMenuItem().addActionListener(this);
        this.getViewZoomInMenuItem().setMnemonic(java.awt.event.KeyEvent.VK_I);
        this.getViewZoomInMenuItem().setIcon(GuiHelper.getIcon("zoominimage.png"));

        this.getViewZoomOutMenuItem().addActionListener(this);
        this.getViewZoomOutMenuItem().setMnemonic(java.awt.event.KeyEvent.VK_O);
        this.getViewZoomOutMenuItem().setIcon(GuiHelper.getIcon("zoomoutimage.png"));

        this.getViewZoomFitMenuItem().addActionListener(this);
        this.getViewZoomFitMenuItem().setMnemonic(java.awt.event.KeyEvent.VK_T);
        this.getViewZoomFitMenuItem().setIcon(GuiHelper.getIcon("zoomtofit.gif"));
        this.getViewZoomFitMenuItem().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));

        this.setRuntime(r);
    }

    private GenerateNodesDialog genNodesDialog = new GenerateNodesDialog(this);

    /**
     * The instance of the runtime to make changes comming from the gui.
     */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private SinalgoRuntime runtime;

    // The zoom level used to draw the graph.
    /**
     * @return The zoom factor currently used to draw the graph.
     */
    @Getter
    private double zoomFactor = 1;

    /**
     * Sets the zoom factor at which the graph will be drawn and repaints the graph.
     *
     * @param zoom The new zoom factor.
     */
    public void setZoomFactor(double zoom) {
        this.setZoomFactorNoRepaint(zoom);
        this.redrawGUI(); // should be sufficient...
        // redrawGUINow();
    }

    /**
     * Sets the zoom factor at which the graph will be drawn, but does not repaint
     * the graph.
     *
     * @param zoom The new zoom factor.
     */
    public void setZoomFactorNoRepaint(double zoom) {
        this.zoomFactor = Math.max(zoom, Configuration.getMinZoomFactor()); // we have open-end zooming ;-)

        this.runtime.getTransformator().changeZoomFactor(this.zoomFactor);
    }

    /**
     * Increase the zoom factor by the factor specified in the config file and
     * redraw the graph.
     */
    public void zoomIn() {
        double newFactor = this.zoomFactor * Configuration.getZoomStep();
        this.setZoomFactor(newFactor);
    }

    /**
     * Multiply the zoom factor by a given number and redraw the graph.
     *
     * @param multiplicativeFactor The factor to multiply the zoom factor with.
     */
    public void zoom(double multiplicativeFactor) {
        this.setZoomFactor(this.zoomFactor * multiplicativeFactor);
    }

    /**
     * Decrease the zoom factor by the factor specified in the config file and
     * redraw the graph.
     */
    public void zoomOut() {
        double newFactor = Math.max(this.zoomFactor / Configuration.getZoomStep(), 0.01);
        this.setZoomFactor(newFactor);
    }

    /**
     * Toggle between full screen and normal view.
     */
    synchronized public void toggleFullScreen() {
        if (Global.isRunning()) {
            return;
        }
        boolean full = !this.isUndecorated();
        this.dispose();
        this.setUndecorated(full); // window must be disposed prior to calling this method
        // setResizable(!full);
        this.setVisible(true);
        if (full) {
            // the following line seems to cause problems with transparent images under
            // windows
            // at least sometimes on my machine, but not on a notebook...
            // under windows, we could use the commented commands, but this seems not to
            // work under linux
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
            // setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            // restore the normal window size
            // setExtendedState(JFrame.NORMAL);
            // GUI.this.setSize(AppConfig.getAppConfig().guiWindowWidth, AppConfig.getAppConfig().guiWindowHeight);
            // GUI.this.setLocation(AppConfig.getAppConfig().guiWindowPosX, AppConfig.getAppConfig().guiWindowPosY);
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(null);
        }
        // update the menu title
        this.getViewFullScreenMenuItem().setText(full ? "Exit Full Screen" : "Full Screen");

    }

    /**
     * This method places GUI elements in the Frame and initializes all its
     * children.
     */
    public void init() {
        WindowAdapter listener = new WindowAdapter() {

            // Catch the close events
            @Override
            public void windowClosing(WindowEvent event) {
                Main.exitApplication();
            }
        };
        this.addWindowListener(listener);
        this.addWindowStateListener(listener);

        // react upon resize-events
        this.addComponentListener(new ComponentListener() {

            int oldX = AppConfig.getAppConfig().getGuiWindowPosX(), oldY = AppConfig.getAppConfig().getGuiWindowPosY();

            @Override
            public void componentResized(ComponentEvent e) {
                if (GUI.this.getExtendedState() == Frame.MAXIMIZED_BOTH) {
                    AppConfig.getAppConfig().setGuiIsMaximized(true);
                    AppConfig.getAppConfig().setGuiWindowPosX(this.oldX);
                    AppConfig.getAppConfig().setGuiWindowPosY(this.oldY);
                } else {
                    AppConfig.getAppConfig().setGuiIsMaximized(false);
                    AppConfig.getAppConfig().setGuiWindowWidth(GUI.this.getWidth());
                    AppConfig.getAppConfig().setGuiWindowHeight(GUI.this.getHeight());
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                // upon maximizing, first the component is moved, then resized. We only catch
                // the resize event
                this.oldX = AppConfig.getAppConfig().getGuiWindowPosX();
                this.oldY = AppConfig.getAppConfig().getGuiWindowPosY();
                AppConfig.getAppConfig().setGuiWindowPosX(GUI.this.getX());
                AppConfig.getAppConfig().setGuiWindowPosY(GUI.this.getY());
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });

        // -----------------------------------------------------
        // Global Key Input Listener
        // -----------------------------------------------------
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventPostProcessor(e -> {
            // -----------------------------------------------------
            // ENTER starts / stops the simulation
            // -----------------------------------------------------
            if (!e.isConsumed() && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ENTER) {
                // Note: the event should not be marked consumed. A consumed Enter may
                // have been used elsewhere, e.g. to select a menu.
                if (Global.isRunning()) {
                    this.getControlPanel().stopSimulation();
                } else {
                    this.getControlPanel().startSimulation();
                }
                return true; // no further event handling for this key event
            }
            return false;
        });

        this.setResizable(true);
        if (AppConfig.getAppConfig().isGuiIsMaximized()) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
        this.setSize(new Dimension(AppConfig.getAppConfig().getGuiWindowWidth(), AppConfig.getAppConfig().getGuiWindowHeight()));
        this.setLocation(AppConfig.getAppConfig().getGuiWindowPosX(), AppConfig.getAppConfig().getGuiWindowPosY());

        JMenuBar menuBar = new JMenuBar();
        this.menuFont = menuBar.getFont().deriveFont(Font.PLAIN);
        this.setGraphMenu(new JMenu("Simulation"));
        this.getGraphMenu().setMnemonic(KeyEvent.VK_S);

        this.setExportMenuItem(new JMenuItem("Export..."));
        this.getExportMenuItem().addActionListener(this);
        this.getExportMenuItem().setMnemonic(KeyEvent.VK_E);
        this.getExportMenuItem().setIcon(GuiHelper.getIcon("export.gif"));

        this.setClearMenuItem(new JMenuItem("Clear Graph"));
        this.getClearMenuItem().addActionListener(this);
        this.getClearMenuItem().setMnemonic(KeyEvent.VK_C);
        this.getClearMenuItem().setIcon(GuiHelper.getIcon("cleargraph.gif"));
        this.getClearMenuItem().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));

        this.setReevaluateMenuItem(new JMenuItem("Reevaluate Connections"));
        this.getReevaluateMenuItem().addActionListener(this);
        this.getReevaluateMenuItem().setMnemonic(KeyEvent.VK_R);
        this.getReevaluateMenuItem().setIcon(GuiHelper.getIcon("connectnodes.gif"));
        this.getReevaluateMenuItem().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));

        this.setGenerateMenuItem(new JMenuItem("Generate Nodes"));
        this.getGenerateMenuItem().addActionListener(this);
        this.getGenerateMenuItem().setMnemonic(KeyEvent.VK_G);
        this.getGenerateMenuItem().setIcon(GuiHelper.getIcon("addnodes.gif"));
        this.getGenerateMenuItem().setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));

        this.setInfoMenuItem(new JMenuItem("Network Info"));
        this.getInfoMenuItem().addActionListener(this);
        this.getInfoMenuItem().setMnemonic(KeyEvent.VK_I);

        this.setExitMenuItem(new JMenuItem("Exit"));
        this.getExitMenuItem().addActionListener(this);
        this.getExitMenuItem().setMnemonic(KeyEvent.VK_X);

        this.setPreferencesMenuItem(new JMenuItem("Preferences"));
        this.getPreferencesMenuItem().addActionListener(this);
        this.getPreferencesMenuItem().setMnemonic(KeyEvent.VK_P);

        this.getGraphMenu().add(this.getGenerateMenuItem());
        this.getGraphMenu().add(this.getReevaluateMenuItem());
        this.getGraphMenu().add(this.getClearMenuItem());

        this.getGraphMenu().addSeparator();
        this.getGraphMenu().add(this.getInfoMenuItem());
        this.getGraphMenu().add(this.getExportMenuItem());
        this.getGraphMenu().add(this.getPreferencesMenuItem());
        this.getGraphMenu().addSeparator();
        this.getGraphMenu().add(this.getExitMenuItem());

        menuBar.add(this.getGraphMenu());

        this.setGlobalMenu(new JMenu("Global"));
        this.getGlobalMenu().setMnemonic(KeyEvent.VK_G);

        // Compose this menu every time when it is shown. This allows us to
        // give some more control to the user about the CustomMethods.
        this.getGlobalMenu().addMenuListener(new MenuListener() {

            @Override
            public void menuCanceled(MenuEvent e) {
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            /**
             * Parse a set of methods and include in the 'Global' menu the methods annotated
             * by {@link GlobalMethod}. For the methods from the project specific
             * CustomGlobal file, the project may revoke or modify the menu name.
             *
             * @param isProjectSpecific
             *            True if the list of methods belongs to the project specific
             *            CustomGlobal file.
             * @return True if at least one entry was added.
             */
            private boolean testMethods(Method[] methods, boolean isProjectSpecific) {
                boolean hasEntry = false;
                Vector<JMenu> subMenus = new Vector<>();

                // sort the methods according to the order-flag given in the annotation
                SortableVector<Method> mlist = new SortableVector<>();
                for (Method m : methods) {
                    AbstractCustomGlobal.GlobalMethod info = m.getAnnotation(AbstractCustomGlobal.GlobalMethod.class);
                    if (info != null) {
                        mlist.add(m);
                    }
                }
                mlist.sort((o1, o2) -> {
                    GlobalMethod info1 = o1
                            .getAnnotation(GlobalMethod.class);
                    GlobalMethod info2 = o2
                            .getAnnotation(GlobalMethod.class);
                    if (info1 != null && info2 != null) {
                        int i1 = info1.order();
                        int i2 = info2.order();
                        return Integer.compare(i1, i2);
                    }
                    return 0; // should not happen, if it does, ordering may be wrong
                });

                for (Method method : mlist) {
                    AbstractCustomGlobal.GlobalMethod info = method
                            .getAnnotation(AbstractCustomGlobal.GlobalMethod.class);
                    if (info != null) {
                        if (!isProjectSpecific) {
                            if (!Modifier.isStatic(method.getModifiers())) {
                                Main.warning("The method '" + method.getName()
                                        + "' in sinalgo.runtime.Global cannot be called from the dropdown menu, as it needs to be static.\nThe method is not added to the menu.");
                                continue;
                            }
                        }
                        if (method.getParameterTypes().length != 0) {
                            if (isProjectSpecific) {
                                Main.warning("The method '" + method.getName()
                                        + "' from the projects CustomGlobal class cannot be called from the dropdown menu, as it needs parameters to be called. \nThe method is not added to the menu.");
                            } else {
                                Main.warning("The method '" + method.getName()
                                        + "' in sinalgo.runtime.Global cannot be called from the dropdown menu, as it needs parameters to be called.\nThe method is not added to the menu.");
                            }
                            continue;
                        }
                        String text = isProjectSpecific
                                ? Global.getCustomGlobal().includeGlobalMethodInMenu(method, info.menuText())
                                : info.menuText();
                        if (text == null) {
                            continue; // the method was dropped by the project
                        }
                        JMenuItem item = new JMenuItem(text);
                        item.addActionListener(GUI.this.getGlobalInvoker());
                        GUI.this.getMethodsAndNames().put(item, method);

                        String subMenuText = info.subMenu();
                        if (subMenuText.equals("")) {
                            GUI.this.getGlobalMenu().add(item);
                        } else {
                            JMenu menu = null;
                            for (JMenu m : subMenus) {
                                if (m.getText().equals(subMenuText)) {
                                    menu = m;
                                    break;
                                }
                            }
                            if (menu == null) {
                                menu = new JMenu(subMenuText);
                                subMenus.add(menu);
                                GUI.this.getGlobalMenu().add(menu);
                            }
                            menu.add(item);
                        }
                        hasEntry = true;
                    }
                }
                return hasEntry;
            }

            @Override
            public void menuSelected(MenuEvent event) {
                GUI.this.getGlobalMenu().removeAll();

                // add the project specific methods
                Method[] methods = Global.getCustomGlobal().getClass().getMethods();
                if (this.testMethods(methods, true)) {
                    GUI.this.getGlobalMenu().addSeparator();
                }

                // add the framework-side defined methods in sinalgo.runtime.Global
                try {
                    methods = Thread.currentThread().getContextClassLoader().loadClass("sinalgo.runtime.Global").getMethods();
                    if (this.testMethods(methods, false)) {
                        GUI.this.getGlobalMenu().addSeparator();
                    }
                } catch (ClassNotFoundException e) {
                    throw new SinalgoFatalException("Could not find class sinalgo.runtime.Global to get the global gui methods from.");
                }

                // And finally the Settings and About dialog
                GUI.this.getGlobalMenu().add(GUI.this.getSettingsMenuItem());

                // and set the font of the menu entries
                GUI.this.setMenuFont(GUI.this.getGlobalMenu());
            }
        });

        menuBar.add(this.getGlobalMenu());

        // ---------------------------------------------
        // View Menu
        // ---------------------------------------------
        this.setViewMenu(new JMenu("View"));
        this.getViewMenu().setMnemonic(KeyEvent.VK_V);
        this.getViewMenu().add(this.getViewZoomOutMenuItem());
        this.getViewMenu().add(this.getViewZoomInMenuItem());
        this.getViewMenu().add(this.getViewZoomFitMenuItem());
        this.getViewMenu().add(this.getViewFullScreenMenuItem());

        menuBar.add(this.getViewMenu());

        // ---------------------------------------------
        // Help Menu
        // ---------------------------------------------
        this.setHelpMenu(new JMenu("Help"));
        this.getHelpMenu().setMnemonic(KeyEvent.VK_H);
        this.getHelpMenu().add(this.getHelpMenuItem());
        this.getHelpMenu().addSeparator();
        this.getHelpMenu().add(this.getAboutMenuItem());
        menuBar.add(this.getHelpMenu());

        this.setMenuFont(menuBar);

        this.setJMenuBar(menuBar);

        // The content pane
        this.setGuiPanel(new JPanel());
        this.getGuiPanel().setLayout(new BoxLayout(this.getGuiPanel(), BoxLayout.X_AXIS));

        this.setGraphPanel(new GraphPanel(this));
        // activate the Tooltip for the graphPanel. The "Default Tooltip" is actually
        // never shown
        // because the text to show is overwritten in the GraphPanel-Classes
        // getToolTipText(MouseEvent e)
        this.getGraphPanel().createToolTip();
        this.getGraphPanel().setToolTipText("Default Tooltip"); // to initialize, must set an arbitrary text
        this.getGraphPanel().requestDefaultViewOnNextDraw();

        if (Configuration.isExtendedControl()) {
            this.setControlPanel(new MaximizedControlPanel(this));
            this.getGuiPanel().add(this.getGraphPanel());
            this.getGuiPanel().add(this.getControlPanel());
        } else {
            this.setControlPanel(new MinimizedControlPanel(this));
            this.getGuiPanel().add(this.getControlPanel());
            this.getGuiPanel().add(this.getGraphPanel());
        }

        this.add(this.getGuiPanel());

        this.setVisible(true);
        // trigger a first paint (needed!)
        this.repaint();
    }

    private void setMenuFont(MenuElement m) {
        m.getComponent().setFont(this.getMenuFont());
        if (m.getSubElements().length > 0) {
            for (MenuElement e : m.getSubElements()) {
                this.setMenuFont(e);
            }
        }
    }

    private JPanel guiPanel;

    /**
     * Switches between the two modes for the control panel depending on the boolean
     * parameter.
     *
     * @param toExtended if set true the control panel is set on Extended (
     */
    public void changePanel(boolean toExtended) {
        // STRANGE! we need to add the new contol panel before removing the old one
        // otherwise, the mouse scrolling wont be detected anymore.
        if (toExtended) { // from minimized to maximized
            this.getGuiPanel().setLayout(new BoxLayout(this.getGuiPanel(), BoxLayout.X_AXIS));
            ControlPanel oldCP = this.getControlPanel();
            this.setControlPanel(new MaximizedControlPanel(this));
            this.getGuiPanel().add(this.getControlPanel(), 2); // content pane must be after graph panel
            this.getGuiPanel().remove(oldCP);
        } else { // from maximized to minimized
            this.getGuiPanel().setLayout(new BoxLayout(this.getGuiPanel(), BoxLayout.Y_AXIS));
            ControlPanel oldCP = this.getControlPanel();
            this.setControlPanel(new MinimizedControlPanel(this));
            this.getGuiPanel().add(this.getControlPanel(), 0); // content pane is first in list
            this.getGuiPanel().remove(oldCP);
        }
        this.getGuiPanel().revalidate();
        this.getGraphPanel().requireFullDrawOnNextPaint();
        this.repaint();
    }

    /**
     * This method resets the gui for the current configuration settings. This
     * method is called when the user removes all nodes.
     */
    public void allNodesAreRemoved() {
        this.getGraphPanel().allNodesAreRemoved();
    }

    /**
     * A boolean indicating whether the paint() method of this gui has been called
     * at least once. THe graph panel starts drawing itself only after this flag has
     * been set to true.
     */
    @Getter
    @Setter
    private boolean firstTimePainted;

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        this.setFirstTimePainted(true);
        this.getControlPanel().repaint();
        this.getGraphPanel().repaint();
    }

    /**
     * Returns the transformation instance that knows how to translate between the
     * logic coordinate system used by the simulation and the corresponding
     * coordinates on the GUI.
     *
     * @return The transformation instance.
     */
    public PositionTransformation getTransformator() {
        return this.runtime.getTransformator();
    }

    /**
     * This method redraws the Graph panel and the control panel.
     * <p>
     * Call this method whenever you require the graph to be redrawn immediately
     * (synchronously) within the simulation code. Note that this method draws the
     * graph immediately. I.e. to avoid concurrent access to the datastructures,
     * this method should only be called when no simulation round is executing.
     * <p>
     * The paint process of the graph panel is as following: First, the graph
     * (nodes, edges, background, ...) is painted onto an image. Then, the content
     * of the image is copied to the screen. Quite often, it is not necessary to
     * redraw the image, and we can paint again the 'old' image from the previous
     * call to paint the control panel. This is for example the case when the mouse
     * moved over a node or edge and triggered a tool tip window to be shown. When
     * the graph or transformation matrix changed, the image needs to be repainted.
     * <p>
     * This method forces a redraw of the image, which may be quite expensive for
     * huge graphs. Thus, call it only, when you really need a synchronous repaint
     * of the image.
     * <p>
     * In almost all cases, calling redrawGUI() is preferred.
     */
    public void redrawGUINow() {
        this.getControlPanel().repaint();
        this.getGraphPanel().paintNow();
    }

    /**
     * Repaints the Control Panel and the Graph Panel. In contrast to redrawGUINow,
     * this method does not enforce the graph panel to be painted immediately, but
     * leaves it up to the JVM to schedule the repaint. Call this method whenever it
     * is not crucial to redraw the graph immediately.
     * <p>
     * <p>
     * The paint process of the graph panel is as following: First, the graph
     * (nodes, edges, background, ...) is painted onto an image. Then, the content
     * of the image is copied to the screen. Quite often, it is not necessary to
     * redraw the image, and we can paint again the 'old' image from the previous
     * call to paint the control panel. This is for example the case when the mouse
     * moved over a node or edge and triggered a tool tip window to be shown. When
     * the graph or transformation matrix changed, the image needs to be repainted.
     * <p>
     * This method forces a redraw of the image, which may be quite expensive for
     * huge graphs. Thus, call it only, when you really need the gui to be updated.
     *
     * @see GUI#redrawGUINow() for how the graph panel is drawn.
     */
    public void redrawGUI() {
        this.getGraphPanel().requireFullDrawOnNextPaint();
        this.getControlPanel().repaint();
        this.getGraphPanel().repaint();
    }

    /**
     * Only redraw the control panel. This is used to redraw the controlpanel
     * (inclusive the small preview picture) without painting the whole graph.
     */
    public void redrawControl() {
        this.getControlPanel().repaint();
    }

    /**
     * This method pops a Error message on the frame with the given message and the
     * given title.
     *
     * @param message The message to display.
     * @param title   The Title of the messageDialog.
     */
    public void popupErrorMessage(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * This method sets the start button of the control panel enabled according to
     * the boolean passed.
     *
     * @param b If b is true the start button is set enabled and the abort button
     *          is set false (and vice versa)
     */
    public void setStartButtonEnabled(boolean b) {
        this.getControlPanel().setStartButtonEnabled(b);
        this.getGraphMenu().setEnabled(b);
        this.getGlobalMenu().setEnabled(b);
        this.getHelpMenu().setEnabled(b);
        this.getViewMenu().setEnabled(b);
        // We could disallow resizing the window here, but this flickers
        // setResizable(b);
    }

    /**
     * Resets the time performed and the number of events already executed.
     *
     * @param time        The time that passed by until the actual moment.
     * @param eventNumber The number of events that have been executed until now.
     */
    public void setRoundsPerformed(double time, long eventNumber) {
        this.controlPanel.setRoundsPerformed(time, eventNumber);
    }

    /**
     * This method changes the number of rounds to be displayed in the control
     * panel.
     *
     * @param i The number to be displayed in the control panel.
     */
    public void setRoundsPerformed(long i) {
        this.getControlPanel().setRoundsPerformed(i);
    }

    /**
     * Sets the event that was processed last.
     *
     * @param e The event that was last processed, null if there was no event.
     */
    public void setCurrentlyProcessedEvent(Event e) {
        this.getControlPanel().setCurrentEvent(e);
    }

    /**
     * Sets the mouse-position of the cursor.
     *
     * @param s A string representation of the position
     */
    public void setMousePosition(String s) {
        this.getControlPanel().setMousePosition(s);
    }

    /**
     * Called when the user presses the button to remove all nodes from the
     * simulation.
     */
    public void clearAllNodes() {
        SinalgoRuntime.clearAllNodes();
    }

    /**
     * Opens a dialog that allows to add new nodes
     */
    public void addNodes() {
        this.getGenNodesDialog().compose(null);
    }

    /**
     * Opoens a dialog to specify the models to craete a node that is placed at the
     * specific position.
     *
     * @param pos The positino where the node will be placed.
     */
    public void addSingleNode(Position pos) {
        this.getGenNodesDialog().compose(pos);
    }

    /**
     * Creates a node with the default settings at the given position.
     *
     * @param pos The position
     */
    public void addSingleDefaultNode(Position pos) {
        this.getGenNodesDialog().generateDefaultNode(pos);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(this.getGenerateMenuItem().getActionCommand())) {
            this.addNodes();
        } else if (e.getActionCommand().equals(this.getClearMenuItem().getActionCommand())) {
            if (0 == JOptionPane.showConfirmDialog(this, "Do you really want to remove all nodes?", "Remove all nodes?",
                    JOptionPane.YES_NO_OPTION)) {
                this.clearAllNodes();
            }
        } else if (e.getActionCommand().equals(this.getPreferencesMenuItem().getActionCommand())) {
            new GraphPreferencesDialog(this);
        } else if (e.getActionCommand().equals(this.getReevaluateMenuItem().getActionCommand())) {
            if (0 == JOptionPane.showConfirmDialog(this,
                    "Do you really want to reevaluate the connections of all nodes?", "Reevaluate Connections?",
                    JOptionPane.YES_NO_OPTION)) {
                SinalgoRuntime.reevaluateConnections();
                this.redrawGUI();
            }
        } else if (e.getActionCommand().equals(this.getExportMenuItem().getActionCommand())) {
            try {
                new Exporter(this).export(new Rectangle(0, 0, this.getGraphPanel().getWidth(), this.getGraphPanel().getHeight()),
                        this.getTransformator());
            } catch (ExportException e1) {
                Main.minorError(e1.getMessage());
            }
        } else if (e.getActionCommand().equals(this.getInfoMenuItem().getActionCommand())) {
            new GraphInfoDialog(this);
        } else if (e.getActionCommand().equals(this.getSettingsMenuItem().getActionCommand())) {
            new GlobalSettingsDialog(this);
        } else if (e.getActionCommand().equals(this.getAboutMenuItem().getActionCommand())) {
            new AboutDialog(this);
        } else if (e.getActionCommand().equals(this.getHelpMenuItem().getActionCommand())) {
            HelpDialog.showHelp(this); // start in a new thread
        } else if (e.getActionCommand().equals(this.getExitMenuItem().getActionCommand())) {
            Main.exitApplication();
        } else if (e.getActionCommand().equals(this.getViewFullScreenMenuItem().getActionCommand())) {
            this.toggleFullScreen();
        } else if (e.getActionCommand().equals(this.getViewZoomInMenuItem().getActionCommand())) {
            this.zoomIn();
        } else if (e.getActionCommand().equals(this.getViewZoomOutMenuItem().getActionCommand())) {
            this.zoomOut();
        } else if (e.getActionCommand().equals(this.getViewZoomFitMenuItem().getActionCommand())) {
            this.getTransformator().zoomToFit(this.getGraphPanel().getWidth(), this.getGraphPanel().getHeight());
            this.setZoomFactor(this.getTransformator().getZoomFactor());
        }
    }

    // class used to invoke the global user-defined methods
    class GlobalInvoker implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            Method method = GUI.this.getMethodsAndNames().get(event.getSource());
            if (method == null) {
                throw new SinalgoFatalException("Cannot find method associated with menu item " + event.getActionCommand());
            }
            try {
                synchronized (GUI.this.getTransformator()) {
                    // synchronize it on the transformator to grant not to be concurrent with
                    // any drawing or modifying action
                    try {
                        method.invoke(Global.getCustomGlobal(), (Object[]) null);
                    } catch (IllegalArgumentException e) {
                        method.invoke(null, (Object[]) null);
                    }
                }
            } catch (IllegalArgumentException | SecurityException | InvocationTargetException | IllegalAccessException e) {
                Main.minorError(e);
            }
        }
    }
}
