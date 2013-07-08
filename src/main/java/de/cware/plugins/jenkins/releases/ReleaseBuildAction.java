package de.cware.plugins.jenkins.releases;

import de.cware.plugins.jenkins.releases.versions.VersionHandler;

import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.Action;

import hudson.model.ParametersAction;
import hudson.util.RunList;
import net.sf.json.JSONObject;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * This action adds an additional menu entry to the Actions menu of a job for which the
 * ReleaseBuildWrapper has been turned on. Additionally it also handles the requests to
 * initiate a major or a minor release.
 *
 * User: cdutz
 * Date: 24.10.12
 * Time: 09:58
 */
public class ReleaseBuildAction implements Action {

    protected MavenModuleSet project;
    protected Map<String, MavenModule> modules;

    protected VersionHandler versionHandler;

    public ReleaseBuildAction(MavenModuleSet project) {
        this.project = project;

        modules = new HashMap<String, MavenModule>(project.getModules().size());
        for(final MavenModule module : project.getModules()) {
            modules.put(module.getModuleName().groupId + ":" + module.getModuleName().artifactId, module);
        }

        versionHandler = new VersionHandler(project);
    }

    public String getIconFileName() {
        return "/plugin/release-plugin/img/release.png";
    }

    public String getDisplayName() {
        return "Release Modules";
    }

    public String getUrlName() {
        return "release";
    }

    public MavenModuleSet getProject() {
        return project;
    }

    public List<MavenModule> getModules() {
        final List<MavenModule> modules = new ArrayList<MavenModule>();
        for (final MavenModule module : project.getModules()) {
            modules.add(module);
        }
        return modules;
    }

    /**
     * If the project root doesn't contain a pom.xml file, then the job has not been
     * built yet and we have to make sure it is checked out. In this case we have to
     * execute a build prior to being able to use the plugin.
     *
     * @return true if the project is checked out.
     */
    public boolean getInitialized() {
        final File projectWorkDir = getProjectWorkDir();
        if (projectWorkDir != null) {
            final File rootModuleWorkDir = new File(projectWorkDir,
                    project.getRootModule().getRelativePath());
            if (rootModuleWorkDir.exists()) {
                final File masterPom = new File(rootModuleWorkDir, "pom.xml");
                return masterPom.exists();
            }
        }
        return false;
    }

    /**
     * If the root contains a pom.xml.releaseBackup file, then a previous release
     * attempt has failed and we have to cleanup before starting another attempt.
     *
     * @return true, if a previous build failed and we need to cleanup.
     */
    public boolean getDirty() {
        final File projectWorkDir = getProjectWorkDir();
        if (projectWorkDir != null) {
            final File rootModuleWorkDir = new File(projectWorkDir,
                    project.getRootModule().getRelativePath());
            if (rootModuleWorkDir.exists()) {
                final File releaseProperties = new File(rootModuleWorkDir, "pom.xml.releaseBackup");
                return releaseProperties.exists();
            }
        }
        return false;
    }

