package ru.capralow.dt.bslls.validator.plugin.internal.ui;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
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
import org.eclipse.xtext.util.CancelIndicator;
import org.osgi.framework.Bundle;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.resource.BslResource;
import com._1c.g5.v8.dt.bsl.validation.CustomValidationMessageAcceptor;
import com._1c.g5.v8.dt.bsl.validation.IExternalBslValidator;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com.github._1c_syntax.bsl.languageserver.codeactions.QuickFixSupplier;
import com.github._1c_syntax.bsl.languageserver.configuration.DiagnosticLanguage;
import com.github._1c_syntax.bsl.languageserver.configuration.LanguageServerConfiguration;
import com.github._1c_syntax.bsl.languageserver.context.DocumentContext;
import com.github._1c_syntax.bsl.languageserver.context.ServerContext;
import com.github._1c_syntax.bsl.languageserver.diagnostics.BSLDiagnostic;
import com.github._1c_syntax.bsl.languageserver.diagnostics.CodeBlockBeforeSubDiagnostic;
import com.github._1c_syntax.bsl.languageserver.diagnostics.DiagnosticSupplier;
import com.github._1c_syntax.bsl.languageserver.diagnostics.FunctionShouldHaveReturnDiagnostic;
import com.github._1c_syntax.bsl.languageserver.diagnostics.LineLengthDiagnostic;
import com.github._1c_syntax.bsl.languageserver.diagnostics.ParseErrorDiagnostic;
import com.github._1c_syntax.bsl.languageserver.diagnostics.ProcedureReturnsValueDiagnostic;
import com.github._1c_syntax.bsl.languageserver.diagnostics.QuickFixProvider;
import com.github._1c_syntax.bsl.languageserver.diagnostics.UnknownPreprocessorSymbolDiagnostic;
import com.github._1c_syntax.bsl.languageserver.diagnostics.UnreachableCodeDiagnostic;
import com.github._1c_syntax.bsl.languageserver.diagnostics.UsingServiceTagDiagnostic;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticInfo;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.languageserver.providers.DiagnosticProvider;
import com.github._1c_syntax.mdclasses.metadata.additional.ConfigurationSource;
import com.google.inject.Inject;

public class BslValidator implements IExternalBslValidator {
	private class ProjectContext {

		private static final String BSL_LS_FILENAME = ".bsl-language-server.json"; //$NON-NLS-1$

		private DiagnosticSupplier diagnosticSupplier;
		private DiagnosticProvider diagnosticProvider;
		private DiagnosticLanguage diagnosticLanguage;
		private QuickFixSupplier quickFixSuplier;
		private ServerContext bslServerContext;

		public ProjectContext(IProject project) {
			LanguageServerConfiguration lsConfiguration = initializeLsConfiguration(project);

			diagnosticLanguage = lsConfiguration.getDiagnosticLanguage();
			diagnosticSupplier = new DiagnosticSupplier(lsConfiguration);
			diagnosticProvider = new DiagnosticProvider(diagnosticSupplier);
			quickFixSuplier = new QuickFixSupplier(diagnosticSupplier);

			bslServerContext = new ServerContext(project.getLocation().toFile().toPath());

			String initializationMessage = ""; //$NON-NLS-1$

			ConfigurationSource configurationSource = bslServerContext.getConfiguration().getConfigurationSource();

			if (configurationSource.equals(ConfigurationSource.EDT))
				initializationMessage = BSL_LS_PREFIX.concat("Инициализация с EDT метаданными: ") //$NON-NLS-1$
						.concat(project.getName());

			else if (configurationSource.equals(ConfigurationSource.EMPTY))
				initializationMessage = BSL_LS_PREFIX.concat("Инициализация БЕЗ метаданных: ") //$NON-NLS-1$
						.concat(project.getName());

			else if (configurationSource.equals(ConfigurationSource.DESIGNER))
				initializationMessage = BSL_LS_PREFIX.concat("Инициализация c метаданными КОНФИГУРАТОРА: ") //$NON-NLS-1$
						.concat(project.getName());

			else
				initializationMessage = BSL_LS_PREFIX.concat("Инициализация c НЕИЗВЕСТНЫМИ метаданными: ") //$NON-NLS-1$
						.concat(project.getName());

			BslValidatorPlugin.log(BslValidatorPlugin.createInfoStatus(initializationMessage));
		}

		private IPath getConfigurationFilePath() {
			Bundle bundle = Platform.getBundle(BslValidatorPlugin.ID);
			return Platform.getStateLocation(bundle);
		}

