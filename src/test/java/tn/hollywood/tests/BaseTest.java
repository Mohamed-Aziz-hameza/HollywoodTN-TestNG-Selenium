package tn.hollywood.tests;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import commonLibs.CommonDriver;
import tn.hollywood.utils.ConfigReader;

/**
 * Classe mère des classes de test : ouvre un navigateur avant la première méthode de la classe
 * et le ferme après la dernière (même principe que TunisianetTest dans le TP).
 * Chaque classe de test dispose ainsi d'une session propre (cookies et panier isolés).
 */
public abstract class BaseTest {

    protected CommonDriver cmnDriver;
    protected WebDriver driver;

    @BeforeClass(alwaysRun = true)
    public void invokeBrowser() throws Exception {
        String browser = ConfigReader.get("browser", "edge");
        boolean headless = ConfigReader.getBoolean("headless", false);

        cmnDriver = new CommonDriver(browser, headless);
        cmnDriver.setPageloadTimeout(ConfigReader.getInt("page.load.timeout", 60));
        cmnDriver.setElementDetectionTimeout(ConfigReader.getInt("implicit.wait", 0));
        driver = cmnDriver.getDriver();
        cmnDriver.navigateToFirstUrl(ConfigReader.baseUrl());
    }

    @AfterClass(alwaysRun = true)
    public void closeBrowser() {
        if (cmnDriver != null) {
            cmnDriver.closeAllBrowsers();
        }
    }

    /** Utilisé par le listener pour la capture d'écran en cas d'échec. */
    public WebDriver getDriver() {
        return driver;
    }
}
