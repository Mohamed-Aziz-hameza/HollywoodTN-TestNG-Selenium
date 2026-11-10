package tn.hollywood.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/** Capture d'écran PNG (utilisée par le listener en cas d'échec). */
public final class ScreenshotUtil {

    private ScreenshotUtil() { }

    /**
     * @return le fichier PNG créé, ou null si la capture est impossible (navigateur fermé, etc.)
     */
    public static Path capture(WebDriver driver, Path directory, String baseName) {
        if (!(driver instanceof TakesScreenshot)) {
            return null;
        }
        try {
            Files.createDirectories(directory);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path target = directory.resolve(baseName.replaceAll("[^A-Za-z0-9_.-]", "_") + "_" + stamp + ".png");
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException | RuntimeException e) {
            System.err.println("[ScreenshotUtil] Capture impossible : " + e.getMessage());
            return null;
        }
    }
}
