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
package sinalgo.gui.popups;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.gui.GUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Common items for popup menus
 */
@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
public abstract class AbstractPopupMenu extends JPopupMenu {

    private static final long serialVersionUID = 6108642977345194041L;

    @Getter
    private GUI parentGUI;

    private JMenuItem zoomIn = new JMenuItem("Zoom In");
    private JMenuItem zoomOut = new JMenuItem("Zoom Out");

    protected AbstractPopupMenu() {
        this.getZoomIn().addActionListener(new ZoomListener());
        this.getZoomOut().addActionListener(new ZoomListener());
    }

    // Listening to the zoom in and zoom out action events
    private class ZoomListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (event.getActionCommand().equals(AbstractPopupMenu.this.getZoomIn().getActionCommand())) {
                AbstractPopupMenu.this.getParentGUI().zoomIn();
            } else if (event.getActionCommand().equals(AbstractPopupMenu.this.getZoomOut().getActionCommand())) {
                AbstractPopupMenu.this.getParentGUI().zoomOut();
            }
        }
    }
}
