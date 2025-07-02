import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.geom.AffineTransform;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class ImageCropper {
    public static BufferedImage crop(ImageEntry entry) {
        try {
            BufferedImage img = ImageIO.read(entry.file);
            int w = img.getWidth();
            int h = img.getHeight();

            int cropX = Math.round(entry.cropX * w);
            int cropY = Math.round(entry.cropY * h);
            int cropW = Math.round(entry.cropW * w);
            int cropH = Math.round(entry.cropH * h);

            cropX = Math.max(0, Math.min(w - 1, cropX));
            cropY = Math.max(0, Math.min(h - 1, cropY));
            cropW = Math.max(1, Math.min(w - cropX, cropW));
            cropH = Math.max(1, Math.min(h - cropY, cropH));

            BufferedImage cropped = img.getSubimage(cropX, cropY, cropW, cropH);

            if (entry.landscape) {
                BufferedImage rotated = new BufferedImage(cropped.getHeight(), cropped.getWidth(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = rotated.createGraphics();
                g2.rotate(Math.PI / 2, rotated.getWidth() / 2, rotated.getHeight() / 2);
                g2.translate((rotated.getWidth() - cropped.getWidth()) / 2, (rotated.getHeight() - cropped.getHeight()) / 2);
                g2.drawImage(cropped, 0, -cropped.getHeight(), null);
                g2.dispose();
                return rotated;
            }

            return cropped;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
