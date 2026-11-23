package circus.robocalc.robochart.epsilon.isacircus.m2t;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.epsilon.egl.EglFileGeneratingTemplate;
import org.eclipse.epsilon.egl.EglFileGeneratingTemplateFactory;
import org.eclipse.epsilon.egl.EglTemplate;
import org.eclipse.epsilon.egl.exceptions.EglRuntimeException;
import org.eclipse.epsilon.egl.execute.context.IEglContext;
import org.eclipse.epsilon.egl.output.IOutputBuffer;
import org.eclipse.epsilon.egl.spec.EglTemplateSpecification;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;

/**
 * Partial output of a template that fails (16 Nov 2026, Fang; generator CLAUDE.md, "M2T errors and output").
 *
 * Before: CircusM2T.egx ran its three rules (IsaCircus, HOL-CSP, Trans core) in one EgxModule.execute(); an EGL
 * template is evaluated into an output buffer and written only when it completes, so an exception in a template
 * wrote no file for it and aborted the EGX (the rules after it did not run).
 *
 * Now each template is caught on its own: {@link Factory} is the template factory of the EGX (the plugin) and
 * creates templates whose process() catches the exception and returns the text generated so far, completed by
 * {@link #compose}; the EGX then writes it under the usual target name (same name and time stamp as a normal
 * output) and goes on with the next rule. dev-tools (GenAll) runs the templates itself and uses {@link #recover}.
 * The failures are collected so that the caller can report them (dialog and log in the plugin).
 *
 * The text written for a failed template: a comment "INCOMPLETE: ..." first; the generated text up to the
 * last top-level command that was started; that last command, which is incomplete, inside a comment; a comment
 * "GENERATOR ERROR: ..." and the command ML \<open>error "GENERATOR ERROR: ..."\<close>, so that Isabelle stops
 * there and shows the reason. Only the M2T step is concerned: when RC2Z (M2M) fails, the handler still stops
 * before the EGX.
 */
public final class PartialOutput {

	private PartialOutput() {
	}

	/** One failed template: its name, the target written, the reason and position of the exception. */
	public static final class Failure {
		public final String template;
		public final String target;
		public final String reason;
		public final String where;
		public final Throwable cause;

		Failure(String template, String target, String reason, String where, Throwable cause) {
			this.template = template;
			this.target = target;
			this.reason = reason;
			this.where = where;
			this.cause = cause;
		}

		@Override
		public String toString() {
			return template + " (" + where + "): " + reason + (target != null ? "; incomplete output " + target : "");
		}
	}

	/** Result of {@link #recover}: the text to write and the failure. */
	public static final class Recovered {
		public final String text;
		public final Failure failure;

		Recovered(String text, Failure failure) {
			this.text = text;
			this.failure = failure;
		}
	}

	/**
	 * Template factory for the EGX: as EglFileGeneratingTemplateFactory, but every template catches its own
	 * exception (see the class comment). Use new EgxModule(new PartialOutput.Factory(outputRoot)).
	 */
	public static final class Factory extends EglFileGeneratingTemplateFactory {
		private final List<Failure> failures = Collections.synchronizedList(new ArrayList<>());

		public Factory(Path outputRoot) throws EglRuntimeException {
			super(outputRoot);
		}

		/** The templates that failed, in the order of the rules. */
		public List<Failure> getFailures() {
			return new ArrayList<>(failures);
		}

		@Override
		protected EglTemplate createTemplate(EglTemplateSpecification spec) throws Exception {
			return new Template(spec, getContextForNewTemplate(), getOutputRootOrRoot(), outputRootPath, failures);
		}
	}

	/** EglFileGeneratingTemplate whose process() returns the completed partial text instead of throwing. */
	static final class Template extends EglFileGeneratingTemplate {
		private final List<Failure> failures;
		private String recovered;

		Template(EglTemplateSpecification spec, IEglContext context, java.net.URI outputRoot, String outputRootPath,
				List<Failure> failures) throws Exception {
			super(spec, context, outputRoot, outputRootPath);
			this.failures = failures;
		}

		@Override
		public String process() throws EglRuntimeException {
			try {
				return super.process();
			} catch (EglRuntimeException | RuntimeException e) {
				Recovered r = recover(getModule().getContext(), fileName(getName()), null, e);
				recovered = r.text;
				failures.add(r.failure);
				return recovered;
			}
		}

		@Override
		public File generate(String path, boolean overwrite, boolean merge) throws EglRuntimeException {
			File f = super.generate(path, overwrite, merge);
			if (recovered != null && !failures.isEmpty()) {
				// record the file written, for the report
				synchronized (failures) {
					for (int i = failures.size() - 1; i >= 0; i--) {
						Failure x = failures.get(i);
						if (x.template.equals(fileName(getName())) && x.target == null) {
							failures.set(i, new Failure(x.template, f != null ? f.getPath() : path, x.reason, x.where,
									x.cause));
							break;
						}
					}
				}
			}
			return f;
		}

		@Override
		protected String getContents() {
			return recovered != null ? recovered : super.getContents();
		}

		@Override
		protected boolean isProcessed() {
			return recovered != null || super.isProcessed();
		}
	}

