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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.nodes.Node;
import sinalgo.tools.storage.DoublyLinkedListEntry;

/**
 * An event for the asynchronous mode. Concrete events extend this class.
 */
public abstract class Event implements DoublyLinkedListEntry {

    /**
     * the time this event happens.
     */
    @Getter
    @Setter
    private double time;

    // the ID of this event (this ID is just used interanally for ordering the
    // events)
    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private long ID;

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private static long nextID = 1;

    /**
     * Gets the next usable ID for Event creation.
     * This acts as a post-increment operation.
     *
     * @return The next usable ID.
     */
    public static long getNextFreeID() {
        long curId = getNextID();
        setNextID(curId + 1);
        return curId;
    }

    /**
     * Returns a string representation of the time when this event executes,
     * truncated to the given number of digits.
     *
     * @param digits The number of digits to display
     * @return A truncated string representation of the time when this event will
     * execute.
     */
    public String getExecutionTimeString(int digits) {
        if (digits > 10) {
            return Double.toString(this.getTime());
        }
        double factor = Math.pow(10, digits);
        double temp = Math.round(this.getTime() * factor) / factor;
        return Double.toString(temp);
    }

    /**
     * Creates an event with a given time to execute and a given node to execute on.
     *
     * @param time The time the event will pappen.
     */
    protected Event(double time) {
        this.setTime(time);
        this.setID(getNextFreeID());
    }

    /**
     * @param hasExecuted True if the event has already executed, otherwise false.
     * @return The text to be displayed in the extended control panel for this
     * event.
     */
    public abstract String getEventListText(boolean hasExecuted);

    /**
     * @param hasExecuted True if the event has already executed, otherwise false.
     * @return The tooltip text to be displayed in the extended control panel for
     * this event.
     */
    public abstract String getEventListToolTipText(boolean hasExecuted);

    /**
     * @return True if this event is associated with a node (e.g. receiver of a
     * packet or the handler of a timer). Otherwise, if this event is
     * framework specific, this method returns false.
     */
    public abstract boolean isNodeEvent();

    /**
     * @return The node for which the event is scheduled (receiver of a packet,
     * handler of a timer event), null if the event is not associated with a
     * node.
     */
    public abstract Node getEventNode();

    /**
     * Frees the this event. Puts it into the event pool.
     */
    public abstract void free();

    /**
     * Called when this event is removed before it was handled. This method does NOT
     * free the event.
     */
    public abstract void drop();

    /**
     * Executes this event, but does not yet free its resources or this event.
     */
    public abstract void handle();

    // the DLLE entry for the DoublyLinkedList
    private DLLFingerList dllFingerList = new DLLFingerList();

    @Override
    public DLLFingerList getDoublyLinkedListFinger() {
        return this.dllFingerList;
    }

}
