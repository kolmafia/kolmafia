// -*- mode: kotlin -*-
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.panteleyev.jpackage.ImageType
import java.text.SimpleDateFormat
import java.util.Date

buildscript {
  repositories {
    mavenCentral()
  }
}

plugins {
  application
  jacoco
  java

  id("com.diffplug.spotless") version "8.1.0"
  id("com.gradleup.shadow") version "9.2.2"
  id("net.nemerosa.versioning") version "3.1.0"
  id("org.ajoberstar.grgit") version "5.3.3"
  id("org.panteleyev.jpackageplugin") version "1.7.6"
  id("com.github.ben-manes.versions") version "0.53.0" // enables ./gradlew dependencyUpdates for outdated

  checkstyle
}

checkstyle {
  toolVersion = "12.1.0"
}

sourceSets {
  main {
    java {
      setSrcDirs(listOf("src", "lib"))
      destinationDirectory.set(file("build/main"))
    }
    resources {
      setSrcDirs(listOf("src", "lib"))
      exclude("**/*.java", "**/*.jar")
    }
  }

  create("lib") {
    java {
      setSrcDirs(listOf("lib"))
      destinationDirectory.set(file("build/lib"))
    }
  }

  test {
    java {
      setSrcDirs(listOf("test"))
      destinationDirectory.set(file("build/test"))
    }
    resources {
      setSrcDirs(listOf("test/resources"))
    }
  }
}

repositories {
  // Use Maven Central for resolving dependencies.
  mavenCentral()
}

dependencies {
  // Use JUnit Jupiter for running JUnit5 tests.
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter:5.14.0")

  testImplementation("org.hamcrest:hamcrest:3.0")
  testImplementation("com.spotify:hamcrest-optional:1.3.2")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.14.0")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.14.0")
  testImplementation("org.mockito:mockito-core:5.20.0")
  testImplementation("org.eclipse.xtext:org.eclipse.xtext.xbase.lib:2.41.0.M1") {
    because("assertion errors including Location/Range/Position need it")
  }
  testImplementation("org.junit-pioneer:junit-pioneer:2.3.0")

  implementation("com.formdev:flatlaf:1.6.5")
  implementation("com.formdev:flatlaf-intellij-themes:1.6.5")
  implementation("com.formdev:flatlaf-swingx:1.6.5")
  // Optional runtime deps for svnkit
  runtimeOnly("com.trilead:trilead-ssh2:1.0.0-build222")
  runtimeOnly("net.java.dev.jna:jna:5.18.1")
  runtimeOnly("net.java.dev.jna:jna-platform:5.18.1")

  "libImplementation"("org.swinglabs:swingx:1.0")

  implementation(
    files("build/lib") {
      builtBy("compileLibJava")
    },
  )

  implementation("net.sourceforge.htmlcleaner:htmlcleaner:2.29")
  implementation("org.jsoup:jsoup:1.21.2")
  implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.24.0") {
    exclude(group = "org.eclipse.xtend", module = "org.eclipse.xtend.lib")
  }
  implementation("org.slf4j:slf4j-nop:2.0.17")
  implementation("org.fusesource.jansi:jansi:2.4.2")
  implementation("com.alibaba.fastjson2:fastjson2:2.0.59")
  implementation("org.mozilla:rhino:1.9.0")
  implementation("org.swinglabs:swingx:1.0")
  implementation("org.tmatesoft.svnkit:svnkit:1.10.11")
  implementation("com.jgoodies:jgoodies-binding:2.13.0")
  implementation("org.eclipse.jgit:org.eclipse.jgit:7.4.0.202509020913-r")
  implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:7.4.0.202509020913-r")

  checkstyle("com.puppycrawl.tools:checkstyle:${checkstyle.toolVersion}")
}

application {
  // Define the main class for the application.
  mainClass.set("net.sourceforge.kolmafia.KoLmafia")
}

spotless {
  format("misc") {
    target(".gitignore")

    trimTrailingWhitespace()
    leadingTabsToSpaces(2)
    endWithNewline()
  }

  freshmark {
    target("*.md")
  }

  kotlinGradle {
    ktlint()
  }
  java {
    target("src/**/*.java", "test/**/*.java")
    googleJavaFormat()
  }
}

