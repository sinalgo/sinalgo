package projects.sample5.nodes.nodeImplementations;

import projects.defaultProject.models.messageTransmissionModels.ConstantTime;
import projects.sample5.nodes.messages.AckPayload;
import projects.sample5.nodes.messages.FloodFindMsg;
import projects.sample5.nodes.messages.PayloadMsg;
import projects.sample5.nodes.timers.PayloadMessageTimer;
import projects.sample5.nodes.timers.RetryFloodingTimer;
import projects.sample5.nodes.timers.RetryPayloadMessageTimer;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.exception.WrongConfigurationException;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;
import sinalgo.nodes.messages.NackBox;
import sinalgo.tools.Tools;
import sinalgo.tools.logging.Logging;

import java.awt.*;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

/**
 * A node that implements a flooding strategy to determine paths to other nodes.
 */
public class FNode extends Node {

    /**
     * A routing table entry
     */
    public class RoutingEntry {

        int sequenceNumber; // sequence number used when this entry was created
        int numHops; // number of hops to reach destination
        Node nextHop; // next hop to take

        RoutingEntry(int seqNumber, int hops, Node hop) {
            this.sequenceNumber = seqNumber;
            this.numHops = hops;
            this.nextHop = hop;
        }
    }

    // counter, incremented and added for each msg sent (not forwarded) by this node
    private int seqID; // an ID used to distinguish successive msg

    // The routing table of this node, maps destination node to a routing entry
    private Hashtable<Node, RoutingEntry> routingTable = new Hashtable<>();

    // messages that could not be sent so far, because no route is known
    private Vector<PayloadMsg> messagesOnHold = new Vector<>();

    /**
     * Method to clear this node's routing table
     */
    public void clearRoutingTable() {
        this.routingTable.clear();
    }

    @Override
    public void checkRequirements() throws WrongConfigurationException {
        // The message delivery time must be constant, this allows the project
        // to easily predict the waiting times
        if (!(Tools.getMessageTransmissionModel() instanceof ConstantTime)) {
            throw new SinalgoFatalException(
                    "This project requires that messages are sent with the ConstantTime MessageTransmissionModel.");
        }
    }

    @Override
    public void handleMessages(Inbox inbox) {
        while (inbox.hasNext()) {
            Message msg = inbox.next();

            // ---------------------------------------------------------------
            if (msg instanceof FloodFindMsg) { // This node received a flooding message.
                // ---------------------------------------------------------------
                FloodFindMsg m = (FloodFindMsg) msg;
                if (m.isFindMessage()) {
                    // forward the message, it's a find-message that has to be
                    // forwarded if the TTL allows. At the same time, update this node's routing
                    // table s.t. it knows how to route to the sender of the flooding-msg.
                    boolean forward = true;
                    if (m.getSender().equals(this)) { // the message bounced back - discard the msg
                        forward = false;
                    } else { // update routing table to the sender of this node
                        RoutingEntry re = this.routingTable.get(m.getSender());
                        if (re == null) { // add a new routing entry
                            this.routingTable.put(m.getSender(),
                                    new RoutingEntry(m.getSequenceID(), m.getHopsToSender(), inbox.getSender()));
                            this.useNewRoutingInfo(m.getDestination(), inbox.getSender());
                        } else if (re.sequenceNumber < m.getSequenceID()) { // update the existing entry
                            re.numHops = m.getHopsToSender();
                            re.sequenceNumber = m.getSequenceID();
                            re.nextHop = inbox.getSender();
                        } else {
                            forward = false; // we've already seen this message once - don't forward it a 2nd time
                        }
                    }
                    if (m.getDestination().equals(this)) { // the lookup has succeeded, this is the node that was searched
                        this.setColor(Color.BLUE);
                        FloodFindMsg copy = m.getRealClone();
                        copy.setHopsToSender(1); // now, this field contains the hops to the destination
                        copy.setFindMessage(false);
                        copy.setSequenceID(++this.seqID);
                        this.send(copy, inbox.getSender()); // send back the echo message
                        forward = false;
                    }

                    if (forward && m.getTtl() > 1) { // forward the flooding request
                        FloodFindMsg copy = m.getRealClone();
                        copy.setTtl(copy.getTtl() - 1);
                        copy.setHopsToSender(copy.getHopsToSender() + 1);
                        this.broadcast(copy);
                    }
                } else { // return the message back to the sender
                    // update the routing table
                    boolean forward = true;
                    this.setColor(Color.GREEN);
                    RoutingEntry re = this.routingTable.get(m.getDestination());
                    if (re == null) { // add a new routing entry
                        this.routingTable.put(m.getDestination(),
                                new RoutingEntry(m.getSequenceID(), m.getHopsToSender(), inbox.getSender()));
                        this.useNewRoutingInfo(m.getDestination(), inbox.getSender());
                    } else if (re.sequenceNumber < m.getSequenceID()) { // update the existing entry
                        re.numHops = m.getHopsToSender();
                        re.sequenceNumber = m.getSequenceID();
                        re.nextHop = inbox.getSender();
                    } else {
                        forward = false;
                    }
                    if (m.getSender().equals(this)) {
                        // this node sent the request - remove timers
                        m.getRetryTimer().deactivate();
                    } else if (forward) {
                        re = this.routingTable.get(m.getSender());
                        if (re != null) {
                            m.setHopsToSender(m.getHopsToSender() + 1); // we can modify the message, its a unicast
                            this.send(m, re.nextHop);
                        }
                    }
                }
            }
            // ---------------------------------------------------------------
            if (msg instanceof PayloadMsg) {
                PayloadMsg m = (PayloadMsg) msg;
                if (m.getDestination().equals(this)) { // the message was for this node
                    if (msg instanceof AckPayload) { // it is an ACK message
                        m.getAckTimer().deactivate();
                        this.setColor(Color.ORANGE);
                    } else { // it is a Payload Msg
                        // handle the payload
                        this.setColor(Color.YELLOW);
                        // send back an ACK
                        AckPayload ack = new AckPayload(m.getSender(), this);
                        ack.setSequenceNumber(m.getSequenceNumber());
                        ack.setAckTimer(m.getAckTimer());
                        this.sendPayloadMessage(ack);
                    }
                } else { // the message was not for this node -> forward
                    this.sendPayloadMessage(m);
                }
            }
        }
    }

