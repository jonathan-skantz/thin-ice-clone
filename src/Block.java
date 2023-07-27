import javax.swing.JComponent;
import javax.swing.border.Border;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

public class Block extends JComponent {
    
    public int velocity;

    public static Window window;

    private BufferedImage bottomImageOriginal;
    private BufferedImage bottomImage;
    private BufferedImage combinedImage;

    private BufferedImage[] frostImages = new BufferedImage[4];  // top, bottom, left, right
    private static int[] rotateDegrees = new int[]{0, 180, -90, 90};

    public String path;

    private TimedCounter tc;
    private int startX;
    private boolean mirrored;

    private static Random rand = new Random();

    public Block(Node.Type type, int size) {
        createBlock(typeToPath(type), size);
    }

    public Block(String path, int size) {
        createBlock(path, size);
    }

    private String typeToPath(Node.Type type) {
        return "src/textures/" + type.name().toLowerCase() + ".png";
    }

    private void createBlock(String path, int size) {

        this.path = path;

        bottomImage = Image.resize(path, size, size);
        bottomImageOriginal = Image.copy(bottomImage);
        combinedImage = Image.copy(bottomImage);

        Window.sprites.add(this);
        setBounds(0, 0, size, size);

        beginAnimation();
    }

    // moving water animation
    private void beginAnimation() {
        if (path.endsWith("blocked.png")) {
            int y = rand.nextInt(getHeight()/2);
            tc = new TimedCounter(-1, 5) {
                @Override
                public void onTick() {
                    bottomImage = Image.repeat(bottomImageOriginal, startX, y);
                    combineFrost();
                    if (++startX >= getWidth()) {
                        startX = 0;
                    }
                }
            };
            tc.start();
        }
        else if (tc != null) {
            tc.reset();
        }
    }

    public void setMirrored(boolean bool) {
        
        if ((mirrored && !bool) || (!mirrored && bool)) {
            mirrored = bool;
            bottomImage = Image.mirror(bottomImage, true, false);
            bottomImageOriginal = Image.copy(bottomImage);
            combineFrost();
        }
        
    }

    public void setType(Node.Type type) {
        path = typeToPath(type);
        bottomImage = Image.resize(typeToPath(type), getWidth(), getHeight());
        bottomImageOriginal = Image.copy(bottomImage);
        combineFrost();

        beginAnimation();
    }

    public void setFrost(Node thisNode, Node frostNeighbor, boolean visible) {
        int i;
        if (thisNode.Y < frostNeighbor.Y) i = 0;
        else if (thisNode.Y > frostNeighbor.Y) i = 1;
        else if (thisNode.X < frostNeighbor.X) i = 2;
        else i = 3;

        int degrees = rotateDegrees[i];

        if (visible) {
            BufferedImage frost = Image.resize("src/textures/frost.png", getWidth(), getHeight());
            frostImages[i] = Image.rotate(frost, degrees);
        }
        else {
            frostImages[i] = null;
        }

        combineFrost();
    }

    private void combineFrost() {
        // refresh combined image
        combinedImage = Image.copy(bottomImage);
        Graphics2D g = combinedImage.createGraphics();


        for (BufferedImage img : frostImages) {
            g.drawImage(img, 0, 0, null);
        }

        g.dispose();
        repaint();
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        
        bottomImage = Image.resize(path, width, height);   // copies original in order to resize the best version
        if (mirrored) {
            bottomImage = Image.mirror(bottomImage, true, false);
        }
        bottomImageOriginal = Image.copy(bottomImage);

        for (int i=0; i<frostImages.length; i++) {
            BufferedImage frost = frostImages[i];
            if (frost != null) {
                frost = Image.resize("src/textures/frost.png", width, height);
                frostImages[i] = Image.rotate(frost, rotateDegrees[i]);
            }
        }
        combineFrost();
    }

    @Override
    public void paintComponent(Graphics g) {
        
        super.paintComponent(g);
        
        g.drawImage(combinedImage, 0, 0, null);

        Border thisBorder = getBorder();
        if (thisBorder != null) {
            thisBorder.paintBorder(this, g, 0, 0, getWidth(), getHeight());
        }
    }

    /**
     * Move the sprite to the center of the window.
     */
    public void moveToCenter() {
        int x = (Window.width - getWidth()) / 2;
        int y = (Window.height - getHeight()) / 2;
        setLocation(x, y);
    }

    /**
     * Move the sprite according to its velocity and current direction.
     */
    public void move(Maze.Direction dir) {

        int x = getX();
        int y = getY();

        switch (dir) {

            case UP:
                y -= velocity;
                break;

            case DOWN:
                y += velocity;
                break;

            case LEFT:
                x -= velocity;
                break;

            case RIGHT:
                x += velocity;
                break;
            
            default:
                break;
        }

        setLocation(x, y);
    }

}
