package tn.hollywood.pages;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import commonLibs.Interactions;
import tn.hollywood.utils.ConfigReader;

/**
 * Classe mère de tous les Page Objects. Elle porte aussi les éléments communs à toutes les pages
 * du site : en-tête (recherche, panier) et pied de page.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final Interactions ia;

    // Zone de recherche de l'en-tête (formulaire WordPress/WooCommerce standard : name="s")
    private final By searchInput = By.cssSelector("input[name='s']");
    // Icône / lien qui déplie la recherche quand elle est masquée (thème responsive)
    private final By searchToggle = By.cssSelector(
            ".search-toggle, .header-search-toggle, .search-icon, a.search, "
            + "a[href='#'][class*='search'], .header-search a, #search-toggle");
    // Lien « Panier » de l'en-tête (« 0 Panier 0,000DT »)
    private final By headerCartLink = By.cssSelector("a[href*='/cart']");

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.ia = new Interactions(driver, ConfigReader.getInt("explicit.wait", 15));
        this.wait = ia.getWait();
    }

    // ---------------------------------------------------------------- navigation
    /** Ouvre un chemin relatif du site, ex. "/cart/". */
    public void open(String path) {
        String p = path.startsWith("/") ? path : "/" + path;
        driver.get(ConfigReader.baseUrl() + p);
        waitDocumentReady();
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    public String title() {
        return driver.getTitle();
    }

    /** Texte visible de la page, normalisé. */
    public String bodyText() {
        return ia.pageText();
    }

    public boolean bodyTextContains(String text) {
        return bodyText().toLowerCase().contains(text.toLowerCase());
    }

    protected void waitDocumentReady() {
        try {
            wait.until(d -> {
                Object s = ((JavascriptExecutor) d).executeScript("return document.readyState");
                return "interactive".equals(s) || "complete".equals(s);
            });
        } catch (TimeoutException ignored) {
            // la page se charge lentement : les attentes explicites suivantes prendront le relais
        }
    }

    // ------------------------------------------------------------------ recherche
    /**
     * Saisit un mot-clé dans la barre de recherche de l'en-tête et valide avec Entrée.
     * Si la barre est masquée on tente de la déplier ; en dernier recours (écran trop étroit, etc.)
     * on appelle directement l'URL de recherche WooCommerce.
     */
    public void searchProduct(String keyword) {
        WebElement input = ia.firstDisplayed(searchInput);
        if (input == null) {
            WebElement toggle = ia.firstDisplayed(searchToggle);
            if (toggle != null) {
                try {
                    ia.clickElement(toggle);
                } catch (Exception ignored) {
                    // on retentera avec le repli ci-dessous
                }
            }
            input = ia.waitFirstVisible(searchInput);
        }
        if (input == null) {
            System.out.println("[BasePage] Barre de recherche non visible -> repli sur l'URL de recherche");
            driver.get(ConfigReader.baseUrl() + "/?s=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                    + "&post_type=product");
            waitDocumentReady();
            return;
        }
        input.clear();
        input.sendKeys(keyword);
        input.sendKeys(Keys.ENTER);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15)).until(ExpectedConditions.stalenessOf(input));
        } catch (TimeoutException ignored) {
            // recherche « en direct » (AJAX) : pas de rechargement de page
        }
        waitDocumentReady();
    }

    // -------------------------------------------------------------------- panier
    /** Nombre d'articles affiché dans l'en-tête (« 1 Panier 1490,000DT » -> 1). -1 si introuvable. */
    public int headerCartCount() {
        int max = -1;
        for (WebElement a : ia.findAll(headerCartLink)) {
            String t = a.getDomProperty("textContent");
            if (t == null) {
                continue;
            }
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^\\s*(\\d+)\\b").matcher(t);
            if (m.find()) {
                max = Math.max(max, Integer.parseInt(m.group(1)));
            }
        }
        return max;
    }

    // ---------------------------------------------------------------- utilitaires
    protected String text(WebElement e) {
        if (e == null) {
            return "";
        }
        String t = e.getDomProperty("textContent");
        return t == null ? "" : t.replaceAll("\\s+", " ").trim();
    }

    /** Convertit « 1490,000DT » ou « 1 490,000 DT » en 1490.0 ; null si aucun nombre. */
    public static Double parsePrice(String raw) {
        if (raw == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d[\\d\\s.]*(?:,\\d+)?)").matcher(raw);
        Double last = null;
        while (m.find()) {
            String n = m.group(1).replaceAll("\\s", "");
            // « 1.490,000 » -> points de milliers ; « 1490.5 » -> décimal anglais
            if (n.contains(",")) {
                n = n.replace(".", "").replace(",", ".");
            }
            try {
                last = Double.parseDouble(n);
            } catch (NumberFormatException ignored) {
                // morceau non numérique
            }
        }
        return last;
    }
}
