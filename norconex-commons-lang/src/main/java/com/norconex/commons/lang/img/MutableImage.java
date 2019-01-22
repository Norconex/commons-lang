/* Copyright 2019 Norconex Inc.
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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Mode;

/**
 * Holds an image in memory and offers simple ways to do common opefactorns.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class MutableImage {

    private BufferedImage image;

    public MutableImage(Path imageFile) throws IOException {
        Objects.requireNonNull(imageFile, "'imageFile' must not be null.");
        image = ImageIO.read(imageFile.toFile());
    }
    public MutableImage(InputStream imageStream) throws IOException {
        Objects.requireNonNull(imageStream, "'imageStream' must not be null.");
        image = ImageIO.read(imageStream);
    }
    public MutableImage(Image image) {
        Objects.requireNonNull(image, "'image' must not be null.");
        if (image instanceof BufferedImage) {
            this.image = (BufferedImage) image;
        } else {
            BufferedImage bimage = new BufferedImage(
                    image.getWidth(null), image.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB);
            // Draw the image on to the buffered image
            Graphics2D bGr = bimage.createGraphics();
            bGr.drawImage(image, 0, 0, null);
            bGr.dispose();
            this.image = bimage;
        }
    }

    public BufferedImage toImage() {
        return image;
    }
    public void write(Path file) throws IOException {
        Objects.requireNonNull(file, "'file' must not be null.");
        write(file, null);
    }
    public void write(Path file, String format) throws IOException {
        Objects.requireNonNull(file, "'file' must not be null.");
        String f = format;
        if (StringUtils.isBlank(format)) {
            f = FilenameUtils.getExtension(file.toString());
        }
        ImageIO.write(image, f, file.toFile());
    }
    public void write(OutputStream out, String format) throws IOException {
        Objects.requireNonNull(out, "'out' must not be null.");
        Objects.requireNonNull(format, "'format' must not be null.");
        ImageIO.write(image, format, out);
    }

    public Dimension getDimension() {
        return new Dimension(image.getWidth(), image.getHeight());
    }

    public MutableImage rotateLeft() {
        return rotate(-90);
    }
    public MutableImage rotateRight() {
        return rotate(90);
    }
    public MutableImage rotate(double degrees) {
        return apply(t -> {
            int w = image.getWidth();
            int h = image.getHeight();
            double rads = Math.toRadians(degrees);
            double sin = Math.abs(Math.sin(rads));
            double cos = Math.abs(Math.cos(rads));
            int newWidth = (int) Math.floor(w * cos + h * sin);
            int newHeight = (int) Math.floor(h * cos + w * sin);
            t.translate((newWidth - w) / 2d, (newHeight - h) / 2d);
            int x = w / 2;
            int y = h / 2;
            t.rotate(rads, x, y);
        });
    }

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

    public MutableImage crop(Rectangle rectangle) {
        Objects.requireNonNull(rectangle, "'rectangle' must not be null.");
        return apply(Scalr.crop(image,
                rectangle.x, rectangle.y, rectangle.width, rectangle.height));
    }

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
        return apply(Scalr.resize(image, Mode.FIT_EXACT, width, height));
    }

    public MutableImage scaleWidth(int width) {
        return apply(Scalr.resize(image, Mode.FIT_TO_WIDTH, width));
    }
    public MutableImage scaleHeight(int height) {
        return apply(Scalr.resize(image, Mode.FIT_TO_HEIGHT, height));
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
        return apply(Scalr.resize(image, Mode.AUTOMATIC, maxWidth, maxHeight));
    }

    private MutableImage apply(BufferedImage newImage) {
        image.flush();
        image = newImage;
        return this;
    }

    private MutableImage apply(Consumer<AffineTransform> c) {
        AffineTransform tx = new AffineTransform();
        c.accept(tx);
        AffineTransformOp op =
                new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        // using scalr here resolves a lot of potential issues.
        BufferedImage newImage = Scalr.apply(image, op);
        if (newImage != image) {
            image.flush();
        }
        image = newImage;
        return this;
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
