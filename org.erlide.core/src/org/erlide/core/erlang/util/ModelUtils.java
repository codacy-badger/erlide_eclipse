package org.erlide.core.erlang.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.erlide.backend.util.StringUtils;
import org.erlide.core.erlang.ErlModelException;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlElement;
import org.erlide.core.erlang.IErlElement.Kind;
import org.erlide.core.erlang.IErlExternal;
import org.erlide.core.erlang.IErlFunction;
import org.erlide.core.erlang.IErlImport;
import org.erlide.core.erlang.IErlModel;
import org.erlide.core.erlang.IErlModule;
import org.erlide.core.erlang.IErlModuleMap;
import org.erlide.core.erlang.IErlPreprocessorDef;
import org.erlide.core.erlang.IErlProject;
import org.erlide.core.erlang.IErlTypespec;
import org.erlide.core.erlang.IOpenable;
import org.erlide.core.erlang.IParent;
import org.erlide.core.erlang.ISourceRange;
import org.erlide.core.erlang.SourceRange;
import org.erlide.jinterface.backend.Backend;
import org.erlide.jinterface.backend.BackendException;
import org.erlide.jinterface.util.ErlLogger;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangLong;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangRangeException;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.google.common.collect.Lists;

import erlang.ErlideOpen;

public class ModelUtils {

    public static final String EXTERNAL_FILES_PROJECT_NAME = "External_Files";

    private static final String DELIMITER = "<>";

    /**
     * Try to find include file, by searching include paths in the project
     * (replacing with path variables if needed). If the file is not in the
     * include paths, the original path is returned
     * 
     * @param project
     *            the project with include dirs
     * @param filePath
     *            the path to the include file
     * @param externalIncludes
     * @return the path to the include file
     * @throws BackendException
     * @throws CoreException
     */
    public static String findIncludeFile(final IErlProject project,
            final String filePath, final String externalIncludes)
            throws CoreException, BackendException {
        if (project == null) {
            return filePath;
        }
        for (final IErlModule module : project.getModules()) {
            final List<IErlModule> allIncludedFiles = module
                    .findAllIncludedFiles(externalIncludes);
            for (final IErlModule includeFile : allIncludedFiles) {
                if (includeFile.getFilePath().equals(filePath)
                        || includeFile.getName().equals(filePath)) {
                    return includeFile.getFilePath();
                }
            }
        }
        return filePath;
    }

    public static IErlTypespec findTypespec(final IErlModule module,
            final String name, final String externalIncludes)
            throws CoreException, BackendException {
        IErlTypespec typespec = module.findTypespec(name);
        if (typespec != null) {
            return typespec;
        }
        final List<IErlModule> includedFiles = module
                .findAllIncludedFiles(externalIncludes);
        for (final IErlModule includedFile : includedFiles) {
            typespec = includedFile.findTypespec(name);
            if (typespec != null) {
                return typespec;
            }
        }
        return null;
    }

    public static IErlModule findExternalModuleFromPath(final String path)
            throws CoreException {
        final Collection<IErlElement> children = ErlangCore.getModel()
                .getChildren();
        for (final IErlElement child : children) {
            if (child instanceof IErlProject) {
                final IErlProject project = (IErlProject) child;
                final IErlModule module = findExternalModuleFromPath(path,
                        project);
                if (module != null) {
                    return module;
                }
            }
        }
        return null;
    }

    public static IErlModule findExternalModuleFromPath(final String path,
            final IErlProject project) throws CoreException {
        final Collection<IErlModule> modules = project.getExternalModules();
        for (final IErlModule module : modules) {
            final String filePath = module.getFilePath();
            if (filePath != null && path != null
                    && StringUtils.equalFilePaths(path, filePath)) {
                return module;
            }
        }
        return null;
    }

    public static List<IErlModule> findExternalModulesFromName(
            final String moduleName, final IErlProject project)
            throws CoreException {
        final List<IErlModule> result = Lists.newArrayList();
        final Collection<IErlModule> modules = project.getExternalModules();
        for (final IErlModule module : modules) {
            if (module.getModuleName().equals(moduleName)
                    || module.getName().equals(moduleName)) {
                result.add(module);
            }
        }
        return result;
    }

    public static boolean isExternalFilesProject(final IProject project) {
        return project.getName().equals(EXTERNAL_FILES_PROJECT_NAME);
    }

