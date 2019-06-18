package ru.capralow.dt.bslls.validator.plugin.internal.ui;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.ui.editor.model.edit.IModification;
import org.eclipse.xtext.ui.editor.quickfix.Fix;
import org.eclipse.xtext.ui.editor.quickfix.IssueResolutionAcceptor;
import org.eclipse.xtext.validation.Issue;

import com._1c.g5.v8.dt.bsl.model.BslPackage;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.ui.quickfix.AbstractExternalQuickfixProvider;

//@SuppressWarnings({ "unchecked", "rawtypes" })
public class BslQuickFix extends AbstractExternalQuickfixProvider {

	@Fix("Пропущена точка с запятой в конце выражения")
	public void processCanonicalSpellingKeywordsDiagnostic(final Issue issue, final IssueResolutionAcceptor acceptor) {
		// В ru.capralow.dt.bslls.validator.plugin.internal.ui.BslValidator есть
		// экземпляр класса Diagnostic.
		// Нужно получить этот класс, затем у него вызвать getQuickFixes
		// Например см класс
		// org.github._1c_syntax.bsl.languageserver.diagnostics.CanonicalSpellingKeywordsDiagnostic
		// Для каждого элемента из этого списка нужно сделать свой accept

		// Сейчас точка останова не срабатывает
		acceptor.accept(issue,
				"Пропущена точка с запятой в конце выражения",
				"Пропущена точка с запятой в конце выражения",
				(String) null,
				(IModification) new AbstractExternalQuickfixProvider.ExternalQuickfixModification(issue,
						(Class) Method.class,
						method -> {
							if (((Method) method).isExport()) {
								final List<INode> nodes = NodeModelUtils.findNodesForFeature((EObject) method,
										(EStructuralFeature) BslPackage.Literals.METHOD__EXPORT);
								if (nodes != null && !nodes.isEmpty()) {
									final INode exportKeywordNode = nodes.get(0);
									return new ReplaceEdit(exportKeywordNode.getTotalOffset(),
											exportKeywordNode.getTotalEndOffset() - exportKeywordNode.getTotalOffset(),
											"");
								}
							}
							return null;
						}));
	}
}
