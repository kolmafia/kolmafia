/*
 * $Id: ColorUtil.java,v 1.12 2009/05/25 01:52:13 kschaefe Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.jdesktop.swingx.color;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

/**
 * A collection of utility methods for working with {@code Color}s.
 * 
 * @author joshua.marinacci@sun.com
 * @author Karl George Schaefer
 */
public class ColorUtil {

    /**
     * Returns a new color equal to the old one, except that there is no alpha
     * (transparency) channel.
     * <p>
     * This method is a convenience and has the same effect as {@code
     * setAlpha(color, 255)}.
     * 
     * @param color
     *            the color to remove the alpha (transparency) from
     * @return a new non-transparent {@code Color}
     * @throws NullPointerException
     *             if {@code color} is {@code null}
     */
    public static Color removeAlpha(Color color) {
        return setAlpha(color, 255);
    }

    /**
     * Returns a new color equal to the old one, except alpha (transparency)
     * channel is set to the new value.
     * 
     * @param color
     *            the color to modify
     * @param alpha
     *            the new alpha (transparency) level. Must be an int between 0
     *            and 255
     * @return a new alpha-applied {@code Color}
     * @throws IllegalArgumentException
     *             if {@code alpha} is not between 0 and 255 inclusive
     * @throws NullPointerException
     *             if {@code color} is {@code null}
     */
    public static Color setAlpha(Color color, int alpha) {
        if (alpha < 0 || alpha > 255) {
            throw new IllegalArgumentException("invalid alpha value");
        }

        return new Color(
                color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /**
     * Returns a new color equal to the old one, except the saturation is set to
     * the new value. The new color will have the same alpha (transparency) as
     * the original color.
     * <p>
     * The color is modified using HSB calculations. The saturation must be a
     * float between 0 and 1. If 0 the resulting color will be gray. If 1 the
     * resulting color will be the most saturated possible form of the passed in
     * color.
     * 
     * @param color
     *            the color to modify
     * @param saturation
     *            the saturation to use in the new color
     * @return a new saturation-applied {@code Color}
     * @throws IllegalArgumentException
     *             if {@code saturation} is not between 0 and 1 inclusive
     * @throws NullPointerException
     *             if {@code color} is {@code null}
     */
    public static Color setSaturation(Color color, float saturation) {
        if (saturation < 0f || saturation > 1f) {
            throw new IllegalArgumentException("invalid saturation value");
        }

        int alpha = color.getAlpha();
        
        float[] hsb = Color.RGBtoHSB(
                color.getRed(), color.getGreen(), color.getBlue(), null);
        Color c = Color.getHSBColor(hsb[0], saturation, hsb[2]);
        
        return setAlpha(c, alpha);
    }

    /**
     * Returns a new color equal to the old one, except the brightness is set to
     * the new value. The new color will have the same alpha (transparency) as
     * the original color.
     * <p>
     * The color is modified using HSB calculations. The brightness must be a
     * float between 0 and 1. If 0 the resulting color will be black. If 1 the
     * resulting color will be the brightest possible form of the passed in
     * color.
     * 
     * @param color
     *            the color to modify
     * @param brightness
     *            the brightness to use in the new color
     * @return a new brightness-applied {@code Color}
     * @throws IllegalArgumentException
     *             if {@code brightness} is not between 0 and 1 inclusive
     * @throws NullPointerException
     *             if {@code color} is {@code null}
     */
    public static Color setBrightness(Color color, float brightness) {
        if (brightness < 0f || brightness > 1f) {
            throw new IllegalArgumentException("invalid brightness value");
        }

        int alpha = color.getAlpha();

        float[] hsb = Color.RGBtoHSB(
                color.getRed(), color.getGreen(), color.getBlue(), null);
        Color c = Color.getHSBColor(hsb[0], hsb[1], brightness);

        return setAlpha(c, alpha);
    }

    /**
     * Creates a {@code String} that represents the supplied color as a
     * hex-value RGB triplet, including the "#". The return value is suitable
     * for use in HTML. The alpha (transparency) channel is neither include nor
     * used in producing the string.
     * 
     * @param color
     *            the color to convert
     * @return the hex {@code String}
     */
    public static String toHexString(Color color) {
        return "#" + Integer.toHexString(color.getRGB() | 0xFF000000).substring(2);
    }

    /**
     * Computes an appropriate foreground color (either white or black) for the
     * given background color.
     * 
     * @param bg
     *            the background color
     * @return {@code Color.WHITE} or {@code Color.BLACK}
     * @throws NullPointerException
     *             if {@code bg} is {@code null}
     */
    public static Color computeForeground(Color bg) {
        float[] rgb = bg.getRGBColorComponents(null);
        float y = .3f * rgb[0] + .59f * rgb[1] + .11f * rgb[2];
        
        return y > .5f ? Color.BLACK : Color.WHITE;
    }

    /**
     * Blends two colors to create a new color. The {@code origin} color is the
     * base for the new color and regardless of its alpha component, it is
     * treated a fully opaque (alpha 255).
     * 
     * @param origin
     *            the base of the new color
     * @param over
     *            the alpha-enabled color to add to the {@code origin} color
     * @return a new color comprised of the {@code origin} and {@code over}
     *         colors
     */
    public static Color blend(Color origin, Color over) {
        if (over == null) {
            return origin;
        }

        if (origin == null) {
            return over;
        }

        int a = over.getAlpha();
        
        int rb = (((over.getRGB() & 0x00ff00ff) * a)
                    + ((origin.getRGB() & 0x00ff00ff) * (0xff - a))) & 0xff00ff00;
        int g = (((over.getRGB() & 0x0000ff00) * a)
                    + ((origin.getRGB() & 0x0000ff00) * (0xff - a))) & 0x00ff0000;

        return new Color((over.getRGB() & 0xff000000) | ((rb | g) >> 8));
    }
    
    /**
     * Obtain a <code>java.awt.Paint</code> instance which draws a checker
     * background of black and white. 
     * Note: The returned instance may be shared.
     * Note: This method should be reimplemented to not use a png resource.
     *
     * @return a Paint implementation
     */
    public static Paint getCheckerPaint() {
        return getCheckerPaint(Color.white,Color.gray,20);
    }
    
    public static Paint getCheckerPaint(Color c1, Color c2, int size) {
        BufferedImage img = new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        
        try {
            g.setColor(c1);
            g.fillRect(0, 0, size, size);
            g.setColor(c2);
            g.fillRect(0, 0, size / 2, size / 2);
            g.fillRect(size / 2, size / 2, size / 2, size / 2);
        } finally {
            g.dispose();
        }
        
        return new TexturePaint(img,new Rectangle(0,0,size,size));
    }
    
    /**
     * Draws an image on top of a component by doing a 3x3 grid stretch of the image
     * using the specified insets.
     * <p>
     * TODO this is image related; move to GraphicsUtilities
     */
    public static void tileStretchPaint(Graphics g, 
                JComponent comp,
                BufferedImage img,
                Insets ins) {
        
        int left = ins.left;
        int right = ins.right;
        int top = ins.top;
        int bottom = ins.bottom;
        
        // top
        g.drawImage(img,
                    0,0,left,top,
                    0,0,left,top,
                    null);
        g.drawImage(img,
                    left,                 0, 
                    comp.getWidth() - right, top, 
                    left,                 0, 
                    img.getWidth()  - right, top, 
                    null);
        g.drawImage(img,
                    comp.getWidth() - right, 0, 
                    comp.getWidth(),         top, 
                    img.getWidth()  - right, 0, 
                    img.getWidth(),          top, 
                    null);

        // middle
        g.drawImage(img,
                    0,    top, 
                    left, comp.getHeight()-bottom,
                    0,    top,   
                    left, img.getHeight()-bottom,
                    null);
        
        g.drawImage(img,
                    left,                  top, 
                    comp.getWidth()-right,      comp.getHeight()-bottom,
                    left,                  top,   
                    img.getWidth()-right,  img.getHeight()-bottom,
                    null);
         
        g.drawImage(img,
                    comp.getWidth()-right,     top, 
                    comp.getWidth(),           comp.getHeight()-bottom,
                    img.getWidth()-right, top,   
                    img.getWidth(),       img.getHeight()-bottom,
                    null);
        
        // bottom
        g.drawImage(img,
                    0,comp.getHeight()-bottom, 
                    left, comp.getHeight(),
                    0,img.getHeight()-bottom,   
                    left,img.getHeight(),
                    null);
        g.drawImage(img,
                    left,                    comp.getHeight()-bottom, 
                    comp.getWidth()-right,        comp.getHeight(),
                    left,                    img.getHeight()-bottom,   
                    img.getWidth()-right,    img.getHeight(),
                    null);
        g.drawImage(img,
                    comp.getWidth()-right,     comp.getHeight()-bottom, 
                    comp.getWidth(),           comp.getHeight(),
                    img.getWidth()-right, img.getHeight()-bottom,   
                    img.getWidth(),       img.getHeight(),
                    null);
    }

    public static Color interpolate(Color b, Color a, float t) {
        float[] acomp = a.getRGBComponents(null);
        float[] bcomp = b.getRGBComponents(null);
        float[] ccomp = new float[4];
        
//        log.fine(("a comp ");
//        for(float f : acomp) {
//            log.fine((f);
//        }
//        for(float f : bcomp) {
//            log.fine((f);
//        }
        for(int i=0; i<4; i++) {
            ccomp[i] = acomp[i] + (bcomp[i]-acomp[i])*t;
        }
//        for(float f : ccomp) {
//            log.fine((f);
//        }
        
        return new Color(ccomp[0],ccomp[1],ccomp[2],ccomp[3]);
    }

}
