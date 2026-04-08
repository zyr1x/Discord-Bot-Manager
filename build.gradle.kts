plugins {
    id("java")
    id("org.springframework.boot") version("4.1.0-M1")
    id("io.spring.dependency-management") version("1.1.7")
}

group = "ru.lewis.botmanager"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:6.4.1")

    implementation("org.springframework.boot:spring-boot-starter")
    //implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    implementation("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}