/**
 * A main method for generating a maze and calculating the shortest path from 
 * startNode to endNode 
 */
import java.util.*;

public class GenerateAndCalculate {
    public static void main(String[] args) {
        MazeGen mazeGen = new MazeGen(10, 15);
        mazeGen.generate();

        System.out.println(mazeGen);

        CalculateSolution solution = new CalculateSolution(mazeGen);

        LinkedList<Node> path = solution.findShortestPath();
        System.out.println("\nCorrect path:");
        for (Node node : path) {
            System.out.print("(" + node.x + "," + node.y + ") ");
        }
    }
}
