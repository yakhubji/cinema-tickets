import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import java.io.PrintWriter;

public class TestRunner {
    public static void main(String[] args) {
        var listener = new SummaryGeneratingListener();
        LauncherDiscoveryRequest req = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectPackage(
                "uk.gov.dwp.uc.pairtest.cinema.tickets"))
            .build();
        Launcher launcher = LauncherFactory.create();
        launcher.execute(req, listener);

        TestExecutionSummary s = listener.getSummary();
        PrintWriter pw = new PrintWriter(System.err, true);
        s.printFailuresTo(pw);
        pw.println("=========================================");
        pw.println("Tests found    : " + s.getTestsFoundCount());
        pw.println("Tests started  : " + s.getTestsStartedCount());
        pw.println("Tests passed   : " + s.getTestsSucceededCount());
        pw.println("Tests failed   : " + s.getTestsFailedCount());
        pw.println("Tests skipped  : " + s.getTestsSkippedCount());
        pw.println("=========================================");
        if (s.getTestsFailedCount() > 0) System.exit(1);
    }
}
