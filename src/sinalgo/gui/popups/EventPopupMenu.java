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
package sinalgo.gui.popups;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;

import sinalgo.runtime.events.Event;

/**
 * @author rflury
 *
 */
@SuppressWarnings("serial")
public class EventPopupMenu extends JPopupMenu implements ActionListener {
	Event event;
	JList list;
	ListCellRenderer renderer;
	private JMenuItem info = new JMenuItem("Info About This Event");
	private JMenuItem delete = new JMenuItem("Delete Event");
	private JMenuItem deleteAll = new JMenuItem("Delete All Events");
	
	public EventPopupMenu(Event e, JList l, ListCellRenderer lcr) {
		this.event = e;
		this.list = l;
		this.renderer = lcr;
		info.addActionListener(this);
		delete.addActionListener(this);
		deleteAll.addActionListener(this);
		
		this.add(info);
		this.addSeparator();
		this.add(delete);
		this.add(deleteAll);
		
		
		
		this.addComponentListener(new ComponentListener() {
			public void componentHidden(ComponentEvent e) {
				list.setCellRenderer(renderer);
				list.repaint();
			}

			public void componentMoved(ComponentEvent e) {
			}

			public void componentResized(ComponentEvent e) {
			}

			public void componentShown(ComponentEvent e) {
			}
		});
		
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(info.getActionCommand())){
			JOptionPane.showMessageDialog(null, event.getEventListText(false)+"\n"+event.getEventListToolTipText(false), "Information about an Event", JOptionPane.NO_OPTION);
		} else if(e.getActionCommand().equals(delete.getActionCommand())){
			sinalgo.runtime.Runtime.removeEvent(event);
		} else if(e.getActionCommand().equals(deleteAll.getActionCommand())){
			sinalgo.runtime.Runtime.removeAllAsynchronousEvents();
		}
		list.setCellRenderer(renderer);
		list.repaint();
	}
	
}
