package in.proroaster.kaushik.plugins.android.coverage

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testing.jacoco.tasks.JacocoMerge
import org.gradle.testing.jacoco.tasks.JacocoReport

import static in.proroaster.kaushik.plugins.android.coverage.ValidationUtil.validateTaskExists

/**
 * Created by kaushik on 8/13/17.
 */
class TaskUtils {
    static void createTasks(Project project) {
        project.afterEvaluate {
            CoverageExtension config = project.coverage
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
                    def variant, variantClassPath
                    if (!flavor) {
                        variant = "${buildType}"
                        variantClassPath = "${buildType}"
                    } else {
                        variant = "${flavor}${buildType.capitalize()}"
                        variantClassPath = "${flavor}/${buildType}"
                    }
                    def unitTestTask = "test${variant.capitalize()}UnitTest"
                    validateTaskExists(unitTestTask, project)
                    def unitTestCoverageTask = "${variant}UnitTestCoverage"
                    createUnitTestCoverageTask(variant, variantClassPath, buildType, flavor, unitTestCoverageTask, unitTestTask, project, config.unitTestConfig)
                    validateTaskExists(unitTestCoverageTask, project)
                    def checkUnitCoverageTask = "check${variant.capitalize()}${TestType.UNIT.typeString}Coverage"
                    createCheckUnitTestCoverageThresholdTask(variant, TestType.UNIT, project, config, checkUnitCoverageTask)
                    validateTaskExists(checkUnitCoverageTask, project)
                    if(config.unitTestConfig.checkThresholdAfterRunningTest) {
                        project.tasks.findByName(checkUnitCoverageTask).dependsOn(unitTestCoverageTask)
                    }
                }
            }
            productFlavorNames.each { flavor ->
                def mergeTask = "merge${flavor.capitalize()}DebugCoverageReports"
                def testTask = "connected${flavor.capitalize()}DebugAndroidTest"
                def variant = "${flavor}Debug"
                def variantClassPath = "${flavor}/debug"
                def coverageTask = "${variant}AndroidTestCoverage"
                createMergeCoverageTasks(flavor, mergeTask, testTask, project)
                validateTaskExists(mergeTask, project)
                createAndroidTestCoverageTask(variant, variantClassPath, "debug", flavor, coverageTask, mergeTask, project, config.instrumentationTestConfig)
                validateTaskExists(coverageTask, project)
                def checkAndroidCoverageTaskName = "check${variant.capitalize()}${TestType.INSTRUMENTATION.typeString}Coverage"
                createCheckAndroidTestCoverageThresholdTask(variant, TestType.INSTRUMENTATION, project, config, checkAndroidCoverageTaskName)
                validateTaskExists(checkAndroidCoverageTaskName, project)
                if(config.instrumentationTestConfig.checkThresholdAfterRunningTest) {
                    project.tasks.findByName(checkAndroidCoverageTaskName).dependsOn(coverageTask)
                }
            }
        }
    }

    static void createCheckUnitTestCoverageThresholdTask(variant, TestType testType, Project project, CoverageExtension config, GString taskName) {
        project.tasks.create(name: taskName, type: CheckCoverageThresholdTask ){
            ((CheckCoverageThresholdTask)it).project = project
            ((CheckCoverageThresholdTask)it).testType = testType
            ((CheckCoverageThresholdTask)it).setVariant(variant)
            ((CheckCoverageThresholdTask)it).setCoverageLimits(config.unitTestConfig.coverageLimits)
        }
    }

    static void createCheckAndroidTestCoverageThresholdTask(variant, TestType testType, Project project, CoverageExtension config, GString taskName) {
        project.tasks.create(name: taskName, type: CheckCoverageThresholdTask ){
            ((CheckCoverageThresholdTask)it).project = project
            ((CheckCoverageThresholdTask)it).testType = testType
            ((CheckCoverageThresholdTask)it).setVariant(variant)
            ((CheckCoverageThresholdTask)it).setCoverageLimits(config.instrumentationTestConfig.coverageLimits)
        }
    }

    static void createAndroidTestCoverageTask(variant, variantClassPath, buildType, flavor, GString coverageTask, GString mergeTask, project, CoverageExtension.InstrumentationTestConfig testConfig) {
        project.tasks.create(name: coverageTask, type: JacocoReport, dependsOn: mergeTask) {
            group = "Coverage"
            description = "Generate report for coverage by instrumentation test for ${variant.capitalize()} variant."
            List<String> exclusions = []
            List<String> inclusions = []
            inclusions.addAll(testConfig.inclusions)
            exclusions.addAll(CoverageConstants.ANDROID_EXCLUSIONS)
            exclusions.addAll(testConfig.exclusions)
            classDirectories = project.fileTree(
                    dir: "${project.buildDir}/intermediates/classes/${variantClassPath}",
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

            executionData = testConfig.executionData == null ? project.files("${project.buildDir}/jacoco/${mergeTask}.exec") : testConfig.executionData
            reports {
                xml.enabled = true
                html.enabled = true
                csv.enabled = testConfig.csvReport
            }
        }

    }

    private static Task createMergeCoverageTasks(flavor, GString mergeTask, GString testTask, Project project) {
        project.tasks.create(name: mergeTask, type: JacocoMerge, dependsOn: testTask) {
            group = "Coverage"
            description = "Merge coverage reports generated in various devices"
            executionData = project.fileTree(dir: "$project.buildDir/outputs/code-coverage/connected/flavors/${flavor}/", includes: ["*.ec"])
        }
    }

    private
    static Task createUnitTestCoverageTask(variant, variantClassPath, buildType, flavor, GString coverageTask, GString testTask, project, CoverageExtension.UnitTestConfig testConfig) {
        project.tasks.create(name: coverageTask, type: JacocoReport, dependsOn: testTask) {
            group = "Coverage"
            description = "Generate report for coverage by unit test for ${variant.capitalize()} variant."
            List<String> exclusions = []
            List<String> inclusions = []
            inclusions.addAll(testConfig.inclusions)
            exclusions.addAll(CoverageConstants.ANDROID_EXCLUSIONS)
            exclusions.addAll(testConfig.exclusions)
            classDirectories = project.fileTree(
                    dir: "${project.buildDir}/intermediates/classes/${variantClassPath}",
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

            executionData = testConfig.executionData == null ? project.files("${project.buildDir}/jacoco/${testTask}.exec") : testConfig.executionData
            reports {
                xml.enabled = true
                html.enabled = true
                csv.enabled = testConfig.csvReport
            }
        }
    }
}
