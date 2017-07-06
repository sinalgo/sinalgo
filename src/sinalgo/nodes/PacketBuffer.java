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

import sinalgo.nodes.edges.Edge;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Packet;



/**
 * Interface for storing the messages on the way to a node. One instance of a class implementing this interface 
 * is stored in each node to store the nodes on the way to this node. It also handles the arriving packets. 
 * In every step you can get an Enumeration over all the arriving packets and you do not have to care for messages
 * still on the way. Remember, that the packets arriving in this round are not stored for further 
 * rounds. So if you don't get them in this round, they are lost. So if you want them to be buffered, 
 * you have to do that by your own.
 */
public interface PacketBuffer {
	/**
	 * This method returns the number of packets arriving in this round.
	 *
	 * @return The number of Packets arriving this node in this round.
	 */
	public abstract int waitingPackets();
	
	
	/**
	 * This method adds a Packet to the buffer.
	 *
	 * @param p The packet to add to the buffer. 
	 */
	public abstract void addPacket(Packet p);
	
	
	/**
	 * This method removes a packet from the buffer. This method is called very rarely.
	 * It is just called by the system when a packet is interferred by interference.
	 * In all the other cases the packets are removed by iterators.
	 *
	 * @param p The Packet to remove from the buffer.
	 */
	public abstract void removePacket(Packet p);
	
	/**
 	 * Sets the positivedelivery flag of all packets to false if they are sent over the 
 	 * specified edge.
	 * 
	 * @param e The edge for which the packets have to be invalidated.
	 */
	public abstract void invalidatePacketsSentOverThisEdge(Edge e);
	
	/**
	 * This method updates the message-buffer for the node. This means, that it
	 * prepares all the messages that are incoming in this round for the user
	 * to get them. Note that is Configuration.checkConnectionsEveyStep is set true
	 * it is possible, that a message is removed in this round.
	 */
	public abstract void updateMessageBuffer();
	
	/**
	 * This method returns a Inbox instance for this PacketBuffer. The inbox instance is used to 
	 * iterate over the PacketBuffer and to get the Header-Information from the Packets. 
	 * 
	 * @return An Inbox instance for this PacketBuffer.
	 */
	public abstract Inbox getInbox();
}

