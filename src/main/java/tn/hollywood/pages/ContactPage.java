package tn.hollywood.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Page « Contactez-nous » (/contactez-nous/). */
public class ContactPage extends BasePage {

    private final By mapFrame = By.cssSelector("iframe[src*='maps']");

    public ContactPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        open("/contactez-nous/");
    }

    public boolean isMapDisplayed() {
        return ia.isPresent(mapFrame);
    }
}
