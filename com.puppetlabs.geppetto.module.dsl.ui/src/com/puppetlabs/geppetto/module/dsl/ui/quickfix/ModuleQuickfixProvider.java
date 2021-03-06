package com.puppetlabs.geppetto.module.dsl.ui.quickfix;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.wizards.IWizardDescriptor;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.ui.editor.model.edit.IModificationContext;
import org.eclipse.xtext.ui.editor.model.edit.ISemanticModification;
import org.eclipse.xtext.ui.editor.quickfix.DefaultQuickfixProvider;
import org.eclipse.xtext.ui.editor.quickfix.Fix;
import org.eclipse.xtext.ui.editor.quickfix.IssueResolutionAcceptor;
import org.eclipse.xtext.validation.Issue;

import com.google.inject.Inject;
import com.puppetlabs.geppetto.forge.model.ModuleName;
import com.puppetlabs.geppetto.module.dsl.ModuleUtil;
import com.puppetlabs.geppetto.module.dsl.metadata.JsonDependency;
import com.puppetlabs.geppetto.module.dsl.metadata.JsonMetadata;
import com.puppetlabs.geppetto.module.dsl.ui.preferences.ModulePreferencesHelper;
import com.puppetlabs.geppetto.module.dsl.validation.ModuleDiagnostics;
import com.puppetlabs.geppetto.semver.Version;

/**
 * Custom quickfixes.
 */
@Singleton
public class ModuleQuickfixProvider extends DefaultQuickfixProvider {
	/**
	 * Find start and length of the element that <code>issue</code> represents in an array. The resulting <code>bounds</code> will include
	 * preceeding or trailing comma (not both) if present so that the syntax
	 * of the array is intact if the range is removed.
	 *
	 * @param txt
	 *            The text containing the array
	 * @param issue
	 *            The issue that represents the array element
	 * @param bounds
	 *            A two element array where the start and lenght will be returned
	 * @return <code>true</code> if the range could be deteremined or <code>false</code> to indicate that the tokens expected
	 *         to precede and succeed the element could not be found.
	 */
	public static boolean findArrayElementBounds(String txt, Issue issue, int[] bounds) {
		int start = issue.getOffset();
		int end = issue.getOffset() + issue.getLength();

		// Find preceding comma or start of array
		char c = 0;
		int cma = start;
		while(--cma >= 0) {
			c = txt.charAt(cma);
			if(c == ',' || c == '[')
				break;
		}

		if(c != ',') {
			if(c != '[')
				// Element must be preceeded by comma or start of array
				return false;

			// No preceding comma. Find trailing comma or end of array
			int eot = txt.length();
			cma = end;
			c = 0;
			while(cma < eot) {
				c = txt.charAt(cma);
				if(c == ',' || c == ']')
					break;
				++cma;
			}
			if(c == ',') {
				end = cma + 1;
				// Also swallow whitespace after comma
				while(end < eot && Character.isWhitespace(txt.charAt(end)))
					end++;
			}
			else if(c == ']')
				end = cma;
			else
				// Neither comma nor array end follow after element.
				return false;

		}
		else
			start = cma;
		bounds[0] = start;
		bounds[1] = end - start;
		return true;
	}

	@Inject
	private ModuleUtil moduleUtil;

	@Inject
	private ModulePreferencesHelper preferenceHelper;

	@Inject
	private IWorkbench workbench;

	private Pattern REF_PATTERN = Pattern.compile("reference to Module \'([^\']+)\'\\.");

	@Fix(ModuleDiagnostics.ISSUE__MISSING_REQUIRED_ATTRIBUTE)
	public void addMissingAttribute(final Issue issue, IssueResolutionAcceptor acceptor) {
		String[] data = issue.getData();
		if(data == null || data.length == 0)
			return;
		final String key = data[0];
		acceptor.accept(
			issue, "Add entry for \"" + key + "\"", "Add missing entry for attribute \"" + key + "\"with template value", null,
			new ISemanticModification() {
				@Override
				public void apply(EObject element, IModificationContext context) throws Exception {
					IXtextDocument doc = context.getXtextDocument();
					StringBuilder bld = new StringBuilder();
					bld.append("\"");
					bld.append(key);
					bld.append("\": \"");
					context.getXtextDocument();
					if("version".equals(key))
						bld.append("0.1.0");
					else if("version_requirement".equals(key))
						bld.append(">=0.0.0");
					else if("author".equals(key))
						bld.append(getModuleOwner());
					else if("name".equals(key)) {
						if((element instanceof JsonMetadata)) {
							bld.append(getModuleOwner());
							bld.append("-");
							bld.append(ModuleName.safeName(doc.<IFile> getAdapter(IFile.class).getParent().getName(), false));
						}
					}
					bld.append("\",\n  ");
					doc.replace(issue.getOffset(), 0, bld.toString());
				}
			});
	}