    public static String getExternalModulePath(final IErlModule module) {
        final List<String> result = Lists.newArrayList();
        IErlElement element = module;
        final IErlModel model = ErlangCore.getModel();
        while (element != model) {
            if (element instanceof IErlExternal) {
                final IErlExternal external = (IErlExternal) element;
                result.add(external.getExternalName());
            } else {
                result.add(element.getName());
            }
            element = (IErlElement) element.getParent();
        }
        return StringUtils.join(DELIMITER, Lists.reverse(result));
    }

    private static IErlExternal getElementWithExternalName(
            final IParent parent, final String segment)
            throws ErlModelException {
        for (final IErlElement i : parent.getChildrenOfKind(Kind.EXTERNAL)) {
            final IErlExternal external = (IErlExternal) i;
            if (external.getExternalName().equals(segment)) {
                return external;
            }
        }
        return null;
    }

    public static IErlModule getModuleFromExternalModulePath(
            final String modulePath) throws ErlModelException {
        final List<String> path = StringUtils.split(DELIMITER, modulePath);
        final IErlElement childNamed = ErlangCore.getModel().getChildNamed(
                path.get(0));
        if (childNamed instanceof IParent) {
            IParent parent = (IParent) childNamed;
            final int n = path.size() - 1;
            for (int i = 1;; i++) {
                if (parent == null) {
                    break;
                }
                if (parent instanceof IOpenable) {
                    final IOpenable openable = (IOpenable) parent;
                    openable.open(null);
                }
                if (i == n) {
                    break;
                }
                parent = getElementWithExternalName(parent, path.get(i));
            }
            if (parent != null) {
                final IErlElement child = parent.getChildNamed(path.get(n));
                if (child instanceof IErlModule) {
                    return (IErlModule) child;
                }
            }
        }
        return null;
    }

    public static List<String> getExternalModulesWithPrefix(final Backend b,
            final String prefix, final IErlProject erlProject)
            throws CoreException {
        final List<String> result = Lists.newArrayList();
        final Collection<IErlModule> modules = erlProject.getExternalModules();
        for (final IErlModule module : modules) {
            final String name = module.getModuleName();
            if (name.startsWith(prefix)) {
                result.add(name);
            }
        }
        return result;
    }

    public static IErlModule getExternalInclude(final Backend backend,
            final IErlProject project, final String externalIncludes,
            final ErlangIncludeFile element) throws BackendException,
            CoreException {
        String pathOrName = element.getFilename();
        if (element.isSystemInclude()) {
            pathOrName = ErlideOpen.getIncludeLib(backend, pathOrName);
        }
        final IErlModule module = findExternalModuleFromPath(pathOrName,
                project);
        if (module != null) {
            return module;
        }
        final IPath p = new Path(pathOrName);
        if (!p.isAbsolute()) {
            return getExternalModule(pathOrName, project);
        }
        return null;
    }

    public static String resolveMacroValue(final String definedName,
            final IErlModule m) {
        if ("?MODULE".equals(definedName)) {
            return m.getModuleName();
        }
        final IErlPreprocessorDef def = m.findPreprocessorDef(
                StringUtils.withoutInterrogationMark(definedName),
                Kind.MACRO_DEF);
        if (def != null) {
            final String extra = def.getExtra();
            final int p = extra.indexOf(',');
            if (p != -1) {
                final String s = extra.substring(p + 1).trim();
                if (s.length() > 0) {
                    return s;
                }
            }
        }
        return definedName;
    }

    public static IErlElement findExternalFunction(String moduleName,
            final ErlangFunction erlangFunction, final String modulePath,
            final IErlProject project, final boolean checkAllProjects,
            final IErlModule module) throws CoreException {
        if (moduleName != null) {
            moduleName = resolveMacroValue(moduleName, module);
            final IErlModule module2 = findExternalModule(moduleName,
                    modulePath, project, checkAllProjects);
            if (module2 != null) {
                module2.open(null);
                final IErlFunction function = module2
                        .findFunction(erlangFunction);
                if (function != null) {
                    return function;
                }
                return module2;
            }
        }
        return null;
    }

