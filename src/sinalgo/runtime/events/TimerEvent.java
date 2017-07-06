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
package sinalgo.runtime.events;


import java.util.Stack;

import sinalgo.nodes.Node;
import sinalgo.nodes.timers.Timer;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.tools.logging.Logging;

/**
 * A event representing the fireing of a timer.
 */
public class TimerEvent extends Event {

	private static Stack<TimerEvent> unusedTimerEvents = new Stack<TimerEvent>();
	public static int numTimerEventsOnTheFly = 0;
	
	public static int getNumFreedTimerEvents() {
		return unusedTimerEvents.size();
	}
	
	public static void clearUnusedTimerEvents() {
		unusedTimerEvents.clear();
	}
	
	/**
	 * The timer this event is generated for. This timer fires when the event is scheduled.
	 */
	public Timer timer;
	
	/**
	 * Creates a TimerEvent for the given timer, a given time and a node.
	 * This event represents the event that happens, when timer fires at
	 * time on eventNode.
	 *
	 * @param timer The timer that will fire.
	 * @param time The time the timer will fire.
	 * @param eventNode The node the timer will fire on.
	 */
	private TimerEvent(Timer timer, double time) {
		super(time);
		this.timer = timer;
	}
	
	/**
	 * Creates a new packetEvent. Takes it from the eventPool if it contains one and creates a new one otherwise.
	 * 
	 * @param timer The imer that fires when this event fires.
	 * @param time The time this event is scheduled to.
	 * @return An instance of PacketEvent
	 */
	public static TimerEvent getNewTimerEvent(Timer timer, double time){
		TimerEvent te = null;
		if(unusedTimerEvents.size() > 0){
			te = unusedTimerEvents.pop();
			if(te.timer != null) { // sanity check
				Main.fatalError(Logging.getCodePosition() + " TimerEvent factory failed! About to return a timer-event that was already returned. (Probably, free() was called > 1 on this timer event.)");
			}
			te.timer = timer;
			te.time = time;
			te.id = nextId++;//implicit increment
		}	else {
			te = new TimerEvent(timer, time);
		}
		numTimerEventsOnTheFly++;
		return te;
	}
	
	/**
	 * Frees the this event. Puts it into the event pool.
	 */
	public void free(){
		this.timer = null; 
		unusedTimerEvents.push(this);
		numTimerEventsOnTheFly --;
	}

	@Override
	public void handle() {
		// a timer fires in the asynchronous case
		timer.fire();
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.runtime.events.Event#drop()
	 */
	public void drop() {
		// nothing to do
	}
	
	public String toString(){
		return "TimerEvent";
	}

	@Override
	public String getEventListText(boolean hasExecuted) {
		if(timer.isNodeTimer()) {
			if(hasExecuted) {
				return "Timer at node " + timer.getTargetNode().ID;
			} else {
				return "TE (Node:" + timer.getTargetNode().ID + ", Time:" + getExecutionTimeString(4) + ")";
			}
		} else {
			if(hasExecuted) {
				return "Global Timer";
			} else {
				return "GTE (Time:" + getExecutionTimeString(4) + ")"; // it is a global timer event
			}
		}
	}
	
	@Override
	public String getEventListToolTipText(boolean hasExecuted) {
		if(timer.isNodeTimer()) {
			if(hasExecuted) {
				return "The timer fired at node " + timer.getTargetNode().ID + "\nThe type of the timer was " + Global.toShortName(timer.getClass().getName()); 
			} else {
				return "At time " + time + " a timer fires at node " + timer.getTargetNode().ID + "\nThe type of the timer is " + Global.toShortName(timer.getClass().getName());
			}
		} else { // a global timer
			if(hasExecuted) {
				return "A global timer fired. Its type was " + Global.toShortName(timer.getClass().getName());
			} else {
				return "At time " + time + " a global timer fires.\nThe type of the timer is " + Global.toShortName(timer.getClass().getName());
			}
		}
	}

	@Override
	public Node getEventNode() {
		return timer.getTargetNode();
	}

	@Override
	public boolean isNodeEvent() {
		return timer.isNodeTimer();
	}

}
