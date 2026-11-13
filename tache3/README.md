# IFT3913, Tâche 3 : Semgrep dans la CI, vulnérabilités et rickroll

Binôme : Chris Alex Belanov Ndabihawenimana (chrismadara45) et Dickson Ma (womboteemo)

Dépôt : https://github.com/chrismadara45/tika, branche `tache3`

Voici les fichiers qu'on a ajoutés ou modifiés pour cette tâche :

| Fichier | Rôle |
|---|---|
| `.github/workflows/tache3-semgrep.yml` | Workflow qui fait échouer le build si un commit ajoute une nouvelle vulnérabilité Semgrep |
| `.github/workflows/tache3-tests.yml` | Lance les tests de `tika-core` et déclenche le rickroll si un test échoue |
| `.github/actions/rickroll/action.yml` | Notre action réutilisable (composite) qui fait le rickroll |
| `tika-core/src/test/java/org/apache/tika/RickrollDemoTest.java` | Test qu'on peut faire échouer exprès pour vérifier le rickroll |
| `tache3/rapports/` | Rapports Semgrep avant et après, et la liste des nouvelles alertes |

---

## 1. Le workflow GitHub Actions

### Comment il fonctionne

Le workflow `tache3 semgrep` se lance à chaque `push` sur `main` ou `tache3`, à chaque pull request, et aussi à la main avec `workflow_dispatch`. Voici ce qu'il fait, dans l'ordre :

1. Il récupère tout l'historique Git (`fetch-depth: 0`), parce que Semgrep en a besoin pour comparer deux commits.
2. Il installe Semgrep 1.182.1 et les règles Java officielles du dépôt `semgrep/semgrep-rules`, figées au commit `16b9e6d`.
3. Il choisit un commit de référence (la baseline) :
   - pour une pull request, le point où la PR s'est séparée de la branche cible (`git merge-base`) ;
   - pour un push, le commit qui était au bout de la branche juste avant le push (`github.event.before`) ;
   - pour une nouvelle branche ou un lancement manuel, le point de séparation avec `main`.
4. Il produit un rapport complet. Ce rapport sert seulement d'information et ne bloque jamais.
5. Il relance Semgrep avec `--baseline-commit <baseline> --error`. Semgrep ne garde alors que les alertes qui n'existaient pas dans la baseline, et s'il en trouve au moins une, il renvoie le code 1 et le job échoue.
6. Il affiche le tableau des nouvelles alertes dans le résumé du job, met une annotation `::error` sur chaque ligne en cause et publie les rapports (JSON, SARIF et texte) en artefact.

### Nos choix

**On compare à une baseline au lieu de bloquer sur tout le scan.** Tika a déjà 84 alertes Semgrep dans son code de production (voir la section 3). Si on bloquait sur le scan complet avec `--error`, tous les builds échoueraient, même ceux qui n'ajoutent rien. Or l'énoncé demande d'échouer seulement quand un commit introduit une *nouvelle* vulnérabilité, et c'est exactement ce que fait le mode baseline. En plus, il compare les alertes selon leur contenu et pas selon leur numéro de ligne : du code qui a juste été déplacé n'est pas compté comme nouveau. On l'a vu en vrai avec l'alerte `unencrypted-socket` de `NetworkParser`, qui est passée de la ligne 68 à la ligne 75 après nos changements. Semgrep ne l'a pas signalée comme nouvelle.

**Sur un push, la baseline est `github.event.before`.** Quand un push contient plusieurs commits, on les vérifie tous ensemble par rapport à l'état de la branche avant le push. Un commit vulnérable ne peut donc pas passer inaperçu au milieu des autres. Pour une pull request, on compare au `merge-base` afin de juger uniquement ce que la PR change.

**Des règles figées en local plutôt que `--config p/java`.** Les packs du registre Semgrep peuvent changer à tout moment, donc le même code pourrait passer un jour et échouer le lendemain. En fixant la version de Semgrep et le commit des règles, on obtient toujours le même résultat, et on a d'ailleurs eu exactement les mêmes rapports en local et dans la CI. Ces règles sont les mêmes que celles du pack Java du registre (le dossier `java/` du dépôt officiel, soit 120 règles).

**On exclut `**/src/test/**`.** Les tests contiennent du code « dangereux » exprès (fichiers malformés, réflexion, etc.), mais ce code n'est jamais livré. On bloque donc seulement sur le code de production.

**On scanne tout le dépôt.** En mode baseline, Semgrep ne réanalyse en détail que les fichiers modifiés. Le contrôle bloquant reste donc rapide, environ 2 minutes sur tout Tika. Le rapport complet prend environ 4 minutes.

