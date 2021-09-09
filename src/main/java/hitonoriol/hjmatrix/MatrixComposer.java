package hitonoriol.hjmatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MatrixComposer {
	private List<Number[]> rows = new ArrayList<>();
	private Stream<Number> contentsStream;
	
	/* Used only by Matrix.create() factory */
	MatrixComposer() {}

	public MatrixComposer addRow(Number... numbers) {
		rows.add(numbers);
		return this;
	}

	Matrix createMatrix() {
		contentsStream = Stream.of(rows.get(0));
		rows.stream()
				.skip(1)
				.forEach(rowArr -> contentsStream = Stream.concat(contentsStream, Stream.of(rowArr)));

		Number[] matrixContents = contentsStream.toArray(Number[]::new);
		final int height = rows.size();
		return new Matrix(matrixContents, height, matrixContents.length / height);
	}
}