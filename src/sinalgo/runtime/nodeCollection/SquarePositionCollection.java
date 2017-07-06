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


import java.util.Enumeration;

import sinalgo.tools.storage.ReusableEnumeration;

/**
 * This class stores the SquarePositions and provides the methods to enumerate them with a reusable 
 * enumeration.
 */
public class SquarePositionCollection {

	private SquarePositionCollectionEnumeration enumeration= null;
	/**
	 * The storage for the SquarePos. This storage is an array and always has the same size(9)
	 */
	private SquarePos[] squares = new SquarePos[9];
	private boolean[] used = new boolean[9];
	private int nextUnused = 0;
	
	/**
	 * The only constructor for the SquarePositionCollection. It fills the data array with empty 
	 * instances.
	 */
	public SquarePositionCollection(){
		for(int i = 0; i < 9; i++){
			squares[i] = new SquarePos(0, 0);
		}
	}
	
	/**
	 * This method adds a SquarePos to the collection. This means: it fills the next unused slot in the data
	 * array with the parameter values.
	 * 
	 * @param x The x parameter of the SquarePos to add.
	 * @param y The y parameter of the SquarePos to add.
	 */
	public void add(int x, int y){
		if(nextUnused == squares.length){
			throw new ArrayIndexOutOfBoundsException("You tried to add more than the possible elements to the SquarePositionCollection");
		}
		used[nextUnused] = true;
		squares[nextUnused].x = x;
		squares[nextUnused].y = y;
		nextUnused++;
	}
	
	/**
	 * This method clears the data array. This means that it sets all the used flags of the elements
	 * in the array to false.
	 */
	public void clear(){
		for(int i = 0; i < 9; i++){
			used[i] = false;
		}
		nextUnused = 0;
	}
	
	/**
	 * This method returns an enumeration over the elements in the data array. This enumeration is
	 * either a new instance or the resetted old one.
	 * 
	 * @return A reusable Enumeration over the collection.
	 */
	public Enumeration<SquarePos> elements(){
		if(enumeration == null){
			enumeration = new SquarePositionCollectionEnumeration();
		}
		else{
			enumeration.reset();
		}
		return enumeration;
	}
	
	private class SquarePositionCollectionEnumeration implements ReusableEnumeration<SquarePos>{

		private int position = 0;
		
		public void reset(){
			position = 0;
		}
		
		public boolean hasMoreElements() {
			if(position < squares.length){
				return (used[position]);
			}
			else{
				return false;
			}
		}

		public SquarePos nextElement() {
			return squares[position++];//increment the position inline
		}
		
	}
}
