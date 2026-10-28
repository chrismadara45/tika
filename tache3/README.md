# IFT3913 – Tâche 3 : Semgrep dans la CI, vulnérabilités et rickroll

Binôme : Chris Alex Belanov Ndabihawenimana (chrismadara45) et Dickson Ma (womboteemo)
Dépôt : https://github.com/chrismadara45/tika, branche `tache3`

| Fichier | Rôle |
|---|---|
| `.github/workflows/tache3-semgrep.yml` | Workflow qui échoue si un commit introduit une nouvelle vulnérabilité Semgrep |
| `.github/workflows/tache3-tests.yml` | Tests de `tika-core`, avec rickroll si un test échoue |
| `.github/actions/rickroll/action.yml` | Action réutilisable (composite) qui fait le rickroll |
| `tika-core/src/test/java/org/apache/tika/RickrollDemoTest.java` | Test qui échoue sur demande, pour valider le rickroll |
| `tache3/rapports/` | Rapports Semgrep avant / après et liste des nouvelles alertes |

---

## 1. Workflow GitHub Actions : bloquer les nouvelles vulnérabilités

### Fonctionnement

Le workflow `tache3 semgrep` se lance à chaque `push` sur `main` ou `tache3`, à chaque pull request, et à la main (`workflow_dispatch`). Il :

1. récupère tout l'historique Git (`fetch-depth: 0`) ;
2. installe Semgrep 1.182.1 et les règles Java officielles du dépôt `semgrep/semgrep-rules`, figées au commit `16b9e6d` ;
3. choisit un commit de référence (baseline) :
   - pull request : le point de divergence avec la branche cible (`git merge-base`) ;
   - push : le commit qui était au sommet de la branche avant le push (`github.event.before`) ;
   - nouvelle branche ou lancement manuel : le point de divergence avec `main` ;
4. produit un rapport complet (informatif, ne bloque jamais) ;
5. relance Semgrep avec `--baseline-commit <baseline> --error` : seules les alertes **absentes** de la baseline sont rapportées, et Semgrep renvoie le code 1 s'il y en a, ce qui fait échouer le job ;
6. écrit le tableau des nouvelles alertes dans le résumé du job, ajoute une annotation `::error` sur chaque ligne fautive et publie les rapports (JSON, SARIF, texte) en artefact.

### Choix de conception

**Mode diff (baseline) plutôt qu'un scan complet bloquant.** Tika contient déjà 84 alertes Semgrep dans son code de production (voir section 3). Un scan complet avec `--error` ferait échouer tous les builds, y compris ceux qui n'ajoutent rien. L'énoncé demande d'échouer quand un commit introduit une *nouvelle* vulnérabilité : le mode baseline répond exactement à ça. Il compare les alertes par leur contenu et non par leur numéro de ligne. Ainsi, quand du code existant se déplace, il n'est pas compté comme nouveau. Exemple réel : l'alerte `unencrypted-socket` de `NetworkParser` est passée de la ligne 68 à la ligne 75 après nos modifications, et Semgrep ne l'a pas rapportée comme nouvelle.

**Baseline `github.event.before` sur un push.** Si un push contient plusieurs commits, on les vérifie tous d'un coup contre l'état précédent de la branche : aucun commit vulnérable ne peut passer en étant « caché » au milieu d'un push. Pour une pull request, on compare au `merge-base` pour ne juger que les changements de la PR.

**Règles figées et locales plutôt que `--config p/java`.** Les packs du registre Semgrep changent sans prévenir : le même code pourrait passer un jour et échouer le lendemain. En fixant la version de Semgrep et le commit des règles, le résultat est reproductible, et nous avons pu obtenir exactement les mêmes rapports en local et dans la CI. Ce sont les mêmes règles que le pack Java du registre (dossier `java/` du dépôt officiel, 120 règles).

**Exclusion de `**/src/test/**`.** Les tests contiennent volontairement du code « dangereux » (fichiers malformés, réflexion, etc.) qui n'est jamais livré. On ne bloque que sur le code de production.

