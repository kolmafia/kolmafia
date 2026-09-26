package net.sourceforge.kolmafia.utilities;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.sourceforge.kolmafia.RequestLogger;

public class GraphicsUtilities {

  public static BufferedImage readImage(File f) {
    try {
      return ImageIO.read(f);
    } catch (IOException x) {
      return null;
    }
  }

  public static BufferedImage[] readImages(List<String> paths) {
    ArrayList<BufferedImage> images = new ArrayList<>();
    for (String path : paths) {
      File f = FileUtilities.downloadImage(path);
      BufferedImage image = readImage(f);
      if (image == null) {
        RequestLogger.printLine("Unable to load image file " + path);
        continue;
      }
      images.add(image);
    }
    return images.toArray(new BufferedImage[0]);
  }

  public static BufferedImage mergeImages(BufferedImage[] images) {
    if (images.length == 0) {
      return null;
    }

    // Assume images all have the same size.
    int width = images[0].getWidth();
    int height = images[0].getHeight();
    BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    // Draw each of the input images onto the output image.
    Graphics g = output.getGraphics();
    for (BufferedImage image : images) {
      g.drawImage(image, 0, 0, null);
    }

    return output;
  }

  public static void writeImage(BufferedImage image, String path) {
    if (image == null) {
      return;
    }

    try {
      File f = FileUtilities.imageFile(path);
      f.createNewFile();
      ImageIO.write(image, "PNG", f);
    } catch (IOException x) {
    }
  }
}
