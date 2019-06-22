package ru.capralow.dt.bslls.validator.plugin.internal.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.resource.XtextResource;
import org.github._1c_syntax.bsl.languageserver.configuration.LanguageServerConfiguration;
import org.github._1c_syntax.bsl.languageserver.context.DocumentContext;
import org.github._1c_syntax.bsl.languageserver.context.ServerContext;
import org.github._1c_syntax.bsl.languageserver.diagnostics.BSLDiagnostic;
import org.github._1c_syntax.bsl.languageserver.diagnostics.FunctionShouldHaveReturnDiagnostic;
import org.github._1c_syntax.bsl.languageserver.diagnostics.ProcedureReturnsValueDiagnostic;
import org.github._1c_syntax.bsl.languageserver.diagnostics.QuickFixProvider;
import org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import org.github._1c_syntax.bsl.languageserver.providers.DiagnosticProvider;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.validation.CustomValidationMessageAcceptor;
import com._1c.g5.v8.dt.bsl.validation.IExternalBslValidator;

public class BslValidator implements IExternalBslValidator {
	private DiagnosticProvider diagnosticProvider;

	public BslValidator() {
		super();

		LanguageServerConfiguration configuration = LanguageServerConfiguration.create();

		Map<String, Either<Boolean, Map<String, Object>>> diagnostics = new HashMap<>();
		diagnostics.put(DiagnosticProvider.getDiagnosticCode(FunctionShouldHaveReturnDiagnostic.class),
				Either.forLeft(false));
		diagnostics.put(DiagnosticProvider.getDiagnosticCode(ProcedureReturnsValueDiagnostic.class),
				Either.forLeft(false));

		configuration.setDiagnostics(diagnostics);

		diagnosticProvider = new DiagnosticProvider(configuration);
	}

	@Override
	public boolean needValidation(EObject object) {
		return object instanceof Module;
	}

	@Override
	public void validate(EObject object, CustomValidationMessageAcceptor messageAcceptor) {
		Module module = (Module) object;
		ICompositeNode node = NodeModelUtils.findActualNodeFor(module);

		String objectUri = module.getUniqueName();
		String objectText = node.getText();

		Document doc = new Document(objectText);

		ServerContext bslServerContext = new ServerContext();
		bslServerContext.addDocument(objectUri, objectText);
		DocumentContext documentContext = bslServerContext.getDocument(objectUri);
		List<Diagnostic> diagnostics = diagnosticProvider.computeDiagnostics(documentContext);
		for (Diagnostic diagnostic : diagnostics) {
			Class<? extends BSLDiagnostic> bslDiagnosticClass = DiagnosticProvider.getBSLDiagnosticClass(diagnostic);
			DiagnosticType diagnosticType = DiagnosticProvider.getDiagnosticType(bslDiagnosticClass);
			org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity diagnosticSeverity = DiagnosticProvider
					.getDiagnosticSeverity(bslDiagnosticClass);

			if (diagnosticType.equals(DiagnosticType.CODE_SMELL) && diagnosticSeverity
					.equals(org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity.INFO))
				continue;

			Integer[] offsetAndLength = getOffsetAndLength(diagnostic.getRange(), doc);
			Integer offset = offsetAndLength[0];
			Integer length = offsetAndLength[1];

			EObject diagnosticObject = new EObjectAtOffsetHelper()
					.resolveContainedElementAt((XtextResource) object.eResource(), offset);
			if (diagnosticObject == null)
				diagnosticObject = object;

			String[] issueData = getIssueData(diagnostic, bslDiagnosticClass, documentContext, doc);

			if (diagnosticType.equals(DiagnosticType.ERROR) || diagnosticType.equals(DiagnosticType.VULNERABILITY))
				messageAcceptor.acceptError(diagnostic
						.getMessage(), diagnosticObject, offset, length, "bsl-language-server", issueData); //$NON-NLS-1$

			else
				messageAcceptor.acceptWarning(diagnostic
						.getMessage(), diagnosticObject, offset, length, "bsl-language-server", issueData); //$NON-NLS-1$
		}

	}

	private String[] getIssueData(Diagnostic diagnostic, Class<? extends BSLDiagnostic> bslDiagnosticClass,
			DocumentContext documentContext, Document doc) {
		String[] issueData = { "" }; //$NON-NLS-1$
		if (!QuickFixProvider.class.isAssignableFrom(bslDiagnosticClass))
			return issueData;

		QuickFixProvider diagnosticInstance = (QuickFixProvider) diagnosticProvider
				.getDiagnosticInstance(bslDiagnosticClass);
		List<CodeAction> quickFixes = diagnosticInstance.getQuickFixes(diagnostic, null, documentContext);

		issueData = new String[quickFixes.size()];
		for (CodeAction quickFix : quickFixes) {
			List<TextEdit> changes = quickFix.getEdit().getChanges().get(documentContext.getUri());
			if (changes.size() != 1)
				continue;

			TextEdit change = changes.get(0);
			Integer[] offsetAndLength = getOffsetAndLength(change.getRange(), doc);
			Integer offset = offsetAndLength[0];
			Integer length = offsetAndLength[1];

			List<String> issueLine = new ArrayList<>();
			issueLine.add(quickFix.getTitle());
			issueLine.add(diagnostic.getMessage());
			issueLine.add(offset.toString());
			issueLine.add(length.toString());
			issueLine.add(change.getNewText());

			issueData[quickFixes.indexOf(quickFix)] = String.join(",", issueLine); //$NON-NLS-1$
		}

		return issueData;

	}

	private Integer[] getOffsetAndLength(Range range, Document doc) {
		Integer offset = 0;
		Integer length = 0;
		try {
			offset = doc.getLineOffset(range.getStart().getLine()) + range.getStart().getCharacter();
			Integer endOffset = doc.getLineOffset(range.getEnd().getLine()) + range.getEnd().getCharacter();
			length = endOffset - offset + 1;

		} catch (BadLocationException e) {
			BslValidatorPlugin
					.log(BslValidatorPlugin.createErrorStatus(Messages.BslValidator_Bad_Location_Exception, e));

		}

		Integer[] result = new Integer[2];
		result[0] = offset;
		result[1] = length;

		return result;
	}

}
