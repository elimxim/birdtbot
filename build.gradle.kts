plugins {
    kotlin("jvm") version "1.9.0"
    id("org.springframework.boot") version "3.1.2"
    id("io.spring.dependency-management") version "1.1.2"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.telegram:telegrambots:6.5.0")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.vdurmont:emoji-java:5.1.1")
}

application {
    mainClass.set("com.github.elimxim.birdtbot.TelegramBotAppKt")
}