		private LanguageServerConfiguration initializeLsConfiguration(IProject project) {
			File configurationFile;

			configurationFile = new File(project.getLocation().toFile().getParent() + File.separator + BSL_LS_FILENAME);
			if (!configurationFile.exists())
				configurationFile = new File(getConfigurationFilePath() + File.separator + BSL_LS_FILENAME);

			if (configurationFile.exists()) {
				String initializationMessage = BSL_LS_PREFIX.concat("Конфигурационный файл: ") //$NON-NLS-1$
						.concat(configurationFile.getPath());
				BslValidatorPlugin.log(BslValidatorPlugin.createInfoStatus(initializationMessage));
			}

			LanguageServerConfiguration lsConfiguration = LanguageServerConfiguration.create(configurationFile);

			Map<String, Either<Boolean, Map<String, Object>>> diagnostics = lsConfiguration.getDiagnostics();
			if (diagnostics == null)
				diagnostics = new HashMap<>();

			Collection<Class<? extends BSLDiagnostic>> duplicateDiagnostics = new ArrayList<>();

			// Свои механизмы в EDT
			duplicateDiagnostics.add(LineLengthDiagnostic.class);
			duplicateDiagnostics.add(ParseErrorDiagnostic.class);
			duplicateDiagnostics.add(UsingServiceTagDiagnostic.class);

			// Диагностики есть в EDT
			duplicateDiagnostics.add(CodeBlockBeforeSubDiagnostic.class);
			duplicateDiagnostics.add(FunctionShouldHaveReturnDiagnostic.class);
			duplicateDiagnostics.add(ProcedureReturnsValueDiagnostic.class);
			duplicateDiagnostics.add(UnknownPreprocessorSymbolDiagnostic.class);
			duplicateDiagnostics.add(UnreachableCodeDiagnostic.class);

			// В настройках можно принудительно включить выключенные диагностики
			for (Class<? extends BSLDiagnostic> diagnostic : duplicateDiagnostics) {
				DiagnosticInfo diagnosticInfo = new DiagnosticInfo(diagnostic, lsConfiguration.getDiagnosticLanguage());
				String diagnocticCode = diagnosticInfo.getCode().getStringValue();
				if (!diagnostics.containsKey(diagnocticCode))
					diagnostics.put(diagnocticCode, falseForLeft);
			}

			lsConfiguration.setDiagnostics(diagnostics);

			return lsConfiguration;
		}
	}

	private static final String QUICKFIX_CODE = "bsl-language-server"; //$NON-NLS-1$

	private static final String BSL_LS_PREFIX = "[BSL LS] "; //$NON-NLS-1$

	private static final Either<Boolean, Map<String, Object>> falseForLeft = Either.forLeft(false);

	private static StringBuilder getIssueData(Diagnostic diagnostic, Class<? extends BSLDiagnostic> bslDiagnosticClass,
			DocumentContext documentContext, Document doc, ProjectContext projectContext) {
		StringBuilder issueData = new StringBuilder();

		if (documentContext == null || !QuickFixProvider.class.isAssignableFrom(bslDiagnosticClass))
			return issueData;

		Optional<Class<? extends QuickFixProvider>> quickFixClass = projectContext.quickFixSuplier
				.getQuickFixClass(diagnostic.getCode());

		if (!quickFixClass.isPresent())
			return issueData;

		QuickFixProvider quickFixProvider = projectContext.quickFixSuplier.getQuickFixInstance(quickFixClass.get());

		List<CodeAction> quickFixes = quickFixProvider
				.getQuickFixes(Collections.singletonList(diagnostic), null, documentContext);

		for (CodeAction quickFix : quickFixes) {
			List<TextEdit> changes = quickFix.getEdit().getChanges().get(documentContext.getUri().toString());
			if (changes.size() != 1)
				continue;

			TextEdit change = changes.get(0);
			Integer[] offsetAndLength = getOffsetAndLength(change.getRange(), doc);
			Integer offset = offsetAndLength[0];
			Integer length = offsetAndLength[1];

			List<String> issueLine = new ArrayList<>();
			issueLine.add(BSL_LS_PREFIX.concat(quickFix.getTitle()));
			issueLine.add(diagnostic.getMessage());
			issueLine.add(offset.toString());
			issueLine.add(length.toString());
			issueLine.add(change.getNewText());

			if (issueData.length() != 0)
				issueData.append(System.lineSeparator());
			issueData.append(String.join("|", issueLine)); //$NON-NLS-1$
		}

		return issueData;

	}

	private static Integer[] getOffsetAndLength(Range range, Document doc) {
		Integer offset = 0;
		Integer length = 0;
		try {
			offset = doc.getLineOffset(range.getStart().getLine()) + range.getStart().getCharacter();
			Integer endOffset = doc.getLineOffset(range.getEnd().getLine()) + range.getEnd().getCharacter();
			length = endOffset - offset;

		} catch (BadLocationException e) {
			BslValidatorPlugin
					.log(BslValidatorPlugin.createErrorStatus(Messages.BslValidator_Bad_Location_Exception, e));

		}

		Integer[] result = new Integer[2];
		result[0] = offset;
		result[1] = length;

		return result;
	}

	private Map<IProject, ProjectContext> projectsContext;

	private EObjectAtOffsetHelper eObjectOffsetHelper;

	@Inject
	private IV8ProjectManager projectManager;

