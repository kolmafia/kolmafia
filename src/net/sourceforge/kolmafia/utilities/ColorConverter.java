package net.sourceforge.kolmafia.utilities;

/** A utility class for converting colors between RGB and HSL color spaces. */
public class ColorConverter {

  /**
   * Converts an RGB color value to HSL. Conversion formula adapted from
   * http://en.wikipedia.org/wiki/HSL_color_space. Assumes r, g, and b are contained in the set [0,
   * 255] and returns h, s, and l in the set [0, 1].
   *
   * @param r The red color component
   * @param g The green color component
   * @param b The blue color component
   * @return An array of three floats representing the HSL values: {hue, saturation, lightness}.
   */
  public static float[] rgbToHsl(int r, int g, int b) {
    float red = r / 255f;
    float green = g / 255f;
    float blue = b / 255f;

    float max = Math.max(red, Math.max(green, blue));
    float min = Math.min(red, Math.min(green, blue));

    float h = 0, s = 0, l = (max + min) / 2;

    if (max == min) {
      // achromatic (grey)
      h = s = 0;
    } else {
      float d = max - min;
      s = l > 0.5f ? d / (2f - max - min) : d / (max + min);

      if (max == red) {
        h = (green - blue) / d + (green < blue ? 6f : 0);
      } else if (max == green) {
        h = (blue - red) / d + 2f;
      } else if (max == blue) {
        h = (red - green) / d + 4f;
      }
      h /= 6f;
    }

    return new float[] {h, s, l};
  }

  /**
   * Converts an HSL color value to RGB. Conversion formula adapted from
   * http://en.wikipedia.org/wiki/HSL_color_space. Assumes h, s, and l are contained in the set [0,
   * 1] and returns r, g, and b in the set [0, 255].
   *
   * @param h The hue
   * @param s The saturation
   * @param l The lightness
   * @return An array of three integers representing the RGB values: {red, green, blue}.
   */
  public static int[] hslToRgb(float h, float s, float l) {
    float r, g, b;

    if (s == 0) {
      // achromatic (grey)
      r = g = b = l;
    } else {
      float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
      float p = 2 * l - q;
      r = hueToRgb(p, q, h + 1f / 3f);
      g = hueToRgb(p, q, h);
      b = hueToRgb(p, q, h - 1f / 3f);
    }

    return new int[] {Math.round(r * 255), Math.round(g * 255), Math.round(b * 255)};
  }

  /**
   * Helper method that converts hue to an RGB component.
   *
   * @param p
   * @param q
   * @param t
   * @return RGB component value
   */
  private static float hueToRgb(float p, float q, float t) {
    if (t < 0) {
      t += 1;
    }
    if (t > 1) {
      t -= 1;
    }
    if (t < 1f / 6f) {
      return p + (q - p) * 6f * t;
    }
    if (t < 1f / 2f) {
      return q;
    }
    if (t < 2f / 3f) {
      return p + (q - p) * (2f / 3f - t) * 6f;
    }
    return p;
  }
}
