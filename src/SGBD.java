package src;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SGBD {

    // Variables membres
    private DBConfig dbConfig;
    private DiskManager diskManager;
    private BufferManager bufferManager;
    private DBManager dbManager;

    /**
     * Constructeur de la classe SGBD.
     */
    public SGBD(DBConfig dbConfig) throws IOException {
        this.dbConfig = dbConfig;

        // Création des managers (Injection de Dépendances)
        this.diskManager = new DiskManager(dbConfig);
        this.bufferManager = new BufferManager(dbConfig, diskManager);
        this.dbManager = new DBManager(dbConfig, diskManager, bufferManager);

        this.dbManager.LoadState();

    }

    // =========================================================================
    // 1. Méthode Run()
    // =========================================================================

    public void Run() {
        Scanner scanner = new Scanner(System.in);
        String commandText;

        try {
            while (true) {
                if (!scanner.hasNextLine()) {
                    break;
                }
                commandText = scanner.nextLine().trim();

                if (commandText.isEmpty()) {
                    continue;
                }

                if ("EXIT".equalsIgnoreCase(commandText)) {
                    break;
                }

                try {
                    this.processCommand(commandText);
                } catch (IllegalArgumentException e) {
                    System.err.println("ERREUR de syntaxe ou de validation : " + e.getMessage());
                } catch (IOException e) {
                    System.err.println("ERREUR IO critique lors de l'exécution : " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("ERREUR inattendue : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } finally {
            this.shutdown();
            scanner.close();
        }
    }

    // =========================================================================
    // 2. Méthode processCommand (Dispatch)
    // =========================================================================

    private void processCommand(String commandText) throws Exception {
        String trimmedCommand = commandText.trim();

        // 1) Normalisation en MAJUSCULES pour détecter le type de commande
        String normalizedCommand = trimmedCommand.toUpperCase().replaceAll("\\s+", " ");

        if (normalizedCommand.isEmpty()) {
            return;
        }

        // Extraire le premier mot (SELECT, INSERT, DELETE, UPDATE...)
        String[] words = normalizedCommand.split(" ");
        String command = words[0];

        // =========================
        // COMMANDES DDL (SCHÉMA)
        // =========================

        if (normalizedCommand.startsWith("CREATE TABLE ")) {
            String args = trimmedCommand.substring("CREATE TABLE".length()).trim();
            this.ProcessCreateTableCommand(args);

        } else if (normalizedCommand.equals("DESCRIBE TABLES")) {
            this.dbManager.DescribeAllTables();

        } else if (normalizedCommand.startsWith("DESCRIBE TABLE ")) {
            String args = trimmedCommand.substring("DESCRIBE TABLE".length()).trim();
            if (!args.isEmpty()) {
                String tableName = args.split("\\s+")[0];
                this.dbManager.DescribeTable(tableName);
            } else {
                System.err.println("ERREUR: Commande DESCRIBE TABLE incomplète. Nom de table manquant.");
            }

        } else if (normalizedCommand.equals("DROP TABLES")) {
            this.dbManager.RemoveAllTables();

        } else if (normalizedCommand.startsWith("DROP TABLE ")) {
            String args = trimmedCommand.substring("DROP TABLE".length()).trim();
            if (!args.isEmpty()) {
                String tableName = args.split("\\s+")[0];
                this.ProcessDropTableCommand(tableName);
            } else {
                System.err.println("ERREUR: Commande DROP TABLE incomplète. Nom de table manquant.");
            }

            // =========================
            // COMMANDES DML (DONNÉES)
            // =========================

        } else if (normalizedCommand.startsWith("INSERT INTO ")) {
            String args = trimmedCommand.substring("INSERT".length()).trim();
            this.ProcessInsertCommand(args);

        } else if (normalizedCommand.startsWith("APPEND INTO ")) {
            // Si tu as déjà ProcessAppendCommand, mets-le ici.
            // Sinon laisse et on l’implémente après.
            String args = trimmedCommand.substring("APPEND".length()).trim();
            this.ProcessAppendCommand(args); // <-- si tu as cette méthode
            // sinon commente cette ligne et dis-moi, je te donne ProcessAppendCommand

        } else if (command.equals("SELECT")) {
            String args = trimmedCommand.substring("SELECT".length()).trim();
            this.ProcessSelectCommand(args);

        } else if (command.equals("DELETE")) {
            // TP7 A4
            String args = trimmedCommand.substring("DELETE".length()).trim();
            this.ProcessDeleteCommand(args);

        } else if (command.equals("UPDATE")) {
            // TP7 A5
            String args = trimmedCommand.substring("UPDATE".length()).trim();
            this.ProcessUpdateCommand(args);

            // =========================
            // COMMANDE DE CONTRÔLE
            // =========================

        } else if (command.equals("EXIT")) {
            // géré dans Run()

        } else {
            System.out.println("Commande inconnue : " + command);
        }
    }

    public void ProcessAppendCommand(String textCommand) throws Exception {
        // textCommand ressemble à : INTO NomRelation ALLRECORDS (S.csv)

        String cmd = textCommand.trim();

        int intoIdx = cmd.toUpperCase().indexOf("INTO");
        int allIdx = cmd.toUpperCase().indexOf("ALLRECORDS");

        if (intoIdx == -1 || allIdx == -1 || intoIdx > allIdx) {
            throw new IllegalArgumentException(
                    "Format APPEND invalide. Attendu: APPEND INTO T ALLRECORDS (F.csv)");
        }

        // 1) Extraire le nom de la relation
        String tableName = cmd.substring(intoIdx + 4, allIdx).trim();
        if (tableName.isEmpty()) {
            throw new IllegalArgumentException("Nom de table manquant dans APPEND.");
        }

        Relation relation = this.dbManager.GetTable(tableName);
        if (relation == null) {
            throw new IllegalArgumentException("Table inexistante : " + tableName);
        }

        // 2) Extraire le nom du fichier entre parenthèses
        int openParen = cmd.indexOf('(', allIdx);
        int closeParen = cmd.lastIndexOf(')');
        if (openParen == -1 || closeParen == -1 || closeParen < openParen) {
            throw new IllegalArgumentException("ALLRECORDS mal formé : parenthèses manquantes.");
        }

        String fileName = cmd.substring(openParen + 1, closeParen).trim();
        if (fileName.isEmpty()) {
            throw new IllegalArgumentException("Nom de fichier CSV manquant.");
        }

        // ⚠️ A2 : "on suppose que le fichier .csv se trouve à la racine de votre
        // dossier projet"
        // Donc : on ouvre juste fileName, sans chemin.
        java.io.File csv = new java.io.File(fileName);
        if (!csv.exists()) {
            throw new IllegalArgumentException("Fichier CSV introuvable à la racine du projet : " + fileName);
        }

        int inserted = 0;

        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csv))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                // A2 : une ligne = un record, valeurs séparées par virgule
                // + mêmes règles que A1 pour les chaînes ("...")
                List<String> rawValues = splitValuesRespectingQuotes(line);

                if (rawValues.size() != relation.getColumns().size()) {
                    // A2 dit que le fichier est compatible, mais si jamais pas le cas, on protège.
                    throw new IllegalArgumentException(
                            "Ligne CSV invalide (mauvais nombre de valeurs): " + line);
                }

                Record rec = new Record();
                for (int i = 0; i < rawValues.size(); i++) {
                    ColumnInfo col = relation.getColumns().get(i);
                    // On réutilise exactement la même validation/normalisation que INSERT (A2
                    // renvoie à A1)
                    validateAndAddValueToRecord(rec, col, rawValues.get(i));
                }

                relation.InsertRecord(rec);
                inserted++;
            }
        }

        // L'énoncé n'impose pas un affichage, donc simple message (optionnel)
        System.out.println("Total appended records=" + inserted);
    }

    public void ProcessUpdateCommand(String textCommand) throws Exception {
        // TP7 A5: UPDATE nomRelation aliasRel SET aliasRel.col=val,... [WHERE ...]
        String cmd = textCommand.trim();

        int setIdx = cmd.toUpperCase().indexOf(" SET ");
        if (setIdx == -1)
            throw new IllegalArgumentException("UPDATE invalide: SET manquant.");

        String beforeSet = cmd.substring(0, setIdx).trim();
        String afterSet = cmd.substring(setIdx + " SET ".length()).trim();

        String wherePart = null;
        int whereIdx = afterSet.toUpperCase().indexOf(" WHERE ");
        String setPart = (whereIdx == -1) ? afterSet : afterSet.substring(0, whereIdx).trim();
        if (whereIdx != -1)
            wherePart = afterSet.substring(whereIdx + " WHERE ".length()).trim();

        String[] t = beforeSet.split("\\s+");
        if (t.length != 2)
            throw new IllegalArgumentException("UPDATE invalide: attendu 'nomRelation alias'.");

        String tableName = t[0].trim();
        String alias = t[1].trim();

        Relation rel = this.dbManager.GetTable(tableName);
        if (rel == null)
            throw new IllegalArgumentException("Table inexistante : " + tableName);

        // Parse SET: alias.col=val,alias.col=val
        java.util.Map<Integer, String> updates = new java.util.HashMap<>();
        String[] assigns = setPart.split(",");
        for (String a : assigns) {
            String one = a.trim();
            int eq = one.indexOf('=');
            if (eq == -1)
                throw new IllegalArgumentException("SET invalide: " + one);

            String colRef = one.substring(0, eq).trim();
            String rawVal = one.substring(eq + 1).trim();

            int colIdx = parseColumnRefToIndex(colRef, alias, rel);
            String val = parseConstant(rawVal);

            updates.put(colIdx, val);
        }

        // Parse WHERE
        java.util.List<Condition> conditions = new java.util.ArrayList<>();
        if (wherePart != null && !wherePart.isEmpty()) {
            String[] conds = wherePart.split("\\s+AND\\s+");
            for (String c : conds)
                conditions.add(parseCondition(c.trim(), alias, rel));
        }

        int updated = 0;
        List<RecordId> all = new ArrayList<>(rel.getAllRecordIds());
        for (RecordId rid : all) {
            Record r = rel.readRecordById(rid);

            boolean ok = true;
            for (Condition c : conditions) {
                if (!c.eval(r, rel)) {
                    ok = false;
                    break;
                }
            }

            if (ok) {
                // appliquer modifications
                java.util.List<String> vals = new java.util.ArrayList<>(r.getValues());
                for (var e : updates.entrySet()) {
                    vals.set(e.getKey(), e.getValue());
                }
                Record newRec = new Record(vals);

                rel.overwriteRecord(rid, newRec);
                updated++;
            }
        }

        System.out.println("Total updated records = " + updated); // TP7 A5
    }

    // Suppression de ProcessExitCommand et ExitCommandException car la méthode
    // Run() le gère directement.

    // =========================================================================
    // 3. Méthodes ProcessXCommand
    // =========================================================================

    public void ProcessCreateTableCommand(String textCommand) throws IOException, IllegalArgumentException {
        // ... (Logique inchangée pour la création) ...
        String command = textCommand.trim();
        int openParen = command.indexOf('(');
        int closeParen = command.lastIndexOf(')');

        if (openParen == -1 || closeParen == -1 || openParen >= closeParen) {
            throw new IllegalArgumentException("Format CREATETABLE invalide. Format: NomTable (colonnes...)");
        }

        String tableName = command.substring(0, openParen).trim();
        String columnsString = command.substring(openParen, closeParen + 1);

        if (this.dbManager.GetTable(tableName) != null) {
            System.err.println("ERREUR: La table '" + tableName + "' existe déjà.");
            return;
        }

        List<ColumnInfo> columns = parseColumns(columnsString);

        PageId headerPageId = null;
        try {
            headerPageId = diskManager.AllocPage();
            Relation newRelation = new Relation(
                    tableName, columns, diskManager, bufferManager, headerPageId);
            newRelation.initHeaderPage();
            this.dbManager.AddTable(newRelation);

        } catch (IOException e) {
            if (headerPageId != null) {
                diskManager.DeallocPage(headerPageId);
            }
            throw new IOException("Erreur IO lors de la création de la table : " + e.getMessage(), e);
        }
    }

    /**
     * Implémentation de la commande INSERT : INTO NomTable VALUES (val1, val2,
     * ...).
     */

    public void ProcessInsertCommand(String textCommand) throws Exception {
        // Commande attendue :
        // INSERT INTO NomRelation VALUES (val1,val2,...,valn)

        String cmd = textCommand.trim();

        int intoIndex = cmd.toUpperCase().indexOf("INTO");
        int valuesIndex = cmd.toUpperCase().indexOf("VALUES");
        if (intoIndex == -1 || valuesIndex == -1 || intoIndex > valuesIndex) {
            throw new IllegalArgumentException("Format INSERT invalide. Attendu: INSERT INTO T VALUES (...)");
        }

        // nom table
        String tableName = cmd.substring(intoIndex + 4, valuesIndex).trim();
        if (tableName.isEmpty()) {
            throw new IllegalArgumentException("Nom de table manquant dans INSERT.");
        }

        Relation relation = this.dbManager.GetTable(tableName);
        if (relation == null) {
            throw new IllegalArgumentException("Table inexistante : " + tableName);
        }

        // valeurs entre (...)
        int openParen = cmd.indexOf('(', valuesIndex);
        int closeParen = cmd.lastIndexOf(')');
        if (openParen == -1 || closeParen == -1 || closeParen < openParen) {
            throw new IllegalArgumentException("VALUES mal formé : parenthèses manquantes.");
        }

        String inside = cmd.substring(openParen + 1, closeParen);

        // split en respectant les guillemets
        List<String> rawValues = splitValuesRespectingQuotes(inside);

        // TP7 dit pas d’espace autour des virgules, mais on tolère trim() sans
        // problème.
        if (rawValues.size() != relation.getColumns().size()) {
            throw new IllegalArgumentException(
                    "Nombre de valeurs (" + rawValues.size() + ") != nombre de colonnes ("
                            + relation.getColumns().size() + ")");
        }

        Record rec = new Record();
        for (int i = 0; i < rawValues.size(); i++) {
            ColumnInfo col = relation.getColumns().get(i);
            validateAndAddValueToRecord(rec, col, rawValues.get(i));
        }

        relation.InsertRecord(rec);
    }

    public void ProcessDropTableCommand(String textCommand) throws IOException, IllegalArgumentException {
        // ⚠️ CORRECTION pour la casse est intégrée ici par le passage de 'tableName' à
        // RemoveTable
        String trimmedCommand = textCommand.trim();
        String upperCaseCommand = trimmedCommand.toUpperCase();
        String tableName = trimmedCommand; // Nom de table dans la casse originale (e.g., Tab1)

        if (trimmedCommand.isEmpty()) {
            throw new IllegalArgumentException("DROP requiert un nom de table (TABLE [Nom]) ou le mot-clé TABLES.");
        }

        if ("TABLES".equals(upperCaseCommand)) {
            System.out.println("Suppression de TOUTES les tables...");
            this.dbManager.RemoveAllTables();
            return;
        }

        try {
            this.dbManager.RemoveTable(tableName); // Appel avec la casse originale
        } catch (IllegalArgumentException e) {
            System.err.println("ERREUR: " + e.getMessage());
        }
    }

    // ProcessDescribeCommand est inutile, le dispatch est fait dans processCommand
    // ProcessSelectCommand est conservé pour la complétude

    public void ProcessSelectCommand(String textCommand) throws Exception {
        // TP7 A3: SELECT ... FROM nomRelation aliasRel [WHERE ...]
        String cmd = textCommand.trim();

        int fromIdx = cmd.toUpperCase().indexOf(" FROM ");
        if (fromIdx == -1)
            throw new IllegalArgumentException("SELECT invalide: FROM manquant.");

        String selectPart = cmd.substring(0, fromIdx).trim(); // "*" ou "a.c1,a.c2"
        String afterFrom = cmd.substring(fromIdx + " FROM ".length()).trim();

        String fromPart;
        String wherePart = null;

        int whereIdx = afterFrom.toUpperCase().indexOf(" WHERE ");
        if (whereIdx == -1) {
            fromPart = afterFrom.trim();
        } else {
            fromPart = afterFrom.substring(0, whereIdx).trim();
            wherePart = afterFrom.substring(whereIdx + " WHERE ".length()).trim();
        }

        String[] ft = fromPart.split("\\s+");
        if (ft.length != 2)
            throw new IllegalArgumentException("FROM invalide: attendu 'nomRelation alias'.");

        String tableName = ft[0].trim();
        String alias = ft[1].trim();

        Relation rel = this.dbManager.GetTable(tableName);
        if (rel == null)
            throw new IllegalArgumentException("Table inexistante : " + tableName);

        // ---- projection: SELECT * ou SELECT alias.col,alias.col ----
        int[] keepCols = null; // null => *
        if (!selectPart.equals("*")) {
            String[] cols = selectPart.split(",");
            keepCols = new int[cols.length];
            for (int i = 0; i < cols.length; i++) {
                String token = cols[i].trim(); // ex: s.C3
                keepCols[i] = parseColumnRefToIndex(token, alias, rel);
            }
        }

        // ---- conditions WHERE ----
        java.util.List<Condition> conditions = new java.util.ArrayList<>();
        if (wherePart != null && !wherePart.isEmpty()) {
            String[] conds = wherePart.split("\\s+AND\\s+"); // TP7 A3: séparés par AND avec espaces
            for (String c : conds) {
                conditions.add(parseCondition(c.trim(), alias, rel));
            }
        }

        // ---- pipeline (TP7 B3) : scan -> select -> project -> print ----
        IRecordIterator scan = new RelationScanner(rel);
        IRecordIterator sel = new SelectOperator(scan, rel, conditions);
        IRecordIterator proj = new ProjectOperator(sel, keepCols);

        RecordPrinter printer = new RecordPrinter(proj);
        printer.printAll();

        proj.Close();
    }

    private int parseColumnRefToIndex(String colRef, String alias, Relation rel) {
        // colRef attendu: alias.colName (TP7 A3)
        int dot = colRef.indexOf('.');
        if (dot == -1)
            throw new IllegalArgumentException("Colonne invalide (attendu alias.col): " + colRef);

        String a = colRef.substring(0, dot).trim();
        String col = colRef.substring(dot + 1).trim();

        if (!a.equals(alias))
            throw new IllegalArgumentException("Alias invalide: " + a + " (attendu " + alias + ")");

        for (int i = 0; i < rel.getColumns().size(); i++) {
            if (rel.getColumns().get(i).getName().equalsIgnoreCase(col))
                return i;
        }
        throw new IllegalArgumentException("Colonne inconnue: " + col);
    }

    private Condition parseCondition(String c, String alias, Relation rel) {
        // TP7 A3: Terme1OPTerme2, OP parmi =,<,>,<=,>=,<>
        String[] ops = new String[] { "<=", ">=", "<>", "=", "<", ">" }; // ordre important
        String opFound = null;
        int pos = -1;

        for (String op : ops) {
            pos = c.indexOf(op);
            if (pos != -1) {
                opFound = op;
                break;
            }
        }
        if (opFound == null)
            throw new IllegalArgumentException("Condition invalide (opérateur manquant): " + c);

        String left = c.substring(0, pos).trim();
        String right = c.substring(pos + opFound.length()).trim();

        Condition.Op op = Condition.parseOp(opFound);

        boolean leftIsCol = left.contains(".");
        boolean rightIsCol = right.contains(".");

        // TP7 A3: au max un terme est une constante
        if (leftIsCol && rightIsCol) {
            int lidx = parseColumnRefToIndex(left, alias, rel);
            int ridx = parseColumnRefToIndex(right, alias, rel);
            return new Condition(lidx, op, ridx);
        }

        if (leftIsCol && !rightIsCol) {
            int lidx = parseColumnRefToIndex(left, alias, rel);
            String val = parseConstant(right);
            return new Condition(lidx, op, val);
        }

        if (!leftIsCol && rightIsCol) {
            // exemple: 8<=t.NoteCT (TP7 A3 exemples)
            int ridx = parseColumnRefToIndex(right, alias, rel);
            String val = parseConstant(left);

            // On inverse l’opérateur car constante OP colonne
            Condition.Op inv = switch (op) {
                case LT -> Condition.Op.GT;
                case GT -> Condition.Op.LT;
                case LE -> Condition.Op.GE;
                case GE -> Condition.Op.LE;
                default -> op; // EQ, NE inchangés
            };

            return new Condition(ridx, inv, val);
        }

        throw new IllegalArgumentException("Condition invalide (aucun terme colonne): " + c);
    }

    private String parseConstant(String raw) {
        raw = raw.trim();

        // valeurs chaîne entre guillemets (TP7 A1/A2/A3)
        if ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("ʺ") && raw.endsWith("ʺ"))) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw; // nombre ou autre
    }

    public void ProcessDeleteCommand(String textCommand) throws Exception {
        // TP7 A4: DELETE nomRelation aliasRel [WHERE ...]
        String cmd = textCommand.trim();

        String wherePart = null;
        int whereIdx = cmd.toUpperCase().indexOf(" WHERE ");
        String beforeWhere = (whereIdx == -1) ? cmd : cmd.substring(0, whereIdx).trim();
        if (whereIdx != -1)
            wherePart = cmd.substring(whereIdx + " WHERE ".length()).trim();

        String[] t = beforeWhere.split("\\s+");
        if (t.length != 2)
            throw new IllegalArgumentException("DELETE invalide: attendu 'nomRelation alias'.");

        String tableName = t[0].trim();
        String alias = t[1].trim();

        Relation rel = this.dbManager.GetTable(tableName);
        if (rel == null)
            throw new IllegalArgumentException("Table inexistante : " + tableName);

        java.util.List<Condition> conditions = new java.util.ArrayList<>();
        if (wherePart != null && !wherePart.isEmpty()) {
            String[] conds = wherePart.split("\\s+AND\\s+");
            for (String c : conds)
                conditions.add(parseCondition(c.trim(), alias, rel));
        }

        int deleted = 0;
        List<RecordId> all = new ArrayList<>(rel.getAllRecordIds());
        for (RecordId rid : all) {
            Record r = rel.readRecordById(rid);

            boolean ok = true;
            for (Condition c : conditions) {
                if (!c.eval(r, rel)) {
                    ok = false;
                    break;
                }
            }

            if (ok) {
                rel.DeleteRecord(rid);
                deleted++;
            }
        }

        System.out.println("Total deleted records = " + deleted); // TP7 A4
    }

    // =========================================================================
    // 4. Utilitaires et Shutdown (Gestion de EXIT)
    // =========================================================================

    private void shutdown() {
        try {
            this.dbManager.SaveState();
            this.bufferManager.flushAllPages();
            this.diskManager.finish();
            diskManager.closeAll();

        } catch (IOException e) {
            System.err.println("Erreur critique lors de l'arrêt du SGBD : " + e.getMessage());
        }
    }

    private List<ColumnInfo> parseColumns(String columnsString) throws IllegalArgumentException {
        // ... (Logique inchangée pour le parsing des colonnes) ...
        List<ColumnInfo> columns = new ArrayList<>();
        columnsString = columnsString.trim();
        if (columnsString.startsWith("("))
            columnsString = columnsString.substring(1);
        if (columnsString.endsWith(")"))
            columnsString = columnsString.substring(0, columnsString.length() - 1);

        String[] colDefinitions = columnsString.split(",");

        for (String def : colDefinitions) {
            def = def.trim();
            if (def.isEmpty())
                continue;

            String[] nameType = def.split(":", 2);
            if (nameType.length != 2)
                throw new IllegalArgumentException("Format de colonne invalide : " + def);

            String name = nameType[0].trim();
            String typeDef = nameType[1].trim();

            String type;
            int size = 0;

            if (typeDef.contains("(")) {
                int open = typeDef.indexOf('(');
                int close = typeDef.indexOf(')');
                if (close == -1 || close < open)
                    throw new IllegalArgumentException("Format invalide : " + typeDef);

                type = typeDef.substring(0, open).trim().toUpperCase();
                size = Integer.parseInt(typeDef.substring(open + 1, close).trim());

                if (!type.equals("VARCHAR")) {
                    throw new IllegalArgumentException("Type inconnu : " + type);
                }
            } else {
                type = typeDef.toUpperCase();
                if (type.equals("INT") || type.equals("FLOAT")) {
                    size = 4;
                } else {
                    throw new IllegalArgumentException("Type inconnu : " + type);
                }
            }

            columns.add(new ColumnInfo(name, type, size));
        }

        if (columns.isEmpty())
            throw new IllegalArgumentException("La table doit avoir au moins une colonne.");
        return columns;
    }

    private static String normalizeStringLiteral(String raw) {
        // raw doit être du style "abc"
        raw = raw.trim();
        if (raw.length() < 2 || raw.charAt(0) != '"' || raw.charAt(raw.length() - 1) != '"') {
            throw new IllegalArgumentException("Valeur VARCHAR mal formée (guillemets requis) : " + raw);
        }
        return raw.substring(1, raw.length() - 1); // sans guillemets
    }

    private static void validateAndAddValueToRecord(Record rec, ColumnInfo col, String rawValue) {
        String type = col.getType().toUpperCase();
        rawValue = rawValue.trim();

        switch (type) {
            case "INT": {
                // validation
                try {
                    Integer.parseInt(rawValue);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Valeur INT invalide : " + rawValue);
                }
                rec.addValue(rawValue);
                break;
            }
            case "FLOAT": {
                try {
                    Float.parseFloat(rawValue);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Valeur FLOAT invalide : " + rawValue);
                }
                rec.addValue(rawValue);
                break;
            }
            case "VARCHAR": {
                String s = normalizeStringLiteral(rawValue);

                // si ta ColumnInfo a une taille (col.getSize())
                int max = col.getSize();
                if (max > 0 && s.length() > max) {
                    throw new IllegalArgumentException(
                            "VARCHAR(" + max + ") trop long : " + s + " (len=" + s.length() + ")");
                }

                // on stocke sans guillemets dans Record
                rec.addValue(s);
                break;
            }
            default:
                throw new IllegalArgumentException("Type non supporté : " + type);
        }
    }

    // Suppression de l'exception de contrôle car elle n'est plus utilisée
    // =========================================================================
    // 5. Méthode main (Point d'entrée statique)
    // =========================================================================

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java SGBD <chemin/vers/config_file.txt>");
            return;
        }

        String configPath = args[0];
        DBConfig config = null;
        SGBD sgbd = null;

        try {
            config = DBConfig.LoadDBConfig(configPath);
            System.out.println("Configuration chargée : " + config);

            sgbd = new SGBD(config);

            sgbd.Run();

        } catch (IOException e) {
            System.err.println("Erreur IO critique ou de configuration : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur inattendue lors du démarrage : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<String> splitValuesRespectingQuotes(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
                cur.append(c); // on garde les guillemets pour validation ensuite
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

}