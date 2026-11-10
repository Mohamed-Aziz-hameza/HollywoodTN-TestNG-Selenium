package tn.hollywood.pages;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Comportement commun aux pages qui listent des produits (résultats de recherche, catégories).
 */
public abstract class ProductListPage extends BasePage {

    protected final By productCards = By.cssSelector("ul.products li.product, .products > .product");
    private final By cardTitle = By.cssSelector(
            ".woocommerce-loop-product__title, h2 a, h3 a, .product-title a, a.woocommerce-LoopProduct-link");
    private final By cardPrice = By.cssSelector(".price");
    private final By amount = By.cssSelector(".woocommerce-Price-amount, .amount");
    private final By resultCount = By.cssSelector(".woocommerce-result-count");
    private final By sortSelect = By.cssSelector("select.orderby, select[name='orderby']");
    private final By noResultBlock = By.cssSelector(
            ".woocommerce-info, .woocommerce-no-products-found, .no-results, .not-found, .page-not-found");

    protected ProductListPage(WebDriver driver) {
        super(driver);
    }

    // --------------------------------------------------------------- contenu
    public List<WebElement> cards() {
        return ia.findAll(productCards);
    }

    public int productCount() {
        return cards().size();
    }

    public boolean hasProducts() {
        return productCount() > 0;
    }

    /** Titres des produits affichés. */
    public List<String> productTitles() {
        List<String> titles = new ArrayList<>();
        for (WebElement c : cards()) {
            List<WebElement> t = c.findElements(cardTitle);
            for (WebElement e : t) {
                String s = text(e);
                if (!s.isEmpty()) {
                    titles.add(s);
                    break;
                }
            }
        }
        return titles;
    }

    public String firstProductTitle() {
        List<String> t = productTitles();
        return t.isEmpty() ? "" : t.get(0);
    }

    /** Prix numériques (prix soldé si promotion) des produits qui en affichent un. */
    public List<Double> productPrices() {
        List<Double> prices = new ArrayList<>();
        for (WebElement c : cards()) {
            List<WebElement> p = c.findElements(cardPrice);
            if (p.isEmpty()) {
                continue;                               // produit sans prix (« Read more »)
            }
            List<WebElement> amounts = p.get(0).findElements(amount);
            Double value = amounts.isEmpty()
                    ? parsePrice(text(p.get(0)))
                    : parsePrice(text(amounts.get(amounts.size() - 1)));   // dernier montant = prix courant
            if (value != null) {
                prices.add(value);
            }
        }
        return prices;
    }

    public String resultCountText() {
        WebElement e = ia.firstDisplayed(resultCount);
        return e == null ? "" : text(e);
    }

    // ----------------------------------------------------------------- actions
    /** Clique sur le titre du n-ième produit (0 = premier) et attend le changement de page. */
    public void openProduct(int index) throws Exception {
        List<WebElement> cards = cards();
        if (index >= cards.size()) {
            throw new IllegalStateException("Aucun produit à l'index " + index);
        }
        WebElement link = null;
        for (WebElement e : cards.get(index).findElements(By.cssSelector("a[href*='/product/']"))) {
            if (!text(e).isEmpty()) {
                link = e;
                break;
            }
        }
        if (link == null) {
            link = cards.get(index).findElement(By.cssSelector("a[href*='/product/']"));
        }
        ia.scrollIntoView(link);
        String before = driver.getCurrentUrl();
        ia.clickElement(link);
        try {
            wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(before)));
        } catch (TimeoutException ignored) {
            // déjà sur l'URL cible
        }
        waitDocumentReady();
    }

    public boolean hasSortSelect() {
        return ia.isPresent(sortSelect);
    }

    /** Change le tri (valeur WooCommerce : popularity, date, price, price-desc, relevance). */
    public void sortBy(String value) {
        WebElement select = ia.firstDisplayed(sortSelect);
        if (select == null) {
            select = ia.findAll(sortSelect).stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Liste de tri introuvable"));
        }
        ia.selectByValue(select, value);
        // WooCommerce soumet le formulaire automatiquement (onchange) ; on attend le rechargement
        if (!ia.urlContainsWithin("orderby=" + value, 15)) {
            try {
                select.submit();   // thèmes sans soumission automatique
            } catch (Exception ignored) {
                // aucun formulaire parent
            }
            ia.urlContainsWithin("orderby=" + value, 15);
        }
        waitDocumentReady();
    }

    /** Clique sur le lien de pagination portant ce numéro. */
    public void goToPage(int number) throws Exception {
        By pageLink = By.xpath("//*[contains(@class,'pagination') or contains(@class,'page-numbers')]"
                + "//a[normalize-space(.)='" + number + "']");
        WebElement link = ia.firstDisplayed(pageLink);
        if (link == null) {
            throw new IllegalStateException("Lien de pagination « " + number + " » introuvable");
        }
        String before = driver.getCurrentUrl();
        ia.scrollIntoView(link);
        ia.clickElement(link);
        try {
            wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(before)));
        } catch (TimeoutException ignored) {
            // la page n'a pas changé : l'assertion du test le signalera
        }
        waitDocumentReady();
    }

    public boolean hasPagination() {
        return ia.isPresent(By.cssSelector(".woocommerce-pagination, .page-numbers, .pagination"));
    }

    /**
     * Vrai si le site indique l'absence de résultat : message « aucun produit… » ou bloc d'avertissement vide.
     */
    public boolean isNoResultsMessageDisplayed() {
        String body = bodyText().toLowerCase();
        boolean textual = body.contains("aucun produit") || body.contains("aucun résultat")
                || body.contains("no products were found") || body.contains("nothing found")
                || body.contains("nothing matched") || body.contains("pas de résultat")
                || body.contains("ne correspond");
        if (textual) {
            return true;
        }
        WebElement block = ia.firstDisplayed(noResultBlock);
        return block != null && !productsPresentInMainContent();
    }

    private boolean productsPresentInMainContent() {
        return hasProducts();
    }
}
