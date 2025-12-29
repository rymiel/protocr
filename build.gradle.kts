plugins {
    id("java")
    application
    id("com.google.protobuf") version "0.9.6"
}

group = "space.rymiel"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("com.google.protobuf:protobuf-bom:4.33.2"))
    implementation("com.google.protobuf:protobuf-java")
}

application {
    mainClass = "space.rymiel.protocr.Main"
}
