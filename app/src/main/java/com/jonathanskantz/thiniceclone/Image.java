package com.jonathanskantz.thiniceclone;

import java.util.HashMap;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

/**
 * Collection of methods for manipulating images.
 */
public class Image {

    public static HashMap<String, BufferedImage> cached = new HashMap<>();

    // NOTE: saves the original and returns a copy
    public static BufferedImage load(String path) {


        //System.out.println(path);
        //path = getAbsPath(path);
        //System.out.println(path);

        if (!cached.containsKey(path)) {
            try {
                // save original with its original size (best resolution)
                //BufferedImage original = ImageIO.read(new File(path));
                InputStream is = App.class.getResourceAsStream(path);
                BufferedImage original = ImageIO.read(is);
                BufferedImage converted = convert(original, getTypeFromPath(path));
                cached.put(path, converted);
            }
            catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return copy(cached.get(path));  // to prevent modifying the original
    }

    private static String getAbsPath(String path) {

        Path cwd = Paths.get(System.getProperty("user.dir"));

        // .getFileName() returns the last folder
        if (!cwd.getFileName().toString().equals("src")) {
            // prepend "src" if current working directory is not "src"
            path = "src/" + path;
        }

        return new File(path).getAbsolutePath().toLowerCase();
    }

    private static int getTypeFromPath(String path) {
        if (path.toLowerCase().endsWith(".png")) {
            return BufferedImage.TYPE_INT_ARGB;
        }
        return BufferedImage.TYPE_INT_RGB;
    }

    // returns an image with the original offset by `startX` and `startY`,
    // and the void filled by drawing copies of `image`
    public static BufferedImage repeat(BufferedImage image, int startX, int startY) {
        
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D g = result.createGraphics();
        g.drawImage(image, startX, startY, null);
        
        int firstStartX = startX;
        int slices = 0;
        
        // NOTE: draws either one or three slices
        if (startX != 0) {

            if (startX > 0) {
                startX -= image.getWidth();
            }
            else if (startX < 0) {
                startX = image.getWidth() + startX;     // NOTE: startX is negative
            }

            g.drawImage(image, startX, startY, null);
            slices++;
        }
        
        if (startY != 0) {
            
            if (startY > 0) {
                startY -= image.getHeight();
            }
            else if (startY < 0) {
                startY = image.getHeight() + startY;
            }
            
            g.drawImage(image, startX, startY, null);
            slices++;
        }
        
        if (slices == 2) {
            // must draw a third
            startX = firstStartX;
            g.drawImage(image, startX, startY, null);
        }

        g.dispose();

        return result;
    }

    // combined underlying method
    private static BufferedImage modify(BufferedImage image, int width, int height, int type) {
        BufferedImage copy = new BufferedImage(width, height, type);
        Graphics2D g = copy.createGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return copy;
    }

    public static BufferedImage copy(BufferedImage image) {
        return modify(image, image.getWidth(), image.getHeight(), image.getType());
    }

    public static BufferedImage convert(BufferedImage image, int type) {
        return modify(image, image.getWidth(), image.getHeight(), type);
    }

    // resizes `image`, even if it was not original quality
    public static BufferedImage resize(BufferedImage image, int width, int height) {
        return modify(image, width, height, image.getType());
    }

    // loads best quality, then resizes
    public static BufferedImage resize(String path, int width, int height) {
        BufferedImage original = load(path);
        return modify(original, width, height, original.getType());
    }

    public static BufferedImage rotate(BufferedImage img, double degrees) {

        int oldWidth = img.getWidth();
        int oldHeight = img.getHeight();

        int newWidth;
        int newHeight;
        
        int type;

        AffineTransform transform = new AffineTransform();
        double radians = Math.toRadians(degrees);

        if (degrees % 90 == 0) {
            newWidth = oldWidth;
            newHeight = oldHeight;
            type = img.getType();
        }
        else {
            // calculate new dimensions of rotated image
            double sin = Math.abs(Math.sin(radians));
            double cos = Math.abs(Math.cos(radians));
            
            newWidth = (int) (oldWidth * cos + oldHeight * sin);
            newHeight = (int) (oldHeight * cos + oldWidth * sin);
            type = BufferedImage.TYPE_INT_ARGB; // will otherwise leave black void
            
            // set anchor point to center of image
            transform.translate((newWidth - oldWidth) / 2, (newHeight - oldHeight) / 2);
        }

        // apply rotation
        BufferedImage rotatedImg = new BufferedImage(newWidth, newHeight, type);
        Graphics2D g = rotatedImg.createGraphics();
        transform.rotate(radians, oldWidth/2, oldHeight/2);
        g.drawImage(img, transform, null);
        g.dispose();

        return rotatedImg;
    }

	public static BufferedImage mirror(BufferedImage img, boolean x, boolean y) {
        BufferedImage mirroredImg = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());

        Graphics2D g = mirroredImg.createGraphics();
		AffineTransform transform = new AffineTransform();

		int transX = 0;
		int transY = 0;
        double scaleX = 1;
        double scaleY = 1;
		if (x) {
            scaleX = -1;
			transX = -img.getWidth();
		}
		if (y) {
            scaleY = -1;
			transY = -img.getHeight();
		}
        
        transform.setToScale(scaleX, scaleY);
		transform.translate(transX, transY);

		g.drawImage(img, transform, null);
		g.dispose();

        return mirroredImg;
	}

}
