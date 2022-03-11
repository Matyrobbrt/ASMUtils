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
import java.util.function.Supplier;

public class ASMUtilsClassLoader extends ClassLoader {

	public static final ASMUtilsClassLoader INSTANCE = new ASMUtilsClassLoader();

	private final Map<String, Class<?>> cache = new HashMap<>();
	private ASMUtilsClassLoader() {
		super(null);
	}

	@Override
	protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
		return Class.forName(name, resolve, Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Defines a class.
	 * 
	 * @param  name the name of the class
	 * @param  data a supplier the data of the class
	 * @return
	 */
	public Class<?> define(String name, Supplier<byte[]> data) {
		return cache.computeIfAbsent(name, $ -> {
			final var d = data.get();
			return defineClass(name, d, 0, d.length);
		});
	}

	/**
	 * Defines a class.
	 * 
	 * @param  name the name of the class
	 * @param  data the data of the class
	 * @return
	 */
	public Class<?> define(String name, byte[] data) {
		return define(name, () -> data);
	}

}
