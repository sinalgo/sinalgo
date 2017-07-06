/*
 Copyright (c) 2007, Distributed Computing Group (DCG)
                    ETH Zurich
                    Switzerland
                    dcg.ethz.ch

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the
   distribution.

 - Neither the name 'Sinalgo' nor the names of its contributors may be
   used to endorse or promote products derived from this software
   without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package sinalgo.gui.multiLineTooltip;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;

/**
 * The Tooltip to display if the user stops with the mouse over the graph panel. Displays information about
 * the edge or node under the current position.
 */
@SuppressWarnings("serial")
public class MultiLineToolTip extends JToolTip
{
	String tipText;
	JComponent component;
	
	/**
	 * The constructor for the MyToolTip class.
	 *
	 */
	public MultiLineToolTip(){
	    updateUI();
	}
	
	public void updateUI(){
	    setUI(MultiLineToolTipUI.createUI(this));
	}
	
	/**
	 * This method sets the number of colums the tooltip has.
	 *
	 * @param columns The number of columns the tooltip has.
	 */
	public void setColumns(int columns){
		this.columns = columns;
		this.fixedwidth = 0;
	}
	
	/**
	 * This method returns the number of columns the tooltip has.
	 *
	 * @return The number of columns the Tooltip has.
	 */
	public int getColumns(){
		return columns;
	}
	
	/**
	 * This method sets the fixed width for the tooltip.
	 *
	 * @param width The fixed width of the tooltip.
	 */
	public void setFixedWidth(int width){
		this.fixedwidth = width;
		this.columns = 0;
	}
	
	/**
	 * This method returns the fixes with of the tooltip.
	 *
	 * @return The fixed width of the tooltip.
	 */
	public int getFixedWidth(){
		return fixedwidth;
	}
	
	protected int columns = 0;
	protected int fixedwidth = 0;
}


// used such that the tooltip can display several lines of text (e.g. newlines)
class MultiLineToolTipUI extends BasicToolTipUI {
	static MultiLineToolTipUI sharedInstance = new MultiLineToolTipUI();
	Font smallFont; 			     
	static JToolTip tip;
	protected CellRendererPane rendererPane;
	
	private static JTextArea textArea ;
	
	public static ComponentUI createUI(JComponent c) {
	    return sharedInstance;
	}
	
	/**
	 * The constructor for the MultiLineToolTipUI class.
	 *
	 */
	public MultiLineToolTipUI() {
	    super();
	}
	
	public void installUI(JComponent c) {
	    super.installUI(c);
		tip = (JToolTip)c;
	    rendererPane = new CellRendererPane();
	    c.add(rendererPane);
	}
	
	public void uninstallUI(JComponent c) {
		super.uninstallUI(c);
		
	    c.remove(rendererPane);
	    rendererPane = null;
	}
	
	public void paint(Graphics g, JComponent c) {
	    Dimension size = c.getSize();
	    textArea.setBackground(c.getBackground());
		rendererPane.paintComponent(g, textArea, c, 1, 1,
					    size.width - 1, size.height - 1, true);
	}
	
	public Dimension getPreferredSize(JComponent c) {
		String tipText = ((JToolTip)c).getTipText();
		if (tipText == null)
			return new Dimension(0,0);
		textArea = new JTextArea(tipText );
		rendererPane.removeAll();
		rendererPane.add(textArea );
		textArea.setWrapStyleWord(true);
		int width = ((MultiLineToolTip)c).getFixedWidth();
		int columns = ((MultiLineToolTip)c).getColumns();
		
		if( columns > 0 ) {
			textArea.setColumns(columns);
			textArea.setSize(0,0);
			textArea.setLineWrap(true);
			textArea.setSize( textArea.getPreferredSize() );
		}	else if( width > 0 ) {
			textArea.setLineWrap(true);
			Dimension d = textArea.getPreferredSize();
			d.width = width;
			d.height++;
			textArea.setSize(d);
		}	else {
			textArea.setLineWrap(false);
		}
		
		Dimension dim = textArea.getPreferredSize();
		
		dim.height += 1;
		dim.width += 1;
		return dim;
	}
	
	public Dimension getMinimumSize(JComponent c) {
	    return getPreferredSize(c);
	}
	
	public Dimension getMaximumSize(JComponent c) {
	    return getPreferredSize(c);
	}
}