**Un fichier de workflow à part.** On n'a pas mis le contrôle dans `main-jdk21-build.yml`, parce que ce workflow ne tourne que sur `main` et fait un build complet d'une heure. Avec un fichier séparé, on a un retour rapide sur la sécurité, indépendant du build. Pour en faire une vraie barrière sur `main`, il suffirait de l'ajouter comme *required status check* dans la protection de branche.

**Des permissions minimales** (`contents: read`). Le workflow lit le code et n'a besoin de rien d'autre.

### Comment on l'a validé

On a poussé la branche en trois commits séparés pour voir le workflow passer du vert au rouge, puis revenir au vert.

1. **Pas de faux positif sur les alertes existantes.** Le premier commit (`tache3: workflow Semgrep…`) ajoute seulement le workflow, l'action et un test. Le workflow est vert alors que le dépôt contient déjà 84 alertes, ce qui montre que seules les nouvelles comptent.
2. **Les nouvelles vulnérabilités sont détectées.** Le deuxième commit ajoute les 7 vulnérabilités de la section 2. Le workflow devient rouge et liste exactement les 9 alertes qui correspondent à ces 7 vulnérabilités, sans rien de plus (l'alerte `unencrypted-socket` déplacée n'y est pas).
3. **Pas de double comptage.** Le troisième commit (documentation et rapports) est vert. Il ne touche à aucun fichier Java, et les 7 vulnérabilités, qui sont déjà dans sa baseline, ne sont pas signalées une deuxième fois.
4. **Même résultat en local.** On a lancé les mêmes commandes sur nos machines avec les mêmes versions et obtenu le même résultat :

```bash
# commit 1 contre main : 0 nouvelle alerte, code de sortie 0
semgrep scan --config semgrep-rules/java --metrics=off --exclude '**/src/test/**' \
  --baseline-commit origin/main --error .
# commit 2 contre commit 1 : 9 nouvelles alertes, code de sortie 1
semgrep scan --config semgrep-rules/java --metrics=off --exclude '**/src/test/**' \
  --baseline-commit HEAD~1 --error .
```

Les exécutions sont visibles ici : https://github.com/chrismadara45/tika/actions/workflows/tache3-semgrep.yml

---

## 2. Les 7 vulnérabilités

On les a toutes mises dans `tika-core`, le module qu'on avait déjà étudié à la tâche 2. Chaque changement est marqué par un commentaire `tache3 (vuln N)`. Le code compile, respecte le checkstyle de Tika et ne change pas le comportement couvert par les tests.

| # | Type (CWE) | Fichier | Règle Semgrep | Alertes |
|---|---|---|---|---|
| 1 | Hachage faible (CWE-328) | `digest/InputStreamDigester.java` | `use-of-md5` | 1 |
| 2 | Chiffrement faible, mode ECB (CWE-327) | `parser/CryptoParser.java` | `use-of-aes-ecb` | 1 |
| 3 | Certificat non vérifié (CWE-295) | `parser/NetworkParser.java` | `insecure-trust-manager` | 3 |
| 4 | Aléatoire non cryptographique (CWE-330) | `io/TemporaryResources.java` | `weak-random` | 1 |
| 5 | Permissions de fichier trop larges (CWE-276/732) | `io/StreamCache.java` | `overly-permissive-file-permission` | 1 |
| 6 | Désérialisation non fiable (CWE-502) | `detect/TrainedModelDetector.java` | `object-deserialization` | 1 |
| 7 | XXE, entités XML externes (CWE-611) | `mime/MimeTypesFactory.java` | `documentbuilderfactory-disallow-doctype-decl-missing` | 1 |

Les chemins partent de `tika-core/src/main/java/org/apache/tika/`.

### Vuln 1 : MD5 par défaut

```java
if (StringUtils.isBlank(algorithm)) {
    return MessageDigest.getInstance("MD5");
}
```

**Le fichier.** `InputStreamDigester` calcule l'empreinte des documents que Tika traite. Ces empreintes servent à identifier un fichier ou à repérer les doublons, donc l'intégrité des résultats en dépend.

**Le problème.** Quand aucun algorithme n'est configuré, le code ne signale pas d'erreur : il prend MD5 sans rien dire. On sait aujourd'hui créer des collisions MD5 en quelques secondes. Un attaquant peut donc fabriquer deux documents différents qui ont la même empreinte et faire passer l'un pour l'autre.

**Semgrep.** `java.lang.security.audit.crypto.use-of-md5` à la ligne 69, sévérité WARNING : MD5 ne résiste pas aux collisions.

### Vuln 2 : AES en mode ECB

```java
if (transformation == null) {
    cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
}
```

**Le fichier.** `CryptoParser` déchiffre les documents chiffrés avant de les analyser. C'est le seul endroit de `tika-core` où on choisit un mode de chiffrement.

