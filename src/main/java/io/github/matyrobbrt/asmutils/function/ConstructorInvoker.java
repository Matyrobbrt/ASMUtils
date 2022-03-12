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
