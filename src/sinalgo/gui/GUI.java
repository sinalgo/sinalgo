/*
 Copyright (c) 2007, Distributed Computing Group (DCG)
                    ETH Zurich
                    Switzerland
                    dcg.ethz.ch

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the
   distribution.

 - Neither the name 'Sinalgo' nor the names of its contributors may be
   used to endorse or promote products derived from this software
   without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package sinalgo.gui;


import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.AppConfig;
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
import sinalgo.io.eps.ExportException;
import sinalgo.io.eps.Exporter;
import sinalgo.nodes.Position;
import sinalgo.runtime.AbstractCustomGlobal;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.runtime.Runtime;
import sinalgo.runtime.events.Event;
import sinalgo.tools.storage.SortableVector;


/**
 * The parent frame for the whole gui. It contains two children: the graph panel and the control panel.
 */
@SuppressWarnings("serial")
public class GUI extends JFrame implements ActionListener{

	private JMenuBar menuBar;
	private Font menuFont;
	private JMenu graphMenu;
	//private JMenuItem load;
	//private JMenuItem save;
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
	
	private GraphPanel graphPanel;
	private ControlPanel controlPanel;
	
	private HashMap<MenuElement, Method> methodsAndNames = new HashMap<MenuElement, Method>(); 
	
	private AppConfig appConfig = AppConfig.getAppConfig();
	
