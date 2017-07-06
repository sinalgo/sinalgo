package sinalgo.gui.helper;

import java.awt.Color;
import java.awt.Graphics;

import sinalgo.configuration.Configuration;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.nodes.Position;
import sinalgo.nodes.messages.Packet;
import sinalgo.tools.Tools;

/**
 * Animations are mostly gui-related extensions which can be enabled through
 * the configuration file. Currently, the following animations are available:
 * <p>
 * - Envelopes for messages that are currently being sent.
 *   The envelope is drawn between the sender and target node, and moves
 *   towards the target node.
 *   The envelopes are drawn in the regular paint method in which the entire
 *   graph is painted. Thus, the message delivery time and the refresh rate 
 *   will determine how often the envelope will be redrawn. 
 */
public class Animations {

	/**
	 * Draws an envelope
	 * @param g The graphics to draw to
	 * @param x X-coord of the center of the envelope
	 * @param y Y-coord of the center of the envelope
	 * @param scale How much the envelope should be scaled. 
	 */
	private static void drawEnvelope(Graphics g, int x, int y, double scale, Color color) {
		int width = (int) (scale * Configuration.messageAnimationEnvelopeWidth);
		int height = (int) (scale * Configuration.messageAnimationEnvelopeHeight);
		int topX = x - width / 2;
		int topY = y - height / 2;
		g.setColor(color);
		g.fillRect(topX, topY, width, height);
		g.setColor(Color.BLACK);
		g.drawRect(topX, topY, width, height);
		g.drawLine(topX, topY, topX + width/2, topY + height / 2); 
		g.drawLine(topX + width/2, topY + height / 2, topX + width, topY); 
	}

	
	/**
	 * Draws an envelope for each message 'on the fly'. The envelope
	 * moves along the direct line of sight between the sender and 
	 * the destination. 
	 * @param g The graphics to paint to
	 * @param pt The position transformation object
	 */
	public static void drawEnvelopes(Graphics g, PositionTransformation pt) {
		double time = Tools.getGlobalTime();
		synchronized(Packet.issuedPackets) { 
			for(Packet p : Packet.issuedPackets) {
				if(p.arrivingTime < time || p.sendingTime > time || p.origin == null || p.destination == null) { 
					continue;
				}
				double fraction = (time - p.sendingTime) / (p.arrivingTime - p.sendingTime);
				Position startPos = p.origin.getPosition();
				Position endPos = p.destination.getPosition();
				double x = startPos.xCoord  + (endPos.xCoord - startPos.xCoord) * fraction; 
				double y = startPos.yCoord  + (endPos.yCoord - startPos.yCoord) * fraction;
				double z = startPos.zCoord  + (endPos.zCoord - startPos.zCoord) * fraction;
				pt.translateToGUIPosition(x, y, z);
				Color c = p.message.getEnvelopeColor(); // may return null, in which case the default color is chosen
				if(c == null) {
					c = Configuration.messageAnimationEnvelopeColor;
				}
				drawEnvelope(g, pt.guiX, pt.guiY, pt.getZoomFactor(), c);
			}
		}
	}

}
