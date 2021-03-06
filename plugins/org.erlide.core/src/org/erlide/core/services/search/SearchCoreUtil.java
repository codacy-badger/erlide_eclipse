package org.erlide.core.services.search;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.erlide.engine.ErlangEngine;
import org.erlide.engine.model.ErlElementKind;
import org.erlide.engine.model.ErlModelException;
import org.erlide.engine.model.IErlElement;
import org.erlide.engine.model.IErlElement.AcceptFlags;
import org.erlide.engine.model.IParent;
import org.erlide.engine.model.erlang.SourceKind;
import org.erlide.engine.model.root.IErlExternal;
import org.erlide.engine.model.root.IErlModel;
import org.erlide.engine.model.root.IErlModule;
import org.erlide.engine.model.root.IErlProject;
import org.erlide.engine.services.search.ErlSearchScope;
import org.erlide.engine.util.NatureUtil;

import com.google.common.collect.Sets;

public class SearchCoreUtil {

    public static ErlSearchScope getProjectsScope(final Collection<IProject> projects,
            final boolean addExternals, final boolean addOtp) throws CoreException {
        final ErlSearchScope result = new ErlSearchScope();
        final Set<String> externalModulePaths = new HashSet<>();
        final IErlModel model = ErlangEngine.getInstance().getModel();
        for (final IProject project : projects) {
            SearchCoreUtil.addProjectToScope(project, result);
            if (NatureUtil.hasErlangNature(project)) {
                final IErlProject erlProject = model.getErlangProject(project);
                SearchCoreUtil.addExternalModules(erlProject, result, externalModulePaths,
                        addExternals, addOtp);
            }
        }
        return result;
    }

    static void addProjectToScope(final IProject project, final ErlSearchScope result)
            throws CoreException {
        if (project == null) {
            return;
        }
        final IErlProject erlProject = ErlangEngine.getInstance().getModel()
                .getErlangProject(project);
        if (erlProject != null) {
            final Collection<IPath> sourcePaths = erlProject.getProperties()
                    .getSourceDirs();
            for (final IPath path : sourcePaths) {
                final IFolder folder = project.getFolder(path);
                SearchCoreUtil.addFolderToScope(folder, result);
            }
        }
    }

    static void addFolderToScope(final IFolder folder, final ErlSearchScope result)
            throws CoreException {
        if (folder != null) {
            for (final IResource r : folder.members()) {
                if (r instanceof IFile) {
                    final IFile f = (IFile) r;
                    SearchCoreUtil.addFileToScope(f, result);
                }
            }
        }
    }

    static void addFileToScope(final IFile file, final ErlSearchScope result) {
        if (SourceKind.hasModuleExtension(file.getName())) {
            final IErlModule module = ErlangEngine.getInstance().getModel()
                    .findModule(file);
            result.addModule(module);
        }
    }

    public static void addExternalModules(final IParent element,
            final ErlSearchScope result, final Set<String> externalModulePaths,
            final boolean addExternals, final boolean addOtp) throws ErlModelException {
        final Collection<IErlElement> externals = element.getChildrenOfKind(
                ErlElementKind.EXTERNAL_ROOT, ErlElementKind.EXTERNAL_APP,
                ErlElementKind.EXTERNAL_FOLDER);
        for (final IErlElement external : externals) {
            external.accept(theElement -> {
                if (theElement instanceof IErlExternal) {
                    final IErlExternal theExternal = (IErlExternal) theElement;
                    if (theExternal.isOTP()) {
                        if (!addOtp) {
                            return false;
                        }
                    } else {
                        if (!addExternals) {
                            return false;
                        }
                    }
                    theExternal.open(null);
                }
                if (theElement instanceof IErlModule) {
                    final IErlModule module = (IErlModule) theElement;
                    if (externalModulePaths.add(module.getFilePath())) {
                        result.addModule(module);
                    }
                }
                return true;
            }, EnumSet.noneOf(AcceptFlags.class), ErlElementKind.MODULE);
        }
    }

    public static void addResourceToScope(final ErlSearchScope result, final IResource r)
            throws CoreException {
        if (r instanceof IProject) {
            final IProject project = (IProject) r;
            SearchCoreUtil.addProjectToScope(project, result);
        } else if (r instanceof IFile) {
            final IFile file = (IFile) r;
            SearchCoreUtil.addFileToScope(file, result);
        } else if (r instanceof IFolder) {
            final IFolder folder = (IFolder) r;
            SearchCoreUtil.addFolderToScope(folder, result);
        }
    }

    public static ErlSearchScope getWorkspaceScope(final boolean addExternals,
            final boolean addOtp) throws ErlModelException {
        final ErlSearchScope result = new ErlSearchScope();
        final Collection<IErlProject> erlangProjects = ErlangEngine.getInstance()
                .getModel().getErlangProjects();
        for (final IErlProject i : erlangProjects) {
            final Collection<IErlModule> modules = i.getModulesAndIncludes();
            for (final IErlModule j : modules) {
                result.addModule(j);
            }
            // addProjectEbin(i, result);
        }
        final Set<String> externalModulePaths = new HashSet<>();
        for (final IErlProject project : erlangProjects) {
            SearchCoreUtil.addExternalModules(project, result, externalModulePaths,
                    addExternals, addOtp);
        }
        return result;
    }

    public static Collection<IProject> getProjects(final String[] projectNames) {
        final Collection<IProject> result = Sets
                .newHashSetWithExpectedSize(projectNames.length);
        final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        for (final String i : projectNames) {
            final IProject project = root.getProject(i);
            result.add(project);
        }
        return result;
    }

    public static boolean isLineDelimiterChar(final char ch) {
        return ch == '\n' || ch == '\r';
    }

}
