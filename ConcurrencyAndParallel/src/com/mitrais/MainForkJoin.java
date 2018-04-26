package com.mitrais;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class MainForkJoin extends RecursiveAction {
    private int[] source;
    private int start;
    private int length;
    private int[] destination;
    private int blurWidth = 15; //should be odd

    private static int sThreshold = 10;

    private MainForkJoin(int[] src, int start, int length, int[] dst) {
        source = src;
        this.start = start;
        this.length = length;
        destination = dst;
    }

    public static void main(String[] args) {
        String srcName = "D:\\Project\\Java\\Bootcamp\\ConcurrencyAndParallel\\tiger.jpg";
        File srcFile = new File(srcName);

        try {
            BufferedImage image = ImageIO.read(srcFile);
            BufferedImage resultImage = BlurTheImage(image);

            String dstName = "D:\\Project\\Java\\Bootcamp\\ConcurrencyAndParallel\\blurred_tiger.jpg";
            File dstFile = new File(dstName);
            ImageIO.write(resultImage, "jpg", dstFile);
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    protected void compute() {
        if (length < sThreshold) {
            computeDirectly();
            return;
        }

        int split = length / 2;

        invokeAll(new MainForkJoin(source, start, split, destination),
                new MainForkJoin(source, start + split, length - split, destination));
    }

    private static BufferedImage BlurTheImage(BufferedImage srcImage) {
        int w = srcImage.getWidth();
        int h = srcImage.getHeight();

        int[] src = srcImage.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];

        int processors = Runtime.getRuntime().availableProcessors();
        System.out.println(processors + " processor" + (processors != 1 ? "s are " : " is ") + "available");

        MainForkJoin mfj = new MainForkJoin(src, 0, src.length, dst);
        ForkJoinPool fjp = new ForkJoinPool();

        long startTime = System.currentTimeMillis();
        fjp.invoke(mfj);
        long endTime = System.currentTimeMillis();

        System.out.println("Image blur took " + (endTime - startTime) + " milliseconds.");

        BufferedImage dstImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        dstImage.setRGB(0, 0, w, h, dst, 0, w);

        return dstImage;
    }

    private void computeDirectly() {
        int sidePixels = (blurWidth - 1) / 2;

        for (int pixelIdx = start; pixelIdx < start + length; pixelIdx++) {
            // Calculate average.
            float rt = 0, gt = 0, bt = 0;
            for (int mi = -sidePixels; mi <= sidePixels; mi++) {
                int mindex = Math.min(Math.max(mi + pixelIdx, 0), source.length - 1);
                int pixel = source[mindex];
                rt += (float) ((pixel & 0x00ff0000) >> 16) / blurWidth;
                gt += (float) ((pixel & 0x0000ff00) >> 8) / blurWidth;
                bt += (float) ((pixel & 0x000000ff) >> 0) / blurWidth;
            }

            // Re-assemble destination pixel.
            int newColor = (0xff000000)
                    | (((int) rt) << 16)
                    | (((int) gt) << 8)
                    | (((int) bt) << 0);

            destination[pixelIdx] = newColor;
        }
    }
}
