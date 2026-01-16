package com.jonathanskantz.thiniceclone;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MazeContainer {
    
    public Maze maze;
    private Maze oldMaze;
    private MazeSolver solver;

    private LinkedList<Node> longestPathOld = new LinkedList<>();

    private TimedCounter tcReset;
    private TimedCounter tcSpawnPlayer;
    private TimedCounter tcNewMaze;
    private LinkedList<Node> nodesToChange = new LinkedList<>();

    public boolean isMirrored;

    public Status status;
    public Status statusAfterAnimation;

    private Block blockPlayer;
    private Block[][] blocks;
    private JLabel textPoints;
    public JLabel textUser;

    private JLabel labelGamesWon;
    public int gamesWon;

    private JLabel textStatus;
    
    private LinkedHashMap<Node, JLabel> hints = new LinkedHashMap<>();
    private int hintsUsed;

    public JPanel sprites = new JPanel(null);
    public JPanel panelMaze = new JPanel(null);

    public JPanel panelStatus = new JPanel();
    public JPanel panelDisconnected = new JPanel();     // gray overlay on panelMaze

    public PlayerRole playerRole;

    public enum PlayerRole {
        P1_SINGLEPLAYER,
        P1_HOST,
        P1_CLIENT,
        P1_LOCAL,

        P2_HOST,
        P2_LOCAL,

        OPPONENT;

        public boolean isP1() {
            return this == P1_SINGLEPLAYER || this == P1_HOST || this == P1_CLIENT || this == P1_LOCAL;
        }

        public boolean isP2() {
            return this == P2_HOST || this == P2_LOCAL;
        }
    }

    public MazeContainer(int windowX, PlayerRole playerRole) {

        this.playerRole = playerRole;

        // set up panels
        maze = new Maze(MazeGen.width, MazeGen.height, Node.Type.WALL);
        panelMaze.setSize(maze.width * Config.blockSize, maze.height * Config.blockSize);
        setStartPosition();

        sprites.setOpaque(false);   // allows buttons to be drawn underneath

        panelStatus.setLayout(new BoxLayout(panelStatus, BoxLayout.Y_AXIS));
        panelStatus.setBackground(new Color(240, 240, 240));
        sprites.add(panelStatus);       // NOTE: panelStatus size is (0,0) since the labels contain no letters (set in setStatus())

        blockPlayer = new Block("/textures/player.png", Config.blockSize - 2);
        blockPlayer.velocity = Config.blockSize;
        blockPlayer.setVisible(false);
        panelMaze.add(blockPlayer);

        setupText();

        createWallBlocks();

        sprites.setBackground(null);
        sprites.setBounds(windowX, 0, Window.mazeWidth, Window.mazeHeight);
        
        sprites.add(panelMaze);
        Window.sprites.add(sprites);

        setupTimerReset();
        setupTimerSpawnPlayer();
        setupTimerNewMaze();

        setStatus(Status.MAZE_EMPTY);

        panelDisconnected.setBackground(new Color(150, 150, 150, 150));
        panelDisconnected.setSize(panelMaze.getSize());

        panelDisconnected.setVisible(false);
        panelMaze.add(panelDisconnected);
        panelMaze.setComponentZOrder(panelDisconnected, 0);
    }

    public void updateAnimationsEnabled() {
        tcReset.skipOnStart = !Config.Host.SHOW_ANIMATIONS.enabled;
        tcNewMaze.skipOnStart = !Config.Host.SHOW_ANIMATIONS.enabled;
        tcSpawnPlayer.skipOnStart = !Config.Host.SHOW_ANIMATIONS.enabled;
    }

    private void setupText() {
        
        textPoints = new JLabel("Points: 0/0");
        textPoints.setFont(App.font);
        textPoints.setSize(textPoints.getPreferredSize());
        textPoints.setForeground(new Color(0, 0, 0, 100));
        textPoints.setLocation(Window.getXCenteredMaze(textPoints), panelMaze.getY() - textPoints.getHeight() - 10);
        sprites.add(textPoints);

        textStatus = new JLabel();
        textStatus.setFont(App.fontInfo);
        textStatus.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        panelStatus.add(textStatus);

        textUser = new JLabel("Singleplayer");
        textUser.setFont(App.font);
        textUser.setSize(textUser.getPreferredSize());
        textUser.setForeground(new Color(0, 0, 0, 100));
        updateTextUserPosition();
        sprites.add(textUser);

        labelGamesWon = new JLabel();
        updateLabelGamesWon();
        sprites.add(labelGamesWon);

    }

    public void updateLabelGamesWon() {       
        labelGamesWon.setText("Games won: " + gamesWon);
        labelGamesWon.setSize(labelGamesWon.getPreferredSize());

        if (playerRole.isP1()) {
            labelGamesWon.setLocation(10, 10);
        }
        else {
            labelGamesWon.setLocation(sprites.getWidth()-labelGamesWon.getWidth()-10, 10);
        }
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
                setStatus(statusAfterAnimation);  // last status must have been PLAYING, since only that allows resetting
                updateMirror();

                solver.longestPath = new LinkedList<>(maze.creationPath);  // instead of solver.findLongestPath()
                updateTextPoints();
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
        tcReset.skipOnStart = !Config.Host.SHOW_ANIMATIONS.enabled;
    }

    private void setupTimerSpawnPlayer() {

        tcSpawnPlayer = new TimedCounter(0.5f, 15) {
            @Override
            public void onStart() {
                blockPlayer.setSize(1, 1);       // prevent spawning full sized from previous spawn
                mirrorPlayer(null);
                movePlayerGraphicsTo(maze.currentNode);
                blockPlayer.setVisible(true);
            }

            @Override
            public void onTick() {
                // resize
                float progress = (float)tcSpawnPlayer.frame / tcSpawnPlayer.frames;
                int size = (int)((Config.blockSize - 2) * progress);
                blockPlayer.setSize(size, size);
            
                movePlayerGraphicsTo(maze.currentNode);
            }

            @Override
            public void onFinish() {
                updateMirror();
                onSkip();
            }

            @Override
            public void onSkip() {

                if (maze.currentNode == null) {
                    return;
                }

                mirrorPlayer(null);
                movePlayerGraphicsTo(maze.startNode);
                blockPlayer.setVisible(true);

                setStatus(statusAfterAnimation);
            }
        };

        tcSpawnPlayer.finished = true;
        tcSpawnPlayer.skipOnStart = !Config.Host.SHOW_ANIMATIONS.enabled;
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
                    blockPlayer.setVisible(false);
                }

                refreshBlockGraphics(node);
                blocks[node.Y][node.X].setBorder(Config.BLOCK_BORDER_GEN);
            }

            @Override
            public void onFinish() {
                onReset();

                if (maze.currentNode == null) {
                    if (Config.multiplayer) {
                        if (playerRole.isP1()) {
                            setStatus(Status.MAZE_EMPTY);
                        }
                        else {
                            if (OnlineClient.connected) {
                                setStatus(Status.WAITING_FOR_HOST_TO_GENERATE);
                            }
                            else {
                                setStatus(Status.WAITING_FOR_HOST_TO_OPEN);
                            }
                        }
                    }
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
        tcNewMaze.skipOnStart = !Config.Host.SHOW_ANIMATIONS.enabled;
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
            blockPlayer.setMirrored(!maze.walkable(maze.currentNode.getNeighbor(Maze.Direction.LEFT)));
        }

        else if (playerMustMove(Maze.Direction.LEFT)) {
            blockPlayer.setMirrored(false);
        }
        else if (playerMustMove(Maze.Direction.RIGHT)) {
            blockPlayer.setMirrored(true);
        }
        else if (dir == Maze.Direction.LEFT) {
            blockPlayer.setMirrored(false);
        }
        else if (dir == Maze.Direction.RIGHT) {
            blockPlayer.setMirrored(true);
        }
        
    }

    public boolean showHint() {

        if (!status.allowsHints()) {
            return false;
        }

        int hintsLength = (int)(Config.Host.HINT_LENGTH.number * maze.creationPath.size());
        
        if (solver.longestPath.size() - maze.pathHistory.size() < hintsLength) {
            hintsLength = solver.longestPath.size() - maze.pathHistory.size();
        }

        if (hintsLength - hints.size() == 0) {
            return false;
        }

        hintsUsed += hintsLength - hints.size();
        updateTextPoints();

        panelMaze.setVisible(false);

        removeHintLabels();

        // TODO: doesnt work with shortestpath
        LinkedList<Node> path = Config.hintTypeLongest ? solver.longestPath : solver.findShortestPath();

        int iStart = maze.pathHistory.size();

        for (int hint=iStart; hint<iStart+hintsLength && hint<path.size(); hint++) {
            Node step = path.get(hint);
            
            JLabel label = new JLabel(String.valueOf(hint-iStart+1));
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setForeground(new Color(100, 100, 100));
            label.setFont(App.hintFont);
            label.setSize(Config.blockSize, Config.blockSize);

            label.setLocation(getBlockPosition(step));
            panelMaze.add(label);
            hints.put(step, label);
            panelMaze.setComponentZOrder(label, 1);

            int pathFirstAppearance = path.indexOf(step);

            if (pathFirstAppearance < iStart) {
                continue;
            }
            if (pathFirstAppearance != hint) {
                // second step on double
                Node nodeBeforeFirstStep;
                JLabel labelFirstStep;

                Object[] labels = hints.values().toArray();
                if (pathFirstAppearance == iStart - 1) {
                    nodeBeforeFirstStep = path.get(iStart);
                    labelFirstStep = (JLabel) labels[pathFirstAppearance-1];
                }
                else {
                    nodeBeforeFirstStep = path.get(pathFirstAppearance-1);
                    labelFirstStep = (JLabel) labels[pathFirstAppearance-iStart];
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
        }

        panelMaze.setVisible(true);
        return true;
        
    }

    private void updateTextUserPosition() {
        textUser.setLocation(Window.getXCenteredMaze(textUser), panelMaze.getY() + panelMaze.getHeight() + 10);
    }

    public void zoom(int ch) {
        blockPlayer.velocity += ch;
          
        panelMaze.setSize(Config.blockSize * maze.width, Config.blockSize * maze.height);
        setStartPosition();

        if (maze.currentNode != null) {
            blockPlayer.setSize(blockPlayer.getWidth() + ch, blockPlayer.getHeight() + ch);
            movePlayerGraphicsTo(maze.currentNode);
        }
        
        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                blocks[y][x].setSize(Config.blockSize, Config.blockSize);
                blocks[y][x].setLocation(getBlockPosition(new Node(x, y)));
            }
        }
        
        for (Node step : hints.keySet()) {
            JLabel label = hints.get(step);
            label.setSize(Config.blockSize, Config.blockSize);
            label.setFont(App.hintFont);
            label.setLocation(getBlockPosition(step));
        }

        updateTextUserPosition();
        textPoints.setLocation(textPoints.getX(), panelMaze.getY() - textPoints.getHeight() - 10);
        
    }

    // also resets maze
    public boolean resetMazeGraphics() {

        if (!status.allowsReset()) {
            return false;
        }
        else if (!Config.Host.ALLOW_RESETTING.enabled) {
            return false;
        }
        else if (maze.pathHistory.size() == 1) {
            // history is empty, no reason to reset
            return false;
        }

        statusAfterAnimation = Status.PLAYING;
        setStatus(Status.RESETTING);

        removeHintLabels();

        tcReset.start();

        return true;
    }

    public void testGameOver() {
        
        // first maze hasn't been set yet
        if (solver == null) {
            return;
        }
        
        if (maze.get(maze.currentNode) == Node.Type.END) {
            setStatus(Status.INCOMPLETE);   // if GAME_WON, testGameOver() would never have been called --> must be INCOMPLETE
            return;
        }

        if (Config.Host.SHOW_UNSOLVABLE.enabled) {

            if (!solver.longestPath.get(maze.pathHistory.size()-1).equals(maze.currentNode)) {
                // update longestPath since user stepped differently

                longestPathOld = new LinkedList<Node>(solver.longestPath);
                solver.findLongestPath();

                // prepend pathHistory (not including currentNode)
                for (int i=maze.pathHistory.size()-2; i>=0; i--) {
                    solver.longestPath.addFirst(maze.pathHistory.get(i));
                }
                
                if (solver.longestPath.size() < maze.creationPath.size()) {
                    setStatus(Status.UNSOLVABLE);
                    solver.longestPath.clear();
                }
            }
        }

    }

    private void updateMoveGraphics(Maze.Direction dir, Node lastNode) {
        updateTextPoints();

        blockPlayer.move(dir);
        mirrorPlayer(dir);
        refreshBlockGraphics(lastNode);

        if (hints.size() > 0) {

            Object[] nodes = hints.keySet().toArray();
            
            if (maze.currentNode.equals(nodes[0])) {
                JLabel label = hints.get(nodes[0]);
                panelMaze.remove((JLabel)label);
                hints.remove(nodes[0]);
            }
            else {
                removeHintLabels();
            }
        }
    }

    public boolean tryToMove(Maze.Direction dir) {

        if (!status.allowsMove()) {
            return false;
        }

        Node lastNode = maze.currentNode;

        if (maze.userMove(dir)) {

            updateMoveGraphics(dir, lastNode);

            int stepsMax = maze.creationPath.size();

            if (maze.pathHistory.size() == stepsMax) {
                
                MazeContainer other = playerRole.isP1() ? App.mazeRight : App.mazeLeft;

                if (hintsUsed <= other.hintsUsed || !Config.multiplayer) {
                    setStatus(Status.GAME_WON);
                    gamesWon++;
                    updateLabelGamesWon();
                    other.setStatus(Status.GAME_LOST);
                }
                else {
                    if (other.status == Status.POTENTIALLY_WON) {
                        setStatus(Status.GAME_LOST);
                        other.setStatus(Status.GAME_WON);
                        other.gamesWon++;
                        other.updateLabelGamesWon();
                    }
                    else {
                        setStatus(Status.POTENTIALLY_WON);
                    }
                }

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
        else if (status != Status.RESETTING && !Config.Host.ALLOW_UNDO_AND_REDO.enabled) {
            return false;
        }

        Node lastNode = maze.currentNode;

        Maze.Direction dir = maze.step(direction);

        if (dir != null) {
            
            updateMoveGraphics(dir, lastNode);

            if (direction == -1) {

                if (status == Status.INCOMPLETE || status == Status.UNSOLVABLE) {
                    setStatus(Status.PLAYING);
                    solver.longestPath = new LinkedList<Node>(longestPathOld);
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

    private void updateTextPoints() {
        int points = maze.pathHistory.size() - hintsUsed;
        int max = maze.creationPath.size();
        textPoints.setText("Points: " + points + "/" + max);
        textPoints.setSize(textPoints.getPreferredSize());
        textPoints.setLocation(Window.getXCenteredMaze(textPoints), textPoints.getY());
    }

    public void setStatus(Status status) {
        if (status == null) {
            this.status = null;
            panelStatus.setVisible(false);
            statusAfterAnimation = null;
            return;
        }

        if (status == statusAfterAnimation) {
            if (animationsFinished()) {
                statusAfterAnimation = null;
            }
            else {
                return;
            }
        }

        this.status = status;

        String result = Status.HTML_START;

        if (status.stringStatus != null) {
            result += status.stringStatus + "<br>";
        }
        textStatus.revalidate();        // since `panelStatus` and `textHelp` may have been resized

        if (playerRole.isP1()) {
            if (status.stringsP1.size() > 0) {
                for (String str : status.stringsP1) {
                    if (playerRole == PlayerRole.P1_CLIENT && str.equals(Status.stringNewMaze)) {
                        // stringNewMaze is only allowed by P1 host, not P1 client
                        continue;
                    }
                    else if (!Config.Host.ALLOW_UNDO_AND_REDO.enabled && (str.equals(Status.stringUndoP1) || str.equals(Status.stringRedoP1))) {
                        continue;
                    }
                    result += str + "<br>";
                }
            }
        }
        else if (playerRole.isP2()) {
            if (status.stringsP2.size() > 0 ) {
                for (String str : status.stringsP2) {
                    if (!Config.Host.ALLOW_UNDO_AND_REDO.enabled && (str.equals(Status.stringUndoP2) || str.equals(Status.stringRedoP2))) {
                        continue;
                    }
                    result += str + "<br>";
                }
            }
        }
        // else: is opponent, don't show any help

        if (result.endsWith("<br>")) {
            result = result.substring(0, result.length()-4);
        }

        result += Status.HTML_END;

        textStatus.setText(result);
        textStatus.setForeground(status.color);
        textStatus.setSize(textStatus.getPreferredSize());
        textStatus.setVisible(true);

        panelStatus.setSize(panelStatus.getPreferredSize());
        panelStatus.setLocation(Window.getXCenteredMaze(panelStatus), 10);
        panelStatus.setVisible(true);
    }

    public void updateMirror() {

        if (maze.currentNode == null) {
            return;
        }

        if (playerRole.isP2() && Config.Host.MIRROR_OPPONENT.enabled != isMirrored) {

            if (animationsFinished()) {
                if (!Status.ANIMATIONS.contains(status)) {
                    statusAfterAnimation = status;
                }
                setStatus(Status.MIRRORING);
                setMaze(maze);
                isMirrored = Config.Host.MIRROR_OPPONENT.enabled;
            }
        }
    }

    // used to clear mazeRight when opponent disconnects
    public void clearMaze() {
        
        tcNewMaze.reset();
        tcSpawnPlayer.reset();
        tcReset.reset();

        tcNewMaze.finished = true;
        tcSpawnPlayer.finished = true;
        tcReset.finished = true;
        
        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                blocks[y][x].clearFrost(new Node(x, y));
                blocks[y][x].setType(Node.Type.WALL);
            }
        }

        maze = new Maze(maze.width, maze.height, Node.Type.WALL);
        oldMaze = new Maze(maze);

        blockPlayer.setVisible(false);
        removeHintLabels();
    }

    // also resets texts
    public void setMaze(Maze maze) {

        if (!tcNewMaze.finished) {
            tcNewMaze.reset();
        }
        else if (!tcSpawnPlayer.finished) {
            tcSpawnPlayer.reset();
        }

        oldMaze = this.maze;
        this.maze = new Maze(maze);

        if (playerRole.isP2()) {
            if (Config.Host.MIRROR_OPPONENT.enabled) {
                // mirror first time
                this.maze.mirror();
                isMirrored = Config.Host.MIRROR_OPPONENT.enabled;
                if (!Status.ANIMATIONS.contains(status)) {
                    statusAfterAnimation = status;
                }
            }
            else if (Config.Host.MIRROR_OPPONENT.enabled != isMirrored) {
                // revert mirror (parameter `maze` is already mirrored, called from updateMirror())
                this.maze.mirror();
                if (!Status.ANIMATIONS.contains(status)) {
                    statusAfterAnimation = status;
                }
            }
        }

        removeHintLabels();
        hintsUsed = 0;
        updateTextPoints();

        solver = new MazeSolver(this.maze);
        solver.longestPath = new LinkedList<>(this.maze.creationPath);  // instead of solver.findLongestPath()

        if (this.maze.width != oldMaze.width || this.maze.height != oldMaze.height) {
            
            panelMaze.setVisible(false);

            for (Block[] row : blocks) {
                for (Block spr : row) {
                    panelMaze.remove(spr);
                }
            }
            panelMaze.setSize(maze.width * Config.blockSize, maze.height * Config.blockSize);
            setStartPosition();
            
            blocks = new Block[this.maze.height][this.maze.width];
            oldMaze = new Maze(this.maze.width, this.maze.height, Node.Type.WALL);

            blockPlayer.setVisible(false);
            createWallBlocks();
            panelMaze.setVisible(true);
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

    private void removeHintLabels() {
        for (JLabel hint : hints.values()) {
            panelMaze.remove(hint);
        }
        hints.clear();

        panelMaze.repaint();   // some hints are still visible
    }

    public void movePlayerGraphicsTo(Node node) {
        int blockX = node.X * Config.blockSize;
        int blockY = node.Y * Config.blockSize;
    
        int centeredX = blockX + (Config.blockSize - blockPlayer.getWidth()) / 2;
        int centeredY = blockY + (Config.blockSize - blockPlayer.getHeight()) / 2;

        blockPlayer.setLocation(centeredX, centeredY);
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
                Block block = new Block("/textures/wall.png", Config.blockSize);
                blocks[y][x] = block;
                block.setLocation(getBlockPosition(new Node(x, y)));
                panelMaze.add(block);
            }
        }
    }

    private void setStartPosition() {
        int startX = (Window.mazeWidth - Config.blockSize * maze.width) / 2;
        int startY = (Window.height - Config.blockSize * maze.height) / 2;
        panelMaze.setLocation(startX, startY);
    }

    public Point getBlockPosition(Node node) {
        int posX = node.X * Config.blockSize;
        int posY = node.Y * Config.blockSize;
        return new Point(posX, posY);
    }


    public void resetAll() {
        clearMaze();
        setStatus(null);
        gamesWon = 0;
        updateLabelGamesWon();
    }

}
