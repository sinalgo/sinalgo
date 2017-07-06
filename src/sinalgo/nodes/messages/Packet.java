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
package sinalgo.nodes.messages;


import java.util.Stack;

import sinalgo.nodes.Node;
import sinalgo.nodes.edges.Edge;
import sinalgo.runtime.Main;
import sinalgo.tools.logging.Logging;
import sinalgo.tools.storage.DoublyLinkedList;
import sinalgo.tools.storage.DoublyLinkedListEntry;


/* TODO: base packet delivery on edge id
 *  2) While a packet is being sent, there needs to be a check in every round that the edge it uses is 
 *     still in use. If this is not the case, it must not be delivered. But as noted in point 1), it must 
 *     remain there until the time it would have been delivered. 
 *     
 *     NOTE: This test must be well coordinated with the update of the connections due to mobility & co. 
 *     Such that if a node has a connection to a neighbor, it can always send a message that arrives in the
 *     next round, and not the mobility can break up this connection and fail the message.
 *     
 *     Actually, testing whether the edge was there all the time is quite easy! Store the edge in the header, 
 *     then when the message arrives, test whether the edge still exists (and is the same object) => compare with ==. 
 */ 


/**
 * A packet encapsulates a message and header information. This class should
 * remain framework internal, the project developers should not even be aware
 * about this class.
 * <p>This class is final. The user of the framework should only subclass the message class
 * to add user specific behavior. 
 */
public final class Packet implements DoublyLinkedListEntry, Comparable<Packet> {
	
	/**
	 * The time the message arrives its destination.
	 */
	public double arrivingTime;
	
	/**
	 * The time of the round when this message was sent.
	 */
	public double sendingTime;
	
	/**
	 * The edge over which this message is sent, may be null if the message
	 * is sent independent of an edge or if the edge was deleted while
	 * the message is being sent.  
	 */
	public Edge edge; 
	
	/**
	 * The intensity of the message. I.e. interference can depend on the intensity of
	 * the message.
	 */
	public double intensity;

	/**
	 * The destination of this packet.
	 */
	public Node destination;

	/**
	 * The origin of this packet.
	 */
	public Node origin;
	
	/**
	 * True if the message will be received by the receiver, otherwise false.
	 * 
	 * This flag is needed to invalidate the packet due to interference or link reliability. Even
	 * if such situations cause the packet from not being delivered, it must remain around until it
	 * arrives at the destination to cause interference with auther packets. 
	 */
	public boolean positiveDelivery;
	
	public enum PacketType {
		UNICAST,
		MULTICAST,
		DUMMY // packets sent for other reasons, e.g. to simulate interference
	}
	
	/**
	 * The type of this message (unicast, multicast or dummy)
	 */
	public PacketType type; 
	
	/**
	 * Sets the positiveDelivery flag of this packet to false such that this packet is not delivered.
	 */
	public void denyDelivery() {
		positiveDelivery = false;
	}
	
	/**
	 * The message to send.
	 */
	public Message message;
	

	
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	// Framework specific methods and member variables
	// => You should not need to modify/overwrite/call/use any of these members or methods
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	
	public static int numPacketsOnTheFly = 0; // number of packets in the system, not yet freed
	
	/**
	 * Constructor to create new Packet objects. If possible, this method returns
	 * a recycled packet. 
	 * @param msg The message to create the packet for.
	 * @return A Packet instance, either a new one or a recycled one.
	 */
	public static Packet fabricatePacket(Message msg){
		numPacketsOnTheFly++;
		if(freePackets.empty()) {
			Packet p = new Packet(msg);
			synchronized(issuedPackets) {
				issuedPackets.append(p);
			}
			return p;
		}
		else{
			Packet rP = freePackets.pop();
			if(rP.message != null) {
				Main.fatalError(Logging.getCodePosition() + " Packet factory failed! About to return a packet that was already returned. (Probably, free() was called > 1 on this packet.)");
			}
			rP.ID = getNextFreeID();
			rP.message = msg;
			synchronized(issuedPackets) {
				issuedPackets.append(rP);
			}
			return rP;
		}
	}
	
	/**
	 * This method marks this packet as unused. This means that it adds itself to the 
	 * packet pool and can thus be recycled by the fabricatePacket-method.
	 * 
	 * @param pack The packet to free.
	 */
	public static void free(Packet pack){
		synchronized(issuedPackets) {
			if(!issuedPackets.remove(pack)) { // nothing happens if the packet is not in the list
				System.err.println(Logging.getCodePosition() + " Bug in packet factory. Please report this error if you see this line.\n\n\n");
			}
		}
		numPacketsOnTheFly--;
		pack.destination = null;
		pack.origin = null;
		pack.edge = null;
		pack.message = null;
		freePackets.push(pack);
	}
	
	/**
	 * The internal id of this packet. 
	 */
	public long ID = 0;
	
	//the next id to give to a packet
	private static long nextID = 1;

	/**
	 * @return The next Free ID to be used.
	 */
	private static long getNextFreeID(){
		if(nextID == 0){
			Main.minorError("The Packet ID counter overflowed. It is likely that the simulation continues correctly despite of this overlow.");
		}
		return nextID++;//implicit post-increment
	}
	
	/**
	 * This is a stack containing all the unused packet instances. To reduce the garbage collection time,
	 * used Packets are not destroyed but are added to a Packet pool. When a new instance is requested, 
	 * the system only creates a new instance, when the stack is empty.
	 */
	private static Stack<Packet> freePackets = new Stack<Packet>();
	

	/**
	 * List of all packet-objects issued and not yet returned with free. 
	 * In theory, all of these packets are 'on the fly', i.e. being sent.
	 * In practice, some of the packets are just being created / destroyed,
	 * or are kept for further processing.
	 * <p>
	 * Note that this list is not equivalent to the 'packetsInTheAir' list used
	 * for interference! This list simply contains all packets objects that are 
	 * currently used.
	 * For now, this member is only experimental.
	 * <p>
	 * Whenever accessing this member, you should synchronize on this member
	 */
	public static DoublyLinkedList<Packet> issuedPackets = new DoublyLinkedList<Packet>(true);
	
	
	public static void clearUnusedPackets() {
		freePackets.clear();
	}
	
	/**
	 * @return The number of packets ready to be reused.
	 */
	public static int getNumFreedPackets() {
		return freePackets.size();
	}
	

	/**
	 * The constructor for the Packet class. This constructor is private to ensure nobody uses it. The 
	 * proper way to create a Packet is to get an instance by calling the fabricatePacket() method.
	 *
	 * @param msg The message to create a packet for.
	 */
	private Packet(Message msg){
		message = msg;
		ID = getNextFreeID();
	}

	/* (non-Javadoc)
	 * @see sinalgo.tools.storage.DoublyLinkedListEntry#getDoublyLinkedListFinger()
	 */
	public DLLFingerList getDoublyLinkedListFinger() {
		return dllFingerList;
	}
	// the DLLE entry for the DoublyLinkedList
	private DLLFingerList dllFingerList = new DLLFingerList();
	
	/**
	 * Compare method to sort lists of packets according to their arriving time.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Packet p) {
		return Double.compare(arrivingTime, p.arrivingTime);
	}
}
