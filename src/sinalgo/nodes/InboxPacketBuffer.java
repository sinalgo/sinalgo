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
package sinalgo.nodes;

import sinalgo.configuration.Configuration;
import sinalgo.nodes.edges.Edge;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Packet;
import sinalgo.nodes.messages.PacketCollection;
import sinalgo.runtime.Global;
import sinalgo.runtime.Runtime;
import sinalgo.tools.storage.DoublyLinkedList;
import sinalgo.tools.storage.ReusableListIterator;

/**
 * A list implementation that holds the messages arriving at a node.
 */
public class InboxPacketBuffer extends DoublyLinkedList<Packet> implements PacketBuffer {
//		 the vector of packets that arrive in this step
	private PacketCollection arrivingPackets = new PacketCollection(); 
	private ReusableListIterator<Packet> bufferIterator = this.iterator();
	
	private Inbox inbox = null;
	
	/**
	 * The default constructor of the dllPacketBuffer-class.
	 */
	public InboxPacketBuffer(){
		super();
	}
	
	/**
	 * Creates a new instance of a DLLPacketBuffer.
	 * <p>
	 * This method lets you specify whether entries keep their finger-entry 
	 * when they are removed from this list. This may increase performance if the
	 * same entries are added and removed several times to/from this list.
	 * 
	 * @param keepFinger If set to true, entries keep their finger for for later reuse (in this or a different list)
	 * when they are removed from this list. When set to false, the finger is removed.
	 */
	public InboxPacketBuffer(boolean keepFinger){
		super(keepFinger);
	}
	
	
	/* (non-Javadoc)
	 * @see nodes.PacketBuffer#addPacket(nodes.messages.Packet)
	 */
	public void addPacket(Packet p) {
		append(p);
	}
	
	/* (non-Javadoc)
	 * @see nodes.PacketBuffer#removePacket(nodes.messages.Packet)
	 */
	public void removePacket(Packet p) {
		remove(p);
	}	

	/* (non-Javadoc)
	 * @see nodes.PacketBuffer#updateMessageBuffer()
	 */
	public void updateMessageBuffer(){
		// ensure that the list of packets is clean (should already be empty)
		arrivingPackets.clear();
		
		bufferIterator.reset();
		while(bufferIterator.hasNext()){
			Packet p = bufferIterator.next();
			
			if(p.arrivingTime <= Global.currentTime){
				
				// only if added
				if(Configuration.interference){
					//remove it from the global queue
					Runtime.packetsInTheAir.remove(p);
				}
				
				bufferIterator.remove();
				if(p.edge != null) {
					p.edge.removeMessageForThisEdge(p.message);
				}
				if(p.positiveDelivery){
					//successful transmission
					arrivingPackets.add(p);
				} else {
					// failed transmission, drop the package
					if(Configuration.generateNAckMessages) {
						p.origin.addNackPacket(p); // return the packet to the sender
					} else {
						Packet.free(p);
					}
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see nodes.PacketBuffer#waitingPackets()
	 */
	public int waitingPackets(){
		return arrivingPackets.size();
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.nodes.PacketBuffer#invalidatePacketsSentOverThisEdge(sinalgo.nodes.edges.Edge)
	 */
	public void invalidatePacketsSentOverThisEdge(Edge e){
		for(Packet p : this) {
			if(p.edge != null && p.edge.getID() == e.getID()){
				p.positiveDelivery = false;
				p.edge = null; // the edge may have been removed and should not be refered to anymore
			}
		}
	}

	/* (non-Javadoc)
	 * @see sinalgo.nodes.PacketBuffer#getInbox()
	 */
	public Inbox getInbox() {
		arrivingPackets.sort();
		if(inbox == null){
			inbox = new Inbox(arrivingPackets);
		}	else {
			inbox.resetForList(arrivingPackets);
		}
		return inbox;
	}
}