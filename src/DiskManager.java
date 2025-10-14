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

    public PageId AllocPage() throws IOException {
        if (!freePages.isEmpty()) {
            Iterator<PageId> it = freePages.iterator();// cette methode retourne un objet Iterator qui permet de
                                                       // parcourir les elements de l'ensemble freePages
            PageId pid = it.next();// cette methode retourne le premier element disponible dans l'ensemble
                                   // freePages
            it.remove();
            return pid;
        }

        File binDir = new File(dbConfig.getDbpath());// repertoire ou sont stockes les fichiers de donnees

        for (int i = 0; i < dbConfig.getDmMaxFileCount(); i++) {
            File file = new File(binDir, "Data" + i + ".rsdb");

            if (!file.exists()) {
                file.createNewFile();
            }

            long fileSize = file.length();// taille actuelle du fichier en octets
            if (fileSize + dbConfig.getPagesize() <= dbConfig.getDmMaxFileSize()) {// verifie si l'ajout d'une nouvelle
                                                                                   // page ne depasse pas la taille
                                                                                   // maximale autorisee
                try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {// ouvre le fichier en mode lecture
                                                                               // ecriture
                    raf.seek(fileSize);// positionne le curseur a la fin du fichier pour ajouter une nouvelle page
                    raf.write(new byte[dbConfig.getPagesize()]);// ecrit des octets vides pour initialiser la nouvelle
                                                                // page
                }
                int pageIdx = (int) (fileSize / dbConfig.getPagesize());
                return new PageId(i, pageIdx);
            }
        }

        throw new IOException("Aucun fichier disponible pour l'allocation.");
    }

    public void DeallocPage(PageId pid) {
        freePages.add(pid);
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