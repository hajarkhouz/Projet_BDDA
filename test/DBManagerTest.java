
package test;

import src.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DBManagerTest {

    // ✅ Suppression récursive propre
    private static void deleteRec(File f) {
        if (f == null || !f.exists())
            return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children)
                    deleteRec(c);
            }
        }
        if (!f.delete()) {
            System.err.println("Avertissement: Impossible de supprimer : " + f.getAbsolutePath());
        }
    }

    private static void cleanDataDirectory(String dbPath) {
        File dataDir = new File(dbPath);
        if (dataDir.exists()) {
            File[] files = dataDir.listFiles();
            if (files != null) {
                for (File file : files)
                    deleteRec(file);
            }
        } else {
            dataDir.mkdirs();
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("=== TEST COMPLET DE DBManager ===");

        DBConfig cfg = DBConfig.LoadDBConfig("config/config_valide.txt");
        cleanDataDirectory(cfg.getDbpath());

        DiskManager disk1 = null;
        BufferManager buffer1 = null;

        DiskManager disk2 = null;
        BufferManager buffer2 = null;

        try {
            disk1 = new DiskManager(cfg);
            buffer1 = new BufferManager(cfg, disk1);

            // --------------------------
            // 1) Créer la table Etudiants
            // --------------------------
            List<ColumnInfo> cols = new ArrayList<>();
            cols.add(new ColumnInfo("id", "INT", 4));
            cols.add(new ColumnInfo("nom", "CHAR", 20));

            PageId header = disk1.AllocPage();
            Relation etudiants = new Relation("Etudiants", cols, disk1, buffer1, header);
            etudiants.initHeaderPage();

            DBManager db1 = new DBManager(cfg, disk1, buffer1);
            db1.AddTable(etudiants);

            System.out.println("\n=== DescribeAllTables après AddTable ===");
            db1.DescribeAllTables();

            // --------------------------
            // 2) Insert records
            // --------------------------
            src.Record r1 = new src.Record();
            r1.addValue("1");
            r1.addValue("zahra");
            src.Record r2 = new src.Record();
            r2.addValue("2");
            r2.addValue("hajar");
            src.Record r3 = new src.Record();
            r3.addValue("3");
            r3.addValue("racha");

            RecordId id1 = etudiants.InsertRecord(r1);
            RecordId id2 = etudiants.InsertRecord(r2);
            RecordId id3 = etudiants.InsertRecord(r3);

            System.out.println("\nRecordIds:");
            System.out.println(id1);
            System.out.println(id2);
            System.out.println(id3);

            System.out.println("\n=== Records actuels ===");
            for (src.Record rec : etudiants.getAllRecords()) {
                System.out.println(rec);
            }

            // --------------------------
            // 3) Delete record
            // --------------------------
            etudiants.DeleteRecord(id2);

            System.out.println("\n=== Après suppression id2 ===");
            for (src.Record rec : etudiants.getAllRecords()) {
                System.out.println(rec);
            }

            // --------------------------
            // 4) SaveState + flush + finish
            // --------------------------
            db1.SaveState();
            buffer1.flushAllPages();
            disk1.finish();

            // --------------------------
            // 5) Simuler redémarrage (nouveaux managers)
            // --------------------------
            disk2 = new DiskManager(cfg);
            buffer2 = new BufferManager(cfg, disk2);

            DBManager db2 = new DBManager(cfg, disk2, buffer2);
            db2.LoadState();

            System.out.println("\n=== Tables chargées depuis database.save ===");
            db2.DescribeAllTables();

            // --------------------------
            // 6) Vérifier GetTable + lire records
            // --------------------------
            Relation loaded = db2.GetTable("Etudiants");
            if (loaded == null) {
                System.err.println("ERREUR: GetTable n'a pas retrouvé Etudiants !");
                return;
            }

            // ✅ assurer que la relation utilise les managers du redémarrage
            loaded.initManagers(disk2, buffer2);

            System.out.println("\n=== Records de la table chargée ===");
            for (src.Record rec : loaded.getAllRecords()) {
                System.out.println(rec);
            }

            // --------------------------
            // 7) Test RemoveTable
            // --------------------------
            db2.RemoveTable("Etudiants");
            System.out.println("\n=== Après RemoveTable('Etudiants') ===");
            db2.DescribeAllTables();

        } finally {
            // Flush/finish propre
            if (buffer2 != null) {
                try {
                    buffer2.flushAllPages();
                } catch (Exception ignored) {
                }
            }
            if (disk2 != null) {
                try {
                    disk2.finish();
                } catch (Exception ignored) {
                }
            }

            if (buffer1 != null) {
                try {
                    buffer1.flushAllPages();
                } catch (Exception ignored) {
                }
            }
            if (disk1 != null) {
                try {
                    disk1.finish();
                } catch (Exception ignored) {
                }
            }
        }

        System.out.println("\nFin du test DBManager.");
    }
}
