/**
 * Copyright (c) 2013 Puppet Labs, Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Puppet Labs
 */
package com.puppetlabs.geppetto.ui.wizard;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.dialogs.WizardExportResourcesPage;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.puppetlabs.geppetto.common.Strings;
import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.forge.Forge;
import com.puppetlabs.geppetto.forge.ForgeService;
import com.puppetlabs.geppetto.forge.client.OAuthModule;
import com.puppetlabs.geppetto.forge.model.Metadata;
import com.puppetlabs.geppetto.forge.model.ModuleName;
import com.puppetlabs.geppetto.forge.util.ChecksumUtils;
import com.puppetlabs.geppetto.module.dsl.ui.preferences.ModulePreferencesHelper;
import com.puppetlabs.geppetto.ui.UIPlugin;
import com.puppetlabs.geppetto.ui.wizard.ModuleExportOperation.ExportSpec;

public class ModuleExportToForgeWizard extends ModuleExportToFileWizard {

	class ModuleExportToForgeWizardPage extends WizardExportResourcesPage implements ModuleExportWizardPage {
		private Text loginField;

		private Text passwordField;

		private Button saveInSecureStoreButton;

		private boolean validationChange;

		private Button dryRunButton;

		public ModuleExportToForgeWizardPage(IStructuredSelection selection) {
			this("moduleExportToForge", selection); //$NON-NLS-1$
			setTitle(UIPlugin.getLocalString("_UI_ExportModulesToForge"));
			setDescription(UIPlugin.getLocalString("_UI_ExportModulesToForge_desc"));
		}

		public ModuleExportToForgeWizardPage(String name, IStructuredSelection selection) {
			super(name, selection);
		}

		@Override
		protected void createDestinationGroup(Composite parent) {
			UIPlugin plugin = UIPlugin.getInstance();
			Group destinationGroup = new Group(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			destinationGroup.setLayout(layout);
			destinationGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
			destinationGroup.setText(plugin.getString("_UI_Forge_Credentials_label"));
			destinationGroup.setFont(parent.getFont());

			Font font = destinationGroup.getFont();

			Label loginLabel = new Label(destinationGroup, SWT.NONE);
			loginLabel.setText(plugin.getString("_UI_Login_label"));
			loginLabel.setFont(font);

			loginField = new Text(destinationGroup, SWT.BORDER | SWT.READ_ONLY);
			GridData data = new GridData(GridData.FILL_HORIZONTAL);
			data.widthHint = SIZING_TEXT_FIELD_WIDTH;
			loginField.setLayoutData(data);
			loginField.setFont(font);

			Label passwordLabel = new Label(destinationGroup, SWT.NONE);
			passwordLabel.setText(plugin.getString("_UI_Password_label"));
			passwordLabel.setFont(font);

			passwordField = new Text(destinationGroup, SWT.BORDER | SWT.PASSWORD);
			data = new GridData(GridData.FILL_HORIZONTAL);
			data.widthHint = SIZING_TEXT_FIELD_WIDTH;
			passwordField.setLayoutData(data);
			passwordField.setFont(font);
			passwordField.addListener(SWT.Modify, this);
		}

		@Override
		protected void createOptionsGroupButtons(Group optionsGroup) {
			UIPlugin plugin = UIPlugin.getInstance();
			Font font = optionsGroup.getFont();
			saveInSecureStoreButton = new Button(optionsGroup, SWT.CHECK);
			saveInSecureStoreButton.setText(plugin.getString("_UI_SaveInSecureStorage_label"));
			saveInSecureStoreButton.addListener(SWT.Selection, this);
			saveInSecureStoreButton.setFont(font);

			dryRunButton = new Button(optionsGroup, SWT.CHECK);
			dryRunButton.setText(plugin.getString("_UI_DryRun_label"));
			dryRunButton.setFont(font);
			dryRunButton.setSelection(false);
		}

		@Override
		public boolean finish() {
			// about to invoke the operation so save our state
			saveWidgetValues();

			if(!saveDirtyEditors())
				// User clicked on cancel when being asked to save dirty editors.
				return false;

			final Injector injector = UIPlugin.getInstance().createInjector(
				new OAuthModule(FORGE_CLIENT_ID, FORGE_CLIENT_SECRET, loginField.getText(), passwordField.getText()));
			try {
				@SuppressWarnings("unchecked")
				List<IResource> whiteCheckedResources = getWhiteCheckedResources();
				File tmpDir = new File(System.getProperty("java.io.tmpdir"));
				File destinationDir = File.createTempFile("forge-", ".tarballs", tmpDir);
				destinationDir.delete();
				destinationDir.mkdir();
				ModuleExportToForgeOperation exportOp = new ModuleExportToForgeOperation(
					getExportSpecs(whiteCheckedResources), destinationDir, dryRunButton.getSelection()) {

					@Override
					protected Forge getForge() {
						return injector.getInstance(Forge.class);
					}

					@Override
					protected ForgeService getForgeService() {
						return injector.getInstance(ForgeService.class);
					}
				};
				boolean result = executeExport(exportOp);
				Diagnostic diag = exportOp.getDiagnostic();
				if(diag.getSeverity() == Diagnostic.ERROR) {
					Exception e = diag.getException();
					ErrorDialog.openError(
						getContainer().getShell(), DataTransferMessages.DataTransfer_exportProblems, null, // no special message
						UIPlugin.createStatus(IStatus.ERROR, diag.toString(), e));
				}
				else
					MessageDialog.openInformation(
						getContainer().getShell(), DataTransferMessages.DataTransfer_information, diag.toString());
				return result;
			}
			catch(CoreException e) {
				ErrorDialog.openError(
					getContainer().getShell(), DataTransferMessages.DataTransfer_exportProblems, null, // no special message
					e.getStatus());
			}
			catch(Exception e) {
				ErrorDialog.openError(
					getContainer().getShell(), DataTransferMessages.DataTransfer_exportProblems, null,
					UIPlugin.createStatus(IStatus.ERROR, e.getMessage(), e));
			}
			return false;
		}

		@Override
		public void handleEvent(Event e) {
			if(validationChange)
				// Don't act on events that stems from login/password settings made
				// by the source group validation
				return;

			Widget source = e.widget;
			if(source == saveInSecureStoreButton && saveInSecureStoreButton.getSelection()) {
				String login = Strings.trimToNull(loginField.getText());
				if(login != null) {
					String password = Strings.emptyToNull(passwordField.getText());
					if(password != null)
						saveSecurePassword(login, password);
				}
			}
			updatePageCompletion();
		}

		@Override
		protected void internalSaveWidgetValues() {
			super.internalSaveWidgetValues();
			IDialogSettings settings = getDialogSettings();
			String login = Strings.trimToNull(loginField.getText());
			if(settings != null)
				settings.put(STORE_LOGIN, login);
			if(saveInSecureStoreButton.getSelection())
				saveSecurePassword(login, Strings.emptyToNull(passwordField.getText()));
		}

		@Override
		protected void restoreWidgetValues() {
			IDialogSettings settings = getDialogSettings();
			if(settings == null)
				return;

			String password = null;
			String login = settings.get(STORE_LOGIN);
			if(login == null)
				login = "";
			else
				password = loadSecurePassword(login);

			if(password == null)
				password = "";

			loginField.setText(login);
			passwordField.setText(password);
		}

		@Override
		public boolean validateDestinationGroup() {
			if("".equals(passwordField.getText())) {
				setErrorMessage(UIPlugin.getLocalString("_UI_EnterPassword"));
				return false;
			}
			return super.validateDestinationGroup();
		}

		@Override
		public boolean validateSourceGroup() {
			if(!super.validateSourceGroup())
				return false;

			try {
				@SuppressWarnings("unchecked")
				List<IResource> whiteCheckedResources = getWhiteCheckedResources();
				UIPlugin plugin = UIPlugin.getInstance();
				String owner = null;
				Diagnostic diag = new Diagnostic();
				for(ExportSpec spec : getExportSpecs(whiteCheckedResources)) {
					try {
						Metadata md = getForge().createFromModuleDirectory(
							spec.getModuleRoot(), spec.getFileFilter(), null, diag);
						if(md != null) {
							ModuleName name = md.getName();
							if(owner == null)
								owner = name.getOwner();
							else if(!owner.equals(name.getOwner())) {
								setErrorMessage(plugin.getString("_UI_MultipleModuleOwners"));
								return false;
							}
						}
					}
					catch(IOException e) {
					}
				}

				if(owner == null) {
					setErrorMessage(plugin.getString("_UI_NoModulesSelected"));
					return false;
				}
				if(!owner.equals(loginField.getText())) {
					// Owner changed
					validationChange = true;
					try {
						loginField.setText(owner);
						String password = null;
						if(saveInSecureStoreButton.getSelection())
							password = loadSecurePassword(owner);
						if(password == null)
							password = "";
						passwordField.setText(password);
					}
					finally {
						validationChange = false;
					}
				}
				return true;
			}
			catch(CoreException e) {
				setErrorMessage(e.getMessage());
				return false;
			}
		}
	}

