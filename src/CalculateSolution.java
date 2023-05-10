import java.util.*;

public class CalculateSolution {

    private MazeGen mazeGen;

    CalculateSolution(MazeGen mazeGen) {
        this.mazeGen = mazeGen;
    }

    private HashMap<Integer, int[]> getNeighboringValues(int i, int j, boolean[][] visited) {
        HashMap<Integer, int[]> neighboringValues = new HashMap<>();
        for (int k = i-1; k < i+2; k++) {
            for (int l = j-1; l < j+2; l++) { 
                if (pointOnGrid(k, l) && mazeGen.getMaze()[l][k] == Node.Type.GROUND && !visited[l][k]) {
                    String value = mazeGen.arrVisual[l][k];
                    int[] indexes = {l, k};
                    System.out.println(l + ", " + k);
                    neighboringValues.put(Integer.valueOf(value), indexes);
                    visited[l][k] = true;
                }
            }
        }
    
        return neighboringValues;
    } 
    
    public LinkedList<Node> findLongestPath() {
        String[][] arrVisual = mazeGen.arrVisual;
        LinkedList<Node> longestPath = new LinkedList<>();
        boolean[][] visited = new boolean[mazeGen.height][mazeGen.width];

        //find neighbording node with lowest value and add to longestPath
        Node curr = mazeGen.currentNode;
        int lowest = Integer.MAX_VALUE;
        HashMap<Integer, int[]> neighboringValues;
        int[] indexOfLowestNeighbor = new int[2];
        
        while(curr != mazeGen.endNode) { 
            neighboringValues = getNeighboringValues(curr.x, curr.y, visited);
            for(int value : neighboringValues.keySet()) {
                if(value < lowest) {
                    lowest = value;
                }
            }

            indexOfLowestNeighbor = neighboringValues.get(lowest);
            // set new curr
            if(indexOfLowestNeighbor[0] == mazeGen.endNode.x && indexOfLowestNeighbor[1] == mazeGen.endNode.y) {
                curr = mazeGen.endNode;
            } else {
                curr = new Node(indexOfLowestNeighbor[0], indexOfLowestNeighbor[1]);
                longestPath.addLast(curr);
                System.out.println(longestPath.size());
            }

        }

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