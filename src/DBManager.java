package src;

import java.io.*;
import java.util.*;

public class DBManager {

    private DBConfig config;
    private DiskManager diskManager;
    private BufferManager bufferManager;
    private Map<String, Relation> tables; // stockage des tables par nom

    // --------------------------
    // Constructeur MODIFIÉ
    // Accepte DiskManager et BufferManager comme arguments (Injection de
    // Dépendances)
    // --------------------------
    public DBManager(DBConfig cfg, DiskManager dm, BufferManager bm) {
        this.config = cfg;
        // Utilisez les instances existantes pour assurer la cohérence du SGBD
        this.diskManager = dm;
        this.bufferManager = bm;
        this.tables = new HashMap<>();
    }

    private String key(String name) {
        return name.trim().toUpperCase();
    }

    // --------------------------
    // Gestion des tables
    // --------------------------
    public void AddTable(Relation tab) {
        if (tab == null) return;
        String k = key(tab.getName());
        if (!tables.containsKey(k)) {
            tables.put(k, tab);
        }
    }
    

    public Relation GetTable(String nomTable) {
        return tables.get(key(nomTable));
    }

    public void RemoveTable(String nomTable) throws IOException {
        Relation r = tables.get(key(nomTable));
        if (r == null)
            // Lève une exception si la table n'existe pas
            throw new IllegalArgumentException(
                    "Erreur: La table '" + nomTable + "' n'existe pas dans la base de données.");

        // Copie de la liste des PageId pour éviter des problèmes lors de la
        // désallocation
        List<PageId> pagesToRemove = new ArrayList<>(r.getDataPages());

        // Supprimer toutes les pages de données
        for (PageId pid : pagesToRemove) {
            diskManager.DeallocPage(pid);
        }

        // Supprimer la header page
        diskManager.DeallocPage(r.getHeaderPageId());

        tables.remove(key(nomTable));
    }

    public void RemoveAllTables() throws IOException {
        List<String> noms = new ArrayList<>(tables.keySet());
        for (String nom : noms) {
            RemoveTable(nom);
        }
        tables.clear();
    }

    // --------------------------
    // Affichage
    // --------------------------
    public void DescribeTable(String nomTable) {
        Relation r = tables.get(key(nomTable));
        if (r != null) System.out.println(r);
    }
    
    public void DescribeAllTables() {
        for (Relation r : tables.values()) {
            System.out.println(r);
        }
    }
    

    // --------------------------
    // Sauvegarde / Chargement
    // --------------------------
    public void SaveState() {
        File saveFile = new File(config.getDbpath(), "database.save");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFile))) {
            oos.writeInt(tables.size());
            for (Relation r : tables.values()) {
                // Nom et colonnes
                oos.writeObject(r.getName());
                oos.writeInt(r.getColumns().size());
                for (ColumnInfo c : r.getColumns()) {
                    oos.writeObject(c.getName());
                    oos.writeObject(c.getType());
                    oos.writeInt(c.getSize());
                }
                // Header page
                PageId header = r.getHeaderPageId();
                oos.writeInt(header.getFileIdx());
                oos.writeInt(header.getPageIdx());
            }
            oos.flush();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde des tables !", e);
        }
    }

    public void LoadState() {
        File saveFile = new File(config.getDbpath(), "database.save");
        if (!saveFile.exists())
            return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile))) {
            int tableCount = ois.readInt();
            for (int i = 0; i < tableCount; i++) {
                String name = (String) ois.readObject();
                int colCount = ois.readInt();
                List<ColumnInfo> cols = new ArrayList<>();
                for (int j = 0; j < colCount; j++) {
                    String colName = (String) ois.readObject();
                    String colType = (String) ois.readObject();
                    int colSize = ois.readInt();
                    cols.add(new ColumnInfo(colName, colType, colSize));
                }
                int fileIdx = ois.readInt();
                int pageIdx = ois.readInt();

                // Utilise les managers injectés pour créer la relation
                Relation r = new Relation(name, cols, diskManager, bufferManager, new PageId(fileIdx, pageIdx));
                AddTable(r);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Erreur lors du chargement de l'état du DBManager !", e);
        }
    }

    // --------------------------
    // Getter pour tests
    // --------------------------
    public Map<String, Relation> getTables() {
        return tables;
    }
}