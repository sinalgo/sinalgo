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

import sinalgo.nodes.Node;
import sinalgo.tools.storage.DoublyLinkedListEntry;

/**
 * An event for the asynchronous mode. Concrete events extend this class.
 */
public abstract class Event implements DoublyLinkedListEntry {

	/**
	 * the time this event happens.
	 */
	public double time;
	
	//the id of this event (this id is just used interanally for ordering the events)
	long id = 0;
	protected static long nextId = 1;
	
	/**
	 * Returns a string representation of the time when this event
	 * executes, truncated to the given number of digits.  
	 * @param digits The number of digits to display
	 * @return A truncated string representation of the time when
	 * this event will execute. 
	 */
	public String getExecutionTimeString(int digits) {
		if(digits > 10) {
			return Double.toString(time);
		}
		double factor = Math.pow(10, digits);
		double temp = Math.round(time * factor) / factor;
		return Double.toString(temp);
	}
	
	/**
	 * Creates an event with a given time to execute and a given node to execute on.
	 *
	 * @param time The time the event will pappen.
	 */
	protected Event(double time){
		this.time = time;
		this.id = nextId++;//impicit increment
	}
	
	/**
	 * @return The text to be displayed in the extended control panel
	 * for this event.
	 * @param hasExecuted True if the event has already executed, otherwise false.
	 */
	public abstract String getEventListText(boolean hasExecuted);

	/**
	 * @return The tooltip text to be displayed in the extended control panel
	 * for this event.
	 * @param hasExecuted True if the event has already executed, otherwise false.
	 */
	public abstract String getEventListToolTipText(boolean hasExecuted);
	
	/**
	 * @return True if this event is associated with a node (e.g. receiver of a 
	 * packet or the handler of a timer). Otherwise, if this event is framework 
	 * specific, this method returns false.
	 */
	public abstract boolean isNodeEvent();

	/**
	 * @return The node for which the event is scheduled (receiver of a packet, 
	 * handler of a timer event), null if the event is not associated with a node.
	 */
	public abstract Node getEventNode();
	
	/**
	 * Frees the this event. Puts it into the event pool.
	 */
	public abstract void free();
	
	/**
	 * Called when this event is removed before it was handled.
	 * This method does NOT free the event.
	 */
	public abstract void drop();
	
	/**
	 * Executes this event, but does not yet free its resources or this event.
	 */
	public abstract void handle();

//	 the DLLE entry for the DoublyLinkedList
	private DLLFingerList dllFingerList = new DLLFingerList();
	public DLLFingerList getDoublyLinkedListFinger() {
		return dllFingerList;
	}
}
