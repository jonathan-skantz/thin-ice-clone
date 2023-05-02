/**
 * A main method for generating a maze and calculating the shortest path from 
 * startNode to endNode 
 */
import java.util.*;

public class GenerateAndCalculate {
    public static void main(String[] args) {
        MazeGenerator mazeGenerator = new MazeGenerator(10);
        mazeGenerator.generateMaze();

        Node startNode = mazeGenerator.getStartNode();
        Node endNode = mazeGenerator.getEndNode();

        System.out.println(mazeGenerator.toString());
        System.out.println("Start node: (" + startNode.x + "," + startNode.y + ")");
        System.out.println("End node: (" + endNode.x + "," + endNode.y + ")");

        CalculateSolution solution = new CalculateSolution(mazeGenerator.maze, startNode, endNode);

        LinkedList<Node> path = solution.findShortestPath();
        System.out.println("\nCorrect path:");
        for (Node node : path) {
            System.out.print("(" + node.x + "," + node.y + ") ");
        }
    }
}
