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

            System.out.println("Test : Allocation d'une page");
            PageId pid = dm.AllocPage();
            System.out.println("Page allouée : " + pid);

            System.out.println("\nTest : Écriture dans la page");// Écriture de données dans la page allouée
            ByteBuffer buffer = ByteBuffer.allocate(config.getPagesize());
            buffer.put("Hello BDDA".getBytes());
            buffer.flip();

            //////////////////
            ///
            /// 
          //  System.out.println("DEBUG : binDir = " + dm.getBinDir());
           // System.out.println("DEBUG : pid.fileIdx = " + pid.getFileIdx());
           // System.out.println("DEBUG : pid.pageIdx = " + pid.getPageIdx());
           // System.out.println("DEBUG : buffer.capacity() = " + buffer.capacity());
            ////////////////////
            dm.WritePage(pid, buffer);
            System.out.println("Écriture réussie.");

            System.out.println("\nTest : Lecture de la page");

            ByteBuffer readBuffer = ByteBuffer.allocate(config.getPagesize());
            dm.ReadPage(pid, readBuffer);
            //readBuffer.flip();
            byte[] data = new byte[readBuffer.remaining()];
            readBuffer.get(data);
            // Afficher uniquement les caractères non nuls
            int len = 0;
            for (; len < data.length; len++) {
                if (data[len] == 0) break; // fin du texte réel
            }
            String content = new String(data, 0, len);
            System.out.println("Contenu lu : '" + content + "'");

            System.out.println("\nTest : Désallocation de la page");
            dm.DeallocPage(pid);
            System.out.println("Page désallouée avec succès.");

        } catch (IOException e) {
            System.err.println("Erreur I/O : " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); // affiche l'erreur exactement ou elle est 
        }
    }
}
