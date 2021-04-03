package edu.cg;

import java.awt.*;
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
    protected class Coordinate {
        public int X;
        public int Y;

        public Coordinate(int X, int Y) {
            this.X = X;
            this.Y = Y;
        }
    }

    public static enum Mode {
        HORIZONTAL("horizontal"),
        VERTICAL("vertical");

        public final String mode;

        private Mode(String mode) {
            this.mode = mode;
        }
    }

    // TODO :  Decide on the fields your BasicSeamsCarver should include. Refer to the recitation and homework 
    private Coordinate[][] coordinates;
    private double[][] dynamicPTable;
    private double[][] energyMatrix;
    private int[][] grayScaledMatrix;
    private int currHeight;
    private int currWidth;
    private Mode currentMode;


    public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
                            int outWidth, int outHeight, RGBWeights rgbWeights) {
        super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
        currHeight = inHeight;
        currWidth = inWidth;
        coordinates = new Coordinate[currHeight][currWidth];

        grayScaledMatrix = new int[currHeight][currWidth];
        setForEachParameters(currWidth, currHeight);
        BufferedImage grayScaledImage = greyscale();

        forEach((y, x) -> {
            coordinates[y][x] = new Coordinate(x, y);
            grayScaledMatrix[y][x] = (new Color(grayScaledImage.getRGB(x, y))).getRed();
        });
    }

    private void carveSeam() {
        initDynamicPTableWithEnergyPixels();
        forEach((y, x) -> {
            calcMinimalCostPixel(y, x);
        });
        // TODO: add function for finding minimal seam
        // TODO: remove minimal seam
    }

    private void calcMinimalCostPixel(int y, int x) {
        if (currentMode == Mode.VERTICAL) {
            calcVerticalCost(y, x);
        } else {
            calcHorizontalCost(y, x);
        }
    }

    private void calcHorizontalCost(int y, int x) {
        if (x == 0) {
            if (y == 0 || y == this.currHeight - 1) {
                return;
            } else {
                this.dynamicPTable[y][x] += Math.abs(grayScaledMatrix[y - 1][x] - grayScaledMatrix[y + 1][x]);
            }
        } else {
            this.dynamicPTable[y][x] += calcHorizontalMinHorizontalValue(y, x);
        }
    }

    private double calcHorizontalMinHorizontalValue(int y, int x) {
        double mu;
        double mh;
        double md;

        if (y == 0) {
            mu = 255;
            mh = dynamicPTable[y][x - 1];
            md = Math.abs(this.grayScaledMatrix[y + 1][x] - this.grayScaledMatrix[y][x - 1]) + dynamicPTable[y + 1][x - 1];
        } else if (y == currHeight - 1) {
            md = 255;
            mh = dynamicPTable[y][x - 1];
            mu = Math.abs(this.grayScaledMatrix[y - 1][x] - this.grayScaledMatrix[y][x - 1]) + dynamicPTable[y - 1][x - 1];
        } else {
            md = Math.abs(this.grayScaledMatrix[y + 1][x] - this.grayScaledMatrix[y][x - 1])
                    + Math.abs(grayScaledMatrix[y - 1][x] - grayScaledMatrix[y + 1][x])
                    + dynamicPTable[y + 1][x - 1];
            mu = Math.abs(this.grayScaledMatrix[y - 1][x] - this.grayScaledMatrix[y][x - 1])
                    + Math.abs(grayScaledMatrix[y - 1][x] - grayScaledMatrix[y + 1][x])
                    + dynamicPTable[y - 1][x - 1];
            mh = Math.abs(this.grayScaledMatrix[y - 1][x] - this.grayScaledMatrix[y + 1][x]) + dynamicPTable[y][x - 1];
        }

        return Math.min(mh, Math.min(mu, md));
    }

    private void calcVerticalCost(int y, int x) {
        if (y == 0) {
            if (x == 0 || x == this.currWidth - 1) {
                return;
            } else {
                this.dynamicPTable[y][x] += Math.abs(grayScaledMatrix[y][x - 1] - grayScaledMatrix[y][x + 1]);
            }
        } else {
            this.dynamicPTable[y][x] += calcVerticalMinVerticalValue(y, x);
        }
    }

    private double calcVerticalMinVerticalValue(int y, int x) {
        double ml;
        double mv;
        double mr;

        if (x == 0) {
            ml = 255;
            mv = dynamicPTable[y - 1][x];
            mr = Math.abs(this.grayScaledMatrix[y][x + 1] - this.grayScaledMatrix[y - 1][x]) + dynamicPTable[y - 1][x + 1];
        } else if (x == currWidth - 1) {
            mr = 255;
            mv = dynamicPTable[y - 1][x];
            ml = Math.abs(this.grayScaledMatrix[y - 1][x] - this.grayScaledMatrix[y][x - 1]) + dynamicPTable[y - 1][x - 1];
        } else {
            mr = Math.abs(this.grayScaledMatrix[y][x + 1] - this.grayScaledMatrix[y][x - 1])
                    + Math.abs(grayScaledMatrix[y][x + 1] - grayScaledMatrix[y - 1][x])
                    + dynamicPTable[y - 1][x + 1];
            ml = Math.abs(this.grayScaledMatrix[y][x + 1] - this.grayScaledMatrix[y][x - 1])
                    + Math.abs(grayScaledMatrix[y - 1][x] - grayScaledMatrix[y][x - 1])
                    + dynamicPTable[y - 1][x - 1];
            mv = Math.abs(this.grayScaledMatrix[y][x + 1] - this.grayScaledMatrix[y][x - 1]) + dynamicPTable[y - 1][x];
        }

        return Math.min(mv, Math.min(ml, mr));
    }

    private void initDynamicPTableWithEnergyPixels() {
        forEach((y, x) -> {
            this.dynamicPTable[y][x] = calcEnergyPixel(x, y);
        });
    }

    private double calcEnergyPixel(int x, int y) {
        double horizontalEnergy = 0;
        double verticalEnergy = 0;
        if (x == currWidth - 1) {
            horizontalEnergy = Math.abs(grayScaledMatrix[y][x] - grayScaledMatrix[y][x - 1]);
        } else {
            horizontalEnergy = Math.abs(grayScaledMatrix[y][x] - grayScaledMatrix[y][x + 1]);
        }

        if (y == currHeight - 1) {
            verticalEnergy = Math.abs(grayScaledMatrix[y][x] - grayScaledMatrix[y - 1][x]);
        } else {
            verticalEnergy = Math.abs(grayScaledMatrix[y][x] - grayScaledMatrix[y + 1][x]);
        }

        double ans = Math.sqrt(Math.pow(horizontalEnergy, 2) + Math.pow(verticalEnergy, 2));
        return ans;
    }

    public BufferedImage carveImage(CarvingScheme carvingScheme) {
        int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
        int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);

        if (carvingScheme == CarvingScheme.VERTICAL_HORIZONTAL) {
            this.currentMode = Mode.VERTICAL;
            for (int i = 0; i < numberOfVerticalSeamsToCarve; i++) {
                carveSeam();
            }
            this.currentMode = Mode.HORIZONTAL;
            for (int i = 0; i < numberOfHorizontalSeamsToCarve; i++) {
                carveSeam();
            }
        } else if (carvingScheme == CarvingScheme.HORIZONTAL_VERTICAL) {
            this.currentMode = Mode.HORIZONTAL;
            for (int i = 0; i < numberOfHorizontalSeamsToCarve; i++) {
                carveSeam();
            }

            this.currentMode = Mode.VERTICAL;
            for (int i = 0; i < numberOfVerticalSeamsToCarve; i++) {
                carveSeam();
            }
        } else {
            carveIntermittently();
        }
        throw new UnimplementedMethodException("carveImage");
    }

    private void carveIntermittently() {
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
