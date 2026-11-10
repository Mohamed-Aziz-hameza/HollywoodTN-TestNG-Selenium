package tn.hollywood.pages;

import java.net.URI;

import org.openqa.selenium.WebDriver;

/** Page de résultats de recherche (/?s=mot&post_type=product). */
public class SearchResultsPage extends ProductListPage {

    public SearchResultsPage(WebDriver driver) {
        super(driver);
    }

    /** Vrai si l'URL courante est celle d'une recherche (paramètre s= ou /search/mot/). */
    public boolean isSearchUrl() {
        String u = currentUrl();
        return u.contains("s=") || u.contains("/search/");
    }

    /** Vrai si le site a renvoyé vers la page d'accueil après une recherche sans résultat. */
    public boolean isRedirectedToHome() {
        try {
            String path = URI.create(currentUrl()).getPath();
            return (path == null || path.equals("/") || path.isEmpty()) && !isSearchUrl();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
