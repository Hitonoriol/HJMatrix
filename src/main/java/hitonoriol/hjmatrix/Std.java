package hitonoriol.hjmatrix;

public class Std {
	public static String setWidth(String string, int length) {
		return String.format("%1$" + length + "s", string);
	}

	public static void out(String str) {
		System.out.print(str);
	}

	public static void out(String str, int width) {
		out(setWidth(str, width));
	}
}