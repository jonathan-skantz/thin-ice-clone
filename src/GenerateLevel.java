import java.util.Random;
import java.util.Stack;

public class GenerateLevel {

    private static final int ROWS = 15;
    private static final int COLS = 19;
    private static final int WALL = 1;
    private static final int ICE = 0;

    private int[][] board = new int[ROWS][COLS];
    Random rand = new Random();

    public GenerateLevel() {
        // Assign all blocks as walls
        for(int i = 0; i < ROWS; i++) {
            for(int j = 0; j < COLS; j++) {
                board[i][j] = WALL;
            }
        }
    }

    /**
     * Generate a randomized level
     * 
     * @return a matrix representation of the level
     */
    public int[][] generate() {
        return randomLevelGeneration();
    }

    /**
     * Perform Recursive Backtracking algorithm on game board to randomize wall placement
     * 
     * @return a matrix representation of the level
     */
    public int[][] randomLevelGeneration() {
        // TODO Implement maze algorithm 

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