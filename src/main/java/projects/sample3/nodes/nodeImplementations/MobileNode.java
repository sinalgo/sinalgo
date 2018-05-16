package projects.sample3.nodes.nodeImplementations;

import projects.sample3.nodes.messages.ByeBye;
import projects.sample3.nodes.messages.InviteMessage;
import projects.sample3.nodes.messages.SmsAckMessage;
import projects.sample3.nodes.messages.SmsMessage;
import projects.sample3.nodes.messages.SubscirbeMessage;
import projects.sample3.nodes.timers.SmsTimer;
import sinalgo.configuration.Configuration;
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.exception.WrongConfigurationException;
import sinalgo.gui.helper.Arrow;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;
import sinalgo.nodes.timers.Timer;
import sinalgo.tools.Tools;
import sinalgo.tools.logging.Logging;

import javax.swing.*;
import java.awt.*;

public class MobileNode extends Node {

    private Logging log = Logging.getLogger();// ("smsLog.txt");

    private Antenna currentAntenna; // the antenna ths node is connected to, null if this node is not connected to
    // an antenna
    private int seqIDCounter;

    public Antenna getCurrentAntenna() {
        return this.currentAntenna;
    }

    public int getNextSeqID() {
        return ++this.seqIDCounter;
    }

    @Override
    public void checkRequirements() throws WrongConfigurationException {
    }

    @Override
    public void handleMessages(Inbox inbox) {
        boolean needSubscription = false;
        Antenna oldAntenna = this.currentAntenna;

        while (inbox.hasNext()) {
            Message msg = inbox.next();

            // -----------------------------------------------------------------------------
            if (msg instanceof InviteMessage) {
                InviteMessage im = (InviteMessage) msg;
                // drop the current antenna if the newer one is closer
                if (this.currentAntenna != null) {
                    double oldDist = this.currentAntenna.getPosition().squareDistanceTo(this.getPosition());
                    double newDist = inbox.getSender().getPosition().squareDistanceTo(this.getPosition());
                    if (oldDist > newDist) {
                        // and store the new one (subscription is sent only after seeing all messages)
                        this.currentAntenna = (Antenna) inbox.getSender();
                        needSubscription = true;
                    } else {
                        if (im.isRequireSubscription()) {
                            needSubscription = true; // subscirbe again
                        }
                    }
                } else {
                    this.currentAntenna = (Antenna) inbox.getSender();
                    needSubscription = true;
                }
            }
            // we should actually handle all invite msgs first, and only then the sms...
            // -----------------------------------------------------------------------------
            else if (msg instanceof SmsAckMessage) {
                SmsAckMessage ack = (SmsAckMessage) msg;
                if (ack.getReceiver().equals(this)) {
                    ack.getSmsTimer().disable(); // stop the timer - the message arrived
                    this.log.logln("Message <" + this.getID() + "," + ack.getSender().getID() + "> acknowledged. Message: " + ack.getText());
                    this.setColor(Color.YELLOW);
                } else {
                    this.log.logln("Message <" + ack.getReceiver().getID() + "," + ack.getSender().getID() + "> ACK arrived at wrong node ("
                            + this.getID() + ") Message: " + ack.getText());
                }
            }
            // -----------------------------------------------------------------------------
            else if (msg instanceof SmsMessage) {
                SmsMessage sms = (SmsMessage) msg;
                if (sms.getReceiver().equals(this)) {
                    this.log.logln("Message <" + sms.getSender().getID() + "," + sms.getReceiver().getID() + "> arrived. Message: " + sms.getText());
                    this.setColor(Color.GREEN);
                    // send ACK
                    if (this.currentAntenna != null) {
                        SmsAckMessage ack = new SmsAckMessage(this.getNextSeqID(), sms.getSender(), this, sms.getText(),
                                sms.getSmsTimer());
                        this.send(ack, this.currentAntenna);
                    }
                } else {
                    this.log.logln("Message <" + sms.getSender().getID() + "," + sms.getReceiver().getID() + "> arrived at wrong node ("
                            + this.getID() + ") Message: " + sms.getText());
                }
            }
        }

        if (oldAntenna != null && !oldAntenna.equals(this.currentAntenna)) { // we switch to a different antenna
            // detach from current antenna
            ByeBye bye = new ByeBye();
            this.send(bye, oldAntenna);
        }

        // subscribe to the closest Antenna
        if (needSubscription) {
            SubscirbeMessage sm = new SubscirbeMessage();
            this.send(sm, this.currentAntenna);
        }
    }

    public MobileNode() {
        try {
            this.setDefaultDrawingSizeInPixels(Configuration.getIntegerParameter("MobileNode/Size"));
        } catch (CorruptConfigurationEntryException e) {
            throw new SinalgoFatalException(e.getMessage());
        }
    }

    @NodePopupMethod(menuText = "Send SMS to...")
    public void sendSMS() {
        Tools.getNodeSelectedByUser(n -> {
            if (n == null) {
                return;
            }
            String text = JOptionPane.showInputDialog(null, "Please enter the SMS text to send");
            if (text == null) {
                return;
            }
            SmsTimer t = new SmsTimer(text, n);
            t.startRelative(1, MobileNode.this);
            MobileNode.this.setColor(Color.RED);
            n.setColor(Color.BLUE);
        }, "Select a node to which the SMS will be sent to.");
    }

    @Override
    public String toString() {
        if (this.currentAntenna != null) {
            return "Connected to Antenna " + this.currentAntenna.getID();
        } else {
            return "Currently not connected.";
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
        super.draw(g, pt, highlight);
        for (Timer t : this.getTimers()) {
            if (t instanceof SmsTimer) {
                SmsTimer st = (SmsTimer) t;
                if (st.isEnabled()) {
                    pt.translateToGUIPosition(this.getPosition());
                    int fromX = pt.getGuiX(), fromY = pt.getGuiY();
                    pt.translateToGUIPosition(st.getDestination().getPosition());
                    Arrow.drawArrow(fromX, fromY, pt.getGuiX(), pt.getGuiY(), g, pt, Color.RED);
                }
            }
        }
    }

}
