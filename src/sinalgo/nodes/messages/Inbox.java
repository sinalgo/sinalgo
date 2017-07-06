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


import java.util.AbstractList;
import java.util.Iterator;

import sinalgo.nodes.Node;
import sinalgo.nodes.edges.Edge;
import sinalgo.tools.storage.ReusableIterator;

/**
 * Each node stores the messages it receives in an instance of this <code>Inbox</code> class.
 * The inbox provides an iterator-like view over the set of messages that are received
 * in the current round in synchronous simulation mode. In asynchronous simulation mode, the 
 * inbox contains only a single message. 
 * <p>
 * For each received message, this iterator stores meta-information, such as the sender of the
 * message. This meta-information is available for the packet that was last returned through 
 * the <code>next()</code> method.
 * <p>
 * In order to iterate several times over the set of packets, you may reset the inbox 
 * by calling <code>reset()</code>, <code>size()</code> returns the number of messages 
 * in the inbox. Call <code>remove()</code> to remove the message from the inbox that was 
 * returned by the last call to <code>next()</code>.
 * <p>
 * Typically, a node iterates over all messages in the inbox by writing
 * <p>
 * <code>while(inbox.hasNext()) {</code><br>
 * <code>&nbsp;&nbsp;Message msg = inbox.next();</code><br>
 * <code>&nbsp;&nbsp;// handle msg</code><br>
 * <code>}</code><br>
 * <p>  
 * <b>Calling sequence</b> In <i>synchronous mode</i>, a node
 * fills its inbox with all messages that arrive in this round at the beginning 
 * of its step(). While executing the step() it calls <code>handleMessages(Inbox inbox)</code>
 * such that the node-implementation may react to the received messages. At the 
 * end of the its step, the node empties its inbox.
 * In <i>aynchronous mode</i>, the inbox contains the (single) message whose receive-event 
 * is currently handled.     
 */
public class Inbox implements ReusableIterator<Message>, Iterable<Message> {
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	// Methods to iterate over the set of messages
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	
	public Iterator<Message> iterator() {
		return this;
	}
	
	/**
	 * Returns true if a call to <code>next()</code> will return a message.
	 * <p>
	 * Note: This inbox iterator is resetable. I.e. you may reset the iterator
	 * at any time to traverse the list of messages several times by calling <code>reset()</code>. 
	 * @return True if there is a message not yet returned, otherwise false.
	 * @see java.util.Iterator#hasNext()  
	 */
	public boolean hasNext() {
		if(packetIter != null) {
			return packetIter.hasNext();
		} else {
			if(singlePacket != null) {
				return activePacket == null;
			} else {
				return false;
			}
		}
	}

	/**
	 * Returns the next message contained in the iterator.
	 * <p>
	 * Note: This inbox iterator is resetable. I.e. you may reset the iterator
	 * at any time to traverse the list of messages several times by calling <code>reset()</code>.
	 * @see java.util.Iterator#next() 
	 */
	public Message next() {
		if(packetIter != null) {
			activePacket = packetIter.next();
		} else {
			if(activePacket == null) {
				activePacket = singlePacket;
			} else {
				activePacket = null;
			}
		}
		if(activePacket == null) {
			throw new IllegalStateException("Call to 'Inbox.next', even though hasNext() returned false.");
		}
		return activePacket.message;
	}

	/**
	 * Removes the message that was returned by the last call to <code>next()</code>.
	 * @see java.util.Iterator#remove()
	 */
	public void remove() {
		activePacket = null;
		if(packetIter != null) {
			//note that this method automatically forwards the exceptions of the original
			//remove method of the iterator.
			packetIter.remove();
		} else {
			singlePacket = null;
		}
	}
	
	/**
	 * Restores the state of this iterator to be in the initial state, such 
	 * that next() returns the first message of the list. Messages
	 * that were removed previously will not be reported anymore. 
	 */
	public void reset(){
		if(packetList == null) {
			resetForPacket(singlePacket);
		} else {
			resetForList(this.packetList);
		}
	}
	
