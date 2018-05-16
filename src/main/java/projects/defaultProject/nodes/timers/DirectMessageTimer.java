package projects.defaultProject.nodes.timers;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;
import sinalgo.nodes.timers.Timer;

/**
 * A timer that sends a message to a given node when it fires, independent of
 * the underlying network graph. I.e. the timer uses the sendDirect() method to
 * deliver the message.
 * <p>
 * The message is sent by the node who starts the timer.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class DirectMessageTimer extends Timer {

    private Message msg; // the msg to send
    private Node target; // the node to send the msg to

    /**
     * Create a new timer for the given message.
     *
     * @param m The message to send to the node n.
     * @param n The node to send the msg to
     */
    public DirectMessageTimer(Message m, Node n) {
        this.setMsg(m);
        this.setTarget(n);
    }

    @Override
    public void fire() {
        this.getTargetNode().sendDirect(this.getMsg(), this.getTarget());
    }

}