tasks.register<Delete>("cleanDist") {
  val dist = file("dist")
  onlyIf {
    dist.exists()
  }
  delete(
    dist.listFiles()?.filter { it.isFile && it.name.startsWith("KoLmafia-") && it.name.endsWith(".jar") }.orEmpty(),
  )
}

tasks.register<Delete>("pruneDist") {
  val dist = file("dist")
  onlyIf {
    dist.exists()
  }
  doFirst {
    val revString = revisionProvider.get()
    delete(
      dist
        .listFiles()
        ?.filter {
          it.isFile && it.name.startsWith("KoLmafia-") && it.name.endsWith(".jar") &&
            (!it.name.contains(revString) || (isDirty() != it.name.endsWith("-M.jar")))
        }.orEmpty(),
    )
  }
}

tasks.test {
  useJUnitPlatform()
  systemProperty("line.separator", "\n")
  systemProperty("junit.jupiter.extensions.autodetection.enabled", true)
  systemProperty("useCWDasROOT", true)
  systemProperty("file.encoding", "UTF-8")
  workingDir("test/root")

  testLogging.showStandardStreams = true

  reports {
    html.required.set(true)
    junitXml.required.set(true)
  }
}

tasks.jacocoTestReport {
  reports {
    xml.required.set(true)
  }
}

tasks.jar {
  manifest {
    attributes(
      "Main-Class" to "net.sourceforge.kolmafia.KoLmafia",
      "Build-Revision" to
        object {
          override fun toString(): String = project.version.toString()
        },
      "Build-Branch" to versioning.info.branchId,
      "Build-Build" to versioning.info.build,
      "Build-Dirty" to isDirty(),
      "Build-Jdk" to "${System.getProperty("java.version")} " +
        "(${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")})",
      "Build-OS" to "${System.getProperty("os.name")} " +
        "${System.getProperty("os.arch")} ${System.getProperty("os.version")}",
    )
  }

  from({
    configurations.runtimeClasspath.get().map {
      if (it.isDirectory) it else zipTree(it)
    }
  }) {
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
  }
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  destinationDirectory.set(file("dist/"))
  archiveBaseName.set("KoLmafia")
  archiveClassifier.set(if (isDirty()) "M" else "")
}

tasks.shadowJar {
  mustRunAfter("cleanDist")
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  destinationDirectory.set(file("dist/"))
  archiveClassifier.set(if (isDirty()) "M" else "")
}

val revisionProvider: Provider<String> =
  providers.provider {
    val commit = findProperty("commit")?.toString() ?: "HEAD"
    val rev =
      grgit
        .log {
          includes = listOf(commit)
        }.size - localCommits(commit)

    rev.toString()
  }

fun resolveGitDir(): File {
  val dotGit = file(".git")
  return if (dotGit.isFile) {
    // In a worktree, .git is a file containing "gitdir: /path/to/git/dir"
    val content = dotGit.readText().trim()
    if (content.startsWith("gitdir:")) {
      file(content.removePrefix("gitdir:").trim())
    } else {
      dotGit
    }
  } else {
    dotGit
  }
}

tasks.register("getRevision") {
  onlyIf {
    file(".git").exists()
  }
  val commit = findProperty("commit")?.toString() ?: "HEAD"
  val gitDir = resolveGitDir()
  inputs.dir(gitDir)
  inputs.property("commit", commit)
  outputs.files(file("build/revision.txt"))

  doLast {
    val revision = revisionProvider.get().trim()
    logger.info("Commit: {} Revision: {}", commit, revision)
    file("build/revision.txt").writeText(revision)
    // Update the version to the new revision
    project.version = revision
    val revString = if (isDirty()) "${project.version}-M" else project.version.toString()
    println("\nRevision: $revString")
  }
}

