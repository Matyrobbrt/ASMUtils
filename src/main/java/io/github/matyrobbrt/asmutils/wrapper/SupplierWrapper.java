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

package io.github.matyrobbrt.asmutils.wrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.github.matyrobbrt.asmutils.Descriptors.OBJECT_DESCRIPTOR;
import static io.github.matyrobbrt.asmutils.Descriptors.OBJECT_NAME;
import static io.github.matyrobbrt.asmutils.Descriptors.SUPPLIER_NAME;
import static org.objectweb.asm.Opcodes.*;

import io.github.matyrobbrt.asmutils.ASMGeneratedType;
import io.github.matyrobbrt.asmutils.ASMUtilsClassLoader;
import io.github.matyrobbrt.asmutils.ClassNameGenerator;
import io.github.matyrobbrt.asmutils.JavaGetter;
import io.github.matyrobbrt.asmutils.LambdaUtils;

/**
 * A wrapper for <i>public</i> methods with no parameters and a return type, or
 * fields. <br>
 * An ASM wrapper is created for that method / field, in order to allow its fast
 * invocation, respectively access. <br>
 * The generated is always {@code final}, and implements
 * {@link java.util.function.Supplier} of the type {@code T}, but based on what
 * it is wrapping, and the access modifier of the wrapped member: <br>
 * <table>
 * <tr>
 * <th>Static</th>
 * <th>Method</th>
 * <th>Field</th>
 * </tr>
 * <tr>
 * <td>Yes</td>
 * <td>The generated class has a constructor with no parameters, and its
 * {@code get} method calls the wrapped method directly.</td>
 * <td>The generated class has a constructor with no parameters, and its
 * {@code get} method gets the wrapped field directly.</td>
 * </tr>
 * <tr>
 * <td>No</td>
 * <td>The generated class holds an instance of the declaring class of the
 * wrapped method, and has a constructor with that instance as the parameter.
 * Its {@code get} method calls the wrapped method through that instance.</td>
 * <td>The generated class holds an instance of the declaring class of the
 * wrapped method, and has a constructor with that instance as the parameter.
 * Its {@code get} method gets the wrapped field from that instance.</td>
 * </tr>
 * </table>
 *
 * @param <T> the return type of the wrapped method / the type of the wrapped
 *            field
 */
public interface SupplierWrapper<T> extends Wrapper, Supplier<T> {

	/**
	 * The type of {@link SupplierWrapper}.
	 */
	Type TYPE = Type.getType(SupplierWrapper.class);

