package in.proroaster.kaushik.plugins.android.coverage

import in.proroaster.kaushik.plugins.android.coverage.CoverageExtension.InstrumentationTestConfig
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.WorkResult
import org.gradle.testing.jacoco.tasks.JacocoMerge
import org.gradle.testing.jacoco.tasks.JacocoReport

import static in.proroaster.kaushik.plugins.android.coverage.ValidationUtil.validateTaskExists

/**
 * Created by kaushik on 8/13/17.
 */
class TaskUtils {
    static void createTasks(Project project) {
        copyTestRunnerScriptToProject(project)
        givePermissionToTestRunnerScript(project)
        project.afterEvaluate {
            createShardingHelperTasks(project)
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
                    if (config.unitTestConfig.checkThresholdAfterRunningTest) {
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
                if (config.instrumentationTestConfig.sharding) {
                    testTask = "shard${flavor.capitalize()}DebugAndroidTest"
                    createShardedTestTask(flavor, testTask, project, config.instrumentationTestConfig)
                    validateTaskExists(testTask, project)
                    project.tasks.findByName(testTask).finalizedBy "cleanUpScript"
                }
                createMergeCoverageTasks(flavor, mergeTask, testTask, project, config.instrumentationTestConfig)
                validateTaskExists(mergeTask, project)
                createAndroidTestCoverageTask(variant, variantClassPath, "debug", flavor, coverageTask, mergeTask, project, config.instrumentationTestConfig)
                validateTaskExists(coverageTask, project)
                def checkAndroidCoverageTaskName = "check${variant.capitalize()}${TestType.INSTRUMENTATION.typeString}Coverage"
                createCheckAndroidTestCoverageThresholdTask(variant, TestType.INSTRUMENTATION, project, config, checkAndroidCoverageTaskName)
                validateTaskExists(checkAndroidCoverageTaskName, project)
                if (config.instrumentationTestConfig.checkThresholdAfterRunningTest) {
                    project.tasks.findByName(checkAndroidCoverageTaskName).dependsOn(coverageTask)
                }
            }
        }
    }

    private static void createShardingHelperTasks(Project project) {
        project.tasks.create(name: "createCoverageDir") {
            doFirst {
                project.mkdir "${project.buildDir}/coverage"
            }
        }
        project.tasks.create(name: "cleanUpScript") {
            doFirst {
                project.file("${project.projectDir}/runTestOnMultipleDevices.sh").delete()
            }
        }
    }

    private static void givePermissionToTestRunnerScript(Project project) {
        try {
            File file = new File("${project.projectDir}/runTestOnMultipleDevices.sh")
            Runtime.getRuntime().exec("chmod 777 ${file.getAbsolutePath()}")
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static WorkResult copyTestRunnerScriptToProject(Project project) {
        project.copy {
            from(project.zipTree(new CoverageConstants().getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).matching {
                include '*.sh'
            })
            into("${project.projectDir}")
        }
    }

    static def createShardedTestTask(flavor, taskName, Project project, InstrumentationTestConfig config) {
        project.tasks.create(name: taskName, type: Exec, dependsOn: ["assemble${flavor.capitalize()}Debug", "assemble${flavor.capitalize()}DebugAndroidTest", "createCoverageDir"]) {
            group = "Verification"
            description = "Runs tests on multiple devices and pulls coverage reports"
            commandLine "./runTestOnMultipleDevices.sh", "${project.buildDir}/outputs/apk/${config.testApkPath}", "${project.buildDir}/outputs/apk/${config.targetApkPath}", config.appPackageName, config.testPackageName, config.testRunner, "${project.buildDir}/coverage/"
        }
    }

    static void createCheckUnitTestCoverageThresholdTask(variant, TestType testType, Project project, CoverageExtension config, GString taskName) {
        project.tasks.create(name: taskName, type: CheckCoverageThresholdTask) {
            ((CheckCoverageThresholdTask) it).project = project
            ((CheckCoverageThresholdTask) it).testType = testType
            ((CheckCoverageThresholdTask) it).setVariant(variant)
            ((CheckCoverageThresholdTask) it).setCoverageLimits(config.unitTestConfig.coverageLimits)
        }
    }

    static void createCheckAndroidTestCoverageThresholdTask(variant, TestType testType, Project project, CoverageExtension config, GString taskName) {
        project.tasks.create(name: taskName, type: CheckCoverageThresholdTask) {
            ((CheckCoverageThresholdTask) it).project = project
            ((CheckCoverageThresholdTask) it).testType = testType
            ((CheckCoverageThresholdTask) it).setVariant(variant)
            ((CheckCoverageThresholdTask) it).setCoverageLimits(config.instrumentationTestConfig.coverageLimits)
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

    private
    static Task createMergeCoverageTasks(flavor, GString mergeTask, GString testTask, Project project, InstrumentationTestConfig config) {
        project.tasks.create(name: mergeTask, type: JacocoMerge, dependsOn: testTask) {
            group = "Coverage"
            description = "Merge coverage reports generated in various devices"
            def coverageData = "$project.buildDir/outputs/code-coverage/connected/flavors/${flavor}/"
            if (config.sharding) {
                coverageData = "$project.buildDir/coverage/"
            }
            executionData = project.fileTree(dir: coverageData, includes: ["*.ec"])
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
