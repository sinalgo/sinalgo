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
package sinalgo.gui.helper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;

/**
 * Similar to FlowLayout, but allows to set max width and then starts a new
 * line.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class MultiLineFlowLayout implements LayoutManager {

    private int maxWidth;
    private int hGap; // horizontal gap between two elements
    private int vGap; // vertical gap between two elements

    /**
     * @param maxWidth Max. width of the container
     * @param hGap     the horizontal gap between two elements
     * @param vGap     the vertical gap between two elements
     */
    public MultiLineFlowLayout(int maxWidth, int hGap, int vGap) {
        this.setMaxWidth(maxWidth);
        this.setHGap(hGap);
        this.setVGap(vGap);
    }

    private int exactLineHeight = -1;

    /**
     * Sets the exact Line Hight. Set the height to <= 0 to disable it
     *
     * @param height The exact height of the line.
     */
    public void setExactLineHight(int height) {
        this.setExactLineHeight(height); // set to <= 0 to disable
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();

            int totalWidth = this.getMaxWidth() - insets.left - insets.right;
            int height = 0;
            int width = 0;
            int maxHeightOfThisLine = 0;

            int componentCount = parent.getComponentCount();
            for (int i = 0; i < componentCount; i++) {
                Component c = parent.getComponent(i);
                Dimension d = c.getPreferredSize();
                if (width + d.width < totalWidth || width == 0) { // also accept any first component on this line
                    width += d.width + this.getHGap();
                    maxHeightOfThisLine = Math.max(maxHeightOfThisLine, d.height);
                } else { // this component must go onto the next line
                    if (this.getExactLineHeight() > 0) {
                        maxHeightOfThisLine = this.getExactLineHeight();
                    }
                    height += maxHeightOfThisLine + this.getVGap();
                    maxHeightOfThisLine = d.height;
                    width = d.width + this.getHGap();
                }
            }
            height += maxHeightOfThisLine;
            return new Dimension(this.getMaxWidth(), insets.top + insets.bottom + height);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return this.preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();

            int totalWidth = this.getMaxWidth() - insets.left - insets.right;
            int height = 0;
            int width = 0;
            int maxHeightOfThisLine = 0;

            int componentCount = parent.getComponentCount();
            for (int i = 0; i < componentCount; i++) {
                Component c = parent.getComponent(i);
                Dimension d = c.getPreferredSize();
                c.setSize(d); // we need to set the size of the components
                if (width + d.width < totalWidth || width == 0) { // also accept any first component on this line
                    c.setLocation(insets.left + width, insets.top + height);
                    width += d.width + this.getHGap();
                    maxHeightOfThisLine = Math.max(maxHeightOfThisLine, d.height);
                } else { // this component must go onto the next line
                    if (this.getExactLineHeight() > 0) {
                        maxHeightOfThisLine = this.getExactLineHeight();
                    }
                    height += maxHeightOfThisLine + this.getVGap();
                    c.setLocation(insets.left, insets.top + height); // width is 0
                    maxHeightOfThisLine = d.height;
                    width = d.width + this.getHGap();
                }
            }
        }
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        // not used by this class
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        // not used by this class
    }
}
