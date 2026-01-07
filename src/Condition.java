package src;

public class Condition {
    public enum Op {
        EQ, NE, LT, GT, LE, GE
    }

    // un côté est une colonne (indice), l’autre peut être colonne ou constante
    private final int leftColIdx; // toujours une colonne
    private final Op op;

    private final boolean rightIsColumn;
    private final int rightColIdx; // si rightIsColumn
    private final String rightConst; // si !rightIsColumn

    public Condition(int leftColIdx, Op op, int rightColIdx) {
        this.leftColIdx = leftColIdx;
        this.op = op;
        this.rightIsColumn = true;
        this.rightColIdx = rightColIdx;
        this.rightConst = null;
    }

    public Condition(int leftColIdx, Op op, String rightConst) {
        this.leftColIdx = leftColIdx;
        this.op = op;
        this.rightIsColumn = false;
        this.rightColIdx = -1;
        this.rightConst = rightConst;
    }

    public boolean eval(Record r, Relation rel) {
        String leftVal = r.getValues().get(leftColIdx);

        String rightVal = rightIsColumn
                ? r.getValues().get(rightColIdx)
                : rightConst;

        String type = rel.getColumns().get(leftColIdx).getType(); // IMPORTANT: type colonne

        int cmp = compare(leftVal, rightVal, type);

        return switch (op) {
            case EQ -> cmp == 0;
            case NE -> cmp != 0;
            case LT -> cmp < 0;
            case GT -> cmp > 0;
            case LE -> cmp <= 0;
            case GE -> cmp >= 0;
        };
    }

    private int compare(String a, String b, String type) {
        type = type.toUpperCase();

        if (type.equals("INT")) {
            int x = Integer.parseInt(a.trim());
            int y = Integer.parseInt(b.trim());
            return Integer.compare(x, y);
        }
        if (type.equals("FLOAT") ) {
            float x = Float.parseFloat(a.trim());
            float y = Float.parseFloat(b.trim());
            return Float.compare(x, y);
        }

        // VARCHAR/CHAR : ordre lexicographique (TP7 B1 explique ce cas)
        String x = a;
        String y = b;
        return x.compareTo(y);
    }

    public static Op parseOp(String op) {
        return switch (op) {
            case "=" -> Op.EQ;
            case "<>" -> Op.NE;
            case "<" -> Op.LT;
            case ">" -> Op.GT;
            case "<=" -> Op.LE;
            case ">=" -> Op.GE;
            default -> throw new IllegalArgumentException("Opérateur inconnu: " + op);
        };
    }
}
