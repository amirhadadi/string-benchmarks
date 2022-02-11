/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.google.protobuf;


import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class Utf8Benchmark {

  private static final Utf8.SafeProcessor safeProcessor = new Utf8.SafeProcessor();
  private static final Utf8.UnsafeProcessor unsafeProcessor = new Utf8.UnsafeProcessor();

  @Param({"The quick brown fox jumps over the lazy dog",
          "Quizdeltagerne spiste jordbær med fløde, mens cirkusklovnen",
          "\uD841\uDF0E\uD841\uDF31\uD841\uDF79\uD843\uDC53\uD843\uDC78"
            + "\uD843\uDC96\uD843\uDCCF\uD843\uDCD5\uD843\uDD15\uD843\uDD7C\uD843\uDD7F"
            + "\uD843\uDE0E\uD843\uDE0F\uD843\uDE77\uD843\uDE9D\uD843\uDEA2"})
  private String text;

  private byte[] encodedText;

  @Setup
  public void setup() {
    encodedText = text.getBytes(StandardCharsets.UTF_8);
  }

  @Benchmark
  public String protobufSafe() throws Throwable {
    return safeProcessor.decodeUtf8(encodedText, 0, encodedText.length);
  }

  @Benchmark
  public String protobufUnsafe() throws Throwable {
    return unsafeProcessor.decodeUtf8(encodedText, 0, encodedText.length);
  }

  @Benchmark
  public String protobufPlainJava() throws Throwable {
    return decodeUtf8PlainJava(encodedText, 0, encodedText.length);
  }

  private static String decodeUtf8PlainJava(byte[] bytes, int index, int size)
    throws InvalidProtocolBufferException {
    try {
      String s = new String(bytes, index, size, Internal.UTF_8);

      // "\uFFFD" is UTF-8 default replacement string, which illegal byte sequences get replaced with.
      if (!s.contains("\uFFFD")) {
        return s;
      }

      // Since s contains "\uFFFD" there are 2 options:
      // 1) The byte array slice is invalid UTF-8.
      // 2) The byte array slice is valid UTF-8 and contains encodings for "\uFFFD".
      // To rule out (1), we encode s and compare it to the byte array slice.
      // If the byte array slice was invalid UTF-8, then we would get a different sequence of bytes.
      if (Arrays.equals(s.getBytes(Internal.UTF_8), Arrays.copyOfRange(bytes, index, index + size))) {
        return s;
      }

      throw InvalidProtocolBufferException.invalidUtf8();
    } catch (IndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException(
        String.format("buffer length=%d, index=%d, size=%d", bytes.length, index, size));
    }
  }
}
