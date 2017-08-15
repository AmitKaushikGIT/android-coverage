# android-coverage
Plugin creates customizable tasks which generate coverage reports for unit and instrumentation tests. 

Also helps you set coverage threshold for you source code and creates tasks to verify your code coverage meets the threshold.

#Latest Version
1.0 - experimental

#Adding gradle dependency

    buildscript {
        repositories {
            mavenCentral()
            maven 
            {
                url "https://dl.bintray.com/amitkaushikgit/android/"
            }
            jcenter()
        }

        dependencies {
            classpath 'in.proroaster.kaushik:coverage:1.0'
        }
    }

#Tasks
Creates following tasks for debug build type:
flavorDebugUnitTestCoverage
flavorDebugInstrumentationTestCoverage
mergeFlavorDebugCoverageReports
checkFlavorDebugUnitTestCoverage
checkFlavorDebugAndroidTestCoverage

#extensions
Offers extentions to configure your reports:

    coverage{
    
        unitTestConfig {
            exclusions []
            inclusions []
            executionData ""
            csvReport false
            boolean checkThresholdAfterRunningTest = true
            def coverageLimits = [:]
        }
   
        instrumentationTestConfig {
            exclusions []
            inclusions []
            executionData ""
            csvReport false
            checkThresholdAfterRunningTest = true
            coverageLimits = [:]
        }
}

#CoverageLimits 
specify coverage thresholds in form of map, where keys are type of coverage

    Valid keys are -{
        'instruction'
        'branch'     
        'line'       
        'complexity' 
        'method'     
        'class'      
    }

#checkThresholdAfterRunningTest

set true if you want to run coverage threshold check whenever you run test(Ideal way)

if you want to run your check separately, remember to generate coverage report before running check

eg. if you set to true then you can run *checkFlavorDebugUnitTestCoverage* which will run *flavorDebugUnitTestCoverage* as dependency

#Notes
 - can be applied to android applications and library
 - jacoco must be applied for coverage