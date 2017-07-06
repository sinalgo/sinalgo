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

import java.awt.Font;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JToolTip;

/**
 * This class extends the JComboBox class and overwrites the createToolTip method to add the class 
 * a multiline tooltip instead of the normal single line tooltip.
 */
@SuppressWarnings("serial")
public class MultiLineToolTipJComboBox extends JComboBox {
	/**
	 * Creates a <code>MultiLineToolTipJComboBox</code> that takes it's items from an
	 * existing <code>ComboBoxModel</code>.  Since the
	 * <code>ComboBoxModel</code> is provided, a combo box created using
	 * this constructor does not create a default combo box model and
	 * may impact how the insert, remove and add methods behave.
	 *
	 * @param aModel the <code>ComboBoxModel</code> that provides the 
	 * 		displayed list of items
	 */
	public MultiLineToolTipJComboBox(ComboBoxModel aModel) {
		super(aModel);
		this.setFont(this.getFont().deriveFont(Font.PLAIN));
	}
	
	/** 
	 * Creates a <code>MultiLineToolTipJComboBox</code> that contains the elements
	 * in the specified array.  By default the first item in the array
	 * (and therefore the data model) becomes selected.
	 *
	 * @param items  an array of objects to insert into the combo box
	 */
	public MultiLineToolTipJComboBox(final Object items[]) {
		super(items);
		this.setFont(this.getFont().deriveFont(Font.PLAIN));
	}
	
	/**
	 * Creates a <code>MultiLineToolTipJComboBox</code> that contains the elements
	 * in the specified Vector.  By default the first item in the vector
	 * and therefore the data model) becomes selected.
	 *
	 * @param items  an array of vectors to insert into the combo box
	 */
	public MultiLineToolTipJComboBox(Vector<?> items) {
		super(items);
		this.setFont(this.getFont().deriveFont(Font.PLAIN));
	}
	
	/**
	 * Creates a <code>MultiLineToolTipJComboBox</code> with a default data model.
	 * The default data model is an empty list of objects.
	 * Use <code>addItem</code> to add items.  By default the first item
	 * in the data model becomes selected.
	 */
	public MultiLineToolTipJComboBox() {
		super();
	}
	
	public JToolTip createToolTip(){
		return new MultiLineToolTip();
	}
}
