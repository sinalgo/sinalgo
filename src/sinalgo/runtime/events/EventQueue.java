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


import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.Vector;

import sinalgo.nodes.Node;
import sinalgo.nodes.edges.Edge;

/**
 * The queue that stores the events of the asynchronous mode. The entries (events) are sorted according to 
 * their execution time so that the first node in the list is the next to execute.
 */
@SuppressWarnings("serial")
public class EventQueue extends TreeSet<Event> {
	
	private Vector<EventQueueListener> listeners = new Vector<EventQueueListener>(5);
	
	/**
	 * The number of events that have been taken out of the eventQueue. Gets automatically
	 * reset with every getNextEvent call.
	 */
	public static int eventNumber = 0;
	
	/**
	 * The constructor for the EventQueue. Constructs a TreeSet with the correct Comparator.
	 */
	public EventQueue(){
		super(new EventComparator());
	}
	
	/**
	 * Returns the next event in the queue.
	 * 
	 * @return The next event in the queue, null if there is none.
	 */
	public Event getNextEvent(){
		try{
			Event e = this.first();
			this.remove(e);
			eventNumber ++;
			notifyListeners();
			return e;
		}
		catch(NoSuchElementException nSEE){
			notifyListeners();
			return null;
		}
	}
	
	/**
	 * Inserts the event into the queue according to its execution time.
	 * 
	 * @param e The event to add to the queue.
	 */
	public void insert(Event e){
		this.add(e);
		notifyListeners();
	}
	
	/**
	 * Removes all the Events for this node. This method is used when a node is removed
	 * from the system, all events in the system that are in the queue have to be removed
	 * too.
	 * <p>
	 * Messages sent by this node are invalidated.
	 * 
	 * @param n The node for which all events are deleted
	 */
	public void removeAllEventsForThisNode(Node n){
		boolean changed = false;
		
		Iterator<Event> eventIter = iterator();
		while(eventIter.hasNext()){
			Event e = eventIter.next();
			if(e.isNodeEvent() && n.equals(e.getEventNode())) {
				// an event that would execute on this node
				eventIter.remove();
				e.free(); // free the event (and also the packet, if it's a packet event)
				changed = true;
			} else {
				// test whether it's a msg event sent by this node, then invalidate
				if(e instanceof PacketEvent) {
					PacketEvent pe = (PacketEvent) e;
					if(pe.packet.origin.equals(n)) {
						pe.packet.denyDelivery();
					}
				}
			}
		}
		
		if(changed){
			notifyListeners();
		}
	}
	
	/**
	 * Invalidates all PacketEvents for that Edge. This method
	 * does not remove the packet events, but only invalidates 
	 * them. This way, the packets are removed from the 'packetsInTheAir' 
	 * buffer at the right time. 
	 * 
	 * @param toDelFor The edge to remove all the events for.
	 */
	public void invalidatePacketEventsForThisEdge(Edge toDelFor){
		boolean changed = false;
		
		Iterator<Event> eventIter = iterator();
		while(eventIter.hasNext()){
			Event eventInQueue = eventIter.next();
			if(eventInQueue instanceof PacketEvent){
				PacketEvent pe = (PacketEvent) eventInQueue;
				if(pe.packet.edge != null && toDelFor.getID() == pe.packet.edge.getID()) {
					pe.packet.positiveDelivery = false;
					pe.packet.edge = null; // the edge may not exist anymore
					changed = true;
				}
			}
		}
		
		if(changed){
			notifyListeners();
		}
	}
	
	/**
	 * Removes all events related with a node (packet events and node-timer events).
	 */
	public void pruneAllNodeEvents() {
		EventQueue eq = new EventQueue();
		eq.listeners = this.listeners; // inherit the listeners

		Iterator<Event> eventIter = iterator();
		while(eventIter.hasNext()){
			Event e = eventIter.next();
			if(e.isNodeEvent()) {
				e.free(); // also frees a corresponding packet event, inclusive the packet
			} else {
				eq.add(e);
			}
		}
		super.clear(); // kill this set
		sinalgo.runtime.Runtime.eventQueue = eq; // replace the event queue
		notifyListeners(); 
	}
	
	/**
	 * Removes all events without executing them 
	 */
	public void dropAllEvents() {
		Iterator<Event> eventIter = iterator();
		while(eventIter.hasNext()){
			Event e = eventIter.next();
			e.drop();
			e.free(); // free the event resources
		}
		super.clear(); // remove all events
		notifyListeners(); 
	}
	
	/**
	 * Removes a single event from this queue without executing it
	 * @param e The event to remove
	 */
	public void dropEvent(Event e) {
		if(this.remove(e)) {
			e.drop();
			e.free();	
		}
		notifyListeners(); 
	}
	
	/**
	 * Triggers an notification to all the registered listeners. Normally this is used internally only, but 
	 * there are some special cases, where the eventQueue and the queue does not notify it (interference)
	 */
	public void notifyListeners(){
		for(int i = 0; i < listeners.size(); i++){
			listeners.elementAt(i).eventQueueChanged();
		}
	}
	
	/**
	 * Adds the specified eventQueueListener to the listeners
	 * 
	 * @param eqList The eventqueuelistener to add
	 */
	public void addEventQueueListener(EventQueueListener eqList){
		listeners.add(eqList);
	}
	
	/**
	 * Removes the specified eventQueueListener to the listeners
	 * 
	 * @param eqList The eventqueuelistener to remove
	 */
	public void removeEventQueueListener(EventQueueListener eqList){
		listeners.remove(eqList);
	}
	
	public Iterator<Event> iterator(){
		return new EventIter(super.iterator());
	}
	
	/**
	 * Forwards all the calls to the internal iterator. All it changes compared to the standard
	 * iterator is, that it frees the event when removing it.
	 */
	private class EventIter implements Iterator<Event>{

		private Iterator<Event> iter;
		private Event current;
		
		private EventIter(Iterator<Event> iter){
			this.iter = iter;
		}
		
		public boolean hasNext() {
			return iter.hasNext();
		}

		public Event next() {
			current = iter.next();
			return current;
		}

		public void remove() {
			iter.remove();
		}
	}
	
	private static class EventComparator implements Comparator<Event>{

		public int compare(Event arg0, Event arg1) {
			// the comparison is twofold: first, we sort based on the time when the event fires
			// if the time is equal for two events, the event that was created earlier is defined
			// to be smaller. (This approach guarantees a consistent ordering). 
			if(arg0.time == arg1.time){
				if(arg0.id > arg1.id){
					return 1;
				}
				else if(arg0.id < arg1.id){
					return -1;
				}
				else{
					return 0;
				}
			}
			else if(arg0.time - arg1.time < 0){
				return -1;
			}
			else{
				return 1;
			}
		}
		
	}
}
