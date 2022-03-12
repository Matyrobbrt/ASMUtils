package io.github.matyrobbrt.asmutils.wrapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.github.matyrobbrt.asmutils.Descriptors.OBJECT_ARRAY_DESCRIPTOR;
import static io.github.matyrobbrt.asmutils.Descriptors.OBJECT_DESCRIPTOR;
import static io.github.matyrobbrt.asmutils.Descriptors.OBJECT_NAME;
import static org.objectweb.asm.Opcodes.*;

import io.github.matyrobbrt.asmutils.ASMGeneratedType;
import io.github.matyrobbrt.asmutils.ASMUtilsClassLoader;
import io.github.matyrobbrt.asmutils.Descriptors;
import io.github.matyrobbrt.asmutils.JavaGetter;
import io.github.matyrobbrt.asmutils.LambdaUtils;
import io.github.matyrobbrt.asmutils.function.ConstructorInvoker;

/**
 * A wrapper for <i>public</i> constructors. <br>
 * An ASM wrapper is created for that constructor in order to allow its fast
 * invocation. <br>
 * The wrapper class is final, implements
 * {@link io.github.matyrobbrt.asmutils.function.ConstructorInvoker} of the type
 * {@code T}, and has the following structure:
 * <ul>
 * <li>a private constructor</li>
 * <li>a field named "INSTANCE", whose type if the wrapper class, and which
 * contains the singleton instance of the wrapper</li>
 * <li>the `invoke` method, which creates the object using the wrapped
 * constructor, casting each argument to the needed type at that position</li>
 * </ul>
 * 
 * @param <T> the "return" type of the wrapped constructor. (the declaring class
 *            of the wrapped constructor)
 */
public interface ConstructorWrapper<T> extends Wrapper, ConstructorInvoker<T> {

	/**
	 * The {@link ConstructorWrapper} type.
	 */
	Type TYPE = Type.getType(ConstructorWrapper.class);

	/**
	 * An alternative for {@link #wrap(Constructor)}, which computes the constructor
	 * based on the {@code declaringClass} and the {@code parameterTypes}.
	 * 
	 * @param  <T>                   the "return" type of the wrapped constructor.
	 *                               (the declaring class of the wrapped
	 *                               constructor)
	 * @param  declaringClass        the declaring class of the constructor
	 * @param  parameterTypes        the parameter types of the constructor
	 * @return                       the wrapper
	 * @throws NoSuchMethodException if a matching constructor is not found.
	 * @throws SecurityException     if
	 *                               {@link java.lang.Class#getDeclaredConstructor(Class...)}
	 *                               threw this exception
	 * @see                          #wrap(Constructor)
	 */
	public static <T> ConstructorWrapper<T> wrap(Class<T> declaringClass, Class<?>... parameterTypes)
			throws NoSuchMethodException, SecurityException {
		return wrap(declaringClass.getDeclaredConstructor(parameterTypes));
	}

