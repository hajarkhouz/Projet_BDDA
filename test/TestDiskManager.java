package test;

import src.DBConfig;
import src.DiskManager;
import src.PageId;

import java.io.IOException;
import java.nio.ByteBuffer;

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
            dm.WritePage(pid, buffer);
            System.out.println("Écriture réussie.");

            System.out.println("\nTest : Lecture de la page");
            ByteBuffer readBuffer = ByteBuffer.allocate(config.getPagesize());
            dm.ReadPage(pid, readBuffer);
            readBuffer.flip();
            byte[] data = new byte[readBuffer.remaining()];
            readBuffer.get(data);
            System.out.println("Contenu lu : " + new String(data).trim());

            System.out.println("\nTest : Désallocation de la page");
            dm.DeallocPage(pid);
            System.out.println("Page désallouée avec succès.");

        } catch (IOException e) {
            System.err.println("Erreur I/O : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }
}
