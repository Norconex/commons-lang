/* Copyright 2019-2022 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.commons.lang.img;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

import com.norconex.commons.lang.EqualsUtil;
import com.norconex.commons.lang.io.ByteArrayOutputStream;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * Holds an image in memory and offers simple ways to do common operations.
 * @since 2.0.0
 */
@EqualsAndHashCode
@ToString
public class MutableImage {

    public enum Quality {
        AUTO(Method.AUTOMATIC),
        LOW(Method.SPEED),
        MEDIUM(Method.BALANCED),
        HIGH(Method.QUALITY),
        MAX(Method.ULTRA_QUALITY);
        private Method scaleMethod;
        Quality(Method scaleMethod) {
            this.scaleMethod = scaleMethod;
        }
        public Method getScaleMethod() {
            return scaleMethod;
        }
    }

    private BufferedImage image;
    private Quality resizeQuality; // whether scaling or stretching

    public MutableImage(@NonNull Path imageFile) throws IOException {
        image = ImageIO.read(imageFile.toFile());
    }
    public MutableImage(@NonNull InputStream imageStream) throws IOException {
        image = ImageIO.read(imageStream);
    }
    public MutableImage(@NonNull Image image) {
        if (image instanceof BufferedImage bufImg) {
            this.image = bufImg;
        } else {
            var bimage = new BufferedImage(
                    image.getWidth(null), image.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB);
            // Draw the image on to the buffered image
            var bGr = bimage.createGraphics();
            bGr.drawImage(image, 0, 0, null);
            bGr.dispose();
            this.image = bimage;
        }
    }

    public Quality getResizeQuality() {
        return resizeQuality;
    }
    public MutableImage setResizeQuality(Quality resizeQuality) {
        this.resizeQuality = resizeQuality;
        return this;
    }

