/*
 * This file is part of the ASMUtils library and is licensed under the MIT
 * license:
 *
 * MIT License
 *
 * Copyright (c) 2022 Matyrobbrt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.matyrobbrt.asmutils;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public final class JavaGetter {

    public static int getJavaVersionAsOpcode(int minVersion, String featureName) {
        final int version = getJavaVersion();
        if (minVersion > version) {
            throw new UnsupportedOperationException(
                    String.format("The feature %s requires Java %s or greater! Currently, the Java version is %s",
                            featureName, minVersion, version));
        }
        return versionToOpcode(version);
    }

    public static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }

    private static final Map<Integer, Integer> TO_OP_CODE;
    static {
        final HashMap<Integer, Integer> map = new HashMap<>();
        map.put(1, V1_1);
        map.put(2, V1_2);
        map.put(3, V1_3);
        map.put(4, V1_4);
        map.put(5, V1_5);
        map.put(6, V1_6);
        map.put(7, V1_7);
        map.put(8, V1_8);
        map.put(9, V9);
        map.put(10, V10);
        map.put(11, V11);
        map.put(12, V12);
        map.put(13, V13);
        map.put(14, V14);
        map.put(15, V15);
        map.put(16, V16);
        map.put(17, V17);
        map.put(18, V18);
        TO_OP_CODE = map;
    }

    public static int versionToOpcode(int version) {
        return TO_OP_CODE.get(version);
    }
}
