package tn.hollywood.tests;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import tn.hollywood.pages.ContactPage;
import tn.hollywood.pages.QuotePage;
import tn.hollywood.utils.ConfigReader;
import tn.hollywood.utils.CsvReader;

/** Module « Contact et devis » : TC19 et TC20 (données : quote_data.csv + config.properties). */
public class ContactQuoteTest extends BaseTest {

    private ContactPage contact;
    private QuotePage quote;

    @BeforeClass
    public void initPages() {
        contact = new ContactPage(driver);
        quote = new QuotePage(driver);
    }

    @Test(priority = 19, groups = {"contact"},
          description = "TC19 - Vérifier les coordonnées de la page Contactez-nous")
    public void contactPageInformation() {
        contact.open();

        SoftAssert soft = new SoftAssert();
        soft.assertTrue(contact.currentUrl().contains("contactez-nous"),
                "La page Contact n'est pas ouverte : " + contact.currentUrl());
        soft.assertTrue(contact.bodyTextContains(ConfigReader.get("expected.phone")),
                "Téléphone attendu absent : " + ConfigReader.get("expected.phone"));
        soft.assertTrue(contact.bodyTextContains(ConfigReader.get("expected.address")),
                "Adresse attendue absente : " + ConfigReader.get("expected.address"));
        soft.assertTrue(contact.bodyTextContains(ConfigReader.get("expected.email")),
                "E-mail attendu absent : " + ConfigReader.get("expected.email"));
        soft.assertTrue(contact.isMapDisplayed(), "La carte Google Maps n'est pas intégrée à la page");
        soft.assertAll();
    }

    /**
     * Ce test n'envoie JAMAIS de demande réelle : le formulaire est soumis vide, puis avec un e-mail
     * invalide ET un champ « Demande » vide, donc la validation côté site doit toujours bloquer l'envoi.
     */
    @Test(priority = 20, groups = {"contact"},
          description = "TC20 - Valider les contrôles du formulaire de devis en ligne")
    public void quoteFormValidation() throws Exception {
        quote.open();
        Assert.assertTrue(quote.isFormDisplayed(), "Le formulaire de devis n'est pas affiché");

        SoftAssert soft = new SoftAssert();

        // Scénario A : formulaire vide
        quote.clickSubmit();
        soft.assertTrue(quote.hasValidationErrors(),
                "Scénario A : aucune erreur de validation sur un formulaire vide");
        soft.assertFalse(quote.isConfirmationDisplayed(),
                "Scénario A : une confirmation d'envoi est affichée pour un formulaire vide");

        // Scénario B : e-mail invalide + message vide (données du CSV)
        Map<String, String> row = CsvReader.read("quote_data.csv").get(0);
        quote.open();
        quote.fill(row.get("name"), row.get("email"), row.get("phone"), row.get("message"));
        quote.clickSubmit();
        soft.assertTrue(quote.hasValidationErrors(),
                "Scénario B : aucune erreur de validation (e-mail invalide)");
        soft.assertTrue(quote.isEmailFieldRejected(),
                "Scénario B : le champ e-mail invalide n'est pas signalé en erreur");
        soft.assertFalse(quote.isConfirmationDisplayed(),
                "Scénario B : une confirmation d'envoi est affichée malgré les données invalides");

        soft.assertAll();
    }
}
