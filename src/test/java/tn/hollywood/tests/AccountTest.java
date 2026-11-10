package tn.hollywood.tests;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import tn.hollywood.pages.LoginPage;
import tn.hollywood.utils.CsvReader;

/**
 * Module « Compte client » : TC17 et TC18 (données : login_data.csv).
 * Uniquement des identifiants FICTIFS : aucun vrai compte n'est utilisé.
 */
public class AccountTest extends BaseTest {

    private LoginPage login;

    @BeforeClass
    public void initPages() {
        login = new LoginPage(driver);
    }

    @Test(priority = 17, groups = {"smoke", "compte"},
          description = "TC17 - Ouvrir la page de connexion")
    public void openLoginPage() {
        login.open();

        Assert.assertTrue(login.currentUrl().contains("my-account"),
                "La page « Mon compte » n'est pas ouverte : " + login.currentUrl());
        Assert.assertTrue(login.isLoginFormDisplayed(),
                "Le formulaire de connexion (identifiant, mot de passe, bouton) n'est pas affiché");
        Assert.assertTrue(login.isLostPasswordLinkDisplayed(),
                "Le lien « Mot de passe perdu ? » n'est pas affiché");
    }

    @Test(priority = 18, groups = {"compte"},
          description = "TC18 - Tenter des connexions invalides (jeu de données CSV)")
    public void invalidLogin() throws Exception {
        SoftAssert soft = new SoftAssert();
        for (Map<String, String> row : CsvReader.read("login_data.csv")) {
            login.open();
            login.login(row.get("username"), row.get("password"));

            soft.assertFalse(login.errorText().isBlank(),
                    "Aucun message d'erreur affiché pour : " + row.get("description"));
            soft.assertFalse(login.isLoggedIn(),
                    "Connexion acceptée à tort pour : " + row.get("description"));
        }
        soft.assertAll();
    }
}
