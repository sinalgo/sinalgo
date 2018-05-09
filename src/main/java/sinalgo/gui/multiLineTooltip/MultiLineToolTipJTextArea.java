package sinalgo.gui.multiLineTooltip;

import lombok.NoArgsConstructor;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;

/**
 * A JTextArea that implements a multi line tool tip. Furthermore, it returns as
 * its preferred with always the value 0.
 */
@NoArgsConstructor
public class MultiLineToolTipJTextArea extends JTextArea {

    private static final long serialVersionUID = -8502823518346846466L;

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width = 0;
        return d;
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

    public MultiLineToolTipJTextArea(Document doc, String text, int rows, int columns) {
        super(doc, text, rows, columns);
    }

    @Override
    public JToolTip createToolTip() {
        return new MultiLineToolTip();
    }
}
