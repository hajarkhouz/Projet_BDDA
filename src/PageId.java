package src;
public class PageId {
    private int fileIdx;
    private int pageIdx;

    // Constructeur : on donne les deux numéros pour créer un PageId
    public PageId(int fileIdx, int pageIdx) {
        this.fileIdx = fileIdx;
        this.pageIdx = pageIdx;
    }

    // Méthodes pour lire les valeurs
    public int getFileIdx() {
        return fileIdx;
    }

    public int getPageIdx() {
        return pageIdx;
    }

    // Méthodes pour modifier les valeurs
    public void setFileIdx(int fileIdx) {
        this.fileIdx = fileIdx;
    }

    public void setPageIdx(int pageIdx) {
        this.pageIdx = pageIdx;
    }

    // Comparaison : est-ce que deux PageId sont identiques ?
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // si c'est le même objet
        if (o == null || getClass() != o.getClass()) return false; // si ce n'est pas un PageId
        PageId pageId = (PageId) o;
        return this.fileIdx == pageId.fileIdx && this.pageIdx == pageId.pageIdx;
    }

    // Code unique pour chaque PageId (utile pour les collections comme HashMap)
    @Override
    public int hashCode() {
        return 31 * fileIdx + pageIdx;
    }

    // Affichage dans la console
    @Override
    public String toString() {
        return "PageId { fileIdx=" + fileIdx + ", pageIdx=" + pageIdx + " }";
    }
}