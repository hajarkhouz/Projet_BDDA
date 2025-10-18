package test;
import src.DBConfig;

public class TestDBConfig {
    public static void main(String[] args) {
        testInstanceDirecte();
        testChargementDepuisFichierValide();
        testChargementDepuisFichierInvalide();
        testFichierInexistant();
    }

    private static void testInstanceDirecte() {
        System.out.println("Test : Création directe d'une instance (mise à jour TP3)");
        DBConfig config = new DBConfig("../BinData", 4096, 10, 10485760L, 100, "MRU");
        System.out.println(config);
    }

    private static void testChargementDepuisFichierValide() {
        System.out.println("\nTest : Chargement depuis un fichier valide (mise à jour TP3)");
        try {
            DBConfig config = DBConfig.LoadDBConfig("config/config_valide.txt");
            System.out.println(config);
            if (config.getBmPolicy().equals("LRU")) {
                System.out.println("Vérification bm_policy OK.");
            } else {
                System.err.println("ERREUR: La politique de remplacement n'est pas LRU.");
            }
        } catch (Exception e) {
            System.err.println("Échec du test : " + e.getMessage());
        }
    }

    private static void testChargementDepuisFichierInvalide() {
        System.out.println("\nTest : Fichier avec paramètres manquants (mise à jour TP3)");
        try {
            DBConfig config = DBConfig.LoadDBConfig("config/config_invalide.txt");
            System.out.println(config);
        } catch (Exception e) {
            System.out.println("Erreur attendue : " + e.getMessage());
        }
    }

    private static void testFichierInexistant() {
        System.out.println("\nTest : Fichier inexistant");
        try {
            DBConfig config = DBConfig.LoadDBConfig("config/fichier_inexistant.txt");
            System.out.println(config);
        } catch (Exception e) {
            System.out.println("Erreur attendue : " + e.getMessage());
        }
    }
}