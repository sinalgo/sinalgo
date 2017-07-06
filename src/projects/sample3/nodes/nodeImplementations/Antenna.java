package projects.sample3.nodes.nodeImplementations;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.Vector;

import projects.sample3.nodes.messages.ByeBye;
import projects.sample3.nodes.messages.SmsMessage;
import projects.sample3.nodes.messages.SubscirbeMessage;
import projects.sample3.nodes.timers.AntennaNeighborhoodClearTimer;
import projects.sample3.nodes.timers.InviteMsgTimer;
import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;
import sinalgo.tools.Tools;

public class Antenna extends Node {
	
	// a list of all antennas
	private static Vector<Antenna> antennaList = new Vector<Antenna>();
	
	@Override
	public void checkRequirements() throws WrongConfigurationException {
	}

	@Override
	public void handleMessages(Inbox inbox) {
		while(inbox.hasNext()) {
			Message msg = inbox.next();
			// -----------------------------------------------------------------------------
			if(msg instanceof SubscirbeMessage) {
				neighbors.add(inbox.getSender());
			}
			// -----------------------------------------------------------------------------
			else if(msg instanceof ByeBye) { 
				neighbors.remove(inbox.getSender());
			}
			// -----------------------------------------------------------------------------
			else if(msg instanceof SmsMessage) {
				SmsMessage sms = (SmsMessage) msg;
				if(isNeighbor(sms.receiver)) {
					this.send(sms, sms.receiver); // forward the message to the destination
				} else if(inbox.getSender() instanceof MobileNode) {
					for(Antenna a : antennaList) {
						if(!a.equals(this)) {
							this.sendDirect(msg, a); // send the msg to all antennas
						}
					}
				}
			}
		}
	}
	
	private boolean isNeighbor(Node aNode) {
		if(neighbors.contains(aNode)) {
			return true;
		}
		if(oldNeighborhood.contains(aNode)) {
			return true;
		} 
		return false;
	}
	
	TreeSet<Node> neighbors = new TreeSet<Node>(new NodeComparer());
	TreeSet<Node> oldNeighborhood = new TreeSet<Node>(new NodeComparer());
	
	/**
	 * In the same round as calling this method, this antenna should broadcast
	 * an invitation that requires resubscription.
	 * Until these new subscriptions arrived, the old neighborhood list is still used. 
	 */
	public void resetNeighborhood() {
		// switch the two neighborhoods, s.t. the old neighborhood is the current neighborhood
		// and the new neighborhood becomes empty
		TreeSet<Node> temp = oldNeighborhood;
		oldNeighborhood = neighbors;
		neighbors = temp;
		neighbors.clear();
		// start a timer to clear the oldNeighborhood
		AntennaNeighborhoodClearTimer t = new AntennaNeighborhoodClearTimer(oldNeighborhood);
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

	/* (non-Javadoc)
	 * @see sinalgo.nodes.Node#toString()
	 */
	public String toString() {
		// show the list of subscribed nodes
		String list = "";
		for(Node n : neighbors) {
			list += " " + n.ID;
		}
		if(oldNeighborhood.size() > 0) {
			list += "\n(";
			for(Node n : oldNeighborhood) {
				list += " " + n.ID;
			}
			list += ")";
		}
		return Tools.wrapToLinesConsideringWS(list, 100);
	}

	private static int radius;
	{ try {
		radius = Configuration.getIntegerParameter("GeometricNodeCollection/rMax");
	} catch(CorruptConfigurationEntryException e) {
		Tools.fatalError(e.getMessage());
	}}
	
	public void draw(Graphics g, PositionTransformation pt, boolean highlight){
		Color bckup = g.getColor();
		g.setColor(Color.BLACK);
		this.drawingSizeInPixels = (int) (defaultDrawingSizeInPixels * pt.getZoomFactor());
		super.drawAsDisk(g, pt, highlight, drawingSizeInPixels);
		g.setColor(Color.LIGHT_GRAY);
		pt.translateToGUIPosition(this.getPosition());
		int r = (int) (radius * pt.getZoomFactor());
		g.drawOval(pt.guiX - r, pt.guiY - r, r*2, r*2);
		g.setColor(bckup);
	}

	public Antenna() {
		try {
			this.defaultDrawingSizeInPixels = Configuration.getIntegerParameter("Antenna/Size");
		} catch (CorruptConfigurationEntryException e) {
			Tools.fatalError(e.getMessage());
		}
	}

	
	/**
	 * Helper class to compare two nodes by their ID
	 */
	class NodeComparer implements Comparator<Node> {
		public int compare(Node n1, Node n2) {
			return n1.ID < n2.ID ? -1 : n1.ID == n2.ID ? 0 : 1;   
		}
	}

}
