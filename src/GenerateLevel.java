import java.util.Random;
import java.util.Stack;

public class GenerateLevel {

    private int[][] board = new int[19][15];
    Random rand = new Random();

    public GenerateLevel() {
        // Assign all blocks as walls
        for(int i = 0; i < 19; i++) {
            for(int j = 0; j < 15; j++) {
                board[i][j] = 1;
            }
        }
    }

    /**
     * Generate a randomized level
     * 
     * @return a matrix representation of the level
     */
    public int[][] generate() {
        return recursiveBacktracking();
    }

    /**
     * Perform Recursive Backtracking algorithm on game board to randomize wall placement
     * 
     * @return a matrix representation of the level
     */
    public int[][] recursiveBacktracking() {
        // TODO Implement maze algorithm 
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