    public static IErlElement findExternalType(final IErlModule module,
            String moduleName, final String typeName, final String modulePath,
            final IErlProject project, final boolean checkAllProjects)
            throws CoreException {
        moduleName = resolveMacroValue(moduleName, module);
        final IErlModule module2 = findExternalModule(moduleName, modulePath,
                project, checkAllProjects);
        if (module2 != null) {
            module2.open(null);
            return module2.findTypespec(typeName);
        }
        return null;
    }

    public static IErlModule findExternalModule(final String moduleName,
            final String modulePath, final IErlProject project,
            final boolean checkAllProjects) throws CoreException {
        IErlModule module = getModuleByName(moduleName, modulePath, project);
        if (module == null) {
            final String moduleFileName;
            if (!ErlideUtil.hasModuleExtension(moduleName)) {
                moduleFileName = moduleName + ".erl";
            } else {
                moduleFileName = moduleName;
            }
            final IErlModule module2 = getExternalModule(moduleFileName,
                    project);
            if (module2 != null) {
                return module2;
            }
            IResource r = null;
            if (project != null) {
                r = ResourceUtil
                        .recursiveFindNamedModuleResourceWithReferences(
                                project.getProject(), moduleFileName,
                                PluginUtils.getSourcePathFilterCreator());

                if (r == null) {
                    if (checkAllProjects) {
                        final IWorkspaceRoot workspaceRoot = ResourcesPlugin
                                .getWorkspace().getRoot();
                        final IProject[] projects = workspaceRoot.getProjects();
                        for (final IProject p : projects) {
                            if (ErlideUtil.hasErlangNature(p)) {
                                ErlLogger.debug("searching project %s",
                                        p.getName());
                                r = ResourceUtil.recursiveFindNamedResource(p,
                                        moduleFileName,
                                        PluginUtils.getSourcePathFilter(p));
                                if (r != null) {
                                    ErlLogger.debug("found %s", r);
                                    break;
                                }
                            }
                        }
                    }
                    if (r == null) {
                        module = findExternalModuleFromPath(modulePath, project);
                    }
                }
            }
            if (r instanceof IFile) {
                module = ErlangCore.getModel().findModule((IFile) r);
            }
        }
        return module;
    }

    public static IErlModule getModuleByName(final String moduleName,
            final String modulePath, final IErlProject project) {
        final IErlModuleMap modelMap = ErlangCore.getModuleMap();
        final Set<IErlModule> modules = modelMap.getModulesByName(moduleName);
        if (modules != null) {
            for (final IErlModule module : modules) {
                if (moduleInProject(module, project)) {
                    final IParent parent = module.getParent();
                    if (parent instanceof IErlElement) {
                        final IErlElement element = (IErlElement) parent;
                        if (element.getKind() != Kind.EXTERNAL) {
                            return module;
                        }
                    }
                }
            }
            for (final IErlModule module : modules) {
                if (moduleInProject(module, project)) {
                    return module;
                }
            }
            if (modulePath != null) {
                final IErlModule module = modelMap.getModuleByPath(modulePath);
                if (module != null && moduleInProject(module, project)) {
                    return module;
                }
            }
        }
        return null;
    }

    public static IErlModule getExternalModule(final String moduleName,
            final IErlProject project) throws CoreException {
        final IErlModule module = getModuleByName(moduleName, null, project);
        if (module != null) {
            return module;
        }
        final List<IErlModule> modules = findExternalModulesFromName(
                moduleName, project);
        if (!modules.isEmpty()) {
            return modules.get(0);
        }
        return null;
    }

    public static IErlPreprocessorDef findPreprocessorDef(
            final Collection<IErlProject> projects, final String moduleName,
            final String definedName, final IErlElement.Kind kind,
            final String externalIncludes) throws CoreException,
            BackendException {
        for (final IErlProject project : projects) {
            if (project != null) {
                final IErlModule module = project.getModule(moduleName);
                if (module != null) {
                    final IErlPreprocessorDef def = findPreprocessorDef(module,
                            definedName, kind, externalIncludes);
                    if (def != null) {
                        return def;
                    }
                }
            }
        }
        return null;
    }

