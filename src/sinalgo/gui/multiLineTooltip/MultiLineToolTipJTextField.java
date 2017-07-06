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


import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.text.Document;

/**
 * This class extends the JTextField class and overwrites the createToolTip method to add the class 
 * a multiline tooltip instead of the normal single line tooltip.
 */
@SuppressWarnings("serial")
public class MultiLineToolTipJTextField extends JTextField {
	
	/**
	 * The default constructor. Just passes the arguments to the JTextField-class'-constructor.
	 */
	public MultiLineToolTipJTextField(){
		super();
	}

	/**
	 * Just passes the arguments to the JTextField-class'-constructor.
	 * @param text the text to be displayed, or <code>null</code>
	 */
	public MultiLineToolTipJTextField(String text){
		super(text);
	}

	/**
	 * Just passes the arguments to the JTextField-class'-constructor.
	 * @param columns  the number of columns to use to calculate 
     *   the preferred width; if columns is set to zero, the
     *   preferred width will be whatever naturally results from
     *   the component implementation
	 */
	public MultiLineToolTipJTextField(int columns){
		super(columns);
	}
	
	/**
	 * Just passes the arguments to the JTextField-class'-constructor.
	 * @param text the text to be displayed, or <code>null</code>
     * @param columns  the number of columns to use to calculate 
     *   the preferred width; if columns is set to zero, the
     *   preferred width will be whatever naturally results from
     *   the component implementation
	 */
	public MultiLineToolTipJTextField(String text, int columns){
		super(text, columns);
	}
	
	/**
	 * Just passes the arguments to the JTextField-class'-constructor.
     * @param doc  the text storage to use; if this is <code>null</code>,
     *		a default will be provided by calling the
     *		<code>createDefaultModel</code> method
     * @param text  the initial string to display, or <code>null</code>
     * @param columns  the number of columns to use to calculate 
     *   the preferred width >= 0; if <code>columns</code>
     *   is set to zero, the preferred width will be whatever
     *   naturally results from the component implementation
     * @exception IllegalArgumentException if <code>columns</code> < 0
	 */
	public MultiLineToolTipJTextField(Document doc, String text, int columns){
		super(doc, text, columns);
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#createToolTip()
	 */
	public JToolTip createToolTip(){
		return new MultiLineToolTip();
	}
}