    @Override
    public void handleNAckMessages(NackBox nackBox) {
        Logging log = Logging.getLogger();
        while (nackBox.hasNext()) {
            nackBox.next();
            log.logln("Node " + this.getID() + " could not send a message to " + nackBox.getReceiver().getID());
        }
    }

    @NodePopupMethod(menuText = "Send Message To...")
    public void sendMessageTo() {
        Tools.getNodeSelectedByUser(n -> {
            if (n == null) {
                return; // aborted
            }
            PayloadMsg msg = new PayloadMsg(n, FNode.this);
            msg.setRequireACK(true);
            msg.setSequenceNumber(++FNode.this.seqID);
            PayloadMessageTimer t = new PayloadMessageTimer(msg);
            t.startRelative(1, FNode.this);
        }, "Select a node to send a message to...");
    }

    /**
     * Tries to send a message if there is a routing entry. If there is no routing
     * entry, a search is started, and the message is put in a buffer of messages on
     * hold.
     *
     * @param msg The message to be sent.
     */
    public void sendPayloadMessage(PayloadMsg msg) {
        RoutingEntry re = this.routingTable.get(msg.getDestination());
        if (re != null) {
            if (msg.getSender().equals(this) && msg.isRequireACK()) { // this node wants to have the message sent - it waits for
                // an ack
                RetryPayloadMessageTimer rpmt = new RetryPayloadMessageTimer(msg);
                rpmt.startRelative(re.numHops * 3, this); // We wait a bit longer than necessary
                if (msg.getAckTimer() != null) {
                    msg.getAckTimer().deactivate();
                }
                msg.setAckTimer(rpmt);
            }
            this.send(msg, re.nextHop);
        } else {
            this.lookForNode(msg.getDestination(), 4);
            this.messagesOnHold.add(msg);
        }
    }

    /**
     * Starts a search for a given node with the given TTL
     *
     * @param destination The destination node.
     * @param ttl         The time-to-live.
     */
    public void lookForNode(Node destination, int ttl) {
        if (ttl > 10_000_000) { // this limits to graphs of diameter 10^7 ....
            return; // we've already searched too far - there is probably no connection!
        }

        FloodFindMsg m = new FloodFindMsg(++this.seqID, this, destination);
        m.setTtl(ttl);
        RetryFloodingTimer rft = new RetryFloodingTimer(destination, m.getTtl());
        // The TTL must depend on the message transmission time. We assume here a
        // constant msg. transm. time of 1 unit.
        rft.startRelative(m.getTtl() * 2 + 1, this);
        m.setRetryTimer(rft);
        this.broadcast(m);
    }

    private void useNewRoutingInfo(Node destination, Node nextHop) {
        Iterator<PayloadMsg> it = this.messagesOnHold.iterator();
        while (it.hasNext()) {
            PayloadMsg m = it.next();
            if (m.getDestination().equals(destination)) {
                this.sendPayloadMessage(m);
                it.remove();
            }
        }
    }

    @Override
    public void init() {
    }

    @Override
    public void neighborhoodChange() {
        // we could remove routing-table entries that use this neighbor
    }

    @Override
    public void preStep() {
    }

    @Override
    public void postStep() {
    }

    @Override
    public String toString() {
        // show the routing table entries
        StringBuilder r = new StringBuilder();
        for (Entry<Node, RoutingEntry> e : this.routingTable.entrySet()) {
            r.append(e.getKey().getID()).append(" => ").append(e.getValue().nextHop.getID()).append(" (").append(e.getValue().numHops).append(")").append("\n");
        }
        return "\n" + r;
    }
}
