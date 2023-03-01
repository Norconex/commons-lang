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

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.norconex.commons.lang.img.MutableImage.Quality;
import com.norconex.commons.lang.io.ByteArrayOutputStream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class MutableImageTest {

    @TempDir
    private Path tempDir;

    // 50 x 75
    private MutableImage img;

    @BeforeEach
    void beforeEach() throws IOException {
        img = new MutableImage(
                Paths.get("src/test/resources/img/triangle.png"));
    }

    @Test
    void testMisc() throws IOException {
        img.toBase64String("png");
        img.toInputStream("png");
        img.write(tempDir.resolve("test.png"));
        img.write(tempDir.resolve("test"), "png");
        img.write(new ByteArrayOutputStream(), "png");

        img.setResizeQuality(Quality.HIGH);
        assertThat(img.getResizeQuality()).isSameAs(Quality.HIGH);
        assertThat(img.getDimension()).isEqualTo(new Dimension(50, 75));
        assertThat(img.getWidth()).isEqualTo(50);
        assertThat(img.getHeight()).isEqualTo(75);

        assertThat(img.getArea()).isEqualTo(3750);
    }

    @Test
    void testStretch() throws IOException {
        img.stretchWidth(100);
        img.stretchHeight(150);
        assertThat(img.getWidth()).isEqualTo(100);
        assertThat(img.getHeight()).isEqualTo(150);
        img.stretch(new Dimension(200, 300));
        assertThat(img.getWidth()).isEqualTo(200);
        assertThat(img.getHeight()).isEqualTo(300);
        img.stretch(25);
        assertThat(img.getWidth()).isEqualTo(25);
        assertThat(img.getHeight()).isEqualTo(25);
    }

    @Test
    void testScale() throws IOException {
        // scaling respects ratios so scaling numbers provided are max values
        img.scale(150);
        assertThat(img.getWidth()).isEqualTo(100);
        assertThat(img.getHeight()).isEqualTo(150);
        img.scale(new Dimension(1000, 50));
        assertThat(img.getWidth()).isEqualTo(33);
        assertThat(img.getHeight()).isEqualTo(50);
    }

    @Test
    void testComparisonImage() throws IOException {
        var largerImg = new MutableImage(img.toImage()).scaleFactor(2);
        var sameImg = new MutableImage(img.toImage());
        var smallerImg =
                new MutableImage(img.toImage()).scaleFactor(0.5);
        assertThat(img.largerThan(largerImg)).isFalse();
        assertThat(img.largerThan(sameImg)).isFalse();
        assertThat(img.largerThan(smallerImg)).isTrue();
        assertThat(img.largerThan((MutableImage) null)).isFalse();
        assertThat(img.smallerThan(largerImg)).isTrue();
        assertThat(img.smallerThan(sameImg)).isFalse();
        assertThat(img.smallerThan(smallerImg)).isFalse();
        assertThat(img.smallerThan((MutableImage) null)).isFalse();
        assertThat(img.largest(largerImg)).isSameAs(largerImg);
        assertThat(img.largest(sameImg)).isSameAs(img);
        assertThat(img.largest(smallerImg)).isSameAs(img);
        assertThat(img.largest(null)).isSameAs(img);
        assertThat(img.smallest(largerImg)).isSameAs(img);
        assertThat(img.smallest(sameImg)).isSameAs(img);
        assertThat(img.smallest(smallerImg)).isSameAs(smallerImg);
        assertThat(img.smallest(null)).isSameAs(img);
        assertThat(img.tallest(largerImg)).isSameAs(largerImg);
        assertThat(img.tallest(sameImg)).isSameAs(img);
        assertThat(img.tallest(smallerImg)).isSameAs(img);
        assertThat(img.tallest(null)).isSameAs(img);
        assertThat(img.widest(largerImg)).isSameAs(largerImg);
        assertThat(img.widest(sameImg)).isSameAs(img);
        assertThat(img.widest(smallerImg)).isSameAs(img);
        assertThat(img.widest(null)).isSameAs(img);
    }

    @Test
    void testComparisonDimension() throws IOException {
        var sameDimension = new Dimension(50, 75);
        var largerDimension = new Dimension(51, 76);
        var smallerDimension = new Dimension(49, 74);
        assertThat(img.largerThan(largerDimension)).isFalse();
        assertThat(img.largerThan(sameDimension)).isFalse();
        assertThat(img.largerThan(smallerDimension)).isTrue();
        assertThat(img.largerThan((Dimension) null)).isFalse();
        assertThat(img.smallerThan(largerDimension)).isTrue();
        assertThat(img.smallerThan(sameDimension)).isFalse();
        assertThat(img.smallerThan(smallerDimension)).isFalse();
        assertThat(img.smallerThan((Dimension) null)).isFalse();
    }

    @ExtensionTest
    void testRotateLeft(String ext) throws IOException {
        test(MutableImage::rotateLeft, ext);
    }
    @ExtensionTest
    void testRotate45Left(String ext) throws IOException {
        test(img -> img.rotate(-45), ext);
    }
    @ExtensionTest
    void testRotateRight(String ext) throws IOException {
        test(MutableImage::rotateRight, ext);
    }
    @ExtensionTest
    void testRotate45Right(String ext) throws IOException {
        test(img -> img.rotate(45), ext);
    }
    @ExtensionTest
    void testFlipHorizontal(String ext) throws IOException {
        test(MutableImage::flipHorizontal, ext);
    }
    @ExtensionTest
    void testFilpVertical(String ext) throws IOException {
        test(MutableImage::flipVertical, ext);
    }
    @ExtensionTest
    void testCrop(String ext) throws IOException {
        test(img -> img.crop(new Rectangle(10, 48, 30, 26)), ext);
    }

    @ExtensionTest
    void testStretch75x20(String ext) throws IOException {
        test(img -> img.stretch(75, 20), ext);
    }
    @ExtensionTest
    void testStretchHeightFactor1_5(String ext) throws IOException {
        test(img -> img.stretchHeightFactor(1.5f), ext);
    }
    @ExtensionTest
    void testStretchWidthFactor1_5(String ext) throws IOException {
        test(img -> img.stretchWidthFactor(1.5f), ext);
    }
    @ExtensionTest
    void testStretchFactor3x0_5(String ext) throws IOException {
        test(img -> img.stretchFactor(3.0f, 0.5f), ext);
    }

    @ExtensionTest
    void testScale75_20(String ext) throws IOException {
        test(img -> img.scale(75, 20), ext);
    }
    @ExtensionTest
    void testScaleHeightFactor1_5(String ext) throws IOException {
        test(img -> img.scaleHeightFactor(1.5f), ext);
    }
    @ExtensionTest
    void testScaleWidthFactor1_5(String ext) throws IOException {
        test(img -> img.scaleWidthFactor(1.5f), ext);
    }
    @ExtensionTest
    void testScaleInHalf(String ext) throws IOException {
        test(img -> img.scaleFactor(0.5f), ext);
    }
    @ExtensionTest
    void testScaleDouble(String ext) throws IOException {
        test(img -> img.scaleFactor(2.0f), ext);
    }

    private void test(Consumer<MutableImage> c, String ext) throws IOException {
        var srcImage = "/img/triangle." + ext;
        new File("target/img-tests").mkdirs();
        var targetImage = "target/img-tests/" +
                Thread.currentThread().getStackTrace()[2].getMethodName()
                        + "." + ext;
        LOG.debug("Writing: " + targetImage);
        try (var is =
                MutableImageTest.class.getResourceAsStream(srcImage)) {
            var imgToWrite = new MutableImage(is);
            //img.setResizeQuality(Quality.MAX);
            c.accept(imgToWrite);
            Assertions.assertTrue(ImageIO.write(
                    imgToWrite.toImage(), ext, new File(targetImage)),
                    "Could not find image writer.");
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ParameterizedTest(name = "extension: {0}")
    @MethodSource("extensionProvider")
    @interface ExtensionTest {
    }
    @SuppressWarnings("unused")
    private static Stream<String> extensionProvider() {
        return Stream.of("png", "gif", "jpg", "bmp");
    }
}
