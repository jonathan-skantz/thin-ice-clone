import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GenerateLevel {

    private static final int ROWS = 15;
    private static final int COLS = 19;
    private static final int WALL = 1;
    private static final int ICE = 0;

    private int[][] board = new int[ROWS][COLS];
    Random rand = new Random();

    /**
     * Generate a randomized level
     * 
     * @return a matrix representation of the level
     */
    public int[][] generate() {
        // Initialize maze with walls
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                board[i][j] = WALL; 
            }
        }

        // Choose random starting point
        int startRow = rand.nextInt(ROWS);
        int startCol = rand.nextInt(COLS);

        return randomLevelGeneration(startRow, startCol);
    }

    /**
     * Generate a randomized level using depth-first search
     * 
     * @return a matrix representation of the level
     */
    public int[][] randomLevelGeneration(int row, int col) {
        // Set current cell to empty
        board[row][col] = 0;

        // Shuffle directions to randomly choose which way to go
        List<int[]> directions = new ArrayList<>(Arrays.asList(
                new int[] {-1, 0}, // up
                new int[] {0, 1},  // right
                new int[] {1, 0},  // down
                new int[] {0, -1}  // left
        ));
        Collections.shuffle(directions, rand);

        // Create paths in each direction
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            int width = rand.nextInt(20) + 1; // generate a random width between 1 and 3

            // Check if new cell is within bounds and hasn't been visited yet
            if (newRow >= 0 && newRow < ROWS && newCol >= 0 && newCol < COLS && board[newRow][newCol] == 1) {
                // Create path of given width
                for (int i = -width/2; i <= width/2; i++) {
                    int pathRow = newRow + i*dir[0];
                    int pathCol = newCol + i*dir[1];
                    if (pathRow >= 0 && pathRow < ROWS && pathCol >= 0 && pathCol < COLS) {
                        board[pathRow][pathCol] = 0;
                    }
                }
                randomLevelGeneration(newRow, newCol);
            }
        }

        return board;
    }    

    public static void printRepresentation(int[][] level) {
        for (int i = 0; i < level.length; i++) {
            for (int j = 0; j < level[0].length; j++) {
                System.out.print(level[i][j] + " ");
            }
            System.out.println(); // Print a newline after each row
        }
    }

    public static void main(String[] args) {
        GenerateLevel level = new GenerateLevel();
        int[][] generatedLevel = level.generate();
        printRepresentation(generatedLevel);
    }
}