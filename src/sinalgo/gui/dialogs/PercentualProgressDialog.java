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
package sinalgo.gui.dialogs;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import sinalgo.gui.GuiHelper;

/**
 * This is a JDialog that shows the percentual progress of a action.
 */
@SuppressWarnings("serial")
public class PercentualProgressDialog extends JDialog{

	private ProgressBarUser pBU;
	private JPanel jp = new JPanel();
	private JProgressBar jPB = new JProgressBar(0, 100);
	private JButton cancel = new JButton("Cancel");
	private JPanel buttonPanel = new JPanel();
	
	/**
	 * This is the constructor for the progress bar.
	 *
	 * @param pbu The ProgressBarUser using this progress bar.
	 * @param title The title of the Dialog.
	 */
	public PercentualProgressDialog(ProgressBarUser pbu, String title){
		super();// non-blocking
		this.setTitle(title);
		create(pbu);
	}
	
//	/**
//	 * This is the constructor for the progress bar.
//	 *
//	 * @param pbu The ProgressBarUser using this progress bar.
//	 * @param title The title of the Dialog.
//	 * @param cancelEnabled whether the cancelButton is enabled or not
//	 */
//	public PercentualProgressDialog(ProgressBarUser pbu, String title, boolean cancelEnabled){
//		super();
//		cancel.setEnabled(cancelEnabled);
//		this.setTitle(title);
//		create(pbu);
//	}

	/**
	 * Constructs a progress bar that is attached to a parent and is modal (blocks
	 * the parent until the progressbar is closed).
	 *
	 * @param pbu The ProgressBarUser using this progress bar.
	 * @param parent The parent JDialog to attach the ProgressBar to.
	 * @param title The title of the Dialog.
	 */
	public PercentualProgressDialog(ProgressBarUser pbu, JDialog parent, String title){
		super(parent, title, true);
		create(pbu);
	}

//	/**
//	 * Constructs a progress bar that is attached to a parent and is modal (blocks
//	 * the parent until the progressbar is closed).
//	 *
//	 * @param pbu The ProgressBarUser using this progress bar.
//	 * @param parent The parent JDialog to attach the ProgressBar to.
//	 * @param title The title of the Dialog.
//	 * @param cancelEnabled whether the cancelButton is enabled or not
//	 */
//	public PercentualProgressDialog(ProgressBarUser pbu, JDialog parent, String title, boolean cancelEnabled){
//		super(parent, title, true);
//		blocking = true;
//		cancel.setEnabled(cancelEnabled);
//		create(pbu);
//	}
	
	/**
	 * Creates a ProgressBar depending on the progressbarUser and the title of the 
	 * 
	 * @param pbu The ProgressBarUser that uses this ProgressBar.
	 */
	public void create(ProgressBarUser pbu){
		GuiHelper.setWindowIcon(this);
		pBU = pbu;
		
		jPB.setStringPainted(true);
		jp.add(jPB);
		
		buttonPanel.add(cancel);
		
		this.setLayout(new BorderLayout());
		
		this.add(BorderLayout.NORTH, jp);
		this.add(BorderLayout.SOUTH, buttonPanel);
		
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		this.setSize(180, 90);
		
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		cancel.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				pBU.cancelClicked();
				setTitle("Undoing...");
				cancel.setEnabled(false);
			}
		});
	}

	/**
	 * This method initializes the ProgressBar and starts the update Thread.
	 */
	public void init(){
		UpdateThread updateThread = new UpdateThread();
		updateThread.start();
		this.setVisible(true); // blocking if this dialog was started with the modal bit set to true.
	}
	
	/**
	 * This method is used to destroy the Progress. It sets it invisible.
	 */
	public void finish() {
		this.dispose(); // setVisible(false) sometimes leaves back an empty window
	}
	
	/**
	 * This method resets the value of the progress bar.
	 * 
	 * @param percent The percentage of the progress.
	 */
	public void setPercentage(double percent){
		jPB.setValue((int)(percent));
	}
	
	private class UpdateThread extends Thread{
		public void run(){
			pBU.performMethod();
		}
	}
}
