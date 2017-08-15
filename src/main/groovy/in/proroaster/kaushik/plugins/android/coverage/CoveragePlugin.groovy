package in.proroaster.kaushik.plugins.android.coverage

import org.gradle.api.Plugin
import org.gradle.api.Project

import static in.proroaster.kaushik.plugins.android.coverage.ValidationUtil.validateJacocoUsedForCoverage
import static in.proroaster.kaushik.plugins.android.coverage.ValidationUtil.validateProjectIsAndroidProject

/**
 * Created by kaushik on 8/13/17.
 */
class CoveragePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        validateProjectIsAndroidProject(project)
        validateJacocoUsedForCoverage(project)
        project.extensions.add("coverage", CoverageExtension.class)

        TaskUtils.createTasks(project)
    }
}
