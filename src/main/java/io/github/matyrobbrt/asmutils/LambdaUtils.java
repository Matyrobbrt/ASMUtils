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

import java.util.function.Supplier;

public class LambdaUtils {

	/**
	 * Sneakily throws the given exception, bypassing compile-time checks for
	 * checked exceptions.
	 *
	 * <p>
	 * <strong>This method will never return normally.</strong> The exception passed
	 * to the method is always rethrown.
	 * </p>
	 *
	 * @param ex the exception to sneakily rethrow
	 */
	@SuppressWarnings("unchecked")
	public static <E extends Throwable> void sneakyThrow(Throwable ex) throws E {
		throw (E) ex;
	}

	/**
	 * Converts the given exception supplier into a regular supplier, rethrowing any
	 * exception as needed.
	 *
	 * @param  supplier the exception supplier to convert
	 * @param  <T>      the type of results supplied by this supplier
	 * @return          a supplier which returns the result from the given throwing
	 *                  supplier, rethrowing any exceptions
	 * @see             #sneakyThrow(Throwable)
	 */
	public static <T> Supplier<T> rethrowSupplier(final ThrowingSupplier<T> supplier) {
		return supplier;
	}

	@FunctionalInterface
	public interface ThrowingSupplier<T> extends Supplier<T> {

		T getThrowing() throws Throwable;

		@Override
		default T get() {
			try {
				return getThrowing();
			} catch (Throwable e) {
				sneakyThrow(e);
				return null; // Should never be reached
			}
		}
	}
}