plugins {
    java
    id("io.freefair.lombok") version "8.4" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

allprojects {
    group = "com.solicitation"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.freefair.lombok")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependencies {
        // Logging (all modules)
        implementation("org.slf4j:slf4j-api:2.0.9")
        implementation("ch.qos.logback:logback-classic:1.4.14")
        implementation("ch.qos.logback:logback-core:1.4.14")

        // Testing (all modules)
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
        testImplementation("net.jqwik:jqwik:1.8.2")
        testImplementation("org.mockito:mockito-core:5.7.0")
        testImplementation("org.assertj:assertj-core:3.24.2")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
