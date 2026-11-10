package commonLibs;

import java.io.File;
import java.time.Duration;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.GeckoDriverService;

import tn.hollywood.utils.ConfigReader;

/**
 * Classe utilitaire qui démarre / pilote / ferme le navigateur (même rôle que dans le TP Tunisianet).
 *
 * Résolution du driver, dans cet ordre :
 *   1. propriété système  -Dwebdriver.chrome.driver=...  (ou edge / gecko)
 *   2. exécutable présent dans le dossier  drivers/  du projet (chromedriver.exe, msedgedriver.exe, geckodriver.exe)
 *   3. Selenium Manager (téléchargement automatique du bon driver, connexion internet requise)
 * Si le driver local est incompatible avec la version du navigateur, on bascule automatiquement sur Selenium Manager.
 */
public class CommonDriver {

    private WebDriver driver;
    private int pageloadTimeout;
    private int elementDetectionTimeout;
    private final String currentWorkingDirectory = System.getProperty("user.dir");

    public CommonDriver(String browserType) throws Exception {
        this(browserType, false);
    }

    public CommonDriver(String browserType, boolean headless) throws Exception {
        browserType = browserType.trim();

        switch (browserType.toLowerCase()) {
            case "chrome":
                driver = createChrome(headless);
                break;

            case "firefox":
                driver = createFirefox(headless);
                break;

            case "edge":
                driver = createEdge(headless);
                break;

            default:
                throw new Exception("Invalid browser type: " + browserType);
        }

        try {
            if (!headless) {
                driver.manage().window().maximize();
            }
        } catch (WebDriverException ignored) {
            // maximisation impossible (environnement sans écran) : on continue
        }
    }

    // ------------------------------------------------------------------ Chrome
    private WebDriver createChrome(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(loadStrategy());
        options.addArguments("--window-size=1920,1080", "--lang=fr-FR", "--disable-notifications",
                "--disable-popup-blocking", "--remote-allow-origins=*");
        if (headless) {
            options.addArguments("--headless=new");
        }
        for (String a : extraArgs()) {
            options.addArguments(a);
        }
        String binary = ConfigReader.get("browser.binary");
        if (binary != null && !binary.isEmpty()) {
            options.setBinary(binary);
        }

        File exe = findDriver("webdriver.chrome.driver", "chromedriver");
        if (exe != null) {
            try {
                return new ChromeDriver(new ChromeDriverService.Builder().usingDriverExecutable(exe).build(), options);
            } catch (WebDriverException e) {
                warn("chromedriver local inutilisable (" + firstLine(e) + ") -> Selenium Manager");
            }
        }
        return new ChromeDriver(options);
    }

    // -------------------------------------------------------------------- Edge
    private WebDriver createEdge(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        options.setPageLoadStrategy(loadStrategy());
        options.addArguments("--window-size=1920,1080", "--lang=fr-FR", "--disable-notifications",
                "--remote-allow-origins=*");
        if (headless) {
            options.addArguments("--headless=new");
        }
        for (String a : extraArgs()) {
            options.addArguments(a);
        }
        String binary = ConfigReader.get("browser.binary");
        if (binary != null && !binary.isEmpty()) {
            options.setBinary(binary);
        }

        File exe = findDriver("webdriver.edge.driver", "msedgedriver");
        if (exe != null) {
            try {
                return new EdgeDriver(new EdgeDriverService.Builder().usingDriverExecutable(exe).build(), options);
            } catch (WebDriverException e) {
                warn("msedgedriver local inutilisable (" + firstLine(e) + ") -> Selenium Manager");
            }
        }
        return new EdgeDriver(options);
    }

    // ----------------------------------------------------------------- Firefox
    private WebDriver createFirefox(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        options.setPageLoadStrategy(loadStrategy());
        options.addPreference("intl.accept_languages", "fr-FR, fr");
        options.addPreference("dom.webnotifications.enabled", false);
        if (headless) {
            options.addArguments("-headless");
        }
        options.addArguments("-width=1920", "-height=1080");
        String binary = ConfigReader.get("browser.binary");
        if (binary != null && !binary.isEmpty()) {
            options.setBinary(binary);
        }

        File exe = findDriver("webdriver.gecko.driver", "geckodriver");
        if (exe != null) {
            try {
                return new FirefoxDriver(new GeckoDriverService.Builder().usingDriverExecutable(exe).build(), options);
            } catch (WebDriverException e) {
                warn("geckodriver local inutilisable (" + firstLine(e) + ") -> Selenium Manager");
            }
        }
        return new FirefoxDriver(options);
    }

    // ----------------------------------------------------------------- helpers
    /** Retourne l'exécutable du driver s'il existe (propriété système, sinon dossier drivers/). */
    private File findDriver(String systemProperty, String baseName) {
        String custom = System.getProperty(systemProperty);
        if (custom != null && new File(custom).isFile()) {
            return new File(custom);
        }
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        File local = new File(currentWorkingDirectory + "/drivers/" + baseName + (windows ? ".exe" : ""));
        return local.isFile() ? local : null;
    }

    private PageLoadStrategy loadStrategy() {
        String s = ConfigReader.get("page.load.strategy", "eager").toLowerCase();
        switch (s) {
            case "normal": return PageLoadStrategy.NORMAL;
            case "none":   return PageLoadStrategy.NONE;
            default:       return PageLoadStrategy.EAGER;
        }
    }

    /** Arguments supplémentaires : -Dbrowser.args=--no-sandbox,--disable-dev-shm-usage */
    private String[] extraArgs() {
        String raw = ConfigReader.get("browser.args", "");
        return raw.isBlank() ? new String[0] : raw.split("\\s*,\\s*");
    }

    private static void warn(String msg) {
        System.err.println("[CommonDriver] " + msg);
    }

    private static String firstLine(Exception e) {
        String m = e.getMessage();
        return m == null ? e.getClass().getSimpleName() : m.split("\n")[0];
    }

    // ------------------------------------------------------------- API publique
    public void navigateToFirstUrl(String url) {
        driver.get(url);
    }

    public WebDriver getDriver() {
        return driver;
    }

    public void setPageloadTimeout(long pageloadTimeoutInSeconds) {
        this.pageloadTimeout = (int) pageloadTimeoutInSeconds;
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageloadTimeout));
    }

    public void setElementDetectionTimeout(long elementDetectionTimeoutInSeconds) {
        this.elementDetectionTimeout = (int) elementDetectionTimeoutInSeconds;
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(elementDetectionTimeout));
    }

    public void closeAllBrowsers() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (WebDriverException ignored) {
                // navigateur déjà fermé
            }
        }
    }
}
