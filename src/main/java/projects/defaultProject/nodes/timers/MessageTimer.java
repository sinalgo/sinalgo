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
package projects.defaultProject.nodes.timers;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;
import sinalgo.nodes.timers.Timer;

/**
 * A timer that sends a message at a given time. The message may be unicast to a
 * specific node or broadcast.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class MessageTimer extends Timer {

    private Node receiver; // the receiver of the message, null if the message should be broadcast
    private Message msg; // the message to be sent

    /**
     * Creates a new MessageTimer object that unicasts a message to a given receiver
     * when the timer fires.
     * <p>
     * Nothing happens when the message cannot be sent at the time when the timer
     * fires.
     *
     * @param msg      The message to be sent when this timer fires.
     * @param receiver The receiver of the message.
     */
    public MessageTimer(Message msg, Node receiver) {
        this.setMsg(msg);
        this.setReceiver(receiver);
    }

    /**
     * Creates a MessageTimer object that broadcasts a message when the timer fires.
     *
     * @param msg The message to be sent when this timer fires.
     */
    public MessageTimer(Message msg) {
        this.setMsg(msg);
        this.setReceiver(null); // indicates broadcasting
    }

    @Override
    public void fire() {
        if (this.getReceiver() != null) { // there's a receiver => unicast the message
            this.getTargetNode().send(this.getMsg(), this.getReceiver());
        } else { // there's no reciever => broadcast the message
            this.getTargetNode().broadcast(this.getMsg());
        }
    }

}
