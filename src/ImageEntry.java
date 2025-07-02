// ImageEntry.java
import java.io.File;

public class ImageEntry {
    public File file;
    public boolean selected = false;
    public int copies = 1;
    public float offsetX = 0.0f;
    public float offsetY = 0.0f;
    public float zoom = 1.0f;
    public float cropX = 0.0f;
    public float cropY = 0.0f;
    public float cropW = 1.0f;
    public float cropH = 1.0f;
    public boolean landscape = false;

    public ImageEntry(File file) {
        this.file = file;
    }
}
