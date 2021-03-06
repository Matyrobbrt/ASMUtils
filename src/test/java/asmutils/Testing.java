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

package asmutils;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matyrobbrt.asmutils.wrapper.ConstructorWrapper;
import io.github.matyrobbrt.asmutils.wrapper.ConsumerWrapper;
import io.github.matyrobbrt.asmutils.wrapper.SupplierWrapper;

@SuppressWarnings({
        "static-method", "unchecked"
})
class Testing {

    @Test
    void testConstructorWrapper() throws Exception {
        final ConstructorWrapper<ConstructorWrapperTestObject> wrapper = ConstructorWrapper
                .wrap((Constructor<ConstructorWrapperTestObject>) ConstructorWrapperTestObject.class
                        .getDeclaredConstructors()[0]);
        final ConstructorWrapperTestObject thing = wrapper.invoke(null, null, "thing c", null, null, null, null,
                "thing h");
        assertThat(thing.c).isSameAs("thing c");
        assertThat(thing.h).isSameAs("thing h");
    }

    @Test
    void testSupplierWrapperOnInstance() throws Exception {
        final SupplierWrapper<String> wrapper = SupplierWrapper
                .wrapMethod(SupplierWrapperTestObject.class.getDeclaredMethod("print"));
        assertThat(wrapper.onTarget(new SupplierWrapperTestObject("yes")).get()).isSameAs("yes");
    }

    @Test
    void testSupplierWrapperStatic() throws Exception {
        final SupplierWrapper<String> wrapper = SupplierWrapper
                .wrapMethod(SupplierWrapperTestObject.class.getDeclaredMethod("hmm"));
        System.out.println(wrapper.get());
        assertThat(wrapper.get()).isSameAs("hmm");
    }

    @Test
    void testSupplierWrapperOnInstanceField() throws Exception {
        final SupplierWrapper<String> wrapper = SupplierWrapper.wrapField(TestObject.class.getDeclaredField("thing"));
        assertThat(wrapper.onTarget(new TestObject("yes")).get()).isSameAs("yes");
    }

    @Test
    void testSupplierWrapperOnStaticField() throws Exception {
        final SupplierWrapper<String> wrapper = SupplierWrapper.wrapField(TestObject.class.getDeclaredField("THING"));
        assertThat(wrapper.get()).isSameAs("ye");
    }

    @Test
    void testConsumerWrapperOnInstance() throws Exception {
        final ConsumerWrapper<AtomicInteger> wrapper = ConsumerWrapper
                .wrap(ConsumerWrapperTestObject.class.getDeclaredMethod("change", AtomicInteger.class));
        final AtomicInteger integer = new AtomicInteger();
        wrapper.onTarget(new ConsumerWrapperTestObject(12)).accept(integer);
        assertThat(integer.get()).isSameAs(12);
    }

    @Test
    void testConsumerWrapperStatic() throws Exception {
        final ConsumerWrapper<AtomicInteger> wrapper = ConsumerWrapper
                .wrap(ConsumerWrapperTestObject.class.getDeclaredMethod("hmm", AtomicInteger.class));
        final AtomicInteger integer = new AtomicInteger();
        wrapper.accept(integer);
        assertThat(integer.get()).isSameAs(12);
    }
}