	/**
	 * Creates a wrapper for the {@code constructor}.
	 * 
	 * @param  <T>                      the "return" type of the wrapped
	 *                                  constructor. (the declaring class of the
	 *                                  wrapped constructor)
	 * @param  constructor              the constructor to wrap
	 * @return                          the wrapper
	 * @throws IllegalArgumentException if the wrapped method meets one of the
	 *                                  following conditions:
	 *                                  <ul>
	 *                                  <li>the constructor is not public</li>
	 *                                  <li>the declaring class of the constructor
	 *                                  is not public</li>
	 *                                  </ul>
	 */
	@SuppressWarnings("unchecked")
	public static <T> ConstructorWrapper<T> wrap(Constructor<T> constructor) {
		return (ConstructorWrapper<T>) PrivateUtils.CACHE.computeIfAbsent(constructor, $ -> {
			if (!Modifier.isPublic(constructor.getDeclaringClass().getModifiers())) {
				throw new IllegalArgumentException(
						"Cannot create constructor wrapper for constructor \"%s\", as its declaring class is not public!"
								.formatted(constructor));
			}
			if (!Modifier.isPublic(constructor.getModifiers())) {
				throw new IllegalArgumentException(
						"Cannot create constructor wrapper for constructor \"%s\", as it is not public!"
								.formatted(constructor));
			}
			final var ownerDescriptor = Type.getDescriptor(constructor.getDeclaringClass());
			final var ownerName = Type.getInternalName(constructor.getDeclaringClass());
			final var constructorDescriptor = Type.getConstructorDescriptor(constructor);
			final var parameterTypes = constructor.getParameterTypes();
			final var generatedName = PrivateUtils.generateName(constructor);
			final var generatedNameDescriptor = "L" + generatedName.replace('.', '/') + ";";
			final var invokeMethodDescriptor = "([%s)%s".formatted(Descriptors.OBJECT_DESCRIPTOR, ownerDescriptor);

			ClassWriter cw = new ClassWriter(0);
			FieldVisitor fv;
			MethodVisitor mv;

			PrivateUtils.LOGGER.debug("Generating ConstructorWrapper for constructor \"{}\"...", constructor);

			cw.visit(JavaGetter.getJavaVersionAsOpcode(8, ConstructorWrapper.class.getSimpleName()),
					ACC_PUBLIC | ACC_SUPER | ACC_FINAL, generatedName,
					"%sL%s<%s>;".formatted(OBJECT_DESCRIPTOR, ownerDescriptor, ConstructorInvoker.INTERNAL_NAME),
					OBJECT_NAME, new String[] {
							ConstructorInvoker.INTERNAL_NAME
			});

			ASMGeneratedType.ANNOTATION_ADDER.accept(cw, TYPE);
			cw.visitSource(".dynamic", null);

			{
				// Add `public static final WrapperClass INSTANCE;`
				fv = cw.visitField(ACC_PUBLIC | ACC_FINAL | ACC_STATIC, "INSTANCE", generatedNameDescriptor, null,
						null);
				fv.visitEnd();
			}

			{
				// Add
				/*
				 * static { INSTANCE = new WrapperClass(); }
				 */
				mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
				mv.visitCode();
				Label label0 = new Label();
				mv.visitLabel(label0);
				mv.visitLineNumber(7, label0);
				mv.visitTypeInsn(NEW, generatedName);
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, generatedName, "<init>", "()V", false);
				mv.visitFieldInsn(PUTSTATIC, generatedName, "INSTANCE", generatedNameDescriptor);
				mv.visitInsn(RETURN);
				mv.visitMaxs(2, 0);
				mv.visitEnd();
			}

			{
				// Add the private constructor
				mv = cw.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
				mv.visitCode();
				Label label0 = new Label();
				mv.visitLabel(label0);
				mv.visitLineNumber(5, label0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESPECIAL, OBJECT_NAME, "<init>", "()V", false);
				mv.visitInsn(RETURN);
				Label label1 = new Label();
				mv.visitLabel(label1);
				mv.visitLocalVariable("this", generatedNameDescriptor, null, label0, label1, 0);
				mv.visitMaxs(1, 1);
				mv.visitEnd();
			}

			{
				// Generate the `invoke` method
				mv = cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, "invoke", invokeMethodDescriptor, null, null);
				mv.visitCode();
				Label label0 = new Label();
				mv.visitLabel(label0);
				mv.visitLabel(label0);
				mv.visitLineNumber(9, label0);
				mv.visitTypeInsn(NEW, ownerName);
				mv.visitInsn(DUP);

				for (var index = 0; index < parameterTypes.length; index++) {
					final var paramTypeName = Type.getInternalName(parameterTypes[index]);
					mv.visitVarInsn(ALOAD, 1);

					if (index > 5) {
						mv.visitIntInsn(BIPUSH, index);
					} else {
						mv.visitInsn(index + 3); // Equivalent to something like ICONST_index
					}

					mv.visitInsn(AALOAD);
					mv.visitTypeInsn(CHECKCAST, paramTypeName); // cast the argument!
				}

				mv.visitMethodInsn(INVOKESPECIAL, ownerName, "<init>", constructorDescriptor, false);
				mv.visitInsn(ARETURN);
				Label label1 = new Label();
				mv.visitLabel(label1);
				mv.visitLocalVariable("this", generatedNameDescriptor, null, label0, label1, 0);
				mv.visitLocalVariable("args", OBJECT_ARRAY_DESCRIPTOR, null, label0, label1, 1);
				mv.visitMaxs(3 + parameterTypes.length, 2); // 3 with no args, incremented for each arg
				mv.visitEnd();
			}
			{
				// Generate the synthetic `invoke` method
				mv = cw.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_VARARGS | ACC_SYNTHETIC, "invoke",
						PrivateUtils.SYNTHETHIC_INVOKE_DESCRIPTOR, null, null);
				mv.visitCode();
				Label label0 = new Label();
				mv.visitLabel(label0);
				mv.visitLineNumber(1, label0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, generatedName, "invoke", invokeMethodDescriptor, false);
				mv.visitInsn(ARETURN);
				mv.visitMaxs(2, 2);
				mv.visitEnd();
			}
			cw.visitEnd();
			final var bytes = cw.toByteArray();
			PrivateUtils.LOGGER.debug("Finished generating ConstructorWrapper for constructor \"{}\"", constructor);
			return new ConstructorWrapper<>() {

				final ConstructorInvoker<T> invoker = (ConstructorInvoker<T>) LambdaUtils
						.rethrowSupplier(() -> getGeneratedClass().getDeclaredField("INSTANCE").get(null)).get();

				@Override
				public @NotNull byte[] getClassBytes() {
					return bytes;
				}

				@Override
				public @NotNull Class<?> getGeneratedClass() {
					return ASMUtilsClassLoader.INSTANCE.define(generatedName, bytes);
				}

				@Override
				public T invoke(Object... args) {
					return invoker.invoke(args);
				}

				@Override
				public Class<?>[] getParameterTypes() {
					return parameterTypes;
				}

			};
		});
	}

	/**
	 * Invokes the wrapped constructor with the specified {@code args}.
	 * 
	 * @param  args                      the arguments to invoke the constructor
	 *                                   with
	 * @return                           the constructed object
	 * @throws ClassCastException        if one of the arguments cannot be cast to
	 *                                   the needed argument at that position
	 * @throws IndexOutOfBoundsException if the number of provided arguments is less
	 *                                   than the number of needed arguments
	 */
	@Override
	T invoke(Object... args);

	/**
	 * Gets the parameter types of the wrapped constructor.
	 * 
	 * @return the parameter types of the wrapped constructor
	 */
	Class<?>[] getParameterTypes();

	//@formatter:off
	class PrivateUtils {
		private static final String SYNTHETHIC_INVOKE_DESCRIPTOR = "(%s)%s".formatted(OBJECT_ARRAY_DESCRIPTOR, OBJECT_DESCRIPTOR);
		private static final Logger LOGGER = LoggerFactory.getLogger(ConstructorWrapper.class);
		private static final Map<Constructor<?>, ConstructorWrapper<?>> CACHE = new HashMap<>();
		private static final AtomicInteger CREATED_WRAPPERS = new AtomicInteger();
		private static <T> String generateName(Constructor<T> ct) {
			return "%s$%s_%s_%s".formatted(
					ConstructorWrapper.class.getSimpleName(),
					CREATED_WRAPPERS.getAndIncrement(),
					ct.getDeclaringClass().getSimpleName(),
					genParameterNames(ct)
			);
		}
		private static <T> String genParameterNames(Constructor<T> ct) {
			final var parametersLength = ct.getParameterTypes().length;
			var names = Stream.of(ct.getParameterTypes())
					.limit(5)
					.map(Class::getSimpleName)
					.reduce("", (a, b) -> {
						if (a.isBlank() || b.isBlank()) {
							return a + b;
						}
						return a + "-" + b;
					});
			if (parametersLength > 5) {
				names = names + "-and%sMore".formatted(parametersLength - 5);
			}
			return names.isBlank() ? "noParameters" : names;
		}
	}
}
