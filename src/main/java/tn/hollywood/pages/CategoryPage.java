package tn.hollywood.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Page d'une catégorie de produits (/product-category/...). */
public class CategoryPage extends ProductListPage {

    private final By heading = By.cssSelector(
            "h1.page-title, h1.woocommerce-products-header__title, h1.entry-title, h1");

    public CategoryPage(WebDriver driver) {
        super(driver);
    }

    public String headingText() {
        WebElement h = ia.waitFirstVisible(heading);
        return text(h);
    }
}
