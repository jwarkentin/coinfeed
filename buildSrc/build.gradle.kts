plugins {
    `kotlin-dsl`
    idea
}

repositories {
    mavenLocal()
    gradlePluginPortal()    // See https://github.com/gradle/gradle/issues/4020#issuecomment-357489131
    jcenter()
}

// idea {
//     module {
//         isDownloadJavadoc = false
//         isDownloadSources = false
//     }
// }
