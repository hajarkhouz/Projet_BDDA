package src;

import java.util.List;

public class SelectOperator implements IRecordIterator {

    private final IRecordIterator child;
    private final Relation rel;
    private final List<Condition> conditions;

    public SelectOperator(IRecordIterator child, Relation rel, List<Condition> conditions) {
        this.child = child;
        this.rel = rel;
        this.conditions = conditions;
    }

    @Override
    public Record GetNextRecord() throws Exception {
        while (true) {
            Record r = child.GetNextRecord();
            if (r == null)
                return null;

            boolean ok = true;
            for (Condition c : conditions) {
                if (!c.eval(r, rel)) {
                    ok = false;
                    break;
                }
            }
            if (ok)
                return r;
        }
    }

    @Override
    public void Reset() throws Exception {
        child.Reset();
    }

    @Override
    public void Close() throws Exception {
        child.Close();
    }
}
