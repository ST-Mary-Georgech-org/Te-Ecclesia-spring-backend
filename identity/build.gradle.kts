plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "1.9.22"
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	runtimeOnly("org.postgresql:postgresql")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
	compileOnly("io.swagger.core.v3:swagger-annotations:2.2.22")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("io.mockk:mockk-jvm:1.13.12")
	testImplementation("com.google.truth:truth:1.4.4")
	testImplementation("org.springframework.security:spring-security-crypto")
	testRuntimeOnly("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation(projects.events)
	implementation(projects.storage)
	implementation(projects.httpClient)
}

tasks.withType<Test> {
	useJUnitPlatform()
}
