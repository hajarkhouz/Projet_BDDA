# Projet_BDDA

MiniSGBDR – Projet de TP

Ce projet consiste en l'implémentation d'un Mini SGBDR (Système de Gestion de Bases de Données Relationnelles) simplifié.

## Objectif

L'objectif est de développer, étape par étape, un SGBD capable de :
- Gérer des commandes proches du SQL (création de tables, insertion, sélection, jointures, etc.)
- Fournir des commandes supplémentaires de type debug pour afficher des informations internes sur les données

## Caractéristiques du MiniSGBDR

- Mono-utilisateur
- Pas de gestion de la concurrence
- Pas de transactions
- Pas de droits d'accès
- Pas de crash recovery

## Pédagogie et progression

Le projet suit le rythme du cours :
- Implémentation des couches bas-niveau du SGBD
- Ajout progressif des fonctionnalités (création de tables, insertion, sélection, jointure, …)
- Simplification du langage de requêtes, tout en restant fidèle aux vraies commandes SQL

Le but final est de mettre en place un moteur relationnel minimaliste, permettant de mieux comprendre l'architecture et les principes fondamentaux d'un SGBD.

## Comment exécuter le projet

### Prérequis
- PowerShell installé sur votre système (Windows) ou PowerShell Core (Linux/Mac)
- Fichiers de données (comme S.csv) placés à la racine du dossier projet si nécessaire

### Étapes d'exécution

#### Sur Windows (PowerShell)
1. **Ouvrir PowerShell**
    - Appuyez sur `Win + X` puis sélectionnez "Windows PowerShell" ou "Terminal"
    - Ou recherchez "PowerShell" dans le menu Démarrer

2. **Naviguer jusqu'au dossier du projet**
```powershell
   cd chemin\vers\Projet_BDDA
```

3. **Exécuter le script**
```powershell
   .\all.ps1
```

#### Sur Linux/Mac (Terminal avec PowerShell Core)
1. **Ouvrir le Terminal**

2. **Naviguer jusqu'au dossier du projet**
```bash
   cd chemin/vers/Projet_BDDA
```

3. **Exécuter le script avec PowerShell**
```bash
   pwsh all.ps1
```
(Si PowerShell Core n'est pas installé, installez-le depuis : https://github.com/PowerShell/PowerShell)

### Utilisation du MiniSGBDR

Une fois le programme lancé, vous pouvez saisir vos commandes SQL simplifiées directement dans le terminal.



### Exemple de test rapide
```sql
CREATE TABLE Pomme (C1:INT,C2:VARCHAR(3),C3:INT)
INSERT INTO Pomme VALUES (1,"aab",2)
INSERT INTO Pomme VALUES (2,"ab",2)
INSERT INTO Pomme VALUES (1,"agh",1)
SELECT * FROM Pomme p
```

**Résultat attendu :**
```
1 ; aab ; 2
2 ; ab ; 2
1 ; agh ; 1
Total selected records = 3
```

## Remarques importantes

### Format des commandes
- **Toutes les commandes doivent être sur une seule ligne**
- Respectez scrupuleusement les espaces indiqués dans la syntaxe
- Les chaînes de caractères sont entourées de guillemets doubles (`"`)
- Il n'y a pas d'espace avant/après les virgules
- Il n'y a pas d'espace avant/après les opérateurs de comparaison (`=`, `<`, `>`, `<=`, `>=`, `<>`)

### Fichiers CSV
- Les fichiers CSV doivent être placés **à la racine du dossier projet**
- Ne pas inclure de chemin dans la commande (juste le nom du fichier)
- Format : une ligne par tuple, valeurs séparées par des virgules
- Les chaînes de caractères dans le CSV doivent être entre guillemets

## Contributions

Projet réalisé dans le cadre du cours de Bases de Données Avancées (BDDA).