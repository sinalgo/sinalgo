package projects.sample3.nodes.timers;

import projects.sample3.nodes.messages.SmsMessage;
import projects.sample3.nodes.nodeImplementations.Antenna;
import projects.sample3.nodes.nodeImplementations.MobileNode;
import sinalgo.nodes.Node;
import sinalgo.nodes.timers.Timer;

public class SmsTimer extends Timer {

    public String text;
    public Node destination;

    public boolean enabled = true;

    public void disable() {
        enabled = false;
    }

    public SmsTimer(String aText, Node aDestination) {
        this.text = aText;
        this.destination = aDestination;
    }

    @Override
    public void fire() {
        if (enabled) {
            MobileNode mn = (MobileNode) this.getTargetNode();
            // Assemble an SMS and send it to the current anteanna
            SmsMessage msg = new SmsMessage(mn.getNextSeqID(), destination, this.getTargetNode(), text, this);
            Antenna a = mn.getCurrentAntenna();
            if (a != null) {
                this.getTargetNode().send(msg, a);
            }
            this.startRelative(8, this.getTargetNode()); // TODO: time?
        }
    }

}
