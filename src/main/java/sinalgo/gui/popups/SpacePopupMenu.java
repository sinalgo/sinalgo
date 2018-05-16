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
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.nodes.Position;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The class for the popupmenus displayed on the graph panel when the user
 * presses the right mouse button over a place, where there is no node and no
 * edge.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class SpacePopupMenu extends AbstractPopupMenu implements ActionListener {

    private static final long serialVersionUID = 8356598949303688723L;

    private Point pos;
    private JMenuItem add = new JMenuItem("Add Node");

    /**
     * The constructor for the SpacePopupMenu class.
     *
     * @param p The Frame to add the AddNodeDialog to if the user clicked AddNode.
     */
    public SpacePopupMenu(GUI p) {
        this.setParentGUI(p);
        this.getAdd().addActionListener(this);
    }

    /**
     * This method composes a Popupmenu for the given position.
     *
     * @param p The position the user clicked to.
     */
    public void compose(Point p) {
        this.setPos(p);

        this.removeAll();

        if (this.getParentGUI().getTransformator().supportReverseTranslation()) {
            this.add(this.getAdd());
            this.addSeparator();
        }

        this.add(this.getZoomIn());
        this.add(this.getZoomOut());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(this.getAdd().getActionCommand())) {
            PositionTransformation pt = this.getParentGUI().getTransformator();
            if (pt.supportReverseTranslation()) {
                pt.translateToLogicPosition(this.getPos().x, this.getPos().y);
                this.getParentGUI().addSingleNode(new Position(pt.getLogicX(), pt.getLogicY(), pt.getLogicZ()));
            }
        }
    }
}
