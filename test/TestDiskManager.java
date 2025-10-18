package test;

import java.io.IOException;
import java.nio.ByteBuffer;
import src.DBConfig;
import src.DiskManager;
import src.PageId;

public class TestDiskManager {

    public static void main(String[] args) {
        try {
            // Charger la configuration
            DBConfig config = DBConfig.LoadDBConfig("config/config_valide.txt");
            DiskManager dm = new DiskManager(config);

            System.out.println("=== Test : Allocation d'une page ===");
            PageId pid = dm.AllocPage();
            System.out.println("Page allouée : " + pid);

            System.out.println("\n=== Test : Écriture dans la page ===");
            ByteBuffer buffer = ByteBuffer.allocate(config.getPagesize());
            buffer.put("Hello BDDA".getBytes());
            buffer.flip();

            dm.WritePage(pid, buffer);
            System.out.println("Écriture réussie.");

            System.out.println("\n=== Test : Lecture de la page ===");
            ByteBuffer readBuffer = ByteBuffer.allocate(config.getPagesize());
            dm.ReadPage(pid, readBuffer);
            byte[] data = new byte[readBuffer.remaining()];
            readBuffer.get(data);
            int len = 0;
            for (; len < data.length; len++) {
                if (data[len] == 0) break;
            }
            String content = new String(data, 0, len);
            System.out.println("Contenu lu : '" + content + "'");

            System.out.println("\n=== Test : Désallocation de la page ===");
            dm.DeallocPage(pid);
            System.out.println("Page désallouée avec succès.");

            // ---------------------------------------------------
            // Test des fonctions init() et finish()
            // ---------------------------------------------------
            System.out.println("\n=== Test : init() et finish() ===");

            dm.init();
            System.out.println(" init() exécutée avec succès.");

            // On teste une allocation/écriture après init()
            PageId testPage = dm.AllocPage();
            ByteBuffer testBuffer = ByteBuffer.allocate(config.getPagesize());
            testBuffer.put("Test init/finish BDDA".getBytes());
            testBuffer.flip();
            dm.WritePage(testPage, testBuffer);
            System.out.println(" Page écrite après init() : " + testPage);

            // On termine par finish()
            dm.finish();
            System.out.println("finish() exécutée avec succès.");

        } catch (IOException e) {
            System.err.println("Erreur I/O : " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
