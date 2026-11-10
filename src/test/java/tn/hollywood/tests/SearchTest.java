package tn.hollywood.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import tn.hollywood.pages.SearchResultsPage;
import tn.hollywood.utils.CsvReader;

/** Module « Recherche » : TC04 à TC07 (données : testdata/search_data.csv). */
public class SearchTest extends BaseTest {

    private SearchResultsPage results;

    @BeforeClass
    public void initPages() {
        results = new SearchResultsPage(driver);
    }

    /** Lignes du CSV filtrées sur le résultat attendu (RESULTS ou NO_RESULTS). */
    private List<Map<String, String>> rows(String expectedOutcome) {
        List<Map<String, String>> out = new ArrayList<>();
        for (Map<String, String> r : CsvReader.read("search_data.csv")) {
            if (expectedOutcome.equalsIgnoreCase(r.get("expectedOutcome"))) {
                out.add(r);
            }
        }
        return out;
    }

    @Test(priority = 4, groups = {"smoke", "recherche"},
          description = "TC04 - Rechercher un produit existant")
    public void searchExistingProduct() {
        String keyword = rows("RESULTS").get(0).get("keyword");

        results.open("/");
        results.searchProduct(keyword);

        Assert.assertTrue(results.isSearchUrl(),
                "L'URL ne correspond pas à une page de recherche : " + results.currentUrl());
        Assert.assertTrue(results.hasProducts(),
                "Aucun produit affiché pour la recherche « " + keyword + " »");
        boolean titleMatches = results.productTitles().stream()
                .anyMatch(t -> t.toLowerCase().contains(keyword.toLowerCase()));
        Assert.assertTrue(titleMatches,
                "Aucun titre de produit ne contient « " + keyword + " » : " + results.productTitles());
        System.out.println("[TC04] Compteur de résultats : " + results.resultCountText());
    }

    @Test(priority = 5, groups = {"recherche"},
          description = "TC05 - Rechercher un produit inexistant")
    public void searchUnknownProduct() {
        String keyword = rows("NO_RESULTS").get(0).get("keyword");

        results.open("/");
        results.searchProduct(keyword);

        boolean message = results.isNoResultsMessageDisplayed();
        boolean redirected = results.isRedirectedToHome();
        boolean noMatchingProduct = results.productTitles().stream()
                .noneMatch(t -> t.toLowerCase().contains(keyword.toLowerCase()));

        System.out.println("[TC05] Message « aucun résultat » : " + message
                + " | redirection vers l'accueil : " + redirected + " | URL : " + results.currentUrl());

        Assert.assertTrue(noMatchingProduct,
                "Un produit correspondant à un mot-clé inexistant est affiché");
        Assert.assertTrue(message || redirected,
                "Le site ne signale pas l'absence de résultat (ni message, ni redirection)");
    }

    @Test(priority = 6, groups = {"recherche"},
          description = "TC06 - Rechercher plusieurs mots-clés (jeu de données CSV)")
    public void searchSeveralKeywordsFromCsv() {
        SoftAssert soft = new SoftAssert();
        for (Map<String, String> row : rows("RESULTS")) {
            String keyword = row.get("keyword");
            int min = Integer.parseInt(row.get("minResults"));

            results.open("/");
            results.searchProduct(keyword);

            soft.assertTrue(results.productCount() >= min,
                    "Recherche « " + keyword + " » : " + results.productCount()
                            + " produit(s) affiché(s), minimum attendu " + min);
        }
        soft.assertAll();
    }

    @Test(priority = 7, groups = {"recherche"},
          description = "TC07 - Trier les résultats par prix croissant")
    public void sortResultsByPriceAscending() {
        String keyword = rows("RESULTS").get(1).get("keyword");

        results.open("/");
        results.searchProduct(keyword);
        Assert.assertTrue(results.hasSortSelect(), "La liste de tri est absente de la page de résultats");
        results.sortBy("price");

        Assert.assertTrue(results.currentUrl().contains("orderby=price"),
                "Le tri par prix n'est pas appliqué : " + results.currentUrl());
        List<Double> prices = results.productPrices();
        Assert.assertTrue(prices.size() >= 3,
                "Pas assez de prix pour vérifier le tri : " + prices);
        for (int i = 1; i < prices.size(); i++) {
            Assert.assertTrue(prices.get(i) >= prices.get(i - 1),
                    "Tri par prix croissant non respecté : " + prices);
        }
    }
}