**Scan de tout le dépôt.** Le mode baseline ne réanalyse en détail que les fichiers modifiés, donc le contrôle bloquant reste rapide (environ 2 minutes) même sur tout Tika. Le rapport complet prend environ 4 minutes.

**Fichier séparé.** Le workflow est dans son propre fichier plutôt que dans `main-jdk21-build.yml`, qui ne se lance que sur `main` et fait un build complet d'une heure. Le contrôle de sécurité donne ainsi un retour rapide et indépendant du build. Pour en faire une vraie barrière sur `main`, il suffit de le déclarer comme *required status check* dans la protection de branche.

**Permissions minimales** (`contents: read`) : le workflow lit le code, il n'a besoin de rien d'autre.

### Validation

1. **Pas de faux positif sur la dette existante.** Le premier commit de la branche (`tache3: workflow Semgrep…`) n'ajoute que le workflow, l'action et un test. Le workflow passe (vert) alors que le dépôt contient 84 alertes : seules les nouveautés comptent.
2. **Détection des nouvelles vulnérabilités.** Le second commit introduit les 7 vulnérabilités de la section 2. Le workflow échoue (rouge) et liste exactement les 9 alertes de ces 7 vulnérabilités, rien de plus (pas l'alerte `unencrypted-socket` déplacée).
3. **Pas de double comptage.** Le troisième commit (documentation et rapports) passe au vert : il ne modifie aucun code Java, et les 7 vulnérabilités, déjà présentes dans sa baseline, ne sont pas signalées à nouveau.
4. **Reproduction locale.** Les mêmes commandes, lancées en local avec les mêmes versions, donnent le même résultat :

```bash
# commit 1 contre main : 0 nouvelle alerte, code de sortie 0
semgrep scan --config semgrep-rules/java --metrics=off --exclude '**/src/test/**' \
  --baseline-commit origin/main --error .
# commit 2 contre commit 1 : 9 nouvelles alertes, code de sortie 1
semgrep scan --config semgrep-rules/java --metrics=off --exclude '**/src/test/**' \
  --baseline-commit HEAD~1 --error .
```

Exécutions GitHub Actions : https://github.com/chrismadara45/tika/actions/workflows/tache3-semgrep.yml

---

## 2. Les 7 vulnérabilités introduites

Toutes sont dans `tika-core`, le module que nous avons étudié à la tâche 2. Chaque modification est marquée par un commentaire `tache3 (vuln N)`. Le code compile, respecte le checkstyle de Tika et ne change pas le comportement testé.

| # | Type (CWE) | Fichier | Règle Semgrep | Alertes |
|---|---|---|---|---|
| 1 | Hachage faible (CWE-328) | `digest/InputStreamDigester.java` | `use-of-md5` | 1 |
| 2 | Chiffrement faible, mode ECB (CWE-327) | `parser/CryptoParser.java` | `use-of-aes-ecb` | 1 |
| 3 | Validation de certificat absente (CWE-295) | `parser/NetworkParser.java` | `insecure-trust-manager` | 3 |
| 4 | Aléatoire non cryptographique (CWE-330) | `io/TemporaryResources.java` | `weak-random` | 1 |
| 5 | Permissions de fichier trop larges (CWE-276/732) | `io/StreamCache.java` | `overly-permissive-file-permission` | 1 |
| 6 | Désérialisation non fiable (CWE-502) | `detect/TrainedModelDetector.java` | `object-deserialization` | 1 |
| 7 | XXE, entités XML externes (CWE-611) | `mime/MimeTypesFactory.java` | `documentbuilderfactory-disallow-doctype-decl-missing` | 1 |

Les chemins sont relatifs à `tika-core/src/main/java/org/apache/tika/`.

### Vuln 1 : MD5 comme algorithme par défaut

```java
if (StringUtils.isBlank(algorithm)) {
    return MessageDigest.getInstance("MD5");
}
```

**Fichier.** `InputStreamDigester` calcule les empreintes des documents que Tika traite. Ces empreintes servent à identifier un fichier ou à repérer des doublons, donc l'intégrité en dépend.

**Pourquoi c'est vulnérable.** Si l'algorithme n'est pas configuré, au lieu d'échouer, le code se rabat en silence sur MD5. On sait fabriquer des collisions MD5 en quelques secondes : un attaquant peut produire deux documents différents avec la même empreinte et faire passer l'un pour l'autre.

**Semgrep.** `java.lang.security.audit.crypto.use-of-md5`, ligne 69, sévérité WARNING : MD5 n'est pas résistant aux collisions.

### Vuln 2 : AES en mode ECB

```java
if (transformation == null) {
    cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
}
```

**Fichier.** `CryptoParser` déchiffre les documents chiffrés avant de les analyser. C'est le seul endroit de `tika-core` qui choisit un mode de chiffrement.

**Pourquoi c'est vulnérable.** En ECB, chaque bloc de 16 octets est chiffré indépendamment : deux blocs identiques en clair donnent deux blocs identiques chiffrés. Les motifs du document restent visibles (l'exemple classique est le pingouin Linux chiffré) et on peut réordonner ou remplacer des blocs sans que ce soit détecté. Le défaut choisi ici est donc le pire possible.

