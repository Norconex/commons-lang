package com.norconex.commons.lang.io;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

class ByteBufferInputStreamTest {

    @Test
    void testRead() throws IOException {
        String val = "12345";
        ByteBuffer bb = ByteBuffer.allocate(6);
        bb.put(val.getBytes(UTF_8));
        bb.position(0);
        try (ByteBufferInputStream in = new ByteBufferInputStream(bb)) {
            byte[] bytes = new byte[val.getBytes().length];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) in.read();
            }
            assertThat(new String(bytes, UTF_8)).isEqualTo(val);
            // no more to read
            assertThat(in.read()).isEqualTo(-1);
            // buffer has none remaining
            assertThat(in.read()).isEqualTo(-1);
        }
    }

    @Test
    void testReadBytetArrayIntInt() throws IOException {
        String val = "0123456789";
        ByteBuffer bb = ByteBuffer.allocate(20);
        try (ByteBufferInputStream in = new ByteBufferInputStream(bb)) {
            byte[] bytes = new byte[val.getBytes().length];
            in.read(bytes, 0, bytes.length);
            assertThat(bytes).isEqualTo(new byte[10]);
        }

        bb.clear();
        bb.put(val.getBytes(UTF_8));
        bb.position(0);
        try (ByteBufferInputStream in = new ByteBufferInputStream(bb)) {
            byte[] bytes = new byte[val.getBytes().length];
            in.read(bytes, 0, bytes.length);
            assertThat(new String(bytes, UTF_8)).isEqualTo(val);

            byte[] bigBytes = new byte[100];
            in.read(bigBytes, 0, 100);
            int numBytes = in.read(bigBytes, 0, 100);
            assertThat(numBytes).isEqualTo(-1);

        }
        bb.clear();
    }
}
