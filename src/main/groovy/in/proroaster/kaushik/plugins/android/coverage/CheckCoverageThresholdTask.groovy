package in.proroaster.kaushik.plugins.android.coverage

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Created by kaushik on 8/15/17.
 */
class CheckCoverageThresholdTask extends DefaultTask {

    private Project project
    private String variant
    private TestType testType
    private def coverageLimits

    @TaskAction
    void checkCoverage() {
        group = "Coverage"
        description = "Check Code Coverage"
        def report = new File("${project.projectDir}/build/reports/jacoco/${variant}${testType.typeString.capitalize()}Coverage/${variant}${testType.typeString.capitalize()}Coverage.xml")

        if (!report.exists()) {
            throw new RuntimeException("You have to run ${testType.typeString} to check threshold")
        }
        logger.lifecycle("Checking coverage results: ${report}")

        def parser = new XmlParser()
        parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
        def results = parser.parse(report)

        def percentage = {
            if(it == null) {
                return 100.00
            }
            def covered = it.'@covered' as Double
            def missed = it.'@missed' as Double
            ((covered / (covered + missed)) * 100).round(2)
        }

        def counters = results.counter
        def metrics = [:]
        metrics << [
                'instruction': percentage(counters.find { it.'@type'.equals('INSTRUCTION') }),
                'branch'     : percentage(counters.find { it.'@type'.equals('BRANCH') }),
                'line'       : percentage(counters.find { it.'@type'.equals('LINE') }),
                'complexity' : percentage(counters.find { it.'@type'.equals('COMPLEXITY') }),
                'method'     : percentage(counters.find { it.'@type'.equals('METHOD') }),
                'class'      : percentage(counters.find { it.'@type'.equals('CLASS') })
        ]

        def failures = []
        metrics.each {
            def limit = getCoverageLimits()[it.key]
            if(limit == null){
                logger.quiet("No threshold defined for property ${it.key}, assuming threshold to be 0%")
                limit = 0
            }
            def coverage_check = "- ${it.key} coverage rate = ${it.value}%; threshold = ${limit}%"
            logger.quiet(coverage_check)
            if (it.value < limit) {
                failures.add(coverage_check)
            }
        }

        if (failures) {
            logger.quiet("------------------ Code Coverage Failed -----------------------")
            failures.each {
                logger.quiet(it)
            }
            logger.quiet("---------------------------------------------------------------")
            throw new GradleException("Code coverage failed")
        } else {
            logger.quiet("Passed Code Coverage Checks")
        }
    }

    void setProject(Project project){
        this.project = project
    }

    void setVariant(String variant){
        this.variant = variant
    }

    void setTestType(TestType testType){
        this.testType = testType
    }

    void setCoverageLimits(Map limits){
        this.coverageLimits = limits
    }

    Project getProject(){
        return this.project
    }

    String getVariant(){
        return this.variant
    }

    TestType getTestType(){
        return this.testType
    }

    Map getCoverageLimits(){
        return this.coverageLimits
    }

}

