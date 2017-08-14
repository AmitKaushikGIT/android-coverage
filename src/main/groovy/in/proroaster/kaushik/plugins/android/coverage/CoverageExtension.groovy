package in.proroaster.kaushik.plugins.android.coverage

import org.gradle.api.Action
import org.gradle.util.ConfigureUtil

/**
 * Created by kaushik on 8/13/17.
 */
class CoverageExtension {
    UnitTestConfig unitTestConfig
    InstrumentationTestConfig instrumentationTestConfig

    UnitTestConfig unitTestConfig(Closure closure) {
        return unitTestConfig(ConfigureUtil.configureUsing(closure))
    }

    void unitTestConfig(Action<? super UnitTestConfig> action){
        if(unitTestConfig == null) {
            unitTestConfig = new UnitTestConfig()
            action.execute(unitTestConfig)
        }
    }

    InstrumentationTestConfig instrumentationTestConfig(Closure closure) {
        return instrumentationTestConfig(ConfigureUtil.configureUsing(closure))
    }

    void instrumentationTestConfig(Action<? super UnitTestConfig> action){
        if(instrumentationTestConfig == null) {
            instrumentationTestConfig = new InstrumentationTestConfig()
            action.execute(instrumentationTestConfig)
        }
    }
    class InstrumentationTestConfig {
        List<String> exclusions
    }

    class UnitTestConfig {
        List<String> exclusions = []
        List<String> inclusions = []
        String executionData = null
        boolean csvReport = false
    }
}
