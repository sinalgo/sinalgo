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
package sinalgo.nodes.messages;

import sinalgo.tools.storage.ReusableIterator;
import sinalgo.tools.storage.SortableVector;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * This class stores the Packets in a vector. It extends the Vector class and
 * overwrites the iterator()-method. The iterator-method now returns a reusable
 * iterator over the vertor. This helps reducing the amount of objects that are
 * allocated and garbage collected.
 */
public class PacketCollection extends SortableVector<Packet> {

    private static final long serialVersionUID = -8788148921758142918L;

    /**
     * This is the one and only constructor for the PacketCollection-class. It
     * generates a Vector with an initial size of 3.
     */
    public PacketCollection() {
        super(3);
    }

    // the instance of the iterator over the PacketCollection
    private ReusableIter iter;

    @Override
    public Iterator<Packet> iterator() {
        if (this.iter == null) {
            this.iter = new ReusableIter();
        } else {
            // reset the iterator instead of returning always a new one
            this.iter.reset();
        }
        return this.iter;
    }

    private Packet remove2(int pos) {
        return this.remove(pos);
    }

    private class ReusableIter implements ReusableIterator<Packet> {

        // the counter for the number of modifications
        private int expectedModCount;

        // the position in the vector
        private int position;

        /**
         * Create a new instance of the ReusableIterator
         */
        private ReusableIter() {
            this.position = 0;
            this.expectedModCount = PacketCollection.this.modCount;
        }

        /**
         * This method resets the reusable iterator. This makes the iterator reusable.
         */
        @Override
        public void reset() {
            this.position = 0;
            this.expectedModCount = PacketCollection.this.modCount;
        }

        @Override
        public boolean hasNext() {
            return (this.position + 1 <= PacketCollection.this.size());
        }

        @Override
        public Packet next() {
            this.checkForComodification();
            return PacketCollection.this.elementAt(this.position++);
        }

        @Override
        public void remove() {
            PacketCollection.this.remove2(--this.position);
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
            if (this.expectedModCount != PacketCollection.this.modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

}
