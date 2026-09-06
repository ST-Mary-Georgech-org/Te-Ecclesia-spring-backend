plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(projects.identity)
	implementation(projects.notifications)
	implementation(projects.events)
	implementation(projects.storage)
	implementation(projects.i18n)
	implementation(projects.audit)
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	runtimeOnly("org.postgresql:postgresql")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation(platform("software.amazon.awssdk:bom:2.33.8"))
	implementation("software.amazon.awssdk:s3")
	implementation(kotlin("stdlib"))
	implementation("software.amazon.awssdk:url-connection-client")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.jar {
	enabled = false
}

