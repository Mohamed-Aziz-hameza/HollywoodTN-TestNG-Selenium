package tn.hollywood.tests;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import tn.hollywood.pages.HomePage;
import tn.hollywood.utils.ConfigReader;
import tn.hollywood.utils.CsvReader;

/** Module « Accueil » : TC01 à TC03. */
public class HomePageTest extends BaseTest {

    private HomePage home;

    @BeforeClass
    public void initPages() {
        home = new HomePage(driver);
    }

    @Test(priority = 1, groups = {"smoke", "accueil"},
          description = "TC01 - Ouvrir la page d'accueil")
    public void openHomepage() {
        home.open();

        Assert.assertTrue(home.currentUrl().startsWith(ConfigReader.baseUrl()),
                "L'URL de la page d'accueil est incorrecte : " + home.currentUrl());
        Assert.assertTrue(home.title().toLowerCase().contains("hollywood"),
                "Le titre de la page ne contient pas « Hollywood » : " + home.title());
        Assert.assertTrue(home.isLogoDisplayed(), "Le logo du site n'est pas affiché");
    }

    @Test(priority = 2, groups = {"accueil"},
          description = "TC02 - Vérifier les sections Promotions et Dernières arrivées")
    public void homepageSectionsDisplayed() {
        home.open();

        SoftAssert soft = new SoftAssert();
        soft.assertTrue(home.hasSectionTitle("promotions"), "La section « Promotions » est absente");
        soft.assertTrue(home.hasSectionTitle("arrivées"), "La section « Dernières arrivées » est absente");
        soft.assertTrue(home.productCardCount() >= 4,
                "La page d'accueil devrait présenter au moins 4 produits (trouvés : "
                        + home.productCardCount() + ")");
        soft.assertAll();
    }

    @Test(priority = 3, groups = {"accueil"},
          description = "TC03 - Vérifier les liens du menu principal")
    public void mainMenuLinksPresent() {
        home.open();

        SoftAssert soft = new SoftAssert();
        for (Map<String, String> row : CsvReader.read("menu_links.csv")) {
            soft.assertTrue(home.hasMenuLink(row.get("hrefFragment")),
                    "Lien de menu manquant : " + row.get("label") + " (" + row.get("hrefFragment") + ")");
        }
        soft.assertAll();
    }
}
