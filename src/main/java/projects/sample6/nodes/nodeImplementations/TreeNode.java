package projects.sample6.nodes.nodeImplementations;

import lombok.Getter;
import lombok.Setter;
import projects.defaultProject.nodes.timers.MessageTimer;
import projects.sample6.nodes.messages.MarkMessage;
import sinalgo.exception.WrongConfigurationException;
import sinalgo.nodes.Node;
import sinalgo.nodes.edges.Edge;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;

import java.awt.*;

/**
 * An internal node (or leaf node) of the tree. Note that the leaves are
 * instances of LeafNode, a subclass of this class.
 */
public class TreeNode extends Node {

    @Getter
    @Setter
    private TreeNode parent; // the parentGUI in the tree, null if this node is the root

    @Override
    public void checkRequirements() throws WrongConfigurationException {
    }

    @Override
    public void handleMessages(Inbox inbox) {
        while (inbox.hasNext()) {
            Message m = inbox.next();
            if (m instanceof MarkMessage) {
                if (this.getParent() == null || !inbox.getSender().equals(this.getParent())) {
                    continue;// don't consider mark messages sent by children
                }
                this.setColor(Color.RED);
                // forward the message to all children
                for (Edge e : this.getOutgoingConnections()) {
                    if (!e.getEndNode().equals(this.getParent())) { // don't send it to the parentGUI
                        this.send(m, e.getEndNode());
                    }
                }
                // alternatively, we could broadcast the message:
                // broadcast(m);
            }
        }
    }

    @Override
    public void init() {
    }

    @Override
    public void neighborhoodChange() {
    }

    @Override
    public void preStep() {
    }

    @Override
    public void postStep() {
    }

    @NodePopupMethod(menuText = "Color children")
    public void colorKids() {
        MarkMessage msg = new MarkMessage();
        MessageTimer timer = new MessageTimer(msg);
        timer.startRelative(1, this);
    }

}
