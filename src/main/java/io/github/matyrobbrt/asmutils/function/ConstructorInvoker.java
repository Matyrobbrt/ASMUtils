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

package io.github.matyrobbrt.asmutils.function;

import org.objectweb.asm.Type;

/**
 * A functional interface which invokes a constructor.
 * 
 * @param <T> the "return" type of the constructor (its declaring class)
 */
@FunctionalInterface
public interface ConstructorInvoker<T> {

	/**
	 * The descriptor of {@link ConstructorInvoker}.
	 */
	String DESCRIPTOR = Type.getDescriptor(ConstructorInvoker.class);
	/**
	 * The internal name of {@link ConstructorInvoker}
	 */
	String INTERNAL_NAME = Type.getInternalName(ConstructorInvoker.class);

	/**
	 * Invokes the constructor with the specified {@code args} and return the
	 * constructed object.
	 * 
	 * @param  args the args to use for invoking the constructor
	 * @return      the constructed object
	 */
	T invoke(Object... args);

}
