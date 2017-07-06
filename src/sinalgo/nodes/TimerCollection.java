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
package sinalgo.nodes;


import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Vector;

import sinalgo.nodes.timers.Timer;
import sinalgo.tools.storage.ReusableIterator;

/**
 * This class is the collection of the timers of a node.
 */
public class TimerCollection implements Iterable<Timer>{
	//Note that a vector is not the best solution, because elements are also removed from the 
	//begining of the collection but the collection will often be filled with only a few timers 
	//and thus this does not matter.
	private Vector<Timer> timers = new Vector<Timer>(0);
	
	//the instance of the reusable iterator
	private ReusableIter iter = null;
	
	//the number of modifications (addings in our case) that are done on this collection.
	private int modCount = 0;
	
	/**
	 * This method returns the number of timers in this collection. This doesn't only include 
	 * the ones to fire in this round but also the ones scheduled in the future.
	 * 
	 * @return The number of Timers in this collection.
	 */
	public int size(){
		return timers.size();
	}
	
	/**
	 * This method adds a Timer into the collection.
	 * 
	 * @param t The timer to add.
	 */
	public void add(Timer t){
		timers.add(t);
		modCount++;
	}
	
	/**
	 * Removes the first occurence of the given timer object in this 
	 * set of timers, does nothing if the timer object is not contained
	 * in this set.
	 * <p>
	 * Note that this collections only holds the timers when simulating
	 * in synchronous mode. In asynchronous mode, the timers are kept
	 * in as events in the global event queue.
   * <p>
   * A more convenient way (and also faster way) to remove a timer
   * is to add a flag to each timer that indicates whether it should perform
   * the action. Instead of removing the timer, set this flag to false.
	 * @param t The timer to remove
	 */
	public void remove(Timer t) {
		timers.remove(t);
	}
	
	/**
	 * This method returns an iterator for the collection. Note that this method doesn't generate
	 * a new instance but resets the ReusableIterator and returns it.
	 * 
	 * @return An iterator over the collection of timers.
	 */
	public Iterator<Timer> iterator(){
		if(iter == null){
			iter = new ReusableIter();
		}
		else{
			iter.reset();
		}
		return iter;
	}
	
	/**
	 * This class is a reusable iterator for the vector based collection. Note that a vector is 
	 * not the best solution, because elements are also removed from the begining of the collection
	 * but the collection will often be filled with only a few timers and thus this does not matter.
	 */
	private class ReusableIter implements ReusableIterator<Timer>{

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
			return (position+1 <= timers.size());
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public Timer next() {
			checkForComodification();
			return timers.elementAt(position++);
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			timers.remove(--position);
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
