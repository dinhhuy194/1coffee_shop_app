pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Mapbox Maven repository - cần để download Mapbox SDK v11
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<HttpHeaderAuthentication>("basic")
            }
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "Bearer ${providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").getOrElse("")}"
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Mapbox Maven repository - cần để resolve Mapbox SDK dependencies
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<HttpHeaderAuthentication>("basic")
            }
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "Bearer ${providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").getOrElse("")}"
            }
        }
    }
}

rootProject.name = "coffeeshop"
include(":app")
 