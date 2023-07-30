import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import java.awt.Color;
import java.awt.Point;

import javax.swing.JLabel;
import javax.swing.JLayeredPane;

public class MazeContainer {
    
    private Maze maze;
    private Maze oldMaze;
    private MazeSolver solver;
    private boolean gameOver;
    
    private TimedCounter tcReset;
    private TimedCounter tcSpawnPlayer;
    private TimedCounter tcNewMaze;
    private LinkedList<Node> nodesToChange = new LinkedList<>();

    private boolean isMirrored;

    private int startX;
    private int startY;

    private Block player;
    private Block[][] blocks;
    private JLabel textStatus;
    private JLabel textSteps;
    
    private LinkedHashMap<JLabel, Node> hints = new LinkedHashMap<>(Config.hintMax);

    public JLayeredPane sprites = new JLayeredPane();
    // sprites.setOpaque(false);    // TODO: possible with JPanel?

    private enum Status {
        COMPLETE(Color.GREEN),
        INCOMPLETE(Color.ORANGE),
        UNSOLVABLE(Color.RED);

        public final Color color;
        private final String display;

        private Status(Color color) {
            this.color = color;
            String d = super.toString().toLowerCase();
            d = String.valueOf(d.charAt(0)).toUpperCase() + d.substring(1);
            display = d;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    public MazeContainer() {

        maze = new Maze(MazeGen.width, MazeGen.height, Node.Type.WALL);
        setStartPosition();

        // setup UI
        player = new Block("src/textures/player.png", Config.blockSize - 2);
        player.velocity = Config.blockSize;
        player.setVisible(false);
        sprites.add(player);

        setupText();

        createWallBlocks();

        sprites.setSize(Window.mazeWidth, Window.mazeHeight);

        Window.sprites.add(sprites);

        setupTimerReset();
        setupTimerSpawnPlayer();
        setupTimerNewMaze();

    }

    private void setupText() {
        
        textSteps = Main.createLabel("Steps: 0/0");
        textSteps.setVisible(true);
        textSteps.setForeground(Color.BLUE);
        textSteps.setLocation(Window.getXCenteredMaze(textSteps), 10);

        sprites.add(textSteps);
        textStatus = Main.createLabel(Status.UNSOLVABLE.toString());
        textStatus.setForeground(Color.RED);
        textStatus.setLocation(Window.getXCenteredMaze(textStatus), textSteps.getY() + textSteps.getHeight() + 10);
        sprites.add(textStatus);
    }

    private boolean allowInput() {

        if (!Main.firstMazeCreated) {
            return false;
        }
        else if (Config.multiplayer) {

            if (!Main.tcCountdown.finished) {
                return false;
            }
            if (!Main.mazeLeft.animationsFinished() || !Main.mazeRight.animationsFinished()) {
                // prevent moving until both players can move
                return false;
            }
        }

        return animationsFinished() &&
                Main.mazeGenThreadDone &&
                !gameOver;
    }

    private boolean allowReset() {
        return Main.firstMazeCreated &&
                animationsFinished() &&
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

            @Override
            public void onFinish() { updateMirror(); }
        };

        tcReset.finished = true;    // used to allow manual stepping
    }

