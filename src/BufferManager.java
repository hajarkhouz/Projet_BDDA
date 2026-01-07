package src;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BufferManager {
    private DBConfig dbConfig; // référence à la config
    private DiskManager diskManager; // référence au DiskManager
    private List<RamBuffer> bufferPool; // liste des buffers
    private Map<PageId, RamBuffer> bufferMap; // map pour retrouver rapidement une page
    private long accessCounter; // compteur global pour LRU/MRU
    private String currentPolicy; // politique de remplacement actuelle

    public BufferManager(DBConfig dbConfig, DiskManager diskManager) {
        this.dbConfig = dbConfig;
        this.diskManager = diskManager;

        // initialisation du buffer pool
        int bufferCount = dbConfig.getBmBufferCount();
        bufferPool = new ArrayList<>(bufferCount);// liste des buffers
        bufferMap = new HashMap<>();

        for (int i = 0; i < bufferCount; i++) {
            bufferPool.add(new RamBuffer(dbConfig.getPagesize()));
        }
        accessCounter = 0;

        this.currentPolicy = dbConfig.getBmPolicy();
        if (this.currentPolicy == null)
            this.currentPolicy = "LRU";

    }

    public RamBuffer getPage(PageId pid) throws IOException {
        // 1. Vérifier si la page est déjà en RAM
        if (bufferMap.containsKey(pid)) {
            RamBuffer buffer = bufferMap.get(pid);
            buffer.setLastAccessOrder(getNextAccessCounter());
            buffer.setPinCount(buffer.getPinCount() + 1);
            return buffer;
        }

        // 2. Chercher un buffer libre ou à remplacer
        RamBuffer chosenBuffer = null;

        // Chercher un buffer libre
        for (RamBuffer buf : bufferPool) {
            if (buf.getPageId() == null) {
                chosenBuffer = buf;
                break;
            }
        }

        // Si aucun buffer libre, appliquer la politique de remplacement
        if (chosenBuffer == null) {
            if (currentPolicy.equalsIgnoreCase("LRU")) {
                long oldest = Long.MAX_VALUE;
                for (RamBuffer buf : bufferPool) {
                    if (buf.getPinCount() == 0 && buf.getLastAccessOrder() < oldest) {
                        oldest = buf.getLastAccessOrder();
                        chosenBuffer = buf;
                    }
                }
            } else if (currentPolicy.equalsIgnoreCase("MRU")) {
                long newest = Long.MIN_VALUE;
                for (RamBuffer buf : bufferPool) {
                    if (buf.getPinCount() == 0 && buf.getLastAccessOrder() > newest) {
                        newest = buf.getLastAccessOrder();
                        chosenBuffer = buf;
                    }
                }
            }
        }

        if (chosenBuffer == null) {
            throw new IOException("Aucun buffer disponible pour charger la page !");
        }

        // 3. Si le buffer à remplacer est dirty, l’écrire sur le disque
        if (chosenBuffer.isDirty() && chosenBuffer.getPageId() != null) {
            diskManager.WritePage(chosenBuffer.getPageId(), chosenBuffer.getData());
            chosenBuffer.setDirty(false);
        }

        // 4. Charger la nouvelle page depuis le disque
        PageId oldPid = chosenBuffer.getPageId();
        if (oldPid != null) {
            bufferMap.remove(oldPid);
        }
        diskManager.ReadPage(pid, chosenBuffer.getData());

        // 5. Mettre à jour le buffer
        chosenBuffer.setPageId(pid);
        chosenBuffer.setPinCount(1);
        chosenBuffer.setDirty(false);
        chosenBuffer.setLastAccessOrder(getNextAccessCounter());

        // 6. Ajouter à la map
        bufferMap.put(pid, chosenBuffer);

        return chosenBuffer;
    }

    public void freePage(PageId pid, boolean valDirty) throws IOException {
        // Vérifier que la page est dans la map
        RamBuffer buffer = bufferMap.get(pid);
        if (buffer == null) {
            throw new IOException("Tentative de libérer une page qui n'est pas en RAM : " + pid);
        }

        // Décrémenter le pinCount
        int pin = buffer.getPinCount();
        if (pin > 0) {
            buffer.setPinCount(pin - 1);
        } else {
            System.err.println("Attention : pinCount déjà à 0 pour la page " + pid);
        }

        // Mettre à jour le flag dirty
        if (valDirty) {
            buffer.setDirty(true);
        }

        // Mettre à jour le lastAccessOrder pour LRU/MRU
        buffer.setLastAccessOrder(getNextAccessCounter());
    }

    public void setCurrentReplacementPolicy(String policy) {
        // Vérifier que la politique est valide
        if (!policy.equalsIgnoreCase("LRU") && !policy.equalsIgnoreCase("MRU")) {
            throw new IllegalArgumentException("Politique de remplacement invalide : " + policy);
        }

        this.currentPolicy = policy; // currentPolicy est une variable membre du BufferManager
    }

    public void flushBuffers() throws IOException {
        for (RamBuffer buffer : bufferPool) {
            // Écrire sur disque si la page est dirty et valide
            if (buffer.isDirty() && buffer.getPageId() != null) {
                diskManager.WritePage(buffer.getPageId(), buffer.getData());
            }

            // Réinitialiser le buffer
            buffer.setPageId(null);
            buffer.setPinCount(0);
            buffer.setDirty(false);
            buffer.getData().clear(); // réinitialiser le ByteBuffer
            buffer.setLastAccessOrder(0);
        }

        // Vider la map des pages
        bufferMap.clear();
    }

    // Getter pour le compteur global si besoin
    public long getNextAccessCounter() {
        return accessCounter++;
    }

    public void flushAllPages() throws IOException {

        for (RamBuffer buffer : bufferPool) {
            // Écrire sur disque si la page est dirty et valide
            if (buffer.isDirty() && buffer.getPageId() != null) {
                diskManager.WritePage(buffer.getPageId(), buffer.getData());
                buffer.setDirty(false); // La page est propre après l'écriture
            }
        }
    }
}
