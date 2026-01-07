// package src;

// import java.util.List;

// public class RelationScanner implements IRecordIterator {

//     private final Relation rel;
//     private final List<Record> all;
//     private int idx;

//     // TP7 B4: version simple = GetAllRecords()
//     public RelationScanner(Relation rel) throws Exception {
//         this.rel = rel;
//         this.all = rel.getAllRecords();
//         this.idx = 0;
//     }

//     @Override
//     public Record GetNextRecord() {
//         if (idx >= all.size()) return null;
//         return all.get(idx++);
//     }

//     @Override
//     public void Reset() {
//         idx = 0;
//     }

//     @Override
//     public void Close() {
//         // rien dans la version simple
//     }
// }

package src;

import java.nio.ByteBuffer;
import java.util.List;

public class RelationScanner implements IRecordIterator {

    private final Relation rel;
    private final List<PageId> pages;

    private int pageIdx; // index dans la liste pages
    private int slotIdx; // index slot dans la page courante

    public RelationScanner(Relation rel) throws Exception {
        this.rel = rel;
        this.pages = rel.getDataPages();
        this.pageIdx = 0;
        this.slotIdx = 0;
    }

    @Override
    public Record GetNextRecord() throws Exception {
        int recordSize = rel.getRecordSize();

        while (pageIdx < pages.size()) {
            PageId pid = pages.get(pageIdx);

            // Charger la page
            RamBuffer buf = rel.getBufferManager().getPage(pid);
            ByteBuffer bb = buf.getData();

            int slotsPerPage = rel.getSlotsPerPage();
            int bitmapOffset = 8 + slotsPerPage * recordSize;

            // Parcourir les slots de la page courante
            while (slotIdx < slotsPerPage) {
                if (bb.get(bitmapOffset + slotIdx) == 1) {
                    int pos = 8 + slotIdx * recordSize;

                    Record r = new Record();
                    rel.readFromBuffer(r, bb, pos);

                    slotIdx++;

                    // IMPORTANT : libérer la page après lecture
                    rel.getBufferManager().freePage(pid, false);

                    return r;
                }
                slotIdx++;
            }

            // fin de page => reset slot et passer à page suivante
            slotIdx = 0;
            pageIdx++;

            rel.getBufferManager().freePage(pid, false);
        }

        return null;
    }

    @Override
    public void Reset() throws Exception {
        this.pageIdx = 0;
        this.slotIdx = 0;
    }

    @Override
    public void Close() throws Exception {
        // rien
    }
}
