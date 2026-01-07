package test;

import java.io.IOException;
import java.nio.ByteBuffer;

import src.DBConfig;
import src.DiskManager;
import src.PageId;

public class TestDiskManager {

    public static void main(String[] args) {
        DiskManager dm = null;

        try {
            // 1) Charger la configuration
            DBConfig config = DBConfig.LoadDBConfig("config/config_valide.txt");
            dm = new DiskManager(config);

            System.out.println("=== Test : Allocation d'une page ===");
            PageId pid = dm.AllocPage();
            System.out.println("Page allouée : " + pid);

            // 2) Écriture dans la page
            System.out.println("\n=== Test : Écriture dans la page ===");
            ByteBuffer buffer = ByteBuffer.allocate(config.getPagesize());
            buffer.put("Hello BDDA".getBytes());
            buffer.flip();

            dm.WritePage(pid, buffer);
            System.out.println("Écriture réussie.");

            // 3) Lecture de la page
            System.out.println("\n=== Test : Lecture de la page ===");
            ByteBuffer readBuffer = ByteBuffer.allocate(config.getPagesize());
            dm.ReadPage(pid, readBuffer);

            byte[] data = new byte[readBuffer.remaining()];
            readBuffer.get(data);

            int len = 0;
            while (len < data.length && data[len] != 0) {
                len++;
            }
            String content = new String(data, 0, len);
            System.out.println("Contenu lu : '" + content + "'");

            // 4) Désallocation
            System.out.println("\n=== Test : Désallocation de la page ===");
            dm.DeallocPage(pid);
            System.out.println("Page désallouée.");

            // 5) Test init() / finish()
            System.out.println("\n=== Test : init() / finish() ===");
            dm.init();
            System.out.println("init() exécuté.");

            PageId pid2 = dm.AllocPage();
            ByteBuffer buffer2 = ByteBuffer.allocate(config.getPagesize());
            buffer2.put("Test init/finish BDDA".getBytes());
            buffer2.flip();
            dm.WritePage(pid2, buffer2);
            System.out.println("Page écrite après init() : " + pid2);

        } catch (IOException e) {
            System.err.println("Erreur I/O : " + e.getMessage());
            e.printStackTrace();

        } finally {
            if (dm != null) {
                dm.finish();
                System.out.println("\nfinish() appelé : état du DiskManager sauvegardé.");
            }
        }
    }
}
