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
package sinalgo.io.mapIO;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import sinalgo.configuration.Configuration;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.io.eps.EPSOutputPrintStream;
import sinalgo.nodes.Position;

/**
 * The optional background that may be drawn onto the deployment
 * field is specified by this Map class, which initializes
 * from a bitmap file.
 * <p>
 * The bitmap file may have any dimension, the framework scales 
 * to fit exactly the deployment area.  
 */
public class Map {
	private int[][] grid = null;
	private ColorModel colorModel = ColorModel.getRGBdefault(); // Color model to undertand RGB
	private int imgWidth, imgHeight; // width / height of this BG image 
	private double xRatio, yRatio; 

	/**
	 * @param aMapImageFile The name of the BMP file containing the background image.
	 * The image is expected to be in the projects root folder.
	 */
	public Map(String aMapImageFile) throws FileNotFoundException {
		// Read the image (preferably a bmp file)
		InputStream in = null;
		try {
			in = new FileInputStream(aMapImageFile);
			BufferedImage img = null;
			if ((img = ImageIO.read(in)) == null) {
				throw new FileNotFoundException("\n'" + aMapImageFile + "' - This image format is not supported.");
			}
			imgWidth = img.getWidth();
			imgHeight= img.getHeight();
			grid = new int[imgWidth][imgHeight];
			// copy the image data
			for(int i=0; i<imgWidth; i++) {
				for(int j=0; j<imgHeight; j++) {
					grid[i][j] = img.getRGB(i, j);
				}
			}
			in.close();
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException("Background map: Cannot open the image file.\n" + e.getMessage());
		} catch (IOException e) {
			throw new FileNotFoundException("Background map: Cannot open the image file\n" + e.getMessage());
		} 
		xRatio = ((double)imgWidth) / Configuration.dimX;
		yRatio = ((double)imgHeight) / Configuration.dimY;
	}
	
	/**
	 * Returns the color of a given position on the deployment area. The 
	 * position is automatically truncated to lie in the simulation plane. 
	 * @param x X-offset of the position
	 * @param y Y-offset of the position
	 * @return The color of the specified position on the deployment area. 
	 */
	private int getColorRGB(double x, double y) {
		int imgx = (int) Math.floor(xRatio * x);
		int imgy = (int) Math.floor(yRatio * y);
		if(imgx < 0) {
			imgx = 0;
		}
		if(imgx >= imgWidth) {
			imgx = imgWidth - 1;
		}
		if(imgy < 0) {
			imgy = 0;
		}
		if(imgy>= imgHeight) {
			imgy = imgHeight - 1;
		}
		return grid[imgx][imgy];
	}
	
	/**
	 * @param x X-offset of the position
	 * @param y Y-offset of the position
	 * @return The color of the specified position on the deployment area. 
	 */
	public Color getColor(double x, double y) {
		return new Color(getColorRGB(x, y));
	}

	public Color getColor(Position p) {
		return getColor(p.xCoord, p.yCoord);
	}
	
	/**
	 * @param x X-offset of the position
	 * @param y Y-offset of the position
	 * @return Whether the specified position on the deployment area is white. 
	 */
	public boolean isWhite(double x, double y) {
		int color = getColorRGB(x,y);
		int r = colorModel.getRed(color); // translate to default RGB values
		int g = colorModel.getGreen(color); // translate to default RGB values
		int b = colorModel.getBlue(color); // translate to default RGB values
		return r + g + b == 765; // r,g,b == 255
	}

	public boolean isWhite(Position p) {
		return isWhite(p.xCoord, p.yCoord);
	}
	
	/**
	 * @param x X-offset of the position
	 * @param y Y-offset of the position
	 * @return Whether the specified position on the deployment area is black. 
	 */
	public boolean isBlack(double x, double y) {
		int color = getColorRGB(x,y);
		int r = colorModel.getRed(color); // translate to default RGB values
		int g = colorModel.getGreen(color); // translate to default RGB values
		int b = colorModel.getBlue(color); // translate to default RGB values
		return r + g + b == 0; // r,g,b == 0
	}

	/**
	 * @param x X-offset of the position
	 * @param y Y-offset of the position
	 * @param c The color to test
	 * @return Whether the specified position on the deployment as the specified color. 
	 */
	public boolean isColor(double x, double y, Color c) {
		int color = getColorRGB(x,y);
		return c.getRed() == colorModel.getRed(color)
		&& c.getBlue() == colorModel.getBlue(color)
		&& c.getGreen() == colorModel.getGreen(color);
	}

	/**
	 * @param x X-offset of the position
	 * @param y Y-offset of the position
	 * @return The red color component for the specified position on the depolyment area.
	 */
	public int getRed(double x, double y) {
		int color = getColorRGB(x, y);
		return colorModel.getRed(color); // translate to default RGB values
	}
	
	/**
	 * @param x X-offset of the position
	 * @param y Y-offset of the position
	 * @return The blue color component for the specified position on the depolyment area.
	 */
	public int getBlue(double x, double y) {
		int color = getColorRGB(x, y);
		return colorModel.getBlue(color); // translate to default RGB values
	}

	/**
	 * @param x X-offset of the position
	 * @param y Y-offset of the position
	 * @return The green color component for the specified position on the depolyment area.
	 */
	public int getGreen(double x, double y) {
		int color = getColorRGB(x, y);
		return colorModel.getGreen(color); // translate to default RGB values
	}

	/**
	 * paints the Map into the specified Graphics object using the specified Transformation.
	 * 
	 * @param g The graphics object to paint the map to.
	 * @param pt The transformation object specifying the current transformation.
	 */
	public void paintMap(Graphics g, PositionTransformation pt) {
		if(pt.getNumberOfDimensions() != 2) {
			sinalgo.runtime.Main.fatalError("Background maps are not supported in 3D.\n" +
			                     "Do not specify a " +	"map while running a simulation in 3D.");
		}
		double lengthX = 1 / xRatio;
		double lengthY = 1 / yRatio;
		
		for(int i=0; i<imgWidth; i++) {
			for(int j=0; j<imgHeight; j++) {
				pt.translateToGUIPosition(i * lengthX, j*lengthY, 0); // top left corner of cell
				int topLeftX = pt.guiX, topLeftY = pt.guiY;
				pt.translateToGUIPosition((i+1) * lengthX, (j+1)*lengthY, 0); // bottom right corner of cell
				Color col = new Color(grid[i][j]);
				g.setColor(col);
				g.fillRect(topLeftX, topLeftY, pt.guiX - topLeftX, pt.guiY - topLeftY);
			}
		}
	}
	
	public void drawToPostScript(EPSOutputPrintStream pw, PositionTransformation pt) {
		double lengthX = 1 / xRatio;
		double lengthY = 1 / yRatio;
		
		for(int i=0; i<imgWidth; i++) {
			for(int j=0; j<imgHeight; j++) {
				Color col = new Color(grid[i][j]);
				if(col == Color.WHITE) {
					continue; // don't paint white
				}
				pt.translateToGUIPosition(i * lengthX, j*lengthY, 0); // top left corner of cell
				double topLeftX = pt.guiXDouble, topLeftY = pt.guiYDouble;
				pt.translateToGUIPosition((i+1) * lengthX, (j+1)*lengthY, 0); // bottom right corner of cell

				pw.setColor(col.getRed(), col.getGreen(), col.getBlue());
				pw.drawFilledRectangle(topLeftX, topLeftY, pt.guiXDouble - topLeftX, pt.guiYDouble - topLeftY);
			}
		}
	}
	
	
}