package circus.robocalc.robochart.epsilon.isacircus.loader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * Loads a RoboChart model (.rct) with its cross-file references resolved (generator issue C).
 *
 * Shared by the plugin handler (CompileToIsaCircusHandler) and the command-line tools in dev-tools/.
 * Depends only on EMF; the caller supplies the resource sets (standalone: an XtextResourceSet from
 * RoboChartStandaloneSetup; in RoboTool: a plain ResourceSetImpl, the .rct factory being registered).
 *
 * Loading the selected file alone leaves references into other files (import P::*, uses I) as
 * unresolved proxies; events then have no name. The loader reads only the files the selected file
 * depends on:
 *   1. text scan of the source files of the project (generated folders excluded): package -> files;
 *   2. the selected file, the files of its own package, and recursively the files of the imported
 *      packages (cycles allowed); the RoboChart library is loaded by the linker on demand;
 *   3. if references remain unresolved: fall back to all source files of the selected file's folder,
 *      then to all source files of the project.
 * After resolution, every reference from the selected file into another file is checked for
 * ambiguity: the target's qualified name must be defined in only one loaded file. A duplicate is
 * reported as a warning (strict = false) or an error (strict = true).
 *
 * The contents of the selected file are then moved into a resource that is alone in its own resource
 * set (resolved references stay as object references), so that RC2Z.eol, which uses RC!X.all(), sees
 * only the selected file, as with the former single-file load.
 */
public class RctLoader {

	/** Folder names that hold generated files or settings, never model sources. */
	public static final Set<String> EXCLUDED_DIRS = new HashSet<>(Arrays.asList(
		"src-gen", "csp-gen", "hol-csp_gen", "circus_gen", "hol-csp-abstact_gen", "isabelle_log", "bin", "target"));

	public enum Mode { CLOSURE, ALL }

	public static class Result {
		/** The selected file's contents, alone in its resource set. */
		public Resource resource;
		/** How the files were chosen: "closure", "folder", "project" or "all". */
		public String level;
		public List<File> loadedFiles = new ArrayList<>();
		/** URIs of all resources of the final resource set (selected file, dependencies, library), for the log. */
		public List<String> resourceUris = new ArrayList<>();
		public int unresolved;
		public List<String> warnings = new ArrayList<>();
		public List<String> log = new ArrayList<>();
	}

	public static class LoadException extends Exception {
		private static final long serialVersionUID = 1L;
		public LoadException(String msg) { super(msg); }
	}

	private final Supplier<ResourceSet> resourceSets;
	private final boolean strict;

	public RctLoader(Supplier<ResourceSet> resourceSets, boolean strict) {
		this.resourceSets = resourceSets;
		this.strict = strict;
	}