	/**
	 * The constructor for the GUI class.
	 *
	 * @param r The runtime instance for which the gui was created.
	 */
	public GUI(Runtime r){
		super(Global.useProject? (Configuration.appName + " - " + Global.projectName) : (Configuration.appName));
		GuiHelper.setWindowIcon(this);
		
		// load the buttons for the menu - these settings should be done only once
		settingsMenuItem.addActionListener(this);
		settingsMenuItem.setMnemonic(java.awt.event.KeyEvent.VK_S);
		aboutMenuItem.addActionListener(this);
		aboutMenuItem.setMnemonic(java.awt.event.KeyEvent.VK_A);
		aboutMenuItem.setIcon(GuiHelper.getIcon("appIcon21.gif"));
		helpMenuItem.addActionListener(this);
		helpMenuItem.setMnemonic(java.awt.event.KeyEvent.VK_H);
		helpMenuItem.setIcon(GuiHelper.getIcon("helpSmall.gif"));
		helpMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));

		viewFullScreenMenuItem.addActionListener(this);
		viewFullScreenMenuItem.setMnemonic(java.awt.event.KeyEvent.VK_F);
		viewFullScreenMenuItem.setIcon(GuiHelper.getIcon("zoomFullView.gif"));
		viewFullScreenMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
		
		viewZoomInMenuItem.addActionListener(this);
		viewZoomInMenuItem.setMnemonic(java.awt.event.KeyEvent.VK_I);
		viewZoomInMenuItem.setIcon(GuiHelper.getIcon("zoominimage.png"));
		
		viewZoomOutMenuItem.addActionListener(this);
		viewZoomOutMenuItem.setMnemonic(java.awt.event.KeyEvent.VK_O);
		viewZoomOutMenuItem.setIcon(GuiHelper.getIcon("zoomoutimage.png"));
		
		viewZoomFitMenuItem.addActionListener(this);
		viewZoomFitMenuItem.setMnemonic(java.awt.event.KeyEvent.VK_T);
		viewZoomFitMenuItem.setIcon(GuiHelper.getIcon("zoomtofit.gif"));
		viewZoomFitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
		
		runtime = r;
	}
	
	/**
	 * @return The graph panel where the graph is drawn onto
	 */
	public GraphPanel getGraphPanel() {
		return graphPanel;
	}

	/**
	 * Returns the control panel of this GUI.
	 * @return The controlpanel of this GUI.
	 */
	public ControlPanel getControlPanel() {
		return controlPanel;
	}
	
	private GenerateNodesDialog genNodesDialog = new GenerateNodesDialog(this);
	
	/**
	 * The instance of the runtime to make changes comming from the gui.
	 */
	public Runtime runtime = null;
	
	// The zoom level used to draw the graph.
	private double zoomFactor = 1; 
	
	/**
	 * @return The zoom factor currently used to draw the graph.
	 */
	public double getZoomFactor() {
		return zoomFactor;
	}
	
	/**
	 * Sets the zoom factor at which the graph will be drawn and repaints the graph. 
	 * @param zoom The new zoom factor. 
	 */
	public void setZoomFactor(double zoom) {
		setZoomFactorNoRepaint(zoom);
		redrawGUI(); // should be sufficient...
		//redrawGUINow();
	}
	
	/**
	 * Sets the zoom factor at which the graph will be drawn, but does not repaint the graph. 
	 * @param zoom The new zoom factor. 
	 */
	public void setZoomFactorNoRepaint(double zoom) {
		zoomFactor = Math.max(zoom, Configuration.minZoomFactor); // we have open-end zooming ;-)
		
		runtime.getTransformator().setZoomFactor(zoomFactor);
	}
	
	/**
	 * Increase the zoom factor by the factor specified in the config file
	 * and redraw the graph.
	 */
	public void zoomIn(){
		double newFactor = zoomFactor * Configuration.zoomStep;
		setZoomFactor(newFactor);
	}
	
	/**
	 * Multiply the zoom factor by a given number and redraw the graph.
	 * @param multiplicativeFactor The factor to multiply the zoom factor with.
	 */
	public void zoom(double multiplicativeFactor) {
		setZoomFactor(zoomFactor * multiplicativeFactor);
	}
	
	/**
	 * Decrease the zoom factor by the factor specified in the config file and redraw the graph.
	 */
	public void zoomOut() {
		double newFactor = Math.max(zoomFactor / Configuration.zoomStep, 0.01);
		setZoomFactor(newFactor);
	}

	/**
	 * Toggle between full screen and normal view. 
	 */
	synchronized public void toggleFullScreen() {
		if(Global.isRunning) {
			return;
		}
		boolean full = !isUndecorated();
		dispose();
		setUndecorated(full); // window must be disposed prior to calling this method
		// setResizable(!full);
		setVisible(true);		
		if(full) {
			// the following line seems to cause problems with transparent images under windows
			// at least sometimes on my machine, but not on a notebook...
			// under windows, we could use the commented commands, but this seems not to work under linux
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
			//setExtendedState(JFrame.MAXIMIZED_BOTH);
		} else {
			// restore the normal window size
			//setExtendedState(JFrame.NORMAL);
			//GUI.this.setSize(appConfig.guiWindowWidth, appConfig.guiWindowHeight);
			//GUI.this.setLocation(appConfig.guiWindowPosX, appConfig.guiWindowPosY);
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(null);
		}
		// update the menu title 
		viewFullScreenMenuItem.setText(full ? "Exit Full Screen" : "Full Screen");
		
	}
	
	
	/**
	 * This method places GUI elements in the Frame and initializes all its children.
	 */
	public void init(){
		WindowAdapter listener = new WindowAdapter(){
			// Catch the close events
			public void windowClosing(WindowEvent event){
				Main.exitApplication();
			}
		};
		addWindowListener(listener);
		addWindowStateListener(listener);
		
		// react upon resize-events
		this.addComponentListener(new ComponentListener() {
			int oldX = appConfig.guiWindowPosX, oldY = appConfig.guiWindowPosY;
			public void componentResized(ComponentEvent e) {
				if(GUI.this.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
					appConfig.guiIsMaximized = true;
					appConfig.guiWindowPosX = oldX;
					appConfig.guiWindowPosY = oldY;
				} else {
					appConfig.guiIsMaximized = false;
					appConfig.guiWindowWidth= GUI.this.getWidth();
					appConfig.guiWindowHeight = GUI.this.getHeight();
				}
			}
			public void componentMoved(ComponentEvent e) {
				// upon maximizing, first the component is moved, then resized. We only catch the resize event
				oldX = appConfig.guiWindowPosX;
				oldY = appConfig.guiWindowPosY;
				appConfig.guiWindowPosX = GUI.this.getX();
				appConfig.guiWindowPosY = GUI.this.getY();
			}
			public void componentShown(ComponentEvent e) {
			}
			public void componentHidden(ComponentEvent e) {
			}
		});
		
		// -----------------------------------------------------
		// Global Key Input Listener
		// -----------------------------------------------------
		KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		focusManager.addKeyEventPostProcessor(new KeyEventPostProcessor() {
			public boolean postProcessKeyEvent(KeyEvent e) {
				// -----------------------------------------------------
				// ENTER starts / stops the simulation
				// -----------------------------------------------------
				if(!e.isConsumed() && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ENTER) {
					// Note: the event should not be marked consumed. A consumed Enter may
					// have been used elsewhere, e.g. to select a menu.
					if(Global.isRunning) {
						controlPanel.stopSimulation();
					} else {
						controlPanel.startSimulation();
					}
					return true; // no further event handling for this key event
				}
				return false;
			}
		});


		setResizable(true);
		if(appConfig.guiIsMaximized) {
			setExtendedState(JFrame.MAXIMIZED_BOTH);
		}
		this.setSize(new Dimension(appConfig.guiWindowWidth, appConfig.guiWindowHeight));
		this.setLocation(appConfig.guiWindowPosX, appConfig.guiWindowPosY);

		menuBar = new JMenuBar();
		menuFont = menuBar.getFont().deriveFont(Font.PLAIN);
		graphMenu = new JMenu("Simulation");
		graphMenu.setMnemonic(java.awt.event.KeyEvent.VK_S);  
		
		exportMenuItem = new JMenuItem("Export...");
		exportMenuItem.addActionListener(this);
		exportMenuItem.setMnemonic(java.awt.event.KeyEvent.VK_E);  
		exportMenuItem.setIcon(GuiHelper.getIcon("export.gif"));
		
		clearMenuItem = new JMenuItem("Clear Graph");
		clearMenuItem.addActionListener(this);
		clearMenuItem.setMnemonic(java.awt.event.KeyEvent.VK_C);
		clearMenuItem.setIcon(GuiHelper.getIcon("cleargraph.gif"));
		clearMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
		
		reevaluateMenuItem = new JMenuItem("Reevaluate Connections");
		reevaluateMenuItem.addActionListener(this);
		reevaluateMenuItem.setMnemonic(java.awt.event.KeyEvent.VK_R);
		reevaluateMenuItem.setIcon(GuiHelper.getIcon("connectnodes.gif"));
		reevaluateMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
		
		generateMenuItem = new JMenuItem("Generate Nodes");
		generateMenuItem.addActionListener(this);
		generateMenuItem.setMnemonic(java.awt.event.KeyEvent.VK_G);
		generateMenuItem.setIcon(GuiHelper.getIcon("addnodes.gif"));
		generateMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
		
		infoMenuItem = new JMenuItem("Network Info");
		infoMenuItem.addActionListener(this);
		infoMenuItem.setMnemonic(java.awt.event.KeyEvent.VK_I);  
		
		exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.addActionListener(this);
		exitMenuItem.setMnemonic(java.awt.event.KeyEvent.VK_X);  
		
		preferencesMenuItem = new JMenuItem("Preferences");
		preferencesMenuItem.addActionListener(this);
		preferencesMenuItem.setMnemonic(java.awt.event.KeyEvent.VK_P);  
		
		graphMenu.add(generateMenuItem);
		graphMenu.add(reevaluateMenuItem);
		graphMenu.add(clearMenuItem);

		graphMenu.addSeparator();
		graphMenu.add(infoMenuItem);
		graphMenu.add(exportMenuItem);
		graphMenu.add(preferencesMenuItem);
		graphMenu.addSeparator();
		graphMenu.add(exitMenuItem);
		
		menuBar.add(graphMenu);
		
		globalMenu = new JMenu("Global");
		globalMenu.setMnemonic(java.awt.event.KeyEvent.VK_G);
		
		// Compose this menu every time when it is shown. This allows us to 
		// give some more control to the user about the CustomMethods. 
		globalMenu.addMenuListener(new MenuListener() {
			public void menuCanceled(MenuEvent e) {}
			public void menuDeselected(MenuEvent e) {}
			
			/**
			 * Parse a set of methods and include in the 'Global' menu the methods
			 * annotated by {@link GlobalMethod}. For the methods from the project
			 * specific CustomGlobal file, the project may revoke or modify the 
			 * menu name.  
			 * @param funcs The set of methods to parse
			 * @param isProjectSpecific True if the list of methods belongs to the
			 * project specific CustomGlobal file.
			 * @return True if at least one entry was added.
			 */
			private boolean testMethods(Method[] methods, boolean isProjectSpecific) {
				boolean hasEntry = false;
				Vector<JMenu> subMenus = new Vector<JMenu>();
				
				// sort the methods according to the order-flag given in the annotation
				SortableVector<Method> mlist = new SortableVector<Method>();
				for(Method m : methods) {
					AbstractCustomGlobal.GlobalMethod info = m.getAnnotation(AbstractCustomGlobal.GlobalMethod.class);
					if(info != null) {
						mlist.add(m);
					}
				}
				mlist.sort(new Comparator<Method>() {
					public int compare(Method o1, Method o2) {
						AbstractCustomGlobal.GlobalMethod info1 = o1.getAnnotation(AbstractCustomGlobal.GlobalMethod.class);
						AbstractCustomGlobal.GlobalMethod info2 = o2.getAnnotation(AbstractCustomGlobal.GlobalMethod.class);
						if(info1 != null && info2 != null){
							int i1 = info1.order();
							int i2 = info2.order();
							return i1 < i2 ? -1 : (i1 == i2 ? 0 : 1);
						}
						return 0; // should not happen, if it does, ordering may be wrong
					}
				});
				
				for(Method method : mlist) {
					AbstractCustomGlobal.GlobalMethod info = method.getAnnotation(AbstractCustomGlobal.GlobalMethod.class);
					if(info != null){
						if(!isProjectSpecific) {
							if(!Modifier.isStatic(method.getModifiers())) {
								Main.warning("The method '" + method.getName() + "' in sinalgo.runtime.Global cannot be called from the dropdown menu, as it needs to be static.\nThe method is not added to the menu.");
								continue;
							}
						}
						if(method.getParameterTypes().length != 0) {
							if(isProjectSpecific) {
								Main.warning("The method '" + method.getName() + "' from the projects CustomGlobal class cannot be called from the dropdown menu, as it needs parameters to be called. \nThe method is not added to the menu.");
							} else {
								Main.warning("The method '" + method.getName() + "' in sinalgo.runtime.Global cannot be called from the dropdown menu, as it needs parameters to be called.\nThe method is not added to the menu.");
							}
							continue;
						}
						String text = isProjectSpecific ? Global.customGlobal.includeGlobalMethodInMenu(method, info.menuText()) : info.menuText();
						if(text == null) {
							continue; // the method was dropped by the project
						}
						JMenuItem item = new JMenuItem(text);
						item.addActionListener(globalInvoker);
						methodsAndNames.put(item, method);

						String subMenuText = info.subMenu();
						if(subMenuText.equals("")) {
							globalMenu.add(item);
						} else {
							JMenu menu = null;
							for(JMenu m : subMenus) {
								if(m.getText().equals(subMenuText)) {
									menu = m; 
									break;
								}
							}
							if(menu == null) {
								menu = new JMenu(subMenuText);
								subMenus.add(menu);
								globalMenu.add(menu);
							}
							menu.add(item);
						}
						hasEntry = true;
					}
				}
				return hasEntry;
			}
			public void menuSelected(MenuEvent event) {
				globalMenu.removeAll();

				// add the project specific methods
				Method[] methods = Global.customGlobal.getClass().getMethods();
				if(testMethods(methods, true)) {
					globalMenu.addSeparator();
				}

				// add the framework-side defined methods in sinalgo.runtime.Global
				try {
					methods = Class.forName("sinalgo.runtime.Global").getMethods();
					if(testMethods(methods, false)) {
						globalMenu.addSeparator();
					}
				} catch (ClassNotFoundException e) {
					Main.fatalError("Could not find class sinalgo.runtime.Global to get the global gui methods from.");
				}
				
				// And finally the Settings and About dialog
				globalMenu.add(settingsMenuItem);
				
				// and set the font of the menu entries
				setMenuFont(globalMenu);
			}
		});
		
		menuBar.add(globalMenu);
		
		// ---------------------------------------------
		// View Menu
		// ---------------------------------------------
		viewMenu = new JMenu("View");
		viewMenu.setMnemonic(java.awt.event.KeyEvent.VK_V);
		viewMenu.add(viewZoomOutMenuItem);
		viewMenu.add(viewZoomInMenuItem);
		viewMenu.add(viewZoomFitMenuItem);
		viewMenu.add(viewFullScreenMenuItem);
		
		menuBar.add(viewMenu);
		
		// ---------------------------------------------
		// Help Menu
		// ---------------------------------------------
		helpMenu = new JMenu("Help");
		helpMenu.setMnemonic(java.awt.event.KeyEvent.VK_H);
		helpMenu.add(helpMenuItem);
		helpMenu.addSeparator();
		helpMenu.add(aboutMenuItem);
		menuBar.add(helpMenu);

		setMenuFont(menuBar);
		
		this.setJMenuBar(menuBar);
		
		// The content pane
		contentPane = new JPanel();

		graphPanel = new GraphPanel(this);
		// activate the Tooltip for the graphPanel. The "Default Tooltip" is actually never shown
		// because the text to show is overwritten in the GraphPanel-Classes getToolTipText(MouseEvent e)
		graphPanel.createToolTip();
		graphPanel.setToolTipText("Default Tooltip"); // to initialize, must set an arbitrary text
		graphPanel.requestDefaultViewOnNextDraw();

		
		if(Configuration.extendedControl) {
			contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));
			controlPanel = new MaximizedControlPanel(this);
			contentPane.add(graphPanel);
			contentPane.add(controlPanel);
		} else {
			contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
			controlPanel = new MinimizedControlPanel(this);
			contentPane.add(controlPanel);
			contentPane.add(graphPanel);
		}
	
		this.add(contentPane);
		
		setVisible(true);
		// trigger a first paint (needed!)
		this.repaint();
	}
	
	private void setMenuFont(MenuElement m) {
		m.getComponent().setFont(menuFont);
		if(m.getSubElements().length > 0) {
			for(MenuElement e : m.getSubElements()) {
				setMenuFont(e);
			}
		}
	}
	
	
	JPanel contentPane = null;
	
	/**
	 * Switches between the two modes for the control panel depending on the boolean parameter.
	 * 
	 * @param toExtended if set true the control panel is set on Extended (
	 */
	public void changePanel(boolean toExtended) {
		// STRANGE! we need to add the new contol panel before removing the old one
		// otherwise, the mouse scrolling wont be detected anymore.
		if(toExtended) { // from minimized to maximized
			contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));
			ControlPanel oldCP = controlPanel;
			controlPanel = new MaximizedControlPanel(this);
			contentPane.add(controlPanel, 2); // content pane must be after graph panel
			contentPane.remove(oldCP);
		} else { // from maximized to minimized
			contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
			ControlPanel oldCP = controlPanel;
			controlPanel = new MinimizedControlPanel(this);
			contentPane.add(controlPanel, 0); // content pane is first in list
			contentPane.remove(oldCP);
		}
		contentPane.revalidate();  
		graphPanel.requireFullDrawOnNextPaint();
		this.repaint();
	}
	
	/**
	 * This method resets the gui for the current configuration settings.
	 * This method is called when the user removes all nodes. 
	 */
	public void allNodesAreRemoved() { 
		graphPanel.allNodesAreRemoved();
	}
	
	/**
	 * A boolean indicating whether the paint() method of this gui has been 
	 * called at least once. THe graph panel starts drawing itself only 
	 * after this flag has been set to true.  
	 */
	public boolean firstTimePainted = false;
	
	/* (non-Javadoc)
	 * @see java.awt.Container#paint(java.awt.Graphics)
	 */
	public void paint(Graphics g){
		super.paint(g);
		firstTimePainted = true;
		controlPanel.repaint();
		graphPanel.repaint();
	}
	
	/**
	 * Returns the transformation instance that knows how to translate
	 * between the logic coordinate system used by the simulation and the
	 * corresponding coordinates on the GUI.
	 * @return The transformation instance.
	 */
	public PositionTransformation getTransformator() {
		return runtime.getTransformator();
	}
	
	/**
	 * This method redraws the Graph panel and the control panel.
	 * <p>
	 * Call this method whenever you require the graph to be redrawn
	 * immediately (synchronously) within the simulation code. Note 
	 * that this method draws the graph immediately. I.e. to avoid concurrent
	 * access to the datastructures, this method should only be called
	 * when no simulation round is executing.
	 * <p>
	 * The paint process of the graph panel is as following: 
	 * First, the graph (nodes, edges, background, ...) is painted onto
	 * an image. Then, the content of the image is copied to the screen.
	 * Quite often, it is not necessary to redraw the image, and we can
	 * paint again the 'old' image from the previous call to paint the
	 * control panel. This is for example the case when the mouse moved
	 * over a node or edge and triggered a tool tip window to be shown.
	 * When the graph or transformation matrix changed, the image needs to
	 * be repainted.  
	 * <p>
	 * This method forces a redraw of the image, which may be quite
	 * expensive for huge graphs. Thus, call it only, when you really
	 * need a synchronous repaint of the image.
	 * <p>
	 * In almost all cases, calling redrawGUI() is preferred.
	 */
	public void redrawGUINow(){
		controlPanel.repaint();
		graphPanel.paintNow();
	}
	
	/**
	 * Repaints the Control Panel and the Graph Panel. 
	 * In contrast to redrawGUINow, this method does not
	 * enforce the graph panel to be painted immediately, but leaves
	 * it up to the JVM to schedule the repaint.
	 * Call this method whenever it is not crucial to redraw the
	 * graph immediately.
	 * <p>
	 * <p>
	 * The paint process of the graph panel is as following: 
	 * First, the graph (nodes, edges, background, ...) is painted onto
	 * an image. Then, the content of the image is copied to the screen.
	 * Quite often, it is not necessary to redraw the image, and we can
	 * paint again the 'old' image from the previous call to paint the
	 * control panel. This is for example the case when the mouse moved
	 * over a node or edge and triggered a tool tip window to be shown.
	 * When the graph or transformation matrix changed, the image needs to
	 * be repainted.  
	 * <p>
	 * This method forces a redraw of the image, which may be quite
	 * expensive for huge graphs. Thus, call it only, when you really
	 * need the gui to be updated.
	 * @see GUI#redrawGUINow() for how the graph panel is drawn.  
	 */
	public void redrawGUI() {
		graphPanel.requireFullDrawOnNextPaint();
		controlPanel.repaint();
		graphPanel.repaint();
	}
	
	/**
	 * Only redraw the control panel. This is used to redraw the controlpanel (inclusive the small
	 * preview picture) without painting the whole graph.
	 */
	public void redrawControl(){
		controlPanel.repaint();
	}
	
	/**
	 * This method pops a Error message on the frame with the given message and the given title.
	 *
	 * @param message The message to display.
	 * @param title The Title of the messageDialog.
	 */
	public void popupErrorMessage(String message, String title){
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * This method sets the start button of the control panel enabled according to the boolean passed.
	 *
	 * @param b If b is true the start button is set enabled and the abort button is set false (and vice versa)
	 */
	public void setStartButtonEnabled(boolean b){
		controlPanel.setStartButtonEnabled(b);
		graphMenu.setEnabled(b);
		globalMenu.setEnabled(b);
		helpMenu.setEnabled(b);
		viewMenu.setEnabled(b);
		// We could disallow resizing the window here, but this flickers
		// setResizable(b);
	}
	
	/**
	 * Resets the time performed and the number of events already executed.
	 * 
	 * @param time The time that passed by until the actual moment.
	 * @param eventNumber The number of events that have been executed until now.
	 */
	public void setRoundsPerformed(double time, int eventNumber){
		controlPanel.setRoundsPerformed(time, eventNumber);
	}
	
	/**
	 * This method changes the number of rounds to be displayed in the control panel.
	 *
	 * @param i The number to be displayed in the control panel.
	 */
	public void setRoundsPerformed(int i){
		controlPanel.setRoundsPerformed(i);
	}
	
	/**
	 * Sets the event that was processed last. 
	 * 
	 * @param e The event that was last processed, null if there was no event. 
	 */
	public void setCurrentlyProcessedEvent(Event e){
		controlPanel.setCurrentEvent(e);
	}
	
	/**
	 * Sets the mouse-position of the cursor. 
	 * @param s A string representation of the position
	 */
	public void setMousePosition(String s) {
		controlPanel.setMousePosition(s);
	}

	/**
	 * Called when the user presses the button to remove all nodes from the simulation.
	 */
	public void clearAllNodes() {
		Runtime.clearAllNodes();
	}

	/**
	 * Opens a dialog that allows to add new nodes
	 */
	public void addNodes() {
		genNodesDialog.compose(null);
	}
	
	/**
	 * Opoens a dialog to specify the models to craete a node that is placed at the
	 * specific position.
	 * @param pos The positino where the node will be placed.
	 */
	public void addSingleNode(Position pos) {
		genNodesDialog.compose(pos);
	}
	
	/**
	 * Creates a node with the default settings at the given position.
	 * @param pos 
	 */
	public void addSingleDefaultNode(Position pos) {
		genNodesDialog.generateDefaultNode(pos);
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(generateMenuItem.getActionCommand())){
			addNodes();
		}
		else if(e.getActionCommand().equals(clearMenuItem.getActionCommand())){
			if(0 == JOptionPane.showConfirmDialog(this, "Do you really want to remove all nodes?", "Remove all nodes?", JOptionPane.YES_NO_OPTION)){
				clearAllNodes();
			}
		}
		else if(e.getActionCommand().equals(preferencesMenuItem.getActionCommand())){
			new GraphPreferencesDialog(this);
		}
		else if(e.getActionCommand().equals(reevaluateMenuItem.getActionCommand())){
			if(0 == JOptionPane.showConfirmDialog(this, "Do you really want to reevaluate the connections of all nodes?", "Reevaluate Connections?", JOptionPane.YES_NO_OPTION)){
				Runtime.reevaluateConnections();
				this.redrawGUI();
			}
		}
		else if(e.getActionCommand().equals(exportMenuItem.getActionCommand())){
			try {
				new Exporter(this).export(new Rectangle(0, 0, graphPanel.getWidth(), graphPanel.getHeight()), getTransformator());
			} catch (ExportException e1) {
				Main.minorError(e1.getMessage());
			}
		}
		else if(e.getActionCommand().equals(infoMenuItem.getActionCommand())){
			new GraphInfoDialog(this);
		}
		else if(e.getActionCommand().equals(settingsMenuItem.getActionCommand())){
			new GlobalSettingsDialog(this);
		}
		else if(e.getActionCommand().equals(aboutMenuItem.getActionCommand())) {
			new AboutDialog(this);
		}
		else if(e.getActionCommand().equals(helpMenuItem.getActionCommand())) {
			HelpDialog.showHelp(this); // start in a new thread
		}
		else if(e.getActionCommand().equals(exitMenuItem.getActionCommand())) {
			Main.exitApplication();
		}
		else if(e.getActionCommand().equals(viewFullScreenMenuItem.getActionCommand())) {
			toggleFullScreen();
		}
		else if(e.getActionCommand().equals(viewZoomInMenuItem.getActionCommand())) {
			zoomIn();
		}
		else if(e.getActionCommand().equals(viewZoomOutMenuItem.getActionCommand())) {
			zoomOut();
		}
		else if(e.getActionCommand().equals(viewZoomFitMenuItem.getActionCommand())) {
			getTransformator().zoomToFit(getGraphPanel().getWidth(),
			                             getGraphPanel().getHeight());
			setZoomFactor(getTransformator().getZoomFactor());			
		}
	}
	
	// class used to invoke the global user-defined methods
	class GlobalInvoker implements ActionListener{

		public void actionPerformed(ActionEvent event) {
			Method method = methodsAndNames.get(event.getSource());
			if(method == null) {
				Main.fatalError("Cannot find method associated with menu item " + event.getActionCommand());
			}
			try {
				synchronized(getTransformator()){
					//synchronize it on the transformator to grant not to be concurrent with
					//any drawing or modifying action
					try{
						method.invoke(Global.customGlobal, (Object[])null);
					}
					catch(IllegalArgumentException e) {
						method.invoke(null, (Object[])null);
					}
				}
			} 
			catch (IllegalArgumentException e) { Main.minorError(e); }
			catch (SecurityException e) { Main.minorError(e); }
			catch (IllegalAccessException e) { Main.minorError(e); }
			catch(InvocationTargetException e) { Main.minorError(e); }
		}
	}
}
