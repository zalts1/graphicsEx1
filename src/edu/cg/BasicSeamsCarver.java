package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.util.ArrayList;


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
    private int[][] pathMatrix;
    private int currHeight;
    private int currWidth;
    private Mode currentMode;
    private ArrayList<Coordinate[]> verticalSeams;
    private ArrayList<Coordinate[]> horizontalSeams;
    private BufferedImage result;


    public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
                            int outWidth, int outHeight, RGBWeights rgbWeights) {
        super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
        currHeight = inHeight;
        currWidth = inWidth;

        if (inWidth < 2 | inHeight < 2)
            throw new RuntimeException("Can not apply seam carving: workingImage is too small");

        coordinates = new Coordinate[currHeight][currWidth];
        grayScaledMatrix = new int[currHeight][currWidth];
        pathMatrix = new int[currHeight][currWidth];
        energyMatrix = new double[currHeight][currWidth];
        verticalSeams = new ArrayList<>();
        horizontalSeams = new ArrayList<>();
        result = duplicateWorkingImage();

        setForEachInputParameters();
        BufferedImage grayScaledImage = greyscale();

        for(int y = 0; y < currHeight; y++) {
            for (int x = 0; x < currWidth; x++) {
                grayScaledMatrix[y][x] = (new Color(grayScaledImage.getRGB(x, y))).getRed();
            }
        }

        this.dynamicPTable = new double[currHeight][currWidth];

        for(int y = 0; y < currHeight; y++) {
            for (int x = 0; x < currWidth; x++) {
                coordinates[y][x] = new Coordinate(x, y);
            }
        }

    }

    private void carveSeam() {
        initDynamicPTableWithEnergyPixels();
        for(int y = 0; y < currHeight; y++) {
            for (int x = 0; x < currWidth; x++) {
                calcMinimalCostPixel(y, x);
            }
        }

    }

    private Coordinate[] findVerticalSeam() {
        Coordinate[] seamToRemove = new Coordinate[currHeight];
        Coordinate minCoor = new Coordinate(-1,-1);
        float minValue = Float.MAX_VALUE;

        for(int x = 0; x < currWidth; x++){
            if(dynamicPTable[currHeight-1][x] < minValue){
                minValue = (float) dynamicPTable[currHeight-1][x];
                minCoor.X = x;
                minCoor.Y = currHeight-1;
            }
        }

        seamToRemove[currHeight-1] = minCoor;
        int currCol = minCoor.X;

        for(int y = currHeight - 1; y > 0; y--){
            if(pathMatrix[y][currCol] == 0){
               seamToRemove[y-1] = new Coordinate(currCol, y-1);
            } else if( pathMatrix[y][currCol] == 1 && currCol < currWidth-1){
                seamToRemove[y-1] = new Coordinate(currCol+1, y-1);
                currCol++;
            }else if(pathMatrix[y][currCol] == -1 && currCol > 0){
                seamToRemove[y-1] = new Coordinate(currCol-1, y-1);
                currCol--;
            }
        }

        return seamToRemove;
    }

    private Coordinate[] findHorizontalSeam() {
        Coordinate[] seamToRemove = new Coordinate[currHeight];
        Coordinate minCoor = new Coordinate(-1,-1);
        float minValue = Float.MAX_VALUE;

        for(int y = 0; y < currHeight; y++){
            if(dynamicPTable[y][currWidth-1] < minValue){
                minValue = (float) dynamicPTable[y][currWidth-1];
                minCoor.X = currWidth-1;
                minCoor.Y = y;
            }
        }

        seamToRemove[currWidth-1] = minCoor;
        int currRow = minCoor.Y;

        for(int x = currWidth - 1; x > 0; x--){
            if(pathMatrix[currRow][x] == 0){
                seamToRemove[x-1] = new Coordinate(x-1, currRow);
            } else if( pathMatrix[currRow][x] == 1 && currRow < currHeight-1){
                seamToRemove[x-1] = new Coordinate(x-1, currRow+1);
                currRow++;
            }else if(pathMatrix[currRow][x] == -1 && currRow > 0){
                seamToRemove[x-1] = new Coordinate(x-1, currRow-1);
                currRow--;
            }
        }

        return seamToRemove;
    }

    private double[] costVerticalEdge(int y, int x){
        double cR, cL, cV;

        if (x == 0) {
            cL = 255;
            cV = 0;
            cR = Math.abs(grayScaledMatrix[y][x + 1] - grayScaledMatrix[y - 1][x]);
        } else if (x == currWidth - 1) {
            cL = Math.abs(this.grayScaledMatrix[y - 1][x] - this.grayScaledMatrix[y][x - 1]);
            cV = 0;
            cR = 255;
        } else {
            cL = Math.abs(this.grayScaledMatrix[y][x + 1] - this.grayScaledMatrix[y][x - 1])
                    + Math.abs(grayScaledMatrix[y - 1][x] - grayScaledMatrix[y][x - 1]);
            cV = Math.abs(this.grayScaledMatrix[y][x + 1] - this.grayScaledMatrix[y][x - 1]);
            cR = Math.abs(this.grayScaledMatrix[y][x + 1] - this.grayScaledMatrix[y][x - 1])
                    + Math.abs(grayScaledMatrix[y][x + 1] - grayScaledMatrix[y - 1][x]);
        }
        double[] ans = {cL,cV,cR};
        return ans;
    }

    private double[] costHorizontalEdge(int x, int y){
        double cU, cH, cD;

        if (y == 0) {
            cU = 255;
            cH = 0;
            cD = Math.abs(this.grayScaledMatrix[y + 1][x] - this.grayScaledMatrix[y][x - 1]);
        } else if (y == currHeight - 1) {
            cU = Math.abs(this.grayScaledMatrix[y - 1][x] - this.grayScaledMatrix[y][x - 1]);
            cH = 0;
            cD = 255;
        } else {
            cU = Math.abs(this.grayScaledMatrix[y - 1][x] - this.grayScaledMatrix[y][x - 1])
                    + Math.abs(grayScaledMatrix[y - 1][x] - grayScaledMatrix[y + 1][x]);
            cH = Math.abs(this.grayScaledMatrix[y - 1][x] - this.grayScaledMatrix[y + 1][x]);
            cD =  Math.abs(this.grayScaledMatrix[y + 1][x] - this.grayScaledMatrix[y][x - 1])
                    + Math.abs(grayScaledMatrix[y - 1][x] - grayScaledMatrix[y + 1][x]);
        }

        double[] ans =  {cU,cH,cD};
        return ans;

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
        double[] Cudh = costHorizontalEdge(x, y);
        double cu = Cudh[0];
        double ch = Cudh[1];
        double cd = Cudh[2];

        if (y == 0) {
            mu = 255;
            mh = ch + dynamicPTable[y][x - 1];
            md = cd + dynamicPTable[y + 1][x - 1];
        } else if (y == currHeight - 1) {
            md = 255;
            mh = ch + dynamicPTable[y][x - 1];
            mu = cu + dynamicPTable[y - 1][x - 1];
        } else {
            md = cd + dynamicPTable[y + 1][x - 1];
            mu = cu + dynamicPTable[y - 1][x - 1];
            mh = ch + dynamicPTable[y][x - 1];
        }

        double minValue = Math.min(mu, Math.min(md, mh));

        if(minValue == mh){
            pathMatrix[y][x] = 0;
        }

        if(minValue == md){
            pathMatrix[y][x] = 1;
        }

        if(minValue == mu){
            pathMatrix[y][x] = -1;
        }

        return minValue;
    }

    private void calcVerticalCost(int y, int x) {
        if (y == 0) {
            if (x == 0 || x == this.currWidth - 1) {
                return;
            } else {
                this.dynamicPTable[y][x] += Math.abs(grayScaledMatrix[0][x - 1] - grayScaledMatrix[0][x + 1]);
            }
        } else {
            this.dynamicPTable[y][x] += calcVerticalMinVerticalValue(y, x);
        }
    }

    private double calcVerticalMinVerticalValue(int y, int x) {
        if(y == currHeight-1){
            System.out.println("a");
        }

        double ml;
        double mv;
        double mr;
        double[] Cvlr = costVerticalEdge(y,x);
        double cL = Cvlr[0];
        double cV = Cvlr[1];
        double cR = Cvlr[2];

        if (x == 0) {
            ml = Integer.MAX_VALUE;
            mv = dynamicPTable[y - 1][x];
            mr = cR + dynamicPTable[y - 1][x + 1];
        } else if (x == currWidth - 1) {
            mr = Integer.MAX_VALUE;
            mv = dynamicPTable[y - 1][x];
            ml = cL + dynamicPTable[y - 1][x - 1];
        } else {
            mr = cR + dynamicPTable[y - 1][x + 1];
            ml = cL + dynamicPTable[y - 1][x - 1];
            mv = cV + dynamicPTable[y - 1][x];
        }

        double minValue = Math.min(mv, Math.min(ml, mr));

        if(minValue == mv){
            pathMatrix[y][x] = 0;
        }

        if(minValue == mr){
            pathMatrix[y][x] = 1;
        }

        if(minValue == ml){
            pathMatrix[y][x] = -1;
        }

        return minValue;
    }

    private void initDynamicPTableWithEnergyPixels() {
        calcEnergyMatrix();
        for(int y = 0; y < currHeight; y++){
            for(int x = 0; x < currWidth; x++){
                this.dynamicPTable[y][x] = energyMatrix[y][x];
            }
        }
    }

    private void calcEnergyMatrix() {
        for(int y = 0; y < currHeight; y++) {
            for (int x = 0; x < currWidth; x++) {
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

                energyMatrix[y][x] = Math.sqrt(Math.pow(horizontalEnergy, 2) + Math.pow(verticalEnergy, 2));
            }
        }

    }

    public BufferedImage carveImage(CarvingScheme carvingScheme) {
        int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
        int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
        BufferedImage ans = newEmptyOutputSizedImage();

        if (carvingScheme == CarvingScheme.VERTICAL_HORIZONTAL) {
            this.currentMode = Mode.VERTICAL;
            for (int i = 0; i < numberOfVerticalSeamsToCarve; i++) {
                removeSingleVerticalSeam();;
            }
            this.currentMode = Mode.HORIZONTAL;
            for (int i = 0; i < numberOfHorizontalSeamsToCarve; i++) {
                removeSingleHorizontalSeam();
            }
        } else if (carvingScheme == CarvingScheme.HORIZONTAL_VERTICAL) {
            this.currentMode = Mode.HORIZONTAL;
            for (int i = 0; i < numberOfHorizontalSeamsToCarve; i++) {
                removeSingleHorizontalSeam();
            }

            this.currentMode = Mode.VERTICAL;
            for (int i = 0; i < numberOfVerticalSeamsToCarve; i++) {
                removeSingleVerticalSeam();
            }
        } else {
            carveIntermittently();
        }
        setForEachWidth(outWidth);
        setForEachHeight(outHeight);

        for(int y = 0; y < currHeight; y++) {
            for (int x = 0; x < currWidth; x++) {
                ans.setRGB(x, y, workingImage.getRGB(coordinates[y][x].X, coordinates[y][x].Y));
            }
        }

        return ans;
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


        BufferedImage currentImage = newEmptyInputSizedImage();
        for(int y = 0; y < currHeight; y++) {
            for (int x = 0; x < currWidth; x++) {
                currentImage.setRGB(x, y, workingImage.getRGB(x, y));
            }
        }

        if(showVerticalSeams){
            for(int i=0; i< numberOfVerticalSeamsToCarve; i++){
                currentMode = Mode.VERTICAL;
                carveSeam();
                Coordinate[] ans = findVerticalSeam();
                verticalSeams.add(ans);
                compressIndexMatrixVertical(ans);
            }
        }else{
            for(int i=0; i<numberOfHorizontalSeamsToCarve; i++){
                carveSeam();
                Coordinate[] ans = findHorizontalSeam();
                horizontalSeams.add(ans);
                compressIndexMatrixVertical(ans);
            }
        }

        colorSeam(seamColorRGB, currentImage);
        return currentImage;

    }

    private void removeSingleVerticalSeam() {
        carveSeam();
        Coordinate[] ans = findVerticalSeam();
        compressIndexMatrixVertical(ans);
    }

    private void removeSingleHorizontalSeam() {
        carveSeam();
        Coordinate[] ans = findVerticalSeam();
        compressIndexMatrixHorizontal(ans);
    }

    private void compressIndexMatrixHorizontal(Coordinate[] seam) {
        for (Coordinate coordinate : seam) {
            int row = coordinate.Y;
            int col = coordinate.X;

            for (int j = row; j < currHeight - 1; j++) {
                coordinates[j][col] = coordinates[j + 1][col];
            }
        }

        currHeight--;
    }


    private void compressIndexMatrixVertical(Coordinate[] seamToRemove) {
        for (Coordinate coordinate : seamToRemove) {
            int row = coordinate.Y;
            int col = coordinate.X;

            for (int j = col; j < currWidth - 1; j++) {
                coordinates[row][j] = coordinates[row][j + 1];
            }
        }

        currWidth--;
    }

    private void colorSeam(int seamColor, BufferedImage Image){
        if(currentMode == Mode.VERTICAL){
            for(Coordinate[] seam : verticalSeams) {
                for (Coordinate coor : seam) {
                    Image.setRGB(coordinates[coor.Y][coor.X].X, coordinates[coor.Y][coor.X].Y, seamColor);
                }
            }
        }else{
            for(Coordinate[] seam : horizontalSeams) {
                for (Coordinate coor : seam) {
                    Image.setRGB(coordinates[coor.Y][coor.X].X, coordinates[coor.Y][coor.X].Y, seamColor);
                }
            }
        }

    }
}
