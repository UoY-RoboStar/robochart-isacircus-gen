package circus.robocalc.robochart.epsilon.isacircus.ui.handlers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;

import circus.robocalc.robochart.epsilon.isacircus.Activator;

/**
 * Log of the IsaCircus compilation, independent of the console (added 5 Nov 2026, diagnosis of the RoboTool
 * test of issue C). Every message goes to the Eclipse error log (Error Log view, .metadata/.log) and is appended
 * to ~/.isacircus/compile.log, with a time stamp; exceptions with their full stack trace. Logging never throws.
 */
public final class CompileLog {

	public static final File LOG_FILE = new File(System.getProperty("user.home"), ".isacircus/compile.log");

	private CompileLog() {
	}

	public static void info(String msg) {
		log(IStatus.INFO, msg, null);
	}

	public static void warning(String msg) {
		log(IStatus.WARNING, msg, null);
	}

	public static void error(String msg, Throwable t) {
		log(IStatus.ERROR, msg, t);
	}

	private static void log(int severity, String msg, Throwable t) {
		try {
			Activator a = Activator.getDefault();
			if (a != null) {
				a.getLog().log(new Status(severity, Activator.PLUGIN_ID, "[IsaCircus] " + msg, t));
			}
		} catch (RuntimeException ignored) {
			// the file below still gets the message
		}
		try {
			LOG_FILE.getParentFile().mkdirs();
			try (PrintWriter w = new PrintWriter(new FileWriter(LOG_FILE, true))) {
				String level = severity == IStatus.ERROR ? "ERROR" : severity == IStatus.WARNING ? "WARNING" : "INFO";
				w.println(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " " + level + " "
					+ msg.replace("\n", "\n    "));
				if (t != null) {
					if (t instanceof EolRuntimeException) {
						w.println("  Epsilon trace: " + t.toString().replace("\n", "\n    "));
					}
					StringWriter sw = new StringWriter();
					t.printStackTrace(new PrintWriter(sw));
					w.println("  " + sw.toString().replace("\n", "\n  "));
				}
			}
		} catch (IOException | RuntimeException ignored) {
			// logging must not break the compilation
		}
	}

	/** Reason and position of an exception, for the dialog: for Epsilon errors module, line and column. */
	public static String describe(Throwable t) {
		EolRuntimeException e = t instanceof EolRuntimeException ? (EolRuntimeException) t : EolRuntimeException.findCause(t);
		if (e == null) {
			return t.getClass().getName() + ": " + t.getMessage();
		}
		StringBuilder sb = new StringBuilder();
		sb.append(e.getReason() != null ? e.getReason() : String.valueOf(e.getMessage()));
		String where = null;
		try {
			if (e.getAst() != null && e.getAst().getModule() != null && e.getAst().getModule().getSourceUri() != null) {
				where = new File(e.getAst().getModule().getSourceUri().getPath()).getName();
			}
		} catch (RuntimeException ignored) {
			// position without module name
		}
		sb.append("\n\nat ").append(where != null ? where : "?").append(", line ").append(e.getLine())
		  .append(", column ").append(e.getColumn());
		if (e.getCause() != null && e.getCause() != e) {
			sb.append("\ncaused by: ").append(e.getCause());
		}
		sb.append("\n\nFull stack trace: Error Log view, and ").append(LOG_FILE.getPath());
		return sb.toString();
	}
}
