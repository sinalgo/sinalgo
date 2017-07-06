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
package sinalgo.nodes.messages;


import java.util.ConcurrentModificationException;
import java.util.Iterator;

import sinalgo.tools.storage.ReusableIterator;
import sinalgo.tools.storage.SortableVector;

/**
 * This class stores the Packets in a vector. It extends the Vector class and overwrites the iterator()-method.
 * The iterator-method now returns a reusable iterator over the vertor. This helps reducing the amount of
 * objects that are allocated and garbage collected.
 */
@SuppressWarnings("serial")
public class PacketCollection extends SortableVector<Packet>{
	
	/**
	 * This is the one and only constructor for the PacketCollection-class. It generates a Vector with an
	 * initial size of 3.
	 */
	public PacketCollection(){
		super(3);
	}
	
	//the instance of the iterator over the PacketCollection
	private ReusableIter iter = null;
	
	/* (non-Javadoc)
	 * @see java.util.AbstractList#iterator()
	 */
	public Iterator<Packet> iterator(){
		if(iter == null){
			iter = new ReusableIter();
		}
		else{
			//reset the iterator instead of returning always a new one
			iter.reset();
		}
		return iter;
	}
	
	private Packet remove2(int pos){
		return remove(pos);
	}
	
	private class ReusableIter implements ReusableIterator<Packet>{

		//the counter for the number of modifications 
		private int expectedModCount;
		
		//the position in the vector
		private int position;
		
		
		/**
		 * Create a new instance of the ReusableIterator
		 */
		private ReusableIter(){
			position = 0;
			expectedModCount = modCount;
		}
		
		/**
		 * This method resets the reusable iterator. This makes the iterator reusable.
		 */
		public void reset(){
			position = 0;
			expectedModCount = modCount;
		}
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return (position+1 <= size());
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public Packet next() {
			checkForComodification();
			return elementAt(position++);
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			remove2(--position);
		}
		
		/**
		 * Tests whether the list has been modified other than through the iteration commands. If this is
		 * the case, the method throws a ConcurrentModificationException. 
		 * @throws ConcurrentModificationException if this list was modified other than through the iterators methods.  
		 */
		final void checkForComodification() {
			if (expectedModCount != modCount)
				throw new ConcurrentModificationException();
		}
	}
	
}
