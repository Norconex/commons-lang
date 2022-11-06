/* Copyright 2022 Norconex Inc.
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
package com.norconex.commons.lang.mock;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.function.FailableRunnable;

public class Mocker {

    public static synchronized String mockStdInOutErr(
            FailableRunnable<IOException> runnable, String... userInputs)
                    throws IOException {
        InputStream originalInput = System.in;
        PrintStream originalOutput = System.out;
        PrintStream originalError = System.err;
        String error = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out, false, UTF_8));
            System.setErr(new PrintStream(out, false, UTF_8));

            ByteArrayInputStream in = new ByteArrayInputStream(
                    (String.join("\n", userInputs) + "\n").getBytes());
            System.setIn(in);
            runnable.run();
            return out.toString();
        } catch (NoSuchElementException e) {
            error = "Tried to read more values than the number of inputs "
                    + "available.";
            throw e;
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
            System.setErr(originalError);
            if (error != null) {
                System.err.println(error);
            }
        }
    }
}
