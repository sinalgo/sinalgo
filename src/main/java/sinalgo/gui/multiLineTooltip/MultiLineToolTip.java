/*
BSD 3-Clause License

Copyright (c) 2007-2013, Distributed Computing Group (DCG)
                         ETH Zurich
                         Switzerland
                         dcg.ethz.ch
              2017-2018, André Brait

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the copyright holder nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package sinalgo.gui.multiLineTooltip;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;
import java.awt.*;

/**
 * The Tooltip to display if the user stops with the mouse over the graph panel.
 * Displays information about the edge or node under the current position.
 */
public class MultiLineToolTip extends JToolTip {

    private static final long serialVersionUID = 1860432832820206556L;

    /**
     * The constructor for the MyToolTip class.
     */
    public MultiLineToolTip() {
        this.updateUI();
    }

    @Override
    public void updateUI() {
        this.setUI(MultiLineToolTipUI.createUI(this));
    }

    /**
     * This method sets the number of colums the tooltip has.
     *
     * @param columns The number of columns the tooltip has.
     */
    public void setColumns(int columns) {
        this.columns = columns;
        this.setFixedWidth(0);
    }

    /**
     * This method sets the fixed width for the tooltip.
     *
     * @param width The fixed width of the tooltip.
     */
    public void setFixedWidth(int width) {
        this.fixedWidth = width;
        this.setColumns(0);
    }

    /**
     * The number of columns the tooltip has.
     *
     * @return The number of columns the tooltip has.
     */
    @Getter
    private int columns;

    /**
     * The fixed with of the tooltip.
     *
     * @return The fixed width of the tooltip.
     */
    @Getter
    private int fixedWidth;
}

// used such that the tooltip can display several lines of text (e.g. newlines)
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
class MultiLineToolTipUI extends BasicToolTipUI {

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private static MultiLineToolTipUI sharedInstance = new MultiLineToolTipUI();

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private static JToolTip tip;

    private CellRendererPane rendererPane;

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private static JTextArea textArea;

    public static ComponentUI createUI(JComponent c) {
        return getSharedInstance();
    }

    /**
     * The constructor for the MultiLineToolTipUI class.
     */
    public MultiLineToolTipUI() {
        super();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        setTip((JToolTip) c);
        this.setRendererPane(new CellRendererPane());
        c.add(this.getRendererPane());
    }

    @Override
    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);

        c.remove(this.getRendererPane());
        this.setRendererPane(null);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Dimension size = c.getSize();
        getTextArea().setBackground(c.getBackground());
        this.getRendererPane().paintComponent(g, getTextArea(), c, 1, 1, size.width - 1, size.height - 1, true);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        String tipText = ((JToolTip) c).getTipText();
        if (tipText == null) {
            return new Dimension(0, 0);
        }
        setTextArea(new JTextArea(tipText));
        this.getRendererPane().removeAll();
        this.getRendererPane().add(getTextArea());
        getTextArea().setWrapStyleWord(true);
        int width = ((MultiLineToolTip) c).getFixedWidth();
        int columns = ((MultiLineToolTip) c).getColumns();

        if (columns > 0) {
            getTextArea().setColumns(columns);
            getTextArea().setSize(0, 0);
            getTextArea().setLineWrap(true);
            getTextArea().setSize(getTextArea().getPreferredSize());
        } else if (width > 0) {
            getTextArea().setLineWrap(true);
            Dimension d = getTextArea().getPreferredSize();
            d.width = width;
            d.height++;
            getTextArea().setSize(d);
        } else {
            getTextArea().setLineWrap(false);
        }

        Dimension dim = getTextArea().getPreferredSize();

        dim.height += 1;
        dim.width += 1;
        return dim;
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return this.getPreferredSize(c);
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        return this.getPreferredSize(c);
    }

}
