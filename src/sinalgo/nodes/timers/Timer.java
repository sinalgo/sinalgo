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
package sinalgo.nodes.timers;


import sinalgo.nodes.Node;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.runtime.Runtime;
import sinalgo.runtime.events.TimerEvent;

/**
 * The superclass of all node timers.
 * <p>
 * A timer is set by a node that wishes to schedule a task in the future.
 * When the timer goes off, it calls the method fire(), which is implemented
 * in the subclass and performs the desired action.
 * <p>
 * In addition, a timer may be set globally such that the simulation may perform
 * an action at a given time. These timers are denoted <i>global timer</i>. They
 * are not associated with a particular node, and the <code>node</code> member of 
 * this class must be set to <code>null</code>. 
 */
public abstract class Timer implements Comparable<Timer> {

	/**
	 * The node that started the timer, null if the timer executes globally
	 */
	protected Node node = null;
	
	private double fireTime = 0; //The time when this timer fires.
	
	/**
	 * Starts this <b>global timer</b> to go off after the indicated time, where the 
	 * time is specified relative to the current time.
	 * <p>
	 * In synchrone mode, set the relative time to 1 to have the timer go off in the following round.  
	 * @param relativeTime The time in which the timer should go off. relativeTime must be greater than 0. 
	 */
	public final void startGlobalTimer(double relativeTime) {
		if(relativeTime <= 0) {
			Main.fatalError("A relative time indicating when a timer should start must be strictly positive.");
		}
		node = null;
		fireTime = Global.currentTime + relativeTime;
		if(Global.isAsynchronousMode) {
			Runtime.eventQueue.insert(TimerEvent.getNewTimerEvent(this, fireTime));
		} else {
			Global.customGlobal.globalTimers.add(this);
		}
	}
	
	/**
	 * Starts this timer to go off after a certain time, where the time is specified
	 * relative to the current time.
	 * <p>
	 * In synchrone mode, set the relative time to 1 to have the timer go off in the following round. 
	 * @param relativeTime The time in which the timer should go off. relativeTime must be greater than 0. 
	 * @param n The node that started the timer and on which the timer will be fired.
	 */
	public final void startRelative(double relativeTime, Node n){
		if(relativeTime <= 0) {
			Main.fatalError("A relative time indicating when a timer should start must be strictly positive.");
		}
		node = n;
		fireTime = Global.currentTime + relativeTime;
		if(Global.isAsynchronousMode){
			Runtime.eventQueue.insert(TimerEvent.getNewTimerEvent(this, fireTime));
		}	else {
			node.getTimers().add(this);
		}
	}
	
	/**
	 * Starts this timer to go off at the specified time, where the time
	 * is given absolute.
	 *  
	 * @param absoluteTime The (absolute) time when the timer should goes off. This time 
	 * must be strictly greater than the current time.
	 * @param n The node that started the timer and on which the timer will be fired.
	 */
	public final void startAbsolute(double absoluteTime, Node n){
		if(absoluteTime <= Global.currentTime){
			Main.fatalError("The absolute time when a timer goes off must be strictly larger than the current time.");
		}
		node = n;
		fireTime = absoluteTime;
		if(Global.isAsynchronousMode){
			Runtime.eventQueue.insert(TimerEvent.getNewTimerEvent(this, fireTime));
		} else {
			node.getTimers().add(this);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Timer t) {
		return Double.compare(fireTime, t.fireTime);
	}
	
	/**
	 * Returns the time this timer goes off.
	 * @return The time this timer goes off.
	 */
	public final double getFireTime(){
		return fireTime;
	}
	
	/**
	 * @return True if the timer is set for a node, false
	 * if this timer is for the framework. 
	 */
	public final boolean isNodeTimer() {
		return node != null;
	}
	
	/**
	 * @return The node on which this timer executes, null if the timer
	 * is set for the framework. 
	 */
	public final Node getTargetNode() {
		return node;
	}
	
	/**
	 * When the timer goes off, it calls this method, which executes the desired task.
	 * Overwrite this method in your subclass to execute the actions that should happen
	 * when the timer goes off.
	 * <p>
	 * You may access the <code>node</code> member of this class to refer
	 * to the node on which the timer is handled. 
	 */
	public abstract void fire();
	
}
