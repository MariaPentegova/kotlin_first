plugins {
    kotlin("jvm") version "1.9.0"
}

group = "com.battleship"
version = "1.0.0"

repositories {
    mavenCentral()  // Все артефакты Exposed находятся здесь[citation:1][citation:3][citation:5]
}

dependencies {
    implementation("org.jetbrains.exposed:exposed-core:0.50.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.50.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.50.1")  

    implementation("org.xerial:sqlite-jdbc:3.45.1.0")

    implementation(kotlin("stdlib"))

    // Тесты
    testImplementation(kotlin("test"))

    implementation("org.slf4j:slf4j-simple:2.0.9")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

tasks.register<JavaExec>("run") {
    mainClass = "MainKt"
    classpath = sourceSets["main"].runtimeClasspath
}
    jvmToolchain(17)
}

tasks.register<JavaExec>("run") {
    mainClass.set("MainKt")
    classpath = sourceSets["main"].runtimeClasspath
}
