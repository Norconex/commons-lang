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

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Ignore
public class MutableImageTest {

    private static final Logger LOG =
            LoggerFactory.getLogger(MutableImageTest.class);

//    private String ext;

    static Stream<String> extensionProvider() {
        return Stream.of("png", "gif", "jpg", "bmp");
    }

//    public MutableImageTest(String extension) {
//        super();
//        this.ext = extension;
//    }

//    @Parameters(name = "{index}: {0}")
//    public static Collection<String> extensions() {
//        return Arrays.asList("png", "gif", "jpg", "bmp");
//    }

    @ExtensionTest
    public void testRotateLeft(String ext) throws IOException {
        test(MutableImage::rotateLeft, ext);
    }

    @ExtensionTest
    public void testRotate45Left(String ext) throws IOException {
        test(img -> img.rotate(-45), ext);
    }
    @ExtensionTest
    public void testRotateRight(String ext) throws IOException {
        test(MutableImage::rotateRight, ext);
    }
    @ExtensionTest
    public void testRotate45Right(String ext) throws IOException {
        test(img -> img.rotate(45), ext);
    }
    @ExtensionTest
    public void testFlipHorizontal(String ext) throws IOException {
        test(MutableImage::flipHorizontal, ext);
    }
    @ExtensionTest
    public void testFilpVertical(String ext) throws IOException {
        test(MutableImage::flipVertical, ext);
    }
    @ExtensionTest
    public void testCrop(String ext) throws IOException {
        test(img -> img.crop(new Rectangle(10, 48, 30, 26)), ext);
    }

    @ExtensionTest
    public void testStretch75x20(String ext) throws IOException {
        test(img -> img.stretch(75, 20), ext);
    }
    @ExtensionTest
    public void testStretchHeightFactor1_5(String ext) throws IOException {
        test(img -> img.stretchHeightFactor(1.5f), ext);
    }
    @ExtensionTest
    public void testStretchWidthFactor1_5(String ext) throws IOException {
        test(img -> img.stretchWidthFactor(1.5f), ext);
    }
    @ExtensionTest
    public void testStretchFactor3x0_5(String ext) throws IOException {
        test(img -> img.stretchFactor(3.0f, 0.5f), ext);
    }

    @ExtensionTest
    public void testScale75_20(String ext) throws IOException {
        test(img -> img.scale(75, 20), ext);
    }
    @ExtensionTest
    public void testScaleHeightFactor1_5(String ext) throws IOException {
        test(img -> img.scaleHeightFactor(1.5f), ext);
    }
    @ExtensionTest
    public void testScaleWidthFactor1_5(String ext) throws IOException {
        test(img -> img.scaleWidthFactor(1.5f), ext);
    }
    @ExtensionTest
    public void testScaleInHalf(String ext) throws IOException {
        test(img -> img.scaleFactor(0.5f), ext);
    }
    @ExtensionTest
    public void testScaleDouble(String ext) throws IOException {
        test(img -> img.scaleFactor(2.0f), ext);
    }

    private void test(Consumer<MutableImage> c, String ext) throws IOException {
        String srcImage = "/img/triangle." + ext;
        new File("target/img-tests").mkdirs();
        String targetImage = "target/img-tests/" +
                Thread.currentThread().getStackTrace()[2].getMethodName()
                        + "." + ext;
        LOG.debug("Writing: " + targetImage);
        try (InputStream is =
                MutableImageTest.class.getResourceAsStream(srcImage)) {
            MutableImage img = new MutableImage(is);
            //img.setResizeQuality(Quality.MAX);
            c.accept(img);
            Assertions.assertTrue(ImageIO.write(
                    img.toImage(), ext, new File(targetImage)),
                    "Could not find image writer.");
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ParameterizedTest(name = "extension: {0}")
//    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("extensionProvider")
    @interface ExtensionTest {
    }
}
