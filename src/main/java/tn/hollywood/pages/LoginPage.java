package tn.hollywood.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Page « Mon compte » (/my-account/) – formulaire de connexion WooCommerce.
 * Le site contient aussi un formulaire de connexion dans un pop-up avec les mêmes id :
 * on utilise donc toujours le premier élément VISIBLE.
 */
public class LoginPage extends BasePage {

    private final By usernameInput = By.cssSelector("input#username, input[name='username']");
    private final By passwordInput = By.cssSelector("input#password, input[name='password']");
    private final By loginButton = By.cssSelector("button[name='login'], button.woocommerce-form-login__submit");
    private final By errorMessage = By.cssSelector(
            ".woocommerce-error, .woocommerce-notice--error, .woocommerce-NoticeGroup-checkout, .alert-danger");
    private final By lostPasswordLink = By.cssSelector("a[href*='lost-password']");
    private final By logoutLink = By.cssSelector("a[href*='customer-logout']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        open("/my-account/");
    }

    public boolean isLoginFormDisplayed() {
        return ia.isVisible(usernameInput) && ia.isVisible(passwordInput) && ia.isVisible(loginButton);
    }

    public boolean isLostPasswordLinkDisplayed() {
        return ia.isVisible(lostPasswordLink);
    }

    public void login(String user, String password) throws Exception {
        WebElement u = ia.waitFirstVisible(usernameInput);
        WebElement p = ia.waitFirstVisible(passwordInput);
        if (u == null || p == null) {
            throw new IllegalStateException("Formulaire de connexion introuvable");
        }
        ia.setText(u, user);
        ia.setText(p, password);
        WebElement btn = ia.waitFirstVisible(loginButton);
        ia.scrollIntoView(btn);
        ia.clickElement(btn);
        waitDocumentReady();
    }

    /** Texte du message d'erreur affiché, ou chaîne vide s'il n'y en a pas. */
    public String errorText() {
        WebElement e = ia.waitFirstVisible(errorMessage);
        return text(e);
    }

    public boolean isLoggedIn() {
        return ia.isPresent(logoutLink);
    }
}
