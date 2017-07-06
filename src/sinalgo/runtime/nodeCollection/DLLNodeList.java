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
package sinalgo.runtime.nodeCollection;

import sinalgo.nodes.Node;
import sinalgo.tools.storage.DoublyLinkedList;
import sinalgo.tools.storage.ReusableListIterator;

/**
 * This class implements a NodeList by extending the DoublyLinkedList class. It stores the nodes in a 
 * DoublyLinkedList.
 */
public class DLLNodeList extends DoublyLinkedList<Node> implements NodeListInterface{
	
	/**
	 * The only constructor for the DLLNodeList. It constructs the NodeList according to the parameters.
	 *
	 * @param keepFinger If set to true, entries keep their finger for later reuse (in this or a different list)
	 * when they are removed from this list. When set to false, the finger is removed.
	 */
	public DLLNodeList(boolean keepFinger){
		super(keepFinger);
	}
	
	public void addNode(Node nw) {
		this.append(nw);
	}

	private ReusableListIterator<Node> theIteratorInstance = super.iterator();
	
	public ReusableListIterator<Node> iterator() {
		theIteratorInstance.reset();
		return theIteratorInstance;
	}

	public boolean removeNode(Node nw) {
		return this.remove(nw);
	}

}
