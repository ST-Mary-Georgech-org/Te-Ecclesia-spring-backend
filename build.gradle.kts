plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
}
description = "TeEcclesia"

val javaVersion = 21

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(javaVersion)
	}
}

allprojects {
	group = "org.teEcclesia"
	version = "0.0.1-SNAPSHOT"

	repositories {
		google()
		mavenCentral()
	}
}

// Enforce a single JVM/toolchain configuration for all modules
subprojects {
	repositories {
		google()
		mavenCentral()
	}

	// Configure java toolchain only if the Java plugin (or a plugin that adds the java extension) is applied
	plugins.withType<JavaPlugin> {
		java {
			toolchain {
				languageVersion = JavaLanguageVersion.of(javaVersion)
			}
		}
	}

	// If a subproject applies the Kotlin JVM plugin, configure its Kotlin jvm toolchain
	plugins.withId("org.jetbrains.kotlin.jvm") {
		kotlin {
			jvmToolchain(javaVersion)
		}
	}

	plugins.withType<JavaPlugin> {
		dependencies {
			constraints {
				implementation("com.google.guava:guava:33.6.0-jre")
				implementation("com.google.protobuf:protobuf-java:4.29.3")
				implementation("io.grpc:grpc-netty-shaded:1.76.0")
				implementation("io.netty:netty-codec-http:4.2.15.Final")
			}
		}
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation(kotlin("stdlib"))
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
	enabled = false
}

subprojects {
	tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
		enabled = project.name == "app"
		if (project.name != "app") {
			mainClass.set("none")
		}
	}
	tasks.withType<Jar> {
		enabled = true
	}
}
repositories {
	google()
	mavenCentral()
}

