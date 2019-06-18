package ru.capralow.dt.bslls.validator.plugin.internal.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
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

			Integer offset = 0;
			Integer length = 0;
			try {
				offset = doc.getLineOffset(diagnostic.getRange().getStart().getLine())
						+ diagnostic.getRange().getStart().getCharacter();
				Integer endOffset = doc.getLineOffset(diagnostic.getRange().getEnd().getLine())
						+ diagnostic.getRange().getEnd().getCharacter();
				length = endOffset - offset;

			} catch (BadLocationException e) {
				String msg = "Не удалось определить объект, к которому относится диагностическое сообщение.";
				BslValidatorPlugin.log(BslValidatorPlugin.createErrorStatus(msg, e));

			}
			EObject diagnosticObject = new EObjectAtOffsetHelper()
					.resolveContainedElementAt((XtextResource) object.eResource(), offset);
			if (diagnosticObject == null)
				diagnosticObject = object;

			// Попытка передать сюда что-нибудь и посмотреть, будет ли доступна информация в
			// quick fix
			String[] issueData = new String[1];
			if (QuickFixProvider.class.isAssignableFrom(bslDiagnosticClass)) {
				QuickFixProvider diagnosticInstance = (QuickFixProvider) diagnosticProvider
						.getDiagnosticInstance(bslDiagnosticClass);
				List<CodeAction> quickFixes = diagnosticInstance.getQuickFixes(diagnostic, null, documentContext);

				issueData = new String[quickFixes.size() + 1];
			}
			issueData[0] = "Пропущена точка с запятой в конце выражения";

			if (diagnosticType.equals(DiagnosticType.ERROR) || diagnosticType.equals(DiagnosticType.VULNERABILITY))
				messageAcceptor.acceptError(diagnostic
						.getMessage(), diagnosticObject, offset, length, issueData[0], issueData);

			else
				messageAcceptor.acceptWarning(diagnostic
						.getMessage(), diagnosticObject, offset, length, issueData[0], issueData);
		}

	}

}
