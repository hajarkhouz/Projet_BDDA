package src;

import java.util.ArrayList;
import java.util.List;

public class Record {

    private List<String> valeurs;

    public Record() {
        this.valeurs = new ArrayList<>();
    }

    public Record(List<String> values) {
        this.valeurs = values;
    }

    public List<String> getValues() {
        return valeurs;
    }

    public void addValue(String v) {
        valeurs.add(v);
    }

    public void clear() {
        valeurs.clear();
    }

    @Override
    public String toString() {
        return valeurs.toString();
    }
}
