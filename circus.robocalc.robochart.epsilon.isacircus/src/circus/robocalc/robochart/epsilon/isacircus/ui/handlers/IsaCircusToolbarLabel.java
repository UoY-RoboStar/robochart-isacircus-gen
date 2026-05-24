package circus.robocalc.robochart.epsilon.isacircus.ui.handlers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

public class IsaCircusToolbarLabel extends WorkbenchWindowControlContribution {

    @Override
    protected org.eclipse.swt.widgets.Control createControl(Composite parent) {
        Label label = new Label(parent, SWT.NONE);
        label.setText("IsaCircus");
        return label;
    }
}