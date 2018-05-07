package sinalgo.gui.helper;

import sinalgo.configuration.Configuration;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.nodes.Position;
import sinalgo.nodes.messages.Packet;
import sinalgo.tools.Tools;

import java.awt.*;

/**
 * Animations are mostly gui-related extensions which can be enabled through the
 * configuration file. Currently, the following animations are available:
 * <p>
 * - Envelopes for messages that are currently being sent. The envelope is drawn
 * between the sender and target node, and moves towards the target node. The
 * envelopes are drawn in the regular paint method in which the entire graph is
 * painted. Thus, the message delivery time and the refresh rate will determine
 * how often the envelope will be redrawn.
 */
public class Animations {

    /**
     * Draws an envelope
     *
     * @param g     The graphics to draw to
     * @param x     X-coord of the center of the envelope
     * @param y     Y-coord of the center of the envelope
     * @param scale How much the envelope should be scaled.
     */
    private static void drawEnvelope(Graphics g, int x, int y, double scale, Color color) {
        int width = (int) (scale * Configuration.getMessageAnimationEnvelopeWidth());
        int height = (int) (scale * Configuration.getMessageAnimationEnvelopeHeight());
        int topX = x - width / 2;
        int topY = y - height / 2;
        g.setColor(color);
        g.fillRect(topX, topY, width, height);
        g.setColor(Color.BLACK);
        g.drawRect(topX, topY, width, height);
        g.drawLine(topX, topY, topX + width / 2, topY + height / 2);
        g.drawLine(topX + width / 2, topY + height / 2, topX + width, topY);
    }

    /**
     * Draws an envelope for each message 'on the fly'. The envelope moves along the
     * direct line of sight between the sender and the destination.
     *
     * @param g  The graphics to paint to
     * @param pt The position transformation object
     */
    public static void drawEnvelopes(Graphics g, PositionTransformation pt) {
        double time = Tools.getGlobalTime();
        synchronized (Packet.ISSUED_PACKETS) {
            for (Packet p : Packet.ISSUED_PACKETS) {
                if (p.getArrivingTime() < time || p.getSendingTime() > time || p.getOrigin() == null || p.getDestination() == null) {
                    continue;
                }
                double fraction = (time - p.getSendingTime()) / (p.getArrivingTime() - p.getSendingTime());
                Position startPos = p.getOrigin().getPosition();
                Position endPos = p.getDestination().getPosition();
                double x = startPos.getXCoord() + (endPos.getXCoord() - startPos.getXCoord()) * fraction;
                double y = startPos.getYCoord() + (endPos.getYCoord() - startPos.getYCoord()) * fraction;
                double z = startPos.getZCoord() + (endPos.getZCoord() - startPos.getZCoord()) * fraction;
                pt.translateToGUIPosition(x, y, z);
                Color c = p.getMessage().getEnvelopeColor(); // may return null, in which case the default color is chosen
                if (c == null) {
                    c = Configuration.getMessageAnimationEnvelopeColor();
                }
                drawEnvelope(g, pt.getGuiX(), pt.getGuiY(), pt.getZoomFactor(), c);
            }
        }
    }

}