	/**
	 * @param selected    the .rct file
	 * @param projectRoot root folder of the project (scanned for sources)
	 * @param mode        CLOSURE (dependencies, with fallback) or ALL (all source files of the project)
	 */
	public Result load(File selected, File projectRoot, Mode mode) throws IOException, LoadException {
		selected = selected.getAbsoluteFile();
		List<File> sources = new ArrayList<>();
		collectSources(projectRoot.getAbsoluteFile(), sources);
		if (!sources.contains(selected)) sources.add(selected);
		Map<String, List<File>> byPackage = new HashMap<>();
		Map<File, ScanInfo> info = new HashMap<>();
		for (File f : sources) {
			ScanInfo si = scan(f);
			info.put(f, si);
			if (si.pkg != null) byPackage.computeIfAbsent(si.pkg, k -> new ArrayList<>()).add(f);
		}

		Result res = new Result();
		LinkedHashSet<File> files = new LinkedHashSet<>();
		if (mode == Mode.ALL) {
			files.addAll(sources);
			res.level = "all";
		} else {
			files.addAll(closure(selected, info, byPackage));
			res.level = "closure";
		}
		ResourceSet rs = loadAll(files);
		Resource sel = rs.getResource(fileUri(selected), true);
		int unresolved = countUnresolved(sel);
		res.log.add("level " + res.level + ": " + files.size() + " file(s), " + unresolved + " unresolved reference(s)");

		if (mode == Mode.CLOSURE && unresolved > 0) {
			LinkedHashSet<File> folder = new LinkedHashSet<>(files);
			for (File f : sources) if (f.getParentFile().equals(selected.getParentFile())) folder.add(f);
			if (folder.size() > files.size()) {
				files = folder;
				rs = loadAll(files);
				sel = rs.getResource(fileUri(selected), true);
				unresolved = countUnresolved(sel);
				res.level = "folder";
				res.log.add("level folder: " + files.size() + " file(s), " + unresolved + " unresolved reference(s)");
			}
		}
		if (mode == Mode.CLOSURE && unresolved > 0) {
			files = new LinkedHashSet<>(sources);
			rs = loadAll(files);
			sel = rs.getResource(fileUri(selected), true);
			unresolved = countUnresolved(sel);
			res.level = "project";
			res.log.add("level project: " + files.size() + " file(s), " + unresolved + " unresolved reference(s)");
		}
		res.loadedFiles.addAll(files);
		for (Resource r : rs.getResources()) res.resourceUris.add(String.valueOf(r.getURI()));
		res.unresolved = unresolved;
		if (unresolved > 0) {
			throw new LoadException(selected.getName() + ": " + unresolved
				+ " unresolved reference(s) after loading all source files of the project:\n" + describeUnresolved(sel));
		}

		checkAmbiguity(sel, rs, res);
		if (strict && !res.warnings.isEmpty()) {
			throw new LoadException(String.join("\n", res.warnings));
		}

		ResourceSet alone = new ResourceSetImpl();
		Resource r2 = new ResourceImpl(URI.createURI("memory:/" + selected.getName()));
		alone.getResources().add(r2);
		r2.getContents().addAll(sel.getContents());
		res.resource = r2;
		return res;
	}

	// ---------------------------------------------------------------- text scan

	static class ScanInfo {
		String pkg;
		/** first segments of the import statements (the imported package or name) */
		List<String> imports = new ArrayList<>();
	}

	private static final Pattern PACKAGE = Pattern.compile("^\\s*package\\s+([A-Za-z_][A-Za-z0-9_]*)", Pattern.MULTILINE);
	private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+([A-Za-z_][A-Za-z0-9_]*)", Pattern.MULTILINE);

	static ScanInfo scan(File f) throws IOException {
		String text = new String(Files.readAllBytes(f.toPath()), "UTF-8");
		text = text.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", "");
		ScanInfo si = new ScanInfo();
		Matcher m = PACKAGE.matcher(text);
		if (m.find()) si.pkg = m.group(1);
		m = IMPORT.matcher(text);
		while (m.find()) si.imports.add(m.group(1));
		return si;
	}

	static void collectSources(File dir, List<File> out) {
		File[] children = dir.listFiles();
		if (children == null) return;
		Arrays.sort(children);
		for (File c : children) {
			if (c.isDirectory()) {
				if (c.getName().startsWith(".") || EXCLUDED_DIRS.contains(c.getName())) continue;
				collectSources(c, out);
			} else if (c.getName().endsWith(".rct")) {
				out.add(c.getAbsoluteFile());
			}
		}
	}

	static LinkedHashSet<File> closure(File selected, Map<File, ScanInfo> info, Map<String, List<File>> byPackage) {
		LinkedHashSet<File> result = new LinkedHashSet<>();
		Deque<File> todo = new ArrayDeque<>();
		todo.add(selected);
		ScanInfo own = info.get(selected);
		if (own != null && own.pkg != null) todo.addAll(byPackage.getOrDefault(own.pkg, Collections.emptyList()));
		while (!todo.isEmpty()) {
			File f = todo.poll();
			if (!result.add(f)) continue;
			ScanInfo si = info.get(f);
			if (si == null) continue;
			for (String imp : si.imports) {
				// packages of other projects or of the library (core, sequence_toolkit, ...) are not in the map
				todo.addAll(byPackage.getOrDefault(imp, Collections.emptyList()));
			}
		}
		return result;
	}

