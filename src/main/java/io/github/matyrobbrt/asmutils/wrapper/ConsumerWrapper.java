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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.objectweb.asm.Opcodes.*;

import io.github.matyrobbrt.asmutils.ASMGeneratedType;
import io.github.matyrobbrt.asmutils.ASMUtilsClassLoader;
import io.github.matyrobbrt.asmutils.ClassNameGenerator;
import io.github.matyrobbrt.asmutils.Descriptors;
import io.github.matyrobbrt.asmutils.JavaGetter;
import io.github.matyrobbrt.asmutils.LambdaUtils;

/**
 * A wrapper for <i>public</i> methods with no return type and
 * <strong>one</strong> parameter. <br>
 * An ASM wrapper is created for that method, in order to allow fast invoking of
 * it. <br>
 * Based on if the wrapped method is static, or not, the ASM-generated class
 * works differently:
 * <dl>
 * <dt>Static Method</dt>
 * <dd>The generated class has a constructor with no parameters, and its
 * {@code accept} method calls the wrapped method directly.</dd>
 * <dt>Instance (non-static) Method</dt>
 * <dd>The generated class holds an instance of the declaring class of the
 * wrapped method, and has a constructor with that instance as the parameter.
 * Its {@code accept} method calls the wrapped method through that
 * instance.</dd>
 * </dl>
 * The generated class is always {@code final} and implements
 * {@link java.util.function.Consumer} of the type {@code T}.
 * 
 * @param <T> the type of the input to the operation
 */
public interface ConsumerWrapper<T> extends Wrapper, Consumer<T> {

	/**
	 * The {@link ConsumerWrapper} type.
	 */
	Type TYPE = Type.getType(ConsumerWrapper.class);

	/**
	 * A variant of {@link #wrap(Method)}, which gets the method with the name
	 * {@code methodName} and the parameter {@code parameterType} from the
	 * {@code declaringClass}.
	 * 
	 * @param  <T>                   the type of the parameter of the method
	 * @param  declaringClass        the class containing the method to wrap
	 * @param  methodName            the name of the method to wrap
	 * @param  parameterType         the parameter type of the method to wrap
	 * @return                       the wrapper
	 * @throws NoSuchMethodException if a matching method is not found.
	 * @throws SecurityException     if
	 *                               {@link java.lang.Class#getDeclaredMethod(String, Class...)}
	 *                               throws a {@link SecurityException}
	 * @see                          #wrap(Method)
	 */
	static <T> ConsumerWrapper<T> wrap(@NotNull Class<?> declaringClass, @NotNull String methodName,
			@NotNull Class<T> parameterType) throws NoSuchMethodException, SecurityException {
		return wrap(declaringClass.getDeclaredMethod(methodName, parameterType));
	}

