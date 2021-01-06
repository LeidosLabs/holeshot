/*
 * Licensed to Leidos, Inc. under one or more contributor license agreements.  
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * Leidos, Inc. licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.leidoslabs.holeshot.elt.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * @author robertsrg
 *
 */
public class TabbedToolbar extends Composite {
	private final ToolBar toolbar;
	private final CTabFolder tabFolder;
	private final List<TabbedToolbarItem> items;

	public TabbedToolbar(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, true));
		toolbar = new ToolBar(this, SWT.VERTICAL);
		toolbar.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));

		tabFolder = new CTabFolder(this, SWT.MULTI | SWT.TOP | SWT.BORDER | SWT.LEFT_TO_RIGHT );
		GridData tabGridData = new GridData(SWT.LEFT, SWT.FILL, false, true);
		tabGridData.exclude = true;
		tabFolder.setVisible(false);
		
		tabFolder.setLayoutData(tabGridData);
		tabFolder.setMinimizeVisible(true);
		tabFolder.setMaximizeVisible(false);
		//tabFolder.setSingle(true);
		items = new ArrayList<TabbedToolbarItem>();

		tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			@Override
			public void minimize(CTabFolderEvent event) {
				swapState();
			}
		});
	}
	
	/**
	 * @return the toolbar
	 */
	public ToolBar getToolbar() {
		return toolbar;
	}

	/**
	 * @return the tabFolder
	 */
	public CTabFolder getTabFolder() {
		return tabFolder;
	}
	
	public TabbedToolbarItem addItem(Image image, String name) {
		TabbedToolbarItem item = new TabbedToolbarItem(image, name);
		items.add(item);
		return item;
	}
	
	private void swapState() {
		final GridData tabFolderGridData = ((GridData)tabFolder.getLayoutData()); 
		final GridData toolbarGridData = ((GridData)toolbar.getLayoutData()); 
		final boolean setTabFolderVisible = tabFolderGridData.exclude;
		final boolean setToolbarVisible = !setTabFolderVisible;
		
		tabFolderGridData.exclude = !setTabFolderVisible;
		tabFolder.setVisible(setTabFolderVisible);
		
		toolbarGridData.exclude = !setToolbarVisible;;
		toolbar.setVisible(setToolbarVisible);
		requestLayout();
	}
	
	
	public class TabbedToolbarItem {
		private final ToolItem toolItem;
		private final CTabItem tabItem;
		
		/**
		 * @return the toolItem
		 */
		public ToolItem getToolItem() {
			return toolItem;
		}

		/**
		 * @return the tabItem
		 */
		public CTabItem getTabItem() {
			return tabItem;
		}

		private TabbedToolbarItem(Image image, String name) {
			toolItem = new ToolItem(toolbar, SWT.PUSH);
			toolItem.setImage(image);
			toolItem.setToolTipText(name);
			toolItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					super.widgetSelected(e);
					swapState();
					tabFolder.setSelection(tabItem);
				}
			});
			tabItem = new CTabItem(tabFolder, SWT.NONE);
			tabItem.setToolTipText(name);
//			tabItem.setText(name);
			tabItem.setImage(image);
		}
	}

}
