package tn.hollywood.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Formulaire « Devis en ligne » (/devis-en-ligne/) – formulaire Gravity Forms.
 * Les champs sont repérés par leur libellé (les id générés changent d'un formulaire à l'autre).
 */
public class QuotePage extends BasePage {

    private static final String UP = "ABCDEFGHIJKLMNOPQRSTUVWXYZÉÈÀ";
    private static final String LOW = "abcdefghijklmnopqrstuvwxyzéèà";

    private final By submit = By.cssSelector(
            ".gform_wrapper input[type='submit'], .gform_wrapper button[type='submit'], "
            + ".gform_button, form input[type='submit'].button");
    private final By errorBlocks = By.cssSelector(
            ".gform_validation_errors, .validation_message, .gfield_error, .validation_error, "
            + ".gfield_validation_message");
    private final By confirmation = By.cssSelector(".gform_confirmation_message, .gform_confirmation_wrapper");

    public QuotePage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        open("/devis-en-ligne/");
    }

    private By fieldByLabel(String labelPart, String tag) {
        String l = labelPart.toLowerCase();
        String cond = "contains(translate(normalize-space(.),'" + UP + "','" + LOW + "'),'" + l + "')";
        return By.xpath("//*[contains(@class,'gfield')][.//label[" + cond + "]]//" + tag
                + "[not(@type='hidden') and not(@type='file')]"
                + " | //label[" + cond + "]/following::" + tag + "[not(@type='hidden') and not(@type='file')][1]");
    }

    public boolean isFormDisplayed() {
        return ia.isVisible(fieldByLabel("e-mail", "input")) && ia.isVisible(fieldByLabel("demande", "textarea"));
    }

    public void fill(String name, String email, String phone, String message) throws Exception {
        type(fieldByLabel("nom", "input"), name);
        type(fieldByLabel("e-mail", "input"), email);
        type(fieldByLabel("léphone", "input"), phone);
        type(fieldByLabel("demande", "textarea"), message);
    }

    private void type(By locator, String value) throws Exception {
        WebElement e = ia.waitFirstVisible(locator);
        if (e == null) {
            throw new IllegalStateException("Champ introuvable : " + locator);
        }
        ia.scrollIntoView(e);
        ia.setText(e, value == null ? "" : value);
    }

    public void clickSubmit() throws Exception {
        WebElement b = ia.waitFirstVisible(submit);
        if (b == null) {
            b = ia.waitFirstVisible(By.xpath("//input[@type='submit'] | //button[normalize-space()='Envoyer']"));
        }
        if (b == null) {
            throw new IllegalStateException("Bouton « Envoyer » introuvable");
        }
        ia.scrollIntoView(b);
        ia.clickElement(b);
        waitDocumentReady();
    }

    /** Vrai si un message de validation est visible OU si un champ obligatoire est invalide (HTML5). */
    public boolean hasValidationErrors() {
        try {
            wait.until(d -> ia.firstDisplayed(errorBlocks) != null || hasInvalidHtml5Field());
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    private boolean hasInvalidHtml5Field() {
        Object r = ((JavascriptExecutor) driver).executeScript(
                "var f=document.querySelectorAll('form input, form textarea');"
                + "for(var i=0;i<f.length;i++){var e=f[i];"
                + "if(e.type!=='hidden' && e.offsetParent!==null && e.checkValidity && !e.checkValidity()) return true;}"
                + "return false;");
        return Boolean.TRUE.equals(r);
    }

    /** Vrai si le champ e-mail est signalé en erreur (Gravity Forms ou validation HTML5). */
    public boolean isEmailFieldRejected() {
        By gfError = By.xpath("//*[contains(@class,'gfield_error')][.//label[contains("
                + "translate(normalize-space(.),'" + UP + "','" + LOW + "'),'mail')]]");
        if (ia.firstDisplayed(gfError) != null) {
            return true;
        }
        WebElement email = ia.firstDisplayed(fieldByLabel("e-mail", "input"));
        if (email == null) {
            return false;
        }
        Object r = ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].checkValidity ? !arguments[0].checkValidity() : false;", email);
        return Boolean.TRUE.equals(r);
    }

    public boolean isConfirmationDisplayed() {
        return ia.firstDisplayed(confirmation) != null;
    }
}
