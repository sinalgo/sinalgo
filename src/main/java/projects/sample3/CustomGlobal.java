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
package projects.sample3;

import projects.sample3.nodes.nodeImplementations.MobileNode;
import projects.sample3.nodes.timers.SmsTimer;
import sinalgo.nodes.Node;
import sinalgo.runtime.AbstractCustomGlobal;
import sinalgo.tools.Tools;

import java.awt.*;

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
public class CustomGlobal extends AbstractCustomGlobal {

    @Override
    public boolean hasTerminated() {
        return false;
    }

    /**
     * An example of a method that will be available through the menu of the GUI.
     */
    @AbstractCustomGlobal.GlobalMethod(menuText = "Reset Color")
    public void resetColor() {
        for (Node n : Tools.getNodeList()) {
            n.setColor(Color.black);
        }
    }

    private boolean automaticSMS;

    @AbstractCustomGlobal.GlobalMethod(menuText = "Toggle Automatic SMS")
    public void toggleAutomaticSMS() {
        this.automaticSMS = !this.automaticSMS;
    }

    @Override
    public void postRound() {
        if (this.automaticSMS) {
            Node sender = this.getRandomMobileNode();
            Node receiver = this.getRandomMobileNode();
            SmsTimer t = new SmsTimer("Automatic SMS", receiver);
            t.startRelative(1, sender);
            sender.setColor(Color.RED);
            receiver.setColor(Color.BLUE);
        }
    }

    private MobileNode getRandomMobileNode() {
        Node n = Tools.getNodeList().getRandomNode();
        while (!(n instanceof MobileNode)) {
            n = Tools.getNodeList().getRandomNode();
        }
        return (MobileNode) n;
    }

    @Override
    public void preRun() {
        // A method called at startup, before the first round is executed.
    }
}
