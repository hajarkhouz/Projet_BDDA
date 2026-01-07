package src;

public interface IRecordIterator {
    // retourne le record courant et avance, null si fini
    Record GetNextRecord() throws Exception;

    void Reset() throws Exception;

    void Close() throws Exception;
}
