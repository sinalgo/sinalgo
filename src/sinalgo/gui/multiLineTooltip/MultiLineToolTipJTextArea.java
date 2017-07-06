package sinalgo.gui.multiLineTooltip;

import java.awt.Dimension;

import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.text.Document;

/**
 * A JTextArea that implements a multi line tool tip.
 * Furthermore, it returns as its preferred with always the value 0.
 */
@SuppressWarnings("serial")
public class MultiLineToolTipJTextArea extends JTextArea {

	/* (non-Javadoc)
	 * @see javax.swing.JTextArea#getPreferredSize()
	 */
	public Dimension getPreferredSize() {
		Dimension d = super.getPreferredSize();
		d.width = 0;
		return d;
	}

	
	public MultiLineToolTipJTextArea() {
	}

	public MultiLineToolTipJTextArea(String text) {
		super(text);
	}

	public MultiLineToolTipJTextArea(Document doc) {
		super(doc);
	}

	public MultiLineToolTipJTextArea(int rows, int columns) {
		super(rows, columns);
	}

	public MultiLineToolTipJTextArea(String text, int rows, int columns) {
		super(text, rows, columns);
	}

	public MultiLineToolTipJTextArea(Document doc, String text, int rows,
																		int columns) {
		super(doc, text, rows, columns);
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#createToolTip()
	 */
	public JToolTip createToolTip(){
		return new MultiLineToolTip();
	}
}
