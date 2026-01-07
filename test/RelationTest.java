package test;

import src.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RelationTest {

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

  public static void main(String[] args) throws IOException {

    System.out.println("=== TEST Relation + Save/Load via DBManager ===");

    DBConfig cfg = DBConfig.LoadDBConfig("config/config_valide.txt");
    cleanDataDirectory(cfg.getDbpath());

    DiskManager disk1 = new DiskManager(cfg);
    BufferManager buffer1 = new BufferManager(cfg, disk1);

    // 1) Colonnes
    List<ColumnInfo> cols = new ArrayList<>();
    cols.add(new ColumnInfo("id", "INT", 4));
    cols.add(new ColumnInfo("nom", "CHAR", 20));

    // 2) Relation
    PageId headerId = disk1.AllocPage();
    Relation rel = new Relation("Etudiants", cols, disk1, buffer1, headerId);
    rel.initHeaderPage();

    // 3) Insert
    src.Record r1 = new src.Record();
    r1.addValue("1");
    r1.addValue("zahra");
    src.Record r2 = new src.Record();
    r2.addValue("2");
    r2.addValue("hajar");
    src.Record r3 = new src.Record();
    r3.addValue("3");
    r3.addValue("racha");

    RecordId id1 = rel.InsertRecord(r1);
    RecordId id2 = rel.InsertRecord(r2);
    RecordId id3 = rel.InsertRecord(r3);

    System.out.println("\nRecordIds:");
    System.out.println(id1);
    System.out.println(id2);
    System.out.println(id3);

    System.out.println("\n=== Records actuels ===");
    for (src.Record rec : rel.getAllRecords()) {
      System.out.println(rec);
    }

    // 4) Delete
    rel.DeleteRecord(id2);
    System.out.println("\n=== Après suppression ===");
    for (src.Record rec : rel.getAllRecords()) {
      System.out.println(rec);
    }

    // 5) Save via DBManager
    DBManager db1 = new DBManager(cfg, disk1, buffer1);
    db1.AddTable(rel);
    db1.SaveState();

    buffer1.flushAllPages();
    disk1.finish();

    // 6) Redémarrage
    DiskManager disk2 = new DiskManager(cfg);
    BufferManager buffer2 = new BufferManager(cfg, disk2);
    DBManager db2 = new DBManager(cfg, disk2, buffer2);
    db2.LoadState();

    System.out.println("\n=== Tables chargées ===");
    db2.DescribeAllTables();

    Relation loaded = db2.GetTable("Etudiants");
    if (loaded == null) {
      System.err.println("ERREUR: Etudiants introuvable après LoadState !");
    } else {
      loaded.initManagers(disk2, buffer2);
      System.out.println("\n=== Records de la table chargée ===");
      for (src.Record rec : loaded.getAllRecords()) {
        System.out.println(rec);
      }
    }

    buffer2.flushAllPages();
    disk2.finish();
  }
}
