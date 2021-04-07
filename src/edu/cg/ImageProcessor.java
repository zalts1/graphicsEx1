package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageProcessor extends FunctioalForEachLoops {
	
	//MARK: Fields
	public final Logger logger;
	public final BufferedImage workingImage;
	public final RGBWeights rgbWeights;
	public final int inWidth;
	public final int inHeight;
	public final int workingImageType;
	public final int outWidth;
	public final int outHeight;
	
	//MARK: Constructors
	public ImageProcessor(Logger logger, BufferedImage workingImage,
			RGBWeights rgbWeights, int outWidth, int outHeight) {
		super(); //Initializing for each loops...
		
		this.logger = logger;
		this.workingImage = workingImage;
		this.rgbWeights = rgbWeights;
		inWidth = workingImage.getWidth();
		inHeight = workingImage.getHeight();
		workingImageType = workingImage.getType();
		this.outWidth = outWidth;
		this.outHeight = outHeight;
		setForEachInputParameters();
	}
	
	public ImageProcessor(Logger logger,
			BufferedImage workingImage,
			RGBWeights rgbWeights) {
		this(logger, workingImage, rgbWeights,
				workingImage.getWidth(), workingImage.getHeight());
	}
	
	//MARK: Change picture hue - example
	public BufferedImage changeHue() {
		logger.log("Prepareing for hue changing...");
		
		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int max = rgbWeights.maxWeight;
		
		BufferedImage ans = newEmptyInputSizedImage();
		
		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r*c.getRed() / max;
			int green = g*c.getGreen() / max;
			int blue = b*c.getBlue() / max;
			Color color = new Color(red, green, blue);
			ans.setRGB(x, y, color.getRGB());
		});
		
		logger.log("Changing hue done!");
		
		return ans;
	}
	
	//MARK: Nearest neighbor - example
	public BufferedImage nearestNeighbor() {
		logger.log("applies nearest neighbor interpolation.");
		BufferedImage ans = newEmptyOutputSizedImage();
		
		pushForEachParameters();
		setForEachOutputParameters();
		
		forEach((y, x) -> {
			int imgX = (int)Math.round((x*inWidth) / ((float)outWidth));
			int imgY = (int)Math.round((y*inHeight) / ((float)outHeight));
			imgX = Math.min(imgX,  inWidth-1);
			imgY = Math.min(imgY, inHeight-1);
			ans.setRGB(x, y, workingImage.getRGB(imgX, imgY));
		});
		
		popForEachParameters();
		
		return ans;
	}
	
	//MARK: Unimplemented methods
	public BufferedImage greyscale() {
		logger.log("Preparing for grey scale...");

		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int weightsSum = rgbWeights.weightsSum;

		BufferedImage ans = newEmptyInputSizedImage();

		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int greyscale = ((r * c.getRed()) + ( g * c.getGreen()) + (b * c.getBlue())) / weightsSum;
			Color color = new Color(greyscale, greyscale, greyscale);
			ans.setRGB(x, y, color.getRGB());
		});

		logger.log("grey scaling done!");

		return ans;
	}

	public BufferedImage gradientMagnitude() {
		this.logger.log("calculates the gradient magnitude.");
		if (this.inHeight < 2 || this.inWidth < 2) {
			throw new RuntimeException("Image is too small for calculating gradient magnitude.");
		}

		BufferedImage greyScaled = this.greyscale();
		BufferedImage ans = this.newEmptyInputSizedImage();
		this.forEach((y, x) -> {
			final int rgb = computeMagnitude(greyScaled, x, y);
			ans.setRGB(x, y, rgb);
			return;
		});

		logger.log("gradient magnitude done!");

		return ans;
	}


	private static int computeMagnitude(BufferedImage greyScaled, int currentX, int currentY) {
		int prevPixelX = -1;
		int prevPixelY = -1;

		if(currentX == 0){
			prevPixelX = 0;
		}

		if(currentY == 0){
			prevPixelY = 0;
		}

		int prevX = currentX + prevPixelX;
		int currentXColor = new Color(greyScaled.getRGB(currentX, currentY)).getBlue();
		int prevXcolor = new Color(greyScaled.getRGB(prevX, currentY)).getBlue();
		int diffColorX = currentXColor - prevXcolor;

		int prevY = currentY + prevPixelY;
		int currentYColor = new Color(greyScaled.getRGB(currentX, currentY)).getBlue();
		int prevYcolor = new Color(greyScaled.getRGB(currentX, prevY)).getBlue();
		int diffColorY = currentYColor - prevYcolor;

		int magnitude = Math.min((int)Math.sqrt(Math.pow(diffColorX, 2) + Math.pow(diffColorY, 2)), 255);
		return new Color(magnitude, magnitude, magnitude).getRGB();
	}

	public BufferedImage bilinear() {
		this.logger.log("applies bilinear interpolation.");

		BufferedImage ans = this.newEmptyOutputSizedImage();
		this.pushForEachParameters();
		this.setForEachOutputParameters();

		this.forEach((y, x) -> {
			float imgX = x * (this.inWidth) / (float)this.outWidth;
			float imgY = y * (this.inHeight) / (float)this.outHeight;

			int currentX = (int)imgX;
			int nextX = Math.min(currentX + 1, this.inWidth - 2);

			int currentY = (int)imgY;
			int nextY = Math.min(currentY + 1, this.inHeight - 2);

			float diffX = (float)nextX - imgX;
			float diffY = (float)nextY - imgY;

			Color c1 = new Color(workingImage.getRGB(currentX, currentY));
			Color c2 = new Color(workingImage.getRGB(nextX, currentY));
			Color c3 = new Color(workingImage.getRGB(currentX, nextY));
			Color c4 = new Color(workingImage.getRGB(nextX , nextY));

			Color c12 = linearInterpolation(c1, c2, diffX);
			Color c34 = linearInterpolation(c3, c4, diffX);
			Color cFinal = linearInterpolation(c12, c34, diffY);
			ans.setRGB(x, y, cFinal.getRGB());
			return;
		});

		this.popForEachParameters();
		return ans;
	}

	private Color linearInterpolation(Color c1, Color c2, float diff) {
		int r12 = (int)((1.0f - diff) * (float) c2.getRed() + diff * (float)c1.getRed());
		int g12 = (int)((1.0f - diff) * (float)c2.getGreen() + diff * (float)c1.getGreen());
		int b12 = (int)((1.0f - diff) * (float)c2.getBlue() + diff * (float)c1.getBlue());

		r12 = Math.min(Math.max(r12, 0), 255);
		g12 = Math.min(Math.max(g12, 0), 255);
		b12 = Math.min(Math.max(b12, 0), 255);
		return new Color(r12, g12, b12);
	}

	//MARK: Utilities
	public final void setForEachInputParameters() {
		setForEachParameters(inWidth, inHeight);
	}
	
	public final void setForEachOutputParameters() {
		setForEachParameters(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyInputSizedImage() {
		return newEmptyImage(inWidth, inHeight);
	}
	
	public final BufferedImage newEmptyOutputSizedImage() {
		return newEmptyImage(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyImage(int width, int height) {
		return new BufferedImage(width, height, workingImageType);
	}
	
	public final BufferedImage duplicateWorkingImage() {
		BufferedImage output = newEmptyInputSizedImage();
		
		forEach((y, x) -> 
			output.setRGB(x, y, workingImage.getRGB(x, y))
		);
		
		return output;
	}
}