	public BslValidator() {
		super();

		eObjectOffsetHelper = new EObjectAtOffsetHelper();
		projectsContext = new HashMap<>();
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			ProjectContext projectContext = new ProjectContext(project);
			projectsContext.put(project, projectContext);
		}
	}

	@Override
	public boolean needValidation(EObject object) {
		if (!(object instanceof Module))
			return false;

		return ((BslResource) ((Module) object).eResource()).isDeepAnalysing();
	}

	@Override
	public void validate(EObject object, CustomValidationMessageAcceptor messageAcceptor, CancelIndicator monitor) {
		validateModule(object, messageAcceptor, monitor);
	}

	private long registerIssue(EObject object, CustomValidationMessageAcceptor messageAcceptor, Diagnostic diagnostic,
			XtextResource eobjectResource, DocumentContext documentContext, Document doc,
			ProjectContext projectContext) {

		long findNodeDifference = 0;

		Optional<Class<? extends BSLDiagnostic>> diagnosticClass = projectContext.diagnosticSupplier
				.getDiagnosticClass(diagnostic.getCode());

		if (!diagnosticClass.isPresent())
			return findNodeDifference;

		Class<? extends BSLDiagnostic> bslDiagnosticClass = diagnosticClass.get();
		DiagnosticInfo diagnosticInfo = new DiagnosticInfo(bslDiagnosticClass, projectContext.diagnosticLanguage);

		Integer[] offsetAndLength = getOffsetAndLength(diagnostic.getRange(), doc);
		Integer offset = offsetAndLength[0];
		Integer length = offsetAndLength[1];

		long startTime = System.currentTimeMillis();
		EObject diagnosticObject = eObjectOffsetHelper.resolveContainedElementAt(eobjectResource, offset);
		findNodeDifference = System.currentTimeMillis() - startTime;
		if (diagnosticObject == null)
			diagnosticObject = object;

		StringBuilder issueData = getIssueData(diagnostic, bslDiagnosticClass, documentContext, doc, projectContext);

		String diagnosticMessage = BSL_LS_PREFIX.concat(diagnostic.getMessage());

		DiagnosticType diagnosticType = diagnosticInfo.getType();
		if (diagnosticType.equals(DiagnosticType.ERROR) || diagnosticType.equals(DiagnosticType.VULNERABILITY))
			messageAcceptor.acceptError(diagnosticMessage,
					diagnosticObject,
					offset,
					length,
					QUICKFIX_CODE,
					issueData.toString());

		else
			messageAcceptor.acceptWarning(diagnosticMessage,
					diagnosticObject,
					offset,
					length,
					QUICKFIX_CODE,
					issueData.toString());

		return findNodeDifference;

	}

	private void validateModule(EObject object, CustomValidationMessageAcceptor messageAcceptor,
			CancelIndicator monitor) {
		if (monitor.isCanceled())
			return;

		IV8Project v8Project = projectManager.getProject(object);
		if (v8Project == null)
			return;

		ProjectContext projectContext = projectsContext.get(v8Project.getProject());
		if (projectContext == null)
			return;

		long startTime = System.currentTimeMillis();
		BslValidatorPlugin
				.log(BslValidatorPlugin.createInfoStatus(BSL_LS_PREFIX.concat("Начало передачи текста модуля"))); //$NON-NLS-1$

		XtextResource eObjectResource = (XtextResource) object.eResource();

		Module module = (Module) object;
		IFile moduleFile = ResourcesPlugin.getWorkspace().getRoot()
				.getFile(new Path(EcoreUtil.getURI(module).toPlatformString(true)));

		ICompositeNode node = NodeModelUtils.findActualNodeFor(module);
		String objectText = node.getText();

		Document doc = new Document(objectText);

		DocumentContext documentContext = projectContext.bslServerContext.addDocument(moduleFile.getLocationURI(),
				objectText);

		long computeStartTime = System.currentTimeMillis();
		List<Diagnostic> diagnostics = projectContext.diagnosticProvider.computeDiagnostics(documentContext);
		long computeDifference = System.currentTimeMillis() - computeStartTime;
		long findNodeDifference = 0;
		int findNodeAmount = diagnostics.size();
		for (Diagnostic diagnostic : diagnostics) {
			if (monitor.isCanceled())
				break;

			long currentNodeDifference = registerIssue(object,
					messageAcceptor,
					diagnostic,
					eObjectResource,
					documentContext,
					doc,
					projectContext);

			findNodeDifference += currentNodeDifference;
		}

		projectContext.diagnosticProvider.clearComputedDiagnostics(documentContext);
		documentContext.clearSecondaryData();

		long difference = System.currentTimeMillis() - startTime;

		if (difference > 30000)
			BslValidatorPlugin.log(BslValidatorPlugin.createInfoStatus(BSL_LS_PREFIX
					.concat("URI модуля длительной проверки: ").concat(moduleFile.getLocation().toString()))); //$NON-NLS-1$

		String differenceText = MessageFormat.format("(всего:{0}сек,анализ:{1}сек,поиск:{2}сек/{3}проб)", //$NON-NLS-1$
				Long.toString(difference / 1000),
				Long.toString(computeDifference / 1000),
				Long.toString(findNodeDifference / 1000),
				findNodeAmount);

		BslValidatorPlugin.log(BslValidatorPlugin
				.createInfoStatus(BSL_LS_PREFIX.concat("Окончание передачи текста модуля ").concat(differenceText))); //$NON-NLS-1$
	}

}