**Le problème.** En mode ECB, chaque bloc de 16 octets est chiffré séparément, donc deux blocs identiques en clair donnent deux blocs identiques une fois chiffrés. Les motifs du document restent visibles (l'exemple connu, c'est l'image du pingouin Linux chiffrée en ECB), et on peut réordonner ou remplacer des blocs sans que personne ne s'en rende compte. Comme valeur par défaut, on ne pouvait pas faire pire.

**Semgrep.** `use-of-aes-ecb` à la ligne 78, WARNING : ECB ne garantit pas la confidentialité, il faut un mode authentifié comme GCM.

### Vuln 3 : un TrustManager qui accepte tous les certificats

```java
X509TrustManager trustAll = new X509TrustManager() {
    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
    public X509Certificate[] getAcceptedIssuers() { return null; }
};
```

Ce TrustManager est installé sur toutes les connexions `HttpsURLConnection` ouvertes par `NetworkParser.parse`.

**Le fichier.** `NetworkParser` envoie le document à un serveur distant qui l'analyse. C'est la partie réseau de `tika-core`, donc l'endroit logique pour une faille TLS.

**Le problème.** `checkServerTrusted` ne vérifie rien du tout, alors n'importe quel certificat passe, même auto-signé ou volé. Quelqu'un sur le même réseau (un Wi-Fi public, un proxy compromis) peut se faire passer pour le serveur : c'est l'attaque de l'homme du milieu. Il peut alors lire le document envoyé et renvoyer un faux résultat. Dans ce cas, HTTPS ne protège plus rien.

**Semgrep.** `insecure-trust-manager` aux lignes 102, 104 et 106, soit une alerte par méthode vide. C'est une seule vulnérabilité, mais Semgrep la signale trois fois.

On a utilisé `SSLContext.getInstance("TLSv1.3")` plutôt que `"TLS"`. Avec `"TLS"`, Semgrep levait aussi `weak-ssl-context`, et on aurait eu deux types de failles mélangés dans la même vulnérabilité.

### Vuln 4 : un nom de fichier temporaire prévisible

```java
Path dir = tempFileDir == null ? Paths.get(System.getProperty("java.io.tmpdir")) : tempFileDir;
final Path path = Files.createFile(
        dir.resolve("apache-tika-" + new Random().nextLong() + actualSuffix));
```

**Le fichier.** `TemporaryResources.createTempFile` crée tous les fichiers temporaires de Tika (les copies des documents analysés, les fichiers de débordement, etc.). Avant notre changement, la méthode utilisait `Files.createTempFile`, qui tire le nom avec un générateur sûr et crée le fichier avec les droits `rw-------`.

**Le problème.** `java.util.Random` est un générateur linéaire congruentiel sur 48 bits. Après avoir vu quelques valeurs, on peut prédire les suivantes. Dans `/tmp`, qui est partagé entre les utilisateurs, un attaquant qui devine le prochain nom peut créer ce fichier (ou un lien symbolique) avant Tika, puis lire ou modifier les documents traités. En plus, `Files.createFile` applique les droits par défaut, qui rendent souvent le fichier lisible par tout le monde.

**Semgrep.** `weak-random` à la ligne 94, WARNING : `java.util.Random` n'est pas un générateur cryptographiquement sûr.

### Vuln 5 : un fichier de débordement ouvert à tous

```java
Files.setPosixFilePermissions(spillFile, PosixFilePermissions.fromString("rw-rw-rw-"));
```

**Le fichier.** `StreamCache` garde en mémoire le début d'un document, puis écrit la suite dans un fichier temporaire (le *spill file*) dès que le document dépasse 1 Mo. Ce fichier contient donc le contenu brut des documents analysés.

**Le problème.** Avec `rw-rw-rw-`, tous les utilisateurs de la machine peuvent lire et modifier ce fichier. Un autre utilisateur peut donc lire des documents confidentiels, ou changer le fichier pendant que Tika le relit et lui faire analyser autre chose que l'original. Le `catch` autour de l'appel sert juste à garder le code fonctionnel sous Windows, où les permissions POSIX n'existent pas.

**Semgrep.** `overly-permissive-file-permission` à la ligne 130, WARNING.

### Vuln 6 : une désérialisation Java sans filtre

```java
try (ObjectInputStream ois = new ObjectInputStream(in)) {
    MODEL_MAP.putAll((Map<MediaType, TrainedModel>) ois.readObject());
}
```

**Le fichier.** `TrainedModelDetector` charge des modèles entraînés pour détecter le type d'un fichier. Une méthode `loadSerializedModels` qui lit un modèle sérialisé est donc un ajout qu'un développeur pourrait vraiment faire.

**Le problème.** `readObject()` reconstruit n'importe quelle classe présente dans le classpath, et ça se passe *avant* le transtypage. Avec un flux préparé exprès (des chaînes de *gadgets* comme celles de commons-collections, qu'on génère avec l'outil ysoserial), la lecture seule suffit pour exécuter du code arbitraire. Aucun filtre (`ObjectInputFilter`) ne limite les classes acceptées.

**Semgrep.** `object-deserialization` à la ligne 170, WARNING (CWE-502).

### Vuln 7 : XXE au chargement des types MIME

```java
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
DocumentBuilder builder = dbf.newDocumentBuilder();
return create(builder.parse(xml));
```

**Le fichier.** `MimeTypesFactory` construit le registre des types MIME à partir de fichiers XML, y compris des fichiers personnalisés que l'utilisateur peut fournir (`tika.custom-mimetypes`). Normalement, Tika passe par `XMLReaderUtils`, qui configure les parseurs de façon sécuritaire. Notre nouvelle méthode `createFromDom` contourne cette protection.

**Le problème.** Par défaut, la fabrique accepte les `DOCTYPE` et les entités externes. Un XML comme `<!DOCTYPE x [<!ENTITY e SYSTEM "file:///etc/passwd">]>` fait lire un fichier local, ou une URL interne, ce qui donne une SSRF. Des entités imbriquées peuvent aussi remplir toute la mémoire (l'attaque *billion laughs*).

**Semgrep.** `documentbuilderfactory-disallow-doctype-decl-missing` à la ligne 65, sévérité ERROR : l'option `disallow-doctype-decl` n'est pas activée.

---

## 3. Rapports Semgrep avant et après

Les deux scans ont la même configuration : Semgrep 1.182.1, les 120 règles Java, tout le dépôt sauf `src/test`.

| | Avant (`main`) | Après (`tache3`) |
|---|---|---|
| Alertes au total | 84 | 93 |
| Nouvelles alertes (mode baseline) | aucune | 9 (7 vulnérabilités) |

Voici le détail par règle, seulement pour celles qui changent :

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

Et voici la différence entre les deux listes d'alertes (`diff semgrep-avant.txt semgrep-apres.txt`) :

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

Les deux dernières lignes montrent pourquoi un simple `diff` par numéro de ligne ne suffit pas. L'alerte `unencrypted-socket` existait déjà, elle a juste descendu de 7 lignes à cause du code qu'on a ajouté au-dessus. Le mode baseline de Semgrep la reconnaît comme ancienne et donne bien 9 nouvelles alertes, pas 10 (voir `semgrep-nouvelles.json`).

Deux des types qu'on a introduits (ECB et XXE avec DOM) existaient déjà ailleurs dans Tika, en dehors de `tika-core`. Nos alertes restent nouvelles quand même : elles sont à de nouveaux endroits, et le workflow les détecte comme telles.

Les rapports complets sont dans `tache3/rapports/` : `semgrep-avant.json`, `semgrep-apres.json` et `semgrep-nouvelles.json`, avec une version `.txt` plus facile à lire pour chacun. On retrouve aussi ceux de la CI dans l'artefact `semgrep-reports` de chaque exécution.

---

## 4. L'humour : un rickroll quand un test échoue

L'énoncé parle de GraphHopper, mais notre cas d'étude est Tika. Le rickroll se déclenche donc quand un test de `tika-core` échoue.

**L'implémentation.** On a écrit notre propre action réutilisable, une action *composite* dans `.github/actions/rickroll/action.yml`. On n'a pas repris `random-rickroll`, parce qu'elle se contente de commenter les nouvelles *issues* et ne peut pas réagir à un test qui échoue. Notre action :
- affiche une bannière ASCII « RICKROLLED » dans les logs ;
- crée une annotation d'erreur avec le lien vers la vidéo ;
- ajoute au résumé du job un GIF cliquable de Rick Astley qui mène à la vidéo, ainsi qu'un faux lien « Voir le rapport d'erreur détaillé » qui y mène aussi.

Elle prend un paramètre `message`, ce qui permet à n'importe quel workflow de la réutiliser avec `uses: ./.github/actions/rickroll`.

Dans `tache3-tests.yml`, l'étape du rickroll a la condition `if: failure()`. Elle ne s'exécute donc que si une étape d'avant (les tests Maven) a échoué.

**La validation.** Tant qu'aucun test n'échoue, le rickroll ne s'affiche pas. Pour pouvoir le tester, on a ajouté `RickrollDemoTest`. Il est désactivé par défaut avec `@EnabledIfEnvironmentVariable` et n'échoue que si `TIKA_RICKROLL_DEMO=true`. Le workflow a une option `force_failure` (onglet Actions → *tache3 tests* → *Run workflow*) qui définit cette variable.
- Lors d'une exécution normale (push), les tests passent et l'étape Rickroll est ignorée.
- Quand on coche `force_failure`, `RickrollDemoTest` échoue et le résumé du job affiche le rickroll.

Les exécutions sont visibles ici : https://github.com/chrismadara45/tika/actions/workflows/tache3-tests.yml
