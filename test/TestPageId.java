package test;
import src.PageId;

public class TestPageId {
    public static void main(String[] args) {
        testCreation();
        testModification();
        testEqualsEtHashCode();
        testToString();
    }

    private static void testCreation() {
        System.out.println("Test : Création d'une instance");
        PageId pid = new PageId(1, 42);
        System.out.println("fileIdx = " + pid.getFileIdx() + ", pageIdx = " + pid.getPageIdx());
    }

    private static void testModification() {
        System.out.println("\nTest : Modification des valeurs");
        PageId pid = new PageId(1, 42);
        pid.setFileIdx(2);
        pid.setPageIdx(99);
        System.out.println("Nouvelles valeurs : fileIdx = " + pid.getFileIdx() + ", pageIdx = " + pid.getPageIdx());
    }

    private static void testEqualsEtHashCode() {
        System.out.println("\nTest : Comparaison et hashCode");
        PageId pid1 = new PageId(1, 42);
        PageId pid2 = new PageId(1, 42);
        PageId pid3 = new PageId(2, 99);

        System.out.println("pid1.equals(pid2) ? " + pid1.equals(pid2)); // true
        System.out.println("pid1.equals(pid3) ? " + pid1.equals(pid3)); // false
        System.out.println("hashCode pid1 = " + pid1.hashCode() + ", hashCode pid2 = " + pid2.hashCode());
    }

    private static void testToString() {
        System.out.println("\nTest : Affichage toString()");
        PageId pid = new PageId(3, 7);
        System.out.println(pid.toString());
    }
}
