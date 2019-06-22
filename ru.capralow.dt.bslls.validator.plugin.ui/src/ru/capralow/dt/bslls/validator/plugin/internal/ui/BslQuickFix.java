package ru.capralow.dt.bslls.validator.plugin.internal.ui;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.xtext.ui.editor.quickfix.Fix;
import org.eclipse.xtext.ui.editor.quickfix.IssueResolutionAcceptor;
import org.eclipse.xtext.validation.Issue;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.ui.quickfix.AbstractExternalQuickfixProvider;

public class BslQuickFix extends AbstractExternalQuickfixProvider {

	@Fix("bsl-language-server")
	public void processBslLanguageServerDiagnostic(final Issue issue, final IssueResolutionAcceptor acceptor) {
		String[] issueData = issue.getData();
		for (String issueLine : issueData) {
			String[] issueList = issueLine.split("[,]"); //$NON-NLS-1$

			String issueCommand = issueList[0];
			String issueMessage = issueList[1];
			Integer issueOffset = Integer.decode(issueList[2]);
			Integer issueLength = Integer.decode(issueList[3]);
			String issueNewText = issueList[4];

			acceptor.accept(issue,
					issueCommand,
					issueMessage,
					(String) null,
					new AbstractExternalQuickfixProvider.ExternalQuickfixModification<Module>(issue,
							Module.class,
							module -> new ReplaceEdit(issueOffset, issueLength, issueNewText)));
		}
	}
}
