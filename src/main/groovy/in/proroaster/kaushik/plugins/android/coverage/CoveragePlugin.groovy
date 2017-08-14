package in.proroaster.kaushik.plugins.android.coverage

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * Created by kaushik on 8/13/17.
 */
class CoveragePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        validateProjectIsAndroidProject(project)
        validateJacocoUsedForCoverage(project)
        project.extensions.add("coverage", CoverageExtension.class)

        UnitTestConfiguration unitTestConfiguration = new UnitTestConfiguration()
        unitTestConfiguration.createUnitTestCoverageTasks(project)
    }

    void addCoverageTasks(Project project) {
        project.afterEvaluate {
            def buildTypesNames = project.android.buildTypes.collect { buildType ->
                buildType.name
            }
            def productFlavorNames = project.android.productFlavors.collect { productFlavor ->
                productFlavor.name
            }
            //In case of no product flavors are defined
            if (!productFlavorNames) {
                productFlavorNames.add('')
            }
            productFlavorNames.each { flavor ->
                buildTypesNames.each { buildType ->
                    def variant, variantSourceSet
                    if (!flavor) {
                        variant = "${buildType}"
                        variantSourceSet = "${buildType}"
                    } else {
                        variant = "${flavor}${buildType.capitalize()}"
                        variantSourceSet = "${flavor}/${buildType}"
                    }
                    def unitTestTask = "test${variant.capitalize()}UnitTest"
                    def instumentationTask = "connected${variant.capitalize()}AndroidTest"
                    validateTaskExists(unitTestTask, project)
                    createTask(variant, variantSourceSet, buildType, flavor, "${variant}UnitTestCoverage", unitTestTask, project)
                    createTask(variant, variantSourceSet, buildType, flavor, "${variant}InstrumentationTestCoverage", instumentationTask, project)
                }
            }
        }
    }

    private Task createTask(variant, variantSourceSet, buildType, flavor, GString coverageTask, GString testTask, project) {
        project.tasks.create(name: coverageTask, type: JacocoReport, dependsOn: testTask) {
            group = "Coverage"
            description = "Generate report for coverage by unit test for ${variant.capitalize()} variant."
            def unitTestExclusions = CoverageConstants.ANDROID_EXCLUSIONS
            classDirectories = project.fileTree(
                    'dir': "${project.buildDir}/intermediates/classes/${variantSourceSet}",
                    'excludes': unitTestExclusions,
            )
            def coverageSourceDirs = project.files(
                    project.fileTree(
                            dir: "src/main/java",
                            excludes: unitTestExclusions,
                    ),
                    project.fileTree(
                            dir: "src/$flavor/java",
                            excludes: unitTestExclusions,
                    ),
                    project.fileTree(
                            dir: "src/$buildType/java",
                            excludes: unitTestExclusions,
                    )
            )
            additionalSourceDirs = project.files(coverageSourceDirs)
            sourceDirectories = coverageSourceDirs

            executionData = project.files("${project.buildDir}/jacoco/${testTask}.exec")
            reports {
                xml.enabled = true
                html.enabled = true
                csv.enabled = true
            }
        }
    }

    def validateTaskExists(GString taskName, Project project) {
        if (!project.tasks.findByName(taskName))
            throw new IllegalStateException("task name format has changed of tasks consider upgrading plugin version")
    }

    def validateJacocoUsedForCoverage(Project project) {
        if (!project.plugins.findPlugin(JacocoPlugin))
            throw new IllegalStateException("Jacoco plugin is required for coverage reporting")
    }

    def validateProjectIsAndroidProject(Project project) {
        if (!project.plugins.findPlugin(AppPlugin) && !project.plugins.findPlugin(LibraryPlugin))
            throw new IllegalStateException("Your project must be android project")
    }
}
