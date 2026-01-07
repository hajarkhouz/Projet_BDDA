package src;

public class ColumnInfo {

    private String name;   // nom colonne
    private String type;   // INT, FLOAT, CHAR, VARCHAR
    private int size;      // T dans CHAR(T) ou VARCHAR(T)

    public ColumnInfo(String name, String type, int size) {
        this.name = name;
        this.type = type;
        this.size = size;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public int getSize() { return size; }

    @Override
    public String toString() {
        return name + " " + type + "(" + size + ")";
    }
}
