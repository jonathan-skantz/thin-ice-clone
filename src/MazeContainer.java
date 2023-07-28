import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import java.awt.Color;
import java.awt.Point;

import javax.swing.JLabel;
import javax.swing.JLayeredPane;

public class MazeContainer {
    
    public Maze maze;
    private Maze oldMaze;
    public MazeSolver solver;
    public boolean gameOver;
    
    private TimedCounter tcReset;
    private TimedCounter tcSpawnPlayer;
    private TimedCounter tcNewMaze;
    private LinkedList<Node> nodesToChange = new LinkedList<>();

    public int startX;
    public int startY;
    public Block player;
    public Block[][] blocks;
    public JLabel textGameOver;
    public JLabel textLevelComplete;
    public LinkedHashMap<JLabel, Node> hints = new LinkedHashMap<>(Config.hintMax);

    public JLayeredPane sprites = new JLayeredPane();

    public MazeContainer(boolean left) {

        maze = new Maze(MazeGen.width, MazeGen.height, Node.Type.WALL);
        setStartPosition();

        if (!left) {
            sprites.setLocation(Window.mazeWidth, 0);
        }

        // setup UI
        player = new Block("src/textures/player.png", Config.blockSize - 2);
        player.velocity = Config.blockSize;
        player.setVisible(false);
        sprites.add(player);

        textGameOver = Main.createLabel("Game over");
        textGameOver.setForeground(Color.RED);
        int x = (Window.mazeWidth - textGameOver.getPreferredSize().width) / 2;
        textGameOver.setLocation(x, 50);
        sprites.add(textGameOver);
        
        textLevelComplete = Main.createLabel("Level complete");
        textLevelComplete.setForeground(Color.BLACK);
        x = (Window.mazeWidth - textLevelComplete.getPreferredSize().width) / 2;
        textLevelComplete.setLocation(x, 50);
        sprites.add(textLevelComplete);

        createWallBlocks();

        sprites.setSize(Window.mazeWidth, Window.mazeHeight);

        Window.sprites.add(sprites);

        setupTimerReset();
        setupTimerSpawnPlayer();
        setupTimerNewMaze();

    }

    private boolean allowInput() {
        return animationsFinished() &&
                Main.mazeGenThreadDone &&
                !gameOver &&
                !maze.complete;
    }

    private boolean allowReset() {
        return animationsFinished() &&
                Main.mazeGenThreadDone;
    }

    public boolean animationsFinished() {
        return tcNewMaze.finished &&
                tcSpawnPlayer.finished &&
                tcReset.finished;
    }

    // sets up animation to auto-stepback all steps in pathHistory
    private void setupTimerReset() {

        tcReset = new TimedCounter(5) {
            @Override
            public void onStart() {
                setFramesAndPreserveFPS(maze.pathHistory.size());
            }

            @Override
            public void onTick() {
                step(-1, false);
                movePlayerGraphicsTo(maze.currentNode);
            }
        };

        tcReset.finished = true;    // used to allow manual stepping
    }

    private void setupTimerSpawnPlayer() {
        tcSpawnPlayer = new TimedCounter(0.5f, 15) {
            @Override
            public void onStart() {
                player.setVisible(true);
                movePlayerGraphicsTo(maze.currentNode);
                mirrorPlayer(null);
            }

            @Override
            public void onTick() {
                // resize
                float progress = (float)tcSpawnPlayer.frame / tcSpawnPlayer.frames;
                int size = (int)((Config.blockSize - 2) * progress);
                player.setSize(size, size);
            
                movePlayerGraphicsTo(maze.currentNode);
            }
        };

        tcSpawnPlayer.finished = true;

    }

    private void setupTimerNewMaze() {
        tcNewMaze = new TimedCounter(10) {
            public void onStart() {               
                setFramesAndPreserveFPS(nodesToChange.size());

                // call onFinish() after one frame's delay
                onFinishDelay = (float)oneFrameInSeconds();
            }

            public void onTick() {
                Node node = nodesToChange.get(frame-1);
                
                if (frame > 1) {
                    // remove border from last node
                    Node lastNode = nodesToChange.get(frame-2);
                    blocks[lastNode.Y][lastNode.X].setBorder(null);
                }
                
                if (node.equals(oldMaze.currentNode)) {
                    player.setVisible(false);
                }

                refreshBlockGraphics(node);
                blocks[node.Y][node.X].setBorder(Config.BLOCK_BORDER_GEN);
            }

            @Override
            public void onFinish() {
                Node node = nodesToChange.get(frame-1);   // since last node should be changed twice
                blocks[node.Y][node.X].setBorder(null);
                
                Main.textGenerating.setVisible(false);  // NOTE: is hidden twice if multiplayer
                tcSpawnPlayer.start();
            }
        };

        tcNewMaze.finished = true;
    }