	/**
	 * Creates a supplier wrapper for the field.
	 * 
	 * @param  <T>                      the type of the field
	 * @param  field                    the field to wrap
	 * @return                          the wrapper
	 * @throws IllegalArgumentException if the field is not public, or its declaring
	 *                                  class is not public
	 */
	@SuppressWarnings("unchecked")
	static <T> SupplierWrapper<T> wrapField(final Field field) {
		return (SupplierWrapper<T>) PrivateUtils.CACHE.computeIfAbsent(field, $ -> {
			final var isStatic = Modifier.isStatic(field.getModifiers());
			if (!Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
				throw new IllegalArgumentException(
						"Cannot create a supplier wrapper for field \"%s\", as its declaring class is not public!"
								.formatted(field));
			}
			if (!Modifier.isPublic(field.getModifiers())) {
				throw new IllegalArgumentException(
						"Cannot create a supplier wrapper for field \"%s\", as it is not public!".formatted(field));
			}
			final var fieldType = field.getType();
			final var fieldDescriptor = Type.getDescriptor(fieldType);
			final var owner = field.getDeclaringClass();
			final var ownerDescriptor = Type.getDescriptor(owner);
			final var ownerName = Type.getInternalName(owner);
			final var generatedName = PrivateUtils.generateName(field);
			final var generatedNameInternal = generatedName.replace('.', '/');
			final var generatedNameDescriptor = "L" + generatedNameInternal + ";";
			final var getMethodDescriptor = "()%s".formatted(fieldDescriptor);

			PrivateUtils.LOGGER.debug("Generating SupplierWrapper for field \"{}\"...", field);

			ClassWriter cw = new ClassWriter(0);
			FieldVisitor fv;
			MethodVisitor mv;

			cw.visit(JavaGetter.getJavaVersionAsOpcode(8, "SupplierWrapper"),
					ACC_PUBLIC | ACC_FINAL | ACC_SUPER, generatedNameInternal,
					"%sL%s<%s>;".formatted(OBJECT_DESCRIPTOR, SUPPLIER_NAME, fieldDescriptor), OBJECT_NAME,
					new String[] {
							SUPPLIER_NAME
			});

			cw.visitSource(".dynamic", null);
			ASMGeneratedType.ANNOTATION_ADDER.accept(cw, TYPE);

			if (!isStatic) {
				fv = cw.visitField(ACC_PRIVATE | ACC_FINAL, "instance", ownerDescriptor, null, null);
				fv.visitEnd();
			}

			{
				mv = cw.visitMethod(ACC_PUBLIC, "<init>",
						"(%s)V".formatted(isStatic ? "" : ownerDescriptor), null, null);
				mv.visitCode();
				Label label0 = new Label();
				mv.visitLabel(label0);
				mv.visitLineNumber(isStatic ? 5 : 9, label0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESPECIAL, OBJECT_NAME, "<init>", "()V", false);
				if (isStatic) {
					mv.visitInsn(RETURN);
					Label label1 = new Label();
					mv.visitLabel(label1);
					mv.visitLocalVariable("this", generatedNameDescriptor, null, label0, label1, 0);
				} else {
					Label label1 = new Label();
					mv.visitLabel(label1);
					mv.visitLineNumber(10, label1);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitVarInsn(ALOAD, 1);
					mv.visitFieldInsn(PUTFIELD, generatedNameInternal, "instance", ownerDescriptor);
					Label label2 = new Label();
					mv.visitLabel(label2);
					mv.visitLineNumber(11, label2);
					mv.visitInsn(RETURN);
					Label label3 = new Label();
					mv.visitLabel(label3);
					mv.visitLocalVariable("this", generatedNameDescriptor, null, label0, label3, 0);
					mv.visitLocalVariable("instance", ownerDescriptor, null, label0, label3, 1);
				}
				mv.visitMaxs(isStatic ? 1 : 2, isStatic ? 1 : 2);
				mv.visitEnd();
			}
			{
				mv = cw.visitMethod(ACC_PUBLIC, "get", getMethodDescriptor, null, null);
				mv.visitCode();
				Label label0 = new Label();
				Label label1 = new Label();
				mv.visitLabel(label0);
				mv.visitLineNumber(isStatic ? 9 : 15, label0);
				if (isStatic) {
					mv.visitLineNumber(9, label0);
					mv.visitFieldInsn(GETSTATIC, ownerName, field.getName(), fieldDescriptor);
					mv.visitInsn(ARETURN);
					mv.visitLabel(label1);
				} else {
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, generatedNameInternal, "instance", ownerDescriptor);
					mv.visitFieldInsn(GETFIELD, ownerName, field.getName(), fieldDescriptor);
					mv.visitInsn(ARETURN);
					mv.visitLabel(label1);
				}
				mv.visitLocalVariable("this", generatedNameDescriptor, null, label0, label1, 0);
				mv.visitMaxs(1, 1);
				mv.visitEnd();
			}
			{
				mv = cw.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "get",
						"()Ljava/lang/Object;", null, null);
				mv.visitCode();
				Label label0 = new Label();
				mv.visitLabel(label0);
				mv.visitLineNumber(1, label0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKEVIRTUAL, generatedNameInternal, "get", getMethodDescriptor, false);
				mv.visitInsn(ARETURN);
				mv.visitMaxs(1, 1);
				mv.visitEnd();
			}
			cw.visitEnd();
			final var bytes = cw.toByteArray();
			PrivateUtils.LOGGER.debug("Finished generating SupplierWrapper for field \"{}\"", field);
			return new SupplierWrapper<T>() {

				final Supplier<T> staticSupplier = isStatic
						? (Supplier<T>) LambdaUtils
								.rethrowSupplier(() -> getGeneratedClass().getDeclaredConstructor().newInstance()).get()
						: null;
				final ConstructorWrapper<?> constructorWrapper = isStatic ? null
						: LambdaUtils.rethrowSupplier(
								() -> ConstructorWrapper.wrap(getGeneratedClass().getConstructor(owner))).get();

				@Override
				public @NotNull byte[] getClassBytes() {
					return bytes;
				}

				@Override
				public @NotNull Class<?> getGeneratedClass() {
					return ASMUtilsClassLoader.INSTANCE.define(generatedName, bytes);
				}

				@Override
				public T get() {
					if (isStatic) {
						return staticSupplier.get();
					} else {
						throw new UnsupportedOperationException(
								"The field wrapped by this wrapper (\"%s\") is not static! Use `onTarget` in order to get a supplier invoking the wrapped field on an instance."
										.formatted(field));
					}
				}

				@Override
				public @NotNull Supplier<T> onTarget(@NotNull Object target) {
					if (isStatic) {
						throw new UnsupportedOperationException(
								"The field wrapped by this wrapper (\"%s\") is static! Use `get` instead."
										.formatted(field));
					}
					if (!owner.isAssignableFrom(target.getClass())) {
						throw new ClassCastException(
								"Object \"%s\" cannot be cast to the declaring class of the wrapped field \"%s\""
										.formatted(target, owner));
					}
					return (Supplier<T>) constructorWrapper.invoke(target);
				}

				@Override
				public boolean isStatic() {
					return isStatic;
				}

				@Override
				public boolean wrapsField() {
					return true;
				}

				@Override
				public @NotNull Class<T> getResultType() {
					return (@NotNull Class<T>) fieldType;
				}

			};
		});
	}

	/**
	 * Creates a {@link SupplierWrapper} for a method.
	 * 
	 * @param  <T>                      the return type of the method
	 * @param  method                   the method to wrap
	 * @return                          the wrapper
	 * @throws IllegalArgumentException if the wrapped method meets one of the
	 *                                  following conditions:
	 *                                  <ul>
	 *                                  <li>the method is not public</li>
	 *                                  <li>the declaring class of the method is not
	 *                                  public</li>
	 *                                  <li>the method returns void (doesn't have a
	 *                                  return type)</li>
	 *                                  <li>the method has parameters</i>
	 *                                  </ul>
	 */
	@SuppressWarnings("unchecked")
	static <T> SupplierWrapper<T> wrapMethod(final Method method) {
		return (SupplierWrapper<T>) PrivateUtils.CACHE.computeIfAbsent(method, $ -> {
			final var isStatic = Modifier.isStatic(method.getModifiers());
			if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
				throw new IllegalArgumentException(
						"Cannot create a supplier wrapper for method \"%s\", as its declaring class is not public!"
								.formatted(method));
			}
			if (method.getParameterTypes().length != 0) {
				throw new IllegalArgumentException(
						"Cannot create a supplier wrapper for method \"%s\", as it has parameters!".formatted(method));
			}
			if (!Modifier.isPublic(method.getModifiers())) {
				throw new IllegalArgumentException(
						"Cannot create a supplier wrapper for method \"%s\", as it is not public!".formatted(method));
			}
			if (method.getReturnType() == void.class) {
				throw new IllegalArgumentException(
						"Cannot create a supplier wrapper for method \"%s\", as its return type is void!"
								.formatted(method));
			}
			final var returnType = method.getReturnType();
			final var returnDescriptor = Type.getDescriptor(returnType);
			final var owner = method.getDeclaringClass();
			final var ownerDescriptor = Type.getDescriptor(owner);
			final var ownerName = Type.getInternalName(owner);
			final var generatedName = PrivateUtils.generateName(method);
			final var generatedNameInternal = generatedName.replace('.', '/');
			final var generatedNameDescriptor = "L" + generatedNameInternal + ";";
			final var getMethodDescriptor = "()%s".formatted(returnDescriptor);

			PrivateUtils.LOGGER.debug("Generating SupplierWrapper for method \"{}\"...", method);

			ClassWriter cw = new ClassWriter(0);
			FieldVisitor fv;
			MethodVisitor mv;

			cw.visit(JavaGetter.getJavaVersionAsOpcode(8, "SupplierWrapper"),
					ACC_PUBLIC | ACC_SUPER | ACC_FINAL, generatedNameInternal,
					"Ljava/lang/Object;L%s<%s>;".formatted(SUPPLIER_NAME, returnDescriptor), OBJECT_NAME, new String[] {
							SUPPLIER_NAME
			});

			cw.visitSource(".dynamic", null);

			ASMGeneratedType.ANNOTATION_ADDER.accept(cw, TYPE);

			if (!isStatic) {
				fv = cw.visitField(ACC_PRIVATE | ACC_FINAL, "instance", ownerDescriptor, null, null);
				fv.visitEnd();
			}

			{
				mv = cw.visitMethod(ACC_PUBLIC, "<init>",
						"(%s)V".formatted(isStatic ? "" : ownerDescriptor), null, null);
				mv.visitCode();
				Label label0 = new Label();
				mv.visitLabel(label0);
				mv.visitLineNumber(isStatic ? 3 : 7, label0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESPECIAL, OBJECT_NAME, "<init>", "()V", false);
				if (isStatic) {
					mv.visitInsn(RETURN);
					Label label1 = new Label();
					mv.visitLabel(label1);
					mv.visitLocalVariable("this", generatedNameDescriptor, null, label0, label1, 0);
				} else {
					Label label1 = new Label();
					mv.visitLabel(label1);
					mv.visitLineNumber(8, label1);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitVarInsn(ALOAD, 1);
					mv.visitFieldInsn(PUTFIELD, generatedNameInternal, "instance", ownerDescriptor);
					Label label2 = new Label();
					mv.visitLabel(label2);
					mv.visitLineNumber(9, label2);
					mv.visitInsn(RETURN);
					Label label3 = new Label();
					mv.visitLabel(label3);
					mv.visitLocalVariable("this", generatedNameDescriptor, null, label0, label3, 0);
					mv.visitLocalVariable("instance", ownerDescriptor, null, label0, label3, 1);
				}
				mv.visitMaxs(isStatic ? 1 : 2, isStatic ? 1 : 2);
				mv.visitEnd();
			}
			{
				mv = cw.visitMethod(ACC_PUBLIC, "get", getMethodDescriptor, null, null);
				mv.visitCode();
				Label label0 = new Label();
				mv.visitLabel(label0);
				mv.visitLineNumber(isStatic ? 7 : 13, label0);

				if (isStatic) {
					mv.visitMethodInsn(INVOKESTATIC, ownerName, method.getName(),
							Type.getMethodDescriptor(method), false);
				} else {
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, generatedNameInternal, "instance", ownerDescriptor);
					mv.visitMethodInsn(INVOKEVIRTUAL, ownerName, method.getName(), getMethodDescriptor,
							false);
				}
				mv.visitInsn(ARETURN);
				Label label1 = new Label();
				mv.visitLabel(label1);
				mv.visitLocalVariable("this", generatedNameDescriptor, null, label0, label1, 0);
				mv.visitMaxs(isStatic ? 1 : 2, 1);
				mv.visitEnd();
			}
			{
				// Generate the synthetic `get`
				mv = cw.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "get",
						"()%s".formatted(OBJECT_DESCRIPTOR), null, null);
				mv.visitCode();
				Label label0 = new Label();
				mv.visitLabel(label0);
				mv.visitLineNumber(1, label0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKEVIRTUAL, generatedNameInternal, "get", getMethodDescriptor, false);
				mv.visitInsn(ARETURN);
				mv.visitMaxs(1, 1);
				mv.visitEnd();
			}
			cw.visitEnd();
			final var bytes = cw.toByteArray();
			PrivateUtils.LOGGER.debug("Finished generating SupplierWrapper for method \"{}\"", method);
			return new SupplierWrapper<T>() {

				final Supplier<T> staticSupplier = isStatic
						? (Supplier<T>) LambdaUtils
								.rethrowSupplier(() -> getGeneratedClass().getDeclaredConstructor().newInstance()).get()
						: null;
				final ConstructorWrapper<?> constructorWrapper = isStatic ? null
						: LambdaUtils.rethrowSupplier(
								() -> ConstructorWrapper.wrap(getGeneratedClass().getConstructor(owner))).get();

				@Override
				public @NotNull byte[] getClassBytes() {
					return bytes;
				}

				@Override
				public @NotNull Class<?> getGeneratedClass() {
					return ASMUtilsClassLoader.INSTANCE.define(generatedName, bytes);
				}

				@Override
				public T get() {
					if (isStatic) {
						return staticSupplier.get();
					} else {
						throw new UnsupportedOperationException(
								"The method wrapped by this wrapper (\"%s\") is not static! Use `onTarget` in order to get a supplier invoking the wrapped method on an instance."
										.formatted(method));
					}
				}

				@Override
				public @NotNull Supplier<T> onTarget(@NotNull Object target) {
					if (isStatic) {
						throw new UnsupportedOperationException(
								"The method wrapped by this wrapper (\"%s\") is static! Use `get` instead."
										.formatted(method));
					}
					if (!owner.isAssignableFrom(target.getClass())) {
						throw new ClassCastException(
								"Object \"%s\" cannot be cast to the declaring class of the wrapped method \"%s\""
										.formatted(target, owner));
					}
					return (Supplier<T>) constructorWrapper.invoke(target);
				}

				@Override
				public boolean isStatic() {
					return isStatic;
				}

				@Override
				public boolean wrapsField() {
					return false;
				}

				@Override
				public @NotNull Class<T> getResultType() {
					return (@NotNull Class<T>) returnType;
				}

			};
		});
	}

	/**
	 * Only if the wrapped member is <strong>static</strong>. <br>
	 * If a field is wrapped, returns it's value, otherwise returns the result of
	 * the invocation of the wrapped method.
	 * 
	 * @return                               the result
	 * @throws UnsupportedOperationException if the wrapped member is <i>not</i>
	 *                                       static
	 */
	@Override
	T get();

	/**
	 * If the element wrapped is <strong>not</strong> static, returns a
	 * {@link java.util.function.Supplier} that, if wrapping a field, returns the
	 * value of the field in that {@code target}, otherwise the value returned by
	 * calling the wrapped method on the {@code target}.
	 * 
	 * @param  target                        the target
	 * @throws UnsupportedOperationException if the element wrapped is
	 *                                       <strong>static</strong>
	 * @throws ClassCastException            if the {@code target} cannot be cast to
	 *                                       the declaring class of the wrapped
	 *                                       element
	 */
	@NotNull
	Supplier<T> onTarget(@NotNull Object target);

	/**
	 * Checks if the member wrapped by this wrapper is static.
	 * 
	 * @return if the member wrapped by this wrapper is static
	 */
	boolean isStatic();

	/**
	 * Checks if this wrapper wraps a field. {@code False} means that a method is
	 * wrapped.
	 * 
	 * @return if this wrapper wraps a field
	 */
	boolean wrapsField();

	/**
	 * If a field is wrapped, returns its type, otherwise returns the return type of
	 * the wrapped method.
	 * 
	 * @return the return type
	 */
	@NotNull
	Class<T> getResultType();

	//@formatter:off
	class PrivateUtils {

		private static final Logger LOGGER = LoggerFactory.getLogger(SupplierWrapper.class);
		private static final Map<Member, SupplierWrapper<?>> CACHE = new HashMap<>();
		private static final AtomicInteger CREATED_WRAPPERS = new AtomicInteger();

		private static String generateName(Method method) {
			final var isStatic = Modifier.isStatic(method.getModifiers());
			return ClassNameGenerator.resolveOnBasePackage("%s$%s$m_%s_%s$%s_%s".formatted(
					SupplierWrapper.class.getSimpleName(),
					CREATED_WRAPPERS.getAndIncrement(), 
					method.getDeclaringClass().getSimpleName(),
					method.getName(), 
					isStatic ? 1 : 0, 
					method.getReturnType().getSimpleName()));
		}
		private static String generateName(Field field) {
			final var isStatic = Modifier.isStatic(field.getModifiers());
			return ClassNameGenerator.resolveOnBasePackage("%s$%s$f_%s_%s$%s_%s".formatted(
					SupplierWrapper.class.getSimpleName(),
					CREATED_WRAPPERS.getAndIncrement(), 
					field.getDeclaringClass().getSimpleName(),
					field.getName(), 
					isStatic ? 1 : 0, 
					field.getType().getSimpleName()));
		}
	}
}
