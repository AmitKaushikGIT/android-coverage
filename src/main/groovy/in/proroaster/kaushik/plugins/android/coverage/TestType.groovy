package in.proroaster.kaushik.plugins.android.coverage

/**
 * Created by kaushik on 8/15/17.
 */
enum TestType {
    UNIT("UnitTest"),
    INSTRUMENTATION("AndroidTest")

    TestType(String typeString){
        this.typeString = typeString
    }
    String typeString
}