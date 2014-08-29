/**
 * Copyright (c) 2013 Puppet Labs, Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Initial implementation - itemis AG (http://www.itemis.eu
 *   Puppet Labs - specialized for puppet
 *
 */
package com.puppetlabs.geppetto.pp.dsl.ui.search;

import java.util.Collection;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.jface.internal.text.TableOwnerDrawSupport;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.ui.search.EObjectDescriptionContentProvider;

/**
 * @author Jan Koehnlein - Initial contribution and API
 * @author Knut Wannheden
 * @author Henrik Lindberg - Puppet Labs, adapted to Puppet
 */
public class PPObjectSearchDialog extends ListDialog {

	protected Text searchControl;

	private String initialPatternText;

	private Label messageLabel;

	private Label searchStatusLabel;

	private IteratorJob sizeCalculationJob;

	private Label matchingElementsLabel;

	/** @since 2.0 */
	// protected Text typeSearchControl;

	private IPPEObjectSearch searchEngine;

	private final ILabelProvider labelProvider;

	private boolean enableStyledLabels;

	public PPObjectSearchDialog(Shell parent, IPPEObjectSearch searchEngine, ILabelProvider labelProvider) {
		super(parent);
		this.searchEngine = searchEngine;
		this.labelProvider = labelProvider;
		setTitle("Open Puppet Element");
		setMessage("Enter element name prefix or pattern (*, ?, or camel case):");
		setAddCancelButton(true);
		// super class needs an IStructuredContentProvider so we register this dummy and
		// register the lazy one later
		setContentProvider(new IStructuredContentProvider() {
			@Override
			public void dispose() {
			}

			@Override
			public Object[] getElements(Object inputElement) {
				return null;
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		setLabelProvider(labelProvider);
	}

	public PPObjectSearchDialog(Shell parent, IPPEObjectSearch searchEngine, ILabelProvider labelProvider, boolean enableStyledLabels) {
		this(parent, searchEngine, labelProvider);
		this.enableStyledLabels = enableStyledLabels;
	}

	/**
	 * Called when the dialog is initially opened and whenever the input text changes. Applies the search filter as
	 * specified by {@link #searchControl} and {@link #typeSearchControl} using {@link #getSearchEngine()} and updates
	 * the result using {@link #startSizeCalculation(Iterable)}.
	 *
	 * @since 2.0
	 */
	protected void applyFilter() {
		String searchPattern = searchControl.getText();

		// TODO: get a set of accepted classes - null or empty collection means all/any
		// String typeSearchPattern = typeSearchControl.getText();
		Collection<EClass> acceptedClasses = null;
		if(searchPattern != null) {
			Iterable<IEObjectDescription> matches = getSearchEngine().findMatches(searchPattern, acceptedClasses);
			startSizeCalculation(matches);
		}
	}

	@Override
	protected Control createDialogArea(Composite container) {
		Composite parent = (Composite) super.createDialogArea(container);
		if(enableStyledLabels && labelProvider instanceof IStyledLabelProvider) {
			final Table table = getTableViewer().getTable();
			final IStyledLabelProvider styledLabelProvider = (IStyledLabelProvider) labelProvider;
			TableOwnerDrawSupport.install(table);
			Listener listener = new Listener() {
				@Override
				public void handleEvent(Event event) {
					handleSetData(event);
				}

				protected void handleSetData(Event event) {
					TableItem item = (TableItem) event.item;
					IEObjectDescription description = (IEObjectDescription) item.getData();
					if(description != null) {
						StyledString styledString = styledLabelProvider.getStyledText(description);
						String displayString = styledString == null
							? description.toString()
							: styledString.toString();
						StyleRange[] styleRanges = styledString.getStyleRanges();
						item.setText(displayString);
						TableOwnerDrawSupport.storeStyleRanges(item, 0, styleRanges);
					}
				}
			};
			table.addListener(SWT.SetData, listener);
		}
		messageLabel = new Label(parent, SWT.NONE);
		setDefaultGridData(messageLabel);
		EObjectDescriptionContentProvider contentProvider = new EObjectDescriptionContentProvider();
		getTableViewer().setContentProvider(contentProvider);
		getTableViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if(selection instanceof IStructuredSelection) {
					IStructuredSelection structuredSelection = (IStructuredSelection) selection;
					if(!structuredSelection.isEmpty()) {
						Object firstElement = structuredSelection.getFirstElement();
						if(firstElement instanceof IEObjectDescription) {
							IEObjectDescription eObjectDescription = (IEObjectDescription) firstElement;
							URI resourceURI = eObjectDescription.getEObjectURI().trimFragment();
							if(resourceURI.isPlatform()) {
								messageLabel.setText(resourceURI.toPlatformString(true));
							}
							else if(resourceURI.isFile()) {
								messageLabel.setText(resourceURI.toFileString());
							}
							else {
								messageLabel.setText(resourceURI.toString());
							}
							return;
						}
					}
				}
				messageLabel.setText(""); //$NON-NLS-1$
			}
		});

		applyFilter();

		return parent;
	}

