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
package projects.sample1;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import projects.sample1.nodes.nodeImplementations.S1Node;
import sinalgo.configuration.Configuration;
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.runtime.AbstractCustomGlobal;
import sinalgo.runtime.GUIRuntime;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.tools.Tools;
import sinalgo.tools.logging.Logging;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.Enumeration;

/**
 * This class holds customized global state and methods for the framework. The
 * only mandatory method to overwrite is <code>hasTerminated</code> <br>
 * Optional methods to override are
 * <ul>
 * <li><code>customPaint</code></li>
 * <li><code>handleEmptyEventQueue</code></li>
 * <li><code>onExit</code></li>
 * <li><code>preRun</code></li>
 * <li><code>preRound</code></li>
 * <li><code>postRound</code></li>
 * <li><code>checkProjectRequirements</code></li>
 * </ul>
 *
 * @see sinalgo.runtime.AbstractCustomGlobal for more details. <br>
 * In addition, this class also provides the possibility to extend the
 * framework with custom methods that can be called either through the menu
 * or via a button that is added to the GUI.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class CustomGlobal extends AbstractCustomGlobal {

    private Logging log = Logging.getLogger("s1_log.txt");

    // The user can optionally specify exitAfter in the config file to indicate
    // after how many rounds the simulation should stop.
    private boolean exitAfterFixedRounds;
    private int exitAfterNumRounds;

    {
        if (Configuration.hasParameter("exitAfter")) {
            try {
                this.setExitAfterFixedRounds(Configuration.getBooleanParameter("exitAfter"));
            } catch (CorruptConfigurationEntryException e1) {
                throw new SinalgoFatalException("The 'exitAfter' needs to be a valid boolean.");
            }
            if (this.isExitAfterFixedRounds()) {
                try {
                    this.setExitAfterNumRounds(Configuration.getIntegerParameter("exitAfter/rounds"));
                } catch (CorruptConfigurationEntryException e) {
                    throw new SinalgoFatalException(
                            "The 'exitAfter/rounds' parameter specifies the maximum time the simulation runs. It needs to be a valid integer.");
                }
            }
        } else {
            this.setExitAfterFixedRounds(false);
        }
    }

    @Override
    public boolean hasTerminated() {
        if (this.isExitAfterFixedRounds()) {
            return this.getExitAfterNumRounds() <= Global.getCurrentTime();
        }

        if (Tools.isSimulationInGuiMode()) {
            return false; // in GUI mode, have the user decide when to stop.
        } else {
            return Global.getCurrentTime() > 100000; // stop after x rounds
        }
    }

    /**
     * An example of a method that will be available through the menu of the GUI.
     */
    @AbstractCustomGlobal.GlobalMethod(menuText = "Echo", order = 1)
    public void echo() {
        // Query the user for an input
        String answer = JOptionPane.showInputDialog(null, "This is an example.\nType in any text to echo.");
        // Show an information message
        JOptionPane.showMessageDialog(null, "You typed '" + answer + "'", "Example Echo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void preRun() {
        // start the communication automatically if the AutoStart flag is set.
        try {
            if (Configuration.hasParameter("AutoStart") && Configuration.getBooleanParameter("AutoStart")) {
                S1Node n = (S1Node) Tools.getNodeList().getRandomNode();
                n.start(); // start from a random node
            }
        } catch (CorruptConfigurationEntryException e) {
            throw new SinalgoFatalException("The 'AutoStart' option in the configuration file specifies whether a node"
                    + "should be automatically selected to start the communication process. This flag needs to be"
                    + "of type boolean (true|false).");
        }
    }

    @Override
    public void postRound() {
        double dt = System.currentTimeMillis() - Global.getStartTimeOfRound().getTime();
        this.getLog().logln("Round " + (int) (Global.getCurrentTime()) + " time: " + dt + " Msg/Round: "
                + Global.getNumberOfMessagesInThisRound());
    }

    /**
     * Custom button to generate a infomation Dialog to show the node with the
     * maximum sent messages.
     */
    @CustomButton(buttonText = "OKButton", imageName = "OK.gif", toolTipText = "Prints out the maximum sent messages of all nodes.")
    public void printMaxMsgSent() {
        S1Node max = null;
        Enumeration<?> nodeEnumer = Tools.getNodeList().getNodeEnumeration();
        while (nodeEnumer.hasMoreElements()) {
            S1Node s1Node = (S1Node) nodeEnumer.nextElement();
            if (max == null) {
                max = s1Node;
            } else {
                if (max.getMsgSent() < s1Node.getMsgSent()) {
                    max = s1Node;
                }
            }
        }
        if (Global.isGuiMode()) {
            if (max != null) {
                JOptionPane.showMessageDialog(((GUIRuntime) Main.getRuntime()).getGUI(),
                        "The node with the maximum sent number of messages is the node with ID " + max.getID()
                                + ". \nIt sent " + max.getMsgSent() + " messages until now.");
            } else {
                JOptionPane.showMessageDialog(((GUIRuntime) Main.getRuntime()).getGUI(), "There is no node.");
            }
        }
    }

    /*
     * The method stopSending can be called through the 'Global' menu of Sinalgo.
     * The menu-item is placed in a sub-menu 'Node Control', order='2' guarantees
     * that it is placed after the 'Echo' menu. Note the use of the method
     * includeGlobalMethodInMenu which lets you specify at each time the menu pops
     * up, what menu-text should be displayed (or no menu at all, if the method
     * returns null.)
     */

    @GlobalMethod(menuText = "...", subMenu = "Node Control", order = 2)
    public void stopSending() {
        S1Node.setSending(!S1Node.isSending());
    }

    @Override
    public String includeGlobalMethodInMenu(Method m, String defaultText) {
        if (m.getName().equals("stopSending")) {
            if (Tools.getNodeList().size() == 0) {
                return null; // don't display this menu option
            }
            return S1Node.isSending() ? "Stop Sending" : "Continue Sending";
        }
        return defaultText;
    }

    @Override
    public void checkProjectRequirements() {
        if (Global.isAsynchronousMode()) {
            throw new SinalgoFatalException(
                    "SampleProject1 is written to be executed in synchronous mode. It doesn't work in asynchronous mode.");
        }
    }

    @Override
    public void onExit() {
        // perform some cleanup operations here
    }
}
