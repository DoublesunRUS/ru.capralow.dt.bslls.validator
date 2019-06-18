package ru.capralow.dt.bslls.validator.plugin.internal.ui;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.xtext.ui.editor.model.edit.IModification;
import org.eclipse.xtext.ui.editor.quickfix.Fix;
import org.eclipse.xtext.ui.editor.quickfix.IssueResolutionAcceptor;
import org.eclipse.xtext.validation.Issue;

import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.ui.quickfix.AbstractExternalQuickfixProvider;

//@SuppressWarnings({ "unchecked", "rawtypes" })
public class BslQuickFix extends AbstractExternalQuickfixProvider {

	@Fix("bsl-language-server")
	public void processBslLanguageServerDiagnostic(final Issue issue, final IssueResolutionAcceptor acceptor) {
		String[] issueData = issue.getData();
		for (String issueLine : issueData) {
			String[] issueList = issueLine.split("[,]");

			String issueCommand = issueList[1];
			String issueMessage = issueList[2];
			Integer issueOffset = Integer.decode(issueList[3]);
			Integer issueLength = Integer.decode(issueList[4]);
			String issueNewText = issueList[5];

			acceptor.accept(issue,
					issueCommand,
					issueMessage,
					(String) null,
					(IModification) new AbstractExternalQuickfixProvider.ExternalQuickfixModification(issue,
							(Class) Method.class,
							method -> {
								return new ReplaceEdit(issueOffset, issueLength, issueNewText);
							}));
		}
	}
}