	/**
	 * The text to write for a template that threw e: the content of the template's current output buffer in ctx
	 * (the text generated so far) completed by {@link #compose}. target is the file name, if known.
	 */
	public static Recovered recover(IEglContext ctx, String template, String target, Throwable e) {
		String partial = "";
		try {
			IOutputBuffer buf = ctx != null ? ctx.getOutputBuffer() : null;
			if (buf != null) {
				partial = buf.toString();
			}
		} catch (RuntimeException ignored) {
			// no buffer: write the markers alone
		}
		EolRuntimeException ee = e instanceof EolRuntimeException ? (EolRuntimeException) e
				: EolRuntimeException.findCause(e);
		String reason = ee != null && ee.getReason() != null ? ee.getReason() : String.valueOf(e.getMessage());
		String where = position(ee, template);
		Failure f = new Failure(template, target, reason, where, e);
		return new Recovered(compose(partial, template, where, reason), f);
	}

	/** The last segment of a template name (EGX gives the URI of the template). */
	static String fileName(String name) {
		if (name == null) {
			return "?";
		}
		int k = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
		return k >= 0 ? name.substring(k + 1) : name;
	}

	/** "line N" when the exception is in the template itself, "line N of <module>" when in an imported module. */
	static String position(EolRuntimeException e, String template) {
		if (e == null) {
			return "line ?";
		}
		String module = null;
		try {
			if (e.getAst() != null && e.getAst().getModule() != null && e.getAst().getModule().getSourceUri() != null) {
				module = new File(e.getAst().getModule().getSourceUri().getPath()).getName();
			}
		} catch (RuntimeException ignored) {
			// position without module
		}
		return "line " + e.getLine() + (module != null && !module.equals(template) ? " of " + module : "");
	}

	/**
	 * Top-level commands at the start of a line, as the templates print them. Proof commands are not in the list,
	 * so that a lemma whose proof was cut stays one (incomplete) command; begin is not either (it belongs to the
	 * theory or locale command before it).
	 */
	private static final Pattern COMMAND = Pattern.compile(
			"^(theory|end|chapter|section|subsection|subsubsection|paragraph|text|text_raw|definition|"
					+ "abbreviation|fixrec|fun|function|primrec|datatype|codatatype|record|type_synonym|typedecl|"
					+ "consts|locale|context|lemma|lemmas|theorem|corollary|declare|notation|no_notation|"
					+ "axiomatization|ML|setup|interpretation|global_interpretation|sublocale|instantiation|"
					+ "instance|named_theorems|method|alphabet|zstore|dataspace|chantype|enumtype|schema|"
					+ "syntax|translations|hide_const|hide_type|unbundle|bundle|term|value|thm)(?![\\w'])",
			Pattern.MULTILINE);

	/**
	 * The written text: the INCOMPLETE comment, the complete commands of partial, the last (incomplete) command
	 * inside a comment, the GENERATOR ERROR comment and an ML command that stops Isabelle with the reason.
	 */
	public static String compose(String partial, String template, String where, String reason) {
		String nl = partial.contains("\r\n") ? "\r\n" : "\n";
		String r = commentSafe(reason).replaceAll("\\s+", " ").trim();
		StringBuilder sb = new StringBuilder();
		sb.append("(* INCOMPLETE: generation failed in template ").append(template).append(" at ").append(where)
				.append(": ").append(r).append(". See the end of this file. *)").append(nl);
		int cut = lastCommandStart(partial);
		String complete = partial.substring(0, cut);
		String tail = partial.substring(cut);
		sb.append(complete);
		if (!complete.isEmpty() && !complete.endsWith("\n")) {
			sb.append(nl);
		}
		if (!tail.trim().isEmpty()) {
			sb.append(nl).append("(* INCOMPLETE COMMAND (the generation stopped inside it), commented out:").append(nl)
					.append(commentSafe(tail)).append(nl).append("*)").append(nl);
		}
		sb.append(nl).append("(* GENERATOR ERROR: ").append(r).append(", template line ")
				.append(where.substring("line ".length())).append(" *)").append(nl);
		sb.append("ML \\<open>error \"GENERATOR ERROR: ").append(mlSafe(reason)).append(" (template ").append(template)
				.append(", ").append(mlSafe(where)).append(")\"\\<close>").append(nl);
		return sb.toString();
	}

	/** Start of the last line that begins a top-level command (0 if none). */
	static int lastCommandStart(String s) {
		Matcher m = COMMAND.matcher(s);
		int last = 0;
		while (m.find()) {
			last = m.start();
		}
		return last;
	}

	/** Text inside an Isabelle comment: no comment delimiters; one line for a reason. */
	static String commentSafe(String s) {
		return s.replace("(*", "( *").replace("*)", "* )");
	}

	/** Text inside an ML string literal in a cartouche: printable ASCII, no quote, backslash or newline. */
	static String mlSafe(String s) {
		StringBuilder b = new StringBuilder();
		for (char c : s.toCharArray()) {
			if (c == '\n' || c == '\r' || c == '\t') {
				b.append(' ');
			} else if (c == '"') {
				b.append('\'');
			} else if (c == '\\') {
				b.append('/');
			} else if (c < 32 || c > 126) {
				b.append('?');
			} else {
				b.append(c);
			}
		}
		return b.toString();
	}
}