	// ---------------------------------------------------------------- loading and checks

	private ResourceSet loadAll(Collection<File> files) {
		ResourceSet rs = resourceSets.get();
		for (File f : files) rs.getResource(fileUri(f), true);
		EcoreUtil.resolveAll(rs);
		return rs;
	}

	private static URI fileUri(File f) {
		return URI.createFileURI(f.getAbsolutePath());
	}

	/** Cross-references of the resource's contents that are still proxies after resolution. */
	static List<EObject[]> unresolved(Resource r) {
		List<EObject[]> out = new ArrayList<>();
		for (Iterator<EObject> it = r.getAllContents(); it.hasNext();) {
			EObject o = it.next();
			for (EReference ref : o.eClass().getEAllReferences()) {
				if (ref.isContainment() || ref.isContainer() || ref.isDerived() || ref.isTransient()) continue;
				for (Object t : values(o, ref)) {
					if (t instanceof EObject && ((EObject) t).eIsProxy()) out.add(new EObject[] { o, (EObject) t });
				}
			}
		}
		return out;
	}

	static int countUnresolved(Resource r) {
		return unresolved(r).size();
	}

	static String describeUnresolved(Resource r) {
		StringBuilder sb = new StringBuilder();
		for (EObject[] p : unresolved(r)) {
			sb.append("  ").append(qualifiedName(p[0])).append(" -> ")
			  .append(EcoreUtil.getURI(p[1])).append('\n');
		}
		return sb.toString();
	}

	private static List<?> values(EObject o, EReference ref) {
		Object v = o.eGet(ref, true);
		if (v == null) return Collections.emptyList();
		if (v instanceof List) return (List<?>) v;
		return Collections.singletonList(v);
	}

	/** Duplicate definitions of the targets of references from the selected file into other files. */
	private static void checkAmbiguity(Resource sel, ResourceSet rs, Result res) {
		Map<String, Set<String>> defs = new HashMap<>();
		for (Resource r : rs.getResources()) {
			for (Iterator<EObject> it = r.getAllContents(); it.hasNext();) {
				EObject o = it.next();
				String qn = qualifiedName(o);
				if (qn == null) continue;
				defs.computeIfAbsent(o.eClass().getName() + " " + qn, k -> new TreeSet<>()).add(r.getURI().lastSegment());
			}
		}
		Set<String> reported = new HashSet<>();
		for (Iterator<EObject> it = sel.getAllContents(); it.hasNext();) {
			EObject o = it.next();
			for (EReference ref : o.eClass().getEAllReferences()) {
				if (ref.isContainment() || ref.isContainer() || ref.isDerived() || ref.isTransient()) continue;
				for (Object t : values(o, ref)) {
					if (!(t instanceof EObject)) continue;
					EObject target = (EObject) t;
					if (target.eResource() == null || target.eResource() == sel) continue;
					String qn = qualifiedName(target);
					if (qn == null) continue;
					String key = target.eClass().getName() + " " + qn;
					res.log.add("reference " + qualifiedName(o) + " -> " + key + " in " + target.eResource().getURI().lastSegment());
					Set<String> where = defs.get(key);
					if (where != null && where.size() > 1 && reported.add(key)) {
						res.warnings.add("ambiguous: " + key + " is defined in " + where);
					}
				}
			}
		}
	}

	/** Names of the named containers, joined by "::"; null for an unnamed object. */
	static String qualifiedName(EObject o) {
		String n = name(o);
		if (n == null) return null;
		StringBuilder sb = new StringBuilder(n);
		for (EObject c = o.eContainer(); c != null; c = c.eContainer()) {
			String cn = name(c);
			if (cn != null) sb.insert(0, cn + "::");
		}
		return sb.toString();
	}

	private static String name(EObject o) {
		EStructuralFeature f = o.eClass().getEStructuralFeature("name");
		if (!(f instanceof EAttribute)) return null;
		Object v = o.eGet(f);
		return v instanceof String && !((String) v).isEmpty() ? (String) v : null;
	}
}
