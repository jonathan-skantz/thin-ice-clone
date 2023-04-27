import java.awt.Color;

public class Main {

    public static void main(String[] args) {

        Window window = new Window();
        
        // Allow sprites to reference the window, without having to
        // save the reference to every instance of Sprite.
        Sprite.window = window;
        
        Sprite player = new Sprite("player.png", 10);

        // setup key callbacks
        KeyHandler listener = new KeyHandler();
        window.addKeyListener(listener);
        
        KeyHandler.ActionKey.UP.setCallback(() -> { player.move(KeyHandler.ActionKey.UP); });
        KeyHandler.ActionKey.DOWN.setCallback(() -> { player.move(KeyHandler.ActionKey.DOWN); });
        KeyHandler.ActionKey.LEFT.setCallback(() -> { player.move(KeyHandler.ActionKey.LEFT); });
        KeyHandler.ActionKey.RIGHT.setCallback(() -> { player.move(KeyHandler.ActionKey.RIGHT); });

        // MazeGenerator mg = new MazeGenerator(10);
        // mg.generateMaze();

        int[][] maze = new int[][]{
            {0, 0, 1, 1, 0},
            {1, 1, 1, 1, 0},
            {1, 1, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0}
        };

        int blockSize = 50;
        int startX = 50;
        int startY = 50;

        Color colorIce = new Color(225, 225, 255);
        Color colorWall = Color.BLUE;

        for (int y=0; y<maze.length; y++) {
            for (int x=0; x<maze[y].length; x++) {
                
                int blockType = maze[y][x];
                Sprite block;

                // System.out.println("sprite " + y + ", " + x);
                if (blockType == 0) {
                    block = new Sprite(blockSize, blockSize, colorWall, 0);
                }
                else {
                    block = new Sprite(blockSize, blockSize, colorIce, 0);
                }

                block.addBorder(6, Color.BLACK);

                int xPos = startX + x * blockSize;
                int yPos = startY + y * blockSize;
                block.moveTo(xPos, yPos);
                
            }
        }

    }

}