	@Override
	protected Label createMessageArea(Composite composite) {
		Label label = super.createMessageArea(composite);
		searchControl = new Text(composite, SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
		setDefaultGridData(searchControl);

		// TODO: NOT MEANINGFUL TO SEARCH FOR ANY ECLASS - OFFER A SELECTION INSTEAD
		// TODO: HANDLE (FUTURE) SEARCH FOR RESOURCE WHERE USER DOES NOT KNOW THE ENCODING OF THE FQN
		//
		// Label typePatternLabel = new Label(composite, SWT.NONE);
		// typePatternLabel.setText("EClass name prefix or pattern");
		// setDefaultGridData(typePatternLabel);
		// typeSearchControl = new Text(composite, SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
		// setDefaultGridData(typeSearchControl);

		Composite labelComposite = new Composite(composite, SWT.NONE);
		setDefaultGridData(labelComposite);
		GridLayout labelCompositeLayout = new GridLayout(2, true);
		labelCompositeLayout.marginWidth = 0;
		labelComposite.setLayout(labelCompositeLayout);
		matchingElementsLabel = new Label(labelComposite, SWT.NONE);
		matchingElementsLabel.setText("Matching items:");
		matchingElementsLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		searchStatusLabel = new Label(labelComposite, SWT.RIGHT);
		searchStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		ModifyListener textModifyListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				applyFilter();
			}
		};
		searchControl.addModifyListener(textModifyListener);
		// typeSearchControl.addModifyListener(textModifyListener);

		// searchControl.addKeyListener(new KeyAdapter() {
		// @Override
		// public void keyPressed(KeyEvent e) {
		// if(e.keyCode == SWT.ARROW_DOWN) {
		// typeSearchControl.setFocus();
		// }
		// }
		// });

		if(initialPatternText != null) {
			searchControl.setText(initialPatternText);
			searchControl.selectAll();
		}

		// typeSearchControl.addKeyListener(new KeyAdapter() {
		searchControl.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.ARROW_DOWN) {
					TableViewer tableViewer = getTableViewer();
					tableViewer.getTable().setFocus();
					if(tableViewer.getSelection().isEmpty()) {
						Object firstElement = tableViewer.getElementAt(0);
						if(firstElement != null) {
							tableViewer.setSelection(new StructuredSelection(firstElement));
						}
					}
				}
			}
		});

		return label;
	}

	/**
	 * @since 2.0
	 */
	protected String getInitialPattern() {
		return initialPatternText;
	}

	protected IPPEObjectSearch getSearchEngine() {
		return searchEngine;
	}

	@Override
	protected int getTableStyle() {
		return super.getTableStyle() | SWT.VIRTUAL;
	}

	@Override
	public int open() {
		if(getInitialPattern() == null) {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if(window != null) {
				ISelection selection = window.getSelectionService().getSelection();
				if(selection instanceof ITextSelection) {
					String text = ((ITextSelection) selection).getText();
					if(text != null) {
						text = text.trim();
						// skip interpolation start
						if(text.startsWith("${"))
							text = text.substring(2);
						// skip interpolation end
						while(text.endsWith("}"))
							text = text.substring(0, text.length() - 1);
						// Skip a leading $ for variable
						if(text.startsWith("$"))
							text = text.substring(1);
						if(text.length() > 0) {
							setInitialPattern(text);
						}
					}
				}
			}
		}
		return super.open();
	}

	private void setDefaultGridData(Control control) {
		control.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
	}

	/**
	 * @since 2.0
	 */
	public void setInitialPattern(String text) {
		this.initialPatternText = text;
	}

	/**
	 * Called by {@link #applyFilter()} and is responsible for calling {@link #updateMatches(Collection, boolean)} with
	 * an appropriately sorted list of matches.
	 *
	 * @since 2.0
	 */
	protected void startSizeCalculation(Iterable<IEObjectDescription> matches) {
		if(getTableViewer() != null) {
			if(sizeCalculationJob != null) {
				sizeCalculationJob.cancel();
				try {
					sizeCalculationJob.join();
				}
				catch(InterruptedException e) {
					sizeCalculationJob = new IteratorJob(this);
				}
			}
			else {
				sizeCalculationJob = new IteratorJob(this);
			}
			sizeCalculationJob.init(matches);
			sizeCalculationJob.schedule();
		}
	}

	public void updateMatches(final Collection<IEObjectDescription> matches, final boolean isFinished) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if(getShell() != null) {
					if(getTableViewer() != null) {
						getTableViewer().setItemCount(matches.size());
						getTableViewer().setInput(matches);
					}
					searchStatusLabel.setText((isFinished)
						? "" : "Searching..."); //$NON-NLS-1$
					matchingElementsLabel.setText("Matching items:" + " (" + matches.size() + " matches)"); //$NON-NLS-1$
				}
			}
		});
	}

}
