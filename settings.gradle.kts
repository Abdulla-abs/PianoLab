pluginManagement {
    repositories {
        maven {
            url = uri("https://mirrors.cloud.tencent.com/gradle")
        }
        maven { url=uri ("https://maven.aliyun.com/repository/google")}
        maven { url=uri ("https://maven.aliyun.com/repository/central")}
        maven { url=uri ("https://maven.aliyun.com/repository/gradle-plugin")}
        maven { url=uri ("https://maven.aliyun.com/repository/public")}


        maven("https://jitpack.io")
        maven { url =uri("https://plugins.gradle.org/m2/" )}
        maven { url =uri("https://maven.aliyun.com/nexus/content/repositories/google") }
        maven { url =uri("https://maven.aliyun.com/nexus/content/groups/public") }
        maven { url =uri("https://maven.aliyun.com/nexus/content/repositories/jcenter")}
        gradlePluginPortal()
        google()
        mavenCentral()

    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url =uri("https://mirrors.cloud.tencent.com/maven/") }
        maven {
            url = uri("https://mirrors.cloud.tencent.com/gradle")
        }
        maven("https://jitpack.io")
        maven { url=uri ("https://maven.aliyun.com/repository/google")}
        maven { url=uri ("https://maven.aliyun.com/repository/central")}
        maven { url=uri ("https://maven.aliyun.com/repository/gradle-plugin")}
        maven { url=uri ("https://maven.aliyun.com/repository/public")}
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        google()
        mavenCentral()

    }
}

rootProject.name = "PianoLab"
include(":app")