    public void dispose() {

        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                Block block = blocks[y][x];
                if (block.tcWater != null) {
                    block.tcWater.reset();
                }
            }
        }

        sprites.removeAll();

        tcReset.reset();
        tcNewMaze.reset();
        tcSpawnPlayer.reset();
    }

    private boolean playerMustMove(Maze.Direction dir) {

        if (maze.walkable(maze.currentNode.getNeighbor(dir))) {

            for (Maze.Direction d : Maze.Direction.values()) {
                if (d != dir && maze.walkable(maze.currentNode.getNeighbor(d))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private void mirrorPlayer(Maze.Direction dir) {

        if (dir == null) {
            player.setMirrored(!maze.walkable(maze.currentNode.getNeighbor(Maze.Direction.LEFT)));
        }

        else if (playerMustMove(Maze.Direction.LEFT)) {
            player.setMirrored(false);
        }
        else if (playerMustMove(Maze.Direction.RIGHT)) {
            player.setMirrored(true);
        }
        else if (dir == Maze.Direction.LEFT) {
            player.setMirrored(false);
        }
        else if (dir == Maze.Direction.RIGHT) {
            player.setMirrored(true);
        }
        
    }

    public void showHint() {

        if (!allowInput() || maze.currentNode == null) {
            return;
        }

        sprites.setVisible(false);
        
        // reset old hint blocks
        for (JLabel hintLabel : hints.keySet()) {
            sprites.remove(hintLabel);
        }

        hints.clear();

        // gets solution based on current node
        LinkedList<Node> path = Config.hintTypeLongest ? solver.findLongestPath() : solver.findShortestPath();

        Node removedFirst = path.removeFirst();

        for (int hint=0; hint<Config.hintMax && hint<path.size(); hint++) {
            Node step = path.get(hint);
            
            JLabel label = new JLabel(String.valueOf(hint+1));
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setForeground(new Color(100, 100, 100));
            label.setFont(Main.hintFont);
            
            label.setSize(Config.blockSize, Config.blockSize);
            int pathFirstAppearance = path.indexOf(step);
            if (pathFirstAppearance != hint) {
                // second step on double
                Node nodeBeforeFirstStep;
                JLabel labelFirstStep;

                Object[] labels = hints.keySet().toArray();
                if (pathFirstAppearance == 0) {
                    nodeBeforeFirstStep = removedFirst;
                    labelFirstStep = (JLabel) labels[pathFirstAppearance];
                }
                else {
                    nodeBeforeFirstStep = path.get(pathFirstAppearance-1);
                    labelFirstStep = (JLabel) labels[pathFirstAppearance];
                }
                if (nodeBeforeFirstStep.X < step.X) {
                    labelFirstStep.setHorizontalAlignment(JLabel.LEFT);
                    label.setHorizontalAlignment(JLabel.RIGHT);
                }
                else {
                    labelFirstStep.setHorizontalAlignment(JLabel.RIGHT);
                    label.setHorizontalAlignment(JLabel.LEFT);
                }

            }
            label.setLocation(getBlockPosition(step));
            
            sprites.add(label);

            hints.put(label, step);
            sprites.setComponentZOrder(label, 1);
        }

        sprites.setVisible(true);
        
    }

    public void zoom(int ch) {
        player.velocity += ch;
            
        startY = (Window.height - Config.blockSize * maze.height) / 2;
        startX = (Window.mazeWidth - Config.blockSize * maze.width) / 2;

        if (maze.currentNode != null) {
            player.setSize(player.getWidth() + ch, player.getHeight() + ch);
            movePlayerGraphicsTo(maze.currentNode);
        }
        
        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                blocks[y][x].setSize(Config.blockSize, Config.blockSize);
                blocks[y][x].setLocation(getBlockPosition(new Node(x, y)));
            }
        }
        
        for (JLabel label : hints.keySet()) {
            label.setSize(Config.blockSize, Config.blockSize);
            label.setFont(Main.hintFont);
            label.setLocation(getBlockPosition(hints.get(label)));
        }
    }

    // also resets maze
    public void resetMazeGraphics() {
        
        if (!allowReset()) {
            return;
        }

        gameOver = false;

        textGameOver.setVisible(false);
        textLevelComplete.setVisible(false);
        removeHintTexts();

        if (Main.ENABLE_ANIMATIONS) {
            tcReset.start();
        }
        else {
            ArrayList<Node> changed = new ArrayList<>(maze.pathHistory);
            maze.reset();

            for (Node node : changed) {
                refreshBlockGraphics(node);
            }

            movePlayerGraphicsTo(maze.currentNode);
            mirrorPlayer(null);
        }
    }

    private void testGameOver() {
        if (solver.findShortestPath().size() == 0) {
            textGameOver.setVisible(true);
            gameOver = true;
        }
    }

    public void tryToMove(Maze.Direction dir) {

        if (!allowInput()) {
            return;
        }

        Node lastNode = maze.currentNode;

        if (maze.userMove(dir)) {

            player.move(dir);
            mirrorPlayer(dir);
            refreshBlockGraphics(lastNode);

            if (hints.size() > 0) {

                Object[] labels = hints.keySet().toArray();
                
                if (maze.currentNode.equals(hints.get(labels[0]))) {
                    sprites.remove((JLabel)labels[0]);
                    hints.remove(labels[0]);
                }
                else {
                    for (JLabel hintLabel : hints.keySet()) {
                        sprites.remove(hintLabel);
                    }
                    hints.clear();
                    sprites.repaint();   // some labels are still visible
                }
            }

            if (maze.complete) {
                textLevelComplete.setVisible(true);
            }

            else {
                testGameOver();
            }
        }

    }

    public void step(int direction, boolean checkAllowInput) {

        if (!gameOver && checkAllowInput && !allowInput()) {
            return;
        }
        
        Node lastNode = maze.currentNode;

        Maze.Direction dir = maze.step(direction);

        if (dir != null) {
            
            removeHintTexts();
            player.move(dir);
            mirrorPlayer(dir);
            refreshBlockGraphics(lastNode);

            if (direction == -1) {

                if (gameOver) {
                    gameOver = false;
                    textGameOver.setVisible(false);
                }
                refreshBlockGraphics(maze.currentNode);
            }
            else {
                testGameOver();
            }
        }
        
    }

    // also resets texts
    public void setMaze(Maze maze) {
        
        oldMaze = this.maze;
        this.maze = maze;

        removeHintTexts();
        gameOver = false;
        textGameOver.setVisible(false);
        textLevelComplete.setVisible(false);

        solver = new MazeSolver(maze);

        if (maze.width != oldMaze.width || maze.height != oldMaze.height) {
            for (Block[] row : blocks) {
                for (Block spr : row) {
                    sprites.remove(spr);
                }
            }
            sprites.repaint();      // some old blocks are still visible
            setStartPosition();

            blocks = new Block[maze.height][maze.width];
            oldMaze = new Maze(maze.width, maze.height, Node.Type.WALL);

            player.setVisible(false);
            createWallBlocks();
        }

        // determine which nodes should change
        nodesToChange = new LinkedList<>();
        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                
                Node node = new Node(x, y);
                
                if (maze.get(node) == oldMaze.get(node) && !(node.equals(oldMaze.currentNode))) {
                    continue;
                }
                nodesToChange.add(node);
            }
        }

        if (nodesToChange.size() == 0) {
            // when switching to multiplayer without any maze generated
            return;
        }

        if (Main.ENABLE_ANIMATIONS) {
            tcNewMaze.start();
        }
        else {
            Main.textGenerating.setVisible(false);
            player.setVisible(true);
            movePlayerGraphicsTo(maze.startNode);
            mirrorPlayer(null);

            for (Node node : nodesToChange) {
                refreshBlockGraphics(node);
            }
            
        }
            
    }

    private void removeHintTexts() {
        for (JLabel hint : hints.keySet()) {
            sprites.remove(hint);
        }
        hints.clear();

        sprites.repaint();   // some hints are still visible
    }

    public void movePlayerGraphicsTo(Node node) {
        int blockX = startX + node.X * Config.blockSize;
        int blockY = startY + node.Y * Config.blockSize;
    
        int centeredX = blockX + (Config.blockSize - player.getWidth()) / 2;
        int centeredY = blockY + (Config.blockSize - player.getHeight()) / 2;

        player.setLocation(centeredX, centeredY);
    }

    private void refreshBlockGraphics(Node node) {
        Block block = blocks[node.Y][node.X];
        Node.Type type = maze.get(node);

        block.setType(type);

        for (Node neighbor : maze.getNeighborsOf(node, false)) {
            boolean nodeCausesFrost = maze.get(node) == Node.Type.DOUBLE || maze.get(node) == Node.Type.END_DOUBLE;
            blocks[neighbor.Y][neighbor.X].setFrost(neighbor, node, nodeCausesFrost);
        }
    }

    public void createWallBlocks() {

        blocks = new Block[maze.height][maze.width];

        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                Block block = new Block("src/textures/wall.png", Config.blockSize);
                blocks[y][x] = block;
                block.setLocation(getBlockPosition(new Node(x, y)));
                sprites.add(block);
            }
        }
    }

    private void setStartPosition() {
        startY = (Window.height - Config.blockSize * maze.height) / 2;
        startX = (Window.mazeWidth - Config.blockSize * maze.width) / 2;
    }

    public Point getBlockPosition(Node node) {
        int posX = startX + node.X * Config.blockSize;
        int posY = startY + node.Y * Config.blockSize;
        return new Point(posX, posY);
    }


}