**Semgrep.** `use-of-aes-ecb`, ligne 78, WARNING : ECB n'assure pas la confidentialité, utiliser un mode authentifié comme GCM.

### Vuln 3 : TrustManager qui accepte tous les certificats

```java
X509TrustManager trustAll = new X509TrustManager() {
    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
    public X509Certificate[] getAcceptedIssuers() { return null; }
};
```

Ce TrustManager est installé sur toute connexion `HttpsURLConnection` ouverte par `NetworkParser.parse`.

**Fichier.** `NetworkParser` envoie le document à un serveur distant pour qu'il l'analyse : c'est la partie réseau de `tika-core`, l'endroit naturel pour une faille TLS.

**Pourquoi c'est vulnérable.** `checkServerTrusted` ne vérifie rien : n'importe quel certificat, même auto-signé ou volé, est accepté. Quelqu'un placé sur le réseau (Wi-Fi public, proxy compromis) peut se faire passer pour le serveur (attaque de l'homme du milieu), lire le document envoyé et renvoyer un faux résultat d'analyse. HTTPS ne protège alors plus rien.

**Semgrep.** `insecure-trust-manager`, lignes 102, 104 et 106 : une alerte par méthode vide. C'est une seule vulnérabilité, signalée trois fois.

Nous avons choisi `SSLContext.getInstance("TLSv1.3")` et non `"TLS"`. Avec `"TLS"`, Semgrep levait aussi `weak-ssl-context`, ce qui aurait mélangé deux types de failles dans une même vulnérabilité.

### Vuln 4 : nom de fichier temporaire prévisible

```java
Path dir = tempFileDir == null ? Paths.get(System.getProperty("java.io.tmpdir")) : tempFileDir;
final Path path = Files.createFile(
        dir.resolve("apache-tika-" + new Random().nextLong() + actualSuffix));
```

**Fichier.** `TemporaryResources.createTempFile` crée tous les fichiers temporaires de Tika (copies des documents analysés, fichiers de débordement). Avant, il utilisait `Files.createTempFile`, qui tire le nom avec un générateur sûr et crée le fichier avec les droits `rw-------`.

**Pourquoi c'est vulnérable.** `java.util.Random` est un générateur linéaire congruentiel sur 48 bits : après avoir observé quelques sorties, on peut prédire les suivantes. Dans `/tmp`, partagé entre utilisateurs, un attaquant qui devine le prochain nom peut créer ce fichier (ou un lien symbolique) avant Tika, puis lire ou modifier les documents traités. En prime, `Files.createFile` applique les droits par défaut (souvent lisible par tous).

