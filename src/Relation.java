package src;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Relation {

    private String name;
    private List<ColumnInfo> columns;
    private PageId headerPageId;
    private int slotsPerPage;
    private DiskManager diskManager;
    private BufferManager bufferManager;

    // Constructeur pour charger une table existante ou créer un objet Relation
    public Relation(String name, List<ColumnInfo> columns,
            DiskManager diskManager, BufferManager bufferManager,
            PageId headerPageId) {

        this.name = name;
        this.columns = columns;
        this.diskManager = diskManager;
        this.bufferManager = bufferManager;

        if (columns != null && !columns.isEmpty()) {
            this.slotsPerPage = computeSlotsPerPage();
        } else {
            this.slotsPerPage = 0;
        }

        this.headerPageId = headerPageId;
    }

    // Constructeur pour usage interne (comme dans DBManager)
    public Relation(String name) {
        this.name = name;
        this.columns = new ArrayList<>();
    }

    // =======================
    // Getters / Setters / Initialisation
    // =======================

    public String getName() {
        return name;
    }

    public List<ColumnInfo> getColumns() {
        return columns;
    }

    public PageId getHeaderPageId() {
        return headerPageId;
    }

    public void setHeaderPageId(PageId headerPageId) {
        this.headerPageId = headerPageId;
    }

    public void initManagers(DiskManager diskManager, BufferManager bufferManager) {
        this.diskManager = diskManager;
        this.bufferManager = bufferManager;
        this.slotsPerPage = computeSlotsPerPage();
    }

    // Header Page: (Offset 0: FULL File/Page | Offset 8: NOT-FULL File/Page)
    public void initHeaderPage() throws IOException {
        RamBuffer buf = bufferManager.getPage(headerPageId);
        ByteBuffer bb = buf.getData();
        bb.position(0);
        bb.putInt(-1); // FULL head file
        bb.putInt(-1); // FULL head page
        bb.putInt(-1); // NOT-FULL head file
        bb.putInt(-1); // NOT-FULL head page
        bufferManager.freePage(headerPageId, true); // Marquer dirty
    }

    public int getColumnCount() {
        return columns.size();
    }

    public void addColumn(String name, String type, int size) {
        columns.add(new ColumnInfo(name, type, size));
        slotsPerPage = computeSlotsPerPage();
    }

    public int getRecordSize() {
        int size = 0;
        for (ColumnInfo c : columns) {
            switch (c.getType()) {
                case "INT":
                case "FLOAT":

                    size += 4;
                    break;

                case "CHAR":
                case "VARCHAR":
                    size += c.getSize();
                    break;
                default:
                    throw new IllegalArgumentException("Type inconnu: " + c.getType());
            }
        }
        return size;
    }

    public int computeSlotsPerPage() {
        int recordSize = getRecordSize();
        int pageSize = diskManager.getDBConfig().getPagesize();
        if (recordSize <= 0)
            return 0;
        return (pageSize - 8) / (recordSize + 1);
    }

    // =======================
    // Record Serialization
    // =======================

    public void writeRecordToBuffer(src.Record record, ByteBuffer buff, int pos) {
        buff.position(pos);
        List<String> values = record.getValues();
        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo col = columns.get(i);
            String value = values.get(i);

            switch (col.getType()) {
                case "INT":
                    buff.putInt(Integer.parseInt(value));
                    break;
                case "FLOAT":
                    buff.putFloat(Float.parseFloat(value));
                    break;

                case "CHAR":
                case "VARCHAR":
                    byte[] strBytes = value.getBytes(StandardCharsets.UTF_8);
                    int max = col.getSize();
                    buff.put(strBytes, 0, Math.min(strBytes.length, max));

                    if (strBytes.length < max) {
                        for (int k = strBytes.length; k < max; k++) {
                            buff.put((byte) 0);
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Type inconnu: " + col.getType());
            }
        }
    }

    public void readFromBuffer(src.Record record, ByteBuffer buff, int pos) {
        record.clear();
        buff.position(pos);
        for (ColumnInfo col : columns) {
            switch (col.getType()) {
                case "INT":
                    record.addValue(String.valueOf(buff.getInt()));
                    break;
                case "FLOAT":

                    record.addValue(String.valueOf(buff.getFloat()));
                    break;

                case "CHAR":
                case "VARCHAR":
                    int max = col.getSize();
                    byte[] data = new byte[max];
                    buff.get(data);
                    String raw = new String(data, StandardCharsets.UTF_8).trim();
                    record.addValue(raw);
                    break;
                default:
                    throw new IllegalArgumentException("Type inconnu: " + col.getType());
            }
        }
    }

    // =======================
    // Gestion des Pages de Données
    // =======================

    public void addDataPage() throws IOException {
        PageId newPageId = diskManager.AllocPage();
        RamBuffer newBuf = bufferManager.getPage(newPageId);
        ByteBuffer bb = newBuf.getData();
        int recordsZone = slotsPerPage * getRecordSize();
        int bitmapOffset = 8 + recordsZone;

        bb.putInt(0, -1); // next file
        bb.putInt(4, -1); // next page

        for (int i = 0; i < slotsPerPage; i++)
            bb.put(bitmapOffset + i, (byte) 0);

        bufferManager.freePage(newPageId, true);
        addToList(newPageId, 8); // L'ajouter à NOT-FULL
    }

    private PageId getFreeDataPageId(int sizeRecord) throws IOException {
        RamBuffer hbuf = bufferManager.getPage(headerPageId);
        ByteBuffer hb = hbuf.getData();

        PageId current = new PageId(hb.getInt(8), hb.getInt(12));
        bufferManager.freePage(headerPageId, false);

        while (current.getFileIdx() != -1) {
            RamBuffer buf = bufferManager.getPage(current);
            ByteBuffer bb = buf.getData();

            int recordSize = getRecordSize();
            int recordsZone = slotsPerPage * recordSize;
            int bitmapOffset = 8 + recordsZone;

            for (int i = 0; i < slotsPerPage; i++) {
                if (bb.get(bitmapOffset + i) == 0) {
                    bufferManager.freePage(current, false);
                    return current;
                }
            }

            int nextFile = bb.getInt(0);
            int nextPage = bb.getInt(4);
            bufferManager.freePage(current, false);

            current = new PageId(nextFile, nextPage);
        }
        return null;
    }

    public RecordId writeRecordToDataPage(src.Record record, PageId pageId) throws IOException {
        RamBuffer buf = bufferManager.getPage(pageId);
        ByteBuffer bb = buf.getData();
        int recordSize = getRecordSize();
        int bitmapOffset = 8 + slotsPerPage * recordSize;

        int freeSlot = -1;
        for (int i = 0; i < slotsPerPage; i++) {
            if (bb.get(bitmapOffset + i) == 0) {
                freeSlot = i;
                break;
            }
        }
        if (freeSlot == -1) {
            bufferManager.freePage(pageId, false);
            throw new IOException("Aucun slot libre sur la page " + pageId);
        }

        bb.put(bitmapOffset + freeSlot, (byte) 1);
        int slotOffset = 8 + freeSlot * recordSize;
        writeRecordToBuffer(record, bb, slotOffset);

        bufferManager.freePage(pageId, true);
        return new RecordId(pageId, freeSlot);
    }

    // ⚠️ MÉTHODE RESTAURÉE
    public List<src.Record> getRecordsInDataPage(PageId pageId) throws IOException {
        List<src.Record> result = new ArrayList<>();
        RamBuffer buf = bufferManager.getPage(pageId);
        ByteBuffer bb = buf.getData();
        int recordSize = getRecordSize();
        int bitmapOffset = 8 + slotsPerPage * recordSize;

        for (int i = 0; i < slotsPerPage; i++) {
            if (bb.get(bitmapOffset + i) == 1) {
                int slotOffset = 8 + i * recordSize;
                src.Record rec = new src.Record();
                readFromBuffer(rec, bb, slotOffset);
                result.add(rec);
            }
        }
        bufferManager.freePage(pageId, false);
        return result;
    }

    public List<src.Record> getAllRecords() throws IOException {
        List<src.Record> all = new ArrayList<>();
        List<PageId> pages = getDataPages();
        for (PageId pid : pages) {
            all.addAll(getRecordsInDataPage(pid));
        }
        return all;
    }

    // ---------------------------
    // Insert / Delete
    // ---------------------------
    public RecordId InsertRecord(src.Record record) throws IOException {
        PageId pid = getFreeDataPageId(getRecordSize());
        if (pid == null) {
            addDataPage();
            pid = getFreeDataPageId(getRecordSize());
        }
        RecordId rid = writeRecordToDataPage(record, pid);
        if (isPageFull(pid)) {
            removeFromList(pid, 8);
            addToList(pid, 0);
        }
        return rid;
    }

    public void DeleteRecord(RecordId rid) throws IOException {
        PageId pid = rid.getPageId();
        int slot = rid.getSlotIdx();
        RamBuffer buf = bufferManager.getPage(pid);
        ByteBuffer bb = buf.getData();
        int recordSize = getRecordSize();
        int bitmapOffset = 8 + slotsPerPage * recordSize;

        if (bb.get(bitmapOffset + slot) == 0) {
            bufferManager.freePage(pid, false);
            return;
        }
        bb.put(bitmapOffset + slot, (byte) 0);
        bufferManager.freePage(pid, true);

        if (isPageEmpty(pid)) {
            removeFromList(pid, 0);
            removeFromList(pid, 8);
            diskManager.DeallocPage(pid);
            return;
        }

        if (isInList(pid, 0) && !isPageFull(pid)) {
            removeFromList(pid, 0);
            addToList(pid, 8);
        }
    }

    // ⚠️ MÉTHODE RESTAURÉE ET VÉRIFIÉE
    private void addToList(PageId pid, int headerOffset) throws IOException {
        RamBuffer hbuf = bufferManager.getPage(headerPageId);
        ByteBuffer hb = hbuf.getData();
        hb.position(headerOffset);
        int headFile = hb.getInt();
        int headPage = hb.getInt();

        // 1. Charger la page à ajouter et lui donner l'ancien PageId de tête comme
        // suivant
        RamBuffer pbuf = bufferManager.getPage(pid);
        ByteBuffer pbb = pbuf.getData();
        pbb.putInt(0, headFile); // nextFile = ancien head file
        pbb.putInt(4, headPage); // nextPage = ancien head page
        bufferManager.freePage(pbuf.getPageId(), true); // La page est modifiée (dirty)

        // 2. Mettre à jour la tête de la liste dans l'en-tête
        hb.position(headerOffset);
        hb.putInt(pid.getFileIdx()); // nouveau head file = pid file
        hb.putInt(pid.getPageIdx()); // nouveau head page = pid page

        bufferManager.freePage(headerPageId, true); // L'en-tête est modifiée (dirty)
    }

    private void removeFromList(PageId pidToRemove, int headerOffset) throws IOException {
        RamBuffer hbuf = bufferManager.getPage(headerPageId);
        ByteBuffer hb = hbuf.getData();
        hb.position(headerOffset);
        int file = hb.getInt();
        int page = hb.getInt();

        if (file == -1) {
            bufferManager.freePage(headerPageId, false);
            return;
        }

        PageId head = new PageId(file, page);

        // Cas 1: La page à supprimer est la TÊTE
        if (head.equals(pidToRemove)) {
            RamBuffer headBuf = bufferManager.getPage(head);
            ByteBuffer hbb = headBuf.getData();
            int nf = hbb.getInt(0), np = hbb.getInt(4); // Lire le "suivant" de la tête
            bufferManager.freePage(head, false);

            // Mettre à jour la nouvelle tête dans l'EN-TÊTE
            hb.position(headerOffset);
            hb.putInt(nf);
            hb.putInt(np);

            bufferManager.freePage(headerPageId, true);
            return;
        }

        // Cas 2: Parcourir le reste de la liste
        PageId prev = head;
        while (true) {
            RamBuffer prevBuf = bufferManager.getPage(prev);
            ByteBuffer pbb = prevBuf.getData();
            int nextFile = pbb.getInt(0), nextPage = pbb.getInt(4);

            PageId nextPid = new PageId(nextFile, nextPage);

            // 1. Vérification de la fin de la liste (next == null)
            if (nextFile == -1) {
                bufferManager.freePage(prev, false);
                break;
            }

            // 2. Le nœud suivant est celui à supprimer
            if (nextPid.equals(pidToRemove)) {

                RamBuffer nextBuf = bufferManager.getPage(nextPid);
                ByteBuffer nbb = nextBuf.getData();
                int afterFile = nbb.getInt(0), afterPage = nbb.getInt(4);
                bufferManager.freePage(nextPid, false);

                pbb.putInt(0, afterFile);
                pbb.putInt(4, afterPage);

                bufferManager.freePage(prev, true);

                bufferManager.freePage(headerPageId, false);
                return;
            }

            bufferManager.freePage(prev, false);
            prev = nextPid;
        }

        bufferManager.freePage(headerPageId, false);
    }

    private boolean isInList(PageId pid, int headerOffset) throws IOException {
        RamBuffer hbuf = bufferManager.getPage(headerPageId);
        ByteBuffer hb = hbuf.getData();
        hb.position(headerOffset);
        int file = hb.getInt();
        int page = hb.getInt();
        bufferManager.freePage(headerPageId, false);
        if (file == -1)
            return false;
        PageId current = new PageId(file, page);
        while (current.getFileIdx() != -1) {
            if (current.equals(pid))
                return true;
            RamBuffer buf = bufferManager.getPage(current);
            ByteBuffer bb = buf.getData();
            int nf = bb.getInt(0), np = bb.getInt(4);
            bufferManager.freePage(current, false);
            current = new PageId(nf, np);
        }
        return false;
    }

    private boolean isPageEmpty(PageId pid) throws IOException {
        RamBuffer buf = bufferManager.getPage(pid);
        ByteBuffer bb = buf.getData();
        int recordSize = getRecordSize();
        int bitmapOffset = 8 + slotsPerPage * recordSize;
        for (int i = 0; i < slotsPerPage; i++)
            if (bb.get(bitmapOffset + i) == 1) {
                bufferManager.freePage(pid, false);
                return false;
            }
        bufferManager.freePage(pid, false);
        return true;
    }

    private boolean isPageFull(PageId pid) throws IOException {
        RamBuffer buf = bufferManager.getPage(pid);
        ByteBuffer bb = buf.getData();
        int recordSize = getRecordSize();
        int bitmapOffset = 8 + slotsPerPage * recordSize;
        for (int i = 0; i < slotsPerPage; i++)
            if (bb.get(bitmapOffset + i) == 0) {
                bufferManager.freePage(pid, false);
                return false;
            }
        bufferManager.freePage(pid, false);
        return true;
    }

    public List<PageId> getDataPages() throws IOException {
        List<PageId> pages = new ArrayList<>();
        Set<PageId> visited = new HashSet<>();

        RamBuffer hbuf = bufferManager.getPage(headerPageId);
        ByteBuffer hb = hbuf.getData();

        // Parcourir la liste FULL (Offset 0) et la liste NOT-FULL (Offset 8)
        for (int headerOffset : new int[] { 0, 8 }) {
            hb.position(headerOffset);
            PageId pid = new PageId(hb.getInt(), hb.getInt());

            while (pid.getFileIdx() != -1) {

                if (visited.contains(pid)) {
                    // System.err.println("Avertissement: Cycle ou double inclusion détecté dans les
                    // pages de données.");
                    break;
                }

                pages.add(pid);
                visited.add(pid);

                RamBuffer buf = bufferManager.getPage(pid);
                ByteBuffer bb = buf.getData();

                int nextFile = bb.getInt(0);
                int nextPage = bb.getInt(4);

                bufferManager.freePage(pid, false);

                pid = new PageId(nextFile, nextPage);
            }
        }

        bufferManager.freePage(headerPageId, false);

        return pages;
    }

    // Méthode ToString()
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" (");

        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo col = columns.get(i);
            sb.append(col.getName()).append(":").append(col.getType());
            if (col.getType().equals("CHAR") || col.getType().equals("VARCHAR")) {
                sb.append("(").append(col.getSize()).append(")");
            }
            if (i < columns.size() - 1)
                sb.append(",");
        }

        sb.append(")");
        return sb.toString();
    }

    public List<RecordId> getAllRecordIds() throws IOException {
        List<RecordId> ids = new ArrayList<>();
        List<PageId> pages = getDataPages();

        int recordSize = getRecordSize();
        for (PageId pid : pages) {
            RamBuffer buf = bufferManager.getPage(pid);
            ByteBuffer bb = buf.getData();

            int bitmapOffset = 8 + slotsPerPage * recordSize;

            for (int i = 0; i < slotsPerPage; i++) {
                if (bb.get(bitmapOffset + i) == 1) {
                    ids.add(new RecordId(pid, i));
                }
            }
            bufferManager.freePage(pid, false);
        }
        return ids;
    }

    public Record readRecordById(RecordId rid) throws IOException {
        PageId pid = rid.getPageId();
        int slot = rid.getSlotIdx();

        RamBuffer buf = bufferManager.getPage(pid);
        ByteBuffer bb = buf.getData();

        int recordSize = getRecordSize();
        int slotOffset = 8 + slot * recordSize;

        Record r = new Record();
        readFromBuffer(r, bb, slotOffset);

        bufferManager.freePage(pid, false);
        return r;
    }

    public void overwriteRecord(RecordId rid, Record newRecord) throws IOException {
        PageId pid = rid.getPageId();
        int slot = rid.getSlotIdx();

        RamBuffer buf = bufferManager.getPage(pid);
        ByteBuffer bb = buf.getData();

        int recordSize = getRecordSize();
        int slotOffset = 8 + slot * recordSize;

        writeRecordToBuffer(newRecord, bb, slotOffset);

        bufferManager.freePage(pid, true);
    }

    public BufferManager getBufferManager() {
        return bufferManager;
    }

    public int getSlotsPerPage() {
        return slotsPerPage;
    }

}