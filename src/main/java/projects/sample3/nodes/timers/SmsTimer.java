package projects.sample3.nodes.timers;

import lombok.Getter;
import lombok.Setter;
import projects.sample3.nodes.messages.SmsMessage;
import projects.sample3.nodes.nodeImplementations.Antenna;
import projects.sample3.nodes.nodeImplementations.MobileNode;
import sinalgo.nodes.Node;
import sinalgo.nodes.timers.Timer;

@Getter
@Setter
public class SmsTimer extends Timer {

    private String text;
    private Node destination;
    private boolean enabled = true;

    public void disable() {
        this.setEnabled(false);
    }

    public SmsTimer(String aText, Node aDestination) {
        this.setText(aText);
        this.setDestination(aDestination);
    }

    @Override
    public void fire() {
        if (this.isEnabled()) {
            MobileNode mn = (MobileNode) this.getTargetNode();
            // Assemble an SMS and send it to the current anteanna
            SmsMessage msg = new SmsMessage(mn.getNextSeqID(), this.getDestination(), this.getTargetNode(), this.getText(), this);
            Antenna a = mn.getCurrentAntenna();
            if (a != null) {
                this.getTargetNode().send(msg, a);
            }
            this.startRelative(8, this.getTargetNode()); // TODO: time?
        }
    }

}
