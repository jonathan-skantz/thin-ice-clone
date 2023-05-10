import java.util.*;

public class CalculateSolution {

    private MazeGen mazeGen;

    CalculateSolution(MazeGen mazeGen) {
        this.mazeGen = mazeGen;
    }

    private ArrayList<Node.Type> neighboringTypes(int i, int j) {
        ArrayList<Node.Type> neighboringTypes = new ArrayList<>();
        for (int k = i-1; k < i+2; k++) {
            for (int l = j-1; l < j+2; l++) { 
                if (pointOnGrid(k, l)) {
                    neighboringTypes.add(mazeGen.getMaze()[l][k]);
                }
            }
        }
    
        return neighboringTypes;
    }    

    private int numNeighboringWalls(ArrayList<Node.Type> neighboringTypes, int i, int j) {
        int numNeighboringWalls = 0;
        if (pointOnGrid(i, j)) {
            for(int k = 0; k < 3; k++) {
                if(neighboringTypes.get(k) == Node.Type.WALL) {
                    numNeighboringWalls++;
                }
            }
        }

        return numNeighboringWalls;
    }

    private boolean isDeadEnd(int i, int j) {
        if(numNeighboringWalls(neighboringTypes(i, j), i, j) == 3) return true; // gör : ? grej
        else return false;
    }

    private boolean leadsToDeadEnd(int i, int j) {
        int numBlocked = 0;
        for(int k = 0; k < neighboringTypes(i, j).size(); k++) {
            if(mazeGen.getMaze()[j][i] == Node.Type.BLOCKED) {
                numBlocked++;
            }
        }

        if(numNeighboringWalls(neighboringTypes(i, j), i, j) == 2 && numBlocked == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Finds the one of the longest paths from the player's current position to the endNode
     * (Multiple possible paths might have the same length, therefore only one of these is returned)
     * 
     * @return a LinkedList of Nodes representing a possible longest path to the endNode
     */
    public LinkedList<Node> findLongestPath() {
        LinkedList<Node> longestPath = new LinkedList<>();
        int longestPathLength = 0;

        for(int i = 0; i < mazeGen.width; i++) {
            for(int j = 0; j < mazeGen.height; j++) {
                if(mazeGen.getMaze()[j][i] == Node.Type.GROUND) {
                    // Don't count dead-ends
                    // Don't count nodes that lead to dead-ends
                    if(isDeadEnd(i, j) || leadsToDeadEnd(i, j)) {
                        continue;
                    }
                    
                    longestPathLength++;
                }
            }
        }

        System.out.println(longestPathLength);

        // TODO: 
        // Solve bug #95
        // Construct paths randomly until one has the maximum length 
        
        return longestPath;

    }

    /**
     * Finds the shortest path from startNode to endNode in the maze.
     *
     * @return a LinkedList of Nodes representing the shortest path from startNode to endNode
     */
    public LinkedList<Node> findShortestPath() {
        // create a queue for BFS
        Queue<Node> queue = new LinkedList<>();

        // create an array to store the parent node of each node in the shortest path
        Node[][] parent = new Node[mazeGen.height][mazeGen.width];

        // initialize the parent array to null
        for (int y=0; y<mazeGen.height; y++) {
            for (int x=0; x<mazeGen.width; x++) {
                parent[y][x] = null;
            }
        }

        // mark the startNode as visited and add it to the queue
        boolean[][] visited = new boolean[mazeGen.height][mazeGen.width];
        visited[mazeGen.currentNode.y][mazeGen.currentNode.x] = true;
        queue.add(mazeGen.currentNode);

        // perform BFS
        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();

            // check if the current node is the endNode
            if (currentNode.equals(mazeGen.endNode)) {
                break;
            }

            // explore the neighbors of the current node
            for (int y=currentNode.y-1; y<=currentNode.y+1; y++) {
                for (int x=currentNode.x-1; x<=currentNode.x+1; x++) {
                    // skip if the neighbor is out of bounds, or is not walkable
                    // TODO: here, check if blocked as well
                    if (!pointOnGrid(x, y) || !pointNotCorner(currentNode, x, y) || !pointNotNode(currentNode, x, y) || mazeGen.get(x, y) == Node.Type.WALL || visited[y][x]) {
                        continue;
                    }

                    // mark the neighbor as visited, add it to the queue, and set its parent
                    visited[y][x] = true;
                    Node nextNode = new Node(x, y);
                    queue.add(nextNode);
                    parent[y][x] = currentNode;
                }
            }
        }

        // construct the shortest path from startNode to endNode
        LinkedList<Node> shortestPath = new LinkedList<>();
        Node currentNode = mazeGen.endNode;
        while (currentNode != null) {
            shortestPath.addFirst(currentNode);
            currentNode = parent[currentNode.y][currentNode.x];
        }

        return shortestPath;
    }

    /**
     * Helper method that checks if a given point is on the grid
     * 
     * @param x x-coordinate
     * @param y y-coordinate
     * @return true if the point is on the grid, false otherwise
     */
    private Boolean pointOnGrid(int x, int y) {
        return x >= 0 && y >= 0 && x < mazeGen.width && y < mazeGen.height;
    }

    /**
     * Helper method that checks if a given point is not diagonally adjacent to a given node
     * 
     * @param node the node to compare against
     * @param x x-coordinate
     * @param y y-coordinate
     * @return true if the point is not diagonally adjacent to the given node, false otherwise
     */
    private Boolean pointNotCorner(Node node, int x, int y) {
        return (x == node.x || y == node.y);
    }

    /**
     * Helper method that checks that a given node is not already part of the maze
     * 
     * @param node the node to compare against
     * @param x x-coordinate
     * @param y y-coordinate
     * @return true if the point is not the same as the given node, false otherwise
     */
    private Boolean pointNotNode(Node node, int x, int y) {
        return !(x == node.x && y == node.y);
    }

}