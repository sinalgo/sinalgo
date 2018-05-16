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
import sinalgo.nodes.Node;
import sinalgo.runtime.events.Event;
import sinalgo.runtime.events.EventQueue;
import sinalgo.tools.logging.LogL;

/**
 * This is the asynchronous SinalgoRuntime Thread that executes the simulation in the
 * Asynchronous case. It handles the global event-queue and takes one event
 * after the other out of it and handles it.
 */
public class AsynchronousRuntimeThread extends Thread {

    /**
     * The number of events to be executed in this run. Has to be set before the
     * thread is started.
     */
    @Getter
    @Setter
    private long numberOfEvents;

    /**
     * Indicates whether the connectivity is initialized or not. In Asynchronous
     * mode the connectivity is generated once at startup and then it does not
     * change anymore.
     */
    private static boolean connectivityInitialized;

    /**
     * The number events the gui will be redrawn after.
     */
    @Getter
    @Setter
    private long refreshRate;

    @Getter(AccessLevel.PRIVATE)
    private GUIRuntime runtime;

    private static Node lastEventNode;

    /**
     * The Condtructor for the AsynchronousRuntimeThread creating an instancs with a
     * given GUIRuntime.
     *
     * @param runtime The GUIRuntime instance this thread is created for.
     */
    AsynchronousRuntimeThread(GUIRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * The Condtructor for the AsynchronousRuntimeThread creating an instance of
     * without a given runtime. This call is absolutely the same as calling
     * AsynchronousRuntimeThread(null)
     */
    AsynchronousRuntimeThread() {
        this.runtime = null;
    }

    /**
     * Determines which nodes are connected according to the connectivity model.
     */
    static void initializeConnectivity() {
        connectivityInitialized = true;
        for (Node n : SinalgoRuntime.getNodes()) {
            n.getConnectivityModel().updateConnections(n);
        }
    }

    @Override
    public void run() {
        Global.setRunning(true);

        Event event = null;

        if (!connectivityInitialized && Configuration.isInitializeConnectionsOnStartup()) {
            initializeConnectivity();
        }

        for (long i = 0; i < this.getNumberOfEvents(); i++) {
            // In GUI-mode, check whether ABORT was pressed.
            if (this.getRuntime() != null && this.getRuntime().isAbort()) {
                this.getRuntime().setAbort(false);
                break;
            }
            if (event != null) {
                event.free(); // free the previous event
            }
            event = SinalgoRuntime.getEventQueue().getNextEvent(); // returns null if there is no further event

            if (event == null && Configuration.isHandleEmptyEventQueue()) {
                Global.getCustomGlobal().handleEmptyEventQueue();
                // and try again
                event = SinalgoRuntime.getEventQueue().getNextEvent(); // returns null if there is no further event
            }
            if (event == null) {
                Global.getLog().logln(LogL.EVENT_QUEUE_DETAILS,
                        "There is no event to be executed. Generate an event manually.");
                if (!Global.isGuiMode()) {
                    Main.exitApplication(); // we're in batch mode and there are no more events -> exit
                }
            }

            if (event != null) {
                Global.setCurrentTime(event.getTime());
                event.handle(); // does not yet free the event
            }

            if (Global.isGuiMode()) {
                if (i % this.refreshRate == this.refreshRate - 1 && i + 1 < this.numberOfEvents) { // only perform if we continue with
                    // more events
                    if (lastEventNode != null) {
                        lastEventNode.highlight(false);
                    }
                    if (event != null) {
                        if (event.isNodeEvent()) {
                            event.getEventNode().highlight(true);
                        }
                        lastEventNode = event.getEventNode(); // may be null, if the event does not execute on a node
                    }
                    this.getRuntime().getGUI().setRoundsPerformed((Global.getCurrentTime()), EventQueue.getEventNumber());
                    this.getRuntime().getGUI().setCurrentlyProcessedEvent(event); // does not store the event
                    this.getRuntime().getGUI().redrawGUINow();
                }
            }
        }

        if (Global.isGuiMode()) {
            this.getRuntime().getGUI().setRoundsPerformed((Global.getCurrentTime()), EventQueue.getEventNumber());
            if (event != null) {
                this.getRuntime().getGUI().setCurrentlyProcessedEvent(event);

                if (lastEventNode != null) {
                    lastEventNode.highlight(false);
                }
                if (event.isNodeEvent()) {
                    event.getEventNode().highlight(true);
                }
                lastEventNode = event.getEventNode();// may be null, if the event does not execute on a node
            } else {
                this.getRuntime().getGUI().setCurrentlyProcessedEvent(null);
                if (lastEventNode != null) {
                    lastEventNode.highlight(false);
                }
            }
            this.getRuntime().getGUI().redrawGUINow();
            this.getRuntime().getGUI().setStartButtonEnabled(true);
        } else { // Batch mode
            // we're in batch mode and the required number of events have been handled -> exit
            Main.exitApplication();
        }
        if (event != null) {
            event.free();
        }
        Global.setRunning(false);
    }
}
