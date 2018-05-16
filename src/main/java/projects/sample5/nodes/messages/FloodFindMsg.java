package projects.sample5.nodes.messages;

import lombok.Getter;
import lombok.Setter;
import projects.sample5.nodes.timers.RetryFloodingTimer;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;

/**
 * The message used to determine a route between two nodes using incremental
 * flooding. This message requires the read-only policy.
 */
@Getter
@Setter
public class FloodFindMsg extends Message {

    /**
     * True if this is a find-message, false if it is the answer-message that
     * returns from the destination when the flooding was successful.
     */
    private boolean isFindMessage;

    /**
     * The TTL for this message when it's being sent as a find-msg
     */
    private int ttl;

    /**
     * The node that wishes to determine the route to the desinationNode;
     */
    private Node sender;

    /**
     * Number of hops to sender
     */
    private int hopsToSender;

    /**
     * Sequence ID of this message
     */
    private int sequenceID;

    private RetryFloodingTimer retryTimer;

    /**
     * The node to which a route should be established.
     */
    private Node destination;

    /**
     * Default constructor.
     */
    public FloodFindMsg(int seqID, Node sender, Node dest) {
        this.setTtl(4); // initial TTL
        this.setFindMessage(true);
        this.setHopsToSender(0);
        this.setSequenceID(seqID);
        this.setSender(sender);
        this.setDestination(dest);
    }

    @Override
    public Message clone() {
        // This message requires a read-only policy
        return this;
    }

    /**
     * @return A real clone of this message, i.e. a new message object
     */
    public FloodFindMsg getRealClone() {
        FloodFindMsg msg = new FloodFindMsg(this.getSequenceID(), this.getSender(), this.getDestination());
        msg.setTtl(this.getTtl());
        msg.setFindMessage(this.isFindMessage());
        msg.setHopsToSender(this.getHopsToSender());
        msg.setRetryTimer(this.getRetryTimer());
        return msg;
    }
}
