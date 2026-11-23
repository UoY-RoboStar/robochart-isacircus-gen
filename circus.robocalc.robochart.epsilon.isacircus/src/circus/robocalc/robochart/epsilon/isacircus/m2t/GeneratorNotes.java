package circus.robocalc.robochart.epsilon.isacircus.m2t;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.epsilon.eol.models.IModel;

/**
 * Warnings of the model-to-model step (16 Nov 2026, Fang). RC2Z.eol puts a warning about a paragraph into the
 * optional attribute generatorNote of ActionPara (a deviation from CZT, marked in circus_metamodel.ecore); at
 * present only the name clash of functions of imported packages (GG, createFunctionDefsAsAxParas) does; the
 * exit-action check of composite states (exitTriggerConflicts) that used it first was removed on 23 Nov 2026
 * (docs/SEMANTICS_DIFFERENCES.md, entry 6). The templates print
 * the warnings as comments; the plugin and dev-tools read them here, after RC2Z, to report them. A warning does
 * not stop the generation.
 */
public final class GeneratorNotes {

	private GeneratorNotes() {
	}

	/** The warnings in the Z model z, in the order of the paragraphs; empty if there are none. */
	public static List<String> collect(IModel z) {
		List<String> notes = new ArrayList<>();
		try {
			for (Object o : z.getAllOfType("ActionPara")) {
				if (o instanceof EObject) {
					EObject e = (EObject) o;
					EStructuralFeature f = e.eClass().getEStructuralFeature("generatorNote");
					if (f != null) {
						Object v = e.eGet(f);
						if (v instanceof String && !((String) v).isEmpty()) {
							notes.add((String) v);
						}
					}
				}
			}
		} catch (Exception ignored) {
			// no ActionPara type: no warnings
		}
		return notes;
	}
}
