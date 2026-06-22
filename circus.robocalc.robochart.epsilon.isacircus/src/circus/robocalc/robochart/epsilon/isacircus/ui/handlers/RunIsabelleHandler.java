package circus.robocalc.robochart.epsilon.isacircus.ui.handlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;

public class RunIsabelleHandler extends AbstractHandler {

    private static final String ISABELLE_NAME = "isabelle";
    private static final int SERVER_PORT = 4711;
    private static final String SESSION_NAME = "RC_HOL-CSP";

    // Timeouts
    // Only ONE step gets a kill-timeout: verifying the isolated first ddlf lemma.
    // A timeout there can ONLY mean apply (deadlock_free' ...) is non-terminating,
    // because the truncated copy contains nothing else that could be slow.
    private static final long TIMEOUT = 120000;                  // 2 min: deadlock_free' apply (isolated)

    // All other steps must NOT kill the process — they always return a result eventually:
    //   - sledgehammer always returns a proof or "No proof found"
    //   - find_counterexample / nitpick / quickcheck always return (may be slow, never hang)
    // A very large value (24 hours) effectively means "no kill timeout; wait for FINISHED".
    private static final long NO_KILL_TIMEOUT = 86400000;       // 24 hours = no effective timeout
    private static final long SLEDGEHAMMER_TIMEOUT = NO_KILL_TIMEOUT;
    private static final long COUNTEREXAMPLE_TIMEOUT = NO_KILL_TIMEOUT;

    // Max sledgehammer iterations to avoid infinite loop
    private static final int MAX_SLEDGEHAMMER_ITERATIONS = 5;

    // Sledgehammer command with extended provers for better coverage
    private static final String SLEDGEHAMMER_CMD =
        "sledgehammer [provers = \"cvc5 verit z3 e spass vampire zipperposition\"]";

    // Static state for session reuse across invocations
    private static String isabellePath = null;
    private static String sessionId = null;
    private static boolean serverRunning = false;

    IFile inputFile;

    // ── Entry point ──────────────────────────────────────────────────────────
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
        if (!(selection.getFirstElement() instanceof IFile)) return null;

        inputFile = (IFile) selection.getFirstElement();
        if (!inputFile.getFileExtension().equals("thy")) return null;

