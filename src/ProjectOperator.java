package src;

import java.util.ArrayList;
import java.util.List;

public class ProjectOperator implements IRecordIterator {

    private final IRecordIterator child;
    private final int[] keepCols; // indices colonnes à garder, null => *

    public ProjectOperator(IRecordIterator child, int[] keepCols) {
        this.child = child;
        this.keepCols = keepCols;
    }

    @Override
    public Record GetNextRecord() throws Exception {
        Record r = child.GetNextRecord();
        if (r == null) return null;

        if (keepCols == null) return r; // SELECT *

        List<String> oldVals = r.getValues();
        List<String> newVals = new ArrayList<>();
        for (int idx : keepCols) newVals.add(oldVals.get(idx));

        return new Record(newVals);
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
