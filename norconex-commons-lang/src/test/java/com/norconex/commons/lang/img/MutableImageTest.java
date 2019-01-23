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
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Ignore
@RunWith(value = Parameterized.class)
public class MutableImageTest {

    private static final Logger LOG =
            LoggerFactory.getLogger(MutableImageTest.class);

    private String ext;

    public MutableImageTest(String extension) {
        super();
        this.ext = extension;
    }

    @Parameters(name = "{index}: {0}")
    public static Collection<String> extensions() {
        return Arrays.asList("png", "gif", "jpg", "bmp");
    }

    @Test
    public void testRotateLeft() throws IOException {
        test(MutableImage::rotateLeft);
    }

    @Test
    public void testRotate45Left() throws IOException {
        test(img -> img.rotate(-45));
    }
    @Test
    public void testRotateRight() throws IOException {
        test(MutableImage::rotateRight);
    }
    @Test
    public void testRotate45Right() throws IOException {
        test(img -> img.rotate(45));
    }
    @Test
    public void testFlipHorizontal() throws IOException {
        test(MutableImage::flipHorizontal);
    }
    @Test
    public void testFilpVertical() throws IOException {
        test(MutableImage::flipVertical);
    }
    @Test
    public void testCrop() throws IOException {
        test(img -> img.crop(new Rectangle(10, 48, 30, 26)));
    }

    @Test
    public void testStretch75x20() throws IOException {
        test(img -> img.stretch(75, 20));
    }
    @Test
    public void testStretchHeightFactor1_5() throws IOException {
        test(img -> img.stretchHeightFactor(1.5f));
    }
    @Test
    public void testStretchWidthFactor1_5() throws IOException {
        test(img -> img.stretchWidthFactor(1.5f));
    }
    @Test
    public void testStretchFactor3x0_5() throws IOException {
        test(img -> img.stretchFactor(3.0f, 0.5f));
    }

    @Test
    public void testScale75_20() throws IOException {
        test(img -> img.scale(75, 20));
    }
    @Test
    public void testScaleHeightFactor1_5() throws IOException {
        test(img -> img.scaleHeightFactor(1.5f));
    }
    @Test
    public void testScaleWidthFactor1_5() throws IOException {
        test(img -> img.scaleWidthFactor(1.5f));
    }
    @Test
    public void testScaleInHalf() throws IOException {
        test(img -> img.scaleFactor(0.5f));
    }
    @Test
    public void testScaleDouble() throws IOException {
        test(img -> img.scaleFactor(2.0f));
    }

    private void test(Consumer<MutableImage> c) throws IOException {
        String srcImage = "/img/triangle." + ext;
        new File("target/img-tests").mkdirs();
        String targetImage = "target/img-tests/" +
                Thread.currentThread().getStackTrace()[2].getMethodName()
                        + "." + ext;
        LOG.info("Writing: " + targetImage);
        try (InputStream is =
                MutableImageTest.class.getResourceAsStream(srcImage)) {
            MutableImage img = new MutableImage(is);
            //img.setResizeQuality(Quality.MAX);
            c.accept(img);
            Assert.assertTrue("Could not find image writer.", ImageIO.write(
                    img.toImage(), ext, new File(targetImage)));
        }
    }
}
