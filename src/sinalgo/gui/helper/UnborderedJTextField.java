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
package sinalgo.gui.helper;


import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;

import sinalgo.gui.multiLineTooltip.MultiLineToolTipJTextField;
import sinalgo.nodes.NotYetImplementedException;

/**
 * This is a class implementing a special form of a Text Field. It is just used to abbreviate the writing
 * effort for all the Dialogs.
 */
@SuppressWarnings("serial")
public class UnborderedJTextField extends MultiLineToolTipJTextField {
	
	private static Font boldHelvetica12 = new Font("Helvetica", Font.BOLD, 12);
	private static Font plainHelvetica12 = new Font("Helvetica", Font.PLAIN, 12);
	
	/**
	 * Creates a Text field having no border that is not editable and that has a font-type according to
	 * the parameter passed.
	 *
	 * @param s The String to display in the text field.
	 * @param type The type of the font. Use Font.BOLD or Font.PLAIN
	 */
	public UnborderedJTextField(String s, int type){
		super(s);
		this.setEditable(false);
		this.setBorder(BorderFactory.createEmptyBorder());
		switch(type){
			case Font.BOLD:
				this.setFont(boldHelvetica12);
				break;
			case Font.PLAIN:
				this.setFont(plainHelvetica12);
				break;
			default:
				throw new NotYetImplementedException("There this Font-Style is not supported.");
		}
		this.revalidate();
	}
	
	/**
	 * Generates a TextField without a border.
	 */
	public UnborderedJTextField(){
		this.setBorder(BorderFactory.createEmptyBorder());
		this.getPreferredSize();
	}
	
	public Dimension getPreferredSize(){
		Dimension d = super.getPreferredSize();
		//increase the preferred width a bit to avoid cutting the border of the text.
		d.width += 2;
		return d;
	}
}
