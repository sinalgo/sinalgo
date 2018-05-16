package projects.sample6.nodes.nodeImplementations;

import lombok.Getter;
import lombok.Setter;
import projects.sample6.nodes.messages.MarkMessage;
import sinalgo.exception.WrongConfigurationException;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;

import java.awt.*;

/**
 * A node on the bottom of the tree
 */
@Getter
@Setter
public class LeafNode extends TreeNode {

    // A counter that may be reset by the user
    @Getter
    @Setter
    private static int smallIdCounter;

    private int smallID;

    public LeafNode() {
        setSmallIdCounter(getSmallIdCounter() + 1);
        this.setSmallID(getSmallIdCounter());
    }

    @Override
    public void checkRequirements() throws WrongConfigurationException {
    }

    @Override
    public void handleMessages(Inbox inbox) {
        while (inbox.hasNext()) {
            Message m = inbox.next();
            if (m instanceof MarkMessage) {
                this.setColor(Color.GREEN);
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

    @Override
    public void draw(Graphics g, PositionTransformation pt, boolean highlight) {
        super.drawNodeAsDiskWithText(g, pt, highlight, Integer.toString(this.getSmallID()), 15, Color.YELLOW);
    }

    @Override
    public String toString() {
        return this.getSmallID() + " (" + this.getID() + ")";
    }

}
