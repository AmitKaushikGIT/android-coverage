package in.proroaster.kaushik.plugins.android.coverage

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Project
import org.gradle.testing.jacoco.plugins.JacocoPlugin

/**
 * Created by kaushik on 8/14/17.
 */
class ValidationUtil {
    static def validateJacocoUsedForCoverage(Project project) {
        if (!project.plugins.findPlugin(JacocoPlugin))
            throw new IllegalStateException("Jacoco plugin is required for coverage reporting")
    }

    static def validateProjectIsAndroidProject(Project project) {
        if (!project.plugins.findPlugin(AppPlugin) && !project.plugins.findPlugin(LibraryPlugin))
            throw new IllegalStateException("Your project must be android project")
    }

    static def validateTaskExists(GString taskName, Project project) {
        if (!project.tasks.findByName(taskName)) {
            println("task name $taskName not found!!")
            throw new IllegalStateException("task name format has changed of tasks consider upgrading plugin version")
        }
    }
}
