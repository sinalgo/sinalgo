package projects.sample5.nodes.edges;

import sinalgo.configuration.Configuration;
import sinalgo.exception.CorruptConfigurationEntryException;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.gui.GraphPanel;
import sinalgo.gui.helper.Arrow;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.io.eps.EPSOutputPrintStream;
import sinalgo.nodes.Position;
import sinalgo.nodes.edges.Edge;

import java.awt.*;

/**
 * This edge draws itself bold whenever it is traversed by a message.
 * <p>
 * This edge requires a configuration file entry that specifies the stroke width
 * at which this edge draws itself. The entry has the following form:
 * <p>
 * &lt;BigEdge strokeWidth="..."&gt;
 */
public class BigEdge extends Edge {

    private int strokeWidth;

    public BigEdge() {
        try {
            this.strokeWidth = Configuration.getIntegerParameter("BigEdge/strokeWidth");
        } catch (CorruptConfigurationEntryException e) {
            throw new SinalgoFatalException(e.getMessage());
        }
    }

    @Override
    public void draw(Graphics g, PositionTransformation pt) {
        Position p1 = this.getStartNode().getPosition();
        pt.translateToGUIPosition(p1);
        int fromX = pt.getGuiX(), fromY = pt.getGuiY(); // temporarily store
        Position p2 = this.getEndNode().getPosition();
        pt.translateToGUIPosition(p2);

        if ((this.getNumberOfMessagesOnThisEdge() == 0) && (this.getOppositeEdge() != null)
                && (this.getOppositeEdge().getNumberOfMessagesOnThisEdge() > 0)) {
            // only draws the arrowHead (if drawArrows is true) - the line is drawn by the
            // 'opposite' edge
            Arrow.drawArrowHead(fromX, fromY, pt.getGuiX(), pt.getGuiY(), g, pt, this.getColor());
        } else {
            if (this.getNumberOfMessagesOnThisEdge() > 0) {
                Arrow.drawArrow(fromX, fromY, pt.getGuiX(), pt.getGuiY(), g, pt, this.getColor());
                g.setColor(this.getColor());
                GraphPanel.drawBoldLine(g, fromX, fromY, pt.getGuiX(), pt.getGuiY(), this.strokeWidth);
            } else {
                Arrow.drawArrow(fromX, fromY, pt.getGuiX(), pt.getGuiY(), g, pt, this.getColor());
            }
        }
    }

    @Override
    public void drawToPostScript(EPSOutputPrintStream pw, PositionTransformation pt) {
        pt.translateToGUIPosition(this.getStartNode().getPosition());
        double eSX = pt.getGuiXDouble();
        double eSY = pt.getGuiYDouble();
        pt.translateToGUIPosition(this.getEndNode().getPosition());
        Color c = this.getColor();
        pw.setColor(c.getRed(), c.getGreen(), c.getBlue());
        if (this.getNumberOfMessagesOnThisEdge() > 0) {
            pw.setLineWidth(0.5 * this.strokeWidth); // bold line
        } else {
            pw.setLineWidth(0.5);
        }

        if (Configuration.isDrawArrows()) {
            pw.drawArrow(eSX, eSY, pt.getGuiXDouble(), pt.getGuiYDouble());
        } else {
            pw.drawLine(eSX, eSY, pt.getGuiXDouble(), pt.getGuiYDouble());
        }
    }

}
