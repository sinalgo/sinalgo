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

import sinalgo.configuration.Configuration;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.NackBox;
import sinalgo.nodes.messages.Packet;
import sinalgo.nodes.messages.Packet.PacketType;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.runtime.Runtime;
import sinalgo.tools.logging.Logging;

/**
 * The event that represents that a message is reaching its destination.
 */
public class PacketEvent extends Event {
	
	private static Stack<PacketEvent> unusedPacketEvents = new Stack<PacketEvent>();
	public static int numPacketEventsOnTheFly = 0;
	
	public static int getNumFreedPacketEvents() {
		return unusedPacketEvents.size();
	}
	
	public static void clearUnusedPacketEvents() {
		unusedPacketEvents.clear();
	}
	
	/**
	 * The packet this event was generated for. This packet (or to be precisely its message) reaches at the time
	 * this event is scheduled.
	 */
	public Packet packet;
	
	/**
	 * Creates a new PacketEvent for a given packet, a given time and a given node. This event
	 * represents that the packet reaches eventNode at time. 
	 *
	 * @param packet The packet that reaches its target.
	 * @param time The time the packet reaches its target.
	 * @param eventNode The node the packet reaches.
	 */
	private PacketEvent(Packet packet, double time){
		super(time);
		this.packet = packet;
	}
	
	/**
	 * Creates a new packetEvent. Takes it from the eventPool if it contains one and creates a new one otherwise.
	 * 
	 * @param packet The packet that arrives when this event fires.
	 * @param time The time this event is scheduled to.
	 * @return An instance of PacketEvent
	 */
	public static PacketEvent getNewPacketEvent(Packet packet, double time){
		PacketEvent pe = null;
		if(unusedPacketEvents.size() > 0){
			pe = unusedPacketEvents.pop();
			if(pe.packet != null) { // sanity check
				Main.fatalError(Logging.getCodePosition() + " PacketEvent factory failed! About to return a packet-event that was already returned. (Probably, free() was called > 1 on this packet event.)");
			}
			pe.packet = packet;
			pe.time = time;
			pe.id = nextId++; //implicit increment
		} else {
			pe = new PacketEvent(packet, time);
		}
		numPacketEventsOnTheFly++;
		return pe;
	}
	
	/**
	 * Frees the this event and the corresponding packet. Puts it into the event pool.
	 */
	public void free(){
		if(packet != null) {
			Packet.free(packet);
			this.packet = null;
		}
		unusedPacketEvents.push(this);
		numPacketEventsOnTheFly--;
	}
	
	// Two static objects to prevent from allocating them all over again
	private static Inbox inbox = new Inbox();
	private static NackBox nAckBox = new NackBox();
	
	@Override
	public void handle() {
		// the arrival of a packet in the asynchronous case
		if(Configuration.interference){
			Runtime.packetsInTheAir.performInterferenceTestBeforeRemove();
			Runtime.packetsInTheAir.remove(packet);
		}
		if(packet.edge != null){
			packet.edge.removeMessageForThisEdge(packet.message);
		}
		if(packet.positiveDelivery){
			packet.destination.handleMessages(inbox.resetForPacket(packet));
		} else {
			if(Configuration.generateNAckMessages && packet.type == PacketType.UNICAST) {
				packet.origin.handleNAckMessages(nAckBox.resetForPacket(packet));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.runtime.events.Event#drop()
	 */
	public void drop() {
		// similar to the arrival of a packet in the asynchronous case
		if(Configuration.interference){
			Runtime.packetsInTheAir.remove(packet);
		}
		if(packet.edge != null){
			packet.edge.removeMessageForThisEdge(packet.message);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		return "PacketEvent";
	}

	@Override
	public String getEventListText(boolean hasExecuted) {
		if(hasExecuted) {
			return "Packet at node " + packet.destination.ID + (packet.positiveDelivery ? " (delivered)" : " (dropped)"); 
		} else {
			return "PE (Node:" + packet.destination.ID + ", Time:" + getExecutionTimeString(4) + ")";
		}
	}

	@Override
	public String getEventListToolTipText(boolean hasExecuted) {
		if(hasExecuted) {
			return "The type of the message is: " + Global.toShortName(packet.message.getClass().getName()) + "\n" +
			(packet.positiveDelivery ? "The message was delivered" : "The message was dropped.");
		} else {
			return "At time " + time + " a message reaches node " + packet.destination.ID + "\n" +
			"The type of the message is: " + Global.toShortName(packet.message.getClass().getName()) + "\n" +
			(packet.positiveDelivery ? "Until now it seems that the message will reach its destination." : "The message has already been disturbed and will not reach its destination.");
		}
	}

	@Override
	public Node getEventNode() {
		return packet.destination;
	}

	@Override
	public boolean isNodeEvent() {
		return true;
	}
}
