package in.proroaster.kaushik.plugins.android.coverage

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * Created by kaushik on 8/14/17.
 */
class CoveragePluginTest extends Specification {
    def 'should apply coverage extensions'(){
        when:
        Project p = ProjectBuilder.builder().build()
        p.apply plugin: 'com.android.application'
        p.apply plugin: 'jacoco'
        p.apply plugin: 'coverage'

        p.coverage {
            unitTestConfig {
                exclusions = ['unit']
                inclusions = ['unit']
                executionData = 'unit'
                csvReport = false
            }
            instrumentationTestConfig {
                exclusions = ['int']
            }
        }
        then:
        CoverageExtension extension = p.coverage
        extension.unitTestConfig.exclusions[0] == "unit"
        extension.instrumentationTestConfig.exclusions[0] == "int"
        extension.unitTestConfig.inclusions[0] == "unit"
        extension.unitTestConfig.executionData == "unit"
        extension.unitTestConfig.csvReport == false
    }
}
