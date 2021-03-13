plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    api(project(":pleo-antaeus-models"))

    testImplementation("org.jetbrains.exposed:exposed:0.17.7")
    testImplementation("com.squareup.retrofit2:retrofit:2.3.0")
    testImplementation("com.squareup.retrofit2:converter-gson:2.3.0")
}

sourceSets {
    create("integrationTest") {
        withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
            kotlin.srcDir("src/integrationTest/kotlin")
            resources.srcDir("src/integrationTest/resources")
            compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"] + sourceSets["test"].output
            runtimeClasspath += output + compileClasspath + sourceSets["test"].runtimeClasspath
        }
    }
}

task<Test>("integrationTest") {
    description = "Runs the integration tests"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    mustRunAfter(tasks["test"])
    useJUnitPlatform()
}

sourceSets {
    create("e2eTest") {
        withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
            kotlin.srcDir("src/e2eTest/kotlin")
            resources.srcDir("src/e2eTest/resources")
            compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
            runtimeClasspath += output + compileClasspath
        }
    }
}

task<Test>("e2eTest") {
    description = "Runs the e2e tests"
    group = "verification"
    testClassesDirs = sourceSets["e2eTest"].output.classesDirs
    classpath = sourceSets["e2eTest"].runtimeClasspath
    useJUnitPlatform()

    systemProperty("apiUrl", System.getProperty("apiUrl") ?: "http://localhost:7000")
}
