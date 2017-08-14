package in.proroaster.kaushik.plugins.android.coverage

import in.proroaster.kaushik.plugins.android.coverage.CoverageExtension.UnitTestConfig
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * Created by kaushik on 8/13/17.
 */
class UnitTestConfiguration {
    UnitTestConfig unitTestConfig;
    void createUnitTestCoverageTasks(Project project) {
        project.afterEvaluate {
            CoverageExtension config = project.coverage
            unitTestConfig = config.unitTestConfig
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
                    validateTaskExists(unitTestTask, project)
                    createTask(variant, variantSourceSet, buildType, flavor, "${variant}UnitTestCoverage", unitTestTask, project)
                }
            }
        }
    }

    private Task createTask(variant, variantSourceSet, buildType, flavor, GString coverageTask, GString testTask, project) {
        project.tasks.create(name: coverageTask, type: JacocoReport, dependsOn: testTask) {
            group = "Coverage"
            description = "Generate report for coverage by unit test for ${variant.capitalize()} variant."
            List<String> exclusions = CoverageConstants.ANDROID_EXCLUSIONS
            List<String> inclusions = []
            inclusions.addAll(unitTestConfig.inclusions)
            exclusions.addAll(unitTestConfig.exclusions)
            classDirectories = project.fileTree(
                    dir: "${project.buildDir}/intermediates/classes/${variantSourceSet}",
                    excludes: exclusions,
                    includes: inclusions
            )
            def coverageSourceDirs = project.files(
                    project.fileTree(
                            dir: "src/main/java",
                            excludes: exclusions,
                            includes: inclusions
                    ),
                    project.fileTree(
                            dir: "src/$flavor/java",
                            excludes: exclusions,
                            includes: inclusions
                    ),
                    project.fileTree(
                            dir: "src/$buildType/java",
                            excludes: exclusions,
                            includes: inclusions
                    )
            )
            additionalSourceDirs = project.files(coverageSourceDirs)
            sourceDirectories = coverageSourceDirs

            executionData = unitTestConfig.executionData == null ? project.files("${project.buildDir}/jacoco/${testTask}.exec") : unitTestConfig.executionData
            reports {
                xml.enabled = true
                html.enabled = true
                csv.enabled = unitTestConfig.csvReport
            }
        }
    }

    def validateTaskExists(GString taskName, Project project) {
        if (!project.tasks.findByName(taskName))
            throw new IllegalStateException("task name format has changed of tasks consider upgrading plugin version")
    }
}
