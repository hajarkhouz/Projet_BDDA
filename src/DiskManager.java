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
    private File binDir;

    public DiskManager(DBConfig dbConfig) {
        this.dbConfig = dbConfig;
        this.freePages = new HashSet<>();
        this.binDir = new File(dbConfig.getDbpath());
        if (!this.binDir.exists()) {
            this.binDir.mkdirs();
        }
    }

    public void Init() {
    }

    public void Finish() {
    }
    
    public PageId AllocPage() throws IOException {
        if (!freePages.isEmpty()) {
            Iterator<PageId> it = freePages.iterator();
            PageId pid = it.next();
            it.remove();
            return pid;
        }

        for (int i = 0; i < dbConfig.getDmMaxFileCount(); i++) {
            File file = new File(this.binDir, "Data" + i + ".rsdb");
            long fileSize = file.length();
            
            if (fileSize + dbConfig.getPagesize() <= dbConfig.getDmMaxFileSize()) {
                try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                    raf.seek(fileSize);
                    raf.write(new byte[dbConfig.getPagesize()]); 
                }
                
                int pageIdx = (int) (fileSize / dbConfig.getPagesize());
                return new PageId(i, pageIdx);
            }
        }

        throw new IOException("Allocation impossible : tous les fichiers sont pleins ou la limite (dmMaxFileCount) est atteinte.");
    }
    
    public void DeallocPage(PageId pid) {
        freePages.add(pid);
    }
 // Lecture sécurisée d'une page
 public void ReadPage(PageId pid, ByteBuffer buffer) throws IOException {
    if (buffer.capacity() != dbConfig.getPagesize()) {
        throw new IllegalArgumentException(
            "Taille du buffer incorrecte ! Doit correspondre à la taille de page configurée."
        );
    }
    File file = new File(this.binDir, "Data" + pid.getFileIdx() + ".rsdb");
    if (!file.exists()) {
        throw new IOException("Fichier de données inexistant : " + file.getAbsolutePath());
    }
    byte[] data = new byte[dbConfig.getPagesize()];
    try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
        raf.seek((long) pid.getPageIdx() * dbConfig.getPagesize());
        raf.readFully(data);
    }
    buffer.clear();
    buffer.put(data);
    buffer.flip();
}
  /*  public void ReadPage(PageId pid, ByteBuffer buffer) throws IOException {
        if (buffer.capacity() != dbConfig.getPagesize()) {
            throw new IllegalArgumentException("Taille du buffer incorrecte ! Doit correspondre à la taille de page configurée.");
        }
        
        File file = new File(this.binDir, "Data" + pid.getFileIdx() + ".rsdb");
        
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek((long) pid.getPageIdx() * dbConfig.getPagesize());
            
            byte[] data = new byte[dbConfig.getPagesize()];
            raf.readFully(data); 
            
            buffer.clear();
            buffer.put(data);
            buffer.flip();
        }
    }*/

   /* public void WritePage(PageId pid, ByteBuffer buffer) throws IOException {
        if (buffer.capacity() != dbConfig.getPagesize()) {
            throw new IllegalArgumentException("Taille du buffer incorrecte ! Doit correspondre à la taille de page configurée.");
        }

        File file = new File(this.binDir, "Data" + pid.getFileIdx() + ".rsdb");
        
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek((long) pid.getPageIdx() * dbConfig.getPagesize());
            
            byte[] data = new byte[dbConfig.getPagesize()];
            buffer.rewind();
            buffer.get(data);
            raf.write(data);
        }
    }*/
    
    public void WritePage(PageId pid, ByteBuffer buffer) throws IOException {
        if (buffer.capacity() != dbConfig.getPagesize()) {
            throw new IllegalArgumentException("Taille du buffer incorrecte ! Doit correspondre à la taille de page configurée.");
        }
    
        
        File file = new File(this.binDir, "Data" + pid.getFileIdx() + ".rsdb");
    
        // Création du fichier si inexistant
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    throw new IOException("Impossible de créer le dossier parent : " + parent.getAbsolutePath());
                }
            }
            if (!file.createNewFile()) {
                throw new IOException("Impossible de créer le fichier : " + file.getAbsolutePath());
            }
        }
    
        System.out.println("DEBUG WritePage: file = " + file.getAbsolutePath());
    
         
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek((long) pid.getPageIdx() * dbConfig.getPagesize());
            byte[] data = new byte[dbConfig.getPagesize()];
            buffer.rewind();
            
            buffer.get(data, 0, buffer.remaining()); // copie seulement ce qui est présent

            raf.write(data);
        }
    }
    
////////////////////////////
   /* public File getBinDir() {
        return binDir;
    }*/
    
}