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
package sinalgo.tools.storage;

import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

/**
 * An extension of the java.util.Vector implementation that allows to sort the
 * content of the vector efficiently.
 *
 * @param <T>
 */
@NoArgsConstructor
public class SortableVector<T> extends Vector<T> {

    private static final long serialVersionUID = 2685289788493437402L;

    /**
     * Constructor specifing the initial size of the data-array
     *
     * @param size Initial size of the array
     */
    public SortableVector(int size) {
        super(size);
    }

    /**
     * Sorts the contents of this vector. The elements contained in the vector may
     * not be null, and need to implement the Comparable interface.
     * <p>
     * Note: T needs to extend Comparable<T>
     */
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public void sort() {
        Arrays.sort(super.elementData, 0, this.size());
    }

    /**
     * Sorts the contents of this vector. The elements contained in the vector may
     * not be null.
     *
     * @param c The comparator to compare any two elements in the vector.
     */
    @Override
    @SuppressWarnings({"unchecked", "AccessingNonPublicFieldOfAnotherObject"})
    public void sort(Comparator<? super T> c) {
        Arrays.sort((T[]) super.elementData, 0, this.size(), c);
    }
}
