package asmutils;

public class TestObject {

	public static final String THING = ye();

	private static final String ye() {
		return "ye";
	}

	public final String thing;

	public TestObject(String thing) {
		this.thing = thing;
	}
}