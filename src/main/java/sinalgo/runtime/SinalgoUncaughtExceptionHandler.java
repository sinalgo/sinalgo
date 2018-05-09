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
package sinalgo.runtime;

import sinalgo.exception.SinalgoFatalException;
import sinalgo.exception.SinalgoWrappedException;
import sinalgo.tools.Tools;
import sinalgo.tools.logging.LogL;
import sinalgo.tools.logging.Logging;

import javax.swing.*;
import java.awt.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Objects;

/**
 * This class implements a UncoughtExceptionHandler. It is used to catch all the
 * uncaught exceptions and forward it to the Main as a fatal error.
 */
public class SinalgoUncaughtExceptionHandler implements UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (e instanceof IllegalComponentStateException
                && Objects.equals(e.getMessage(), "component must be showing on the screen to determine its location")) {
            return;
        } else if (e instanceof SinalgoWrappedException) {
            fatalError(e.getCause());
        } else if (e instanceof OutOfMemoryError) {
            SinalgoRuntime.setNodes(null);
            Tools.disposeRecycledObjects(Logging.getLogger().getOutputStream());
            System.gc();
            Runtime r = Runtime.getRuntime();
            long maxMem = r.maxMemory() / 1048576;
            fatalError("Sinalgo ran out of memory. (" + maxMem + " MB is not enough). \n"
                    + "To allow the VM to use more memory, modify the javaVMmaxMem entry of the config file.", e);
        } else if (e instanceof SinalgoFatalException) {
            fatalError(String.format("%s" + (e.getCause() != null ? ":\n%s" : ""), e.getMessage(), e.getCause()),
                    (e.getCause() != null ? e.getCause() : e));
        }

        StringBuilder st = new StringBuilder("    ");
        StackTraceElement[] ste = e.getStackTrace();
        for (StackTraceElement element : ste) {
            st.append(element.toString()).append("\n    ");
        }
        fatalError("There was an Exception in Thread " + t + " \n\n"
                + "Exception: " + e + ": \n\n"
                + "Message: " + e.getMessage() + "\n\n"
                + "Cause: " + e.getCause() + "\n\n"
                + "StackTrace: " + st, e);
    }

    /**
     * Exits the application due to a fatal error.
     * <p>
     * Before exiting, an error-message is diplayed if in GUI-mode. In any case, the
     * error is written to System.err.
     *
     * @param t The exception causing the error.
     */
    private static void fatalError(Throwable t) {
        if (t.getCause() != null) {
            fatalError("----------------------------------------------\n"
                    + t + "\n"
                    + "----------------------------------------------\n"
                    + "Message:\n" + t.getMessage() + "\n"
                    + "----------------------------------------------\n"
                    + "Cause:\n"
                    + "----------------------------------------------\n"
                    + t.getCause() + "\n"
                    + "----------------------------------------------\n", t);
        } else {
            String message = t.toString()
                    + "\n\n" + "Message: " + t.getMessage()
                    + "\n\n";
            fatalError(message, null);
        }
    }

    /**
     * Exits the application due to a fatal error.
     * <p>
     * Before exiting, an error-message is diplayed if in GUI-mode. In any case, the
     * error is written to System.err.
     *
     * @param message The message containing the error description.
     */
    private static void fatalError(String message, Throwable cause) {
        if (Global.isGuiMode()) {
            if (Main.getRuntime() != null) {
                JOptionPane.showMessageDialog(((GUIRuntime) Main.getRuntime()).getGUI(),
                        Tools.wrapAndCutToLines(message, 30), "Fatal Error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, Tools.wrapAndCutToLines(message, 30), "Fatal Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        if (Logging.isActivated()) {
            Global.getLog().logln(LogL.ALWAYS, "\n" + message + "\n\n" + getStackTrace(cause));
        } else {
            System.err.println("\n" + "-------------------------------------------------------\n"
                    + "Fatal Error\n"
                    + "-------------------------------------------------------\n"
                    + message + "\n"
                    + "-------------------------------------------------------\n"
                    + "Stack Trace\n"
                    + "-------------------------------------------------------\n"
                    + getStackTrace(cause)
                    + "-------------------------------------------------------\n");
        }

        try {
            // The main thread has an exception handler installed, that would again call
            // fatalError, and possibly cause an infinite loop
            Global.getCustomGlobal().onFatalErrorExit();
        } catch (Throwable t) {
            System.err.println(
                    "\n\n Furthermore, an exception was thrown "
                            + "in CustomGlobal.onFatalErrorExit():\n"
                            + t.getMessage());
        }
        Main.cleanup();
        System.exit(1);
    }

    /**
     * @return The current stacktrace as a string.
     */
    private static String getStackTrace(Throwable t) {
        StringBuilder s = new StringBuilder();
        StackTraceElement[] list = t.getStackTrace();
        if (list.length <= 2) {
            return ""; // no stack trace
        }
        for (int i = 2; true; i++) {
            s.append(list[i].toString());
            if (i >= list.length - 1) {
                break;
            }
            s.append("\n");
        }
        return s.toString();
    }

}
