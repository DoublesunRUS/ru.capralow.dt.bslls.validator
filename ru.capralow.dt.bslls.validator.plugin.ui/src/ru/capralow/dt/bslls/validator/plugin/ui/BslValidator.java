package ru.capralow.dt.bslls.validator.plugin.ui;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.resource.XtextResource;
import org.github._1c_syntax.bsl.languageserver.context.ServerContext;
import org.github._1c_syntax.bsl.languageserver.diagnostics.BSLDiagnostic;
import org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import org.github._1c_syntax.bsl.languageserver.providers.DiagnosticProvider;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.validation.CustomValidationMessageAcceptor;
import com._1c.g5.v8.dt.bsl.validation.IExternalBslValidator;

public class BslValidator implements IExternalBslValidator {
	private DiagnosticProvider diagnosticProvider;

	public BslValidator() {
		super();

		diagnosticProvider = new DiagnosticProvider();
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
		List<Diagnostic> diagnostics = diagnosticProvider.computeDiagnostics(bslServerContext.getDocument(objectUri));
		for (Diagnostic diagnostic : diagnostics) {
			Class<? extends BSLDiagnostic> diagnosticClass = DiagnosticProvider.getBSLDiagnosticClass(diagnostic);
			DiagnosticType diagnosticType = DiagnosticProvider.getDiagnosticType(diagnosticClass);
			org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity diagnosticSeverity = DiagnosticProvider
					.getDiagnosticSeverity(diagnosticClass);

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

			if (diagnosticType.equals(DiagnosticType.ERROR) || diagnosticType.equals(DiagnosticType.VULNERABILITY))
				messageAcceptor.acceptError(diagnostic.getMessage(), diagnosticObject, offset, length, "", "");

			else
				messageAcceptor.acceptWarning(diagnostic.getMessage(), diagnosticObject, offset, length, "", "");
		}

	}

}
