public class GenerateAndCalculate {
    public static void main(String[] args) {
        MazeGenerator mazeGenerator = new MazeGenerator(10);
        mazeGenerator.generateMaze();

        Node startNode = mazeGenerator.getStartNode();
        Node endNode = mazeGenerator.getEndNode();
        
        System.out.println(mazeGenerator.printRepresentation());
        System.out.println("Start node: (" + startNode.x + "," + startNode.y + ")");
        System.out.println("End node: (" + endNode.x + "," + endNode.y + ")");
    }
}