    public BufferedImage toImage() {
        return image;
    }
    public String toBase64String(String format) throws IOException {
        var baos = new ByteArrayOutputStream();
        ImageIO.write(deAlpha(image, format), format, baos);
        return "data:image/" + format + ";base64,"
                + Base64.getMimeEncoder().encodeToString(baos.toByteArray());
    }
    /**
     * Decodes a Base64 image string.
     * @param base64Image Base64 encoded image
     * @return a new mutable image or <code>null</code> if Base64 image is
     *     <code>null</code>
     * @throws IOException problem decoding the image
     * @since 3.0.0
     */
    public static MutableImage fromBase64String(String base64Image)
            throws IOException {
        if (base64Image == null) {
            return null;
        }

        var base64 = base64Image.replaceFirst("(?is)^.*?base64,(.*)$", "$1");
        var bais = new ByteArrayInputStream(
                Base64.getMimeDecoder().decode(base64));
        return new MutableImage(ImageIO.read(bais));
    }
    public InputStream toInputStream(String format) throws IOException {
        var os = new ByteArrayOutputStream();
        ImageIO.write(deAlpha(image, format), format, os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    public void write(@NonNull Path file) throws IOException {
        write(file, null);
    }
    public void write(@NonNull Path file, String format) throws IOException {
        var f = format;
        if (StringUtils.isBlank(format)) {
            f = FilenameUtils.getExtension(file.toString());
        }
        ImageIO.write(deAlpha(image, format), f, file.toFile());
    }
    public void write(@NonNull OutputStream out, @NonNull String format)
            throws IOException {
        ImageIO.write(deAlpha(image, format), format, out);
    }

    public Dimension getDimension() {
        return new Dimension(image.getWidth(), image.getHeight());
    }
    public int getHeight() {
        return image.getHeight();
    }
    public int getWidth() {
        return image.getWidth();
    }

    //--- Rotation -------------------------------------------------------------

    /**
     * Rotates this image counterclockwise by 90 degrees.
     * @return this image
     */
    public MutableImage rotateLeft() {
        return rotate(-90);
    }
    /**
     * Rotates this image clockwise by 90 degrees.
     * @return this image
     */
    public MutableImage rotateRight() {
        return rotate(90);
    }
    /**
     * Rotates this image clockwise by the specified degrees.
     * @param degrees degrees by which to rotate the image
     * @return this image
     */
    public MutableImage rotate(double degrees) {
        return apply(t -> {
            var w = image.getWidth();
            var h = image.getHeight();
            var rads = Math.toRadians(degrees);
            var sin = Math.abs(Math.sin(rads));
            var cos = Math.abs(Math.cos(rads));
            var newWidth = (int) Math.floor(w * cos + h * sin);
            var newHeight = (int) Math.floor(h * cos + w * sin);
            t.translate((newWidth - w) / 2d, (newHeight - h) / 2d);
            var x = w / 2;
            var y = h / 2;
            t.rotate(rads, x, y);
        });
    }

    //--- Flip -----------------------------------------------------------------

    public MutableImage flipHorizontal() {
        return apply(t -> {
            t.translate(image.getWidth(), 0);
            t.scale(-1.0, 1.0);
        });
    }
    public MutableImage flipVertical() {
        return apply(t -> {
            t.translate(0, image.getHeight());
            t.scale(1.0, -1.0);
        });
    }

    //--- Crop -----------------------------------------------------------------

    public MutableImage crop(@NonNull Rectangle rectangle) {
        return apply(Scalr.crop(image,
                rectangle.x, rectangle.y, rectangle.width, rectangle.height));
    }

    //--- Stretch --------------------------------------------------------------

    public MutableImage stretchWidth(int width) {
        return stretch(width, image.getHeight());
    }
    public MutableImage stretchHeight(int height) {
        return stretch(image.getWidth(), height);
    }
    public MutableImage stretchWidthFactor(double factor) {
        return stretch((int) (image.getWidth() * factor), image.getHeight());
    }
    public MutableImage stretchHeightFactor(double factor) {
        return stretch(image.getWidth(), (int) (image.getHeight() * factor));
    }
    public MutableImage stretch(Dimension dimension) {
        Objects.requireNonNull(dimension, "'dimension' must not be null.");
        return stretch((int) dimension.getWidth(), (int) dimension.getHeight());
    }
    public MutableImage stretchFactor(double factorWidth, double factorHeight) {
        return stretch(
                (int) (image.getWidth() * factorWidth),
                (int) (image.getHeight() * factorHeight));
    }
    public MutableImage stretch(int size) {
        return stretch(size, size);
    }
    public MutableImage stretch(int width, int height) {
        return apply(Scalr.resize(
                image, getScaleMethod(), Mode.FIT_EXACT, width, height));
    }

    //--- Scale ----------------------------------------------------------------

    public MutableImage scaleWidth(int width) {
        return apply(Scalr.resize(
                image, getScaleMethod(), Mode.FIT_TO_WIDTH, width));
    }
    public MutableImage scaleHeight(int height) {
        return apply(Scalr.resize(
                image, getScaleMethod(), Mode.FIT_TO_HEIGHT, height));
    }
    public MutableImage scaleWidthFactor(double factor) {
        return scaleWidth((int) (image.getWidth() * factor));
    }
    public MutableImage scaleHeightFactor(double factor) {
        return scaleHeight((int) (image.getHeight() * factor));
    }
    public MutableImage scaleFactor(double factor) {
        return scale(
                (int) (image.getWidth() * factor),
                (int) (image.getHeight() * factor));
    }
    public MutableImage scale(int size) {
        return scale(size, size);
    }
    public MutableImage scale(Dimension maxDimension) {
        Objects.requireNonNull(
                maxDimension, "'maxDimension' must not be null.");
        return scale((int) maxDimension.getWidth(),
                (int) maxDimension.getHeight());
    }
    public MutableImage scale(int maxWidth, int maxHeight) {
        return apply(Scalr.resize(image,
                getScaleMethod(), Mode.AUTOMATIC, maxWidth, maxHeight));
    }

    //--- Comparison -----------------------------------------------------------

    public boolean largerThan(MutableImage img) {
        if (img == null) {
            return false;
        }
        return largerThan(img.getDimension());
    }
    public boolean largerThan(Dimension dim) {
        if (dim == null) {
            return false;
        }
        return largerThan(dim.width, dim.height);
    }
    public boolean largerThan(int width, int height) {
        return image.getWidth() > width && image.getHeight() > height;
    }
    public MutableImage largest(MutableImage img) {
        if (img == null) {
            return this;
        }
        if (smallerThan(img)) {
            return img;
        }
        return this;
    }
    public boolean smallerThan(MutableImage img) {
        if (img == null) {
            return false;
        }
        return smallerThan(img.getDimension());
    }
    public boolean smallerThan(Dimension dim) {
        if (dim == null) {
            return false;
        }
        return smallerThan((int) dim.getWidth(), (int) dim.getHeight());
    }
    public boolean smallerThan(int width, int height) {
        return image.getWidth() < width && image.getHeight() < height;
    }
    public MutableImage smallest(MutableImage img) {
        if (img == null) {
            return this;
        }
        if (largerThan(img)) {
            return img;
        }
        return this;
    }
    public MutableImage tallest(MutableImage img) {
        if (img == null) {
            return this;
        }
        if (img.getHeight() > image.getHeight()) {
            return img;
        }
        return this;
    }
    public MutableImage widest(MutableImage img) {
        if (img == null) {
            return this;
        }
        if (img.getWidth() > image.getWidth()) {
            return img;
        }
        return this;
    }

    //--- Area -----------------------------------------------------------------

    public long getArea() {
        return (long) image.getWidth() * (long) image.getHeight();
    }

    //--- Private methods ------------------------------------------------------

    private Method getScaleMethod() {
        return Optional.ofNullable(
                resizeQuality).orElse(Quality.AUTO).getScaleMethod();
    }

    private MutableImage apply(BufferedImage newImage) {
        image.flush();
        image = newImage;
        return this;
    }

    private MutableImage apply(Consumer<AffineTransform> c) {
        var tx = new AffineTransform();
        c.accept(tx);
        var op =
                new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        // using scalr here resolves a lot of potential issues.
        var newImage = Scalr.apply(image, op);
        if (newImage != image) {
            image.flush();
        }
        image = newImage;
        return this;
    }

    private BufferedImage deAlpha(BufferedImage img, String format) {
        // Remove alpha layer for formats not supporting it. This prevents
        // some files from having a colored background (instead of transparency)
        // or to not be saved properly (e.g. png to bmp).
        if (EqualsUtil.equalsNoneIgnoreCase(format, "png", "gif")) {
            var fixedImg = new BufferedImage(img.getWidth(), img.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            fixedImg.createGraphics().drawImage(
                    img, 0, 0, Color.WHITE, null);
            return fixedImg;
        }
        return img;
    }
}
