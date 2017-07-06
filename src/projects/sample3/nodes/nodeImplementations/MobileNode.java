package projects.sample3.nodes.nodeImplementations;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JOptionPane;

import projects.sample3.nodes.messages.ByeBye;
import projects.sample3.nodes.messages.InviteMessage;
import projects.sample3.nodes.messages.SmsAckMessage;
import projects.sample3.nodes.messages.SmsMessage;
import projects.sample3.nodes.messages.SubscirbeMessage;
import projects.sample3.nodes.timers.SmsTimer;
import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.gui.helper.Arrow;
import sinalgo.gui.helper.NodeSelectionHandler;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;
import sinalgo.nodes.timers.Timer;
import sinalgo.tools.Tools;
import sinalgo.tools.logging.Logging;

public class MobileNode extends Node {
	
	Logging log = Logging.getLogger();//("smsLog.txt");
	
	Antenna currentAntenna = null; // the antenna ths node is connected to, null if this node is not connected to an antenna
	private int seqIDCounter = 0;

	public Antenna getCurrentAntenna() {
		return currentAntenna;
	}
	
	public int getNextSeqID() {
		return ++seqIDCounter;
	}
	
	@Override
	public void checkRequirements() throws WrongConfigurationException {
	}

	@Override
	public void handleMessages(Inbox inbox) {
		boolean needSubscription = false;
		Antenna oldAntenna = currentAntenna;
		
		while(inbox.hasNext()) {
			Message msg = inbox.next();
			
			// -----------------------------------------------------------------------------
			if(msg instanceof InviteMessage) {
				InviteMessage im = (InviteMessage) msg;
				// drop the current antenna if the newer one is closer
				if(currentAntenna != null) {
					double oldDist = currentAntenna.getPosition().squareDistanceTo(this.getPosition());
					double newDist = inbox.getSender().getPosition().squareDistanceTo(this.getPosition());
					if(oldDist > newDist) {
						// and store the new one (subscription is sent only after seeing all messages)
						this.currentAntenna = (Antenna) inbox.getSender();
						needSubscription = true;
					} else {
						if(im.requireSubscription) {
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
			else if(msg instanceof SmsAckMessage) {
				SmsAckMessage ack = (SmsAckMessage) msg;
				if(ack.receiver.equals(this)) {
					ack.smsTimer.disable(); // stop the timer - the message arrived
					log.logln("Message <" + this.ID + "," + ack.sender.ID + "> acknowledged. Message: " + ack.text);
					this.setColor(Color.YELLOW);
				} else {
					log.logln("Message <" + ack.receiver.ID + "," + ack.sender.ID + "> ACK arrived at wrong node (" + this.ID + ") Message: " + ack.text );
				}
			}			
			// -----------------------------------------------------------------------------
			else if(msg instanceof SmsMessage) {
				SmsMessage sms = (SmsMessage) msg;
				if(sms.receiver.equals(this)) {
					log.logln("Message <" + sms.sender.ID+ "," + sms.receiver.ID + "> arrived. Message: " + sms.text);
					this.setColor(Color.GREEN);
					// send ACK
					if(currentAntenna != null) {
						SmsAckMessage ack = new SmsAckMessage(this.getNextSeqID(), sms.sender, this, sms.text, sms.smsTimer);
						this.send(ack, currentAntenna);
					}
				} else {
					log.logln("Message <" + sms.sender.ID+ "," + sms.receiver.ID + "> arrived at wrong node (" + this.ID + ") Message: " + sms.text);
				}
			}
		}

		if(oldAntenna != null && !currentAntenna.equals(oldAntenna)) { // we switch to a different antenna
			// detach from current antenna
			ByeBye bye = new ByeBye();
			this.send(bye, oldAntenna);
		}

		// subscribe to the closest Antenna
		if(needSubscription) {
			SubscirbeMessage sm = new SubscirbeMessage();
			this.send(sm, currentAntenna);
		}
	}

	public MobileNode() {
		try {
			this.defaultDrawingSizeInPixels = Configuration.getIntegerParameter("MobileNode/Size");
		} catch (CorruptConfigurationEntryException e) {
			Tools.fatalError(e.getMessage());
		}
	}

	@NodePopupMethod(menuText = "Send SMS to...")
	public void sendSMS() {
		Tools.getNodeSelectedByUser(new NodeSelectionHandler() {
			public void handleNodeSelectedEvent(Node n) {
				if(n == null) {
					return;
				}
				String text = JOptionPane.showInputDialog(null, "Please enter the SMS text to send");
				if(text == null) {
					return;
				}
				SmsTimer t = new SmsTimer(text, n);
				t.startRelative(1, MobileNode.this);
				MobileNode.this.setColor(Color.RED);
				n.setColor(Color.BLUE);
			}
		}, "Select a node to which the SMS will be sent to.");
	}
	
	public String toString() {
		if(currentAntenna != null) {
			return "Connected to Antenna " + currentAntenna.ID;
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

	public void draw(Graphics g, PositionTransformation pt, boolean highlight){
		super.draw(g, pt, highlight);
		for(Timer t : this.getTimers()) {
			if(t instanceof SmsTimer) {
				SmsTimer st = (SmsTimer) t;
				if(st.enabled) {
					pt.translateToGUIPosition(this.getPosition());
					int fromX = pt.guiX, fromY = pt.guiY;
					pt.translateToGUIPosition(st.destination.getPosition());
					Arrow.drawArrow(fromX, fromY, pt.guiX, pt.guiY, g, pt, Color.RED);
				}
			}
		}
	}
	
}
