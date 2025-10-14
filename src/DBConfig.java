package src;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class DBConfig {
    private String dbpath;
    private int pageSize;
    private int dmMaxFileCount;
    private long dmMaxFileSize;

    public DBConfig(String dbpath, int pageSize, int dmMaxFileCount, long dmMaxFileSize) {
        this.dbpath = dbpath;
        this.pageSize = pageSize;
        this.dmMaxFileCount = dmMaxFileCount;
        this.dmMaxFileSize = dmMaxFileSize;
    }
    public String getDbpath() {
        return dbpath;
    }

    public int getPagesize() {
        return pageSize;
    }

    public int getDmMaxFileCount() {
        return dmMaxFileCount;
    }

    public long getDmMaxFileSize() {
        return dmMaxFileSize;
    }

    @Override
    public String toString() {
        return "DBConfig { dbpath='" + dbpath + "', pageSize=" + pageSize +
               ", dmMaxFileCount=" + dmMaxFileCount + ", dmMaxFileSize=" + dmMaxFileSize + " }";
    }

    public static DBConfig LoadDBConfig(String fichier_config) {
        String dbpath = null;
        int pageSize = 0;
        int dmMaxFileCount = 0;
        long dmMaxFileSize = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(fichier_config))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("dbpath")) {
                    dbpath = line.split("=")[1].trim().replace("'", "");
                } else if (line.startsWith("pagesize")) {
                    pageSize = Integer.parseInt(line.split("=")[1].trim());
                } else if (line.startsWith("dm_maxfilecount")) {
                    dmMaxFileCount = Integer.parseInt(line.split("=")[1].trim());
                } else if (line.startsWith("dm_maxfilesize")) {
                    dmMaxFileSize = Long.parseLong(line.split("=")[1].trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (dbpath == null || pageSize == 0 || dmMaxFileCount == 0 || dmMaxFileSize == 0) {
            throw new RuntimeException("Paramètres manquants dans le fichier config !");
        }

        return new DBConfig(dbpath, pageSize, dmMaxFileCount, dmMaxFileSize);
    }
}