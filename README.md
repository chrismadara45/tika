# IFT3913 – Tâche 2 : génération de tests avec ChatUniTest et analyse de mutation

**Binôme :** Chris Alex Belanov Ndabihawenimana ([chrismadara45](https://github.com/chrismadara45)) et Dickson Ma ([womboteemo](https://github.com/womboteemo))

**Cas d'étude :** Apache Tika, module `tika-core` (fork de [umontreal-diro/tika](https://github.com/umontreal-diro/tika)), branche [`tache2`](https://github.com/chrismadara45/tika/tree/tache2)

Le README original d'Apache Tika est disponible dans le dépôt d'origine.

## Résumé

| Étape | Mutants tués (sur 406) | Score de mutation | Force des tests | Couverture de lignes |
|---|---|---|---|---|
| Tests originaux | 165 | 41 % | 76 % | 307/452 (68 %) |
| + tests générés par ChatUniTest (corrigés) | 197 | 49 % | 81 % | 320/452 (71 %) |
| + tests écrits à la main | 294 | 72 % | 88 % | 392/452 (87 %) |

Les trois rapports PIT complets sont dans [`docs/pit-original`](docs/pit-original), [`docs/pit-final`](docs/pit-final) et [`docs/pit-manual`](docs/pit-manual).

## 1. Classes à tester

Nous avons choisi trois classes utilitaires de `tika-core` qui ont déjà une classe de test, mais dont la couverture est incomplète et qui laissent des mutants vivants.

| Classe | Tests existants | Couverture JaCoCo (instructions) | Lignes couvertes (PIT) | Mutants tués | Mutants survivants | Mutants non couverts |
|---|---|---|---|---|---|---|
| `org.apache.tika.io.EndianUtils` | `EndianUtilsTest` (4 tests) | 23 % | 31/121 | 38/206 (18 %) | 14 | 154 |
| `org.apache.tika.io.FilenameUtils` | `FilenameUtilsTest` (10 tests) | 93 %, mais 24 branches manquées | 154/175 | 79/115 (69 %) | 20 | 16 |
| `org.apache.tika.mime.MediaType` | `MediaTypeTest` (9 tests) | 79 % | 122/156 | 48/85 (56 %) | 19 | 18 |

Justification :

- **`EndianUtils`** est le cas le plus net : seules 4 méthodes de lecture sur une trentaine sont testées. Tous les accesseurs sur tableau (`getIntLE`, `getIntBE`, `getShortBE`, `getUByte`, etc.) et la moitié des lectures sur flux n'ont aucun test, d'où 154 mutants non couverts.
- **`FilenameUtils`** a une bonne couverture d'instructions, mais 24 branches manquées et 20 mutants couverts qui survivent. C'est un bon cas pour voir si des tests générés apportent de meilleurs oracles, et pas seulement de la couverture.
- **`MediaType`** est entre les deux : des méthodes simples jamais vérifiées (`getBaseType`, `hasParameters`, `equals`, `compareTo`, les fabriques `application`, `audio`, etc.) avec 19 survivants et 18 non couverts.

Méthodes visées par la génération : `EndianUtils#readIntLE`, `getIntLE`, `getShortBE`, `getUByte` ; `FilenameUtils` (classe entière) ; `MediaType#getBaseType`, `hasParameters`, `application`. Dans le rapport initial, toutes ces méthodes ont des mutants non couverts ou survivants, sauf `normalize` et `getName` de `FilenameUtils`, déjà bien testées, que la génération sur la classe entière a traitées en premier.

## 2. ChatUniTest dans le pipeline Maven

Le plugin est déclaré dans [`tika-core/pom.xml`](tika-core/pom.xml) :

```xml
<plugin>
  <groupId>io.github.zju-aces-ise</groupId>
  <artifactId>chatunitest-maven-plugin</artifactId>
  <version>2.1.1</version>
  <configuration>
    <apiKeys>xxx</apiKeys>
    <model>code-llama</model>
    <url>http://localhost:11434/v1/chat/completions</url>
    <testNumber>2</testNumber>
    <maxRounds>3</maxRounds>
    <thread>false</thread>
  </configuration>
</plugin>
```

Environnement utilisé :

- MacBook Pro Intel, 8 Go de RAM, macOS 12, exécution sur processeur seulement.
- Ollama 0.5.7 (dernière version encore compatible avec macOS 12), qui expose une API compatible OpenAI sur `localhost:11434`.
- Modèle ouvert local : `qwen2.5-coder:1.5b`. ChatUniTest n'accepte que des noms de modèles qu'il connaît, donc le modèle est enregistré sous l'alias `code-llama` (`ollama cp qwen2.5-coder:1.5b code-llama`).
- Java 21 et Maven 3.9.11.

Commandes de génération, depuis `tika-core` :

```bash
mvn chatunitest:class  -DselectClass=FilenameUtils
mvn chatunitest:method -DselectMethod=EndianUtils#getIntLE
```

Difficultés rencontrées pour faire fonctionner l'outil :

1. **Version de Java.** Avec Java 24, le plugin s'arrête sur `Invalid Java version format: 24`. Il fonctionne avec Java 21.
2. **Taille du modèle.** Avec `qwen2.5-coder:3b`, chaque requête dépassait le délai du plugin (`SocketTimeoutException`). Nous sommes passés à la version 1.5b.
3. **Dépendance `chatunitest-starter`.** La documentation du plugin demande d'ajouter cette dépendance. Elle amène JUnit 4 dans le classpath, et Surefire bascule alors sur le fournisseur JUnit 4 : plus aucun test JUnit 5 de Tika n'est exécuté (`Tests run: 0`). Nous l'avons utilisée pendant la génération, puis retirée du `pom.xml`. Il faut la remettre temporairement pour régénérer des tests.

ChatUniTest n'est pas exécuté dans la GitHub Action, puisqu'il a besoin d'Ollama en local. Seuls les tests générés et versionnés y sont exécutés.

## 3. Tests générés

### Où sont les tests

- Sortie brute de ChatUniTest, non modifiée : [`tika-core/chatunitest-tests/`](tika-core/chatunitest-tests) (9 fichiers, 24 méthodes de test).
- Historique complet des tentatives (prompts, réponses du modèle, erreurs de compilation) : [`docs/chatunitest-history/`](docs/chatunitest-history).
- Tests corrigés et intégrés à la suite : [`EndianUtilsGeneratedTest`](tika-core/src/test/java/org/apache/tika/io/EndianUtilsGeneratedTest.java), [`FilenameUtilsGeneratedTest`](tika-core/src/test/java/org/apache/tika/io/FilenameUtilsGeneratedTest.java) et [`MediaTypeGeneratedTest`](tika-core/src/test/java/org/apache/tika/mime/MediaTypeGeneratedTest.java) (15 tests). Chaque test porte un commentaire qui indique s'il est gardé tel quel ou ce qui a été corrigé.

### Résultat de la génération, méthode par méthode

| Méthode visée | Résultat de ChatUniTest | Tests générés | Passent tels quels |
|---|---|---|---|
| `EndianUtils#readIntLE` | échec : 6 tentatives, aucune ne compile | 0 | – |
| `EndianUtils#getIntLE` | succès à la 2e tentative | 1 | 1 |
| `EndianUtils#getShortBE` (2 surcharges) | succès | 2 | 2 |
| `EndianUtils#getUByte` | succès à la 2e tentative | 1 | 0 |
| `FilenameUtils#normalize` | succès | 1 | 0 |
| `FilenameUtils#getName` | succès à la 2e ronde (1re réponse inexploitable) | 14 | 3 |
| `FilenameUtils#getSuffixFromPath` | succès | 3 | 0 |
| `FilenameUtils#getSanitizedEmbeddedFileName` | échec : 6 tentatives, code non extractible ou délai dépassé | 0 | – |
| `FilenameUtils#getSanitizedEmbeddedFilePath` | échec : délai dépassé, génération interrompue | 0 | – |
| `MediaType#getBaseType` | succès | 1 | 1 |
| `MediaType#hasParameters` | succès | 1 | 0 |
| `MediaType#application` | échec : délai dépassé, génération interrompue | 0 | – |
| **Total** | 8 méthodes sur 12 | **24** | **7** |

### Les tests compilent-ils et s'exécutent-ils sans intervention ?

**Compilation.** Les 9 fichiers conservés par ChatUniTest compilent, mais c'est parce que l'outil ne garde que ce qui compile. Avant cela, il a fallu plusieurs rondes de réparation, et pour 4 méthodes sur 12 il n'a rien produit :

- Pour `readIntLE`, les 6 tentatives échouent sur la même erreur : le modèle écrit `BufferUnderrunException` au lieu de `EndianUtils.BufferUnderrunException` (classe interne). Il reçoit le message `cannot find symbol` à chaque ronde et ne le corrige jamais.
- Pour les méthodes longues de `FilenameUtils`, la réponse du modèle est tronquée ou mal formée (`extract code failed`), ou n'arrive pas dans le délai.

**Exécution.** Sur les 24 tests générés, seulement 7 passent sans intervention. Les 17 autres compilent mais échouent, tous à cause d'un oracle faux. ChatUniTest les compte pourtant comme réussis (`compile and execute successfully`), car il vérifie que le test s'exécute, pas qu'il passe.

**Intégration.** Aucun fichier brut ne pouvait être ajouté tel quel à `src/test/java` : le build de Tika impose un checkstyle (pas d'import `*`, pas d'import inutilisé, en-tête de licence) que tous les fichiers générés violent.

### Corrections nécessaires

| Type de correction | Nombre | Détail |
|---|---|---|
| Oracle faux (valeur attendue erronée) | 17 assertions | `getUByte` (1), `hasParameters` (1), `normalize` (1), `getName` (11), `getSuffixFromPath` (3) |
| Mauvaise utilisation de l'API | 2 tests | `new MediaType("text/plain", "plain")` au lieu de `new MediaType("text", "plain")` dans `getBaseType` et `hasParameters` |
| Méthode visée jamais appelée | 1 test | le test de `getBaseType` n'appelle pas `getBaseType()` ; il a été réécrit |
| Réflexion inutile | 1 test | `normalize` est publique, mais le test passe par `getDeclaredMethod` |
| Cas annoncé mais non testé | 1 test | le commentaire dit « null name » mais le test passe `""` ; ajout d'un vrai cas `null` |
| Doublons exacts supprimés | 3 tests | trois tests de `getName` reprennent une entrée déjà testée sous un autre nom |
| Imports et en-têtes (mécanique) | 9 fichiers | imports `*` et inutilisés (Mockito, etc.), en-tête de licence, regroupement en 3 classes |

Seuls 6 tests ont été gardés sans changement de logique : `getIntLE`, les deux `getShortBE` et les 3 tests de `getName` qui passaient (avec `StringUtils.EMPTY` remplacé par `""`).

### Critique des oracles, comparés aux tests écrits à la main

**Points positifs**

- Les oracles ne sont jamais triviaux : aucun `assertNotNull`, toutes les assertions comparent à une valeur précise.
- Pour les méthodes de calcul simples, le modèle choisit de bonnes données : `{0x12, 0x34, 0x56, 0x78}` pour `getIntLE` donne quatre octets distincts, donc toute erreur d'ordre ou de décalage est détectée. Ce seul test tue 15 mutants.
- Pour `getName`, le modèle pense aux cas `null` et chaîne vide, ce qui est pertinent.

**Points négatifs**

- **Les valeurs attendues sont devinées, pas dérivées du code.** Le modèle attend `0x1234` de `getUByte`, qui ne lit qu'un octet. Il attend `"txt"` de `getSuffixFromPath`, alors que la Javadoc dit explicitement que le point est inclus. Il attend `"%20"` pour un espace dans `normalize`, comme pour un encodage d'URL, alors que l'espace n'est pas dans la liste des caractères réservés. Il suppose que `getName` conserve les `:`, alors que le code les traite comme séparateurs de chemin (11 tests faux sur la même idée).
- **Un test qui passe n'est pas forcément un bon test.** Le test généré pour `getBaseType` passe, mais par accident : il construit `new MediaType("text/plain", "plain", ...)`, ce qui donne un objet incohérent dont `getType()` retourne `"text/plain"`, puis vérifie cette valeur. Il n'appelle jamais la méthode visée.
- **Redondance.** 14 tests pour `getName`, mais seulement 11 entrées distinctes et une seule idée répétée. Le test manuel existant `testGetName` couvre plus de comportements en 12 lignes (`..`, chemins Windows, barres obliques finales) et vérifie déjà correctement le cas des deux-points (`path:to:file.ppt` donne `file.ppt`).
- **Oracle circulaire.** Dans `getShortBE` avec décalage, la valeur attendue est recalculée par le test avec la même formule que le code (`testData[0] << 8 | testData[1]`). Ça passe et ça tue des mutants ici, mais ce style d'oracle reproduirait un bogue au lieu de le détecter.
- **Bruit.** Imports Mockito jamais utilisés, constantes privées de la classe recopiées dans le test sans servir, `throws IOException, TikaException` sur des méthodes qui ne lancent rien.

**Comparaison avec les tests écrits à la main.** Les tests originaux de Tika utilisent des exemples tirés du domaine (un exemple documenté de boutisme mixte pour `readIntME`, de vrais noms de fichiers piégés pour `FilenameUtils`) et vérifient les cas d'erreur (flux trop court). Les tests générés restent sur le cas nominal le plus simple et aucun ne teste une exception. À noter que les tests manuels ne sont pas parfaits non plus : dans `EndianUtilsTest.testReadUIntBE`, le cas « flux trop court » appelle `readUIntLE` par erreur de copier-coller, ce que l'analyse de mutation a révélé (voir section 5).

## 4. Analyse de mutation

PIT 1.30.0 (avec `pitest-junit5-plugin` 1.2.3) est configuré dans [`tika-core/pom.xml`](tika-core/pom.xml) sur les trois classes, avec les mutateurs par défaut. Commande, depuis `tika-core` :

```bash
mvn org.pitest:pitest-maven:mutationCoverage
```

### Scores par classe

Mutants tués (les mutants en délai dépassé sont comptés comme tués, comme le fait PIT).

| Classe | Mutants | Tests originaux | + tests générés | + tests manuels |
|---|---|---|---|---|
| `EndianUtils` | 206 | 38 (18 %) | 63 (31 %) | 141 (68 %) |
| `FilenameUtils` | 115 | 79 (69 %) | 79 (69 %) | 84 (73 %) |
| `MediaType` | 85 | 48 (56 %) | 55 (65 %) | 69 (81 %) |
| **Total** | **406** | **165 (41 %)** | **197 (49 %)** | **294 (72 %)** |

### Les tests générés détectent-ils tous les mutants ?

Non. Après ajout des tests générés, il reste 209 mutants non détectés : 47 couverts mais survivants et 162 non couverts. Les tests générés tuent 32 mutants de plus que les tests originaux.

### Mutants détectés par les tests générés, et pourquoi

| Test généré | Méthode mutée | Mutants tués | Pourquoi ils sont détectés |
|---|---|---|---|
| `testGetIntLE` | `getIntLE` | 15 | Les quatre octets d'entrée sont tous différents. Inverser un décalage (`<<` en `>>`), remplacer une addition par une soustraction, changer un `i++` en `i--` ou un `& 0xFF` en `\| 0xFF` change au moins un octet du résultat `0x78563412`. Le retour remplacé par `0` est détecté pour la même raison. |
| `testGetShortBE`, `testGetShortBEWithOffset` | `getShortBE`, `getUShortBE` | 8 | Même principe sur deux octets distincts (`0x12`, `0x34`) : toute mutation du décalage, de l'addition ou du masque donne une valeur différente de `0x1234`. |
| `testGetUByte` (oracle corrigé) | `getUByte` | 2 | `0x12 \| 0xFF` vaut `0xFF` et non `0x12` ; le retour remplacé par `0` diffère aussi. Avec l'oracle généré (`0x1234`), le test échouait sur le code original et n'aurait donc rien prouvé. |
| `testGetBaseType` (réécrit) | `getBaseType`, `getType`, `getSubtype` | 5 | Le test appelle `getBaseType()` sur un type avec paramètre et vérifie type, sous-type et absence de paramètres. La condition inversée renvoie l'objet avec ses paramètres ; les retours `null` ou `""` et l'erreur d'indice dans `getSubtype` sont détectés par les assertions sur les chaînes. |
| `testHasParameters` (oracle corrigé) | `hasParameters` | 2 | Le test vérifie les deux côtés (`false` sans paramètre, `true` avec). Le mutant « retourne toujours `true` » échoue sur le premier cas, la condition inversée sur les deux. Le test généré attendait `true` deux fois et ne pouvait donc pas distinguer ces mutants. |
| Tests de `FilenameUtilsGeneratedTest` | `normalize`, `getName`, `getSuffixFromPath` | 0 | Ces trois méthodes étaient déjà testées. Les 9 tests générés ne tuent aucun mutant que `FilenameUtilsTest` ne tuait pas déjà. |

Ce tableau montre deux choses. D'abord, les 25 mutants gagnés sur `EndianUtils` viennent de méthodes qui n'avaient aucun test : le gain vient de la couverture, pas de la finesse des oracles. Ensuite, 9 des 32 mutants tués le sont par des tests dont nous avons corrigé l'oracle ou la logique. Les versions brutes de `getUByte` et `hasParameters` échouaient sur le code original, donc PIT n'aurait pas pu les utiliser.

## 5. Tests supplémentaires écrits à la main

Les tests sont dans [`EndianUtilsManualTest`](tika-core/src/test/java/org/apache/tika/io/EndianUtilsManualTest.java), [`MediaTypeManualTest`](tika-core/src/test/java/org/apache/tika/mime/MediaTypeManualTest.java) et [`FilenameUtilsManualTest`](tika-core/src/test/java/org/apache/tika/io/FilenameUtilsManualTest.java). Ils tuent 97 mutants de plus (197 à 294). Pour chaque test : l'intention, la motivation des données et l'explication de l'oracle.

### `EndianUtilsManualTest`

**`testReadZeroIsNotUnderrun`** (7 mutants)
- *Intention :* un flux qui contient des octets nuls est une donnée valide, pas une fin de flux.
- *Données :* quatre (ou deux) octets `0x00`. C'est la seule entrée qui distingue `< 0` de `<= 0` dans le test `(ch1 | ch2 | ch3 | ch4) < 0`, donc celle qui tue les mutants de frontière.
- *Oracle :* la valeur lue est `0`, puisque tous les octets valent zéro, quel que soit le boutisme. Aucune exception ne doit être lancée.

**`testReadUIntBEUnderrun`** (1 mutant)
- *Intention :* `readUIntBE` signale un flux trop court.
- *Données :* un flux de 3 octets alors que la méthode en lit 4. Le test original `testReadUIntBE` voulait vérifier ce cas, mais appelle `readUIntLE` par erreur ; le cas n'était donc jamais testé.
- *Oracle :* le quatrième `read()` retourne `-1`, donc la Javadoc impose `BufferUnderrunException`.

**`testReadIntLEAndBE`** (18 mutants)
- *Intention :* `readIntLE` et `readIntBE` assemblent les octets dans le bon ordre et signalent un flux trop court.
- *Données :* `12 34 56 78`, quatre octets distincts pour détecter toute permutation ; `FF FE FD FC`, octets avec le bit de poids fort à 1 pour vérifier le résultat négatif ; puis un flux de 3 octets.
- *Oracle :* par définition du boutisme. En petit-boutiste, le premier octet lu est le poids faible : `0x78563412`. En gros-boutiste : `0x12345678`.

**`testReadUShortAndShort`** (12 mutants)
- *Intention :* les lectures sur 16 bits, signées et non signées.
- *Données :* `34 12` pour l'ordre des octets ; `FE FF`, qui vaut 65534 en non signé et -2 en signé, pour vérifier la conversion ; un flux d'un seul octet pour l'erreur.
- *Oracle :* `0x1234` en petit-boutiste et `0x3412` en gros-boutiste ; `0xFFFE` interprété en complément à deux sur 16 bits vaut -2.

**`testGetIntBE`** (15 mutants)
- *Intention :* lecture d'un entier gros-boutiste dans un tableau, avec et sans décalage.
- *Données :* `12 34 56 78` à la position 0, puis `FF FE FD FC` à la position 1 d'un tableau qui commence par `00`. Le décalage non nul vérifie que l'indice de départ est respecté, les octets hauts vérifient le masque `& 0xFF`.
- *Oracle :* les octets dans l'ordre de lecture : `0x12345678` et `0xFFFEFDFC`.

**`testGetUShortLEAndShortLE`** (9 mutants)
- *Intention :* lecture 16 bits petit-boutiste dans un tableau, signée et non signée.
- *Données :* `34 12`, et `FE FF` à la position 1.
- *Oracle :* `0x1234` ; `0xFFFE` en non signé et -2 en signé.

**`testGetUIntLEAndBE`** (6 mutants)
- *Intention :* les entiers non signés sur 32 bits ne deviennent pas négatifs.
- *Données :* `01 02 03 84`, dont l'octet de poids fort en petit-boutiste a le bit de signe à 1 ; puis `FF FF FF FF`, la valeur maximale.
- *Oracle :* le résultat est un `long` positif : `0x84030201L` (et non la valeur négative de l'`int`), `0x01020384L` en gros-boutiste, et `0xFFFFFFFFL` soit 4 294 967 295.

**`testGetLongLE`** (8 mutants)
- *Intention :* lecture d'un entier 64 bits petit-boutiste.
- *Données :* 9 octets tous différents, lus à la position 1 puis 0, pour vérifier à la fois l'ordre des 8 octets et les bornes de la boucle.
- *Oracle :* les 8 octets lus, du dernier au premier : `0x8807060504030201L` à la position 1.

**`testUbyteToInt`** (2 mutants)
- *Intention :* un octet est converti en entier non signé.
- *Données :* les valeurs aux frontières du signe : `0xFF`, `0x80`, `0x7F`, `0x00`.
- *Oracle :* la valeur non signée de l'octet : 255, 128, 127, 0.

### `MediaTypeManualTest`

**`testEquals`** (4 mutants)
- *Intention :* deux types de même nom sont égaux même s'ils sont des objets différents ; un type n'est égal ni à un autre type, ni à un objet d'une autre classe, ni à `null`.
- *Données :* un `MediaType` construit et un `MediaType` analysé pour le même nom `text/plain` (deux instances distinctes), `text/html`, la chaîne `"text/plain"` et `null`.
- *Oracle :* le contrat de `Object.equals`. Les mutants « retourne toujours `true` » et « retourne toujours `false` » échouent chacun sur un des cas.

**`testCompareTo`** (1 mutant)
- *Intention :* l'ordre entre types suit l'ordre de leurs noms.
- *Données :* `text/html` et `text/plain`, qui diffèrent seulement après le préfixe commun.
- *Oracle :* ordre lexicographique : `h` précède `p`, donc le résultat est négatif dans un sens, positif dans l'autre, et nul pour deux types égaux. Le mutant « retourne `0` » échoue.

**`testConvenienceFactories`** (5 mutants)
- *Intention :* les fabriques `application`, `audio`, `image`, `text` et `video` ajoutent le bon préfixe.
- *Données :* un sous-type courant par fabrique (`xml`, `mpeg`, `png`, `csv`, `mp4`).
- *Oracle :* le nom de la fabrique : le résultat est le type `préfixe/sous-type`. Aucune de ces fabriques n'avait de test.

**`testSetOfTypesIgnoresNull`** (2 mutants)
- *Intention :* `set(MediaType...)` ignore les `null` et retourne un ensemble non modifiable.
- *Données :* deux types valides séparés par un `null`.
- *Oracle :* la Javadoc (« unmodifiable set ») : taille 2, les deux types présents, et `add` lance `UnsupportedOperationException`.

**`testSetOfStringsIgnoresUnparseable`** (2 mutants)
- *Intention :* `set(String...)` ignore les chaînes qui ne sont pas des types valides.
- *Données :* deux types valides et `"nonsense"`, qui n'a pas de `/`.
- *Oracle :* `parse` retourne `null` pour une chaîne sans `/`, donc l'ensemble contient 2 éléments.

### `FilenameUtilsManualTest`

`getSanitizedEmbeddedFileName` cherche le nom d'un document imbriqué dans plusieurs métadonnées, dans un ordre de priorité. Les tests originaux ne renseignent que la première, donc l'ordre n'était jamais vérifié. ChatUniTest n'a rien produit pour cette méthode.

**`testEmbeddedNamePrefersResourceName`** (1 mutant)
- *Intention :* quand plusieurs métadonnées sont présentes, le nom de ressource est prioritaire.
- *Données :* `RESOURCE_NAME_KEY = first.txt` et `INTERNAL_PATH = second.txt`, deux noms différents pour savoir lequel a été retenu.
- *Oracle :* l'ordre de priorité écrit dans le code : `first.txt`.

**`testEmbeddedNameFallsBackToInternalPath`** (1 mutant)
- *Intention :* sans nom de ressource, le chemin interne est utilisé, et seul le nom de fichier est gardé.
- *Données :* `INTERNAL_PATH = dir/second.txt` et un identifiant de relation `third.png` de priorité inférieure.
- *Oracle :* le chemin interne passe avant l'identifiant, et le répertoire est retiré : `second.txt`.

**`testEmbeddedNameFallsBackToRelationshipId`** (3 mutants)
- *Intention :* troisième niveau de repli.
- *Données :* seulement `EMBEDDED_RELATIONSHIP_ID = third.png`.
- *Oracle :* c'est la seule source de nom disponible : `third.png`.

**`testEmbeddedNameWithoutAnyNameIsNull`** (0 mutant nouveau)
- *Intention :* sans aucune métadonnée de nom, la méthode ne fabrique pas de nom.
- *Données :* un objet `Metadata` vide.
- *Oracle :* le code retourne `null` quand le chemin est vide. Ce test documente le comportement mais ne tue pas de mutant de plus.

### Mutants encore vivants

Il reste 112 mutants non détectés (42 survivants, 70 non couverts). Les principaux :

- **`readLongLE` et `readLongBE` (48 non couverts).** Nous ne les avons pas testées, faute de temps. Des tests sur le modèle de `testReadIntLEAndBE` les tueraient.
- **Mutants équivalents.** Dans `getIntLE` et `getIntBE`, le dernier `i++` peut devenir `i--` sans effet, car `i` n'est plus lu ensuite. Aucun test ne peut les tuer.
- **Mutants équivalents en pratique.** Dans les tests de fin de flux `(ch1 | ch2 | ch3 | ch4) < 0`, remplacer l'un des deux premiers `|` par `&` ne change rien avec un vrai flux : une fois la fin atteinte, toutes les lectures suivantes retournent `-1`, donc le dernier `|` suffit. Seul un flux artificiel qui retournerait `-1` puis une valeur les distinguerait (10 mutants).
- **Méthodes privées de `MediaType`** (`isSimpleName`, `union`) et les branches de repli de `getEmbeddedPath` dans `FilenameUtils`, atteignables seulement à travers des entrées très particulières.
- **`getSuffixFromPath`, ligne 134.** Le mutant change `n.length() - i < 6` en `<= 6`. Le tuer demande une extension de 5 caractères, comme `a.abcde`. Le code retourne alors une chaîne vide, alors que la Javadoc annonce qu'une extension de 5 caractères ou moins est acceptée. Nous n'avons pas écrit de test qui fige ce comportement, car il semble contredire la documentation.

## 6. Exécution

Tous les nouveaux tests passent en local (33 tests : 15 générés et 18 manuels) et le checkstyle de Tika ne signale aucune violation.

```bash
cd tika-core
mvn test -Dtest='*ManualTest,*GeneratedTest' -Dsurefire.failIfNoSpecifiedTests=false
```

Les workflows d'origine de Tika ne se déclenchent que sur la branche `main`. Nous avons ajouté [`.github/workflows/tache2.yml`](.github/workflows/tache2.yml), qui exécute sur la branche `tache2` les tests originaux, générés et manuels des trois classes. Résultats : [onglet Actions](https://github.com/chrismadara45/tika/actions/workflows/tache2.yml).
