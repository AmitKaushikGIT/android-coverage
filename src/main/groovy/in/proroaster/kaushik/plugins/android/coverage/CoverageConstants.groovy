package in.proroaster.kaushik.plugins.android.coverage

/**
 * Created by kaushik on 8/13/17.
 */
class CoverageConstants {
    public static final ANDROID_EXCLUSIONS = [
            '**/R.class',
            '**/R$*.class',
            '**/*$ViewInjector*.*',
            '**/*$ViewBinder*.*',
            '**/BuildConfig.*',
            '**/Manifest*.*'
    ]


}
