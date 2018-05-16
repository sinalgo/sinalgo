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
package projects.sample4.nodes.nodeImplementations;

import projects.defaultProject.nodes.timers.MessageTimer;
import projects.sample4.nodes.messages.S4Message;
import projects.sample4.nodes.timers.S4SendDirectTimer;
import sinalgo.configuration.Configuration;
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.exception.WrongConfigurationException;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.io.eps.EPSOutputPrintStream;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;
import sinalgo.tools.Tools;

import java.awt.*;

public class S4Node extends Node {

    @Override
    public void checkRequirements() throws WrongConfigurationException {
        // Nothing to do - we could check here, that proper models are set, and other
        // settings are correct
    }

    @Override
    public void handleMessages(Inbox inbox) {
        while (inbox.hasNext()) {
            Message msg = inbox.next();
            if (msg instanceof S4Message) {
                S4Message m = (S4Message) msg;
                // green and yellow messages are forwarded to all neighbors
                if (m.getColor() == Color.GREEN && !this.getColor().equals(m.getColor())) {
                    this.broadcast(m);
                } else if (m.getColor() == Color.YELLOW && !this.getColor().equals(m.getColor())) {
                    this.broadcast(m);
                }
                this.setColor(m.getColor()); // set this node's color
            }
        }
    }

    @NodePopupMethod(menuText = "Multicast RED")
    public void multicastRED() {
        this.sendColorMessage(Color.RED, null);
    }

    @NodePopupMethod(menuText = "Multicast BLUE")
    public void multicastBLUE() {
        this.sendColorMessage(Color.BLUE, null);
    }

    @NodePopupMethod(menuText = "BROADCAST GREEN")
    public void broadcastGREEN() {
        this.sendColorMessage(Color.GREEN, null);
    }

    @NodePopupMethod(menuText = "BROADCAST YELLOW")
    public void broadcastYELLOW() {
        this.sendColorMessage(Color.YELLOW, null);
    }

    /**
     * Sends a message to (a neighbor | all neighbors) with the specified color as
     * message content.
     *
     * @param c  The color to write in the message.
     * @param to Receiver node, or null, if all neighbors should receive the
     *           message.
     */
    private void sendColorMessage(Color c, Node to) {
        S4Message msg = new S4Message();
        msg.setColor(c);
        if (Tools.isSimulationInAsynchroneMode()) {
            // sending the messages directly is OK in async mode
            if (to != null) {
                this.send(msg, to);
            } else {
                this.broadcast(msg);
            }
        } else {
            // In Synchronous mode, a node is only allowed to send messages during the
            // execution of its step. We can easily schedule to send this message during the
            // next step by setting a timer. The MessageTimer from the default project
            // already
            // implements the desired functionality.
            MessageTimer t;
            if (to != null) {
                t = new MessageTimer(msg, to); // unicast
            } else {
                t = new MessageTimer(msg); // multicast
            }
            t.startRelative(Tools.getRandomNumberGenerator().nextDouble(), this);
        }
    }

    @NodePopupMethod(menuText = "Unicast Gray")
    public void unicastGRAY() {
        Tools.getNodeSelectedByUser(n -> {
            if (n == null) {
                return; // the user aborted
            }
            this.sendColorMessage(Color.GRAY, n);
        }, "Select a node to which you want to send a 'yellow' message.");
    }

    @NodePopupMethod(menuText = "Unicast CYAN")
    public void unicastCyan() {
        Tools.getNodeSelectedByUser(n -> {
            if (n == null) {
                return; // the user aborted
            }
            this.sendColorMessage(Color.CYAN, n);
        }, "Select a node to which you want to send a 'cyan' message.");
    }

    /**
     * This popup method demonstrates how a message can be sent even when there is
     * no edge between the sender and receiver
     */
    @NodePopupMethod(menuText = "send DIRECT PINK")
    public void sendDirectPink() {
        Tools.getNodeSelectedByUser(n -> {
            if (n == null) {
                return; // the user aborted
            }
            S4Message msg = new S4Message();
            msg.setColor(Color.pink);
            if (Tools.isSimulationInAsynchroneMode()) {
                this.sendDirect(msg, n);
            } else {
                // we need to set a timer, such that the message is
                // sent during the next round, when this node performs its step.
                S4SendDirectTimer timer = new S4SendDirectTimer(msg, n);
                timer.startRelative(1.0, S4Node.this);
            }
        }, "Select a node to which you want to send a direct 'PINK' message.");
    }

    private boolean simpleDraw;

    @Override
    public void init() {
        if (Configuration.hasParameter("S4Node/simpleDraw")) {
            try {
                this.simpleDraw = Configuration.getBooleanParameter("S4Node/simpleDraw");
            } catch (CorruptConfigurationEntryException e) {
                throw new SinalgoFatalException("Invalid config field S4Node/simpleDraw: Expected a boolean.\n" + e.getMessage());
            }
        } else {
            this.simpleDraw = false;
        }
        // nothing to do here
    }

    @Override
    public void neighborhoodChange() {
        // not called in async mode!
    }

    @Override
    public void preStep() {
        // not called in async mode!
    }

    @Override
    public void postStep() {
        // not called in async mode!
    }

    private boolean drawRound;

    private boolean isDrawRound() {
        if (this.drawRound) {
            return true;
        }
        return this.getColor().equals(Color.YELLOW);
    }

    @NodePopupMethod(menuText = "Draw as Circle")
    public void drawRound() {
        this.drawRound = !this.drawRound;
        Tools.repaintGUI();
    }

    @Override
    public void draw(Graphics g, PositionTransformation pt, boolean highlight) {
        // overwrite the draw method to change how the GUI represents this node
        if (this.simpleDraw) {
            super.draw(g, pt, highlight);
        } else {
            if (this.isDrawRound()) {
                super.drawNodeAsDiskWithText(g, pt, highlight, Long.toString(this.getID()), 16, Color.WHITE);
            } else {
                super.drawNodeAsSquareWithText(g, pt, highlight, Long.toString(this.getID()), 16, Color.WHITE);
            }
        }
    }

    @Override
    public void drawToPostScript(EPSOutputPrintStream pw, PositionTransformation pt) {
        if (this.isDrawRound()) {
            super.drawToPostScriptAsDisk(pw, pt, this.getDrawingSizeInPixels() / 2, this.getColor());
        } else {
            super.drawToPostscriptAsSquare(pw, pt, this.getDrawingSizeInPixels(), this.getColor());
        }
    }
}
