package src;

import java.util.List;

public class RecordPrinter {

    private final IRecordIterator it;

    public RecordPrinter(IRecordIterator it) {
        this.it = it;
    }

    public int printAll() throws Exception {
        int count = 0;
        Record r;

        while ((r = it.GetNextRecord()) != null) {
            List<String> vals = r.getValues();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < vals.size(); i++) {
                sb.append(vals.get(i));
                if (i < vals.size() - 1)
                    sb.append(" ; "); // TP7 A3
            }
            sb.append("."); // TP7 A3
            System.out.println(sb.toString());
            count++;
        }

        System.out.println("Total selected records = " + count); // TP7 A3
        return count;
    }
}
