package in.proroaster.kaushik.plugins.android.coverage

import org.gradle.api.Action
import org.gradle.util.ConfigureUtil

/**
 * Exposes properties to gradle DSL which can be used to configure coverage tasks
 */
class CoverageExtension {
    UnitTestConfig unitTestConfig
    InstrumentationTestConfig instrumentationTestConfig

    UnitTestConfig unitTestConfig(Closure closure) {
        return unitTestConfig(ConfigureUtil.configureUsing(closure))
    }

    void unitTestConfig(Action<? super UnitTestConfig> action) {
        if (unitTestConfig == null) {
            unitTestConfig = new UnitTestConfig()
            action.execute(unitTestConfig)
        }
    }

    InstrumentationTestConfig instrumentationTestConfig(Closure closure) {
        return instrumentationTestConfig(ConfigureUtil.configureUsing(closure))
    }

    void instrumentationTestConfig(Action<? super InstrumentationTestConfig> action) {
        if (instrumentationTestConfig == null) {
            instrumentationTestConfig = new InstrumentationTestConfig()
            action.execute(instrumentationTestConfig)
        }
    }

    class InstrumentationTestConfig {
        List<String> exclusions = []
        List<String> inclusions = []
        String executionData = null
        boolean csvReport = false
        boolean checkThresholdAfterRunningTest = true
        def coverageLimits = [:]
    }

    class UnitTestConfig {
        List<String> exclusions = []
        List<String> inclusions = []
        String executionData = null
        boolean csvReport = false
        boolean checkThresholdAfterRunningTest = true
        def coverageLimits = [:]
    }
}
