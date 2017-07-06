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
package sinalgo.tools;

/**
 * A tuple-class which can hold two objects in a type-safe manner.
 * 
 * @param <A> The type of the first entry of the tuple instance.
 * @param <B> The type of the second entry of the tuple instance.
 */
public class Tuple<A,B> {

	/**
	 * The first value of this tuple. 
	 */
	public A first;
	
	/**
	 * The second value of this tuple
	 */
	public B second; 

	/**
	 * Constructs a new tuple and initializes the two fields.
	 * @param a The value for the first entry
	 * @param b The value for the second entry
	 */
	public Tuple(A a, B b) {
		first = a;
		second = b;
	}
	
	/**
	 * Default constructor for this class. 
	 */
	public Tuple() {
	}
	
	/**
	 * Two tuples are equal if the two pairs of objects
	 * stored in the tuplets are equal.
	 *  (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if(o == null || !(o instanceof Tuple)) {
			return false;
		}
		Tuple<?,?> t = (Tuple<?,?>) o;
		if(first == null && second == null) {
			return t.first == null && t.second == null;
		}
		if(first == null) {
			return t.first == null && second.equals(t.second);
		}
		if(second == null) {
			return t.second == null && first.equals(t.first);
		}
		return first.equals(t.first) && second.equals(t.second);
	}
}
