/*
 * $Id: ImagePainter.java,v 1.21 2009/03/10 14:36:26 kschaefe Exp $
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

package org.jdesktop.swingx.painter;

//import org.jdesktop.swingx.editors.PainterUtil;
import org.jdesktop.swingx.painter.effects.AreaEffect;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * <p>A Painter instance that paints an image. Any Image is acceptable. This
 * Painter also allows the developer to specify a "Style" -- CENTERED, TILED,
 * SCALED, POSITIONED, and CSS_POSITIONED; with the following meanings:</p>
 *
 * <ul>
 *  <li><b>CENTERED</b>: draws the image unscaled and positioned in the center of
 * the component</li>
 *  <li><b>TILED</b>: draws the image repeatedly across the component, filling the
 * entire background.</li>
 *  <li><b>SCALED</b>: draws the image stretched large enough (or small enough) to
 * cover the entire component. The stretch may not preserve the aspect ratio of the
 * original image.</li>
 *  <li><b>POSITIONED</b>: draws the image at the location specified by the imageLocation
 * property. This style of drawing will respect the imageScale property.</li>
 *  <li><b>CSS_POSITIONED</b>: draws the image using CSS style background positioning.
 *It will use the location specified by the imageLocation property. This property should
 *contain a point with the x and y values between 0 and 1. 0,0 will put the image in the
 *upper left hand corner, 1,1 in the lower right, and 0.5,0.5 in the center. All other values
 *will be interpolated accordingly. For a more
 * complete definition of the positioning algorithm see the
 * <a href="http://www.w3.org/TR/CSS21/colors.html#propdef-background-position">CSS 2.1 spec</a>.
 * </li>
 * </ul>
 *
 * @author Richard
 */
public class ImagePainter extends AbstractAreaPainter<Object> {
    /**
     * Logger to use
     */
    private static final Logger LOG = Logger.getLogger(ImagePainter.class.getName());
    
    /**
     * The image to draw
     */
    private transient BufferedImage img;
    
    private URL imageURL;
    
    private boolean horizontalRepeat;
    private boolean verticalRepeat;
    
    private boolean scaleToFit = false;
    private ScaleType scaleType = ScaleType.InsideFit;
    
    public enum ScaleType { InsideFit, OutsideFit, Distort }
    
    /**
     * Create a new ImagePainter. By default there is no image, and the alignment
     * is centered.
     */
    public ImagePainter() {
        this((BufferedImage)null);
    }
    
