package projects.sample5.nodes.edges;

import java.awt.Color;
import java.awt.Graphics;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.gui.GraphPanel;
import sinalgo.gui.helper.Arrow;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.io.eps.EPSOutputPrintStream;
import sinalgo.nodes.Position;
import sinalgo.nodes.edges.Edge;
import sinalgo.tools.Tools;

/**
 * This edge draws itself bold whenever it is traversed by a message.
 * <p>
 * This edge requires a configuration file entry that specifies the stroke
 * width at which this edge draws itself. The entry has the following form:
 * <p>
 * &lt;BigEdge strokeWidth="..."&gt;
 */
public class BigEdge extends Edge {
	int strokeWidth;

	public BigEdge() {
		try {
			 strokeWidth= Configuration.getIntegerParameter("BigEdge/strokeWidth");
		} catch(CorruptConfigurationEntryException e) {
			Tools.fatalError(e.getMessage());
		}
	}

	public void draw(Graphics g, PositionTransformation pt) {
		Position p1 = startNode.getPosition();
		pt.translateToGUIPosition(p1);
		int fromX = pt.guiX, fromY = pt.guiY; // temporarily store
		Position p2 = endNode.getPosition();
		pt.translateToGUIPosition(p2);
		
		if((this.numberOfMessagesOnThisEdge == 0)&&
				(this.oppositeEdge != null)&&
				(this.oppositeEdge.numberOfMessagesOnThisEdge > 0)){
			// only draws the arrowHead (if drawArrows is true) - the line is drawn by the 'opposite' edge
			Arrow.drawArrowHead(fromX, fromY, pt.guiX, pt.guiY, g, pt, getColor());
		} else {
			if(numberOfMessagesOnThisEdge > 0) {
				Arrow.drawArrow(fromX, fromY, pt.guiX, pt.guiY, g, pt, getColor());
				g.setColor(getColor());
				GraphPanel.drawBoldLine(g, fromX, fromY, pt.guiX, pt.guiY, strokeWidth);
			} else {
				Arrow.drawArrow(fromX, fromY, pt.guiX, pt.guiY, g, pt, getColor());
			}
		}
	}
	
	public void drawToPostScript(EPSOutputPrintStream pw, PositionTransformation pt) {
		pt.translateToGUIPosition(startNode.getPosition());
		double eSX = pt.guiXDouble;
		double eSY = pt.guiYDouble;
		pt.translateToGUIPosition(endNode.getPosition());
		Color c = getColor();
		pw.setColor(c.getRed(), c.getGreen(), c.getBlue());
		if(numberOfMessagesOnThisEdge > 0) {
			pw.setLineWidth(0.5 * strokeWidth); // bold line
		} else {
			pw.setLineWidth(0.5);
		}
		
		if(Configuration.drawArrows){
			pw.drawArrow(eSX, eSY, pt.guiXDouble, pt.guiYDouble);
		}
		else{
			pw.drawLine(eSX, eSY, pt.guiXDouble, pt.guiYDouble);
		}
	}

}
