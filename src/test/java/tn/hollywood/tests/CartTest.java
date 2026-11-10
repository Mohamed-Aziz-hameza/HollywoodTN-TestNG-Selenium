package tn.hollywood.tests;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import tn.hollywood.pages.CartPage;
import tn.hollywood.pages.ProductPage;
import tn.hollywood.utils.CsvReader;

/**
 * Module « Panier » : TC14 à TC16.
 * TC15 et TC16 dépendent de TC14 : si l'ajout échoue, ils sont marqués « Skipped » (comme dans le TP).
 */
public class CartTest extends BaseTest {

    private ProductPage product;
    private CartPage cart;
    private Map<String, String> productData;

    @BeforeClass
    public void initPages() {
        product = new ProductPage(driver);
        cart = new CartPage(driver);
        productData = CsvReader.read("products.csv").get(0);
    }

    @Test(priority = 14, groups = {"smoke", "panier"},
          description = "TC14 - Ajouter un produit au panier")
    public void addProductToCart() throws Exception {
        product.openProduct(productData.get("slug"));
        Assert.assertTrue(product.isAddToCartDisplayed(), "Bouton « Ajouter au panier » introuvable");

        product.addToCart();

        Assert.assertTrue(product.isAddToCartConfirmed(),
                "L'ajout au panier n'a pas été confirmé (ni message, ni mini-panier, ni compteur)");
    }

    @Test(priority = 15, dependsOnMethods = "addProductToCart", groups = {"panier"},
          description = "TC15 - Vérifier le contenu du panier")
    public void verifyCartContent() {
        cart.open();

        SoftAssert soft = new SoftAssert();
        soft.assertEquals(cart.rowCount(), 1, "Le panier devrait contenir exactement 1 ligne");
        soft.assertTrue(cart.firstProductName().toLowerCase()
                        .contains(productData.get("expectedTitle").toLowerCase()),
                "Le produit du panier est inattendu : « " + cart.firstProductName() + " »");
        soft.assertEquals(cart.firstQuantity(), "1", "La quantité par défaut devrait être 1");
        soft.assertTrue(cart.isTotalsDisplayed(), "Le total du panier n'est pas affiché");
        soft.assertTrue(cart.isCheckoutButtonDisplayed(), "Le bouton de validation de commande est absent");
        soft.assertAll();
    }

    @Test(priority = 16, dependsOnMethods = "addProductToCart", groups = {"panier"},
          description = "TC16 - Supprimer le produit du panier")
    public void removeProductFromCart() throws Exception {
        cart.open();
        Assert.assertTrue(cart.rowCount() >= 1, "Le panier est déjà vide avant la suppression");

        cart.removeFirstItem();

        Assert.assertTrue(cart.isCartEmpty(), "Le panier devrait être vide après la suppression");
        Assert.assertEquals(cart.rowCount(), 0, "Il reste des lignes dans le panier");
    }
}
