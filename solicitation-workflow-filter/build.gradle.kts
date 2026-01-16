plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    // Internal dependencies
    implementation(project(":solicitation-common"))
    implementation(project(":solicitation-models"))
    implementation(project(":solicitation-filters"))
    implementation(project(":solicitation-storage"))

    // AWS Lambda
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.3")
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}
