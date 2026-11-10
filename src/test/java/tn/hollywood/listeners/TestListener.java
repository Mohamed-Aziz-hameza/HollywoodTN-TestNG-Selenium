package tn.hollywood.listeners;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.openqa.selenium.WebDriver;
import org.testng.IConfigurationListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import tn.hollywood.tests.BaseTest;
import tn.hollywood.utils.ConfigReader;
import tn.hollywood.utils.ScreenshotUtil;

/**
 - Listener TestNG déclaré dans testng.xml. Il fournit :

 -  - livrable 8 : rapport d'exécution HTML  (test-results/rapport_execution.html)

 -  - livrable 9 : capture d'écran PNG de CHAQUE test (réussi ou en échec) dans test-results/screenshots

 -                 + trace d'exception TXT pour chaque test en échec (test-results/errors).

 */
public class TestListener implements ITestListener, ISuiteListener, IConfigurationListener {

    // ---- Informations de l'étudiant affichées dans le rapport -------------------------------
    private static final String STUDENT_NAME = "Mohamed Aziz Hameza";
    private static final String STUDENT_CLASS = "ISIL 3-1 (Jour)";
    private static final String STUDENT_SCHOOL = "Polytech-Intl";
    // -----------------------------------------------------------------------------------------

    private static final Path RESULTS_DIR = Paths.get(ConfigReader.get("results.dir", "test-results"));
    private static final Path SHOT_DIR = RESULTS_DIR.resolve("screenshots");
    private static final Path ERR_DIR = RESULTS_DIR.resolve("errors");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final class Row {
        String tcId = "";
        String className = "";
        String method = "";
        String description = "";
        String status = "";
        long durationMs;
        String startedAt = "";
        String message = "";
        String screenshot = "";
        String trace = "";
    }

    private final List<Row> rows = Collections.synchronizedList(new ArrayList<>());
    private final List<String> configFailures = Collections.synchronizedList(new ArrayList<>());
    private long suiteStart;

