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
package sinalgo.tools.logging;

/**
 * Lists the log-levels. Levels set to true will be included 
 * in the log-file. 
 */
public class LogL {
	/**
	 * Stuff that is always logged. You should not set this to false.
	 */
	public static final boolean ALWAYS = true;
	/**
	 * Stuff that is always logged. You should not set this to false.
	 */
	public static final boolean EVENT_QUEUE_DETAILS = false;
	/**
	 * details about errors. You should not set this to false.
	 */
	public static final boolean ERROR_DETAIL = true;
	/**
	 * Prints out all the warnings. You should not set this to false.
	 */
	public static final boolean WARNING = true;
	/**
	 * Prints out all info messages. You should not set this to false.
	 */
	public static final boolean INFO = true; 
	/**
	 * Hints about strange behaviour, that perhaps wasn't intended. You should not set this to false.
	 */
	public static final boolean HINTS = true;
	/**
	 * details about the round
	 */
	public static final boolean ROUND_DETAIL = false;
	/**
	 * details about the messages
	 */
	public static final boolean MESSAGE_DETAIL = false;
	/**
	 * Details about the interference.
	 */
	public static final boolean INTERFERENCE_DETAIL = false;
	/**
	 * details about the nodes
	 */
	public static final boolean NODE_DETAIL = false;
	/**
	 * Details about the connectivity of the nodes.
	 */
	public static final boolean CONNECTIVITY_DETAIL = false;
	/**
	 * GUI calling sequence
	 */
	public static final boolean GUI_SEQ = false;
	/**
	 * Details about the GUI
	 */
	public static final boolean GUI_DETAIL = false;
	/**
	 * More details about the GUI and the i/o of the user to the GUI.
	 */
	public static final boolean GUI_ULTRA_DETAIL = false;
}


