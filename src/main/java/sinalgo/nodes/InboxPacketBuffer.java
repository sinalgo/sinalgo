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
package sinalgo.nodes;

import sinalgo.configuration.Configuration;
import sinalgo.nodes.edges.Edge;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Packet;
import sinalgo.nodes.messages.PacketCollection;
import sinalgo.runtime.Global;
import sinalgo.runtime.SinalgoRuntime;
import sinalgo.tools.storage.DoublyLinkedList;
import sinalgo.tools.storage.ReusableListIterator;

/**
 * A list implementation that holds the messages arriving at a node.
 */
public class InboxPacketBuffer extends DoublyLinkedList<Packet> implements PacketBuffer {

    // the vector of packets that arrive in this step
    private PacketCollection arrivingPackets = new PacketCollection();
    private ReusableListIterator<Packet> bufferIterator = this.iterator();

    private Inbox inbox;

    /**
     * The default constructor of the dllPacketBuffer-class.
     */
    public InboxPacketBuffer() {
        super();
    }

    /**
     * Creates a new instance of a DLLPacketBuffer.
     * <p>
     * This method lets you specify whether entries keep their finger-entry when
     * they are removed from this list. This may increase performance if the same
     * entries are added and removed several times to/from this list.
     *
     * @param keepFinger If set to true, entries keep their finger for for later reuse (in
     *                   this or a different list) when they are removed from this list.
     *                   When set to false, the finger is removed.
     */
    public InboxPacketBuffer(boolean keepFinger) {
        super(keepFinger);
    }

    @Override
    public void addPacket(Packet p) {
        this.append(p);
    }

    @Override
    public void removePacket(Packet p) {
        this.remove(p);
    }

    @Override
    public void updateMessageBuffer() {
        // ensure that the list of packets is clean (should already be empty)
        this.arrivingPackets.clear();

        this.bufferIterator.reset();
        while (this.bufferIterator.hasNext()) {
            Packet p = this.bufferIterator.next();

            if (p.getArrivingTime() <= Global.getCurrentTime()) {

                // only if added
                if (Configuration.isInterference()) {
                    // remove it from the global queue
                    SinalgoRuntime.getPacketsInTheAir().remove(p);
                }

                this.bufferIterator.remove();
                if (p.getEdge() != null) {
                    p.getEdge().removeMessageForThisEdge(p.getMessage());
                }
                if (p.isPositiveDelivery()) {
                    // successful transmission
                    this.arrivingPackets.add(p);
                } else {
                    // failed transmission, drop the package
                    if (Configuration.isGenerateNAckMessages()) {
                        p.getOrigin().addNackPacket(p); // return the packet to the sender
                    } else {
                        Packet.free(p);
                    }
                }
            }
        }
    }

    @Override
    public int waitingPackets() {
        return this.arrivingPackets.size();
    }

    @Override
    public void invalidatePacketsSentOverThisEdge(Edge e) {
        for (Packet p : this) {
            if (p.getEdge() != null && p.getEdge().getID() == e.getID()) {
                p.setPositiveDelivery(false);
                p.setEdge(null); // the edge may have been removed and should not be refered to anymore
            }
        }
    }

    @Override
    public Inbox getInbox() {
        this.arrivingPackets.sort();
        if (this.inbox == null) {
            this.inbox = new Inbox(this.arrivingPackets);
        } else {
            this.inbox.resetForList(this.arrivingPackets);
        }
        return this.inbox;
    }
}