**Semgrep.** `weak-random`, ligne 94, WARNING : `java.util.Random` n'est pas un générateur cryptographiquement sûr.

### Vuln 5 : fichier de débordement accessible à tous

```java
Files.setPosixFilePermissions(spillFile, PosixFilePermissions.fromString("rw-rw-rw-"));
```

**Fichier.** `StreamCache` garde en mémoire le début d'un document, puis écrit le reste dans un fichier temporaire (*spill file*) quand il dépasse 1 Mo. Ce fichier contient donc le contenu brut des documents analysés.

**Pourquoi c'est vulnérable.** `rw-rw-rw-` donne à tous les utilisateurs de la machine le droit de lire et d'écrire ce fichier. Un autre utilisateur peut lire des documents confidentiels, ou modifier le fichier pendant que Tika le relit et lui faire analyser autre chose que l'original. Le `catch` garde le code portable sous Windows, où les permissions POSIX n'existent pas.

**Semgrep.** `overly-permissive-file-permission`, ligne 130, WARNING.

### Vuln 6 : désérialisation Java sans filtre

```java
try (ObjectInputStream ois = new ObjectInputStream(in)) {
    MODEL_MAP.putAll((Map<MediaType, TrainedModel>) ois.readObject());
}
```

**Fichier.** `TrainedModelDetector` charge des modèles de détection de type entraînés. Ajouter une méthode `loadSerializedModels` qui lit un modèle sérialisé est un ajout plausible pour un développeur.

