package commonLibs;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Actions de base sur les éléments (clic, saisie, survol, attentes…).
 * Évolution de la classe Interactions du TP : ajout d'attentes explicites et de gestion des
 * éléments dupliqués (le site contient plusieurs formulaires avec les mêmes id : page + pop-up).
 */
public class Interactions {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public Interactions(WebDriver driver) {
        this(driver, 15);
    }

    public Interactions(WebDriver driver, int timeoutSeconds) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }

    // ------------------------------------------------------------------ actions
    public void clickElement(WebElement element) throws Exception {
        if (element.isDisplayed() && element.isEnabled()) {
            try {
                element.click();
            } catch (org.openqa.selenium.ElementClickInterceptedException e) {
                jsClick(element);           // bandeau / pop-up qui masque l'élément
            }
        } else {
            throw new Exception("Element not clickable: " + element);
        }
    }

    public void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    public void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    public void setText(WebElement element, String text) throws Exception {
        if (element.isDisplayed()) {
            element.clear();
            element.sendKeys(text);
        } else {
            throw new Exception("Element not visible: " + element);
        }
    }

    public void clearText(WebElement element) {
        if (element.isDisplayed()) {
            element.clear();
        }
    }

    public void selectByVisibleText(WebElement element, String visibleText) {
        new Select(element).selectByVisibleText(visibleText);
    }

    public void selectByValue(WebElement element, String value) {
        new Select(element).selectByValue(value);
    }

    public void hoverOverElement(WebElement element) {
        new Actions(driver).moveToElement(element).perform();
    }

    public String getText(WebElement element) {
        return element.getText();
    }

    public boolean isElementDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ attentes
    /** Attend et retourne le premier élément VISIBLE correspondant au locator, ou null après le délai. */
    public WebElement waitFirstVisible(By locator) {
        try {
            return wait.until(d -> firstDisplayed(locator));
        } catch (TimeoutException e) {
            return null;
        }
    }

    /** Premier élément affiché parmi tous ceux qui correspondent (null s'il n'y en a pas). */
    public WebElement firstDisplayed(By locator) {
        for (WebElement e : driver.findElements(locator)) {
            try {
                if (e.isDisplayed()) {
                    return e;
                }
            } catch (StaleElementReferenceException ignored) {
                // l'élément a été rafraîchi : on l'ignore
            }
        }
        return null;
    }

    /** Vrai si au moins un élément visible correspond au locator dans le délai imparti. */
    public boolean isVisible(By locator) {
        return waitFirstVisible(locator) != null;
    }

    /** Vrai si le locator est présent dans le DOM (visible ou non), sans attendre. */
    public boolean isPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    public List<WebElement> findAll(By locator) {
        return driver.findElements(locator);
    }

    public WebElement waitClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public void waitUrlContains(String fragment) {
        wait.until(ExpectedConditions.urlContains(fragment));
    }

    public boolean urlContainsWithin(String fragment, int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(ExpectedConditions.urlContains(fragment));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public WebDriverWait getWait() {
        return wait;
    }

    /** Texte visible de toute la page, espaces normalisés (utile pour les vérifications de contenu). */
    public String pageText() {
        Object o = ((JavascriptExecutor) driver)
                .executeScript("return document.body ? document.body.innerText : '';");
        return o == null ? "" : o.toString().replaceAll("\\s+", " ").trim();
    }
}
