package edu.cg;
//// stavital
// nadav zaltsman
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
			double imgX = x * (this.inWidth - 1) / (float)this.outWidth;
			double imgY = y * (this.inHeight - 1) / (float)this.outHeight;

			int currentX = (int)imgX;
			int nextX = Math.min(currentX, this.inWidth - 1);

			int currentY = (int)imgY;
			int nextY = Math.min(currentY, this.inHeight - 1);

			double diffX = nextX - imgX;
			double diffY = nextY - imgY;

			int c1 = workingImage.getRGB((int)imgX, (int)imgY);
			int c2 = workingImage.getRGB((int)imgX + 1, (int)imgY);
			int c3 = workingImage.getRGB((int)imgX, (int)imgY + 1);
			int c4 = workingImage.getRGB((int)imgX + 1 , (int)imgY + 1);

			int c12 = getColor(c1, c2, diffX);
			int c34 = getColor(c3, c4, diffX);
			int cFinal = getColor(c12, c34, diffY);
			ans.setRGB(x, y, cFinal);
			return;
		});

		this.popForEachParameters();
		return ans;
	}

	private int getColor(int c1, int c2, double diff) {
		int r1 = (c1>>16)&0xFF;
		int g1 = (c1>>8)&0xFF;
		int b1 = (c1>>0)&0xFF;
		int r2 = (c2>>16)&0xFF;
		int g2 = (c2>>8)&0xFF;
		int b2 = (c2>>0)&0xFF;

		int r12 = calc(r1, r2, diff);
		int g12 = calc(g1, g2, diff);
		int b12 = calc(b1, b2, diff);
		return (r12<<16 | g12<<8 | b12);
	}

	private int calc(int c1, int c2, double diff) {
		int ans = (int)(((1 - diff) * c1) + (diff * c2));
		return Math.min(Math.max(ans, 0), 255);
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