    /**
     * Create a new ImagePainter with the specified image and the Style
     * Style.CENTERED
     *
     * @param image the image to be painted
     */
    public ImagePainter(BufferedImage image) {
        this(image,HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
    }
    
    /**
     * Create a new ImagePainter with the specified image and alignment.
     * @param horizontal the horizontal alignment
     * @param vertical the vertical alignment
     * @param image the image to be painted
     */
    public ImagePainter(BufferedImage image, HorizontalAlignment horizontal, VerticalAlignment vertical) {
        super();
        setCacheable(true);
        this.img = image;
        this.setVerticalAlignment(vertical);
        this.setHorizontalAlignment(horizontal);
        this.setFillPaint(null);
        this.setBorderPaint(null);
    }
    
    public ImagePainter(URL url) throws IOException {
        this(ImageIO.read(url));
    }
    public ImagePainter(URL url, HorizontalAlignment horizontal, VerticalAlignment vertical) throws IOException {
        this(ImageIO.read(url),horizontal,vertical);
    }
    
    /**
     * Sets the image to paint with.
     * @param image if null, clears the image. Otherwise, this will set the
     * image to be painted.
     */
    public void setImage(BufferedImage image) {
        if (image != img) {
            Image oldImage = img;
            img = image;
            setDirty(true);
            firePropertyChange("image", oldImage, img);
        }
    }
    
    /**
     * Gets the current image used for painting.
     * @return the image used for painting
     */
    public BufferedImage getImage() {
        if(img == null && imageURL != null) {
            loadImage();
        }
        return img;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
protected void doPaint(Graphics2D g, Object component, int width, int height) {
        if (img == null && imageURL != null) {
            loadImage();
        }
        
        Shape shape = provideShape(g, component,width,height);
        switch (getStyle()) {
            case BOTH:
                drawBackground(g,shape,width,height);
                drawBorder(g,shape,width,height);
                break;
            case FILLED:
                drawBackground(g,shape,width,height);
                break;
            case OUTLINE:
                drawBorder(g,shape,width,height);
                break;
            case NONE:
                break;
        }
    }
    
    private void drawBackground(Graphics2D g, Shape shape, int width, int height) {
        Paint p = getFillPaint();
        
        if(p != null) {
            if(isPaintStretched()) {
                p = calculateSnappedPaint(p, width, height);
            }
            g.setPaint(p);
            g.fill(shape);
        }
        
        if(getAreaEffects() != null) {
            for(AreaEffect ef : getAreaEffects()) {
                ef.apply(g, shape, width, height);
            }
        }
        
        
        if (img != null) {
            int imgWidth = img.getWidth(null);
            int imgHeight = img.getHeight(null);
            if (imgWidth == -1 || imgHeight == -1) {
                //image hasn't completed loading, do nothing
            } else {
                Rectangle rect = calculateLayout(imgWidth, imgHeight, width, height);
                if(verticalRepeat || horizontalRepeat) {
                    Shape oldClip = g.getClip();
                    Shape clip = g.getClip();
                    if(clip == null) {
                        clip = new Rectangle(0,0,width,height);
                    }
                    Area area = new Area(clip);
                    TexturePaint tp = new TexturePaint(img,rect);
                    if(verticalRepeat && horizontalRepeat) {
                        area.intersect(new Area(new Rectangle(0,0,width,height)));
                        g.setClip(area);
                    } else if (verticalRepeat) {
                        area.intersect(new Area(new Rectangle(rect.x,0,rect.width,height)));
                        g.setClip(area);
                    } else {
                        area.intersect(new Area(new Rectangle(0,rect.y,width,rect.height)));
                        g.setClip(area);
                    }
                    g.setPaint(tp);
                    g.fillRect(0,0,width,height);
                    g.setClip(oldClip);
                } else {
                    if(scaleToFit) {
                        int sw = imgWidth;
                        int sh = imgHeight;
                        if(scaleType == ScaleType.InsideFit) {
                            if(sw > width) {
                                float scale = (float)width/(float)sw;
                                sw = (int)(sw * scale);
                                sh = (int)(sh * scale);
                            }
                            if(sh > height) {
                                float scale = (float)height/(float)sh;
                                sw = (int)(sw * scale);
                                sh = (int)(sh * scale);
                            }
                        }
                        if(scaleType == ScaleType.OutsideFit) {
                            if(sw > width) {
                                float scale = (float)width/(float)sw;
                                sw = (int)(sw * scale);
                                sh = (int)(sh * scale);
                            }
                            if(sh < height) {
                                float scale = (float)height/(float)sh;
                                sw = (int)(sw * scale);
                                sh = (int)(sh * scale);
                            }
                        }
                        if(scaleType == ScaleType.Distort) {
                            sw = width;
                            sh = height;
                        }
                        int x=0;
                        int y=0;
                        switch(getHorizontalAlignment()) {
                            case CENTER:
                                x=(width/2)-(sw/2);
                                break;
                            case RIGHT:
                                x=width-sw;
                                break;
                        }
                        switch(getVerticalAlignment()) {
                            case CENTER:
                                y=(height/2)-(sh/2);
                                break;
                            case BOTTOM:
                                y=height-sh;
                                break;
                        }
                        g.drawImage(img, x, y, sw, sh, null);
                    } else {
                        int sw = rect.width;
                        int sh = rect.height;
                        if(imageScale != 1.0) {
                            sw = (int)((double)sw * imageScale);
                            sh = (int)((double)sh * imageScale);
                        }
                        g.drawImage(img, rect.x, rect.y, sw, sh, null);
                    }
                }
            }
        }
        
    }
    
    private void drawBorder(Graphics2D g, Shape shape, int width, int height) {
        if(getBorderPaint() != null) {
            g.setPaint(getBorderPaint());
            g.setStroke(new BasicStroke(getBorderWidth()));
            g.draw(shape);
        }
    }
    
    public void setScaleToFit(boolean scaleToFit) {
        boolean old = isScaleToFit(); 
        this.scaleToFit = scaleToFit;
        setDirty(true);
        firePropertyChange("scaleToFit", old, isScaleToFit());
    }
    
    
    public boolean isScaleToFit() {
        return scaleToFit;
    }


    private double imageScale = 1.0;

    private Logger log = Logger.getLogger(ImagePainter.class.getName());
    
    /**
     * Sets the scaling factor used when drawing the image
     * @param imageScale the new image scaling factor
     */
    public void setImageScale(double imageScale) {
        double old = getImageScale();
        this.imageScale = imageScale;
        setDirty(true);
        firePropertyChange("imageScale",old,this.imageScale);
    }
    /**
     * Gets the current scaling factor used when drawing an image.
     * @return the current scaling factor
     */
    public double getImageScale() {
        return imageScale;
    }
    
    private void loadImage() {
        try {
            String img = getImageString();
            // use the resolver if it's there
            if(img != null) {
                URL url = new URL(img);
                setImage(ImageIO.read(url));
            }
        } catch (IOException ex) {
            log.severe("ex: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private String imageString;
    
    /**
     * Used by the persistence mechanism.
     */
    public String getImageString() {
        return imageString;
    }
    
    /**
     * Used by the persistence mechanism.
     */
    public void setImageString(String imageString) {
        log.fine("setting image string to: " + imageString);
        String old = this.getImageString();
        this.imageString = imageString;
        loadImage();
        setDirty(true);
        firePropertyChange("imageString",old,imageString);
    }
    /*
    public String getBaseURL() {
        return baseURL;
    }
     
    private String baseURL;
     
    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }*/
    
    /**
     * Indicates if the image will be repeated horizontally.
     * @return if the image will be repeated horizontally
     */
    public boolean isHorizontalRepeat() {
        return horizontalRepeat;
    }
    
    /**
     * Sets if the image should be repeated horizontally.
     * @param horizontalRepeat the new horizontal repeat value
     */
    public void setHorizontalRepeat(boolean horizontalRepeat) {
        boolean old = this.isHorizontalRepeat();
        this.horizontalRepeat = horizontalRepeat;
        setDirty(true);
        firePropertyChange("horizontalRepeat",old,this.horizontalRepeat);
    }
    
    /**
     * Indicates if the image will be repeated vertically.
     * @return if the image will be repeated vertically
     */
    public boolean isVerticalRepeat() {
        return verticalRepeat;
    }
    
    /**
     * Sets if the image should be repeated vertically.
     * @param verticalRepeat new value for the vertical repeat
     */
    public void setVerticalRepeat(boolean verticalRepeat) {
        boolean old = this.isVerticalRepeat();
        this.verticalRepeat = verticalRepeat;
        setDirty(true);
        firePropertyChange("verticalRepeat",old,this.verticalRepeat);
    }
    
    /**
     *
     */
    @Override
protected Shape provideShape(Graphics2D g, Object comp, int width, int height) {
        if(getImage() != null) {
            BufferedImage img = getImage();
            int imgWidth = img.getWidth();
            int imgHeight = img.getHeight();
            
            return calculateLayout(imgWidth, imgHeight, width, height);
        }
        return new Rectangle(0,0,0,0);
        
    }
    
    public ScaleType getScaleType() {
        return scaleType;
    }
    
    public void setScaleType(ScaleType scaleType) {
        ScaleType old = getScaleType();
        this.scaleType = scaleType;
        setDirty(true);
        firePropertyChange("scaleType", old, getScaleType());
    }
    
}
