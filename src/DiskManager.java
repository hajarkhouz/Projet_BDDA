package src;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public class DiskManager {
    private DBConfig dbConfig;
    // Set qui garde la trace des PageId désalloués et réutilisables
    private Set<PageId> freePages;
    private File binDir;
    private static final String DATA_FILE_EXTENSION = ".bin";
    private final Map<Integer, RandomAccessFile> openFiles = new HashMap<>();

    /**
     * Constructeur : Initialise le chemin de base des données.
     * 
     * @param dbConfig La configuration du SGBD.
     */
    public DiskManager(DBConfig dbConfig) {
        this.dbConfig = dbConfig;
        this.freePages = new HashSet<>();
        // Chemin absolu vers le dossier où les fichiers de données seront stockés
        this.binDir = new File(dbConfig.getDbpath());

        // S'assurer que le répertoire de base des données existe
        if (!this.binDir.exists()) {
            this.binDir.mkdirs();
        }

        // Appeler init() ici pour charger l'état des pages libres au démarrage
        this.init();
    }

    /**
     * Charge l'état des pages libres depuis le fichier de sauvegarde
     * (freepages.txt).
     */
    public void init() {
        freePages.clear(); // Réinitialiser avant de charger

        File saveFile = new File(binDir, "freepages.txt");
        if (saveFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(saveFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length != 2)
                        continue;
                    try {
                        int fileIdx = Integer.parseInt(parts[0].trim());
                        int pageIdx = Integer.parseInt(parts[1].trim());
                        freePages.add(new PageId(fileIdx, pageIdx));
                    } catch (NumberFormatException e) {
                        System.err.println("Ligne invalide dans freepages.txt : " + line);
                    }
                }
            } catch (IOException e) {
                System.err.println("Erreur de lecture du fichier freepages.txt : " + e.getMessage());
            }
        }
    }

    /**
     * Sauvegarde l'état actuel des pages libres dans freepages.txt.
     */
    public void finish() {
        if (!binDir.exists())
            binDir.mkdirs();

        File saveFile = new File(binDir, "freepages.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(saveFile))) {
            for (PageId pid : freePages) {
                bw.write(pid.getFileIdx() + "," + pid.getPageIdx());
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Erreur d'écriture du fichier freepages.txt : " + e.getMessage());
        }
    }

    /**
     * Alloue une nouvelle page sur le disque. Réutilise une page libre si possible,
     * sinon étend le fichier de données le moins rempli.
     * * @return Le PageId de la page allouée.
     * 
     * @throws IOException Si l'allocation est impossible.
     */
    public PageId AllocPage() throws IOException {
        // 1. Réutiliser une page libre
        if (!freePages.isEmpty()) {
            Iterator<PageId> it = freePages.iterator();
            PageId pid = it.next();
            it.remove();
            return pid;
        }

        // 2. Trouver un emplacement dans un fichier existant ou nouveau
        for (int i = 0; i < dbConfig.getDmMaxFileCount(); i++) {
            File file = new File(this.binDir, "Data" + i + DATA_FILE_EXTENSION);
            long fileSize = file.length();

            // Vérifier la limite de taille du fichier
            if (fileSize >= dbConfig.getDmMaxFileSize()) {
                continue;
            }

            // Si le fichier n'existe pas, on le crée
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (!parent.exists())
                    parent.mkdirs();
                if (!file.createNewFile()) {
                    throw new IOException("Impossible de créer le fichier Data" + i + DATA_FILE_EXTENSION);
                }
                fileSize = 0; // Le fichier est maintenant créé mais vide
            }

            // Vérifier la place restante avant d'étendre
            if (fileSize + dbConfig.getPagesize() <= dbConfig.getDmMaxFileSize()) {
                int pageIdx = (int) (fileSize / dbConfig.getPagesize());

                // CRITIQUE : Étendre la taille du fichier pour allouer physiquement l'espace
                // pour la nouvelle page
                try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                    raf.setLength(fileSize + dbConfig.getPagesize());
                    // Optionnel : remplir la nouvelle page avec des zéros (déjà fait par setLength
                    // sur certains OS, mais bonne pratique)
                    // La nouvelle page sera initialisée par le BufferManager/Relation, donc on se
                    // contente d'allouer l'espace.
                }

                return new PageId(i, pageIdx);
            }
        }

        throw new IOException(
                "Allocation impossible : tous les fichiers sont pleins ou la limite (dmMaxFileCount) est atteinte.");
    }

    /**
     * Marque une page comme libre, permettant sa réutilisation.
     * 
     * @param pid Le PageId à désallouer.
     */
    public void DeallocPage(PageId pid) {
        if (pid != null) {
            freePages.add(pid);
        }
    }

    /**
     * Lit une page spécifique et place son contenu dans le ByteBuffer.
     * 
     * @param pid    Le PageId à lire.
     * @param buffer Le ByteBuffer où les données seront stockées.
     * @throws IOException Si le fichier n'existe pas ou si l'accès est hors limite.
     */
    public void ReadPage(PageId pid, ByteBuffer buffer) throws IOException {
        if (buffer.capacity() != dbConfig.getPagesize()) {
            throw new IllegalArgumentException(
                    "Taille du buffer incorrecte ! Doit correspondre à la taille de page configurée.");
        }

        File file = new File(this.binDir, "Data" + pid.getFileIdx() + DATA_FILE_EXTENSION);

        if (!file.exists()) {
            throw new IOException("Fichier de données inexistant : " + file.getAbsolutePath());
        }

        // try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
        // long offset = (long) pid.getPageIdx() * dbConfig.getPagesize();

        // // Vérification de la validité de l'offset avant la lecture
        // if (offset >= raf.length()) {
        // // Si on essaie de lire au-delà de la fin du fichier, on lève une erreur ou
        // on retourne des zéros.
        // // Ici, on préfère lever une erreur car la page devrait exister.
        // throw new IOException("Lecture hors limite : Page " + pid + " non initialisée
        // sur le disque.");
        // }

        // byte[] data = new byte[dbConfig.getPagesize()];
        // raf.seek(offset);
        // raf.readFully(data);

        // buffer.clear();
        // buffer.put(data);
        // buffer.flip();

        // } catch (IOException e) {
        // // Afficher le détail de l'erreur pour le débogage et re-lancer
        // System.err.println("Erreur de lecture de la page " + pid + " : " +
        // e.getMessage());
        // throw e;
        // }

        RandomAccessFile raf = getOrOpen(pid.getFileIdx());
        synchronized (raf) {
            long offset = (long) pid.getPageIdx() * dbConfig.getPagesize();
            if (offset >= raf.length()) {
                throw new IOException("Lecture hors limite : Page " + pid + " non initialisée sur le disque.");
            }
            byte[] data = new byte[dbConfig.getPagesize()];
            raf.seek(offset);
            raf.readFully(data);

            buffer.clear();
            buffer.put(data);
            buffer.flip();
        }

    }

    /**
     * Écrit le contenu du ByteBuffer sur la page spécifiée.
     * 
     * @param pid    Le PageId où écrire.
     * @param buffer Le ByteBuffer contenant les données.
     * @throws IOException Si l'écriture échoue.
     */
    public void WritePage(PageId pid, ByteBuffer buffer) throws IOException {
        if (buffer.capacity() != dbConfig.getPagesize()) {
            throw new IllegalArgumentException(
                    "Taille du buffer incorrecte ! Doit correspondre à la taille de page configurée.");
        }

        File file = new File(this.binDir, "Data" + pid.getFileIdx() + DATA_FILE_EXTENSION);

        // Assurez-vous que le fichier existe (important pour la première écriture après
        // AllocPage)
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (!parent.exists())
                parent.mkdirs();
            if (!file.createNewFile()) {
                throw new IOException("Impossible de créer le fichier : " + file.getAbsolutePath());
            }
        }

        // try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
        // long offset = (long) pid.getPageIdx() * dbConfig.getPagesize();

        // // CRITIQUE : Si on écrit au-delà de la taille actuelle, étendre le fichier
        // // Cela agit comme un filet de sécurité si AllocPage a été contourné ou si le
        // // fichier a été vidé.
        // if (offset + dbConfig.getPagesize() > raf.length()) {
        // raf.setLength(offset + dbConfig.getPagesize());
        // }

        // raf.seek(offset);

        // byte[] data = new byte[dbConfig.getPagesize()];
        // buffer.rewind();
        // buffer.get(data, 0, dbConfig.getPagesize());

        // raf.write(data);
        // }

        RandomAccessFile raf = getOrOpen(pid.getFileIdx());
        synchronized (raf) {
            long offset = (long) pid.getPageIdx() * dbConfig.getPagesize();
            if (offset + dbConfig.getPagesize() > raf.length()) {
                raf.setLength(offset + dbConfig.getPagesize());
            }

            raf.seek(offset);

            byte[] data = new byte[dbConfig.getPagesize()];
            buffer.rewind();
            buffer.get(data, 0, dbConfig.getPagesize());
            raf.write(data);
        }

    }

    public DBConfig getDBConfig() {
        return this.dbConfig;
    }

    private synchronized RandomAccessFile getOrOpen(int fileIdx) throws IOException {
        RandomAccessFile raf = openFiles.get(fileIdx);
        if (raf == null) {
            File file = new File(this.binDir, "Data" + fileIdx + DATA_FILE_EXTENSION);
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (!parent.exists())
                    parent.mkdirs();
                file.createNewFile();
            }
            raf = new RandomAccessFile(file, "rw");
            openFiles.put(fileIdx, raf);
        }
        return raf;
    }

    public synchronized void closeAll() {
        for (RandomAccessFile raf : openFiles.values()) {
            try { raf.close(); } catch (IOException ignored) {}
        }
        openFiles.clear();
    }
    

}