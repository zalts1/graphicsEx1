package edu.cg;

import java.awt.image.BufferedImage;


public class BasicSeamsCarver extends ImageProcessor {
	
	// An enum describing the carving scheme used by the seams carver.
	// VERTICAL_HORIZONTAL means vertical seams are removed first.
	// HORIZONTAL_VERTICAL means horizontal seams are removed first.
	// INTERMITTENT means seams are removed intermittently : vertical, horizontal, vertical, horizontal etc.
	public static enum CarvingScheme {
		VERTICAL_HORIZONTAL("Vertical seams first"),
		HORIZONTAL_VERTICAL("Horizontal seams first"),
		INTERMITTENT("Intermittent carving");
		
		public final String description;
		
		private CarvingScheme(String description) {
			this.description = description;
		}
	}
	
	// A simple coordinate class which assists the implementation.
	protected class Coordinate{
		public int X;
		public int Y;
		public Coordinate(int X, int Y) {
			this.X = X;
			this.Y = Y;
		}
	}
	
	// TODO :  Decide on the fields your BasicSeamsCarver should include. Refer to the recitation and homework 
	private Coordinate[][] coordinates;
	private BufferedImage grayScaledImage;
	private double[][] energyMatrix;
	private int[][] grayScaled;
	
	public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, int outHeight, RGBWeights rgbWeights) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
		coordinates = new Coordinate[inHeight][inWidth];

		forEach((y, x) -> coordinates[y][x] = new Coordinate(x, y));
		grayScaled = new int[inHeight][inWidth];
		setForEachParameters(inWidth, inHeight);
		grayScaledImage = greyscale();

		forEach((y, x) -> {
			coordinates[y][x] = new Coordinate(x,y);
		});

		forEach((y, x) -> {
			energyMatrix[y][x] = calcEnergyMatrix(x, y);
		});

	}

	private double calcEnergyMatrix(int x, int y) {
		double horizontalEnergy = 0;
		double verticalEnergy = 0;
		if(x == inWidth - 1){
			horizontalEnergy = Math.abs(grayScaled[y][x] - grayScaled[y][x - 1]);
		}else{
			horizontalEnergy = Math.abs(grayScaled[y][x] - grayScaled[y][x + 1]);
		}

		if(y == inHeight -1){
			verticalEnergy = Math.abs(grayScaled[y][x] - grayScaled[y - 1][x]);
		}else{
			Math.abs(grayScaled[y][x] - grayScaled[y + 1][x]);
		}

		double ans = Math.sqrt(Math.pow(horizontalEnergy, 2) + Math.pow(verticalEnergy, 2));
		return ans;
	}
	
	public BufferedImage carveImage(CarvingScheme carvingScheme) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		// TODO :  Perform Seam Carving. Overall you need to remove 'numberOfVerticalSeamsToCarve' vertical seams
				// and 'numberOfHorizontalSeamsToCarve' horizontal seams from the image.
				// Note you must consider the 'carvingScheme' parameter in your procedure.
				// Return the resulting image.
		throw new UnimplementedMethodException("carveImage");
	}
	
	public BufferedImage showSeams(boolean showVerticalSeams, int seamColorRGB) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		// TODO :  Present either vertical or horizontal seams on the input image.
				// If showVerticalSeams = true, carve 'numberOfVerticalSeamsToCarve' vertical seams from the image.
				// Then, generate a new image from the input image in which you mark all of the vertical seams that
				// were chosen in the Seam Carving process. 
				// This is done by painting each pixel in each seam with 'seamColorRGB' (overriding its' previous value). 
				// Similarly, if showVerticalSeams = false, carve 'numberOfHorizontalSeamsToCarve' horizontal seams
				// from the image.
				// Then, generate a new image from the input image in which you mark all of the horizontal seams that
				// were chosen in the Seam Carving process.

		throw new UnimplementedMethodException("showSeams");
	}
}
