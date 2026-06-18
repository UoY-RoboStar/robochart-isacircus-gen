package circus.robocalc.robochart.epsilon.isacircus.ui.handlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;

public class RunIsabelleHandler extends AbstractHandler implements IRunnableWithProgress {

    private static final String ISABELLE_NAME = "isabelle";
    private static final int SERVER_PORT = 4711;
    private static final String SESSION_NAME = "HOL-CSP_RS";
    private static final long TIMEOUT = 120000; // 2 minutes

    // Static state for session reuse
    private static String isabellePath = null;
    private static String sessionId = null;
    private static boolean serverRunning = false;

    IFile inputFile;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);

        if (!(selection.getFirstElement() instanceof IFile)) {
            return null;
        }
        inputFile = (IFile) selection.getFirstElement();

        if (!inputFile.getFileExtension().equals("thy")) {
            return null;
        }

        org.eclipse.core.runtime.jobs.Job job = new org.eclipse.core.runtime.jobs.Job("Isabelle Verification") {
            @Override
            protected org.eclipse.core.runtime.IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
                try {
                    RunIsabelleHandler.this.run(monitor);
                } catch (InvocationTargetException e) {
                    return new org.eclipse.core.runtime.Status(
                        org.eclipse.core.runtime.IStatus.ERROR,
                        "circus.robocalc.robochart.epsilon.isacircus",
                        "Isabelle verification failed", e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return org.eclipse.core.runtime.Status.OK_STATUS;
            }
        };
        job.setUser(false);
        job.schedule();

        return null;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        monitor.beginTask("Running Isabelle verification...", 10);

        // Get Eclipse Console for output
        MessageConsole console = getOrCreateConsole("Isabelle Verification");
        MessageConsoleStream out = console.newMessageStream();
        showConsole(console);
        console.clearConsole();

        String thyAbsPath = new File(inputFile.getLocationURI()).getAbsolutePath();
        String theoryPath = thyAbsPath.endsWith(".thy")
            ? thyAbsPath.substring(0, thyAbsPath.length() - 4) : thyAbsPath;
        String thyFileName = theoryPath.substring(theoryPath.lastIndexOf("/") + 1);

        // Prepare log file
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String projectDir = new File(inputFile.getProject().getLocationURI()).getAbsolutePath();
        String logDir = projectDir + "/isabelle_log";
        new File(logDir).mkdirs();
        String logPath = logDir + "/" + thyFileName + "_" + timestamp + ".log";
        
        PrintWriter logWriter = null;

        try {
            logWriter = new PrintWriter(new FileWriter(logPath));
            DualOutput dual = new DualOutput(out, logWriter);

            // Step 1: Find Isabelle (reuse if already found)
            if (isabellePath == null) {
                dual.println("TASK 1: Detecting Isabelle path...");
                isabellePath = findIsabelleExecutable();
                dual.println("✅ Isabelle detected at: " + isabellePath);
            } else {
                dual.println("TASK 1: Isabelle path already known: " + isabellePath);
            }
            monitor.worked(1);

            // Step 2 & 3: Start and check server (reuse if already running)
            if (!serverRunning) {
                dual.println("\nTASK 2: Starting Isabelle server...");
                startIsabelleServer(isabellePath, dual);
                monitor.worked(1);

                dual.println("\nTASK 3: Checking if Isabelle server is running...");
                boolean running = checkIsabelleServer(isabellePath, dual);
                if (!running) {
                    throw new InvocationTargetException(new IOException("Isabelle server is not running."));
                }
                dual.println("✅ Isabelle server is running.");
                serverRunning = true;
            } else {
                dual.println("\nTASK 2 & 3: Isabelle server already running.");
            }
            monitor.worked(1);

            // Step 4: Connect client (reuse if session exists)
            if (sessionId == null) {
                dual.println("\nTASK 4: Connecting to Isabelle client...");
                startIsabelleClient(isabellePath, dual);
                monitor.worked(1);

                // Step 5: Start session
                dual.println("\nTASK 5: Starting " + SESSION_NAME + " session...");
                sessionId = sendCommandAndGetSessionId(
                    "{\"session\": \"" + SESSION_NAME + "\"}", "session_start", dual);
                dual.println("✅ Session started successfully. Session ID: " + sessionId);
            } else {
                dual.println("\nTASK 4 & 5: Reusing existing session: " + sessionId);
            }
            monitor.worked(2);

            // Step 6: Run theory file
            dual.println("\nTASK 6: Running Theory File...");
            runTheoryFile(sessionId, theoryPath, dual);
            monitor.worked(3);

            dual.println("Log saved to: " + logPath);

            // Refresh workspace to show log file
            try {
                inputFile.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
            } catch (Exception e) {
                // ignore
            }

        } catch (IOException e) {
            throw new InvocationTargetException(e, e.getMessage());
        } finally {
            monitor.done();
            if (logWriter != null) logWriter.close();
            try { out.close(); } catch (IOException e) { /* ignore */ }
        }
    }

    // ── Shutdown: called by Activator on Eclipse close ───────────────────────
    public static void shutdown() {
        try {
            if (sessionId != null && isabellePath != null) {
                stopSessionStatic();
                sessionId = null;
            }
            if (serverRunning && isabellePath != null) {
                stopIsabelleServerStatic();
                serverRunning = false;
            }
        } catch (IOException e) {
            // ignore errors during shutdown
        }
    }

    private static void stopSessionStatic() throws IOException {
        Process process = new ProcessBuilder("isabelle", "client", "-n", ISABELLE_NAME).start();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String stopCommand = "session_stop {\"session_id\": \"" + sessionId + "\"}\n";
            writer.write(stopCommand);
            writer.flush();
            String response;
            while ((response = reader.readLine()) != null) {
                if (response.contains("\"ok\":true")) break;
            }
        }
    }

    private static void stopIsabelleServerStatic() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(isabellePath, "server", "-n", ISABELLE_NAME, "-x");
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) { /* consume output */ }
        }
    }

    // ── Find Isabelle executable ─────────────────────────────────────────────
    private String findIsabelleExecutable() throws IOException {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String command = os.contains("win") ? "where" : "which";
            Process process = new ProcessBuilder(command, "isabelle").start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String path = reader.readLine();
                if (path != null && !path.isEmpty()) {
                    return path.trim();
                }
            }
        } catch (Exception e) {
            // auto detection failed
        }
        throw new IOException("Isabelle not found in PATH.");
    }

    // ── Start Isabelle server ────────────────────────────────────────────────
    private void startIsabelleServer(String isabellePath, DualOutput out) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(isabellePath, "server", "-n", ISABELLE_NAME,
            "-p", String.valueOf(SERVER_PORT));
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.println("⬅ [Isabelle Server] " + line);
                if (line.startsWith("server")) {
                    out.println("✅ Isabelle server started successfully.");
                    break;
                }
            }
        }
    }

    // ── Check Isabelle server ────────────────────────────────────────────────
    private boolean checkIsabelleServer(String isabellePath, DualOutput out) throws IOException {
        Process process = new ProcessBuilder(isabellePath, "server", "-l").start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            if (line == null) return false;
            out.println("⬅ [Isabelle Server Check] " + line);
            return line.contains(ISABELLE_NAME);
        }
    }

    // ── Start Isabelle client ────────────────────────────────────────────────
    private void startIsabelleClient(String isabellePath, DualOutput out) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(isabellePath, "client", "-n", ISABELLE_NAME);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("OK")) {
                    out.println("✅ Isabelle client connected successfully.");
                    break;
                }
            }
        }
    }

    // ── Send command and get session ID ──────────────────────────────────────
    private String sendCommandAndGetSessionId(String jsonPayload, String command,
            DualOutput out) throws IOException {
        Process process = new ProcessBuilder("isabelle", "client", "-n", ISABELLE_NAME).start();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            String fullCommand = command + " " + jsonPayload + "\n";
            writer.write(fullCommand);
            writer.flush();
            out.println("➡ Sent: " + fullCommand.trim());

            String sessionName = jsonPayload.split("\"session\": \"")[1].split("\"")[0];
            String response;
            String sid = null;

            while ((response = reader.readLine()) != null) {
                if (response.contains("\"verbose\":\"true\"")) continue;
                if (response.trim().matches("\\d+")) continue;
                if (response.startsWith("OK") && response.contains("\"isabelle_id\"")) continue;
                if (response.startsWith("server")) continue;
                if (response.startsWith("OK") && response.contains("\"task\"")) {
                    out.println("⬅ [Session Start] Command received. Loading session " + sessionName + "...");
                    continue;
                }
                if (response.contains("\"verbose\":\"false\"") && response.contains("\"message\"")) {
                    String message = response.split("\"message\":\"")[1].split("\"")[0];
                    out.println("⬅ [Session Start] " + message);
                    continue;
                }
                if (response.contains("FINISHED")) {
                    out.println("⬅ [Session Start] FINISHED");
                } else {
                    out.println("⬅ [Session Start] " + response);
                }
                if (response.contains("\"session_id\"")) {
                    sid = response.split("\"session_id\":\"")[1].split("\"")[0];
                    break;
                }
            }

            if (sid == null) throw new IOException("Failed to retrieve session_id.");
            return sid;
        }
    }

    // ── Run theory file ──────────────────────────────────────────────────────
    private void runTheoryFile(String sessionId, String theoryPath, DualOutput out)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder("isabelle", "client", "-n", ISABELLE_NAME).start();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            String command = String.format(
                "use_theories {\"session_id\": \"%s\", \"theories\": [\"%s\"]}\n",
                sessionId, theoryPath);
            writer.write(command);
            writer.flush();
            out.println("➡ Sent: " + command.trim());
            out.println("⬅ [Theory Execution] Loading dependencies and running proof, this may take a while...");

            String targetFileName = theoryPath.substring(theoryPath.lastIndexOf("/") + 1) + ".thy";

            // Read thy file to extract lemma names and their status
            LinkedHashMap<String, String> lemmaStatus = new LinkedHashMap<>();
            LinkedHashMap<String, Integer> lemmaLineNumbers = new LinkedHashMap<>();
            File thyFile = new File(theoryPath + ".thy");
            if (thyFile.exists()) {
                Scanner scanner = new Scanner(thyFile);
                String lastLemma = null;
                boolean hasNitpickOrQuickcheck = false;
                int lineNum = 0;
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    lineNum++;
                    if (line.startsWith("lemma ")) {
                        lastLemma = line.substring(6).split(":")[0].trim();
                        lemmaStatus.put(lastLemma, "unknown");
                        lemmaLineNumbers.put(lastLemma, lineNum);
                        hasNitpickOrQuickcheck = false;
                    } else if (line.equals("nitpick") || line.startsWith("nitpick ")
                            || line.equals("quickcheck") || line.startsWith("quickcheck ")) {
                        hasNitpickOrQuickcheck = true;
                    } else if (line.equals("oops") && lastLemma != null) {
                        lemmaStatus.put(lastLemma, hasNitpickOrQuickcheck ? "nitpick_oops" : "oops");
                        lastLemma = null;
                        hasNitpickOrQuickcheck = false;
                    } else if (line.equals("done") || line.equals("sorry") || line.equals("qed")) {
                        lastLemma = null;
                        hasNitpickOrQuickcheck = false;
                    }
                }
                scanner.close();
            }

            // Set timeout
            long startTime = System.currentTimeMillis();
            String response = null;
            while (true) {
                if (System.currentTimeMillis() - startTime > TIMEOUT) {
                    out.println("\n========== VERIFICATION RESULT ==========");
                    out.println("⏱️ TIMEOUT: Proof did not complete within " + (TIMEOUT / 1000) + " seconds.");
                    out.println("   Some lemmas may be stuck.");
                    out.println("   Consider adding 'sorry' or 'oops' to unfinished lemmas.");
                    out.println("==========================================\n");
                    process.destroy();
                    return;
                }
                if (reader.ready()) {
                    response = reader.readLine();
                    if (response == null) break;
                    out.logOnly("⬅ [Raw] " + response);
                    if (response.contains("FINISHED")) break;
                } else {
                    Thread.sleep(500);
                }
            }

            if (response == null || !response.contains("FINISHED")) return;

            String[] nodeBlocks = response.split("\"node_name\"");

            for (String block : nodeBlocks) {
                if (!block.contains(targetFileName)) continue;

                out.println("\n========== VERIFICATION RESULT ==========");

                String[] messages = block.split("\\{\"kind\":");
                String currentLemma = null;
                LinkedHashMap<String, List<String>> lemmaResults = new LinkedHashMap<>();
                Set<String> reportedLemmas = new HashSet<>();

                for (String msg : messages) {
                    if (!msg.contains("\"writeln\"")) continue;

                    int msgStart = msg.indexOf("\"message\":\"") + 11;
                    if (msgStart < 11) continue;
                    int msgEnd = msg.indexOf("\",\"pos\"", msgStart);
                    if (msgEnd < 0) continue;
                    String message = msg.substring(msgStart, msgEnd);

                    int lineStart = msg.indexOf("\"line\":", msgEnd);
                    int lineNumber = -1;
                    if (lineStart != -1) {
                        int lineEnd = msg.indexOf(",", lineStart + 7);
                        if (lineEnd > lineStart) {
                            try {
                                lineNumber = Integer.parseInt(msg.substring(lineStart + 7, lineEnd).trim());
                            } catch (NumberFormatException e) { /* ignore */ }
                        }
                    }

                    if (message.startsWith("theorem\\n")) {
                        if (currentLemma != null && !reportedLemmas.contains(currentLemma)) {
                            List<String> lines = new ArrayList<>();
                            lines.add("✅ PASSED: " + currentLemma);
                            lemmaResults.put(currentLemma, lines);
                            reportedLemmas.add(currentLemma);
                        }
                        String[] msgLines = message.split("\\\\n");
                        currentLemma = msgLines.length >= 2 ? msgLines[1].trim().replace(":", "") : "unknown";

                    } else if (message.contains("found a counterexample")) {
                        String ce = message.replace("\\n", "\n   ").replace("\\\\", "");

                        String counterexampleLemma = null;
                        if (lineNumber != -1) {
                            int closestLine = -1;
                            for (Map.Entry<String, Integer> entry : lemmaLineNumbers.entrySet()) {
                                if (entry.getValue() <= lineNumber && entry.getValue() > closestLine) {
                                    closestLine = entry.getValue();
                                    counterexampleLemma = entry.getKey();
                                }
                            }
                        }

                        String targetLemma = counterexampleLemma != null ? counterexampleLemma : currentLemma;

                        if (targetLemma != null && !reportedLemmas.contains(targetLemma)) {
                            List<String> lines = new ArrayList<>();
                            lines.add("🔍 Lemma " + targetLemma + " found a counterexample:");
                            lines.add("   " + ce);
                            lemmaResults.put(targetLemma, lines);
                            reportedLemmas.add(targetLemma);
                            if (targetLemma.equals(currentLemma)) currentLemma = null;
                        }
                    }
                }

                if (currentLemma != null && !reportedLemmas.contains(currentLemma)) {
                    List<String> lines = new ArrayList<>();
                    lines.add("✅ PASSED: " + currentLemma);
                    lemmaResults.put(currentLemma, lines);
                    reportedLemmas.add(currentLemma);
                }

                for (String lemmaName : lemmaStatus.keySet()) {
                    if (lemmaResults.containsKey(lemmaName)) {
                        for (String line : lemmaResults.get(lemmaName)) {
                            out.println(line);
                        }
                    } else {
                        String status = lemmaStatus.get(lemmaName);
                        if (status.equals("oops")) {
                            out.println("⚠️  UNPROVED (oops): " + lemmaName);
                        } else if (status.equals("nitpick_oops")) {
                            out.println("🔍 COUNTEREXAMPLE SEARCH (no result returned): " + lemmaName);
                        }
                    }
                }

                out.println("==========================================\n");
                break;
            }
        }
    }

    // ── Dual output helper: writes to both Console and log file ──────────────
    private static class DualOutput {
        private final MessageConsoleStream console;
        private final PrintWriter log;
        DualOutput(MessageConsoleStream console, PrintWriter log) {
            this.console = console;
            this.log = log;
        }
        void println(String msg) {
            try {
                console.println(msg);
            } catch (Exception e) { /* ignore */ }
            log.println(msg);
            log.flush();
        }
        // Write to log file only, not to Console
        void logOnly(String msg) {
            log.println(msg);
            log.flush();
        }
    }
    // ── Eclipse Console helpers ──────────────────────────────────────────────
    private MessageConsole getOrCreateConsole(String name) {
        IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
        for (IConsole c : manager.getConsoles()) {
            if (name.equals(c.getName()) && c instanceof MessageConsole) {
                return (MessageConsole) c;
            }
        }
        MessageConsole c = new MessageConsole(name, null);
        manager.addConsoles(new IConsole[]{c});
        return c;
    }

    private void showConsole(MessageConsole console) {
        IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
        manager.showConsoleView(console);
    }
}