    protected File getProjectWorkDir() {
        final RunList<MavenModuleSetBuild> builds = project.getBuilds();
        // This code assumes that the builds work dir stays the same during builds.
        final MavenModuleSetBuild build = builds.getLastBuild();

        // Convert the FilePath into a File object that we can actually use.
        if(build != null) {
            try {
                return new File(build.getWorkspace().toURI().getPath());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void doPerformInitialize(StaplerRequest request, StaplerResponse response) {
        try {
            final InitializeInterceptorAction action = new InitializeInterceptorAction();

            // Schedule the build.
            // This will make jenkins trigger the build. While performing the build all registered BuildWrappers
            // can contribute. Particularly the ReleaseBuildWrapper will react on the ReleaseCause cause class and
            // trigger the release build.
            //
            // Depending on the result of the scheduleBuild method the user is then redirected to the main page of
            // the current Job or redirected to a failure-page, if something went wrong.
            if(project.scheduleBuild(0, new ReleaseCause(), action)) {
                response.sendRedirect(request.getContextPath() + '/' + project.getUrl());
            } else {
                response.sendRedirect(request.getContextPath() + '/' + project.getUrl() + '/' +
                        getUrlName() + "/failure");
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void doPerformCleanup(StaplerRequest request, StaplerResponse response) {
        try {
            final CleanupInterceptorAction action = new CleanupInterceptorAction();

            // Schedule the build.
            // This will make jenkins trigger the build. While performing the build all registered BuildWrappers
            // can contribute. Particularly the ReleaseBuildWrapper will react on the ReleaseCause cause class and
            // trigger the release build.
            //
            // Depending on the result of the scheduleBuild method the user is then redirected to the main page of
            // the current Job or redirected to a failure-page, if something went wrong.
            if(project.scheduleBuild(0, new ReleaseCause(), action)) {
                response.sendRedirect(request.getContextPath() + '/' + project.getUrl());
            } else {
                response.sendRedirect(request.getContextPath() + '/' + project.getUrl() + '/' +
                        getUrlName() + "/failure");
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void doPerformMajorRelease(StaplerRequest request, StaplerResponse response) {
        try {
            // Action that saves the configuration settings that were active during the build and attaches this to
            // the build results, so in the future the information, which properties were active during a build is
            // still available.
            final ParametersAction parameters = new ParametersAction();

            final MajorReleaseInterceptorAction action =
                    new MajorReleaseInterceptorAction(versionHandler.getNextReleaseVersion(
                            project.getRootModule().getModuleName()));

            // Schedule the build.
            // This will make jenkins trigger the build. While performing the build all registered BuildWrappers
            // can contribute. Particularly the ReleaseBuildWrapper will react on the ReleaseCause cause class and
            // trigger the release build.
            //
            // Depending on the result of the scheduleBuild method the user is then redirected to the main page of
            // the current Job or redirected to a failure-page, if something went wrong.
            if(project.scheduleBuild(0, new ReleaseCause(), parameters, action)) {
                response.sendRedirect(request.getContextPath() + '/' + project.getUrl());
            } else {
                response.sendRedirect(request.getContextPath() + '/' + project.getUrl() + '/' +
                        getUrlName() + "/failure");
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void doPerformMinorRelease(StaplerRequest request, StaplerResponse response) {
        try {

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Prepare a list of inverse dependencies. So we can check that when releasing one module
            // all modules that depend on this are also released.
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////

            // First parse the root pom and build a maven model of this project.
            final File rootPomFile = new File(getProjectWorkDir(), "pom.xml");
            if(!rootPomFile.exists()) {
                response.sendRedirect(request.getContextPath() + '/' + project.getUrl() +
                        getUrlName() + "/failure?reason=couldntParseProjectRootPom");
                return;
            }

            // Prepare a list of all maven models of all artifacts this project consists of.
            final Map<String, Model> mavenModels = parseMavenModel(rootPomFile);

            // Prepare a map containing information about which modules depend on a particular artifact.
            final Map<String, List<String>> references = new HashMap<String, List<String>>();
            for(final String curModuleKey : mavenModels.keySet()) {
                final List<String> moduleReferences = prepareDependenciesMap(curModuleKey, mavenModels);
                for(final String dependencyKey : moduleReferences) {
                    if(mavenModels.containsKey(dependencyKey)) {
                        if(!references.containsKey(dependencyKey)) {
                            references.put(dependencyKey, new ArrayList<String>());
                        }
                        references.get(dependencyKey).add(curModuleKey);
                    }
                }
            }

            // Create a transitive closure of all dependencies, as processing the modules directly doesn't
            // directly handle transitive dependencies.
            final Map<String, List<String>> referencesClosure = new HashMap<String, List<String>>();
            for(final String curModuleKey : mavenModels.keySet()) {
                final List<String> closure = calculateDependencyClosure(curModuleKey, references);
                referencesClosure.put(curModuleKey, closure);
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Build a map of all modules that should be released.
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////

            final JSONObject form = request.getSubmittedForm();
            final boolean autoSelectMissingModules = form.getBoolean("autoSelectMissingModules");
            final JSONObject artifacts = form.getJSONObject("artifacts");

            // Get a list of all selected modules.
            final List<String> selectedModules = new ArrayList<String>();
            for(final String artifactName : (Collection<String>) artifacts.keySet()) {
                final JSONObject artifactSettings = artifacts.getJSONObject(artifactName);
                if(artifactSettings.getBoolean("release")) {
                    final String moduleKey = artifactSettings.getString("groupId") + ":" +
                            artifactSettings.getString("artifactId");
                    selectedModules.add(moduleKey);
                }
            }

            // Build a list of all missing modules because of dependencies.
            final List<String> missingModules = new ArrayList<String>();
            for(final String selectedModule : selectedModules) {
                if(referencesClosure.get(selectedModule) != null) {
                    for(final String referencingModule : referencesClosure.get(selectedModule)) {
                        if(!selectedModules.contains(referencingModule) && !missingModules.contains(referencingModule)) {
                            missingModules.add(referencingModule);
                        }
                    }
                }
            }

            // Additionally add any missing parent modules.
            for(final String moduleName : selectedModules) {
                final Model module = mavenModels.get(moduleName);
                if(module.getParent() != null) {
                    Parent parent = module.getParent();
                    while(parent != null) {
                        final String parentKey = parent.getGroupId() + ":" + parent.getArtifactId();

                        // Get the parent module.
                        final Model parentModule = mavenModels.get(parentKey);

                        // The parent is no longer part of this project, so we can abort.
                        if(parentModule == null) {
                            break;
                        }

                        // If the parent is part of this project and it is not part of the selection
                        // and has not yet been added to the missing modules list, then add it to that
                        // list.
                        if(!selectedModules.contains(parentKey) && !missingModules.contains(parentKey)) {
                            missingModules.add(parentKey);
                        }
                        parent = parentModule.getParent();
                    }
                }
            }

            // Depending on the value of autoSelectMissingModules add missing modules or cause the build to fail.
            if(!missingModules.isEmpty()) {
                // Automatically add all missing modules.
                if(autoSelectMissingModules) {
                    selectedModules.addAll(missingModules);
                }
                // Cause the build to fail.
                else {
                    final StringBuilder moduleList = new StringBuilder();
                    for(final String missingModuleKey : missingModules) {
                        if(moduleList.length() > 0) {
                            moduleList.append(",");
                        }
                        moduleList.append(missingModuleKey);
                    }
                    response.sendRedirect(request.getContextPath() + '/' + project.getUrl() +
                            getUrlName() + "/failure?reason=missingModules&moduleList=" + moduleList);
                    return;
                }
            }

            // Get the versions of all modules, depending on if they should be released or not.
            final Map<MavenModule, String> releaseVersions = new HashMap<MavenModule, String>();
            final Map<MavenModule, String> notReleaseVersions = new HashMap<MavenModule, String>();
            for(final String currentModule : mavenModels.keySet()) {
                final MavenModule module = modules.get(currentModule);
                if(selectedModules.contains(currentModule)) {
                    final JSONObject artifactSettings = artifacts.getJSONObject(module.getModuleName().artifactId);
                    final String releaseVersion = artifactSettings.getString("version");
                    releaseVersions.put(module, releaseVersion);
                } else {
                    final String latestVersion = versionHandler.getCurrentReleaseVersion(module.getModuleName());
                    notReleaseVersions.put(module, latestVersion);
                }
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////

            // Action that saves the configuration settings that were active during the build and attaches this to
            // the build results, so in the future the information, which properties were active during a build is
            // still available.
            final ParametersAction parameters = new ParametersAction();

            final MinorReleaseInterceptorAction action =
                    new MinorReleaseInterceptorAction(releaseVersions, notReleaseVersions);

            // Schedule the build.
            // This will make jenkins trigger the build. While performing the build all registered BuildWrappers
            // can contribute. Particularly the ReleaseBuildWrapper will react on the ReleaseCause cause class and
            // trigger the release build.
            //
            // Depending on the result of the scheduleBuild method the user is then redirected to the main page of
            // the current Job or redirected to a failure-page, if something went wrong.
            if(project.scheduleBuild(0, new ReleaseCause(), parameters, action)) {
                response.sendRedirect(request.getContextPath() + '/' + project.getUrl());
            } else {
                response.sendRedirect(request.getContextPath() + '/' + project.getUrl() +
                        getUrlName() + "/failure?reason=couldntSchedule");
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public VersionHandler getVersionHandler() {
        return versionHandler;
    }

    /**
     * @param curPomFile File reference to the current maven pom.
     * @return A map containing the Maven model for each module of this project.
     * @throws IOException
     * @throws XmlPullParserException
     */
    protected Map<String, Model> parseMavenModel(File curPomFile)
            throws IOException, XmlPullParserException {

        final Map<String, Model> result = new HashMap<String, Model>();

        // Read the pom file
        final MavenXpp3Reader reader = new MavenXpp3Reader();
        final Model model = reader.read(new FileReader(curPomFile));

        // Process the content.
        if(model != null) {
            // Get the module key from the model.
            final String moduleKey = model.getGroupId() + ":" + model.getArtifactId();
            // Save the model in the model-map.
            result.put(moduleKey, model);

            // If this module contains child modules, add all of them too.
            if(model.getModules() != null) {
                for(final String modulePath : model.getModules()) {
                    final File moduleDirectory = new File(curPomFile.getParentFile(), modulePath);
                    final File modulePomFile = new File(moduleDirectory, "pom.xml");
                    if(modulePomFile.exists()) {
                        result.putAll(parseMavenModel(modulePomFile));
                    }
                }
            }
        }

        return result;
    }


    /**
     * Initialize a map containing information about which artifact is used by which other artifacts.
     * This map represents an inverse view of the classical dependency logic.
     *
     * @param curModuleKey key of the currently active module.
     * @param modelMap map containing all Maven Models of all modules.
     * @return A map containing an inverse view of the normal maven dependencies.
     */
    protected List<String> prepareDependenciesMap(String curModuleKey, Map<String, Model> modelMap) {

        final List<String> result = new ArrayList<String>();

        final Model model = modelMap.get(curModuleKey);

        // Process the content.
        if(model != null) {
            for(final Dependency dependency : model.getDependencies()) {
                final String dependencyId = dependency.getGroupId() + ":" + dependency.getArtifactId();
                if(!result.contains(dependencyId)) {
                    result.add(dependencyId);
                }
            }
        }

        return result;
    }

    /**
     * Method for make sure that the dependencies contain all transitive dependencies.
     * @param curModuleKey current module key.
     * @param dependencies map of all non-transitive dependencies.
     * @return list of transitive dependencies.
     */
    protected List<String> calculateDependencyClosure(
            String curModuleKey, Map<String, List<String>> dependencies) {
        final List<String> result = new ArrayList<String>();

        if(dependencies.containsKey(curModuleKey)) {
            for(final String currentModuleKey : dependencies.get(curModuleKey)) {
                result.add(currentModuleKey);
                final List<String> dependencyClosure = calculateDependencyClosure(currentModuleKey, dependencies);
                for(final String dependencyKey : dependencyClosure) {
                    if(!result.contains(dependencyKey)) {
                        result.add(dependencyKey);
                    }
                }
            }
        }

        return result;
    }

}
