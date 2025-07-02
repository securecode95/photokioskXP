// ThumbPanel.java

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.awt.image.RasterFormatException;

public class ThumbPanel extends JPanel {
    private final ImageEntry entry;
    private final PhotoKiosk kiosk;
    private final JLabel thumb = new JLabel();
    private final JButton plus = new JButton("+");
    private final JButton minus = new JButton("-");

    public ThumbPanel(final PhotoKiosk kiosk,final ImageEntry entry) {
        this.entry = entry;
        this.kiosk = kiosk;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel south = new JPanel(new FlowLayout());
        south.add(minus);
        south.add(new JLabel(entry.copies + "×"));
        south.add(plus);

        add(thumb, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        updateThumbnailFromEntry();

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                kiosk.setLastSelected(entry);
                setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
            }
        });

        plus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                entry.copies++;
                updateThumbnailFromEntry();
            }
        });
        minus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (entry.copies > 1) entry.copies--;
                updateThumbnailFromEntry();
            }
        });
    }

    public ImageEntry getEntry() {
        return entry;
    }

    public void updateThumbnailFromEntry() {
        thumb.setIcon(new ImageIcon(getScaledImage(entry.file, 160)));
        repaint();
    }

    public void rebuildThumbnail() {
        try {
            BufferedImage fullImg = ImageIO.read(entry.file);
            int imgW = fullImg.getWidth();
            int imgH = fullImg.getHeight();

            int cropX = (int) (entry.cropX * imgW);
            int cropY = (int) (entry.cropY * imgH);
            int cropW = (int) (entry.cropW * imgW);
            int cropH = (int) (entry.cropH * imgH);

            if (cropW <= 0 || cropH <= 0) return;

            BufferedImage cropped = fullImg.getSubimage(cropX, cropY, cropW, cropH);
            thumb.setIcon(new ImageIcon(cropped.getScaledInstance(160, -1, Image.SCALE_SMOOTH)));
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (RasterFormatException ex) {
            ex.printStackTrace();
        }
        repaint();
    }

    private Image getScaledImage(File file, int width) {
        try {
            BufferedImage img = ImageIO.read(file);
            int w = img.getWidth();
            int h = img.getHeight();
            float scale = (float) width / w;
            int height = (int) (h * scale);
            return img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        } catch (IOException e) {
            return null;
        }
    }
}