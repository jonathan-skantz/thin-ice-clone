import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MazeContainer {
    
    private Maze maze;
    private Maze oldMaze;
    private MazeSolver solver;

    private TimedCounter tcReset;
    private TimedCounter tcSpawnPlayer;
    private TimedCounter tcNewMaze;
    private LinkedList<Node> nodesToChange = new LinkedList<>();

    public boolean isMirrored;

    public Status status;
    
    private Block player;
    private Block[][] blocks;
    private JLabel textSteps;
    public JLabel textUser;

    private JLabel textStatus;
    private JLabel textHelp;
    
    private LinkedHashMap<JLabel, Node> hints = new LinkedHashMap<>();

    public JPanel sprites = new JPanel(null);
    public JPanel mazePanel = new JPanel(null);

    public JPanel panelStatus = new  JPanel();

    private static final Color COLOR_GREEN = new Color(50, 150, 50);
    private static final Color COLOR_ORANGE = new Color(200, 150, 50);
    private static final Color COLOR_RED = new Color(200, 50, 50);
    private static final Color COLOR_INFO = new Color(50, 50, 50);

    private static final String STRING_NEW_MAZE = "Press " + KeyEvent.getKeyText(KeyHandler.Action.MAZE_NEW.keyCode) + " to generate new maze.";

    public enum Status {
        WAITING_FOR_FIRST_MAZE(COLOR_INFO, "First maze not generated.", STRING_NEW_MAZE),
        WAITING_FOR_OPPONENT(COLOR_INFO, "Waiting for opponent...", STRING_NEW_MAZE),
        GAME_WON(COLOR_GREEN, "Game won.", STRING_NEW_MAZE),
        INCOMPLETE(COLOR_ORANGE),
        UNSOLVABLE(COLOR_RED),
        GAME_LOST(COLOR_RED, "Game lost.", STRING_NEW_MAZE),
        SURRENDERED(COLOR_RED, "Surrendered.", STRING_NEW_MAZE),
        
        READY(COLOR_GREEN),
        // TODO: keybinds can change
        NOT_READY(COLOR_INFO, "Press " + KeyEvent.getKeyText(KeyHandler.Action.P1_READY.keyCode) + " to begin.",
                                "Press " + KeyEvent.getKeyText(KeyHandler.Action.P1_SURRENDER.keyCode) + " to surrender."),
        NOT_READY_P2(COLOR_INFO, "Press " + KeyEvent.getKeyText(KeyHandler.Action.P2_READY.keyCode) + " to begin.",
                                "Press " + KeyEvent.getKeyText(KeyHandler.Action.P2_SURRENDER.keyCode) + " to surrender."),
        NOT_READY_OPPONENT(COLOR_INFO, "Waiting for opponent..."),
        PLAYING(COLOR_GREEN),
        
        // info
        RESETTING(COLOR_INFO, "Resetting..."),
        GENERATING(COLOR_INFO, "Generating..."),
        MIRRORING(COLOR_INFO, "Mirroring..."),
        COPYING(COLOR_INFO, "Copying...");

        public final Color color;
        public final String stringStatus;
        public final String stringHelp;

        private Status(Color color, String textStatus) {
            this(color, textStatus, null);
        }

        private Status(Color color, String stringStatus, String stringHelp) {
            this.color = color;
            this.stringStatus = stringStatus;
            this.stringHelp = stringHelp;
        }

        // default display is the capitalized name of the enum field
        private Status(Color color) {
            this.color = color;
            String d = toString().toLowerCase();
            d = String.valueOf(d.charAt(0)).toUpperCase() + d.substring(1);
            stringStatus = d;
            stringHelp = null;
        }

        public boolean hidesInfo() {
            return this == GENERATING || this == RESETTING || this == MIRRORING || this == READY || stringHelp == null;
        }

        public boolean allowsStep() {
            return this == RESETTING || this == PLAYING || this == INCOMPLETE || this == UNSOLVABLE;
        }

        public boolean allowsHints() {
            return this == PLAYING;
        }

        public boolean allowsReset() {
            return this == PLAYING || this == INCOMPLETE || this == UNSOLVABLE;
        }

        public boolean allowsReady() {
            return this == NOT_READY || this == NOT_READY_OPPONENT || this == NOT_READY_P2;
        }

        public boolean allowsSurrender() {
            return this == PLAYING || this == INCOMPLETE || this == UNSOLVABLE || this == NOT_READY || this == NOT_READY_P2 || this == NOT_READY_OPPONENT;
        }

        public boolean allowsNewMaze() {
            return this == WAITING_FOR_FIRST_MAZE || this == WAITING_FOR_OPPONENT || this == GAME_WON || this == GAME_LOST || this == SURRENDERED;
        }

        public boolean allowsMove() {
            return this == PLAYING;
        }
    }

    public MazeContainer(int windowX) {

        // set up panels
        maze = new Maze(MazeGen.width, MazeGen.height, Node.Type.WALL);
        mazePanel.setSize(maze.width * Config.blockSize, maze.height * Config.blockSize);
        setStartPosition();

        sprites.setOpaque(false);   // allows buttons to be drawn underneath

        panelStatus.setLayout(new BoxLayout(panelStatus, BoxLayout.Y_AXIS));
        panelStatus.setBackground(new Color(240, 240, 240));
        sprites.add(panelStatus);       // NOTE: panelStatus size is (0,0) since the labels contain no letters (set in setStatus())

        player = new Block("src/textures/player.png", Config.blockSize - 2);
        player.velocity = Config.blockSize;
        player.setVisible(false);
        mazePanel.add(player);

        setupText();

        createWallBlocks();

        sprites.setBounds(windowX, 0, Window.mazeWidth, Window.mazeHeight);
        
        sprites.add(mazePanel);
        Window.sprites.add(sprites);

        setupTimerReset();
        setupTimerSpawnPlayer();
        setupTimerNewMaze();

        setStatus(Status.WAITING_FOR_FIRST_MAZE);
    }

    public void updateAnimationsEnabled() {
        tcReset.skipOnStart = !Config.hostShowAnimations;
        tcNewMaze.skipOnStart = !Config.hostShowAnimations;
        tcSpawnPlayer.skipOnStart = !Config.hostShowAnimations;
    }

    private void setupText() {
        
        textSteps = new JLabel("Steps: 0/0");
        textSteps.setFont(Main.font);
        textSteps.setSize(textSteps.getPreferredSize());
        textSteps.setForeground(new Color(0, 0, 0, 100));
        textSteps.setLocation(Window.getXCenteredMaze(textSteps), mazePanel.getY() - textSteps.getHeight() - 10);
        sprites.add(textSteps);

        textStatus = new JLabel();
        textStatus.setFont(Main.fontInfo);
        textStatus.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        panelStatus.add(textStatus);

        textHelp = new JLabel();
        textHelp.setFont(Main.fontInfo);
        textHelp.setForeground(COLOR_INFO);
        textHelp.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        panelStatus.add(textHelp);

        textUser = new JLabel("Singleplayer");
        textUser.setFont(Main.font);
        textUser.setSize(textUser.getPreferredSize());
        textUser.setForeground(new Color(0, 0, 0, 100));
        updateTextUserPosition();
        sprites.add(textUser);
    }

    public void setUserText(String text) {
        textUser.setText(text);
        textUser.setSize(textUser.getPreferredSize());
        updateTextUserPosition();        
        textUser.setVisible(true);
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
                step(-1);
                movePlayerGraphicsTo(maze.currentNode);
            }

            @Override
            public void onFinish() {
                setStatus(Status.PLAYING);  // last status must have been PLAYING, since only that allows resetting
                updateMirror();
            }

            @Override
            public void onSkip() {

                ArrayList<Node> changed = new ArrayList<>(maze.pathHistory);
                maze.reset();

                for (Node node : changed) {
                    refreshBlockGraphics(node);
                }

                movePlayerGraphicsTo(maze.currentNode);

                onFinish();
            }
        };

        tcReset.finished = true;    // used to allow manual stepping
        tcReset.skipOnStart = !Config.hostShowAnimations;
    }

    private void setupTimerSpawnPlayer() {

        MazeContainer thisContainer = this;

        tcSpawnPlayer = new TimedCounter(0.5f, 15) {
            @Override
            public void onStart() {
                player.setSize(1, 1);       // prevent spawning full sized from previous spawn
                mirrorPlayer(null);
                movePlayerGraphicsTo(maze.currentNode);
                player.setVisible(true);
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
            public void onFinish() {
                updateMirror();
                onSkip();
            }

            @Override
            public void onSkip() {

                if (status == Status.WAITING_FOR_FIRST_MAZE) {
                    System.out.println(Main.mazeLeft.status);
                    System.out.println(Main.mazeRight.status);
                    return;
                }

                mirrorPlayer(null);
                movePlayerGraphicsTo(maze.startNode);
                player.setVisible(true);

                if (thisContainer == Main.mazeLeft) {
                    if (Main.mazeLeft.status != Status.READY) {
                        setStatus(Status.NOT_READY);
                    }
                }
                else {
                    if (Main.mazeRight.status != Status.READY) {
                        if (Config.multiplayerOnline) {
                            Main.mazeRight.setStatus(Status.NOT_READY_OPPONENT);
                        }
                        else if (Config.multiplayerOffline) {
                            Main.mazeRight.setStatus(Status.NOT_READY_P2);
                        }
                    }
                }
            }
        };

        tcSpawnPlayer.finished = true;
        tcSpawnPlayer.skipOnStart = !Config.hostShowAnimations;
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
                onReset();

                if (maze.currentNode == null) {
                    setStatus(Status.WAITING_FOR_FIRST_MAZE);
                }
                else {
                    tcSpawnPlayer.start();
                }
            }
            
            @Override
            public void onReset() {
                if (nodesToChange.size() == 0) {
                    return;
                }
                Node node;
                if (frame == 0) {
                    node = nodesToChange.get(0);
                }
                else {
                    node = nodesToChange.get(frame-1);
                }
                blocks[node.Y][node.X].setBorder(null);
            }

            @Override
            public void onSkip() {               
                for (Node node : nodesToChange) {
                    refreshBlockGraphics(node);
                }
                tcSpawnPlayer.skip();
            }
        };

        tcNewMaze.finished = true;
        tcNewMaze.skipOnStart = !Config.hostShowAnimations;
    }

    // pauses animations
    private void freeze() {

        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                Block block = blocks[y][x];
                if (block.tcWater != null) {
                    block.tcWater.reset();
                }
            }
        }

        tcReset.reset();
        tcNewMaze.reset();
        tcSpawnPlayer.reset();

        tcReset.finished = true;
        tcNewMaze.finished = true;
        tcSpawnPlayer.finished = true;
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

    public boolean showHint() {

        if (!status.allowsHints()) {
            return false;
        }

        mazePanel.setVisible(false);
        
        // reset old hint blocks
        for (JLabel hintLabel : hints.keySet()) {
            mazePanel.remove(hintLabel);
        }

        hints.clear();

        // gets solution based on current node
        LinkedList<Node> path = Config.hintTypeLongest ? solver.findLongestPath() : solver.findShortestPath();

        Node removedFirst = path.removeFirst();

        int hintsLength = (int)(Config.hostHintLength * maze.creationPath.size());

        for (int hint=0; hint<hintsLength && hint<path.size(); hint++) {
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
            
            mazePanel.add(label);

            hints.put(label, step);
            mazePanel.setComponentZOrder(label, 1);
        }

        mazePanel.setVisible(true);
        return true;
        
    }

    private void updateTextUserPosition() {
        textUser.setLocation(Window.getXCenteredMaze(textUser), mazePanel.getY() + mazePanel.getHeight() + 10);
    }

    public void zoom(int ch) {
        player.velocity += ch;
          
        mazePanel.setSize(Config.blockSize * maze.width, Config.blockSize * maze.height);
        setStartPosition();

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

        updateTextUserPosition();
        textSteps.setLocation(textSteps.getX(), mazePanel.getY() - textSteps.getHeight() - 10);
        
    }

    // also resets maze
    public boolean resetMazeGraphics() {

        if (!status.allowsReset()) {
            return false;
        }

        else if (maze.pathHistory.size() == 1) {
            // history is empty, no reason to reset
            return false;
        }

        setStatus(Status.RESETTING);

        removeHintTexts();

        tcReset.start();

        return true;
    }

    public void testGameOver() {
        
        // first maze hasn't been set yet
        if (solver == null) {
            return;
        }
        
        if (solver.findShortestPath().size() == 0) {
            
            if (maze.get(maze.currentNode) == Node.Type.END) {
                setStatus(Status.INCOMPLETE);
            }
            else if (Config.hostShowUnsolvable) {
                setStatus(Status.UNSOLVABLE);
            }
        }
    }

    public boolean tryToMove(Maze.Direction dir) {

        if (!status.allowsMove()) {
            return false;
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
                    mazePanel.remove((JLabel)labels[0]);
                    hints.remove(labels[0]);
                }
                else {
                    for (JLabel hintLabel : hints.keySet()) {
                        mazePanel.remove(hintLabel);
                    }
                    hints.clear();
                    mazePanel.repaint();   // some labels are still visible
                }
            }

            int stepsMax = Main.mazeLeft.maze.creationPath.size();     // both have same max steps

            if (maze.pathHistory.size() == stepsMax) {
                setStatus(Status.GAME_WON);
                MazeContainer other = this == Main.mazeLeft ? Main.mazeRight : Main.mazeLeft;
                other.setStatus(MazeContainer.Status.GAME_LOST);

                // enable UI
                for (Component comp : UI.buttons.getComponents()) {
                    comp.setEnabled(true);
                }
            }
            else {
                testGameOver();
            }

            return true;
        }

        return false;

    }

    public boolean step(int direction) {

        if (!status.allowsStep()) {
            return false;
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

                if (status == Status.INCOMPLETE || status == Status.UNSOLVABLE) {
                    setStatus(Status.PLAYING);
                }
                refreshBlockGraphics(maze.currentNode);
            }
            else {
                testGameOver();
            }

            return true;
        }

        return false;
        
    }

    private void updateTextSteps() {
        int steps = maze.pathHistory.size();
        int max = maze.creationPath.size();
        textSteps.setText("Steps: " + steps + "/" + max);
        textSteps.setSize(textSteps.getPreferredSize());
        textSteps.setLocation(Window.getXCenteredMaze(textSteps), textSteps.getY());
    }

    public void setStatus(Status status) {
        this.status = status;

        if (status.stringStatus == null) {
            textStatus.setVisible(false);
        }
        else {
            textStatus.setText(status.stringStatus);
            textStatus.setForeground(status.color);
            textStatus.setSize(textStatus.getPreferredSize());
            textStatus.setVisible(true);
        }
        textStatus.revalidate();        // since `panelStatus` and `textHelp` may have been resized

        if (status.hidesInfo() || Config.multiplayerOnline && this == Main.mazeRight) {
            textHelp.setVisible(false);
        }
        else {
            textHelp.setText(status.stringHelp);
            textHelp.setSize(textHelp.getPreferredSize());
            textHelp.setVisible(true);
        }
        panelStatus.setVisible(true);
        panelStatus.setSize(panelStatus.getPreferredSize());
        panelStatus.setLocation(Window.getXCenteredMaze(panelStatus), 10);
    }

    public void updateMirror() {

        if (status == Status.WAITING_FOR_FIRST_MAZE) {
            return;
        }

        if (this == Main.mazeRight && Config.hostMirrorOpponent != isMirrored) {

            if (animationsFinished()) {
                setStatus(Status.MIRRORING);
                setMaze(maze);
                isMirrored = Config.hostMirrorOpponent;
            }
        }
    }

    // used to clear mazeRight when opponent disconnects
    public void clearMaze() {
        
        freeze();
        
        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                blocks[y][x].clearFrost(new Node(x, y));
                blocks[y][x].setType(Node.Type.WALL);
            }
        }

        maze = new Maze(maze.width, maze.height, Node.Type.WALL);
        oldMaze = new Maze(maze);

        player.setVisible(false);
        removeHintTexts();
    }

    // also resets texts
    public void setMaze(Maze maze) {

        oldMaze = this.maze;
        this.maze = new Maze(maze);

        if (this == Main.mazeRight) {
            if (Config.hostMirrorOpponent) {
                // mirror first time
                this.maze.mirror();
                isMirrored = Config.hostMirrorOpponent;
            }
            else if (Config.hostMirrorOpponent != isMirrored) {
                // revert mirror (parameter `maze` is already mirrored, called from updateMirror())
                this.maze.mirror();
            }
        }

        removeHintTexts();
        
        updateTextSteps();

        solver = new MazeSolver(this.maze);

        if (this.maze.width != oldMaze.width || this.maze.height != oldMaze.height) {
            
            mazePanel.setVisible(false);

            for (Block[] row : blocks) {
                for (Block spr : row) {
                    mazePanel.remove(spr);
                }
            }
            mazePanel.setSize(maze.width * Config.blockSize, maze.height * Config.blockSize);
            setStartPosition();
            
            blocks = new Block[this.maze.height][this.maze.width];
            oldMaze = new Maze(this.maze.width, this.maze.height, Node.Type.WALL);

            player.setVisible(false);
            createWallBlocks();
            mazePanel.setVisible(true);
        }

        updateTextUserPosition();

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

        if (nodesToChange.size() == 0 || (nodesToChange.size() == 1 && nodesToChange.getFirst().equals(oldMaze.currentNode))) {
            if (status != Status.MIRRORING) {
                tcSpawnPlayer.onFinish();
            }
            return;
        }

        tcNewMaze.start();
            
    }

    private void removeHintTexts() {
        for (JLabel hint : hints.keySet()) {
            mazePanel.remove(hint);
        }
        hints.clear();

        mazePanel.repaint();   // some hints are still visible
    }

    public void movePlayerGraphicsTo(Node node) {
        int blockX = node.X * Config.blockSize;
        int blockY = node.Y * Config.blockSize;
    
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
                mazePanel.add(block);
            }
        }
    }

    private void setStartPosition() {
        int startX = (Window.mazeWidth - Config.blockSize * maze.width) / 2;
        int startY = (Window.height - Config.blockSize * maze.height) / 2;
        mazePanel.setLocation(startX, startY);
    }

    public Point getBlockPosition(Node node) {
        int posX = node.X * Config.blockSize;
        int posY = node.Y * Config.blockSize;
        return new Point(posX, posY);
    }


}
