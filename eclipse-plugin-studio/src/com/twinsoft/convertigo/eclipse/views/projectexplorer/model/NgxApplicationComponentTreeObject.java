/*
 * Copyright (c) 2001-2022 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.beans.BeanInfo;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.IScriptComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenu;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditor;
import com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditorInput;
import com.twinsoft.convertigo.eclipse.editors.ngx.ComponentFileEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.mobile.ComponentRefManager;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.mobile.ComponentRefManager.Mode;
import com.twinsoft.convertigo.engine.util.FileUtils;

public class NgxApplicationComponentTreeObject extends NgxComponentTreeObject implements IEditableTreeObject {
	public static final String P_TPL_VERSION = "#tplVersion";
	
	public NgxApplicationComponentTreeObject(Viewer viewer, ApplicationComponent object) {
		super(viewer, object);
	}

	public NgxApplicationComponentTreeObject(Viewer viewer, ApplicationComponent object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public void setParent(TreeParent parent) {
		super.setParent(parent);
	}

	@Override
	public ApplicationComponent getObject() {
		return (ApplicationComponent) super.getObject();
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		return super.testAttribute(target, name, value);
	}

	
	@Override
	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		super.treeObjectRemoved(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		Set<Object> done = checkDone(treeObjectEvent);
		
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject deletedTreeObject = (DatabaseObjectTreeObject)treeObject;
			DatabaseObject deletedObject = deletedTreeObject.getObject();
			try {
				String projectName = getObject().getProject().getName();
				boolean doUpdate = false;
				if (deletedTreeObject != null) {
					DatabaseObject parentOfDeleted = deletedTreeObject.getParentDatabaseObjectTreeObject().getObject();
					String deletedobjectQName = parentOfDeleted.getQName() + "." + deletedObject.getName();

					if (deletedTreeObject.isChildOf(this)) {
						resetMainScriptComponents(parentOfDeleted);
					}
					for (String useQName: ComponentRefManager.getCompConsumersUsedBy(deletedobjectQName, projectName)) {
						resetMainScriptComponents(ComponentRefManager.getDatabaseObjectByQName(useQName));
					}
					
					if (deletedTreeObject.isChildOf(this)) {
						// an shared object of this app has been deleted
						if (deletedObject instanceof UIActionStack || deletedObject instanceof UISharedRegularComponent) {
							for (String useQName: ComponentRefManager.getCompConsumersUsedBy(deletedobjectQName, projectName)) {
								ComponentRefManager.get(Mode.use).removeConsumer(deletedobjectQName, useQName);
							}
							
							// delete shared component icon file
							if (deletedObject instanceof UISharedRegularComponent) {
								File iconFile = new File(getObject().getProject().getDirPath(), ((UISharedRegularComponent)deletedObject).getIconFileName());
								FileUtils.deleteQuietly(iconFile);
							}
						}
						// a UIUseShared has been deleted
						if (deletedObject instanceof UIUseShared) {
							UIUseShared uius = (UIUseShared)deletedObject;
							String compQName = uius.getSharedComponentQName();
							if (!compQName.isEmpty()) {
								ComponentRefManager.get(Mode.use).removeConsumer(compQName, deletedobjectQName);
							}
						}
						// a UIDynamicInvoke has been deleted
						if (deletedObject instanceof UIDynamicInvoke) {
							UIDynamicInvoke uidi = (UIDynamicInvoke)deletedObject;
							String compQName = uidi.getSharedActionQName();
							if (!compQName.isEmpty()) {
								ComponentRefManager.get(Mode.use).removeConsumer(compQName, deletedobjectQName);
							}
						}
					} else {
						// an external shared object has been deleted and was used in this app
						if (deletedObject instanceof UIActionStack || deletedObject instanceof UISharedRegularComponent) {
							for (String useQName: ComponentRefManager.getCompConsumersUsedBy(deletedobjectQName, projectName)) {
								ComponentRefManager.get(Mode.use).removeConsumer(deletedobjectQName, useQName);
								doUpdate = true;
							}
						}
						// an object has been removed from an external object used in this app
						if (deletedTreeObject.getParentDatabaseObjectTreeObject().getObject() instanceof UIComponent) {
							UIComponent puic = (UIComponent) deletedTreeObject.getParentDatabaseObjectTreeObject().getObject();
							UIActionStack uias = puic.getSharedAction();
							if (uias != null && uias.isEnabled()) {
								if (ComponentRefManager.isCompUsedBy(uias.getQName(), projectName)) {
									doUpdate = true;
								}
							}
							UISharedComponent uisc = puic.getSharedComponent();
							if (uisc != null && uisc.isEnabled()) {
								if (ComponentRefManager.isCompUsedBy(uisc.getQName(), projectName)) {
									doUpdate = true;
								}
							}
						}
					}
				}
				
				if (deletedTreeObject.isChildOf(this) || doUpdate) {
					if (!done.add(getObject())) {
						return;
					}
					getObject().reset();
					getObject().updateSourceFiles();
				}				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}

	protected void updateNgxApp() {
		
	}
	
	@Override
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		super.treeObjectAdded(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		Set<Object> done = checkDone(treeObjectEvent);
		
		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "" : propertyName);
		
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject doto = (DatabaseObjectTreeObject)treeObject;
			DatabaseObject dbo = doto.getObject();
			
			try {
				String projectName = getObject().getProject().getName();
				boolean doUpdate = false;
				if (dbo.bNew) {
					if (dbo instanceof UIComponent) {
						if (doto.isChildOf(this)) {
							// a shared component has been added to this app
							if (dbo instanceof UISharedRegularComponent) {
								File iconFile = new File(getObject().getProject().getDirPath(), ((UISharedRegularComponent)dbo).getIconFileName());
								if (!iconFile.exists()) {
									Image image = ConvertigoPlugin.getDefault().getBeanIcon(dbo, BeanInfo.ICON_COLOR_32x32);
									ImageLoader saver = new ImageLoader();
									saver.data = new ImageData[] { image.getImageData() };
									saver.save(iconFile.getCanonicalPath(), SWT.IMAGE_PNG);
								}
							}
							// a UIDynamicInvoke has been added to this app
							if (dbo instanceof UIDynamicInvoke) {
								UIDynamicInvoke uidi = (UIDynamicInvoke)dbo;
								String compQName = uidi.getSharedActionQName();
								if (!compQName.isEmpty()) {
									ComponentRefManager.get(Mode.use).addConsumer(compQName, uidi.getQName());
								}
							}
							// a UIUseShared has been added to this app
							if (dbo instanceof UIUseShared) {
								UIUseShared uius = (UIUseShared)dbo;
								String compQName = uius.getSharedComponentQName();
								if (!compQName.isEmpty()) {
									ComponentRefManager.get(Mode.use).addConsumer(compQName, uius.getQName());
								}
							}
						} else {
							// an external shared action has changed and is used in this app
							UIActionStack uias = ((UIComponent)dbo).getSharedAction();
							if (uias != null && uias.isEnabled()) {
								if (ComponentRefManager.isCompUsedBy(uias.getQName(), projectName)) {
									doUpdate = true;
								}
							}
							// an external shared component has changed and is used in this app
							UISharedComponent uisc = ((UIComponent)dbo).getSharedComponent();
							if (uisc != null && uisc.isEnabled()) {
								if (ComponentRefManager.isCompUsedBy(uisc.getQName(), projectName)) {
									doUpdate = true;
								}
							}
						}
					}
				}
				
				if (dbo.bNew && (doto.isChildOf(this) || doUpdate)) {
					if (!done.add(getObject())) {
						return;
					}
					getObject().reset();
					resetMainScriptComponents(dbo);
					getObject().updateSourceFiles();
				}				
			} catch (Exception e) {}
		}
	}
	
	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		Set<Object> done = checkDone(treeObjectEvent);
		
		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "" : propertyName);
		
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject doto = (DatabaseObjectTreeObject)treeObject;
			DatabaseObject dbo = doto.getObject();
			
			try {
//				ApplicationComponent ac = getObject();
//				
//				// for Page or Menu or Component or Action
//				if (ac.equals(dbo.getParent())) {
//					markApplicationAsDirty(done);
//					
//					if (propertyName.equals("name")) {
//						String oldName = (String)oldValue;
//						String newName = (String)newValue;
//						
//						boolean fromSameProject = getProjectTreeObject().equals(doto.getProjectTreeObject());
//						if ((treeObjectEvent.update == TreeObjectEvent.UPDATE_ALL) 
//							|| ((treeObjectEvent.update == TreeObjectEvent.UPDATE_LOCAL) && fromSameProject)) {
//							
//							if (dbo instanceof UISharedRegularComponent) {
//								UISharedRegularComponent uisc = (UISharedRegularComponent)dbo;
//								try {
//									File oldIconFile = new File(ac.getProject().getDirPath(), uisc.getIconFileName(oldName));
//									File newIconFile = new File(ac.getProject().getDirPath(), uisc.getIconFileName(newName));
//									if (oldIconFile.exists() && !newIconFile.exists()) {
//										oldIconFile.renameTo(newIconFile);
//									}
//								} catch (Exception e) {}
//							}
//						}
//					}
//				}
//				// for any component inside a route
//				else if (ac.equals(dbo.getParent().getParent())) {
//					markApplicationAsDirty(done);
//				}
//				// for any UI component inside a menu or a stack
//				else if (dbo instanceof UIComponent) {
//					UIComponent uic = (UIComponent)dbo;
//					
//					UIDynamicMenu menu = uic.getMenu();
//					if (menu != null) {
//						if (ac.equals(menu.getParent())) {
//							markApplicationAsDirty(done);
//						}
//					}
//				}
//				// for this application
//				else if (this.equals(doto)) {
//					if (propertyName.equals("isPWA")) {
//						if (!newValue.equals(oldValue)) {
//							markPwaAsDirty();
//						}
//					} else if (propertyName.equals("componentScriptContent")) {
//						if (!newValue.equals(oldValue)) {
//							markComponentTsAsDirty();
//							markApplicationAsDirty(done);
//						}
//					} else if (propertyName.equals("useClickForTap")) {
//						for (TreeObject to: this.getChildren()) {
//							if (to instanceof ObjectsFolderTreeObject) {
//								ObjectsFolderTreeObject ofto = (ObjectsFolderTreeObject)to;
//								if (ofto.folderType == ObjectsFolderTreeObject.FOLDER_TYPE_PAGES) {
//									for (TreeObject cto: ofto.getChildren()) {
//										if (cto instanceof NgxPageComponentTreeObject) {
//											((NgxPageComponentTreeObject)cto).markPageAsDirty(done);
//										}
//									}
//								}
//							}
//						}
//						markApplicationAsDirty(done);
//					} else if (propertyName.equals("tplProjectName")) {
//						// close app editor and reinitialize builder
//						Project project = ac.getProject();
//						Engine.logStudio.info("tplProjectName property of " + project + " changed, reloading builder...");
//						closeAllEditors(false);
//						MobileBuilder.releaseBuilder(project);
//						MobileBuilder.initBuilder(project);
//						
//						IProject iproject = ConvertigoPlugin.getDefault().getProjectPluginResource(project.getName());
//						iproject.refreshLocal(IResource.DEPTH_INFINITE, null);
//						
//						// force app sources regeneration
//						for (TreeObject to: this.getChildren()) {
//							if (to instanceof ObjectsFolderTreeObject) {
//								ObjectsFolderTreeObject ofto = (ObjectsFolderTreeObject)to;
//								if (ofto.folderType == ObjectsFolderTreeObject.FOLDER_TYPE_PAGES) {
//									for (TreeObject cto: ofto.getChildren()) {
//										if (cto instanceof NgxPageComponentTreeObject) {
//											((NgxPageComponentTreeObject)cto).markPageAsDirty(done);
//										}
//									}
//								}
//							}
//						}
//						markApplicationAsDirty(done);
//						
//						// delete node modules and alert user
//						final File nodeModules = new File(project.getDirPath(), "/_private/ionic/node_modules");
//						if (nodeModules.exists()) {
//							ProgressMonitorDialog dialog = new ProgressMonitorDialog(ConvertigoPlugin.getMainShell());
//							dialog.open();
//							dialog.run(true, false, new IRunnableWithProgress() {
//								@Override
//								public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//									monitor.beginTask("deleting node modules", IProgressMonitor.UNKNOWN);
//									String alert = "template changed!";
//									if (com.twinsoft.convertigo.engine.util.FileUtils.deleteQuietly(nodeModules)) {
//										alert = "You have just changed the template.\nPackages have been deleted and will be reinstalled next time you run your application again.";
//									} else {
//										alert = "You have just changed the template: packages could not be deleted!\nDo not forget to reinstall the packages before running your application again, otherwise it may be corrupted!";
//									}
//									monitor.done();
//									ConvertigoPlugin.infoMessageBox(alert);
//								}
//							});
//						}
//						
//					} else {
//						markApplicationAsDirty(done);
//					}
//				}
				
				boolean doUpdate = false;
				String projectName = getObject().getProject().getName();
				if (doto.equals(this)) {
					// application tpl has changed
					if (propertyName.equals("tplProjectName")) {
						Engine.logStudio.info("tplProjectName property of " + projectName + " changed, reloading builder...");
						
						// close app editor and reinitialize builder
						Project project = getObject().getProject();
						closeAllEditors(false);
						MobileBuilder.releaseBuilder(project);
						MobileBuilder.initBuilder(project);
						
						// refresh resources
						IProject iproject = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
						iproject.refreshLocal(IResource.DEPTH_INFINITE, null);
						
						// delete node modules and alert user
						final File nodeModules = new File(getObject().getProject().getDirPath(), "/_private/ionic/node_modules");
						if (nodeModules.exists()) {
							ProgressMonitorDialog dialog = new ProgressMonitorDialog(ConvertigoPlugin.getMainShell());
							dialog.open();
							dialog.run(true, false, new IRunnableWithProgress() {
								@Override
								public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
									monitor.beginTask("deleting node modules", IProgressMonitor.UNKNOWN);
									String alert = "template changed!";
									if (com.twinsoft.convertigo.engine.util.FileUtils.deleteQuietly(nodeModules)) {
										alert = "You have just changed the template.\nPackages have been deleted and will be reinstalled next time you run your application again.";
									} else {
										alert = "You have just changed the template: packages could not be deleted!\nDo not forget to reinstall the packages before running your application again, otherwise it may be corrupted!";
									}
									monitor.done();
									ConvertigoPlugin.infoMessageBox(alert);
								}
							});
						}
						return;
					}
				}
				if (dbo instanceof UIComponent) {
					if (doto.isChildOf(this)) {
						if (dbo instanceof UISharedRegularComponent || dbo instanceof UIActionStack) {
							// a shared component or shared action of this app changed its name
							if (propertyName.equals("name")) {
								String oldName = (String)oldValue;
								String newName = (String)newValue;
								
								// modify consumers
								ComponentRefManager.get(Mode.use).copyKey(oldName, newName);
								
								// rename shared component icon file
								if (dbo instanceof UISharedRegularComponent) {
									UISharedRegularComponent uisc = (UISharedRegularComponent)dbo;
									try {
										File oldIconFile = new File(getObject().getProject().getDirPath(), uisc.getIconFileName(oldName));
										File newIconFile = new File(getObject().getProject().getDirPath(), uisc.getIconFileName(newName));
										if (oldIconFile.exists() && !newIconFile.exists()) {
											oldIconFile.renameTo(newIconFile);
										}
									} catch (Exception e) {}
								}
							}
						}
						
						if (dbo instanceof UIDynamicInvoke) {
							UIDynamicInvoke uidi = (UIDynamicInvoke)dbo;
							String useQName = uidi.getQName();
							// a UIDynamicInvoke of this app changed its target shared component
							if (propertyName.equals("stack")) {
								String oldCompQName = (String) oldValue;
								String newCompQName = (String) newValue;
								if (!oldCompQName.isEmpty()) {
									ComponentRefManager.get(Mode.use).removeConsumer(oldCompQName, useQName);
								}
								if (!newCompQName.isEmpty()) {
									ComponentRefManager.get(Mode.use).addConsumer(newCompQName, useQName);
								}
							}
							// a UIDynamicInvoke of this app changed its enablement
							if (propertyName.equals("isEnabled")) {
								boolean oldEnabled = (Boolean) oldValue;
								boolean newEnabled = (Boolean) newValue;
								String compQName = uidi.getSharedActionQName();
								if (!compQName.isEmpty() && !oldEnabled && newEnabled) {
									ComponentRefManager.get(Mode.use).addConsumer(compQName, useQName);
								}
								if (!compQName.isEmpty() && oldEnabled && !newEnabled) {
									ComponentRefManager.get(Mode.use).removeConsumer(compQName, useQName);
								}
							}
						}
						if (dbo instanceof UIUseShared) {
							UIUseShared uius = (UIUseShared)dbo;
							String useQName = uius.getQName();
							// a UIUseShared of this app changed its target shared component
							if (propertyName.equals("sharedcomponent")) {
								String oldCompQName = (String) oldValue;
								String newCompQName = (String) newValue;
								if (!oldCompQName.isEmpty()) {
									ComponentRefManager.get(Mode.use).removeConsumer(oldCompQName, useQName);
								}
								if (!newCompQName.isEmpty()) {
									ComponentRefManager.get(Mode.use).addConsumer(newCompQName, useQName);
								}
							}
							// a UIUseShared of this app changed its enablement
							if (propertyName.equals("isEnabled")) {
								boolean oldEnabled = (Boolean) oldValue;
								boolean newEnabled = (Boolean) newValue;
								String compQName = uius.getSharedComponentQName();
								if (!compQName.isEmpty() && !oldEnabled && newEnabled) {
									ComponentRefManager.get(Mode.use).addConsumer(compQName, useQName);
								}
								if (!compQName.isEmpty() && oldEnabled && !newEnabled) {
									ComponentRefManager.get(Mode.use).removeConsumer(compQName, useQName);
								}
							}
						}
					} else {
						// an external shared action has changed and is used in this app
						UIActionStack uias = ((UIComponent)dbo).getSharedAction();
						if (uias != null) {
							if (ComponentRefManager.isCompUsedBy(uias.getQName(), projectName)) {
								doUpdate = true;
							}
						}
						// an external shared component has changed and is used in this app
						UISharedComponent uisc = ((UIComponent)dbo).getSharedComponent();
						if (uisc != null) {
							if (ComponentRefManager.isCompUsedBy(uisc.getQName(), projectName)) {
								doUpdate = true;
							}
						}
					}
				}
				
				if (doto.equals(this) || doto.isChildOf(this) || doUpdate) {
					if (!done.add(getObject())) {
						return;
					}
					getObject().reset();
					resetMainScriptComponents(dbo);
					if (oldValue != null && newValue != null) {
						getObject().updateSourceFiles();
					}
				}
			} catch (Exception e) {}
		}
	}
	
	private static void resetMainScriptComponents(DatabaseObject dbo) {
		resetMainScriptComponents(dbo, 0);
	}
	
	private static void resetMainScriptComponents(DatabaseObject dbo, int level) {
		try {
			if (dbo != null) {
				if (dbo instanceof ApplicationComponent) {
					ApplicationComponent app = (ApplicationComponent)dbo;
					if (!app.isReset()) {
						app.reset();
						Engine.logEngine.debug("Application "+ app.getName() + " has been reset");
						return;
					}
				} else if (dbo instanceof PageComponent) {
					PageComponent page = (PageComponent)dbo;
					if (!page.isReset()) {
						page.reset();
						Engine.logEngine.debug("PageComponent "+ page.getName() + " has been reset");
						return;
					}
				} else if (dbo instanceof UIComponent) {
					UIComponent uic = (UIComponent)dbo;
					IScriptComponent main = uic.getMainScriptComponent();
					if (main != null) {
						if (main instanceof ApplicationComponent) {
							ApplicationComponent app = (ApplicationComponent)main;
							if (!app.isReset()) {
								app.reset();
								Engine.logEngine.debug("Application "+ app.getName() + " has been reset");
							}
						} else if (main instanceof PageComponent) {
							PageComponent page = (PageComponent)main;
							if (!page.isReset()) {
								page.reset();
								Engine.logEngine.debug("PageComponent "+ page.getName() + " has been reset");
							}
						} else if (main instanceof UISharedComponent) {
							UISharedComponent uisc = (UISharedComponent)main;
							if (!uisc.isReset()) {
								uisc.reset();
								Engine.logEngine.debug("UISharedComponent "+ uisc.getName() + " has been reset");
							}
						}
					}
					
					if (level == 1) {
						return;
					}
					
					// reset direct UIDynamicInvoke components
					UIActionStack uias = uic.getSharedAction();
					if (uias != null) {
						for (String useQName: ComponentRefManager.getCompConsumers(uias.getQName())) {
							resetMainScriptComponents(ComponentRefManager.getDatabaseObjectByQName(useQName), 1);
						}
					}
					// reset direct UIUseShared components
					UISharedComponent uisc = uic.getSharedComponent();
					if (uisc != null) {
						for (String useQName: ComponentRefManager.getCompConsumers(uisc.getQName())) {
							resetMainScriptComponents(ComponentRefManager.getDatabaseObjectByQName(useQName), 1);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	@Override
	public void hasBeenModified(boolean bModified) {
		super.hasBeenModified(bModified);
	}
	
//	protected void markComponentTsAsDirty() {
//		ApplicationComponent ac = getObject();
//		try {
//			ac.markComponentTsAsDirty();
//		} catch (EngineException e) {
//			ConvertigoPlugin.logException(e,
//					"Error while writing the app.component.ts for application '" + ac.getName() + "'");	}
//	}
//	
//	protected void markApplicationAsDirty(Set<Object> done) {
//		ApplicationComponent ac = getObject();
//		if (!done.add(ac)) {
//			return;
//		}
//		//System.out.println("---markApplicationAsDirty, with done : '" + done + "'");
//		try {
//			ac.markApplicationAsDirty();
//		} catch (EngineException e) {
//			ConvertigoPlugin.logException(e,
//					"Error while writing the application source files for '" + ac.getName() + "'");	}
//	}
//	
//	protected void markPwaAsDirty() {
//		ApplicationComponent ac = getObject();
//		try {
//			ac.markPwaAsDirty();
//		} catch (EngineException e) {
//			ConvertigoPlugin.logException(e,
//					"Error while writing the application PWA state for '" + ac.getName() + "'");	}
//	}

	public void editAppComponentTsFile() {
		final ApplicationComponent application = getObject();
		try {
			// Refresh project resource
			String projectName = application.getProject().getName();
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			
			// Close editor
			String filePath = application.getProject().getMobileBuilder().getTempTsRelativePath(application);
			IFile file = project.getFile(filePath);
			closeComponentFileEditor(file);
			
			// Write temporary file
			application.getProject().getMobileBuilder().writeAppComponentTempTs(application);
			file.refreshLocal(IResource.DEPTH_ZERO, null);
			
			// Open file in editor
			if (file.exists()) {
				IEditorInput input = new ComponentFileEditorInput(file, application);
				if (input != null) {
					IEditorDescriptor desc = PlatformUI
							.getWorkbench()
							.getEditorRegistry()
							.getDefaultEditor(file.getName());
					
					IWorkbenchPage activePage = PlatformUI
							.getWorkbench()
							.getActiveWorkbenchWindow()
							.getActivePage();
	
					String editorId = desc.getId();
					
					IEditorPart editorPart = activePage.openEditor(input, editorId);
					addMarkers(file, editorPart);
					editorPart.addPropertyListener(new IPropertyListener() {
						boolean isFirstChange = false;
						
						@Override
						public void propertyChanged(Object source, int propId) {
							if (source instanceof ITextEditor) {
								if (propId == IEditorPart.PROP_DIRTY) {
									if (!isFirstChange) {
										isFirstChange = true;
										return;
									}
									
									isFirstChange = false;
									ITextEditor editor = (ITextEditor)source;
									IDocumentProvider dp = editor.getDocumentProvider();
									IDocument doc = dp.getDocument(editor.getEditorInput());
									FormatedContent componentScriptContent = new FormatedContent(MobileBuilder.getMarkers(doc.get()));
									NgxApplicationComponentTreeObject.this.setPropertyValue("componentScriptContent", componentScriptContent);
								}
							}
						}
					});
				}			
			}
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to open typescript file for page '" + application.getName() + "'!");
		}
	}
	
	@Override
	public void launchEditor(String editorType) {
		activeEditor();
	}
	
	@Override
	public void closeAllEditors(boolean save) {
		super.closeAllEditors(save);// will close any child component editor
		
		ApplicationComponent application = (ApplicationComponent) getObject();
		synchronized (application) {
			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (activePage != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i = 0; i < editorRefs.length; i++) {
					IEditorReference editorRef = (IEditorReference) editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if (editorInput != null && editorInput instanceof ApplicationComponentEditorInput) {
							if (((ApplicationComponentEditorInput) editorInput).is(application)) {
								activePage.closeEditor(editorRef.getEditor(false),false);
							}
						}
					} catch(Exception e) {
						
					}
				}
			}
		}
	}

	public ApplicationComponentEditor activeEditor() {
		return activeEditor(true);
	}
	
	public ApplicationComponentEditor activeEditor(boolean autoLaunch) {
		ApplicationComponentEditor editorPart = null;
		ApplicationComponent application = (ApplicationComponent) getObject();
		
		synchronized (application) {
			String tpl = application.getTplProjectName();
			try {
				if (StringUtils.isBlank(tpl) || Engine.theApp.databaseObjectsManager.getOriginalProjectByName(tpl, false) == null) {
					throw new InvalidParameterException("The value '" + tpl + "' of the property 'Template project' from '" + application.getQName() + "' is incorrect.");
				}

				IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				if (activePage != null) {
					IEditorReference[] editorRefs = activePage.getEditorReferences();
					for (int i = 0; i < editorRefs.length; i++) {
						IEditorReference editorRef = (IEditorReference) editorRefs[i];
						try {
							IEditorInput editorInput = editorRef.getEditorInput();
							if ((editorInput != null) && (editorInput instanceof ApplicationComponentEditorInput)) {
								if (((ApplicationComponentEditorInput) editorInput).is(application)) {
									editorPart = (ApplicationComponentEditor) editorRef.getEditor(false);
								}
							}
						} catch(PartInitException e) {

						}
					}

					if (editorPart != null) {
						activePage.activate(editorPart);
					} else {
						IEditorPart editor = activePage.openEditor(new ApplicationComponentEditorInput(application, autoLaunch),
								"com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditor");
						if (editor instanceof ApplicationComponentEditor) {
							editorPart = (ApplicationComponentEditor) editor;
						} else {
							ConvertigoPlugin.logWarning("The Application Component Editor won't open, please see the error log.");
						}
					}
				}
			} catch (Exception e) {
				ConvertigoPlugin.logException(e,
						"Error while loading the page editor '"
								+ application.getName() + "'");
			}
		}
		return editorPart;
	}
	
	@Override
	protected List<PropertyDescriptor> getDynamicPropertyDescriptors() {
		List<PropertyDescriptor> l = super.getDynamicPropertyDescriptors();
		PropertyDescriptor pd = new PropertyDescriptor(P_TPL_VERSION, "Template version");
		pd.setDescription("The project's ionicTpl version.");
		pd.setCategory("Information");
		l.add(pd);
		return l;
	}

	@Override
	public Object getPropertyValue(Object id) {
		if (P_TPL_VERSION.equals(id)) {
			return getObject().getTplProjectVersion();
		}
		return super.getPropertyValue(id);
	}	
	
}
