package com.quran.labs.androidquran.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import timber.log.Timber;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class QuranPartialPageChecker {

    public QuranPartialPageChecker() {
        // Empty constructor
    }

    /**
     * Checks all the pages to find partially downloaded images.
     */
    public List<Integer> checkPages(String directory, int numberOfPages, String width) {
        try {
            // check the partial images for the width
            return checkPartialImages(directory, width, numberOfPages);
        } catch (Throwable throwable) {
            Timber.e(throwable, "Error while checking partial pages: %s", width);
        }
        return new ArrayList<>();
    }

    /**
     * Check for partial images and return them.
     * This opens every downloaded image and looks at the last set of pixels.
     * If the last few rows are blank, the image is assumed to be partial and
     * the image is returned.
     */
    private List<Integer> checkPartialImages(String directoryName, String width, int numberOfPages) {
        List<Integer> result = new ArrayList<>();

        // Scale images down to 1/16th of size
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 16;

        File directory = new File(directoryName);
        // Optimization to avoid re-generating the pixel array every time
        int[] pixelArray = null;

        // Skip pages 1 and 2 since they're "special" (not full pages)
        for (int page = 3; page <= numberOfPages; page++) {
            String filename = QuranFileUtils.getPageFileName(page);
            File file = new File(directory, filename);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(directoryName + File.separator + filename, options);

                // This is an optimization to avoid allocating 8 * width of memory for everything.
                int rowsToCheck =
                        // madani, 9 for 1920, 7 for 1280, 5 for 1260 and 1024, and
                        //   less for smaller images.
                        // for naskh, 1 for everything
                        // for qaloon, 2 for the largest size, 1 for the smallest
                        // for warsh, 2 for everything
                        switch (width) {
                            case "_1024":
                            case "_1260":
                                return 5;
                            case "_1280":
                                return 7;
                            case "_1920":
                                return 9;
                            default:
                                return 4;
                        };

                int bitmapWidth = bitmap.getWidth();
                // These should all be the same size, so we can just allocate once
                int[] pixels = (pixelArray != null && pixelArray.length == (bitmapWidth * rowsToCheck))
                        ? pixelArray
                        : (pixelArray = new int[bitmapWidth * rowsToCheck]);

                // Get the set of pixels
                bitmap.getPixels(pixels, 0, bitmapWidth, 0, bitmap.getHeight() - rowsToCheck, bitmapWidth, rowsToCheck);

                // See if there's any non-0 pixel
                boolean foundPixel = false;
                for (int pixel : pixels) {
                    if (pixel != 0) {
                        foundPixel = true;
                        break;
                    }
                }

                // If all are non-zero, assume the image is partially blank
                if (!foundPixel) {
                    result.add(page);
                }
            }
        }
        return result;
    }
}