	@Override
	public void createLinkingIssueResolutions(Issue issue, IssueResolutionAcceptor acceptor) {
		super.createLinkingIssueResolutions(issue, acceptor);
		Matcher m = REF_PATTERN.matcher(issue.getMessage());
		if(m.find()) {
			final String key = m.group(1);
			acceptor.accept(issue, "Import " + key + " from Puppet Forge", "Import the missing " + key +
				" module from the Puppet Forge repository", null, new ISemanticModification() {
				@Override
				public void apply(EObject element, IModificationContext context) throws Exception {
					IWizardDescriptor descriptor = workbench.getImportWizardRegistry().findWizard(
						"com.puppetlabs.geppetto.ui.ImportPuppetModuleFromForgeWizard");
					IWorkbenchWizard wizard = descriptor.createWizard();
					WizardDialog wd = new WizardDialog(workbench.getActiveWorkbenchWindow().getShell(), wizard);
					((NewWithKeyword) wizard).startWithKeyword(key);
					wd.setTitle(wizard.getWindowTitle());
					wd.open();
				}
			});
			acceptor.accept(
				issue, "Import " + key + " from local disk", "Import the missing " + key + " module from a local source folder", null,
				new ISemanticModification() {
					@Override
					public void apply(EObject element, IModificationContext context) throws Exception {
						IWizardDescriptor descriptor = workbench.getImportWizardRegistry().findWizard(
							"com.puppetlabs.geppetto.ui.ImportPuppetModuleFromSourceWizard");
						IWorkbenchWizard wizard = descriptor.createWizard();
						WizardDialog wd = new WizardDialog(workbench.getActiveWorkbenchWindow().getShell(), wizard);
						wd.setTitle(wizard.getWindowTitle());
						wd.open();
					}
				});
		}
	}

	public String getModuleOwner() {
		String moduleOwner = preferenceHelper.getForgeLogin();
		if(moduleOwner == null)
			moduleOwner = ModuleName.safeOwner(System.getProperty("user.name"));
		return moduleOwner;
	}

	private Version getResolvedMetadataVersion(EObject element) {
		return moduleUtil.getVersion(moduleUtil.getReferencedModule(((JsonDependency) element.eContainer().eContainer())));
	}

	@Fix(ModuleDiagnostics.ISSUE__DEPENDENCY_DECLARED_MORE_THAN_ONCE)
	public void removeRedundantDependency(final Issue issue, final IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Remove dependency", "Remove duplication of dependency", null, new ISemanticModification() {
			@Override
			public void apply(EObject element, IModificationContext context) throws Exception {
				IXtextDocument doc = context.getXtextDocument();
				int[] bounds = new int[2];
				if(findArrayElementBounds(doc.get(), issue, bounds))
					doc.replace(bounds[0], bounds[1], "");
			}
		});

	}

	@Fix(ModuleDiagnostics.ISSUE__MODULE_VERSION_RANGE_MISMATCH)
	public void selectMatchingRange(final Issue issue, final IssueResolutionAcceptor acceptor) {
		acceptor.accept(
			issue, "Use matching >=n.n.n range", "Change to a range that matches versions\ngreater or equal to the selected module", null,
			new ISemanticModification() {
				@Override
				public void apply(EObject element, IModificationContext context) throws Exception {
					StringBuilder bld = new StringBuilder();
					bld.append("\">=");
					getResolvedMetadataVersion(element).toString(bld);
					bld.append("\"");
					context.getXtextDocument().replace(issue.getOffset(), issue.getLength(), bld.toString());
				}
			});
		acceptor.accept(
			issue, "Use matching n.x range", "Change to a range that matches versions\nwith the same major number as selected module",
			null, new ISemanticModification() {
				@Override
				public void apply(EObject element, IModificationContext context) throws Exception {
					StringBuilder bld = new StringBuilder();
					bld.append("\"");
					bld.append(getResolvedMetadataVersion(element).getMajor());
					bld.append(".x\"");
					context.getXtextDocument().replace(issue.getOffset(), issue.getLength(), bld.toString());
				}
			});
		acceptor.accept(
			issue, "Use matching n.n.x range",
			"Change to a range that matches versions\nwith the same major and minor number as selected module", null,
			new ISemanticModification() {
				@Override
				public void apply(EObject element, IModificationContext context) throws Exception {
					Version v = getResolvedMetadataVersion(element);
					StringBuilder bld = new StringBuilder();
					bld.append("\"");
					bld.append(v.getMajor());
					bld.append(".");
					bld.append(v.getMinor());
					bld.append(".x\"");
					context.getXtextDocument().replace(issue.getOffset(), issue.getLength(), bld.toString());
				}
			});
		acceptor.accept(
			issue, "Use exact range", "Change to a range exactly matches the version\nof the selected module", null,
			new ISemanticModification() {
				@Override
				public void apply(EObject element, IModificationContext context) throws Exception {
					StringBuilder bld = new StringBuilder();
					bld.append("\"");
					getResolvedMetadataVersion(element).toString(bld);
					bld.append("\"");
					context.getXtextDocument().replace(issue.getOffset(), issue.getLength(), bld.toString());
				}
			});
	}
}