	private static final String FORGE_CLIENT_ID = "cac18b1f07f13a244c47644548b29cbbe58048f3aaccdeefa7c0306467afda44";

	private static final String FORGE_CLIENT_SECRET = "2227c9a7392382f58b5e4d084b705827cb574673ff7d2a5905ef21685fd48e40";

	@Inject
	private ModulePreferencesHelper preferenceHelper;

	private static final String STORE_LOGIN = "ModuleExportToForgeWizardPage.STORE_LOGIN"; //$NON-NLS-1$

	@Override
	ModuleExportWizardPage createMainPage(IStructuredSelection selection) {
		return new ModuleExportToForgeWizardPage(selection);
	}

	private ISecurePreferences getPasswordNode(String login) {
		if(login == null)
			return null;

		ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
		if(preferences == null)
			return null;

		String host = Strings.trimToNull(preferenceHelper.getForgeURI());
		if(host == null)
			return null;

		StringBuilder bld = new StringBuilder();
		bld.append("/Puppetforge Credentials/"); //$NON-NLS-1$
		bld.append(login);
		bld.append('/');
		ChecksumUtils.appendSHA1(bld, host);
		return preferences.node(bld.toString());
	}

	private String loadSecurePassword(String login) {
		ISecurePreferences node = getPasswordNode(login);
		if(node != null)
			try {
				return node.get("password", null);
			}
		catch(StorageException e) {
		}
		return null;
	}

	private void saveSecurePassword(String login, String password) {
		ISecurePreferences node = getPasswordNode(login);
		if(node != null) {
			try {
				node.put("password", password, true); //$NON-NLS-1$
			}
			catch(StorageException ex) { /* ignored on purpose */
			}
		}
	}

}
