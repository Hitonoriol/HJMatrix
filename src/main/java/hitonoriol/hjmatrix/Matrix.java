package hitonoriol.hjmatrix;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.apache.commons.lang3.mutable.MutableBoolean;

public class Matrix {
	private int height, width;
	private Number contents[];

	public static Comparator<Number> numberComparator = (num1, num2) -> Double.compare(num1.doubleValue(),
			num2.doubleValue());
	public static BiPredicate<Number, Number> greater = (num1, num2) -> numberComparator.compare(num1, num2) > 0;
	public static BiPredicate<Number, Number> smaller = (num1, num2) -> numberComparator.compare(num1, num2) < 0;
	public static BiPredicate<Number, Number> equal = (num1, num2) -> numberComparator.compare(num1, num2) == 0;

	public static BiPredicate<Number, Number> greaterOrEqual = (num1, num2) -> greater.test(num1, num2)
			|| equal.test(num1, num2);
	public static BiPredicate<Number, Number> smallerOrEqual = (num1, num2) -> smaller.test(num1, num2)
			|| equal.test(num1, num2);

	public static BiPredicate<Number, Number> maxPredicate = (currentMax, num) -> greater.test(num, currentMax);
	public static BiPredicate<Number, Number> minPredicate = (currentMin, num) -> greater.test(currentMin, num);

	public static interface Operator extends BiFunction<Number, Number, Number> {
		static Operator addition = (num1, num2) -> num1.doubleValue() + num2.doubleValue();
		static Operator subtraction = (num1, num2) -> num1.doubleValue() - num2.doubleValue();
	}

	public Matrix(Number[] contents, int height, int width) {
		this.contents = contents;
		this.height = height;
		this.width = width;
	}

	public Matrix(int height, int width) {
		this(new Number[height * width], height, width);
	}
	
	public Matrix setContents(Number... numbers) {
		if (numbers.length == height * width)
			contents = numbers;
		return this;
	}
	
	private int getIdx(final int i, final int j) {
		return j + i * width;
	}

	private int getJ(final int idx) {
		return idx % width;
	}

	private int getI(final int idx) {
		return idx / width;
	}

	public int getSize() {
		return height * width;
	}

	public Number get(final int i, final int j) {
		final int idx = getIdx(i, j);
		if (idx < getSize())
			return contents[idx];
		else
			return null;
	}

	public Number get(Coords elemCoords) {
		return get(elemCoords.i, elemCoords.j);
	}

	public double getDouble(int i, int j) {
		return get(i, j).doubleValue();
	}

	public int getInt(int i, int j) {
		return get(i, j).intValue();
	}

	public void set(final int i, final int j, Number value) {
		contents[getIdx(i, j)] = value;
	}

	private void move(int srcI, int srcJ, int destI, int destJ) {
		set(destI, destJ, get(srcI, srcJ));
	}

	/* Generalization for addition/subtraction */
	private Matrix applyOperator(Matrix rhs, Operator operator) {
		if (getSize() == rhs.getSize())
			forEachElement(coords -> set(coords.i, coords.j,
					operator.apply(get(coords), rhs.get(coords.i, coords.j))));
		return this;
	}

	/*
	 * Check if every element of this matrix satisfies the given BiPredicate relative to other Matrix
	 * this[i][j] (?) rhs[i][j]		<-- where (?) returns a boolean value for each (i, j)
	 */
	public boolean applyPredicate(Matrix rhs, BiPredicate<Number, Number> predicate) {
		MutableBoolean res = new MutableBoolean(true);

		forEachElement(coords -> {
			if (!predicate.test(get(coords), rhs.get(coords.i, coords.j)))
				res.setValue(false);
		});

		return res.booleanValue();
	}

	public Matrix add(Matrix rhs) {
		applyOperator(rhs, Operator.addition);
		return this;
	}

	public Matrix sub(Matrix rhs) {
		applyOperator(rhs, Operator.subtraction);
		return this;
	}

	public Matrix multiply(Matrix rhs) {
		if (width != rhs.height)
			return null;

		Matrix ret = new Matrix(height, rhs.width);
		for (int i = 0; i < height; i++)
			for (int j = 0; j < rhs.width; j++) {
				double sum = 0;
				for (int k = 0; k < width; k++)
					sum += getDouble(i, k) * rhs.getDouble(k, j);
				ret.set(i, j, sum);
			}
		return ret;
	}

	public boolean rowHas(final int i, final Number elem) {
		MutableBoolean hasElem = new MutableBoolean(false);

		forEachInRow(i, n -> {
			if (hasElem.isFalse())
				hasElem.setValue(n.doubleValue() == elem.doubleValue());
		});

		return hasElem.booleanValue();
	}

	public boolean colHas(final int j, final Number elem) {
		MutableBoolean hasElem = new MutableBoolean(false);

		forEachInCol(j, n -> {
			if (hasElem.isFalse())
				hasElem.setValue(n.doubleValue() == elem.doubleValue());
		});

		return hasElem.booleanValue();
	}

	public boolean hasRow(final int i) {
		return i < height && i >= 0;
	}

	public boolean hasCol(final int j) {
		return j < width && j >= 0;
	}

	public void delRow(final int i) {
		if (height == 1)
			return;

		if (i < height - 1) {
			int startIdx = getIdx(i, 0);
			for (int ni = i + 1, ki = i; ni < height; ++ni, ++ki)
				for (int k = startIdx + width; k < getSize(); ++k)
					move(ni, getJ(k), ki, getJ(k));
		}
		--height;
	}

