package asmutils;

import java.util.function.Supplier;

public final class TestSupplier implements Supplier<String> {

	@Override
	public String get() {
		return TestObject.THING;
	}

}
