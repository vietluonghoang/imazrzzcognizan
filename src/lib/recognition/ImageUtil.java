package lib.recognition;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.opencv.core.Rect;

public class ImageUtil {

    public void toGray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int red, green, blue, color, sum;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Color c = new Color(image.getRGB(j, i));
                red = (int) (c.getRed());
                green = (int) (c.getGreen());
                blue = (int) (c.getBlue());
                sum = red + green + blue;
                if (sum >= 765) {
                    color = 255;
                } else {
                    color = 0;
                }

                Color newColor = new Color(color, color, color);

                image.setRGB(j, i, newColor.getRGB());
            }
        }
    }

    public String convertImageToGray(String filepath) throws IOException {
        File input = new File(filepath);
        BufferedImage image = ImageIO.read(input);

        toGray(image);

        String path_file = "grayimage/" + "gray_" + input.getName();
        File output = new File(path_file);
        ImageIO.write(image, "png", output);

        return path_file;
    }

    public String cropImage(String filepath, Rect rect) throws IOException {
        File input = new File(filepath);
        BufferedImage image = ImageIO.read(input);

        BufferedImage croppedImage = image.getSubimage(rect.x, rect.y, rect.width, rect.height);

        String path_file = "cropimage/" + "crop_" + input.getName();
        File output = new File(path_file);
        ImageIO.write(croppedImage, "png", output);

        return path_file;
    }

}