	/**
	 * Returns the number of messages hold in this inbox.
	 * @return The number of messages hold in this inbox.
	 */
	public int size() {
		if(packetList != null) {
			return packetList.size();
		} else {
			if(singlePacket != null) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	// Meta Information about the message that was returned by the last call to next()
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	
	/**
	 * Returns the sender of the message that was returned by the last call to <code>next()</code>.
	 * @return The sender of the message that was returned by the last call to <code>next()</code>.
	 * @exception IllegalStateException If the iterator is not in a valid state. This may happen after
	 * a call to <code>remove()</code> or <code>reset()</code>, or if <code>next()</code> was never 
	 * called.
	 */
	public Node getSender(){
		if(activePacket != null){
			return activePacket.origin;
		}
		else{
			throw new IllegalStateException("Call to 'Inbox.getSender', but there is no active packet.");
		}
	}
	
	/**
	 * Returns the receiver of the message that was returned by the last call to <code>next()</code>.
	 * In most cases, this will be the node that handles the message.
	 * @return the receiver of the message that was returned by the last call to <code>next()</code>.
	 * @exception IllegalStateException If the iterator is not in a valid state. This may happen after
	 * a call to <code>remove()</code> or <code>reset()</code>, or if <code>next()</code> was never 
	 * called.
	 */
	public Node getReceiver(){
		if(activePacket != null){
			return activePacket.destination;
		}
		else{
			throw new IllegalStateException("Call to 'Inbox.getReceiver', but there is no active packet.");
		}
	}
	
	/**
	 * Returns the time at which message that was returned by the last call to <code>next()</code> is arriving.
	 * In most cases, this will be the current simulation time.
	 * @return the time at which message that was returned by the last call to <code>next()</code> is arriving.
	 * @exception IllegalStateException If the iterator is not in a valid state. This may happen after
	 * a call to <code>remove()</code> or <code>reset()</code>, or if <code>next()</code> was never 
	 * called.
	 */
	public double getArrivingTime() {
		if(activePacket != null) {
			return activePacket.arrivingTime;
		} else {
			throw new IllegalStateException("Call to 'Inbox.getArrivingTime', but there is no active packet.");
		}
	}
	
	/**
	 * Returns the intensity at which the message that was returned by the last call to <code>next()</code> was sent.
	 * @return the intensity at which the message that was returned by the last call to <code>next()</code> was sent.
	 * @exception IllegalStateException If the iterator is not in a valid state. This may happen after
	 * a call to <code>remove()</code> or <code>reset()</code>, or if <code>next()</code> was never 
	 * called.
	 */
	public double getIntensity() {
		if(activePacket != null) {
			return activePacket.intensity;
		} else {
			throw new IllegalStateException("Call to 'Inbox.getIntensitiy', but there is no active packet.");
		}
	}
	
	/**
	 * Returns the time when the message that was returned by the last call to <code>next()</code> was sent.
	 * @return the time when the message that was returned by the last call to <code>next()</code> was sent.
	 * @exception IllegalStateException If the iterator is not in a valid state. This may happen after
	 * a call to <code>remove()</code> or <code>reset()</code>, or if <code>next()</code> was never 
	 * called.
	 */
	public double getSendingTime() {
		if(activePacket != null) {
			return activePacket.sendingTime;
		} else {
			throw new IllegalStateException("Call to 'Inbox.getSendingTime', but there is no active packet.");
		}
	}
	
	/**
	 * @return The edge over which the current message reached this node. 
	 * The returned object may be null if the edge was removed in the meantime. 
	 */
	public Edge getIncomingEdge() {
		if(activePacket != null) {
			return activePacket.edge; 
		} else {
			throw new IllegalStateException("Call to 'Inbox.getSendingTime', but there is no active packet.");
		}
	}
	
	
	
	
	
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	// Framework specific methods and member variables
	// => You should not need to modify/overwrite/call/use any of these members or methods
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	
	private Iterator<Packet> packetIter; // The iterator over the packet list.
	private Packet activePacket = null; // The actual packet to return the information for.
	private AbstractList<Packet> packetList; //the packet list
	private Packet singlePacket = null; // if the inbox is initialized for a single packet, it is stored here.
	
	/**
	 * <b>This is a framework internal method. Project developers should not need to call this method.</b><br>
	 * Empties this inbox and frees all packets. (Thus, the packets may be reused for other messages 
	 * after this call.) 
	 */
	public void freePackets(){
		activePacket = null;
		if(packetList != null) {
			Iterator<Packet> packetIter = packetList.iterator();
			while(packetIter.hasNext()){
				Packet.free(packetIter.next());
			}
			packetList.clear();
		} else {
			if(singlePacket != null) {
				Packet.free(singlePacket);
				singlePacket = null;
			}
		}
	}

	/**
	 * <b>This is a framework internal method. Project developers should not need to call this method.</b><br>
	 * Constructs an inbox for a given list of packets.
	 * @param t The list of packets this inbox contains.
	 */
	public Inbox(AbstractList<Packet> t){
		resetForList(t);
	}
	
	/**
	 * <b>This is a framework internal method. Project developers should not need to call this method.</b><br>
	 * Creates an inbox for a single packet
	 * @param p The packet this inbox contains.
	 */
	public Inbox(Packet p) {
		resetForPacket(p);
	}

	/**
	 * Dummy constructor
	 */
	public Inbox() {
		resetForPacket(null);
	}
	
	/**
	 * <b>This is a framework internal method. Project developers should not need to call this method.</b><br>
	 * Resets this inbox to contain the given list of packets.
	 * @param list The list of packets to include in this inbox.
	 * @return This inbox instance.
	 */
	public Inbox resetForList(AbstractList<Packet> list){
		packetList = list;
		activePacket = null;
		packetIter = packetList.iterator();
		singlePacket = null;
		return this;
	}
	
	/**
	 * <b>This is a framework internal method. Project developers should not need to call this method.</b><br>
	 * Resets the inbox to contain a single packet.
	 * @param p The packet to include in this inbox.
	 * @return This inbox object.
	 */
	public Inbox resetForPacket(Packet p) {
		packetList = null;
		activePacket = null;
		packetIter = null;
		singlePacket = p;
		return this;
	}
}