	public void delCol(final int j) {
		if (width == 1)
			return;

		int delElems = 0;
		for (int i = 0; i < height; ++i) {
			set(i, j, null);
			++delElems;
		}

		for (int k = 0; k < delElems; ++k)
			for (int idx = 0; idx < getSize(); ++idx)
				if (contents[idx] == null)
					for (; idx < getSize() - 1; ++idx)
						move(getI(idx + 1), getJ(idx + 1), getI(idx), getJ(idx));

		--width;
	}

	/*
	 * Pick an element based on a BiPredicate that compares some "intermediate" value to every element in row
	 * (Can be used with minPredicate / maxPredicate) 
	 */
	public Number pickFromRow(BiPredicate<Number, Number> minMaxPredicate, int i) {
		Number pick = get(i, 0), testNum;
		for (int j = 1; j < width; ++j)
			if (minMaxPredicate.test(pick, testNum = get(i, j)))
				pick = testNum;
		return pick;
	}

	/* Pick an element based on a BiPredicate that compares some "intermediate" value to every element in column */
	public Number pickFromCol(BiPredicate<Number, Number> minMaxPredicate, int j) {
		Number pick = get(0, j), testNum;
		for (int i = 1; i < height; ++i)
			if (minMaxPredicate.test(pick, testNum = get(i, j)))
				pick = testNum;
		return pick;
	}

	public IntStream iStream() {
		return IntStream.range(0, getHeight());
	}

	public IntStream jStream() {
		return IntStream.range(0, getWidth());
	}

	/* Iterate over every element in this matrix */
	public void forEachElement(Consumer<Coords> elementConsumer) {
		Coords elementCoords = new Coords();
		for (int i = 0; i < height; ++i)
			for (int j = 0; j < width; ++j)
				elementConsumer.accept(elementCoords.set(i, j));

	}

	/* Iterate over the row, feeding each element into the Consumer */
	public void forEachInRow(final int i, Consumer<Number> numberConsumer) {
		for (int j = 0; j < width; ++j)
			numberConsumer.accept(get(i, j));
	}

	/* Iterate over the column, feeding each element into the Consumer */
	public void forEachInCol(final int j, Consumer<Number> numberConsumer) {
		for (int i = 0; i < height; ++i)
			numberConsumer.accept(get(i, j));
	}

	/* Iterate over two rows, feeding their elements in pairs (where j coords are the same) to the BiConsumer */
	public void forEachRowPair(final int i1, final int i2, BiConsumer<Number, Number> rowPairConsumer) {
		for (int j = 0; j < width; ++j)
			rowPairConsumer.accept(get(i1, j), get(i2, j));
	}

	/* Iterate over two cols, feeding their elements in pairs (where i coords are the same) to the BiConsumer */
	public void forEachColPair(final int j1, final int j2, BiConsumer<Number, Number> colPairConsumer) {
		for (int i = 0; i < height; ++i)
			colPairConsumer.accept(get(i, j1), get(i, j2));
	}

	public boolean colsEqual(final int j1, final int j2) {
		MutableBoolean eq = new MutableBoolean(true);
		forEachColPair(j1, j2, (colVal1, colVal2) -> {
			if (eq.booleanValue())
				eq.setValue(colVal1.doubleValue() == colVal2.doubleValue());
		});
		return eq.booleanValue();
	}

	public boolean rowsEqual(final int i1, final int i2) {
		MutableBoolean eq = new MutableBoolean(true);
		forEachRowPair(i1, i2, (rowVal1, rowVal2) -> {
			if (eq.booleanValue())
				eq.setValue(rowVal1.doubleValue() == rowVal2.doubleValue());
		});
		return eq.booleanValue();
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	private static NumberFormat numberFormatter = NumberFormat.getInstance(Locale.US);
	static {
		numberFormatter.setMinimumFractionDigits(0);
		numberFormatter.setRoundingMode(RoundingMode.HALF_DOWN);
	}

	private static String round(Number num, int n) {
		numberFormatter.setMaximumFractionDigits(n);
		return numberFormatter.format(num.doubleValue());
	}

	/* Dump contents to stdout with specified col/row labels & cell width */
	public void dump(int cellWidth, String rowName, String colName) {
		if (colName != null) {
			Std.out(Std.setWidth("", cellWidth));
			for (int i = 0; i < this.width; ++i)
				Std.out(colName + (i + 1), cellWidth);
			Std.out("\n");
		}

		for (int i = 0; i < height; ++i) {
			Std.out(rowName == null ? "" : rowName + (i + 1), cellWidth);
			for (int j = 0; j < this.width; ++j)
				Std.out(round(get(i, j), 2) + "", cellWidth);
			Std.out("\n");
		}
		Std.out("\n");
	}

	public void dump(String rowName, String colName) {
		dump(5, rowName, colName);
	}

	public void dump() {
		dump(null, null);
	}

	/* Factory for initialization by hand */
	public static Matrix create(Consumer<MatrixComposer> composerConsumer) {
		MatrixComposer composer = new MatrixComposer();
		composerConsumer.accept(composer);
		return composer.createMatrix();
	}

	/* Element descriptor for forEachElement(), can be used to retrieve the element via Matrix.get(<Coords>) */
	public static class Coords {
		public int i, j;

		public Coords() {
			set(0, 0);
		}

		public Coords(int i, int j) {
			set(i, j);
		}

		public Coords set(int i, int j) {
			this.i = i;
			this.j = j;
			return this;
		}
	}
}
