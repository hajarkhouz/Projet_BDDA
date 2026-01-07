package test;

import src.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class BufferManagerTest {

    private static void deleteRec(File f) {
        if (f == null || !f.exists())
            return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null)
                for (File c : children)
                    deleteRec(c);
        }
        f.delete();
    }

    private static void cleanDataDirectory(String dbPath) {
        File dataDir = new File(dbPath);
        if (dataDir.exists()) {
            File[] files = dataDir.listFiles();
            if (files != null)
                for (File file : files)
                    deleteRec(file);
        } else {
            dataDir.mkdirs();
        }
    }

    private static void writeString(ByteBuffer bb, String s) {
        bb.clear();
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        int n = Math.min(bytes.length, bb.capacity());
        bb.put(bytes, 0, n);
        bb.flip();
    }

    private static String readString(ByteBuffer bb) {
        // lit jusqu'au premier 0
        byte[] data = new byte[bb.capacity()];
        bb.rewind();
        bb.get(data);
        int len = 0;
        while (len < data.length && data[len] != 0)
            len++;
        return new String(data, 0, len, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws IOException {
        System.out.println("=== BufferManagerTest ===");

        DBConfig cfg = DBConfig.LoadDBConfig("config/config_valide.txt");
        cleanDataDirectory(cfg.getDbpath());

        DiskManager dm = null;
        BufferManager bm = null;

        try {
            dm = new DiskManager(cfg);
            bm = new BufferManager(cfg, dm);

            // Optionnel : forcer une policy pour test
            bm.setCurrentReplacementPolicy(cfg.getBmPolicy());

            // 1) Allouer 3 pages (utile pour tester l'éviction si bm_buffercount petit)
            PageId p1 = dm.AllocPage();
            PageId p2 = dm.AllocPage();
            PageId p3 = dm.AllocPage();

            System.out.println("Pages allouées :");
            System.out.println("p1 = " + p1);
            System.out.println("p2 = " + p2);
            System.out.println("p3 = " + p3);

            // 2) Charger p1, écrire dedans, libérer en dirty
            System.out.println("\n--- Test écriture + dirty + flush ---");
            RamBuffer b1 = bm.getPage(p1);
            writeString(b1.getData(), "PAGE1-HELLO");
            bm.freePage(p1, true); // dirty = true

            // flush => écrit sur disque
            bm.flushAllPages();

            // 3) Recharger p1 et vérifier lecture
            RamBuffer b1b = bm.getPage(p1);
            String s1 = readString(b1b.getData());
            bm.freePage(p1, false);

            System.out.println("Contenu relu p1 : " + s1);
            if (!"PAGE1-HELLO".equals(s1)) {
                throw new RuntimeException("❌ Erreur: contenu p1 différent de celui écrit !");
            } else {
                System.out.println("✅ OK: contenu p1 persisté sur disque.");
            }

            // 4) Test pin/unpin simple
            System.out.println("\n--- Test pinCount ---");
            RamBuffer tmp = bm.getPage(p2);
            // pinCount est au moins 1 ici
            bm.freePage(p2, false);
            System.out.println("✅ OK: getPage/freePage sur p2.");

            // 5) Test éviction (si bm_buffercount petit, ex: 2)
            System.out.println("\n--- Test éviction (LRU/MRU) ---");
            // Charger p1, p2, p3 : si pool < 3, il y aura remplacement
            RamBuffer bp1 = bm.getPage(p1);
            bm.freePage(p1, false);
            RamBuffer bp2 = bm.getPage(p2);
            bm.freePage(p2, false);
            RamBuffer bp3 = bm.getPage(p3);
            bm.freePage(p3, false);

            System.out.println("✅ OK: chargement p1, p2, p3 (remplacement si nécessaire) sans crash.");

            // 6) Tester changement de policy
            System.out.println("\n--- Test changement policy ---");
            bm.setCurrentReplacementPolicy("LRU");
            RamBuffer t1 = bm.getPage(p1);
            bm.freePage(p1, false);
            bm.setCurrentReplacementPolicy("MRU");
            RamBuffer t2 = bm.getPage(p2);
            bm.freePage(p2, false);
            System.out.println("✅ OK: switch policy LRU/MRU.");

            // 7) Nettoyage final : flush buffers et sauvegarde freepages
            bm.flushAllPages();

        } finally {
            if (bm != null) {
                try {
                    bm.flushAllPages();
                } catch (Exception ignored) {
                }
            }
            if (dm != null) {
                dm.finish(); // ✅ important
            }
        }

        System.out.println("\n✅ Fin BufferManagerTest.");
    }
}

