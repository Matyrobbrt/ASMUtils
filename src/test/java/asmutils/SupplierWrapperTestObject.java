package asmutils;

public class SupplierWrapperTestObject {

	private final String toPrint;

	public SupplierWrapperTestObject(String toPrint) {
		this.toPrint = toPrint;
	}

	public String print() {
		return toPrint;
	}

	public static String hmm() {
		return "hmm";
	}

}
