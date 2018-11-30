import java.util.concurrent.Callable;

public class CallableRowProcessor implements Callable<Integer> {

    private int k, start, end;
    private int[][] matrix;


    private int intMax = Integer.MAX_VALUE;

    public CallableRowProcessor(int[][] matrix,
                                int k, int start, int end
    ) {
        this.matrix = matrix;
        this.k = k;
        this.start = start;
        this.end = end;
    }

    public Integer call() {
        for (int i = start; i < end; i++) {
            for (int j = 0; j < matrix.length; j++) {
                if (matrix[i][k] == intMax || matrix[k][j] == intMax) {
                    continue;
                } else if (matrix[i][j] > matrix[i][k] + matrix[k][j]) {
                    matrix[i][j] = matrix[i][k] + matrix[k][j];
                }
            }
        }

        return new Integer(0);
    }
}
