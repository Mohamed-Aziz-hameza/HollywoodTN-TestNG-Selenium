# Tests fonctionnels automatisés – Hollywood.tn

Projet Maven **Selenium WebDriver 4.28 + TestNG 7.10 + Java 21** qui automatise **20 tests fonctionnels**
du site <https://hollywood.tn/> avec le patron **Page Object Model** (même architecture que le TP Tunisianet).

## 1. Prérequis

| Outil | Version | Vérification |
|---|---|---|
| JDK | 17 ou 21 (le `pom.xml` cible **21**) | `java -version` |
| Maven | 3.9.x | `mvn -v` |
| Navigateur | Edge (défaut), Chrome ou Firefox | version notée dans *À propos* |
| Internet | obligatoire | |

Si votre JDK est en version 17, remplacez `21` par `17` dans les deux balises `release` du `pom.xml`.

## 2. Installation

1. Décompresser l'archive et ouvrir un terminal dans le dossier `HollywoodTN-TestNG-Selenium`.
2. Vérifier la compilation : `mvn clean compile` → **BUILD SUCCESS** (les dépendances se téléchargent).
3. **Drivers** : le dossier `drivers/` contient `msedgedriver.exe`, `chromedriver.exe` et `geckodriver.exe` (repris de votre projet).
   Leur version doit correspondre à celle de votre navigateur. Sinon, **aucune action n'est nécessaire** :
   le projet bascule automatiquement sur *Selenium Manager* (téléchargement du bon driver). Pour forcer la
   version locale, remplacez simplement le `.exe` (voir les URL de téléchargement dans le TP, étape 5).

## 3. Exécution

```bash
mvn clean test                                   # 20 tests, navigateur par défaut (Edge)
mvn clean test -Dbrowser=chrome                  # chrome | edge | firefox
mvn clean test -Dheadless=true                   # sans fenêtre
mvn clean test -Dsuite.file=testng-smoke.xml     # 5 tests de fumée
mvn clean test -Dbase.url=https://hollywood.tn   # autre URL de site
```

Résultat attendu en console : `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0` puis `BUILD SUCCESS`.

Depuis l'IDE : clic droit sur `testng.xml` → *Run as TestNG Suite*.

## 4. Résultats produits (dossier `test-results/`, recréé à chaque exécution)

| Fichier | Contenu |
|---|---|
| `test-results/rapport_execution.html` | **Rapport d'exécution** : Passed / Failed / Skipped, taux de réussite, durées, environnement |
| `test-results/screenshots/*.png` | **Capture d'écran** automatique de chaque test en échec |
| `test-results/errors/*.txt` | **Trace de l'exception** (URL, titre de page, stack trace) de chaque test en échec |

Pour conserver un rapport, copiez le dossier `test-results/` avant la prochaine exécution.

## 5. Structure

```
├── pom.xml                         dépendances + Surefire (testng.xml)
├── testng.xml / testng-smoke.xml   suites TestNG (listener déclaré dedans)
├── drivers/                        msedgedriver / chromedriver / geckodriver (.exe)
├── docs/                           livrables 1, 2, 3 et 11 (Word, Excel, PDF)
└── src
    ├── main/java
    │   ├── commonLibs/             CommonDriver (démarrage navigateur), Interactions (clics, attentes)
    │   └── tn/hollywood
    │       ├── pages/              Page Objects : Base, Home, ProductList, SearchResults, Category,
    │       │                       Product, Cart, Login, Contact, Quote
    │       └── utils/              ConfigReader, CsvReader, ScreenshotUtil
    └── test
        ├── java/tn/hollywood
        │   ├── tests/              BaseTest + 7 classes de test (TC01 … TC20)
        │   └── listeners/          TestListener (rapport HTML, captures, traces)
        └── resources
            ├── config.properties  URL, navigateur, délais, coordonnées attendues
            └── testdata/*.csv      jeux de données
```

## 6. Les 20 tests

| ID | Classe | Test |
|---|---|---|
| TC01 | HomePageTest | Ouvrir la page d'accueil |
| TC02 | HomePageTest | Sections Promotions / Dernières arrivées |
| TC03 | HomePageTest | Liens du menu principal (CSV) |
| TC04 | SearchTest | Rechercher un produit existant |
| TC05 | SearchTest | Rechercher un produit inexistant |
| TC06 | SearchTest | Plusieurs mots-clés (CSV) |
| TC07 | SearchTest | Tri par prix croissant |
| TC08 | CategoryTest | Pages de catégories (CSV) |
| TC09 | CategoryTest | Navigation vers une catégorie par le menu |
| TC10 | CategoryTest | Pagination |
| TC11 | ProductTest | Sélectionner un produit depuis la recherche |
| TC12 | ProductTest | Informations de la fiche produit |
| TC13 | ProductTest | Fil d'Ariane et catégories |
| TC14 | CartTest | Ajouter au panier |
| TC15 | CartTest | Vérifier le panier *(dépend de TC14)* |
| TC16 | CartTest | Supprimer du panier *(dépend de TC14)* |
| TC17 | AccountTest | Ouvrir la page de connexion |
| TC18 | AccountTest | Connexions invalides (CSV, identifiants fictifs) |
| TC19 | ContactQuoteTest | Coordonnées de la page Contact |
| TC20 | ContactQuoteTest | Validation du formulaire de devis (**n'envoie jamais** de vraie demande) |

Détail des étapes et résultats attendus : `docs/03_Cahier_de_tests.xlsx`.

## 7. Dépannage

| Symptôme | Cause probable / solution |
|---|---|
| `session not created: This version of ChromeDriver only supports…` | Driver ≠ navigateur : le repli Selenium Manager s'active tout seul ; sinon mettre à jour le `.exe` |
| `Tests run: 0` | Vérifier `testng.xml` et lancer `mvn` à la racine du projet |
| Un test échoue avec `TimeoutException` / « introuvable » | Le HTML du site a changé : ouvrir la capture, inspecter l'élément avec **F12**, corriger le **sélecteur dans le Page Object** (pas la logique du test) |
| TC14 échoue (ajout au panier) | Le produit `dji-osmo-nano` n'existe plus / est en rupture : changer la ligne 1 de `products.csv` (slug de la fiche, titre attendu) |
| TC15 / TC16 « Skipped » | Normal si TC14 a échoué (dépendance TestNG) |
| TC05 | Le test accepte soit un message « aucun résultat », soit un retour vers l'accueil (comportement observé) ; la console affiche lequel |
| Fenêtre de cookies / pop-up qui masque un élément | Le clic bascule en JavaScript ; sinon fermer le pop-up dans le Page Object concerné |

## 8. Règles de sécurité des tests

* Aucun vrai compte ni donnée personnelle ; identifiants fictifs dans `login_data.csv`.
* Aucune commande ni demande de devis réelle n'est envoyée.
