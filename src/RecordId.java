package src;

public class RecordId {

    private PageId pageId; // Page où se trouve le record
    private int slotIdx; // Index du slot dans la page

    public RecordId(PageId pageId, int slotIdx) {
        this.pageId = pageId;
        this.slotIdx = slotIdx;
    }

    public PageId getPageId() {
        return pageId;
    }

    public int getSlotIdx() {
        return slotIdx;
    }

    public void setPageId(PageId pageId) {
        this.pageId = pageId;
    }

    public void setSlotIdx(int slotIdx) {
        this.slotIdx = slotIdx;
    }

    @Override
    public String toString() {
        return "RecordId{pageId=" + pageId + ", slotIdx=" + slotIdx + "}";
    }
}
