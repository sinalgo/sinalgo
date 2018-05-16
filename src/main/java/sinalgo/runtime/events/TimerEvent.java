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
package sinalgo.runtime.events;

import lombok.Getter;
import lombok.Setter;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.nodes.Node;
import sinalgo.nodes.timers.Timer;
import sinalgo.runtime.Global;
import sinalgo.tools.logging.Logging;

import java.util.Stack;

/**
 * A event representing the fireing of a timer.
 */
public class TimerEvent extends Event {

    private static Stack<TimerEvent> unusedTimerEvents = new Stack<>();

    @Getter
    @Setter
    private static int numTimerEventsOnTheFly;

    public static int getNumFreedTimerEvents() {
        return unusedTimerEvents.size();
    }

    public static void clearUnusedTimerEvents() {
        unusedTimerEvents.clear();
    }

    /**
     * The timer this event is generated for. This timer fires when the event is
     * scheduled.
     */
    @Getter
    @Setter
    private Timer timer;

    /**
     * Creates a TimerEvent for the given timer, a given time and a node. This event
     * represents the event that happens, when timer fires at time on eventNode.
     *
     * @param timer The timer that will fire.
     * @param time  The time the timer will fire.
     */
    private TimerEvent(Timer timer, double time) {
        super(time);
        this.setTimer(timer);
    }

    /**
     * Creates a new packetEvent. Takes it from the eventPool if it contains one and
     * creates a new one otherwise.
     *
     * @param timer The imer that fires when this event fires.
     * @param time  The time this event is scheduled to.
     * @return An instance of PacketEvent
     */
    public static TimerEvent getNewTimerEvent(Timer timer, double time) {
        TimerEvent te;
        if (unusedTimerEvents.size() > 0) {
            te = unusedTimerEvents.pop();
            if (te.getTimer() != null) { // sanity check
                throw new SinalgoFatalException(Logging.getCodePosition()
                        + " TimerEvent factory failed! About to return a timer-event that was already returned. (Probably, free() was called > 1 on this timer event.)");
            }
            te.setTimer(timer);
            te.setTime(time);
            te.setID(getNextFreeID());
        } else {
            te = new TimerEvent(timer, time);
        }
        setNumTimerEventsOnTheFly(getNumTimerEventsOnTheFly() + 1);
        return te;
    }

    /**
     * Frees the this event. Puts it into the event pool.
     */
    @Override
    public void free() {
        this.setTimer(null);
        unusedTimerEvents.push(this);
        setNumTimerEventsOnTheFly(getNumTimerEventsOnTheFly() - 1);
    }

    @Override
    public void handle() {
        // a timer fires in the asynchronous case
        this.getTimer().fire();
    }

    @Override
    public void drop() {
        // nothing to do
    }

    @Override
    public String toString() {
        return "TimerEvent";
    }

    @Override
    public String getEventListText(boolean hasExecuted) {
        if (this.getTimer().isNodeTimer()) {
            if (hasExecuted) {
                return "Timer at node " + this.getTimer().getTargetNode().getID();
            } else {
                return "TE (Node:" + this.getTimer().getTargetNode().getID() + ", Time:" + this.getExecutionTimeString(4) + ")";
            }
        } else {
            if (hasExecuted) {
                return "Global Timer";
            } else {
                return "GTE (Time:" + this.getExecutionTimeString(4) + ")"; // it is a global timer event
            }
        }
    }

    @Override
    public String getEventListToolTipText(boolean hasExecuted) {
        if (this.getTimer().isNodeTimer()) {
            if (hasExecuted) {
                return "The timer fired at node " + this.getTimer().getTargetNode().getID() + "\nThe type of the timer was "
                        + Global.toShortName(this.getTimer().getClass().getName());
            } else {
                return "At time " + this.getTime() + " a timer fires at node " + this.getTimer().getTargetNode().getID()
                        + "\nThe type of the timer is " + Global.toShortName(this.getTimer().getClass().getName());
            }
        } else { // a global timer
            if (hasExecuted) {
                return "A global timer fired. Its type was " + Global.toShortName(this.getTimer().getClass().getName());
            } else {
                return "At time " + this.getTime() + " a global timer fires.\nThe type of the timer is "
                        + Global.toShortName(this.getTimer().getClass().getName());
            }
        }
    }

    @Override
    public Node getEventNode() {
        return this.getTimer().getTargetNode();
    }

    @Override
    public boolean isNodeEvent() {
        return this.getTimer().isNodeTimer();
    }

}
