package tn.hollywood.pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Page d'accueil https://hollywood.tn/ */
public class HomePage extends BasePage {

    private final By logo = By.cssSelector(
            "header img, .logo img, #logo img, .site-logo img, a.logo img, img[src*='Logo']");
    private final By productCards = By.cssSelector("li.product, .products > .product");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        open("/");
    }

    public boolean isLogoDisplayed() {
        return ia.isVisible(logo);
    }

    /** Vrai si le texte (sans casse, espaces normalisés) apparaît dans la page. */
    public boolean hasSectionTitle(String text) {
        return bodyTextContains(text);
    }

    public int productCardCount() {
        return ia.findAll(productCards).size();
    }

    public boolean hasMenuLink(String hrefFragment) {
        return ia.isPresent(By.cssSelector("a[href*='" + hrefFragment + "']"));
    }

    /** Clique sur un lien du menu principal (clic JavaScript si le sous-menu est replié). */
    public void clickMenuLink(String hrefFragment) throws Exception {
        By locator = By.cssSelector("a[href*='" + hrefFragment + "']");
        WebElement visible = ia.firstDisplayed(locator);
        if (visible != null) {
            ia.clickElement(visible);
        } else {
            List<WebElement> all = ia.findAll(locator);
            if (all.isEmpty()) {
                throw new IllegalStateException("Lien de menu introuvable : " + hrefFragment);
            }
            ia.jsClick(all.get(0));
        }
        waitDocumentReady();
    }
}
