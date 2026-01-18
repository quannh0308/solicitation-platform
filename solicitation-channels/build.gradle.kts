dependencies {
    // Internal dependencies
    implementation(project(":solicitation-common"))
    implementation(project(":solicitation-models"))
    
    // Test dependencies
    testImplementation(project(":solicitation-models"))

    // AWS SDK
    implementation(platform("software.amazon.awssdk:bom:2.20.26"))
    implementation("software.amazon.awssdk:sns")
    implementation("software.amazon.awssdk:ses")
}