tasks.register("gitUpdate") {
  doLast {
    val remote =
      grgit.branch
        .current()
        .trackingBranch.name
    val latestHead = grgit.resolve.toCommit(remote)
    grgit.fetch {
      this.remote = remote
    }
    if (grgit.resolve.toCommit(remote) == latestHead) {
      println("Already up-to-date, nothing to do.")
      return@doLast
    }
    val dirty = isDirty()
    if (dirty) {
      // This pollutes the reflog, but there's no stash functionality in
      // grgit...
      grgit.commit {
        message = "temporary stash commit"
        all = true
      }
    }
    grgit.pull {
      rebase = true
    }
    if (dirty) {
      grgit.reset {
        setMode("mixed")
        commit = "HEAD^"
      }
    }
  }
}

java {
  sourceCompatibility = JavaVersion.toVersion(findProperty("javaSourceCompatibility").toString())
  targetCompatibility = JavaVersion.toVersion(findProperty("javaTargetCompatibility").toString())

  toolchain {
    languageVersion.set(JavaLanguageVersion.of(findProperty("javaTargetCompatibility").toString().toInt()))
  }
}

tasks.withType<Checkstyle>().configureEach {
  maxHeapSize = "2g"
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
  // Mockito requires dynamic loading for mock creation.
  jvmArgs("-XX:+EnableDynamicAgentLoading")
  testLogging {
    events(
      TestLogEvent.FAILED,
      TestLogEvent.SKIPPED,
      TestLogEvent.STANDARD_ERROR,
      TestLogEvent.STANDARD_OUT,
    )
    exceptionFormat = TestExceptionFormat.FULL
  }
}

tasks.register<Delete>("cleanJpackage") {
  onlyIf {
    file("build/releases").exists()
  }
  delete("build/releases")
}

gradle.taskGraph.whenReady {
  if (hasTask(":tsDefs") || hasTask(":test")) {
    tasks.compileJava
      .get()
      .options.compilerArgs
      .add("-parameters")
  }
}

tasks.register<JavaExec>("tsDefs") {
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("net.sourceforge.kolmafia.textui.TypescriptDefinition")
  doFirst {
    args(lastRevision())
  }
}

tasks.named("tsDefs") {
  dependsOn("getRevision")
}

tasks.jpackage {
  dependsOn("shadowJar", "cleanJpackage")
  input = file("dist")
  destination = file("build/releases")
  mainClass = "net.sourceforge.kolmafia.KoLmafia"
  appName = "KoLmafia"

  linux {
    type = ImageType.DEB
    icon = file("util/linux/KoLmafia.ico")
  }
  mac {
    type = ImageType.DMG
    icon = file("util/macosx/limeglass.icns")
  }
  windows {
    type = ImageType.EXE
    icon = file("util/windows/KoLmafia.ico")
    winShortcut = true
    winPerUserInstall = true
    javaOptions = listOf("-DuseCWDasROOT=true")
  }
  mainJar = "KoLmafia-" + lastRevision() + (if (isDirty()) "-M" else "") + ".jar"
  appVersion = SimpleDateFormat("yy.MM").format(Date()) + "." + lastRevision()
}

tasks.clean {
  dependsOn("cleanDist")
}
tasks.named("pruneDist") {
  dependsOn("getRevision")
}

// Note that pruneDist relies on getRevision.
tasks.jar {
  dependsOn("pruneDist")
}
tasks.shadowJar {
  dependsOn("pruneDist")
}

tasks.startShadowScripts {
  dependsOn("jar")
}
tasks.startScripts {
  dependsOn("jar")
}

tasks.distTar {
  dependsOn("shadowJar")
}
tasks.distZip {
  dependsOn("shadowJar")
}
tasks.startScripts {
  dependsOn("shadowJar")
}

tasks.jacocoTestReport {
  dependsOn("test")
}

fun isDirty(): Boolean = versioning.info.dirty || localCommits(findProperty("commit")?.toString() ?: "HEAD") > 0

fun localCommits(commit: String): Int =
  grgit
    .log {
      includes = listOf(commit)
      excludes = listOf("origin/main")
    }.size

fun lastRevision(): String {
  val revisionFile = file("build/revision.txt")
  return if (revisionFile.exists()) revisionFile.readText().trim() else "0"
}

// Set version from last build/revision.txt if up-to-date
version = lastRevision()
