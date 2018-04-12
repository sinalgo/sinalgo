package projects.sample6.nodes.nodeImplementations;

import projects.sample6.nodes.messages.MarkMessage;
import sinalgo.exception.WrongConfigurationException;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;

import java.awt.*;

/**
 * A node on the bottom of the tree
 */
public class LeafNode extends TreeNode {

    // A counter that may be reset by the user
    public static int smallIdCounter = 0;
    public int smallID;

    public LeafNode() {
        smallID = ++smallIdCounter;
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
        super.drawNodeAsDiskWithText(g, pt, highlight, Integer.toString(this.smallID), 15, Color.YELLOW);
    }

    @Override
    public String toString() {
        return smallID + " (" + ID + ")";
    }

}
