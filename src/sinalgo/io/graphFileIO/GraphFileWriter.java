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
package sinalgo.io.graphFileIO;

/**
 * This is a concrete Implementation of the GraphFileWriterInterface. It writed the current graph
 * to a File specified by the parameters of the Consturctor.
 */
public class GraphFileWriter{
//	private File file;
//
//	/**
//	 * The one and only constructor for the GraphFileWriter class.
//	 * 
//	 * @param file The file where to save the informations about the current graph.
//	 */
//	public GraphFileWriter(File file)
//	{
//		this.file = file;
//	}
//	
//	/**
//	 * Writes the information about the current graph in the file selected in the
//	 * constructor. Uses the serialize method of the nodes.
//	 */
//	public void write(){
//		
//		FileOutputStream fOut;
//		
//		try {
//			
//			if(file.exists()){
//				file.delete();
//			}
//			
//			file.createNewFile();
//			
//			fOut = new FileOutputStream(file);
//			
//			//write the dimension on top of the file
//			fOut.write(("DIMENSION="+Configuration.dimX+","+Configuration.dimY+";").getBytes());
//			fOut.write(("EDGETYPE="+Configuration.getEdgeType()+"\n").getBytes());
//			
//			Enumeration<Node> nodeEnumer = Runtime.nodes.getNodeEnumeration();
//			
//			while(nodeEnumer.hasMoreElements()){
//				Node node = nodeEnumer.nextElement();
//				fOut.write((node.serialize()+"\n").toString().getBytes());
//			}
//			
//			fOut.flush();
//			fOut.close();
//			
//		} catch (IOException e) {
//			Global.log.logln(LogL.ERROR_DETAIL, "Error saving the graph.");
//		}
//	}
}
