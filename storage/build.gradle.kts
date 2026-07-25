plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(platform("software.amazon.awssdk:bom:2.33.8"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:url-connection-client")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk-jvm:1.13.12")
    testImplementation("com.google.truth:truth:1.4.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}