package src;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;



public class DiskManager {
    private DBConfig dbConfig;
    private Set<PageId> freePages;

    public DiskManager(DBConfig dbConfig) {
        this.dbConfig = dbConfig;
        this.freePages = new HashSet<>();
    }

    public void Init() {
        // Initialisation des structures internes
        freePages = new HashSet<>();
        // Optionnel : charger les pages libres depuis un fichier (dm.save)
    }

    public void Finish() {
        // Optionnel : sauvegarder les pages libres dans un fichier (dm.save)
    }

    

    public void WritePage(PageId pid, ByteBuffer buffer) throws IOException {
        if (buffer.capacity() != dbConfig.getPagesize()) {
            throw new IllegalArgumentException("Taille du buffer incorrecte !");
        }

        File file = new File(dbConfig.getDbpath(), "Data" + pid.getFileIdx() + ".rsdb");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek((long) pid.getPageIdx() * dbConfig.getPagesize());
            byte[] data = new byte[dbConfig.getPagesize()];
            buffer.rewind();
            buffer.get(data);
            raf.write(data);
        }
    }
}