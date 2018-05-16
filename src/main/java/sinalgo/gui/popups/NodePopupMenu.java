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
package sinalgo.gui.popups;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.configuration.Configuration;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.gui.GUI;
import sinalgo.gui.dialogs.NodeInfoDialog;
import sinalgo.nodes.Node;
import sinalgo.runtime.Main;
import sinalgo.runtime.SinalgoRuntime;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * The class for the popupmenus displayed on the graph panel when the user
 * presses the right mouse button over a node.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class NodePopupMenu extends AbstractPopupMenu implements ActionListener {

    private static final long serialVersionUID = 3539517948195533969L;

    private HashMap<String, Method> methodsAndDescriptions = new HashMap<>();

    private Node node;
    private JMenuItem info = new JMenuItem("Info");
    private JMenuItem delete = new JMenuItem("Delete Node");
    private JMenuItem showCoordinateCube = new JMenuItem("Show coordinate cube");
    private JMenuItem hideCoordinateCube = new JMenuItem("Hide coordinate cube");

    /**
     * The constructor for the NodePopupMenu class.
     *
     * @param p The parentGUI gui, where the popupMenu appears in.
     */
    public NodePopupMenu(GUI p) {
        this.setParentGUI(p);
        this.getInfo().addActionListener(this);
        this.getDelete().addActionListener(this);
        this.getShowCoordinateCube().addActionListener(this);
        this.getHideCoordinateCube().addActionListener(this);
    }

    /**
     * This method composes the popupmenu for the given node.
     *
     * @param n The node the popupmenu is about.
     */
    public void compose(Node n) {

        this.setNode(n);

        this.getMethodsAndDescriptions().clear();
        this.removeAll();

        this.add(this.getInfo());

        if (Configuration.getDimensions() == 3) {
            if (this.getParentGUI().getGraphPanel().containsNodeToDrawCoordinateCube(n)) {
                this.add(this.getHideCoordinateCube());
            } else {
                this.add(this.getShowCoordinateCube());
            }
        }

        this.add(this.getDelete());

        this.addSeparator();

        JMenuItem dummy = new JMenuItem("No Methods specified");
        dummy.setEnabled(false);

        boolean customMethods = false;

        Method[] methods = this.getNode().getClass().getMethods();
        for (Method method : methods) {
            Node.NodePopupMethod info = method.getAnnotation(Node.NodePopupMethod.class);
            if (info != null) {
                String text = n.includeMethodInPopupMenu(method, info.menuText());
                if (text == null) { // the user dismissed this menu item
                    continue;
                }
                JMenuItem item = new JMenuItem(text);
                item.addActionListener(this); // BUGFIX for 0.75.0 -> 0.75.1 : this line was missing
                this.add(item);
                customMethods = true;
                this.getMethodsAndDescriptions().put(text, method); // BUGFIX: 1st parameter was info.menuText()
            }
        }

        if (!customMethods) {
            this.add(dummy);
        }

        this.addSeparator();

        this.add(this.getZoomIn());
        this.add(this.getZoomOut());
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals(this.getInfo().getActionCommand())) {
            new NodeInfoDialog(this.getParentGUI(), this.getNode());
        } else if (event.getActionCommand().equals(this.getDelete().getActionCommand())) {
            SinalgoRuntime.removeNode(this.getNode());
            this.getParentGUI().redrawGUI();
        } else if (event.getActionCommand().equals(this.getShowCoordinateCube().getActionCommand())) {
            this.getParentGUI().getGraphPanel().setNodeToDrawCoordinateCube(this.getNode());
            this.getParentGUI().repaint(); // need not repaint the graph, only the toppings
        } else if (event.getActionCommand().equals(this.getHideCoordinateCube().getActionCommand())) {
            this.getParentGUI().getGraphPanel().removeNodeToDrawCoordinateCube(this.getNode());
            this.getParentGUI().repaint(); // need not repaint the graph, only the toppings
        } else {
            // try to execute a custom-command
            Method clickedMethod = this.getMethodsAndDescriptions().get(event.getActionCommand());
            if (clickedMethod == null) {
                throw new SinalgoFatalException("Cannot find method associated with menu item " + event.getActionCommand());
            }
            try {
                synchronized (this.getParentGUI().getTransformator()) {
                    // synchronize it on the transformator to grant not to be concurrent with
                    // any drawing or modifying action
                    clickedMethod.invoke(this.getNode());
                }
            } catch (InvocationTargetException e) {
                String text = "";
                if (null != e.getCause()) {
                    text = "\n\n" + e.getCause() + "\n\n" + e.getCause().getMessage();
                }
                Main.minorError("The method '" + clickedMethod.getName()
                        + "' has thrown an exception and did not terminate properly:\n" + e + text);
            } catch (IllegalArgumentException e) {
                Main.minorError("The method '" + clickedMethod.getName() + "' cannot be invoked without parameters:\n"
                        + e + "\n\n" + e.getMessage());
            } catch (IllegalAccessException e) {
                Main.minorError("The method '" + clickedMethod.getName() + "' cannot be accessed:\n" + e + "\n\n"
                        + e.getMessage());
            }

            this.getParentGUI().redrawGUI();
        }
    }
}
