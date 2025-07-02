// PhotoKiosk.java

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaPrintableArea;

public class PhotoKiosk {

    public static final String FORMAT_10x15 = "10×15 cm (CK-6015)";
    public static final String FORMAT_15x23 = "15×23 cm (CK-9523)";

    private final JPanel thumbGrid = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
    private final JComboBox fmtBox = new JComboBox(new String[]{FORMAT_10x15, FORMAT_15x23});
    private ImageEntry lastSelectedImage = null;
    private final java.util.Timer driveTimer = new java.util.Timer(true);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new PhotoKiosk().buildGui();
            }
        });
    }

    private void buildGui() {
        JFrame f = new JFrame("Mitsubishi Photo Kiosk XP");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setSize(1100, 700);
        f.setLocationRelativeTo(null);
        thumbGrid.setBackground(Color.DARK_GRAY);

        JScrollPane scroll = new JScrollPane(thumbGrid);

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton printBtn = new JButton("Print selected");
        printBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        printBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { printSelected(); }
        });

        JButton prevBtn = new JButton("Preview");
        prevBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        prevBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (lastSelectedImage != null) {
                    PreviewDialog.showDialog(
                            PhotoKiosk.this,
                            lastSelectedImage,
                            (String) fmtBox.getSelectedItem()
                    );
                } else {
                    JOptionPane.showMessageDialog(null, "Select a photo first.");
                }
            }
        });

        side.add(new JLabel("Print format:"));
        side.add(fmtBox);
        side.add(Box.createVerticalStrut(20));
        side.add(printBtn);
        side.add(Box.createVerticalStrut(10));
        side.add(prevBtn);
        side.add(Box.createVerticalGlue());

        f.add(scroll, BorderLayout.CENTER);
        f.add(side, BorderLayout.EAST);
        f.setVisible(true);

        startDriveWatcher();
    }

    private void startDriveWatcher() {
        driveTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            private final Set knownRoots = new HashSet();
            public void run() {
                Set now = new HashSet();

                File[] roots = File.listRoots();
                for (int i = 0; i < roots.length; i++) {
                    File r = roots[i];
                    now.add(r.getAbsolutePath());
                    if (!knownRoots.contains(r.getAbsolutePath()) && isRemovable(r)) {
                        scanDrive(r);
                    }
                }

                String user = System.getProperty("user.name");
                File media = new File("/run/media/" + user);
                if (!media.exists()) media = new File("/media/" + user);
                if (media.exists()) {
                    File[] usbs = media.listFiles();
                    if (usbs != null) {
                        for (int i = 0; i < usbs.length; i++) {
                            File u = usbs[i];
                            now.add(u.getAbsolutePath());
                            if (!knownRoots.contains(u.getAbsolutePath())) scanDrive(u);
                        }
                    }
                }

                if (!knownRoots.equals(now)) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            thumbGrid.removeAll();
                            thumbGrid.revalidate(); thumbGrid.repaint();
                        }
                    });
                }
                knownRoots.clear();  knownRoots.addAll(now);
            }
        }, 0, 5000);
    }

    private boolean isRemovable(File root) {
        FileSystemView v = FileSystemView.getFileSystemView();
        return v.isDrive(root) && !root.getAbsolutePath().equalsIgnoreCase("C:\\");
    }

    private void scanDrive(final File dir) {
        new Thread(new Runnable() {
            public void run() { recurse(dir); }
            private void recurse(File d) {
                File[] list = d.listFiles();
                if (list == null) return;
                for (int i = 0; i < list.length; i++) {
                    File f = list[i];
                    if (f.isDirectory()) recurse(f);
                    else if (isPhoto(f)) addThumbLater(f);
                }
            }
        }).start();
    }

    private boolean isPhoto(File f) {
        String n = f.getName().toLowerCase();
        return n.endsWith(".jpg")||n.endsWith(".jpeg")||n.endsWith(".png")||n.endsWith(".bmp");
    }

    private void addThumbLater(final File f) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ImageEntry ie = new ImageEntry(f);
                ThumbPanel tp = new ThumbPanel(PhotoKiosk.this, ie);
                thumbGrid.add(tp); thumbGrid.revalidate();
            }
        });
    }

    void setLastSelected(ImageEntry ie){ lastSelectedImage = ie; }

    public void refreshThumbnail(ImageEntry entry) {
        Component[] comps = thumbGrid.getComponents();
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] instanceof ThumbPanel) {
                ThumbPanel tp = (ThumbPanel) comps[i];
                if (tp.getEntry().file.equals(entry.file)) {
                    tp.updateThumbnailFromEntry();
                    tp.repaint();
                    break;
                }
            }
        }
    }

    private void printSelected() {
        Component[] comps = thumbGrid.getComponents();
        if (comps.length == 0) { JOptionPane.showMessageDialog(null,"No images."); return; }

        String format = (String) fmtBox.getSelectedItem();
        PrintService printer = findPrinter(format);
        if (printer == null){ JOptionPane.showMessageDialog(null,"No printer"); return; }

        for (int i = 0; i < comps.length; i++) {
            if (!(comps[i] instanceof ThumbPanel)) continue;
            ThumbPanel tp = (ThumbPanel) comps[i];
            ImageEntry ie = tp.getEntry();
            if (!ie.selected) continue;

            for (int c = 0; c < ie.copies; c++) {
                try {
                    BufferedImage img = ImageIO.read(ie.file);
                    int w = img.getWidth();
                    int h = img.getHeight();
                    int cropX = (int)(ie.cropX * w);
                    int cropY = (int)(ie.cropY * h);
                    int cropW = (int)(ie.cropW * w);
                    int cropH = (int)(ie.cropH * h);

                    if (cropW <= 0 || cropH <= 0 || cropX + cropW > w || cropY + cropH > h) {
                        cropX = 0; cropY = 0; cropW = w; cropH = h;
                    }

                    BufferedImage cropped = img.getSubimage(cropX, cropY, cropW, cropH);
                    File tempFile = File.createTempFile("print", ".jpg");
                    ImageIO.write(cropped, "jpg", tempFile);

                    FileInputStream fis = new FileInputStream(tempFile);
                    DocFlavor df = DocFlavor.INPUT_STREAM.JPEG;
                    Doc doc = new SimpleDoc(fis, df, null);
                    HashPrintRequestAttributeSet atts = new HashPrintRequestAttributeSet();
                    atts.add(new Copies(1));
                    atts.add(new MediaPrintableArea(0,0,210,297,MediaPrintableArea.MM));
                    printer.createPrintJob().print(doc, atts);
                    fis.close(); tempFile.delete();
                } catch(Exception ex){ ex.printStackTrace(); }
            }
        }
    }

    private PrintService findPrinter(String format){
        PrintService[] arr = PrintServiceLookup.lookupPrintServices(null,null);
        for(int i=0;i<arr.length;i++){
            String n = arr[i].getName().toLowerCase();
            if(FORMAT_10x15.equals(format)&&n.contains("6015")) return arr[i];
            if(FORMAT_15x23.equals(format)&&n.contains("9523")) return arr[i];
        }
        return PrintServiceLookup.lookupDefaultPrintService();
    }
}