    private void setupTimerSpawnPlayer() {
        tcSpawnPlayer = new TimedCounter(0.5f, 15) {
            @Override
            public void onStart() {
                player.setSize(1, 1);       // prevent spawning full sized from previous spawn
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

            @Override
            public void onFinish() { updateMirror(); }
        };

        tcSpawnPlayer.finished = true;

    }

    private void setupTimerNewMaze() {
        tcNewMaze = new TimedCounter(10) {
            public void onStart() {               
                setFramesAndPreserveFPS(nodesToChange.size());

                // call onFinish() after one frame's delay
                setOnFinishDelay((float)oneFrameInSeconds());
            }

            public void onTick() {
                Node node = nodesToChange.get(frame-1);
                // maze.set(node, newMaze.get(node));
                
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
                
                Main.textStatus.setVisible(false);  // NOTE: is hidden twice if multiplayer
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

        textStatus.setVisible(false);
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

    public void testGameOver() {
        if (Config.showUnsolvable && solver.findShortestPath().size() == 0) {
            
            if (maze.get(maze.currentNode) == Node.Type.END) {
                updateTextStatus(Status.INCOMPLETE);
            }
            else {
                updateTextStatus(Status.UNSOLVABLE);
            }
            gameOver = true;
        }
    }

    public void tryToMove(Maze.Direction dir) {

        if (!allowInput()) {
            return;
        }

        Node lastNode = maze.currentNode;

        if (maze.userMove(dir)) {

            updateTextSteps();

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

            int stepsMax = Main.mazeLeft.maze.creationPath.size();     // both have same max steps

            if (maze.pathHistory.size() == stepsMax) {

                if (Config.multiplayer) {
                    int winner = this == Main.mazeLeft ? 1 : 2;
                    Main.updateTextStatus("Winner: player " + winner);
                }
                else {
                    updateTextStatus(Status.COMPLETE);
                }
                Main.firstMazeCreated = false;  // prevents moving
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
            
            updateTextSteps();

            removeHintTexts();
            player.move(dir);
            mirrorPlayer(dir);
            refreshBlockGraphics(lastNode);

            if (direction == -1) {

                if (gameOver) {
                    gameOver = false;
                    textStatus.setVisible(false);
                }
                refreshBlockGraphics(maze.currentNode);
            }
            else {
                testGameOver();
            }
        }
        
    }

    private void updateTextSteps() {
        int steps = maze.pathHistory.size();
        int max = maze.creationPath.size();
        textSteps.setText("Steps: " + steps + "/" + max);
        textSteps.setSize(textSteps.getPreferredSize());
        textSteps.setLocation(Window.getXCenteredMaze(textSteps), 10);
    }

    private void updateTextStatus(Status status) {
        textStatus.setText(status.toString());
        textStatus.setForeground(status.color);
        textStatus.setSize(textStatus.getPreferredSize());
        textStatus.setLocation(Window.getXCenteredMaze(textStatus), textSteps.getY() + textSteps.getHeight() + 10);
        textStatus.setVisible(true);
    }

    public void updateMirror() {

        if (this == Main.mazeRight && Config.mirrorRightMaze != isMirrored) {

            if (animationsFinished() && Main.firstMazeCreated) {
                Main.updateTextStatus("Mirroring...");
                setMaze(maze);
                isMirrored = Config.mirrorRightMaze;
            }
        }
        else {
            Main.textStatus.setVisible(false);
        }
    }

    // also resets texts
    public void setMaze(Maze maze) {
        
        if (!animationsFinished() || (this == Main.mazeRight && !Config.multiplayer)) {
            return;
        }

        oldMaze = this.maze;
        this.maze = new Maze(maze);

        if (this == Main.mazeRight) {
            if (Config.mirrorRightMaze) {
                // mirror first time
                this.maze.mirror();
                isMirrored = Config.mirrorRightMaze;
            }
            else if (Config.mirrorRightMaze != isMirrored) {
                // revert mirror (parameter `maze` is already mirrored, called from updateMirror())
                this.maze.mirror();
            }
        }

        removeHintTexts();
        gameOver = false;
        
        updateTextSteps();

        solver = new MazeSolver(maze);

        if (this.maze.width != oldMaze.width || this.maze.height != oldMaze.height) {
            for (Block[] row : blocks) {
                for (Block spr : row) {
                    sprites.remove(spr);
                }
            }
            sprites.repaint();      // some old blocks are still visible
            setStartPosition();
            
            blocks = new Block[this.maze.height][this.maze.width];
            oldMaze = new Maze(this.maze.width, this.maze.height, Node.Type.WALL);

            player.setVisible(false);
            createWallBlocks();
        }

        // determine which nodes should change
        nodesToChange.clear();
        for (int y=0; y<this.maze.height; y++) {
            for (int x=0; x<this.maze.width; x++) {
                
                Node node = new Node(x, y);
                
                if (this.maze.get(node) != oldMaze.get(node) || node.equals(oldMaze.currentNode)) {
                    nodesToChange.add(node);
                }
            }
        }

        if (nodesToChange.size() == 0) {
            // when switching to multiplayer without any maze generated
            textStatus.setVisible(false);
            return;
        }

        if (Main.ENABLE_ANIMATIONS) {
            tcNewMaze.start();
        }
        else {
            Main.textStatus.setVisible(false);
            player.setVisible(true);
            movePlayerGraphicsTo(this.maze.startNode);
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
