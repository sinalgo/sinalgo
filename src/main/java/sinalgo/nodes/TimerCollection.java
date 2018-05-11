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
package sinalgo.nodes;

import sinalgo.nodes.timers.Timer;
import sinalgo.tools.storage.ReusableIterator;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Vector;

/**
 * This class is the collection of the timers of a node.
 */
public class TimerCollection implements Iterable<Timer> {

    // Note that a vector is not the best solution, because elements are also
    // removed from the
    // begining of the collection but the collection will often be filled with only
    // a few timers
    // and thus this does not matter.
    private Vector<Timer> timers = new Vector<>(0);

    // the instance of the reusable iterator
    private ReusableIter iter;

    // the number of modifications (addings in our case) that are done on this
    // collection.
    private int modCount;

    /**
     * This method returns the number of timers in this collection. This doesn't
     * only include the ones to fire in this round but also the ones scheduled in
     * the future.
     *
     * @return The number of Timers in this collection.
     */
    public int size() {
        return this.timers.size();
    }

    /**
     * This method adds a Timer into the collection.
     *
     * @param t The timer to add.
     */
    public void add(Timer t) {
        this.timers.add(t);
        this.modCount++;
    }

    /**
     * Removes the first occurence of the given timer object in this set of timers,
     * does nothing if the timer object is not contained in this set.
     * <p>
     * Note that this collections only holds the timers when simulating in
     * synchronous mode. In asynchronous mode, the timers are kept in as events in
     * the global event queue.
     * <p>
     * A more convenient way (and also faster way) to remove a timer is to add a
     * flag to each timer that indicates whether it should perform the action.
     * Instead of removing the timer, set this flag to false.
     *
     * @param t The timer to remove
     */
    public void remove(Timer t) {
        this.timers.remove(t);
    }

    /**
     * This method returns an iterator for the collection. Note that this method
     * doesn't generate a new instance but resets the ReusableIterator and returns
     * it.
     *
     * @return An iterator over the collection of timers.
     */
    @Override
    public Iterator<Timer> iterator() {
        if (this.iter == null) {
            this.iter = new ReusableIter();
        } else {
            this.iter.reset();
        }
        return this.iter;
    }

    /**
     * This class is a reusable iterator for the vector based collection. Note that
     * a vector is not the best solution, because elements are also removed from the
     * begining of the collection but the collection will often be filled with only
     * a few timers and thus this does not matter.
     */
    private class ReusableIter implements ReusableIterator<Timer> {

        // the counter for the number of modifications
        private int expectedModCount;

        // the position in the vector
        private int position;

        /**
         * Create a new instance of the ReusableIterator
         */
        private ReusableIter() {
            this.position = 0;
            this.expectedModCount = TimerCollection.this.modCount;
        }

        /**
         * This method resets the reusable iterator. This makes the iterator reusable.
         */
        @Override
        public void reset() {
            this.position = 0;
            this.expectedModCount = TimerCollection.this.modCount;
        }

        @Override
        public boolean hasNext() {
            return (this.position + 1 <= TimerCollection.this.timers.size());
        }

        @Override
        public Timer next() {
            this.checkForComodification();
            return TimerCollection.this.timers.elementAt(this.position++);
        }

        @Override
        public void remove() {
            TimerCollection.this.timers.remove(--this.position);
        }

        /**
         * Tests whether the list has been modified other than through the iteration
         * commands. If this is the case, the method throws a
         * ConcurrentModificationException.
         *
         * @throws ConcurrentModificationException if this list was modified other than through the iterators
         *                                         methods.
         */
        final void checkForComodification() {
            if (this.expectedModCount != TimerCollection.this.modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
}
