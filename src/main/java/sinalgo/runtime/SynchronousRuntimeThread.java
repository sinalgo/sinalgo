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
package sinalgo.runtime;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.configuration.Configuration;
import sinalgo.exception.WrongConfigurationException;
import sinalgo.nodes.Node;
import sinalgo.tools.logging.LogL;

import java.util.Date;

/**
 * The runtime implementation for the synchronous simulation mode
 */
public class SynchronousRuntimeThread extends Thread {

    /**
     * The number of rounds the thread has to perform.
     */
    @Getter
    @Setter
    private long numberOfRounds;

    @Getter(AccessLevel.PRIVATE)
    private GUIRuntime runtime; // If in GUI-MODE, this member holds the the GUIRuntime

    /**
     * The rate to refresh the graph. This means all how many steps the gui has to
     * be redrawn.
     */
    @Getter
    @Setter
    private long refreshRate = 1;

    /**
     * The constructor for the RuntimeThread class. This constructor is used to
     * create a RuntimeThread with a enabled GUI.
     *
     * @param r The instance of the GuiRuntime that has started this thread.
     */
    SynchronousRuntimeThread(GUIRuntime r) {
        this.runtime = r;
    }

    /**
     * Default constructor for the batch-mode.
     */
    SynchronousRuntimeThread() {
        this.runtime = null;
    }

    @Override
    public void run() {
        Global.setRunning(true);

        Global.setStartTime(new Date());

        for (long i = 0; i < this.getNumberOfRounds(); i++) {
            // In GUI-mode, check whether ABORT was pressed.
            if (this.getRuntime() != null && this.getRuntime().isAbort()) {
                this.getRuntime().setAbort(false);
                break;
            }

            // INCREMENT THE GLOBAL TIME by 1
            Global.setCurrentTime(Global.getCurrentTime() + 1);
            Global.setEvenRound(!Global.isEvenRound()); // flip the bit

            Global.setStartTimeOfRound(new Date());
            Global.setNumberOfMessagesInThisRound(0);

            Global.getCustomGlobal().preRound();
            Global.getCustomGlobal().handleGlobalTimers();

            // Mobility is performed in a separate iteration over all nodes to avoid
            // inconsistencies.
            if (Configuration.isMobility()) {
                for (Node n : SinalgoRuntime.getNodes()) {
                    n.setPosition(n.getMobilityModel().getNextPos(n));
                }
            }

            // Before the nodes perform their step, the entire network graph is updated
            // such that all nodes see the same network when they perform their step.
            for (Node n : SinalgoRuntime.getNodes()) {
                n.updateConnections();
            }

            // Test all messages still being sent for interference
            if (Configuration.isInterference()) {
                SinalgoRuntime.getPacketsInTheAir().testForInterference();
            }

            // Perform the step for each node
            try {
                for (Node n : SinalgoRuntime.getNodes()) {
                    n.step();
                }
            } catch (WrongConfigurationException wCE) {
                Main.minorError(wCE); // in gui, a popup is shown. in batch, exits.
                if (Global.isGuiMode()) {
                    this.getRuntime().getGUI().redrawGUINow();
                    this.getRuntime().getGUI().setStartButtonEnabled(true);
                }
                Global.setRunning(false);
                return;
            }

            Global.getCustomGlobal().postRound();

            if (Global.isGuiMode()) { // redraw the graph all 'refreshRate' Steps except the last
                if ((i % this.getRefreshRate()) == (this.getRefreshRate() - 1)) {
                    if (i != this.getNumberOfRounds() - 1) {
                        this.getRuntime().getGUI().redrawGUINow(); // this is a SYNCHRONOUS call to redraw the graph!
                    }
                }
                this.getRuntime().getGUI().setRoundsPerformed((int) (Global.getCurrentTime()));
            }

            // test whether the application should exit
            if (Global.getCustomGlobal().hasTerminated()) {
                if (Global.isGuiMode() && !Configuration.isExitOnTerminationInGUI()) { // do not quit GUI mode
                    this.getRuntime().getGUI().redrawGUINow();
                    this.getRuntime().getGUI().setStartButtonEnabled(true);

                    Global.setRunning(false);
                    return;
                }
                if (LogL.HINTS) {
                    Date tem = new Date();
                    long time = tem.getTime() - Global.getStartTime().getTime();
                    Global.getLog().logln(
                            "Termination criteria fulfilled at round " + Global.getCurrentTime() + " after " + time + " ms");
                    Global.getLog().logln(
                            "Hint: Sinalgo terminated because the function 'hasTerminated()' in CustomGlobal returned true.\n");
                }
                Main.exitApplication(); // exit the application
            }

            Global.setNumberOfMessagesOverAll(Global.getNumberOfMessagesOverAll() + Global.getNumberOfMessagesInThisRound());

            if (LogL.ROUND_DETAIL) {
                Global.getLog().logln("Round " + (Global.getCurrentTime()) + " finished");
                Global.getLog().logln("In this round " + Global.getNumberOfMessagesInThisRound() + " Messages were sent");
                Global.getLog().logln("Overall " + Global.getNumberOfMessagesOverAll() + " Messages were sent\n");
            }
        }

        if (Global.isGuiMode()) {
            this.getRuntime().getGUI().redrawGUINow();
            this.getRuntime().getGUI().setStartButtonEnabled(true);
        } else { // we reached the end of a synchronous simulation in batch mode
            if (LogL.HINTS) {
                Date tem = new Date();
                long time = tem.getTime() - Global.getStartTime().getTime();
                Global.getLog().logln(
                        "Simulation stopped regularly after " + Global.getCurrentTime() + " rounds during " + time + " ms");
                Global.getLog().logln("Which makes " + (time / Global.getCurrentTime()) + " ms per round.\n");
            }
            Main.exitApplication(); // exit explicitely, s.t. CustomGlobal.onExit() is called
        }
        Global.setRunning(false);
    }
}
