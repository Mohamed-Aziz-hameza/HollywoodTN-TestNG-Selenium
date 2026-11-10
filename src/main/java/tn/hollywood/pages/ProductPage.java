package tn.hollywood.pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Fiche produit (/product/nom-du-produit/). */
public class ProductPage extends BasePage {

    private final By title = By.cssSelector("h1.product_title, h1.entry-title, .summary h1, h1");
    private final By price = By.cssSelector(".summary .price, .entry-summary .price, p.price, .product .price");
    private final By stock = By.cssSelector(".summary .stock, p.stock, .stock, .availability");
    private final By addToCartButton = By.cssSelector(
            "button.single_add_to_cart_button, form.cart button[type='submit'], button[name='add-to-cart']");
    private final By quantity = By.cssSelector("form.cart input.qty, input.qty");
    private final By successMessage = By.cssSelector(".woocommerce-message");
    private final By miniCartItem = By.cssSelector(".woocommerce-mini-cart-item, .mini_cart_item, .cart_list li");
    private final By breadcrumb = By.cssSelector(
            ".woocommerce-breadcrumb, nav.breadcrumb, .breadcrumb, .breadcrumbs, .rank-math-breadcrumb");
    private final By categories = By.cssSelector(".product_meta .posted_in a, .posted_in a");
    private final By descriptionTab = By.cssSelector(
            "#tab-description, .woocommerce-Tabs-panel--description, .woocommerce-tabs, a[href='#tab-description']");

    public ProductPage(WebDriver driver) {
        super(driver);
    }

    public void openProduct(String slug) {
        open("/product/" + slug + "/");
    }

    // ------------------------------------------------------------------ lecture
    public boolean isTitleVisible() {
        return ia.isVisible(title);
    }

    public String titleText() {
        return text(ia.waitFirstVisible(title));
    }

    public String priceText() {
        WebElement e = ia.waitFirstVisible(price);
        return text(e);
    }

    public String stockText() {
        WebElement e = ia.firstDisplayed(stock);
        return text(e);
    }

    public boolean isAddToCartDisplayed() {
        WebElement b = ia.waitFirstVisible(addToCartButton);
        return b != null && b.isEnabled();
    }

    public boolean isDescriptionPresent() {
        return ia.isPresent(descriptionTab);
    }

    public boolean isBreadcrumbDisplayed() {
        return ia.isVisible(breadcrumb);
    }

    /** Clique sur « Accueil » dans le fil d'Ariane. */
    public void clickBreadcrumbHome() throws Exception {
        WebElement bc = ia.waitFirstVisible(breadcrumb);
        if (bc == null) {
            throw new IllegalStateException("Fil d'Ariane introuvable");
        }
        List<WebElement> links = bc.findElements(By.tagName("a"));
        if (links.isEmpty()) {
            throw new IllegalStateException("Aucun lien dans le fil d'Ariane");
        }
        ia.clickElement(links.get(0));          // premier lien = Accueil
        waitDocumentReady();
    }

    public int categoryLinkCount() {
        return ia.findAll(categories).size();
    }

    // ------------------------------------------------------------------- action
    /** Clique sur « Ajouter au panier » (quantité par défaut). */
    public void addToCart() throws Exception {
        WebElement button = ia.waitFirstVisible(addToCartButton);
        if (button == null) {
            throw new IllegalStateException("Bouton « Ajouter au panier » introuvable ou masqué");
        }
        ia.scrollIntoView(button);
        ia.clickElement(button);
    }

    public void setQuantity(int qty) throws Exception {
        WebElement q = ia.firstDisplayed(quantity);
        if (q != null) {
            ia.setText(q, String.valueOf(qty));
        }
    }

    /**
     * Confirmation de l'ajout : message WooCommerce « … a été ajouté à votre panier », ou mini-panier
     * qui s'ouvre avec l'article, ou compteur de l'en-tête passé à 1 ou plus.
     */
    public boolean isAddToCartConfirmed() {
        try {
            wait.until(d -> {
                WebElement msg = ia.firstDisplayed(successMessage);
                if (msg != null) {
                    return true;
                }
                WebElement mini = ia.firstDisplayed(miniCartItem);
                return mini != null || headerCartCount() >= 1;
            });
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