    // ------------------------------------------------------------------ suite
    @Override
    public void onStart(ISuite suite) {
        suiteStart = System.currentTimeMillis();
        try {
            deleteRecursively(RESULTS_DIR);              // chaque exécution repart d'un dossier propre
            Files.createDirectories(SHOT_DIR);
            Files.createDirectories(ERR_DIR);
        } catch (IOException e) {
            System.err.println("[TestListener] Impossible de préparer " + RESULTS_DIR + " : " + e.getMessage());
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        try {
            Files.createDirectories(RESULTS_DIR);
            Path report = RESULTS_DIR.resolve("rapport_execution.html");
            Files.writeString(report, buildHtml(), StandardCharsets.UTF_8);
            System.out.println("\n[Rapport] " + report.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[TestListener] Écriture du rapport impossible : " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ tests
    @Override
    public void onTestSuccess(ITestResult result) {
        Row row = toRow(result, "PASSED");
        shoot(result, row, "PASSED");                     // capture aussi pour les tests réussis
        rows.add(row);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Row row = toRow(result, "FAILED");
        String base = (row.tcId.isEmpty() ? "" : row.tcId + "_") + row.method;

        shoot(result, row, "FAILED");

        // Trace de l'exception
        WebDriver driver = driverOf(result);
        String url = "n/a";
        String pageTitle = "n/a";
        if (driver != null) {
            try {
                url = driver.getCurrentUrl();
                pageTitle = driver.getTitle();
            } catch (RuntimeException ignored) {
                // session fermée
            }
        }
        try {
            Files.createDirectories(ERR_DIR);
            Path txt = ERR_DIR.resolve(base.replaceAll("[^A-Za-z0-9_.-]", "_") + ".txt");
            StringBuilder sb = new StringBuilder();
            sb.append("Test        : ").append(row.description.isEmpty() ? row.method : row.description).append('\n');
            sb.append("Classe      : ").append(result.getTestClass().getName()).append('\n');
            sb.append("Méthode     : ").append(row.method).append('\n');
            sb.append("Date        : ").append(LocalDateTime.now().format(DT)).append('\n');
            sb.append("URL         : ").append(url).append('\n');
            sb.append("Titre page  : ").append(pageTitle).append('\n');
            sb.append("Capture     : ").append(row.screenshot.isEmpty() ? "(aucune)" : row.screenshot).append('\n');
            sb.append("------------------------------------------------------------\n");
            sb.append(stackTrace(result.getThrowable()));
            Files.writeString(txt, sb.toString(), StandardCharsets.UTF_8);
            row.trace = "errors/" + txt.getFileName();
        } catch (IOException e) {
            System.err.println("[TestListener] Écriture de la trace impossible : " + e.getMessage());
        }
        rows.add(row);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        rows.add(toRow(result, "SKIPPED"));
    }

    // --------------------------------------------------- échecs de configuration
    @Override
    public void onConfigurationFailure(ITestResult result) {
        Throwable t = result.getThrowable();
        configFailures.add(result.getMethod().getMethodName() + " (" + result.getTestClass().getName() + ") : "
                + (t == null ? "erreur inconnue" : t.getClass().getSimpleName() + " - " + formatMessage(t.getMessage())));
    }

    // ----------------------------------------------------------------- helpers
    /** Prend une capture d'écran du navigateur et enregistre son chemin relatif dans la ligne du rapport. */
    private void shoot(ITestResult result, Row row, String suffix) {
        WebDriver driver = driverOf(result);
        if (driver == null) {
            return;
        }
        String base = (row.tcId.isEmpty() ? "" : row.tcId + "_") + row.method + "_" + suffix;
        Path png = ScreenshotUtil.capture(driver, SHOT_DIR, base);
        if (png != null) {
            row.screenshot = "screenshots/" + png.getFileName();
        }
    }

    private Row toRow(ITestResult r, String status) {
        Row row = new Row();
        String desc = r.getMethod().getDescription() == null ? "" : r.getMethod().getDescription();
        row.description = desc;
        row.tcId = desc.matches("^TC\\d+.*") ? desc.split("\\s", 2)[0] : "";
        row.className = r.getTestClass().getRealClass().getSimpleName();
        row.method = r.getMethod().getMethodName();
        row.status = status;
        row.durationMs = r.getEndMillis() - r.getStartMillis();
        row.startedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(r.getStartMillis()), ZoneId.systemDefault())
                .format(DT);
        Throwable t = r.getThrowable();
        if (t != null) {
            row.message = formatMessage(t.getMessage());
            if (row.message.isEmpty()) {
                row.message = t.getClass().getName();
            }
        }
        return row;
    }

    private WebDriver driverOf(ITestResult result) {
        Object inst = result.getInstance();
        return inst instanceof BaseTest ? ((BaseTest) inst).getDriver() : null;
    }

    private static String stackTrace(Throwable t) {
        if (t == null) {
            return "(aucune exception)";
        }
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static String formatMessage(String s) {
        if (s == null) {
            return "";
        }
        String trimmed = s.trim();
        // If it's a TestNG SoftAssert failure message containing newlines, convert them to HTML breaks (<br>)
        // so all failed assertions show fully instead of getting cut off.
        String formatted = trimmed.replace("\n", "<br>");
        return formatted.length() > 1200 ? formatted.substring(0, 1200) + "…" : formatted;
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                    // fichier verrouillé : ignoré
                }
            });
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // ------------------------------------------------------------- rapport HTML
    private static final String CSS = """
        :root{--ok:#1a7f37;--ko:#c62828;--sk:#b26a00;--navy:#12263f;--bg:#f3f5f8;--card:#fff;--tx:#1f2933;--mut:#66727f;--line:#e6eaf0}
        *{box-sizing:border-box}
        body{margin:0;font-family:"Segoe UI",Roboto,Arial,sans-serif;background:var(--bg);color:var(--tx);font-size:14px}
        header{background:var(--navy);color:#fff;padding:26px 40px}
        header h1{margin:0 0 6px;font-size:24px;font-weight:600}
        header p{margin:0;color:#b8c4d6;font-size:13px}
        main{padding:28px 40px;max-width:1400px;margin:auto}
        .card{background:var(--card);border-radius:12px;padding:20px 24px;box-shadow:0 1px 4px rgba(18,38,63,.08);margin-bottom:24px}
        .card h2{font-size:16px;margin:0 0 14px;color:var(--navy)}
        .student{display:flex;flex-wrap:wrap;gap:34px;align-items:center;border-left:5px solid var(--ko)}
        .student div span{display:block;font-size:12px;color:var(--mut);text-transform:uppercase;letter-spacing:.5px}
        .student div b{font-size:16px;color:var(--navy)}
        .kpis{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:16px;margin-bottom:18px}
        .kpi{background:var(--card);border-radius:12px;padding:18px 20px;box-shadow:0 1px 4px rgba(18,38,63,.08);border-top:4px solid #8aa0b8}
        .kpi b{display:block;font-size:32px;line-height:1.1}
        .kpi span{color:var(--mut);font-size:13px}
        .kpi.ok{border-color:var(--ok)}.kpi.ok b{color:var(--ok)}
        .kpi.ko{border-color:var(--ko)}.kpi.ko b{color:var(--ko)}
        .kpi.sk{border-color:var(--sk)}.kpi.sk b{color:var(--sk)}
        .bar{height:12px;border-radius:6px;background:#e4e8ee;overflow:hidden;display:flex;margin:0 0 24px}
        .bar i{display:block;height:100%}
        table{border-collapse:collapse;width:100%}
        .env td{padding:10px 8px;border-bottom:1px solid var(--line)}
        .env tr:last-child td{border-bottom:none}
        .env td:first-child{width:220px;color:var(--mut)}
        .toolbar{display:flex;flex-wrap:wrap;gap:10px;align-items:center;margin-bottom:16px}
        .toolbar button{border:1px solid #cdd5df;background:#fff;color:var(--navy);padding:7px 16px;border-radius:20px;cursor:pointer;font-size:13px;font-weight:600}
        .toolbar button:hover{background:#eef2f7}
        .toolbar button.active{background:var(--navy);color:#fff;border-color:var(--navy)}
        .toolbar input{margin-left:auto;padding:8px 14px;border:1px solid #cdd5df;border-radius:20px;min-width:260px;font-size:13px}
        .count{color:var(--mut);font-size:13px;margin-left:6px}
        .tests th{background:#eef2f7;color:var(--navy);text-align:left;font-size:12px;text-transform:uppercase;letter-spacing:.4px;padding:11px 12px}
        .tests td{padding:12px;border-bottom:1px solid var(--line);vertical-align:top}
        .tests tr.row:hover td{background:#fafbfd}
        .tests td.id{font-weight:700;color:var(--navy);white-space:nowrap}
        .tests small{color:var(--mut)}
        .badge{display:inline-block;padding:3px 12px;border-radius:12px;color:#fff;font-size:12px;font-weight:700;letter-spacing:.3px}
        .badge.PASSED{background:var(--ok)}.badge.FAILED{background:var(--ko)}.badge.SKIPPED{background:var(--sk)}
        tr.row-FAILED td{background:#fff6f6}
        tr.row-SKIPPED td{background:#fffaf1}
        .msg{color:#7a1d1d;font-family:Consolas,monospace;font-size:12px;word-break:break-word}
        .proof{width:190px}
        img.th{width:170px;border:1px solid #cdd5df;border-radius:6px;display:block;margin-bottom:6px;cursor:zoom-in;transition:box-shadow .15s}
        img.th:hover{box-shadow:0 2px 10px rgba(0,0,0,.25)}
        a{color:#0b5cad;text-decoration:none}a:hover{text-decoration:underline}
        .warn{color:var(--ko)}
        .empty{display:none;text-align:center;color:var(--mut);padding:24px}
        footer{color:var(--mut);font-size:12px;text-align:center;padding:10px 0 30px}
        #lb{display:none;position:fixed;inset:0;background:rgba(10,20,35,.88);z-index:10;align-items:center;justify-content:center;cursor:zoom-out}
        #lb img{max-width:94%;max-height:92%;border-radius:6px;box-shadow:0 4px 30px rgba(0,0,0,.5)}
        """;

    private static final String JS = """
        (function(){
          var rows=[].slice.call(document.querySelectorAll('tr.row'));
          var btns=[].slice.call(document.querySelectorAll('.toolbar button'));
          var q=document.getElementById('q'), cnt=document.getElementById('cnt'), empty=document.getElementById('empty');
          var f='ALL';
          function apply(){
            var t=q.value.toLowerCase().trim(), n=0;
            rows.forEach(function(r){
              var ok=(f==='ALL'||r.dataset.status===f)&&(t===''||r.dataset.text.indexOf(t)>-1);
              r.style.display=ok?'':'none'; if(ok)n++;
            });
            cnt.textContent=n+' / '+rows.length+' test(s) affiché(s)';
            empty.style.display=n===0?'block':'none';
          }
          btns.forEach(function(b){b.addEventListener('click',function(){
            btns.forEach(function(x){x.classList.remove('active');}); b.classList.add('active'); f=b.dataset.f; apply();
          });});
          q.addEventListener('input',apply); apply();
          var lb=document.getElementById('lb'), li=lb.querySelector('img');
          [].slice.call(document.querySelectorAll('img.th')).forEach(function(i){
            i.addEventListener('click',function(e){e.preventDefault(); li.src=i.src; lb.style.display='flex';});
          });
          lb.addEventListener('click',function(){lb.style.display='none';});
          document.addEventListener('keydown',function(e){if(e.key==='Escape')lb.style.display='none';});
        })();
        """;

    private String buildHtml() {
        List<Row> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparing((Row r) -> r.tcId.isEmpty() ? "ZZ" + r.method : r.tcId));

        long passed = sorted.stream().filter(r -> r.status.equals("PASSED")).count();
        long failed = sorted.stream().filter(r -> r.status.equals("FAILED")).count();
        long skipped = sorted.stream().filter(r -> r.status.equals("SKIPPED")).count();
        long total = sorted.size();
        double rate = total == 0 ? 0 : passed * 100.0 / total;
        long durationSec = (System.currentTimeMillis() - suiteStart) / 1000;

        StringBuilder h = new StringBuilder(24000);
        h.append("<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>Rapport d'exécution – Hollywood.tn</title><style>").append(CSS).append("</style></head><body>");

        h.append("<header><h1>Rapport d'exécution – Tests fonctionnels Hollywood.tn</h1>")
                .append("<p>Selenium WebDriver + TestNG + Maven · généré le ").append(LocalDateTime.now().format(DT))
                .append("</p></header><main>");

        // KPI
        h.append("<div class=\"kpis\">")
                .append(kpi("", String.valueOf(total), "Tests exécutés"))
                .append(kpi("ok", String.valueOf(passed), "Passed"))
                .append(kpi("ko", String.valueOf(failed), "Failed"))
                .append(kpi("sk", String.valueOf(skipped), "Skipped"))
                .append(kpi("", String.format("%.0f %%", rate), "Taux de réussite"))
                .append(kpi("", durationSec + " s", "Durée totale"))
                .append("</div>");
        if (total > 0) {
            h.append("<div class=\"bar\" title=\"Passed / Failed / Skipped\">")
                    .append("<i style=\"width:").append(passed * 100.0 / total).append("%;background:var(--ok)\"></i>")
                    .append("<i style=\"width:").append(failed * 100.0 / total).append("%;background:var(--ko)\"></i>")
                    .append("<i style=\"width:").append(skipped * 100.0 / total).append("%;background:var(--sk)\"></i></div>");
        }

        // Étudiant (au-dessus de l'environnement d'exécution)
        h.append("<div class=\"card student\">")
                .append("<div><span>Réalisé par</span><b>").append(esc(STUDENT_NAME)).append("</b></div>")
                .append("<div><span>Classe</span><b>").append(esc(STUDENT_CLASS)).append("</b></div>")
                .append("<div><span>Établissement</span><b>").append(esc(STUDENT_SCHOOL)).append("</b></div>")
                .append("<div><span>Projet</span><b>Tests fonctionnels Selenium + TestNG</b></div></div>");

        // Environnement
        h.append("<div class=\"card\"><h2>Environnement d'exécution</h2><table class=\"env\">")
                .append(envRow("Site testé", ConfigReader.baseUrl()))
                .append(envRow("Navigateur", ConfigReader.get("browser", "edge")
                        + (ConfigReader.getBoolean("headless", false) ? " (headless)" : "")))
                .append(envRow("Système", System.getProperty("os.name") + " " + System.getProperty("os.version")))
                .append(envRow("Java", System.getProperty("java.version")))
                .append(envRow("Stratégie de chargement", ConfigReader.get("page.load.strategy", "eager")))
                .append("</table></div>");

        // Échecs de configuration
        if (!configFailures.isEmpty()) {
            h.append("<div class=\"card\"><h2 class=\"warn\">Échecs de configuration (démarrage du navigateur, etc.)</h2><ul>");
            for (String c : configFailures) {
                h.append("<li class=\"msg\">").append(esc(c)).append("</li>");
            }
            h.append("</ul><p>Les tests concernés apparaissent en « Skipped ».</p></div>");
        }

        // Détail + filtre
        h.append("<div class=\"card\"><h2>Détail des ").append(total).append(" cas de test</h2>")
                .append("<div class=\"toolbar\">")
                .append("<button class=\"active\" data-f=\"ALL\">Tous (").append(total).append(")</button>")
                .append("<button data-f=\"PASSED\">Passed (").append(passed).append(")</button>")
                .append("<button data-f=\"FAILED\">Failed (").append(failed).append(")</button>")
                .append("<button data-f=\"SKIPPED\">Skipped (").append(skipped).append(")</button>")
                .append("<span class=\"count\" id=\"cnt\"></span>")
                .append("<input id=\"q\" type=\"search\" placeholder=\"Rechercher (TC07, panier, recherche…)\">")
                .append("</div>")
                .append("<table class=\"tests\"><thead><tr>")
                .append("<th>ID</th><th>Cas de test</th><th>Classe / méthode</th><th>Statut</th><th>Durée</th>")
                .append("<th>Détails / erreur</th><th>Preuves</th></tr></thead><tbody>");
        for (Row r : sorted) {
            String label = r.description.replaceFirst("^TC\\d+\\s*-\\s*", "");
            String searchable = (r.tcId + " " + label + " " + r.className + " " + r.method + " " + r.status + " "
                    + r.message).toLowerCase();
            h.append("<tr class=\"row row-").append(r.status).append("\" data-status=\"").append(r.status)
                    .append("\" data-text=\"").append(esc(searchable)).append("\">")
                    .append("<td class=\"id\">").append(esc(r.tcId)).append("</td>")
                    .append("<td>").append(esc(label)).append("</td>")
                    .append("<td>").append(esc(r.className)).append("<br><small>").append(esc(r.method)).append("</small></td>")
                    .append("<td><span class=\"badge ").append(r.status).append("\">").append(r.status).append("</span></td>")
                    .append("<td>").append(String.format("%.1f s", r.durationMs / 1000.0)).append("</td>")
                    // Note: Using unescaped message here so that the inserted <br> tags render line breaks properly in the HTML table cell
                    .append("<td class=\"msg\">").append(r.message).append("</td><td class=\"proof\">");
            if (!r.screenshot.isEmpty()) {
                h.append("<a href=\"").append(esc(r.screenshot)).append("\" target=\"_blank\"><img class=\"th\" src=\"")
                        .append(esc(r.screenshot)).append("\" alt=\"capture ").append(esc(r.tcId)).append("\"></a>");
            }
            if (!r.trace.isEmpty()) {
                h.append("<a href=\"").append(esc(r.trace)).append("\" target=\"_blank\">Trace de l'exception (.txt)</a>");
            }
            h.append("</td></tr>");
        }
        h.append("</tbody></table><div class=\"empty\" id=\"empty\">Aucun test ne correspond au filtre.</div></div>");

        h.append("<footer>Captures d'écran : <code>test-results/screenshots</code> · ")
                .append("Traces d'exceptions : <code>test-results/errors</code> · ")
                .append(esc(STUDENT_NAME)).append(" – ").append(esc(STUDENT_CLASS)).append(" – ").append(esc(STUDENT_SCHOOL))
                .append("</footer></main>")
                .append("<div id=\"lb\"><img alt=\"agrandissement\"></div><script>").append(JS).append("</script></body></html>");
        return h.toString();
    }

    private static String kpi(String cls, String value, String label) {
        return "<div class=\"kpi " + cls + "\"><b>" + value + "</b><span>" + label + "</span></div>";
    }

    private static String envRow(String k, String v) {
        return "<tr><td>" + esc(k) + "</td><td>" + esc(v) + "</td></tr>";
    }
}