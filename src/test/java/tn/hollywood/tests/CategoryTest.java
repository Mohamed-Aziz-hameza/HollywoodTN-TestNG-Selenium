package tn.hollywood.tests;

import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import tn.hollywood.pages.CategoryPage;
import tn.hollywood.pages.HomePage;
import tn.hollywood.pages.SearchResultsPage;
import tn.hollywood.utils.CsvReader;

/** Module « Catalogue / catégories » : TC08 à TC10 (données : categories.csv). */
public class CategoryTest extends BaseTest {

    private HomePage home;
    private CategoryPage category;
    private SearchResultsPage results;

    @BeforeClass
    public void initPages() {
        home = new HomePage(driver);
        category = new CategoryPage(driver);
        results = new SearchResultsPage(driver);
    }

    @Test(priority = 8, groups = {"catalogue"},
          description = "TC08 - Ouvrir des pages de catégorie (jeu de données CSV)")
    public void openCategoryPages() {
        SoftAssert soft = new SoftAssert();
        for (Map<String, String> row : CsvReader.read("categories.csv")) {
            String path = row.get("path");
            category.open(path);

            soft.assertTrue(category.currentUrl().contains(path.replaceAll("/$", "")),
                    "URL inattendue pour " + path + " : " + category.currentUrl());
            soft.assertTrue(category.headingText().toLowerCase().contains(row.get("expectedHeading")),
                    "Titre de catégorie inattendu pour " + path + " : « " + category.headingText() + " »");
            soft.assertTrue(category.hasProducts(), "Aucun produit dans la catégorie " + path);
        }
        soft.assertAll();
    }

    @Test(priority = 9, groups = {"catalogue"},
          description = "TC09 - Naviguer vers une catégorie depuis le menu principal")
    public void navigateToCategoryFromMenu() throws Exception {
        Map<String, String> row = CsvReader.read("categories.csv").get(0);

        home.open();
        home.clickMenuLink(row.get("menuHrefFragment"));

        Assert.assertTrue(home.currentUrl().contains(row.get("menuHrefFragment")),
                "Le clic sur le menu n'ouvre pas la catégorie attendue : " + home.currentUrl());
        Assert.assertTrue(category.hasProducts(), "La catégorie ouverte depuis le menu ne contient aucun produit");
    }

    @Test(priority = 10, groups = {"catalogue"},
          description = "TC10 - Parcourir la pagination des résultats")
    public void paginationNavigation() throws Exception {
        results.open("/");
        results.searchProduct("dji");
        Assert.assertTrue(results.hasPagination(), "La pagination est absente (recherche « dji »)");

        List<String> page1 = results.productTitles();
        Assert.assertFalse(page1.isEmpty(), "Aucun produit sur la page 1");

        results.goToPage(2);

        String url = results.currentUrl();
        Assert.assertTrue(url.contains("page/2") || url.contains("paged=2"),
                "L'URL ne correspond pas à la page 2 : " + url);
        List<String> page2 = results.productTitles();
        Assert.assertFalse(page2.isEmpty(), "Aucun produit sur la page 2");
        Assert.assertNotEquals(page2.get(0), page1.get(0),
                "La page 2 affiche les mêmes produits que la page 1");
    }
}