    public static IErlPreprocessorDef findPreprocessorDef(
            final IErlModule module, final String definedName,
            final IErlElement.Kind kind, final String externalIncludes)
            throws CoreException, BackendException {
        String unquoted = StringUtils.unquote(definedName);
        final Set<String> names = new HashSet<String>(3);
        if (kind == Kind.RECORD_DEF) {
            while (names.add(unquoted)) {
                unquoted = resolveMacroValue(unquoted, module);
            }
        } else {
            names.add(unquoted);
        }
        names.add(definedName);
        final List<IErlModule> allIncludedFiles = module
                .findAllIncludedFiles(externalIncludes);
        allIncludedFiles.add(0, module);
        for (final IErlModule includedFile : allIncludedFiles) {
            for (final String name : names) {
                includedFile.open(null);
                final IErlPreprocessorDef preprocessorDef = includedFile
                        .findPreprocessorDef(name, kind);
                if (preprocessorDef != null) {
                    return preprocessorDef;
                }
            }
        }
        return null;
    }

    public static List<OtpErlangObject> getImportsAsList(final IErlModule mod) {
        if (mod == null) {
            return NO_IMPORTS;
        }
        final Collection<IErlImport> imports = mod.getImports();
        if (imports.isEmpty()) {
            return NO_IMPORTS;
        }
        final List<OtpErlangObject> result = new ArrayList<OtpErlangObject>(
                imports.size());
        for (final IErlImport i : imports) {
            final Collection<ErlangFunction> functions = i.getFunctions();
            final OtpErlangObject funsT[] = new OtpErlangObject[functions
                    .size()];
            int j = 0;
            for (final ErlangFunction f : functions) {
                funsT[j] = f.getNameArityTuple();
                j++;
            }
            final OtpErlangTuple modFunsT = new OtpErlangTuple(
                    new OtpErlangObject[] {
                            new OtpErlangAtom(i.getImportModule()),
                            new OtpErlangList(funsT) });
            result.add(modFunsT);
        }
        return result;
    }

    public static List<IErlPreprocessorDef> getPreprocessorDefs(
            final IErlModule module, final IErlElement.Kind kind,
            final String externalIncludes) throws CoreException,
            BackendException {
        final List<IErlPreprocessorDef> result = Lists.newArrayList();
        final List<IErlModule> modulesWithIncludes = module
                .findAllIncludedFiles(externalIncludes);
        modulesWithIncludes.add(module);
        for (final IErlModule m : modulesWithIncludes) {
            result.addAll(m.getPreprocessorDefs(kind));
        }
        return result;
    }

    public static final ArrayList<OtpErlangObject> NO_IMPORTS = new ArrayList<OtpErlangObject>(
            0);

    public static List<IErlModule> getModulesWithReferencedProjectsWithPrefix(
            final IErlProject project, final String prefix)
            throws CoreException {
        final IErlModel model = ErlangCore.getModel();
        final List<IErlModule> result = new ArrayList<IErlModule>();
        if (project == null) {
            return result;
        }
        project.open(null);
        addModulesWithPrefix(prefix, result, project.getModules());
        for (final IProject p : project.getProject().getReferencedProjects()) {
            final IErlProject ep = model.findProject(p);
            if (ep != null) {
                ep.open(null);
                addModulesWithPrefix(prefix, result, ep.getModules());
            }
        }
        return result;
    }

    private static void addModulesWithPrefix(final String prefix,
            final List<IErlModule> result, final Collection<IErlModule> modules) {
        for (final IErlModule module : modules) {
            if (module.getModuleName().startsWith(prefix)) {
                result.addAll(modules);
            }
        }
    }

    public static String[] getPredefinedMacroNames() {
        return new String[] { "MODULE", "LINE", "FILE" };
    }

    public static ISourceRange findVariable(final Backend backend,
            final ISourceRange range, final String variableName,
            final String elementText) throws OtpErlangRangeException {
        final OtpErlangTuple res2 = ErlideOpen.findFirstVar(backend,
                variableName, elementText);
        if (res2 != null) {
            final int relativePos = ((OtpErlangLong) res2.elementAt(0))
                    .intValue() - 1;
            final int length = ((OtpErlangLong) res2.elementAt(1)).intValue();
            final int start = relativePos + range.getOffset();
            return new SourceRange(start, length);
        }
        return range;
    }

    public static boolean moduleInProject(final IErlModule module,
            final IErlProject project) {
        final IErlProject project2 = module.getProject();
        if (project == null) {
            return true;
        }
        if (project2 == null) {
            return false;
        }
        return project.equals(project2);
    }
}