        org.eclipse.core.runtime.jobs.Job job =
            new org.eclipse.core.runtime.jobs.Job("Isabelle Verification") {
                @Override
                protected org.eclipse.core.runtime.IStatus run(
                        org.eclipse.core.runtime.IProgressMonitor monitor) {
                    try {
                        runVerification(monitor);
                    } catch (Exception e) {
                        return new org.eclipse.core.runtime.Status(
                            org.eclipse.core.runtime.IStatus.ERROR,
                            "circus.robocalc.robochart.epsilon.isacircus",
                            "Isabelle verification failed: " + e.getMessage(), e);
                    }
                    return org.eclipse.core.runtime.Status.OK_STATUS;
                }
            };
        job.setUser(true);
        job.schedule();
        return null;
    }

    // ── Main verification logic ───────────────────────────────────────────────
    private void runVerification(org.eclipse.core.runtime.IProgressMonitor monitor)
            throws IOException, InterruptedException {

        MessageConsole console = getOrCreateConsole("Isabelle Verification");
        MessageConsoleStream out = console.newMessageStream();
        showConsole(console);
        console.clearConsole();

        String thyAbsPath = new File(inputFile.getLocationURI()).getAbsolutePath();
        String theoryPath = thyAbsPath.endsWith(".thy")
            ? thyAbsPath.substring(0, thyAbsPath.length() - 4) : thyAbsPath;
        String thyFileName = theoryPath.substring(theoryPath.lastIndexOf("/") + 1);

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String projectDir = new File(inputFile.getProject().getLocationURI()).getAbsolutePath();
        String logDir = projectDir + "/isabelle_log";
        new File(logDir).mkdirs();
        String logPath = logDir + "/" + thyFileName + "_" + timestamp + ".log";

        PrintWriter logWriter = null;
        try {
            logWriter = new PrintWriter(new FileWriter(logPath, StandardCharsets.UTF_8));
            DualOutput dual = new DualOutput(out, logWriter);

            if (monitor.isCanceled()) { dual.println("⛔ Cancelled."); return; }
            if (isabellePath == null) {
                dual.println("TASK 1: Detecting Isabelle path...");
                isabellePath = findIsabelleExecutable();
                dual.println("✅ Isabelle detected at: " + isabellePath);
            } else {
                dual.println("TASK 1: Isabelle path already known: " + isabellePath);
            }

            if (monitor.isCanceled()) { dual.println("⛔ Cancelled."); return; }
            if (!serverRunning) {
                dual.println("\nTASK 2: Starting Isabelle server...");
                startIsabelleServer(isabellePath, dual);
                dual.println("\nTASK 3: Checking if Isabelle server is running...");
                boolean running = checkIsabelleServer(isabellePath, dual);
                if (!running) throw new IOException("Isabelle server is not running.");
                dual.println("✅ Isabelle server is running.");
                serverRunning = true;
            } else {
                dual.println("\nTASK 2 & 3: Isabelle server already running.");
            }

            if (monitor.isCanceled()) { dual.println("⛔ Cancelled."); return; }
            if (sessionId == null) {
                dual.println("\nTASK 4: Connecting to Isabelle client...");
                startIsabelleClient(isabellePath, dual);
                dual.println("\nTASK 5: Starting " + SESSION_NAME + " session...");
                sessionId = sendCommandAndGetSessionId(
                    "{\"session\": \"" + SESSION_NAME + "\"}", "session_start", dual);
                dual.println("✅ Session started. Session ID: " + sessionId);
            } else {
                dual.println("\nTASK 4 & 5: Reusing existing session: " + sessionId);
            }

            if (monitor.isCanceled()) { dual.println("⛔ Cancelled."); return; }
            dual.println("\nTASK 6: Running Theory File...");
            runTheoryFile(theoryPath, thyAbsPath, dual, monitor);

            dual.println("\nLog saved to: " + logPath);
            try {
                inputFile.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
            } catch (Exception e) { /* ignore */ }

        } catch (IOException e) {
            if (logWriter != null) logWriter.println("ERROR: " + e.getMessage());
            sessionId = null;
            serverRunning = false;
            throw e;
        } finally {
            if (logWriter != null) logWriter.close();
            try { out.close(); } catch (IOException e) { /* ignore */ }
        }
    }

    // ── Run theory file with three-step decision logic ────────────────────────
    //
    // Step 1: Verify ONLY the first ddlf lemma, in isolation, via a truncated copy
    //         (prefix + first lemma + "end"), under a kill-timeout. Because the copy
    //         contains nothing slow except the apply, a timeout there unambiguously
    //         means apply (deadlock_free' ...) is non-terminating.
    //
    //   Outcome A (timeout)        → apply is stuck → delete apply+done, insert oops,
    //                                then run counterexample search on the full file.
    //   Outcome C (FINISHED, ok)   → proof complete → report DEADLOCK FREE, done.
    //   Outcome B (FINISHED, fail) → "Failed to finish proof" → go to sledgehammer.
    //
    // Step 2 (only after B): sledgehammer loop on the FULL file, NO kill-timeout.
    //   success → report DEADLOCK FREE (proof closed by ...).
    //   failure → keep apply, replace ONLY done with oops, run counterexample search.
    //
    // Step 3 (counterexample search): run the full file, NO kill-timeout; wait for
    //   nitpick/quickcheck to return naturally and report the counterexample.
    private void runTheoryFile(String theoryPath, String thyAbsPath,
            DualOutput dual, org.eclipse.core.runtime.IProgressMonitor monitor)
            throws IOException, InterruptedException {

        dual.println("➡ Loading: " + theoryPath + ".thy");
        dual.println("⬅ [Theory Execution] Loading dependencies, this may take a while...");

        // ── Step 1: isolate the first ddlf lemma into a truncated copy ────────
        int applyLine = findApplyDeadlockFreeLine(thyAbsPath);
        int doneLine  = findDoneLine(thyAbsPath);
        if (applyLine <= 0 || doneLine <= 0) {
            dual.println("\n⚠️  Could not locate 'apply (deadlock_free' ...)' or 'done'.");
            dual.println("   Falling back to running the whole file (no kill-timeout).");
            TheoryResult whole = executeTheory(theoryPath, NO_KILL_TIMEOUT, dual, monitor);
            if (monitor.isCanceled()) { dual.println("⛔ Cancelled."); return; }
            dual.println("\n========== VERIFICATION RESULT ==========");
            printLemmaResults(whole, dual);
            dual.println("==========================================\n");
            return;
        }

        String[] truncated = createTruncatedDdlfCopy(theoryPath, thyAbsPath, doneLine, dual);
        String ddlfTheoryPath = truncated[0];   // path WITHOUT .thy
        String ddlfAbsPath    = truncated[1];   // path WITH .thy

        TheoryResult result;
        try {
            dual.println("\n── Step 1: verifying first ddlf lemma in isolation "
                + "(kill-timeout " + (TIMEOUT / 1000) + "s) ──");
            result = executeTheory(ddlfTheoryPath, TIMEOUT, dual, monitor);
        } finally {
            // Always clean up the temporary truncated copy
            try { Files.deleteIfExists(Paths.get(ddlfAbsPath)); } catch (Exception e) { /* ignore */ }
        }

        if (monitor.isCanceled()) { dual.println("⛔ Cancelled."); return; }

        // ── Outcome A: timeout → apply (deadlock_free' ...) is non-terminating ─
        if (result.timedOut) {
            dual.println("\n⏱️ TIMEOUT after " + (TIMEOUT / 1000) + "s on the isolated ddlf lemma.");
            dual.println("   Since this copy contained only the apply, this means");
            dual.println("   apply (deadlock_free' ...) does not terminate.");
            dual.println("   → Deleting apply AND done, inserting oops, then searching for a counterexample.");

            // Re-locate on the FULL file (line numbers are from the original, unchanged file)
            int aLine = findApplyDeadlockFreeLine(thyAbsPath);
            int dLine = findDoneLine(thyAbsPath);
            if (aLine > 0 && dLine > 0) {
                removeLines(thyAbsPath, aLine, dLine);   // delete BOTH apply and done
                insertLine(thyAbsPath, aLine, "  oops");
                dual.println("   ✏️ Lines " + aLine + "-" + dLine + " replaced with 'oops'.");
            }
            runCounterexample(theoryPath, thyAbsPath, dual, monitor);
            return;
        }

        // ── Outcome C: FINISHED + ok → proof complete, DEADLOCK FREE ──────────
        if (result.ok) {
            dual.println("\n========== VERIFICATION RESULT ==========");
            dual.println("✅ DEADLOCK FREE: apply (deadlock_free' ...) closed the proof.");
            printLemmaResults(result, dual);
            dual.println("==========================================\n");
            return;
        }

        // ── Outcome B: FINISHED + "Failed to finish proof" → sledgehammer ─────
        if (result.failedFinishLine > 0) {
            dual.println("\n⚠️  apply completed but the proof is not closed "
                + "('Failed to finish proof').");
            dual.println("   → Running sledgehammer (no kill-timeout; it always returns).");

            // The done line in the FULL file (the truncated copy's numbering matches the
            // prefix, but we operate on the full file from here on).
            int fullDoneLine = findDoneLine(thyAbsPath);

            boolean resolved = runSledgehammerLoop(
                theoryPath, thyAbsPath, fullDoneLine, dual, monitor);

            if (monitor.isCanceled()) { dual.println("⛔ Cancelled."); return; }

            if (resolved) {
                // ── Step 2 success: sledgehammer closed the proof → DEADLOCK FREE ─
                dual.println("\n========== VERIFICATION RESULT ==========");
                dual.println("✅ DEADLOCK FREE: proof closed by sledgehammer.");
                dual.println("   (The ddlf lemma is now proved; see the updated 'by ...' in the file.)");
                dual.println("==========================================\n");
            } else {
                // ── Step 2 failure: keep apply, replace ONLY done with oops ──────
                dual.println("\n❌ Sledgehammer could not close the proof.");
                dual.println("   → Keeping apply, replacing ONLY 'done' with oops, "
                    + "then searching for a counterexample.");
                int dLine = findDoneLine(thyAbsPath);
                if (dLine > 0) {
                    replaceLine(thyAbsPath, dLine, "  oops");   // keep apply; replace done only
                    dual.println("   ✏️ Line " + dLine + " ('done') replaced with 'oops'.");
                }
                runCounterexample(theoryPath, thyAbsPath, dual, monitor);
            }
            return;
        }

        // ── Fallback: FINISHED, not ok, but no recognised failure marker ──────
        dual.println("\n========== VERIFICATION RESULT ==========");
        dual.println("⚠️  Verification finished but the result is inconclusive "
            + "(no proof success, no 'Failed to finish proof').");
        printLemmaResults(result, dual);
        dual.println("==========================================\n");
    }

    // ── Create a truncated copy containing only the first ddlf lemma ──────────
    // The copy keeps everything up to and including the first 'done' line, then
    // closes ALL still-open scopes with the right number of 'end' lines.
    //
    // A RoboChart .thy opens two scopes that are still open at the first lemma:
    //   theory <name> ... begin      (theory scope)
    //   locale <name> begin          (locale scope)
    // so two 'end's are needed. Rather than hard-code 2, we count how many
    // 'begin's are still unclosed at the 'done' line and emit that many 'end's,
    // which is correct regardless of how many scopes the file opens.
    //
    // Returns {pathWithoutExt, pathWithExt}.
    private String[] createTruncatedDdlfCopy(String theoryPath, String thyAbsPath,
            int doneLine, DualOutput dual) throws IOException {

        List<String> lines = Files.readAllLines(Paths.get(thyAbsPath), StandardCharsets.UTF_8);

        String origName = theoryPath.substring(theoryPath.lastIndexOf("/") + 1);
        String newName  = origName + "_ddlf_check";
        String dir      = thyAbsPath.substring(0, thyAbsPath.lastIndexOf("/") + 1);
        String newAbs   = dir + newName + ".thy";
        String newPath  = dir + newName;   // without .thy

        List<String> out = new ArrayList<>();
        int openScopes = 0;   // unclosed 'begin' count within the kept range

        // Keep prefix + the first lemma (up to and including 'done')
        for (int i = 0; i < doneLine && i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();

            // Rewrite the theory header so the theory name matches the new file name
            if (trimmed.startsWith("theory ")) {
                line = line.replaceFirst("theory\\s+\\S+", "theory " + newName);
            }

            // Track scope nesting: count 'begin' / 'end' as whole words.
            // (Handles 'theory X ... begin', a lone 'begin', and lone 'end'.)
            openScopes += countWord(trimmed, "begin");
            openScopes -= countWord(trimmed, "end");

            out.add(line);
        }

        // The 'done' line we just included does not change scope nesting.
        // Whatever scopes remain open must each be closed with one 'end'.
        if (openScopes < 1) openScopes = 1;   // safety: always close at least the theory
        out.add("");
        for (int k = 0; k < openScopes; k++) {
            out.add("end");
        }

        Files.write(Paths.get(newAbs), out, StandardCharsets.UTF_8);
        dual.println("   📄 Truncated copy created: " + newName + ".thy "
            + "(prefix + first ddlf lemma + " + openScopes + " end).");
        return new String[]{ newPath, newAbs };
    }

    // Count occurrences of a whole-word keyword (begin/end) in a line.
    // Whole-word so we don't match inside identifiers like 'beginning' or names.
    private int countWord(String line, String word) {
        int count = 0;
        java.util.regex.Matcher m =
            java.util.regex.Pattern.compile("\\b" + word + "\\b").matcher(line);
        while (m.find()) count++;
        return count;
    }

    // ── Sledgehammer loop: iterates until proof closed or exhausted ───────────
    // Returns true if proof was closed with by(...), false if gave up.
    private boolean runSledgehammerLoop(String theoryPath, String thyAbsPath,
            int doneLineNumber, DualOutput dual,
            org.eclipse.core.runtime.IProgressMonitor monitor)
            throws IOException, InterruptedException {

        int currentDoneLine = doneLineNumber;

        // Save the very original file content for restoration on failure
        List<String> veryOriginalLines = Files.readAllLines(
            Paths.get(thyAbsPath), StandardCharsets.UTF_8);

        // Working copy that accumulates apply(...) steps across iterations
        List<String> workingLines = new ArrayList<>(veryOriginalLines);

        for (int iter = 1; iter <= MAX_SLEDGEHAMMER_ITERATIONS; iter++) {
            if (monitor.isCanceled()) {
                // Restore on cancel
                Files.write(Paths.get(thyAbsPath), veryOriginalLines, StandardCharsets.UTF_8);
                return false;
            }

            dual.println("\n   [Sledgehammer iteration " + iter + "/"
                + MAX_SLEDGEHAMMER_ITERATIONS + "]");

            // Replace current target line with sledgehammer in working copy
            List<String> sledgeLines = new ArrayList<>(workingLines);
            sledgeLines.set(currentDoneLine - 1, "  " + SLEDGEHAMMER_CMD);

            // Write to original file (theory name must match filename)
            Files.write(Paths.get(thyAbsPath), sledgeLines, StandardCharsets.UTF_8);
            dual.println("   Modified file: line " + currentDoneLine + " → sledgehammer");

            TheoryResult sledgeResult;
            try {
                sledgeResult = executeTheory(theoryPath, SLEDGEHAMMER_TIMEOUT, dual, monitor);
            } finally {
                // Restore working lines after each sledgehammer run
                // (working lines reflect accumulated apply steps so far)
                Files.write(Paths.get(thyAbsPath), workingLines, StandardCharsets.UTF_8);
                dual.println("   File restored to working state.");
            }

            if (monitor.isCanceled()) {
                Files.write(Paths.get(thyAbsPath), veryOriginalLines, StandardCharsets.UTF_8);
                return false;
            }

            if (sledgeResult.timedOut) {
                Files.write(Paths.get(thyAbsPath), veryOriginalLines, StandardCharsets.UTF_8);
                dual.println("   ⏱️ Sledgehammer timed out after "
                    + (SLEDGEHAMMER_TIMEOUT / 1000) + "s.");
                return false;
            }

            String proof = sledgeResult.sledgehammerProof;
            if (proof == null) {
                Files.write(Paths.get(thyAbsPath), veryOriginalLines, StandardCharsets.UTF_8);
                dual.println("   ❌ Sledgehammer returned no proof suggestion.");
                return false;
            }

            dual.println("   💡 Sledgehammer suggests: " + proof);

            if (proof.startsWith("by ") || proof.startsWith("by(")) {
                // Proof closed! Update working lines and write final result
                workingLines.set(currentDoneLine - 1, "  " + proof);
                Files.write(Paths.get(thyAbsPath), workingLines, StandardCharsets.UTF_8);
                dual.println("   ✅ Proof closed with: " + proof);
                dual.println("   ✏️ File updated permanently.");
                return true;

            } else if (proof.startsWith("apply ") || proof.startsWith("apply(")) {
                // Sub-goal not closed — add apply(...) to working lines
                // Insert a placeholder on next line; sledgeLines will replace it with SLEDGEHAMMER_CMD
                workingLines.set(currentDoneLine - 1, "  " + proof);
                workingLines.add(currentDoneLine, "  oops (* placeholder *)");
                currentDoneLine = currentDoneLine + 1;
                dual.println("   ↩ Sub-goal not closed. Applied: " + proof);
                dual.println("   ↩ Running sledgehammer on remaining sub-goals (line "
                    + currentDoneLine + ")...");
            } else {
                Files.write(Paths.get(thyAbsPath), veryOriginalLines, StandardCharsets.UTF_8);
                dual.println("   ❓ Unexpected proof format: " + proof);
                return false;
            }
        }

        // Max iterations reached — restore original
        Files.write(Paths.get(thyAbsPath), veryOriginalLines, StandardCharsets.UTF_8);
        dual.println("   ❌ Reached max iterations (" + MAX_SLEDGEHAMMER_ITERATIONS
            + ") without closing proof.");
        return false;
    }

    // ── Run counterexample search (NO kill-timeout) ───────────────────────────
    // nitpick / quickcheck always return eventually; we wait for FINISHED.
    private void runCounterexample(String theoryPath, String thyAbsPath,
            DualOutput dual, org.eclipse.core.runtime.IProgressMonitor monitor)
            throws IOException, InterruptedException {

        if (monitor.isCanceled()) { dual.println("⛔ Cancelled."); return; }
        dual.println("\n🔍 Running counterexample search (find_counterexample / nitpick / quickcheck)...");
        dual.println("   No kill-timeout: waiting for the search to return naturally.");

        TheoryResult result = executeTheory(theoryPath, NO_KILL_TIMEOUT, dual, monitor);

        if (monitor.isCanceled()) { dual.println("⛔ Cancelled."); return; }

        dual.println("\n========== COUNTEREXAMPLE RESULT ==========");
        printLemmaResults(result, dual);
        dual.println("==========================================\n");
    }

    // ── Execute a theory file and collect results ─────────────────────────────
    private TheoryResult executeTheory(String theoryPath, long timeoutMs,
            DualOutput dual, org.eclipse.core.runtime.IProgressMonitor monitor)
            throws IOException, InterruptedException {

        TheoryResult result = new TheoryResult();
        String targetFileName = theoryPath.substring(theoryPath.lastIndexOf("/") + 1) + ".thy";

        // Parse lemma structure from file
        LinkedHashMap<String, String> lemmaStatus = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> lemmaLineNumbers = new LinkedHashMap<>();
        File thyFile = new File(theoryPath + ".thy");
        if (thyFile.exists()) {
            List<String> fileLines = Files.readAllLines(Paths.get(theoryPath + ".thy"), StandardCharsets.UTF_8);
            String lastLemma = null;
            boolean hasCounterexampleTool = false;
            for (int i = 0; i < fileLines.size(); i++) {
                String line = fileLines.get(i).trim();
                int lineNum = i + 1;
                if (line.startsWith("lemma ")) {
                    lastLemma = line.substring(6).split(":")[0].trim();
                    lemmaStatus.put(lastLemma, "unknown");
                    lemmaLineNumbers.put(lastLemma, lineNum);
                    hasCounterexampleTool = false;
                } else if (line.equals("nitpick") || line.startsWith("nitpick ")
                        || line.equals("quickcheck") || line.startsWith("quickcheck ")) {
                    hasCounterexampleTool = true;
                } else if (line.equals("oops") && lastLemma != null) {
                    lemmaStatus.put(lastLemma,
                        hasCounterexampleTool ? "counterexample_search" : "oops");
                    lastLemma = null;
                    hasCounterexampleTool = false;
                } else if (line.equals("done") || line.equals("sorry") || line.equals("qed")) {
                    lastLemma = null;
                    hasCounterexampleTool = false;
                }
            }
        }

        Process process = new ProcessBuilder("isabelle", "client", "-n", ISABELLE_NAME).start();
        try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream()));
             BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

            String command = String.format(
                "use_theories {\"session_id\": \"%s\", \"theories\": [\"%s\"]}\n",
                sessionId, theoryPath);
            writer.write(command);
            writer.flush();
            dual.println("➡ Sent: " + command.trim());

            final boolean[] timedOutFlag = {false};
            final boolean[] cancelledFlag = {false};

            // Timeout thread — destroys process after timeoutMs
            Thread timeoutThread = new Thread(() -> {
                try {
                    long start = System.currentTimeMillis();
                    while (System.currentTimeMillis() - start < timeoutMs) {
                        Thread.sleep(500);
                        if (monitor.isCanceled()) {
                            cancelledFlag[0] = true;
                            process.destroy();
                            return;
                        }
                    }
                    timedOutFlag[0] = true;
                    process.destroy();
                } catch (InterruptedException e) { /* stopped normally */ }
            });
            timeoutThread.setDaemon(true);
            timeoutThread.start();

            String response = null;
            // Track current lemma context for real-time NOTE parsing
            String currentNoteLemma = null;

            // Blocking read — readLine() blocks until data arrives or process ends
            while ((response = reader.readLine()) != null) {
                    dual.println("⬅ [Raw] " + response);

                    // ── Real-time parsing of NOTE messages ────────────────
                    if (response.startsWith("NOTE")) {
                        // Sledgehammer: "Try this: by (...)" or "Try this: apply (...)"
                        if (response.contains("Try this:")) {
                            String suggestion = extractTryThis(response);
                            if (suggestion != null && result.sledgehammerProof == null) {
                                result.sledgehammerProof = suggestion;
                                dual.println("   💡 Sledgehammer: " + suggestion);
                            }
                        }

                        // Track which lemma we are currently in via theorem messages
                        if (response.contains("\"theorem\\\\n")) {
                            try {
                                String msg = extractMessageField(response);
                                String[] parts = msg.split("\\\\n");
                                if (parts.length >= 2) {
                                    currentNoteLemma = parts[1].trim().replace(":", "");
                                }
                            } catch (Exception e) { /* ignore */ }
                        }

                        // Nitpick confirmed counterexample
                        // NOTE: "Nitpick found a potentially spurious counterexample" is
                        // intentionally NOT handled here — spurious counterexamples are
                        // unreliable and may be false positives. Only confirmed counterexamples
                        // (without "potentially spurious") are reported.
                        if (response.contains("Nitpick found a counterexample")
                                && !response.contains("potentially spurious")) {
                            String msg = extractMessageField(response);
                            String lemmaName = currentNoteLemma != null
                                ? currentNoteLemma
                                : findLemmaForLine(
                                    extractLineFromNote(response), lemmaLineNumbers, null);
                            if (lemmaName != null && !result.reportedLemmas.contains(lemmaName)) {
                                List<String> lines = new ArrayList<>();
                                lines.add("🔍 Nitpick counterexample for " + lemmaName + ":");
                                lines.add("   " + msg.replace("\\n", "\n   "));
                                result.lemmaResults.put(lemmaName, lines);
                                result.reportedLemmas.add(lemmaName);
                                dual.println("🔍 Nitpick counterexample for " + lemmaName + ":");
                                dual.println("   " + msg.replace("\\n", "\n   "));
                            }
                        }

                        // Quickcheck counterexample
                        if (response.contains("Quickcheck found a counterexample")) {
                            String msg = extractMessageField(response);
                            String lemmaName = currentNoteLemma != null
                                ? currentNoteLemma
                                : findLemmaForLine(
                                    extractLineFromNote(response), lemmaLineNumbers, null);
                            if (lemmaName != null && !result.reportedLemmas.contains(lemmaName)) {
                                List<String> lines = new ArrayList<>();
                                lines.add("🔍 Quickcheck counterexample for " + lemmaName + ":");
                                lines.add("   " + msg.replace("\\n", "\n   "));
                                result.lemmaResults.put(lemmaName, lines);
                                result.reportedLemmas.add(lemmaName);
                                dual.println("🔍 Quickcheck counterexample for " + lemmaName + ":");
                                dual.println("   " + msg.replace("\\n", "\n   "));
                            }
                        }

                        // Nitpick/Quickcheck no counterexample found
                        if (response.contains("No counterexample") 
                                || response.contains("no counterexample")) {
                            String lemmaName = currentNoteLemma;
                            if (lemmaName != null && !result.reportedLemmas.contains(lemmaName)) {
                                dual.println("   ❓ No counterexample found for: " + lemmaName);
                            }
                        }

                        // Quickcheck failed (e.g. no code equations)
                        if (response.contains("Quickcheck") && response.contains("failed")) {
                            String msg = extractMessageField(response);
                            dual.println("   ⚠️ Quickcheck failed: " + msg.replace("\\n", " "));
                        }
                    }

                    if (response.contains("FINISHED")) break;
            }

            // Stop the timeout thread
            timeoutThread.interrupt();

            if (cancelledFlag[0]) {
                dual.println("⛔ Cancelled.");
                result.timedOut = true;
                return result;
            }
            if (timedOutFlag[0]) {
                result.timedOut = true;
                return result;
            }
            if (response == null || !response.contains("FINISHED")) return result;

            result.ok = response.contains("\"ok\":true");

            // Parse "Failed to finish proof" + line number from done
            if (!result.ok && response.contains("Failed to finish proof")) {
                result.failedFinishLine = extractLineNumber(response, "Failed to finish proof");
            }

            // Parse lemma results from FINISHED node blocks
            // This catches nitpick/quickcheck results that appear in node messages
            String[] nodeBlocks = response.split("\"node_name\"");
            for (String block : nodeBlocks) {
                if (!block.contains(targetFileName)) continue;
                parseLemmaResults(block, lemmaStatus, lemmaLineNumbers,
                    result.lemmaResults, result.reportedLemmas);
                // Also directly scan for counterexamples in node messages
                if (block.contains("Nitpick found a counterexample")
                        && !block.contains("potentially spurious")) {
                    extractCounterexamplesFromBlock(block, "Nitpick",
                        lemmaLineNumbers, result, dual);
                }
                if (block.contains("Quickcheck found a counterexample")) {
                    extractCounterexamplesFromBlock(block, "Quickcheck",
                        lemmaLineNumbers, result, dual);
                }
                // Extract sledgehammer "Try this:" from node messages if not already found
                if (result.sledgehammerProof == null && block.contains("Try this:")) {
                    String[] msgParts = block.split("\"message\":\"");
                    for (String part : msgParts) {
                        if (!part.contains("Try this:")) continue;
                        // Extract just the Try this portion
                        int tryIdx = part.indexOf("Try this:");
                        if (tryIdx < 0) continue;
                        String candidate = extractTryThis(part.substring(tryIdx));
                        if (candidate != null) {
                            result.sledgehammerProof = candidate;
                            dual.println("   💡 Sledgehammer (from FINISHED): " + candidate);
                            break;
                        }
                    }
                }
                break;
            }
            result.lemmaStatus = lemmaStatus;
        }
        return result;
    }

    // ── Extract counterexamples directly from a FINISHED node block ──────────
    private void extractCounterexamplesFromBlock(String block, String tool,
            LinkedHashMap<String, Integer> lemmaLineNumbers,
            TheoryResult result, DualOutput dual) {
        try {
            // Find all message entries containing the counterexample
            String[] msgEntries = block.split("\\{\"kind\":\"writeln\"");
            for (String entry : msgEntries) {
                String keyword = tool + " found a counterexample";
                if (!entry.contains(keyword)) continue;
                if (tool.equals("Nitpick") && entry.contains("potentially spurious")) continue;

                String msg = extractMessageField("{\"kind\":\"writeln\"" + entry);
                int lineNum = extractLineFromNote("{\"kind\":\"writeln\"" + entry);

                String lemmaName = findLemmaForLine(lineNum, lemmaLineNumbers, null);
                if (lemmaName == null) continue;
                if (result.reportedLemmas.contains(lemmaName)) continue;

                String ceText = msg.replace("\\n", "\n   ").replace("\\\\", "");
                List<String> lines = new ArrayList<>();
                lines.add("🔍 " + tool + " counterexample for " + lemmaName + ":");
                lines.add("   " + ceText);
                result.lemmaResults.put(lemmaName, lines);
                result.reportedLemmas.add(lemmaName);
                dual.println("🔍 " + tool + " counterexample for " + lemmaName + ":");
                dual.println("   " + ceText);
            }
        } catch (Exception e) { /* ignore parse errors */ }
    }

    // ── Find line number of apply (deadlock_free' ...) ────────────────────────
    private int findApplyDeadlockFreeLine(String thyAbsPath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(thyAbsPath), StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("apply (deadlock_free") || 
                trimmed.startsWith("apply(deadlock_free")) {
                return i + 1;
            }
        }
        return -1;
    }

    // ── Find line number of first 'done' ─────────────────────────────────────
    private int findDoneLine(String thyAbsPath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(thyAbsPath), StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equals("done")) return i + 1;
        }
        return -1;
    }

    // ── Remove lines from startLine to endLine (inclusive, 1-based) ──────────
    private void removeLines(String filePath, int startLine, int endLine) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        // Remove from end to start to preserve line numbers
        for (int i = endLine - 1; i >= startLine - 1; i--) {
            if (i >= 0 && i < lines.size()) lines.remove(i);
        }
        Files.write(Paths.get(filePath), lines, StandardCharsets.UTF_8);
    }

    // ── Insert a line at position lineNumber (1-based), shifting rest down ────
    private void insertLine(String filePath, int lineNumber, String content) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        lines.add(lineNumber - 1, content);
        Files.write(Paths.get(filePath), lines, StandardCharsets.UTF_8);
    }

    // ── Insert a line after lineNumber (1-based) ──────────────────────────────
    private void insertLineAfter(String filePath, int lineNumber, String content)
            throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        lines.add(lineNumber, content); // lineNumber is 1-based, add() is 0-based = insert after
        Files.write(Paths.get(filePath), lines, StandardCharsets.UTF_8);
    }

    // ── Replace a specific line (1-based) ────────────────────────────────────
    private void replaceLine(String filePath, int lineNumber, String newContent)
            throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        if (lineNumber > 0 && lineNumber <= lines.size()) {
            lines.set(lineNumber - 1, newContent);
            Files.write(Paths.get(filePath), lines, StandardCharsets.UTF_8);
        }
    }

    // ── Extract "message" field from a NOTE line ──────────────────────────────
    private String extractMessageField(String response) {
        try {
            int idx = response.indexOf("\"message\":\"");
            if (idx < 0) return "";
            int start = idx + 11;
            // Find end — escaped string, walk char by char
            StringBuilder sb = new StringBuilder();
            boolean escaped = false;
            for (int i = start; i < response.length(); i++) {
                char c = response.charAt(i);
                if (escaped) {
                    sb.append(c);
                    escaped = false;
                } else if (c == '\\') {
                    sb.append(c);
                    escaped = true;
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    // ── Extract line number from a NOTE message ───────────────────────────────
    private int extractLineFromNote(String response) {
        try {
            int idx = response.indexOf("\"line\":");
            if (idx < 0) return -1;
            int end = response.indexOf(",", idx + 7);
            if (end < 0) end = response.indexOf("}", idx + 7);
            if (end < 0) return -1;
            return Integer.parseInt(response.substring(idx + 7, end).trim());
        } catch (Exception e) { return -1; }
    }

    // ── Extract line number near a keyword in FINISHED JSON ───────────────────
    private int extractLineNumber(String response, String keyword) {
        try {
            int idx = response.indexOf(keyword);
            if (idx < 0) return -1;
            int lineIdx = response.indexOf("\"line\":", idx);
            if (lineIdx < 0) return -1;
            int lineEnd = response.indexOf(",", lineIdx + 7);
            if (lineEnd < 0) lineEnd = response.indexOf("}", lineIdx + 7);
            if (lineEnd < 0) return -1;
            return Integer.parseInt(response.substring(lineIdx + 7, lineEnd).trim());
        } catch (Exception e) { return -1; }
    }

    // ── Extract "Try this: by (...)" or "Try this: apply (...)" ──────────────
    private String extractTryThis(String response) {
        try {
            int idx = response.indexOf("Try this:");
            if (idx < 0) return null;

            // Get the text after "Try this: "
            int afterTryThis = idx + 9; // length of "Try this:"
            while (afterTryThis < response.length()
                    && response.charAt(afterTryThis) == ' ') {
                afterTryThis++;
            }

            // Find by( or apply( with parentheses
            int byParenIdx = response.indexOf("by (", idx);
            int applyParenIdx = response.indexOf("apply (", idx);

            // Find by without parentheses (e.g. "by auto", "by blast")
            int bySimpleIdx = response.indexOf("by ", idx);

            // Find apply without parentheses (e.g. "apply blast")
            int applySimpleIdx = response.indexOf("apply ", idx);

            // Determine what starts first after "Try this:"
            int start = -1;
            boolean hasParens = false;
            boolean isApply = false;

            // Check by(... first
            if (byParenIdx >= 0 && (start < 0 || byParenIdx < start)) {
                start = byParenIdx;
                hasParens = true;
                isApply = false;
            }
            // Check apply(... 
            if (applyParenIdx >= 0 && (start < 0 || applyParenIdx < start)) {
                start = applyParenIdx;
                hasParens = true;
                isApply = true;
            }
            // Check by simple (only if closer than paren versions)
            if (bySimpleIdx >= 0 && (start < 0 || bySimpleIdx < start)) {
                start = bySimpleIdx;
                hasParens = false;
                isApply = false;
            }
            // Check apply simple
            if (applySimpleIdx >= 0 && (start < 0 || applySimpleIdx < start)) {
                start = applySimpleIdx;
                hasParens = false;
                isApply = true;
            }

            if (start < 0) return null;

            if (hasParens) {
                // Find matching closing paren
                int depth = 0;
                for (int i = start; i < response.length(); i++) {
                    char c = response.charAt(i);
                    if (c == '(') depth++;
                    else if (c == ')') {
                        depth--;
                        if (depth == 0) {
                            return response.substring(start, i + 1)
                                .replace("\\n", " ")
                                .replace("\\\\", "\\")
                                .trim();
                        }
                    }
                }
            } else {
                // No parens — extract until space+paren (timing info) or end of meaningful text
                // e.g. "by auto (12 ms)" → extract "by auto"
                // e.g. "apply blast (2 ms)" → extract "apply blast"
                int end = start;
                // Skip the keyword (by/apply) and tactic name
                // Find the timing "(N ms)" or end of string/quote
                int timingIdx = response.indexOf(" (", start);
                int quoteIdx = response.indexOf("\"", start);
                int newlineIdx = response.indexOf("\\n", start);

                if (timingIdx >= 0) end = timingIdx;
                if (quoteIdx >= 0 && (end < 0 || quoteIdx < end)) end = quoteIdx;
                if (newlineIdx >= 0 && (end < 0 || newlineIdx < end)) end = newlineIdx;

                if (end > start) {
                    return response.substring(start, end).replace("\\\\", "\\").trim();
                } else {
                    return response.substring(start).replace("\\\\", "\\").trim();
                }
            }
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    // ── Parse lemma results from a node block ────────────────────────────────
    private void parseLemmaResults(String block,
            LinkedHashMap<String, String> lemmaStatus,
            LinkedHashMap<String, Integer> lemmaLineNumbers,
            LinkedHashMap<String, List<String>> lemmaResults,
            Set<String> reportedLemmas) {

        String[] messages = block.split("\\{\"kind\":");
        String currentLemma = null;

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
                        lineNumber = Integer.parseInt(
                            msg.substring(lineStart + 7, lineEnd).trim());
                    } catch (NumberFormatException e) { /* ignore */ }
                }
            }

            if (message.startsWith("theorem\\n")) {
                if (currentLemma != null && !reportedLemmas.contains(currentLemma)) {
                    List<String> result = new ArrayList<>();
                    result.add("✅ PASSED: " + currentLemma);
                    lemmaResults.put(currentLemma, result);
                    reportedLemmas.add(currentLemma);
                }
                String[] msgLines = message.split("\\\\n");
                currentLemma = msgLines.length >= 2
                    ? msgLines[1].trim().replace(":", "") : "unknown";

            } else if ((message.contains("Nitpick found a counterexample")
                        && !message.contains("potentially spurious"))
                    || message.contains("Quickcheck found a counterexample")) {
                String ce = message.replace("\\n", "\n   ").replace("\\\\", "");
                String tool = message.contains("Nitpick") ? "Nitpick" : "Quickcheck";
                String targetLemma = findLemmaForLine(lineNumber, lemmaLineNumbers, currentLemma);
                if (targetLemma != null && !reportedLemmas.contains(targetLemma)) {
                    List<String> result = new ArrayList<>();
                    result.add("🔍 " + tool + " counterexample for " + targetLemma + ":");
                    result.add("   " + ce);
                    lemmaResults.put(targetLemma, result);
                    reportedLemmas.add(targetLemma);
                }
            } else if (message.contains("potentially spurious")) {
                // NOTE: Nitpick potentially spurious counterexamples are intentionally
                // ignored — they are unreliable and may be false positives.
            } else if (message.contains("No counterexample found")
                    || message.contains("no counterexample")) {
                if (currentLemma != null && !reportedLemmas.contains(currentLemma)) {
                    List<String> result = new ArrayList<>();
                    result.add("❓ No counterexample found: " + currentLemma);
                    lemmaResults.put(currentLemma, result);
                    reportedLemmas.add(currentLemma);
                }
            }
        }

        if (currentLemma != null && !reportedLemmas.contains(currentLemma)) {
            List<String> result = new ArrayList<>();
            result.add("✅ PASSED: " + currentLemma);
            lemmaResults.put(currentLemma, result);
            reportedLemmas.add(currentLemma);
        }
    }

    private String findLemmaForLine(int lineNumber,
            LinkedHashMap<String, Integer> lemmaLineNumbers, String fallback) {
        if (lineNumber < 0) return fallback;
        String closest = fallback;
        int closestLine = -1;
        for (Map.Entry<String, Integer> entry : lemmaLineNumbers.entrySet()) {
            if (entry.getValue() <= lineNumber && entry.getValue() > closestLine) {
                closestLine = entry.getValue();
                closest = entry.getKey();
            }
        }
        return closest;
    }

    // ── Print lemma results ───────────────────────────────────────────────────
    private void printLemmaResults(TheoryResult result, DualOutput dual) {
        if (result.lemmaStatus == null) return;
        for (String lemmaName : result.lemmaStatus.keySet()) {
            if (result.lemmaResults.containsKey(lemmaName)) {
                for (String line : result.lemmaResults.get(lemmaName)) {
                    dual.println(line);
                }
            } else {
                String status = result.lemmaStatus.get(lemmaName);
                if ("oops".equals(status)) {
                    dual.println("⚠️  UNPROVED (oops): " + lemmaName);
                } else if ("counterexample_search".equals(status)) {
                    dual.println("🔍 COUNTEREXAMPLE SEARCH (no result): " + lemmaName);
                }
            }
        }
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────
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
        } catch (IOException e) { /* ignore */ }
    }

    private static void stopSessionStatic() throws IOException {
        Process process = new ProcessBuilder("isabelle", "client", "-n", ISABELLE_NAME).start();
        try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream()));
             BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
            writer.write("session_stop {\"session_id\": \"" + sessionId + "\"}\n");
            writer.flush();
            String response;
            while ((response = reader.readLine()) != null) {
                if (response.contains("\"ok\":true")) break;
            }
        }
    }

    private static void stopIsabelleServerStatic() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(isabellePath, "server",
            "-n", ISABELLE_NAME, "-x");
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) { /* consume */ }
        }
    }

    // ── Find Isabelle executable ──────────────────────────────────────────────
    private String findIsabelleExecutable() throws IOException {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String command = os.contains("win") ? "where" : "which";
            Process process = new ProcessBuilder(command, "isabelle").start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String path = reader.readLine();
                if (path != null && !path.isEmpty()) return path.trim();
            }
        } catch (Exception e) { /* fall through */ }
        throw new IOException("Isabelle not found in PATH.");
    }

    // ── Start Isabelle server ─────────────────────────────────────────────────
    private void startIsabelleServer(String isabellePath, DualOutput out) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(isabellePath, "server",
            "-n", ISABELLE_NAME, "-p", String.valueOf(SERVER_PORT));
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
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

    // ── Check Isabelle server ─────────────────────────────────────────────────
    private boolean checkIsabelleServer(String isabellePath, DualOutput out) throws IOException {
        Process process = new ProcessBuilder(isabellePath, "server", "-l").start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            if (line == null) return false;
            out.println("⬅ [Isabelle Server Check] " + line);
            return line.contains(ISABELLE_NAME);
        }
    }

    // ── Start Isabelle client ─────────────────────────────────────────────────
    private void startIsabelleClient(String isabellePath, DualOutput out) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(isabellePath, "client", "-n", ISABELLE_NAME);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("OK")) {
                    out.println("✅ Isabelle client connected successfully.");
                    break;
                }
            }
        }
    }

    // ── Send session_start and get session ID ─────────────────────────────────
    private String sendCommandAndGetSessionId(String jsonPayload, String command,
            DualOutput out) throws IOException {
        Process process = new ProcessBuilder("isabelle", "client", "-n", ISABELLE_NAME).start();
        try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream()));
             BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

            String fullCommand = command + " " + jsonPayload + "\n";
            writer.write(fullCommand);
            writer.flush();
            out.println("➡ Sent: " + fullCommand.trim());

            String response;
            String sid = null;
            while ((response = reader.readLine()) != null) {
                out.println("⬅ [Session Raw] " + response);
                if (response.contains("FINISHED") && response.contains("\"session_id\"")) {
                    out.println("⬅ [Session Start] FINISHED");
                    sid = response.split("\"session_id\":\"")[1].split("\"")[0];
                    break;
                } else if (response.contains("FAILED")) {
                    out.println("⬅ [Session Start] FAILED: " + response);
                    break;
                } else if (response.contains("\"message\"")) {
                    try {
                        String message = response.split("\"message\":\"")[1].split("\"")[0];
                        out.println("⬅ [Session Start] " + message);
                    } catch (Exception e) { /* ignore */ }
                }
            }
            if (sid == null) throw new IOException("Failed to retrieve session_id.");
            return sid;
        }
    }

    // ── Result container ──────────────────────────────────────────────────────
    private static class TheoryResult {
        boolean ok = false;
        boolean timedOut = false;
        int failedFinishLine = -1;           // line number of failed 'done'
        String sledgehammerProof = null;     // "by (...)" or "apply (...)"
        LinkedHashMap<String, String> lemmaStatus = new LinkedHashMap<>();
        LinkedHashMap<String, List<String>> lemmaResults = new LinkedHashMap<>();
        Set<String> reportedLemmas = new HashSet<>();
    }

    // ── DualOutput: Console + log file ───────────────────────────────────────
    private static class DualOutput {
        private final MessageConsoleStream console;
        private final PrintWriter log;
        DualOutput(MessageConsoleStream console, PrintWriter log) {
            this.console = console;
            this.log = log;
        }
        void println(String msg) {
            try { console.println(msg); } catch (Exception e) { /* ignore */ }
            log.println(msg);
            log.flush();
        }
        void logOnly(String msg) {
            log.println(msg);
            log.flush();
        }
    }

    // ── Eclipse Console helpers ───────────────────────────────────────────────
    private MessageConsole getOrCreateConsole(String name) {
        IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
        for (IConsole c : manager.getConsoles()) {
            if (name.equals(c.getName()) && c instanceof MessageConsole)
                return (MessageConsole) c;
        }
        MessageConsole c = new MessageConsole(name, null);
        manager.addConsoles(new IConsole[]{c});
        return c;
    }

    private void showConsole(MessageConsole console) {
        ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
    }
}
