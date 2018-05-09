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
package sinalgo.gui.dialogs;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.gui.GuiHelper;

import javax.swing.*;
import java.awt.*;

/**
 * This is a JDialog that shows the percentual progress of a action.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class PercentualProgressDialog extends JDialog {

    private static final long serialVersionUID = 1320076393340365149L;

    private ProgressBarUser pBU;
    private JPanel jp = new JPanel();
    private JProgressBar jPB = new JProgressBar(0, 100);
    private JButton cancel = new JButton("Cancel");
    private JPanel buttonPanel = new JPanel();

    /**
     * This is the constructor for the progress bar.
     *
     * @param pbu   The ProgressBarUser using this progress bar.
     * @param title The title of the Dialog.
     */
    public PercentualProgressDialog(ProgressBarUser pbu, String title) {
        super();// non-blocking
        this.setTitle(title);
        this.create(pbu);
    }

    // /**
    // * This is the constructor for the progress bar.
    // *
    // * @param pbu The ProgressBarUser using this progress bar.
    // * @param title The title of the Dialog.
    // * @param cancelEnabled whether the cancelButton is enabled or not
    // */
    // public PercentualProgressDialog(ProgressBarUser pbu, String title, boolean
    // cancelEnabled){
    // super();
    // cancel.setEnabled(cancelEnabled);
    // this.setTitle(title);
    // create(pbu);
    // }

    /**
     * Constructs a progress bar that is attached to a parentGUI and is modal (blocks
     * the parentGUI until the progressbar is closed).
     *
     * @param pbu    The ProgressBarUser using this progress bar.
     * @param parent The parentGUI JDialog to attach the ProgressBar to.
     * @param title  The title of the Dialog.
     */
    public PercentualProgressDialog(ProgressBarUser pbu, JDialog parent, String title) {
        super(parent, title, true);
        this.create(pbu);
    }

    // /**
    // * Constructs a progress bar that is attached to a parentGUI and is modal (blocks
    // * the parentGUI until the progressbar is closed).
    // *
    // * @param pbu The ProgressBarUser using this progress bar.
    // * @param parentGUI The parentGUI JDialog to attach the ProgressBar to.
    // * @param title The title of the Dialog.
    // * @param cancelEnabled whether the cancelButton is enabled or not
    // */
    // public PercentualProgressDialog(ProgressBarUser pbu, JDialog parentGUI, String
    // title, boolean cancelEnabled){
    // super(parentGUI, title, true);
    // blocking = true;
    // cancel.setEnabled(cancelEnabled);
    // create(pbu);
    // }

    /**
     * Creates a ProgressBar depending on the progressbarUser and the title of the
     *
     * @param pbu The ProgressBarUser that uses this ProgressBar.
     */
    public void create(ProgressBarUser pbu) {
        GuiHelper.setWindowIcon(this);
        this.setPBU(pbu);

        this.getJPB().setStringPainted(true);
        this.getJp().add(this.getJPB());

        this.getButtonPanel().add(this.getCancel());

        this.setLayout(new BorderLayout());

        this.add(BorderLayout.NORTH, this.getJp());
        this.add(BorderLayout.SOUTH, this.getButtonPanel());

        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setSize(180, 90);

        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        this.getCancel().addActionListener(e -> {
            this.getPBU().cancelClicked();
            this.setTitle("Undoing...");
            this.getCancel().setEnabled(false);
        });
    }

    /**
     * This method initializes the ProgressBar and starts the update Thread.
     */
    public void init() {
        new UpdateThread().start();
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
    public void setPercentage(double percent) {
        this.getJPB().setValue((int) (percent));
    }

    private class UpdateThread extends Thread {

        @Override
        public void run() {
            PercentualProgressDialog.this.getPBU().performMethod();
        }
    }
}
