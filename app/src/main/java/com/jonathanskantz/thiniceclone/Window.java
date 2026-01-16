package com.jonathanskantz.thiniceclone;

/**
 * Class for creating and setting up a JFrame.
 */
import java.awt.Color;

import java.awt.Insets;
import java.awt.Point;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Window {
    
    private static final Color BG_COLOR = Color.LIGHT_GRAY;
    private static final String TITLE = "Thin Ice";
    
    public static int width = 500;        // actual canvas size, excluding border/insets
    public static int height = 500;

    public static int mazeWidth = 500;
    public static int mazeHeight = 500;

    public static JPanel sprites = new JPanel();   // a panel containing all sprites

    public static JFrame frame;     // only used publically in UI to add popup dialog

    public static void setup() {
        
        frame = new JFrame(TITLE);
        frame.addKeyListener(new KeyHandler());

        frame.setLocationRelativeTo(null);        // center the window on screen

        frame.setResizable(false);
        frame.setLayout(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        sprites.setLayout(null);

        frame.pack();   // so that insets are calculated
        Insets ins = frame.getInsets();
        
        int frameW = width + ins.left + ins.right;
        int frameH = height + ins.top + ins.bottom;
        frame.setSize(frameW, frameH);
        
        sprites.setBounds(0, 0, width, height);
        sprites.setBackground(BG_COLOR);        
        frame.add(sprites);   // finally add the panel
        
        frame.setVisible(true);
    }

    public static int getXCentered(JComponent comp) {
        return (width - comp.getWidth()) / 2;
    }

    public static int getYCentered(JComponent comp) {
        return (height - comp.getHeight()) / 2;
    }

    public static int getXCenteredMaze(JComponent comp) {
        return (mazeWidth - comp.getWidth()) / 2 ;
    }

    public static Point getXYCentered(JComponent comp) {
        return new Point(getXCentered(comp), getYCentered(comp));
    }

    public static void setSize(int width, int height) {
        Window.width = width;
        Window.height = height;
        
        Insets ins = frame.getInsets();
        int frameW = width + ins.left + ins.right;
        int frameH = height + ins.top + ins.bottom;
        frame.setSize(frameW, frameH);
        sprites.setSize(width, height);

        App.onResize();
    }

}
