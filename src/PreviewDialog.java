// PreviewDialog.java

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class PreviewDialog {

    public static void showDialog(final PhotoKiosk owner,
                                  final ImageEntry entry,
                                  final String format) {

        final BufferedImage original;
        try {
            original = ImageIO.read(entry.file);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Could not load image");
            return;
        }

        final JDialog d = new JDialog((Frame) null, "Preview – " + entry.file.getName(), true);
        d.setSize(800, 600);
        d.setLocationRelativeTo(null);

        final JPanel canvas = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;

                int W = getWidth(), H = getHeight();
                int imgW = original.getWidth();
                int imgH = original.getHeight();
                float imgAspect = (float) imgW / imgH;
                float canvasAspect = (float) W / H;

                int drawW, drawH;
                if (imgAspect > canvasAspect) {
                    drawW = W;
                    drawH = (int)(W / imgAspect);
                } else {
                    drawH = H;
                    drawW = (int)(H * imgAspect);
                }

                int x = (W - drawW) / 2;
                int y = (H - drawH) / 2;

                Image img = original.getScaledInstance(drawW, drawH, Image.SCALE_SMOOTH);
                g2.drawImage(img, x, y, drawW, drawH, null);

                int cropX = x + (int)(entry.cropX * drawW);
                int cropY = y + (int)(entry.cropY * drawH);
                int cropW = (int)(entry.cropW * drawW);
                int cropH = (int)(entry.cropH * drawH);

                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke(2));
                g2.drawRect(cropX, cropY, cropW, cropH);
            }
        };

        final boolean[] cancelled = new boolean[]{true};

        JButton zoomIn = new JButton("+");
        JButton zoomOut = new JButton("-");
        JButton rotate = new JButton("Rotate");
        JButton up = new JButton("↑");
        JButton down = new JButton("↓");
        JButton left = new JButton("←");
        JButton rightBtn = new JButton("→");
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        JButton reset = new JButton("Reset");

        zoomIn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                entry.cropW = Math.max(0.05f, Math.min(1f, entry.cropW * 0.9f));
                entry.cropH = Math.max(0.05f, Math.min(1f, entry.cropH * 0.9f));
                canvas.repaint();
            }
        });
        zoomOut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                entry.cropW = Math.min(1f, entry.cropW * 1.1f);
                entry.cropH = Math.min(1f, entry.cropH * 1.1f);
                canvas.repaint();
            }
        });
        rotate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean prev = entry.landscape;
                entry.landscape = !prev;
                float temp = entry.cropW;
                entry.cropW = entry.cropH;
                entry.cropH = temp;
                canvas.repaint();
            }
        });
        up.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                entry.cropY = Math.max(0f, entry.cropY - 0.01f);
                canvas.repaint();
            }
        });
        down.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                entry.cropY = Math.min(1f - entry.cropH, entry.cropY + 0.01f);
                canvas.repaint();
            }
        });
        left.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                entry.cropX = Math.max(0f, entry.cropX - 0.01f);
                canvas.repaint();
            }
        });
        rightBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                entry.cropX = Math.min(1f - entry.cropW, entry.cropX + 0.01f);
                canvas.repaint();
            }
        });

        reset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                entry.zoom = 1.0f;
                entry.cropX = 0.0f;
                entry.cropY = 0.0f;
                entry.cropW = 1.0f;
                entry.cropH = 1.0f;
                entry.landscape = false;
                canvas.repaint();
            }
        });

        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelled[0] = false;
                owner.refreshThumbnail(entry);
                d.dispose();
            }
        });

        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                d.dispose();
            }
        });

        // Touch support (mouse drag to move crop box)
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            int lastX, lastY;
            boolean dragging = false;
            public void mouseDragged(MouseEvent e) {
                int W = canvas.getWidth(), H = canvas.getHeight();
                int dx = e.getX() - lastX;
                int dy = e.getY() - lastY;
                lastX = e.getX();
                lastY = e.getY();
                float relDX = (float) dx / W;
                float relDY = (float) dy / H;
                entry.cropX = Math.min(1f - entry.cropW, Math.max(0f, entry.cropX + relDX));
                entry.cropY = Math.min(1f - entry.cropH, Math.max(0f, entry.cropY + relDY));
                canvas.repaint();
            }
            public void mouseMoved(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
            }
        });

        JPanel ctrl = new JPanel();
        ctrl.setLayout(new BoxLayout(ctrl, BoxLayout.Y_AXIS));
        ctrl.add(new JLabel("Zoom"));
        ctrl.add(zoomIn);
        ctrl.add(zoomOut);
        ctrl.add(new JLabel("Move"));
        ctrl.add(up);
        ctrl.add(down);
        ctrl.add(left);
        ctrl.add(rightBtn);
        ctrl.add(rotate);
        ctrl.add(reset);
        ctrl.add(save);
        ctrl.add(cancel);

        d.setLayout(new BorderLayout());
        d.add(canvas, BorderLayout.CENTER);
        d.add(ctrl, BorderLayout.EAST);
        d.setVisible(true);

        if (cancelled[0]) {
            entry.zoom = 1f;
            entry.cropX = 0.0f;
            entry.cropY = 0.0f;
            entry.cropW = 1.0f;
            entry.cropH = 1.0f;
            entry.landscape = false;
        }
    }
}