	/**
	 * Creates a wrapper for the {@code method}.
	 * 
	 * @param  <T>                      the type of the parameter of the method
	 * @param  method                   the method to wrap
	 * @return                          the wrapper
	 * @throws IllegalArgumentException if the wrapped method meets one of the
	 *                                  following conditions:
	 *                                  <ul>
	 *                                  <li>the method doesn't have only one
	 *                                  parameter</li>
	 *                                  <li>the declaring class of the method is not
	 *                                  public</li>
	 *                                  <li>the method is not public</li>
	 *                                  <li>the method has a return type (doesn't
	 *                                  return void)</li>
	 *                                  </ul>
	 */
	@SuppressWarnings("unchecked")
	static <T> ConsumerWrapper<T> wrap(@NotNull Method method) {
		return (ConsumerWrapper<T>) PrivateUtils.CACHE.computeIfAbsent(method, $ -> {
			final var isStatic = Modifier.isStatic(method.getModifiers());
			if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
				throw new IllegalArgumentException(
						"Cannot create a consumer wrapper for method \"%s\", as its declaring class is not public!"
								.formatted(method));
			}
			if (method.getParameterTypes().length != 1) {
				throw new IllegalArgumentException(
						"Cannot create a consumer wrapper for method \"%s\", as it doesn't have only one parameter!"
								.formatted(method));
			}
			if (!Modifier.isPublic(method.getModifiers())) {
				throw new IllegalArgumentException(
						"Cannot create a consumer wrapper for method \"%s\", as it is not public!".formatted(method));
			}
			if (method.getReturnType() != void.class) {
				throw new IllegalArgumentException(
						"Cannot create a consumer wrapper for method \"%s\", as its return type is not void!"
								.formatted(method));
			}
			final var parameterType = method.getParameterTypes()[0];
			final var inputDescriptor = Type.getDescriptor(parameterType);
			final var inputName = Type.getInternalName(parameterType);
			final var owner = method.getDeclaringClass();
			final var ownerDescriptor = Type.getDescriptor(owner);
			final var ownerName = Type.getInternalName(owner);
			final var generatedName = PrivateUtils.generateName(method);
			final var generatedNameInternal = generatedName.replace('.', '/');
			final var generatedNameDescriptor = "L" + generatedNameInternal + ";";
			final var consumerMethodDescriptor = "(%s)V".formatted(inputDescriptor);

			PrivateUtils.LOGGER.debug("Generating ConsumerWrapper for method \"{}\"...", method);

			ClassWriter cw = new ClassWriter(0);
			FieldVisitor fieldVisitor;
			MethodVisitor mv;

			cw.visit(JavaGetter.getJavaVersionAsOpcode(8, "ConsumerWrapper"), ACC_PUBLIC | ACC_SUPER | ACC_FINAL,
					generatedNameInternal,
					"Ljava/lang/Object;Ljava/util/function/Consumer<%s>;".formatted(inputDescriptor),
					Descriptors.OBJECT_NAME, new String[] {
							"java/util/function/Consumer"
			});

			cw.visitSource(".dynamic", null);

			ASMGeneratedType.ANNOTATION_ADDER.accept(cw, TYPE);

			if (!isStatic) {
				fieldVisitor = cw.visitField(ACC_PRIVATE | ACC_FINAL, "instance", ownerDescriptor, null, null);
				fieldVisitor.visitEnd();
			}
			{
				mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(%s)V".formatted(isStatic ? "" : ownerDescriptor), null,
						null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitLineNumber(9, l0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESPECIAL, Descriptors.OBJECT_NAME, "<init>", "()V", false);
				if (!isStatic) {
					Label l1 = new Label();
					mv.visitLabel(l1);
					mv.visitLineNumber(10, l1);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitVarInsn(ALOAD, 1);
					mv.visitFieldInsn(PUTFIELD, generatedNameInternal, "instance", ownerDescriptor);
				}
				Label l2 = new Label();
				mv.visitLabel(l2);
				mv.visitLineNumber(11, l2);
				mv.visitInsn(RETURN);
				if (!isStatic) {
					Label l3 = new Label();
					mv.visitLabel(l3);
					mv.visitLocalVariable("this", generatedNameDescriptor, null, l0, l3, 0);
					mv.visitLocalVariable("instance", ownerDescriptor, null, l0, l3, 1);
				}
				mv.visitMaxs(isStatic ? 1 : 2, 2);
				mv.visitEnd();
			}
			{
				mv = cw.visitMethod(ACC_PUBLIC, "accept", consumerMethodDescriptor, null, null);
				mv.visitCode();
				Label label0 = new Label();
				if (isStatic) {
					mv.visitLabel(label0);
					mv.visitLineNumber(9, label0);
					mv.visitVarInsn(ALOAD, 1);
					mv.visitMethodInsn(INVOKESTATIC, ownerName, method.getName(), Type.getMethodDescriptor(method),
							false);
					Label l1 = new Label();
					mv.visitLabel(l1);
					mv.visitLineNumber(10, l1);
					mv.visitInsn(RETURN);
					Label l2 = new Label();
					mv.visitLabel(l2);
					mv.visitLocalVariable("this", generatedNameDescriptor, null, label0, l2, 0);
					mv.visitLocalVariable("t", inputDescriptor, null, label0, l2, 1);
				} else {
					mv.visitLabel(label0);
					mv.visitLineNumber(15, label0);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, generatedNameInternal, "instance", ownerDescriptor);
					mv.visitVarInsn(ALOAD, 1);
					mv.visitMethodInsn(INVOKEVIRTUAL, ownerName, method.getName(), Type.getMethodDescriptor(method),
							false);
					Label l1 = new Label();
					mv.visitLabel(l1);
					mv.visitLineNumber(16, l1);
					mv.visitInsn(RETURN);
					Label l2 = new Label();
					mv.visitLabel(l2);
					mv.visitLocalVariable("this", generatedNameDescriptor, null, label0, l2, 0);
					mv.visitLocalVariable("t", inputDescriptor, null, label0, l2, 1);
				}
				mv.visitMaxs(isStatic ? 1 : 2, 2);
				mv.visitEnd();
			}
			{
				mv = cw.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "accept", "(Ljava/lang/Object;)V", null,
						null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitLineNumber(1, l0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitTypeInsn(CHECKCAST, inputName);
				mv.visitMethodInsn(INVOKEVIRTUAL, generatedNameInternal, "accept", consumerMethodDescriptor, false);
				mv.visitInsn(RETURN);
				mv.visitMaxs(2, 2);
				mv.visitEnd();
			}
			cw.visitEnd();
			final var bytes = cw.toByteArray();
			PrivateUtils.LOGGER.debug("Finished generating ConsumerWrapper for method \"{}\"", method);
			return new ConsumerWrapper<T>() {

				@SuppressWarnings("rawtypes")
				final Consumer staticInvoker = LambdaUtils.rethrowSupplier(
						() -> isStatic ? (Consumer) getGeneratedClass().getDeclaredConstructor().newInstance() : null)
						.get();
				final ConstructorWrapper<?> constructorWrapper = isStatic ? null
						: LambdaUtils.rethrowSupplier(
								() -> ConstructorWrapper.wrap(getGeneratedClass().getConstructor(owner))).get();

				@Override
				public byte[] getClassBytes() {
					return bytes;
				}

				@Override
				public Class<?> getGeneratedClass() {
					return ASMUtilsClassLoader.INSTANCE.define(generatedName, bytes);
				}

				@Override
				public Consumer<T> onTarget(Object target) {
					if (isStatic) {
						throw new UnsupportedOperationException(
								"The method wrapped by this wrapper (\"%s\") is static! Use `accept` instead."
										.formatted(method));
					}
					if (!owner.isAssignableFrom(target.getClass())) {
						throw new ClassCastException(
								"Object \"%s\" cannot be cast to the declaring class of the wrapped method \"%s\""
										.formatted(target, owner));
					}
					return (Consumer<T>) constructorWrapper.invoke(target);
				}

				@Override
				public void accept(T t) {
					if (isStatic) {
						staticInvoker.accept(t);
					} else {
						throw new UnsupportedOperationException(
								"The method wrapped by this wrapper (\"%s\") is not static! Use `onTarget` in order to get a consumer invoking the wrapped method on an instance."
										.formatted(method));
					}
				}

				@Override
				public boolean isStatic() {
					return isStatic;
				}

				@Override
				public Class<T> getInputType() {
					return (Class<T>) parameterType;
				}

			};
		});
	}

	/**
	 * If the method wrapped by this wrapper is <strong>not</strong> static, returns
	 * a {@link java.util.function.Consumer} that accepts the wrapped method on the
	 * {@code target}.
	 * 
	 * @param  target                        the target that the returned consumer
	 *                                       accepts the wrapped method on
	 * @return                               the consumer
	 * @throws UnsupportedOperationException if the method wrapped by this wrapper
	 *                                       is <strong>static</strong>
	 * @throws ClassCastException            if the {@code target} cannot be cast to
	 *                                       the declaring class of the wrapped
	 *                                       method
	 */
	@NotNull
	Consumer<T> onTarget(@NotNull Object target);

	/**
	 * If the method wrapped by this wrapper is <strong>static</strong>, the method
	 * is invoked with the inputted argument.
	 * 
	 * @param  t                             the input argument
	 * @throws UnsupportedOperationException if the method wrapped by this wrapper,
	 *                                       is <strong>not</strong> static
	 */
	@Override
	void accept(T t);

	/**
	 * Checks if the method wrapped by this wrapper is static.
	 * 
	 * @return if the method wrapped by this wrapper is static
	 */
	boolean isStatic();

	/**
	 * Returns the input type of the wrapped method.
	 * 
	 * @return the input type of the wrapped method
	 */
	@NotNull
	Class<T> getInputType();

	//@formatter:off
	class PrivateUtils {
		private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerWrapper.class);
		private static final Map<Method, ConsumerWrapper<?>> CACHE = new HashMap<>();
		private static final AtomicInteger CREATED_WRAPPERS = new AtomicInteger();
		private static String generateName(Method method) {
			final var isStatic = Modifier.isStatic(method.getModifiers());
			return ClassNameGenerator.resolveOnBasePackage("%s$%s_%s_%s$%s_%s".formatted(
					ConsumerWrapper.class.getSimpleName(),
					CREATED_WRAPPERS.getAndIncrement(),
					method.getDeclaringClass().getSimpleName(),
					method.getName(),
					isStatic ? 1 : 0,
					method.getParameterTypes()[0].getSimpleName()
			));
		}
	}
}
