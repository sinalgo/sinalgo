package projects.sample3.nodes.nodeImplementations;

import projects.sample3.nodes.messages.ByeBye;
import projects.sample3.nodes.messages.SmsMessage;
import projects.sample3.nodes.messages.SubscirbeMessage;
import projects.sample3.nodes.timers.AntennaNeighborhoodClearTimer;
import projects.sample3.nodes.timers.InviteMsgTimer;
import sinalgo.configuration.Configuration;
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.exception.WrongConfigurationException;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;
import sinalgo.tools.Tools;

import java.awt.*;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.Vector;

public class Antenna extends Node {

    // a list of all antennas
    private static Vector<Antenna> antennaList = new Vector<>();

    @Override
    public void checkRequirements() throws WrongConfigurationException {
    }

    @Override
    public void handleMessages(Inbox inbox) {
        while (inbox.hasNext()) {
            Message msg = inbox.next();
            // -----------------------------------------------------------------------------
            if (msg instanceof SubscirbeMessage) {
                this.neighbors.add(inbox.getSender());
            }
            // -----------------------------------------------------------------------------
            else if (msg instanceof ByeBye) {
                this.neighbors.remove(inbox.getSender());
            }
            // -----------------------------------------------------------------------------
            else if (msg instanceof SmsMessage) {
                SmsMessage sms = (SmsMessage) msg;
                if (this.isNeighbor(sms.getReceiver())) {
                    this.send(sms, sms.getReceiver()); // forward the message to the destination
                } else if (inbox.getSender() instanceof MobileNode) {
                    for (Antenna a : antennaList) {
                        if (!a.equals(this)) {
                            this.sendDirect(msg, a); // send the msg to all antennas
                        }
                    }
                }
            }
        }
    }

    private boolean isNeighbor(Node aNode) {
        if (this.neighbors.contains(aNode)) {
            return true;
        }
        return this.oldNeighborhood.contains(aNode);
    }

    private TreeSet<Node> neighbors = new TreeSet<>(new NodeComparer());
    private TreeSet<Node> oldNeighborhood = new TreeSet<>(new NodeComparer());

    /**
     * In the same round as calling this method, this antenna should broadcast an
     * invitation that requires resubscription. Until these new subscriptions
     * arrived, the old neighborhood list is still used.
     */
    public void resetNeighborhood() {
        // switch the two neighborhoods, s.t. the old neighborhood is the current
        // neighborhood
        // and the new neighborhood becomes empty
        TreeSet<Node> temp = this.oldNeighborhood;
        this.oldNeighborhood = this.neighbors;
        this.neighbors = temp;
        this.neighbors.clear();
        // start a timer to clear the oldNeighborhood
        AntennaNeighborhoodClearTimer t = new AntennaNeighborhoodClearTimer(this.oldNeighborhood);
        t.startRelative(3, this);
    }

    @Override
    public void init() {
        // start a msg timer to periodically send the invite msg
        InviteMsgTimer timer = new InviteMsgTimer();
        timer.startRelative(1, this);
        antennaList.add(this);
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
    public String toString() {
        // show the list of subscribed nodes
        StringBuilder list = new StringBuilder();
        for (Node n : this.neighbors) {
            list.append(" ").append(n.getID());
        }
        if (this.oldNeighborhood.size() > 0) {
            list.append("\n(");
            for (Node n : this.oldNeighborhood) {
                list.append(" ").append(n.getID());
            }
            list.append(")");
        }
        return Tools.wrapToLinesConsideringWS(list.toString(), 100);
    }

    private static int radius;

    static {
        try {
            radius = Configuration.getIntegerParameter("GeometricNodeCollection/rMax");
        } catch (CorruptConfigurationEntryException e) {
            throw new SinalgoFatalException(e.getMessage());
        }
    }

    @Override
    public void draw(Graphics g, PositionTransformation pt, boolean highlight) {
        Color bckup = g.getColor();
        g.setColor(Color.BLACK);
        this.setDrawingSizeInPixels((int) (this.getDefaultDrawingSizeInPixels() * pt.getZoomFactor()));
        super.drawAsDisk(g, pt, highlight, this.getDrawingSizeInPixels());
        g.setColor(Color.LIGHT_GRAY);
        pt.translateToGUIPosition(this.getPosition());
        int r = (int) (radius * pt.getZoomFactor());
        g.drawOval(pt.getGuiX() - r, pt.getGuiY() - r, r * 2, r * 2);
        g.setColor(bckup);
    }

    public Antenna() {
        try {
            this.setDefaultDrawingSizeInPixels(Configuration.getIntegerParameter("Antenna/Size"));
        } catch (CorruptConfigurationEntryException e) {
            throw new SinalgoFatalException(e.getMessage());
        }
    }

    /**
     * Helper class to compare two nodes by their ID
     */
    class NodeComparer implements Comparator<Node> {

        @Override
        public int compare(Node n1, Node n2) {
            return Long.compare(n1.getID(), n2.getID());
        }
    }

}
