import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.*;

public class FloydWarshall {
    private static final int I = Integer.MAX_VALUE; // Infinity
    private static final int dim = 5000;
    private static double fill = 0.3;
    private static int maxDistance = 100;
    private static int adjacencyMatrix[][] = new int[dim][dim];
    private static int d[][] = new int[dim][dim];

    private static int nThreads = 8;
    private static int partitionSize = 100;
    private static int dParallel[][] = new int[dim][dim];

    /*
     * Generate a randomized matrix to use for the algorithm.
     */
    private static void generateMatrix() {
        Random random = new Random();
        for (int i = 0; i < dim; i++)
        {
            for (int j = 0; j < dim; j++)
            {
                if (i != j)
                    adjacencyMatrix[i][j] = I;
            }
        }
        for (int i = 0; i < dim * dim * fill; i++)
        {
            adjacencyMatrix[random.nextInt(dim)][random.nextInt(dim)] =
                random.nextInt(maxDistance + 1);
        }
    }
    /*
     * Execute Floyd Warshall on adjacencyMatrix.
     */
    private static void execute() {
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++)
            {
                d[i][j] = adjacencyMatrix[i][j];
                if (i == j)
                {
                    d[i][j] = 0;
                }
            }
        }
        for (int k = 0; k < dim; k++) {
            for (int i = 0; i < dim; i++) {
                for (int j = 0; j < dim; j++) {
                    if (d[i][k] == I || d[k][j] == I) {
                        continue;
                    } else if (d[i][j] > d[i][k] + d[k][j]) {
                        d[i][j] = d[i][k] + d[k][j];
                    }
                }
            }
            //System.out.println("pass " + (k + 1) + "/" + dim);
        }
    }
    /*
     *
     */
    private static void executeParallel() {

        // copy adjacencyMatrix into dParallel and zero out diagonals
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++)
            {
                dParallel[i][j] = adjacencyMatrix[i][j];
                if (i == j)
                {
                    dParallel[i][j] = 0;
                }
            }
        }

        // Set up for parallel processing
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        ArrayList<Future<Integer>> rowFutureList = new ArrayList<>(dim / partitionSize);

        // Outer loop.  Each iteration k ust be complete before the next starts
        // Inner loop is parallel
        for (int k = 0; k < dim; k++) {

            // Start parallel processing
            // Submit jobs as batches of rows for this iteration
            for (int i = 0; i < dim; i += partitionSize) {

                // Submit partition of rows
                int endRow = i + partitionSize;
                // Handle remainder at end matrix if necessary
                if (endRow > dim) {
                    endRow = dim ;
                }
                // Submit rows
                Callable<Integer> rows = new CallableRowProcessor(dParallel, k, i, endRow);
                Future<Integer> rowFuture = executor.submit(rows);
                rowFutureList.add(rowFuture);

            } // End Callable submission loop

            //System.out.println("parallel pass " + (k + 1) + "/" + dim);

            // Make sure whole array is processed before next iteration
            for (Future<Integer> future : rowFutureList) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            } // End future synchronization loop

            rowFutureList.clear();

        } // End k iteration loop

        executor.shutdown();
    } // End executeParallel()

    /*
     * Print matrix[dim][dim]
     */
    private static void print(int matrix[][]) {
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (matrix[i][j] == I) {
                    System.out.print("I" + " ");
                } else {
                    System.out.print(matrix[i][j] + " ");
                }
            }
            System.out.println();
        }
        System.out.println();
    }
    /*
     * Compare two matrices, matrix1[dim][dim] and matrix2[dim][dim] and
     * print whether they are equivalent.
     */
    private static void compare (int matrix1[][], int matrix2[][]) {
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (matrix1[i][j] != matrix2[i][j])
                {
                    System.out.println("Comparison failed");
                    return;
                }
            }
        }
        System.out.println("Comparison succeeded");
    }
    public static void main(String[] args) {
        long start, end, startParallel, endParallel;
        generateMatrix();

        start = System.nanoTime();
        execute();
        end = System.nanoTime();

        startParallel = System.nanoTime();
        executeParallel();
        endParallel = System.nanoTime();

        System.out.println("Sequential time consumed: " + (double)(end - start) / 1000000000
                            + "\nParallel time consumed: " + (double)(endParallel - startParallel) / 1000000000);
        compare(d, dParallel);

        //print(d);
        //print(dParallel);
    }
}
