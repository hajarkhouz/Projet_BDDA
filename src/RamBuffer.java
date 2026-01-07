package src;

import java.nio.ByteBuffer;

public class RamBuffer {

    private PageId pageId;       // null si buffer vide
    private int pinCount;        // nombre d’utilisateurs
    private boolean dirty;       // page modifiée ou non
    private ByteBuffer data;     // contenu de la page
    private long lastAccessOrder; // compteur global pour LRU/MRU

    public RamBuffer(int pageSize) {
        data = ByteBuffer.wrap(new byte[pageSize]);
        this.pageId = null;
        this.pinCount = 0;
        this.dirty = false;
        this.lastAccessOrder = 0;
    }

    // Getter et Setter pour lastAccessOrder
    public long getLastAccessOrder() { return lastAccessOrder; }
    public void setLastAccessOrder(long order) { this.lastAccessOrder = order; }

    // pageId
    public PageId getPageId() { return pageId; }
    public void setPageId(PageId pageId) { this.pageId = pageId; }

    // pinCount
    public int getPinCount() { return pinCount; }
    public void setPinCount(int pinCount) { this.pinCount = pinCount; }

    // dirty
    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }

    // data
    public ByteBuffer getData() { return data; }
}
