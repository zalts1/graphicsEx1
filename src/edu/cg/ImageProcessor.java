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
			final float imgX = x * this.inWidth / (float)this.outWidth;
			final float imgY = y * this.inHeight / (float)this.outHeight;

			int currentX = (int)imgX;
			int nextX = Math.min(currentX + 1, this.inWidth - 1);

			int currentY = (int)imgY;
			int nextY = Math.min(currentY + 1, this.inHeight - 1);

			// this is what i tried according to the lecture summary that for some reason works worse
			// float t = (imgX - currentX) / (float)(nextX - currentX);
			// float s = Math.abs((imgY - nextY) / (float)(nextY - currentY));

			//this is the original
			float diffX = nextX - imgX;
			float diffY = nextY - imgY;

			Color c1 = linearX(this.workingImage, currentY, currentX, nextX, diffX);
			Color c2 = linearX(this.workingImage, nextY, currentX, nextX, diffX);
			Color c3 = weightedMean(c1, c2, diffY);
			ans.setRGB(x, y, c3.getRGB());
			return;
		});

		this.popForEachParameters();
		return ans;
	}

	private static Color linearX(final BufferedImage img, final int y, final int currentX, final int nextX, final float dx) {
		final Color c1 = new Color(img.getRGB(currentX, y));
		final Color c2 = new Color(img.getRGB(nextX, y));
		return weightedMean(c1, c2, dx);
	}

	private static Color weightedMean(final Color c1, final Color c2, final float delta) {
		final float r1 = (float)c1.getRed();
		final float g1 = (float)c1.getGreen();
		final float b1 = (float)c1.getBlue();
		final float r2 = (float)c2.getRed();
		final float g2 = (float)c2.getGreen();
		final float b2 = (float)c2.getBlue();
		final int r3 = weightedMean(r1, r2, delta);
		final int g3 = weightedMean(g1, g2, delta);
		final int b3 = weightedMean(b1, b2, delta);
		return new Color(r3, g3, b3);
	}

	private static int weightedMean(final float c1, final float c2, final float delta) {
		final int ans = (int)(c1 * delta + (1.0f - delta) * c2);
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
