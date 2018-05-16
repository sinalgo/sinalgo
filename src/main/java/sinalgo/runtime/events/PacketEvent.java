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
package sinalgo.runtime.events;

import lombok.Getter;
import lombok.Setter;
import sinalgo.configuration.Configuration;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.NackBox;
import sinalgo.nodes.messages.Packet;
import sinalgo.nodes.messages.Packet.PacketType;
import sinalgo.runtime.Global;
import sinalgo.runtime.SinalgoRuntime;
import sinalgo.tools.logging.Logging;

import java.util.Stack;

/**
 * The event that represents that a message is reaching its destination.
 */
@Getter
@Setter
public class PacketEvent extends Event {

    private static Stack<PacketEvent> unusedPacketEvents = new Stack<>();

    @Getter
    @Setter
    private static int numPacketEventsOnTheFly;

    public static int getNumFreedPacketEvents() {
        return unusedPacketEvents.size();
    }

    public static void clearUnusedPacketEvents() {
        unusedPacketEvents.clear();
    }

    /**
     * The packet this event was generated for. This packet (or to be precisely its
     * message) reaches at the time this event is scheduled.
     */
    private Packet packet;

    /**
     * Creates a new PacketEvent for a given packet, a given time and a given node.
     * This event represents that the packet reaches eventNode at time.
     *
     * @param packet The packet that reaches its target.
     * @param time   The time the packet reaches its target.
     */
    private PacketEvent(Packet packet, double time) {
        super(time);
        this.packet = packet;
    }

    /**
     * Creates a new packetEvent. Takes it from the eventPool if it contains one and
     * creates a new one otherwise.
     *
     * @param packet The packet that arrives when this event fires.
     * @param time   The time this event is scheduled to.
     * @return An instance of PacketEvent
     */
    public static PacketEvent getNewPacketEvent(Packet packet, double time) {
        PacketEvent pe;
        if (unusedPacketEvents.size() > 0) {
            pe = unusedPacketEvents.pop();
            if (pe.getPacket() != null) { // sanity check
                throw new SinalgoFatalException(Logging.getCodePosition()
                        + " PacketEvent factory failed! About to return a packet-event that was already returned. (Probably, free() was called > 1 on this packet event.)");
            }
            pe.setPacket(packet);
            pe.setTime(time);
            pe.setID(getNextFreeID());
        } else {
            pe = new PacketEvent(packet, time);
        }
        setNumPacketEventsOnTheFly(getNumPacketEventsOnTheFly() + 1);
        return pe;
    }

    /**
     * Frees the this event and the corresponding packet. Puts it into the event
     * pool.
     */
    @Override
    public void free() {
        if (this.packet != null) {
            Packet.free(this.packet);
            this.packet = null;
        }
        unusedPacketEvents.push(this);
        setNumPacketEventsOnTheFly(getNumPacketEventsOnTheFly() - 1);
    }

    // Two static objects to prevent from allocating them all over again
    private static Inbox inbox = new Inbox();
    private static NackBox nAckBox = new NackBox();

    @Override
    public void handle() {
        // the arrival of a packet in the asynchronous case
        if (Configuration.isInterference()) {
            SinalgoRuntime.getPacketsInTheAir().performInterferenceTestBeforeRemove();
            SinalgoRuntime.getPacketsInTheAir().remove(this.packet);
        }
        if (this.getPacket().getEdge() != null) {
            this.getPacket().getEdge().removeMessageForThisEdge(this.getPacket().getMessage());
        }
        if (this.getPacket().isPositiveDelivery()) {
            this.getPacket().getDestination().handleMessages(inbox.resetForPacket(this.getPacket()));
        } else {
            if (Configuration.isGenerateNAckMessages() && this.getPacket().getType() == PacketType.UNICAST) {
                this.getPacket().getOrigin().handleNAckMessages(nAckBox.resetForPacket(this.packet));
            }
        }
    }

    @Override
    public void drop() {
        // similar to the arrival of a packet in the asynchronous case
        if (Configuration.isInterference()) {
            SinalgoRuntime.getPacketsInTheAir().remove(this.getPacket());
        }
        if (this.getPacket().getEdge() != null) {
            this.getPacket().getEdge().removeMessageForThisEdge(this.getPacket().getMessage());
        }
    }

    @Override
    public String toString() {
        return "PacketEvent";
    }

    @Override
    public String getEventListText(boolean hasExecuted) {
        if (hasExecuted) {
            return "Packet at node " + this.getPacket().getDestination().getID()
                    + (this.getPacket().isPositiveDelivery() ? " (delivered)" : " (dropped)");
        } else {
            return "PE (Node:" + this.getPacket().getDestination().getID() + ", Time:" + this.getExecutionTimeString(4) + ")";
        }
    }

    @Override
    public String getEventListToolTipText(boolean hasExecuted) {
        if (hasExecuted) {
            return "The type of the message is: " + Global.toShortName(this.getPacket().getMessage().getClass().getName()) + "\n"
                    + (this.getPacket().isPositiveDelivery() ? "The message was delivered" : "The message was dropped.");
        } else {
            return "At time " + this.getTime() + " a message reaches node " + this.getPacket().getDestination().getID() + "\n"
                    + "The type of the message is: " + Global.toShortName(this.getPacket().getMessage().getClass().getName()) + "\n"
                    + (this.getPacket().isPositiveDelivery() ? "Until now it seems that the message will reach its destination."
                    : "The message has already been disturbed and will not reach its destination.");
        }
    }

    @Override
    public Node getEventNode() {
        return this.getPacket().getDestination();
    }

    @Override
    public boolean isNodeEvent() {
        return true;
    }
}
