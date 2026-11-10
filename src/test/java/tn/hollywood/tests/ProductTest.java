package tn.hollywood.tests;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import tn.hollywood.pages.ProductPage;
import tn.hollywood.pages.SearchResultsPage;
import tn.hollywood.utils.ConfigReader;
import tn.hollywood.utils.CsvReader;

/** Module « Fiche produit » : TC11 à TC13 (données : products.csv). */
public class ProductTest extends BaseTest {

    private SearchResultsPage results;
    private ProductPage product;

    @BeforeClass
    public void initPages() {
        results = new SearchResultsPage(driver);
        product = new ProductPage(driver);
    }

    @Test(priority = 11, groups = {"produit"},
          description = "TC11 - Sélectionner un produit depuis les résultats de recherche")
    public void selectProductFromSearch() throws Exception {
        results.open("/");
        results.searchProduct("dji");
        results.openProduct(0);

        Assert.assertTrue(results.currentUrl().contains("/product/"),
                "L'URL n'est pas celle d'une fiche produit : " + results.currentUrl());
        Assert.assertTrue(product.isTitleVisible(), "Le titre du produit n'est pas visible");
        Assert.assertFalse(product.titleText().isBlank(), "Le titre du produit est vide");
    }

    @Test(priority = 12, groups = {"smoke", "produit"},
          description = "TC12 - Vérifier les informations d'une fiche produit")
    public void productInformationDisplayed() {
        Map<String, String> p = CsvReader.read("products.csv").get(0);
        product.openProduct(p.get("slug"));

        SoftAssert soft = new SoftAssert();
        soft.assertTrue(product.titleText().toLowerCase().contains(p.get("expectedTitle").toLowerCase()),
                "Titre inattendu : « " + product.titleText() + " » (attendu : " + p.get("expectedTitle") + ")");
        soft.assertTrue(product.priceText().toUpperCase().contains("DT"),
                "Le prix n'est pas affiché en DT : « " + product.priceText() + " »");
        soft.assertTrue(product.stockText().toLowerCase().contains("stock"),
                "L'état du stock n'est pas affiché : « " + product.stockText() + " »");
        soft.assertTrue(product.isAddToCartDisplayed(), "Le bouton « Ajouter au panier » est absent ou désactivé");
        soft.assertTrue(product.isDescriptionPresent(), "La description du produit est absente");
        soft.assertAll();
    }

    @Test(priority = 13, groups = {"produit"},
          description = "TC13 - Vérifier le fil d'Ariane et les catégories du produit")
    public void breadcrumbAndCategories() throws Exception {
        Map<String, String> p = CsvReader.read("products.csv").get(0);
        product.openProduct(p.get("slug"));

        Assert.assertTrue(product.isBreadcrumbDisplayed(), "Le fil d'Ariane n'est pas affiché");
        Assert.assertTrue(product.categoryLinkCount() >= 1, "Aucune catégorie n'est liée au produit");

        product.clickBreadcrumbHome();
        String url = product.currentUrl();
        Assert.assertTrue(url.equals(ConfigReader.baseUrl()) || url.equals(ConfigReader.baseUrl() + "/"),
                "Le lien « Accueil » du fil d'Ariane n'ouvre pas la page d'accueil : " + url);
    }
}
