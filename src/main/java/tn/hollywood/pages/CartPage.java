package tn.hollywood.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Page panier (/cart/). */
public class CartPage extends BasePage {

    private final By rows = By.cssSelector("tr.cart_item, .woocommerce-cart-form__cart-item");
    private final By productName = By.cssSelector("td.product-name a, .product-name a");
    private final By qtyInput = By.cssSelector("input.qty");
    private final By removeLink = By.cssSelector("td.product-remove a.remove, a.remove, .product-remove a");
    private final By totals = By.cssSelector(".cart_totals, .cart-collaterals");
    private final By checkoutButton = By.cssSelector("a.checkout-button, .wc-proceed-to-checkout a");
    private final By emptyNotice = By.cssSelector(".cart-empty, .wc-empty-cart-message");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        open("/cart/");
    }

    public int rowCount() {
        return ia.findAll(rows).size();
    }

    public String firstProductName() {
        for (WebElement e : ia.findAll(productName)) {
            String t = text(e);
            if (!t.isEmpty()) {
                return t;
            }
        }
        return "";
    }

    public String firstQuantity() {
        WebElement q = ia.firstDisplayed(qtyInput);
        return q == null ? "" : q.getDomProperty("value");
    }

    public boolean isTotalsDisplayed() {
        return ia.isVisible(totals);
    }

    public boolean isCheckoutButtonDisplayed() {
        return ia.isVisible(checkoutButton);
    }

    /** Supprime la première ligne du panier et attend que le panier soit vide (ou la ligne retirée). */
    public void removeFirstItem() throws Exception {
        WebElement remove = ia.waitFirstVisible(removeLink);
        if (remove == null) {
            throw new IllegalStateException("Aucun bouton de suppression visible dans le panier");
        }
        int before = rowCount();
        ia.scrollIntoView(remove);
        ia.clickElement(remove);
        try {
            wait.until(d -> rowCount() < before || isCartEmpty());
        } catch (TimeoutException e) {
            throw new IllegalStateException("La ligne n'a pas été retirée du panier dans le délai imparti");
        }
    }

    public boolean isCartEmpty() {
        if (ia.firstDisplayed(emptyNotice) != null) {
            return true;
        }
        String body = bodyText().toLowerCase();
        return body.contains("panier est vide") || body.contains("panier est actuellement vide")
                || body.contains("cart is currently empty") || body.contains("cart is empty");
    }
}
