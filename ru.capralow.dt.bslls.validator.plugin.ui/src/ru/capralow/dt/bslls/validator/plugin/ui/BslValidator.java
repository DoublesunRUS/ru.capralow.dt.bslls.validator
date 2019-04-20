package ru.capralow.dt.bslls.validator.plugin.ui;

import java.util.List;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.github._1c_syntax.bsl.languageserver.context.ServerContext;
import org.github._1c_syntax.bsl.languageserver.providers.DiagnosticProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.validation.CustomValidationMessageAcceptor;
import com._1c.g5.v8.dt.bsl.validation.IExternalBslValidator;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

public class BslValidator implements IExternalBslValidator {
	private static final Logger LOGGER = LoggerFactory.getLogger(BslValidator.class.getSimpleName());

	private DiagnosticProvider diagnosticProvider;
	private ServerContext bslServerContext;

	public BslValidator() {
		super();

		diagnosticProvider = new DiagnosticProvider();
		bslServerContext = new ServerContext();
	}

	@Override
	public boolean needValidation(EObject object) {
		if (!(object instanceof Module))
			return false;

		ICompositeNode node = NodeModelUtils.findActualNodeFor(object);
		String moduleText = node.getText();

		bslServerContext.addDocument(((Module) object).getUniqueName(), moduleText);

		return true;
	}

	@Override
	public void validate(EObject object, CustomValidationMessageAcceptor messageAcceptor) {
		Module module = (Module) object;

		ICompositeNode node = NodeModelUtils.findActualNodeFor(object);
		String moduleText = node.getText();
		Document doc = new Document(moduleText);

		XtextResource resource = (XtextResource) module.eResource();
		String moduleName = module.getUniqueName();
		List<Diagnostic> diagnostics = diagnosticProvider.computeDiagnostics(bslServerContext.getDocument(moduleName));
		for (Diagnostic diagnostic : diagnostics) {
			Integer offset = 0;
			Integer length = 0;
			try {
				offset = doc.getLineOffset(diagnostic.getRange().getStart().getLine())
						+ diagnostic.getRange().getStart().getCharacter();
				Integer endOffset = doc.getLineOffset(diagnostic.getRange().getEnd().getLine())
						+ diagnostic.getRange().getEnd().getCharacter();
				length = endOffset - offset;

			} catch (BadLocationException e) {
				LOGGER.error("Не удалось определить объект, к которому относится диагностическое сообщение.", e);

			}
			EObject diagnosticObject = new EObjectAtOffsetHelper().resolveContainedElementAt(resource, offset);

			if (diagnostic.getSeverity().equals(DiagnosticSeverity.Error))
				messageAcceptor.acceptError(diagnostic.getMessage(), diagnosticObject, offset, length, "", "");

			else if (diagnostic.getSeverity().equals(DiagnosticSeverity.Warning))
				messageAcceptor.acceptWarning(diagnostic.getMessage(), diagnosticObject, offset, length, "", "");

			else if (diagnostic.getSeverity().equals(DiagnosticSeverity.Information))
				messageAcceptor.acceptWarning(diagnostic.getMessage(), diagnosticObject, offset, length, "", "");

			else if (diagnostic.getSeverity().equals(DiagnosticSeverity.Hint))
				messageAcceptor.acceptWarning(diagnostic.getMessage(), diagnosticObject, offset, length, "", "");
		}

	}

}