**Pourquoi c'est vulnérable.** `readObject()` reconstruit n'importe quelle classe présente dans le classpath *avant* le transtypage. Avec un flux forgé (chaînes de *gadgets* comme celles de commons-collections, générées par l'outil ysoserial), la simple lecture peut exécuter du code arbitraire. Aucun filtre (`ObjectInputFilter`) ne limite les classes acceptées.

**Semgrep.** `object-deserialization`, ligne 170, WARNING (CWE-502).

### Vuln 7 : XXE dans le chargement des types MIME

```java
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
DocumentBuilder builder = dbf.newDocumentBuilder();
return create(builder.parse(xml));
```

**Fichier.** `MimeTypesFactory` construit le registre des types MIME à partir de fichiers XML, y compris des fichiers personnalisés fournis par l'utilisateur (`tika.custom-mimetypes`). Tika passe normalement par `XMLReaderUtils`, qui sécurise les parseurs ; la nouvelle méthode `createFromDom` le contourne.

**Pourquoi c'est vulnérable.** Une fabrique par défaut accepte les `DOCTYPE` et les entités externes. Un XML comme `<!DOCTYPE x [<!ENTITY e SYSTEM "file:///etc/passwd">]>` fait lire un fichier local (ou une URL interne, donc SSRF), et des entités imbriquées peuvent saturer la mémoire (*billion laughs*).

**Semgrep.** `documentbuilderfactory-disallow-doctype-decl-missing`, ligne 65, sévérité ERROR : la fonctionnalité `disallow-doctype-decl` n'est pas activée.

---

## 3. Comparaison des rapports Semgrep avant / après

Même configuration pour les deux scans : Semgrep 1.182.1, 120 règles Java, tout le dépôt sauf `src/test`.

| | Avant (`main`) | Après (`tache3`) |
|---|---|---|
| Alertes totales | 84 | 93 |
| Alertes nouvelles (mode baseline) | — | 9 (7 vulnérabilités) |

Répartition par règle (seules les lignes qui changent) :

| Règle | Avant | Après | Différence |
|---|---|---|---|
| `insecure-trust-manager` | 0 | 3 | +3 |
| `use-of-md5` | 0 | 1 | +1 |
| `weak-random` | 0 | 1 | +1 |
| `overly-permissive-file-permission` | 0 | 1 | +1 |
| `object-deserialization` | 0 | 1 | +1 |
| `use-of-aes-ecb` | 1 | 2 | +1 |
| `documentbuilderfactory-disallow-doctype-decl-missing` | 1 | 2 | +1 |

Les 14 autres règles (`unsafe-reflection` 17, `command-injection-process-builder` 16, `insecure-resteasy-deserialization` 16, `hardcoded-conditional` 11, etc.) ont exactement le même nombre d'alertes avant et après.

Différence entre les deux listes d'alertes (`diff semgrep-avant.txt semgrep-apres.txt`) :

```
> tika-core/.../detect/TrainedModelDetector.java:170   object-deserialization
> tika-core/.../digest/InputStreamDigester.java:69     use-of-md5
> tika-core/.../io/StreamCache.java:130                overly-permissive-file-permission
> tika-core/.../io/TemporaryResources.java:94          weak-random
> tika-core/.../mime/MimeTypesFactory.java:65          documentbuilderfactory-disallow-doctype-decl-missing
> tika-core/.../parser/CryptoParser.java:78            use-of-aes-ecb
> tika-core/.../parser/NetworkParser.java:102,104,106  insecure-trust-manager
< tika-core/.../parser/NetworkParser.java:68           unencrypted-socket
> tika-core/.../parser/NetworkParser.java:75           unencrypted-socket
```

Les deux dernières lignes montrent pourquoi un simple `diff` par numéro de ligne ne suffit pas : l'alerte `unencrypted-socket` existait déjà, elle a seulement descendu de 7 lignes à cause du code ajouté au-dessus. Le mode baseline de Semgrep la reconnaît comme ancienne. Il retourne bien 9 alertes et non 10 (fichier `semgrep-nouvelles.json`).

Deux types introduits (ECB, XXE DOM) existaient déjà ailleurs dans Tika, hors de `tika-core`. Nos alertes sont quand même nouvelles : elles sont à de nouveaux endroits, et le workflow les détecte comme telles.

Rapports complets : `tache3/rapports/semgrep-avant.json`, `semgrep-apres.json`, `semgrep-nouvelles.json` (et versions `.txt` lisibles). Ceux de la CI sont aussi dans l'artefact `semgrep-reports` de chaque exécution.

---

## 4. Humour : rickroll quand un test échoue

L'énoncé parle de GraphHopper, mais notre cas d'étude est Tika : le rickroll se déclenche donc quand un test de `tika-core` échoue.

**Implémentation.** Nous avons écrit notre propre action réutilisable, une action *composite* dans `.github/actions/rickroll/action.yml`. Nous n'avons pas repris `random-rickroll` parce qu'elle ne fait que commenter les nouvelles *issues* : elle ne sait pas réagir à un échec de test. Notre action :
- affiche une bannière ASCII « RICKROLLED » dans les logs ;
- crée une annotation d'erreur qui contient le lien vers la vidéo ;
- ajoute au résumé du job un GIF de Rick Astley cliquable, qui mène à la vidéo, et un faux lien « Voir le rapport d'erreur détaillé » qui y mène aussi.

Elle prend un paramètre `message`, donc n'importe quel workflow peut la réutiliser avec `uses: ./.github/actions/rickroll`.

Dans `tache3-tests.yml`, l'étape a la condition `if: failure()` : elle ne s'exécute que si une étape précédente (les tests Maven) a échoué.

**Validation.** Sans test cassé, le rickroll ne doit pas apparaître. Nous avons donc ajouté `RickrollDemoTest`, désactivé par défaut grâce à `@EnabledIfEnvironmentVariable` : il n'échoue que si `TIKA_RICKROLL_DEMO=true`. Le workflow a une option `force_failure` (onglet Actions → *tache3 tests* → *Run workflow*) qui définit cette variable.
- Exécution normale (push) : les tests passent, l'étape Rickroll est ignorée.
- Exécution avec `force_failure` coché : `RickrollDemoTest` échoue et le résumé du job affiche le rickroll.

Exécutions : https://github.com/chrismadara45/tika/actions/workflows/tache3-tests.yml
