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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;

/**
 * Grid Layout which allows components of differrent sizes. 
 */
@SuppressWarnings("serial")
public class NonRegularGridLayout extends GridLayout {

	/**
	 * This creates an instance of the NonRegularGridLayout with 1 row and no colums without gaps.
	 */
	public NonRegularGridLayout() {
	    this(1, 0, 0, 0);
	}
	
	/**
	 * This creates an instance of the NonRegularGridLayout with given number of rows and colums.
	 *
	 * @param rows The number of rows to prepare the grid for.
	 * @param cols The number of colums to prepare the grid for.
	 */
	public NonRegularGridLayout(int rows, int cols) {
		this(rows, cols, 0, 0);
	}
	
	/**
	 * This creates an instance of the NonRegularGridLayout with given number of rows, colums, and given gaps 
	 * between the rows and the colums respectively.
	 *
	 * @param rows The number of rows to prepare the Grid for.
	 * @param cols The number of colums to prepare the Grid for.
	 * @param hgap The horizontal gap.
	 * @param vgap The vertical gap.
	 */
	public NonRegularGridLayout(int rows, int cols, int hgap, int vgap) {
		super(rows, cols, hgap, vgap);
	}
	
	private boolean alignToLeft = false;
	
	/**
	 * Sets whether all components of the grid should be left-aligned and the
	 * remaining space given to the right-most cells.
	 * @param alignLeft
	 */
	public void setAlignToLeft (boolean alignLeft) {
		alignToLeft = alignLeft;
	}
	
	public Dimension preferredLayoutSize(Container parent) {
		//System.err.println("preferredLayoutSize");
	    synchronized (parent.getTreeLock()) {
		    Insets insets = parent.getInsets();
		    int ncomponents = parent.getComponentCount();
		    int nrows = getRows();
		    int ncols = getColumns();
		    if (nrows > 0) {
		    	ncols = (ncomponents + nrows - 1) / nrows;
		    } 
		    else {
		    	nrows = (ncomponents + ncols - 1) / ncols;
		    }
		    int[] w = new int[ncols];
		    int[] h = new int[nrows];
		    for (int i = 0; i < ncomponents; i ++) {
		    	int r = i / ncols;
		        int c = i % ncols;
		        Component comp = parent.getComponent(i);
		        Dimension d = comp.getPreferredSize();
		        if (w[c] < d.width) {
		          w[c] = d.width;
		        }
		        if (h[r] < d.height) {
		          h[r] = d.height;
		        }
		    }
		    int nw = 0;
		    for (int j = 0; j < ncols; j ++) {
		    	nw += w[j];
		    }
		    int nh = 0;
		    for (int i = 0; i < nrows; i ++) {
		    	nh += h[i];
		    }
		    return new Dimension(insets.left + insets.right + nw + (ncols-1)*getHgap(), 
		    		insets.top + insets.bottom + nh + (nrows-1)*getVgap());
	    }
	}
	
	public Dimension minimumLayoutSize(Container parent) {
		//System.err.println("minimumLayoutSize");
	    synchronized (parent.getTreeLock()) {
		    Insets insets = parent.getInsets();
		    int ncomponents = parent.getComponentCount();
		    int nrows = getRows();
		    int ncols = getColumns();
		    if (nrows > 0) {
		    	ncols = (ncomponents + nrows - 1) / nrows;
		    } 
		    else {
		        nrows = (ncomponents + ncols - 1) / ncols;
		    }
		    int[] w = new int[ncols];
		    int[] h = new int[nrows];
		    for (int i = 0; i < ncomponents; i ++) {
		        int r = i / ncols;
		        int c = i % ncols;
		        Component comp = parent.getComponent(i);
		        Dimension d = comp.getMinimumSize();
		        if (w[c] < d.width) {
		        	w[c] = d.width;
		        }
		        if (h[r] < d.height) {
		        	h[r] = d.height;
		        }
		    }
		    int nw = 0;
		    for (int j = 0; j < ncols; j ++) {
		    	nw += w[j];
		    }
		    int nh = 0;
		    for (int i = 0; i < nrows; i ++) {
		        nh += h[i];
		    }
		    return new Dimension(insets.left + insets.right + nw + (ncols-1)*getHgap(), 
		    		insets.top + insets.bottom + nh + (nrows-1)*getVgap());
	    }
	}
	
	public void layoutContainer(Container parent) {
		//System.err.println("layoutContainer");
	    synchronized (parent.getTreeLock()) {
		    Insets insets = parent.getInsets();
		    int ncomponents = parent.getComponentCount();
		    int nrows = getRows();
		    int ncols = getColumns();
		    if (ncomponents == 0) {
		    	return;
		    }
		    if (nrows > 0) {
		        ncols = (ncomponents + nrows - 1) / nrows;
		    } 
		    else {
		        nrows = (ncomponents + ncols - 1) / ncols;
		    }
		    int hgap = getHgap();
		    int vgap = getVgap();
			// scaling factors      
		    Dimension pd = preferredLayoutSize(parent);

		    if(alignToLeft) {
		    	int[] w = new int[ncols]; // maximal width
		    	int[] h = new int[nrows]; // maximal height
		    	for (int i = 0; i < ncomponents; i ++) {
		    		int r = i / ncols;
		    		int c = i % ncols;
		    		Component comp = parent.getComponent(i);
		    		Dimension d = comp.getPreferredSize();
		    		if (w[c] < d.width) {
		        	w[c] = d.width;
		        }
		        if (h[r] < d.height) {
		        	h[r] = d.height;
		        }
		    	}
		    	int totW = 0;
		    	for(int i : w) {
		    		totW += i + hgap;
		    	}
		    	totW -= hgap;
		    	if(totW < parent.getWidth()) {
		    		w[ncols-1] += parent.getWidth() - totW;
		    	}
		    	
		    	for (int c = 0, x = insets.left; c < ncols; c ++) {
		    		for (int r = 0, y = insets.top; r < nrows; r ++) {
		    			int i = r * ncols + c;
		    			if (i < ncomponents) {
		    				parent.getComponent(i).setBounds(x, y, w[c], h[r]);
		    			}
		    			y += h[r] + vgap;
		    		}
		    		x += w[c] + hgap;
		    	}
		    } else {
		    	double sw = (1.0 * parent.getWidth()) / pd.width;
		    	double sh = (1.0 * parent.getHeight()) / pd.height;
		    	// scale
		    	int[] w = new int[ncols]; // maximal width
		    	int[] h = new int[nrows]; // maximal height
		    	for (int i = 0; i < ncomponents; i ++) {
		    		int r = i / ncols;
		    		int c = i % ncols;
		    		Component comp = parent.getComponent(i);
		    		Dimension d = comp.getPreferredSize();
		    		d.width = (int) (sw * d.width);
		        d.height = (int) (sh * d.height);
		        if (w[c] < d.width) {
		        	w[c] = d.width;
		        }
		        if (h[r] < d.height) {
		        	h[r] = d.height;
		        }
		    	}
		    	for (int c = 0, x = insets.left; c < ncols; c ++) {
		    		for (int r = 0, y = insets.top; r < nrows; r ++) {
		    			int i = r * ncols + c;
		    			if (i < ncomponents) {
		    				parent.getComponent(i).setBounds(x, y, w[c], h[r]);
		    			}
		    			y += h[r] + vgap;
		    		}
		    		x += w[c] + hgap;
		    	}
		    }
	    }
	}
	}
	
