package com.example

import org.gradle.api.Plugin
import org.gradle.api.Project

class ChaosMonkeyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.extensions.create('chaosMonkey', ChaosMonkeyExtension)

        project.afterEvaluate {
            if (project.plugins.hasPlugin('org.springframework.boot')) {
                project.dependencies {
                    implementation 'de.codecentric:chaos-monkey-spring-boot:2.5.0'
                    implementation 'org.springframework:spring-aspects'
                }
            }
        }

        project.tasks.register('enableChaos') {
            group = 'chaosMonkey'
            description = 'Enable Chaos Monkey assaults'

            doLast {
                println "Enabling Chaos Monkey..."

                def resDir = project.file('src/main/resources')
                if (!resDir.exists()) {
                    resDir.mkdirs()
                }
                def chaosConfig = new File(resDir, 'application-chaos-monkey.properties')
                chaosConfig.text =
                """
                    chaos.monkey.enabled=true
                    chaos.monkey.assaults.latency-active=true
                    chaos.monkey.assaults.exceptions-active=true
                    chaos.monkey.watch-controller=true
                    management.endpoints.web.exposure.include=health,info,chaosmonkey
                    management.endpoint.chaosmonkey.enabled=true
                """
                println "Chaos Monkey configuration written to src/main/resources/application-chaos-monkey.properties"
            }
        }

        project.tasks.named('bootRun').configure {
            doFirst {
                if (project.gradle.startParameter.taskNames.contains('enableChaos')) {
                    jvmArgs = ['-Dspring.profiles.active=chaos-monkey']
                    println "Running bootRun with chaos-monkey profile"
                }
            }
        }
    }
}

class ChaosMonkeyExtension {
    boolean enabled = false
}
