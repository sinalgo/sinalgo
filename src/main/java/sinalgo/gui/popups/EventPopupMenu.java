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

import sinalgo.runtime.SinalgoRuntime;
import sinalgo.runtime.events.Event;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * @author rflury
 */
public class EventPopupMenu extends JPopupMenu implements ActionListener {

    private static final long serialVersionUID = 315706472796346139L;

    private Event event;
    private JList list;
    private ListCellRenderer renderer;
    private JMenuItem info = new JMenuItem("Info About This Event");
    private JMenuItem delete = new JMenuItem("Delete Event");
    private JMenuItem deleteAll = new JMenuItem("Delete All Events");

    public EventPopupMenu(Event e, JList l, ListCellRenderer lcr) {
        this.event = e;
        this.list = l;
        this.renderer = lcr;
        this.info.addActionListener(this);
        this.delete.addActionListener(this);
        this.deleteAll.addActionListener(this);

        this.add(this.info);
        this.addSeparator();
        this.add(this.delete);
        this.add(this.deleteAll);

        this.addComponentListener(new ComponentListener() {

            @Override
            public void componentHidden(ComponentEvent e) {
                EventPopupMenu.this.list.setCellRenderer(EventPopupMenu.this.renderer);
                EventPopupMenu.this.list.repaint();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentResized(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }
        });

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(this.info.getActionCommand())) {
            JOptionPane.showMessageDialog(null,
                    this.event.getEventListText(false) + "\n" + this.event.getEventListToolTipText(false),
                    "Information about an Event", JOptionPane.INFORMATION_MESSAGE);
        } else if (e.getActionCommand().equals(this.delete.getActionCommand())) {
            SinalgoRuntime.removeEvent(this.event);
        } else if (e.getActionCommand().equals(this.deleteAll.getActionCommand())) {
            SinalgoRuntime.removeAllAsynchronousEvents();
        }
        this.list.setCellRenderer(this.renderer);
        this.list.repaint();
    }

}
