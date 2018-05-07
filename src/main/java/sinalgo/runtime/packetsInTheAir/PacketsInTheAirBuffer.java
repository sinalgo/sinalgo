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
package sinalgo.runtime.packetsInTheAir;

import sinalgo.configuration.Configuration;
import sinalgo.nodes.messages.Packet;
import sinalgo.tools.storage.DoublyLinkedList;
import sinalgo.tools.storage.ReusableListIterator;

import java.util.Iterator;

/**
 * The class for the storage of all the packets currently in the air. This is
 * used for interference.
 * <p>
 * This class implements the iterable interface. The iterator contains all
 * packets that actively contribute to the interference. I.e. there is only one
 * packet per multicast that actively contributes to interference, namely the
 * one that takes longest to send.
 */
public class PacketsInTheAirBuffer implements Iterable<Packet> {

    private boolean newAdded = true;

    // The list of all packets that actively contribute to interference.
    private DoublyLinkedList<Packet> activePackets = new DoublyLinkedList<>(true);

    // the list of all packets that may experience interference, but do not actively
    // contribute to
    // interference. E.g. All but one of the multicast packets are placed in this
    // list. Only the
    // packet from a multicast that takes longest is added to the active 'packets'
    // list.
    private DoublyLinkedList<Packet> passivePackets = new DoublyLinkedList<>(true);

    private ReusableListIterator<Packet> activePacketsIterator = this.activePackets.iterator();
    private ReusableListIterator<Packet> passivePacketsIterator = this.passivePackets.iterator();

    /**
     * Removes a packet from the list of packets being sent, does nothing if the
     * packet was not added (e.g. packets sent through sendDirect)
     *
     * @param p The packet to remove
     */
    public void remove(Packet p) {
        if (!this.activePackets.remove(p)) {
            if (!this.passivePackets.remove(p)) {
                // The packet was in neither list
                return; // nothing changed, the interference did not change
            }
        }
        if (Configuration.isAsynchronousMode() && !Configuration.isInterferenceIsAdditive()) {
            this.testForInterference();
        }
    }

    /**
     * Tests all packets for interference, and sets the interference flag if
     * necessary.
     * <p>
     * In the synchronous mode, this method is called in every round, after the
     * nodes have moved. Only packets that are still supposed to arrvie are checked.
     * <p>
     * In asynchronous mode, this method is called only when something happens that
     * could change the interference. (Two cases: Either call all the time when the
     * PacketsInTheAir buffer changes, or assume that the interference is
     * 'additive', i.e. interference only decreses if a packet is removed. In the
     * latter case, we need to test for interference only upon removal of a packet
     * and only if there were insertions after the last removal.
     */
    public void testForInterference() {
        // check for packets that are interferred
        // PS: only check the packets for interference that are still alive
        // dead packets are still "int the air" as the sender does not know that it is
        // disturbed.
        this.activePacketsIterator.reset();
        while (this.activePacketsIterator.hasNext()) {
            Packet pack = this.activePacketsIterator.next();
            if (pack.isPositiveDelivery()) {
                // test if the packet is disturbed according to the destinations interference
                // model.
                pack.setPositiveDelivery(!pack.getDestination().getInterferenceModel().isDisturbed(pack));
            }
        }
        // and the same for the passive packets
        this.passivePacketsIterator.reset();
        while (this.passivePacketsIterator.hasNext()) {
            Packet pack = this.passivePacketsIterator.next();
            if (pack.isPositiveDelivery()) {
                // test if the packet is disturbed according to the destinations interference
                // model.
                pack.setPositiveDelivery(!pack.getDestination().getInterferenceModel().isDisturbed(pack));
            }
        }
    }

    /**
     * In asynchronous mode, this method is called before a packet is removed from
     * the list of packets in the air. If necessary, it determines for all messages
     * currently being sent the interfence.
     * <p>
     * If the interference is not additive, the interference test is performed after
     * every insertion/removal to this list, and this method needs not do anything.
     */
    public void performInterferenceTestBeforeRemove() {
        if (!Configuration.isInterferenceIsAdditive()) {
            return;
        }
        if (this.newAdded) {
            this.testForInterference();
            this.newAdded = false;
        }
    }

    /**
     * The method to add a Packet to the PacketsInTheAirBuffer. It also checks
     * whether this message caused interference to any other message in the buffer
     * or to itself.
     *
     * @param p The Packet to add.
     */
    public void add(Packet p) {
        this.newAdded = true;
        this.activePackets.append(p);
        if (Configuration.isAsynchronousMode() && !Configuration.isInterferenceIsAdditive()) {
            this.testForInterference();
        }
    }

    /**
     * Adds a packet to the list of packets that are currently being sent. The added
     * packet is declared to NOT cause any interference itself. This is needed for
     * all but one packets of a multicasting, in which case the packet which travels
     * longest is added to the active list, the remaining ones to the passive list.
     * <p>
     * Packets in this list are also checked for interference.
     *
     * @param p The packet to add to the passive list
     */
    public void addPassivePacket(Packet p) {
        this.passivePackets.append(p);
    }

    /**
     * Removes a packet from the list of all passive packets and adds it to the list
     * of active packets. This method is used in the multicast implementation to
     * efficiently find the packet which takes longest to deliver.
     *
     * @param p The packet to upgrade
     */
    public void upgradeToActivePacket(Packet p) {
        this.passivePackets.remove(p);
        this.add(p);
    }

    /**
     * Returns the number of packets in the air. Multicast packets are counted as
     * one packet.
     *
     * @return The number of packets in the air.
     */
    public int size() {
        return this.activePackets.size();
    }

    /**
     * Returns an iterator over all packets currently being sent that actively
     * contribute to interference. (This list only contains one message per
     * multicast.)
     * <p>
     * Important! You must not add / remove any entries to/of this list.
     *
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Packet> iterator() {
        return this.activePackets.iterator();
    }
}
