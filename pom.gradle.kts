import java.util.function.Predicate
import java.util.function.Supplier

/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/05/25 12:26:10
 *
 * Java8Converter/Java8Converter/pom.gradle.kts
 */


open class PomTask : DefaultTask(), java.util.function.Supplier<MutableMap<String, Any>> {


    @Suppress("PrivatePropertyName")
    val HEX_DIGITS = charArrayOf('0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

    fun ByteArray.fastMD(type: String = "SHA-1"): ByteArray {
        val md = java.security.MessageDigest.getInstance(type)
        md.update(this)
        return md.digest()
    }

    fun File.fastMD(type: String = "sha1") {
        File("$path.$type").writeText(
                readBytes().fastMD(type).toHex()
        )
    }

    fun ByteArray.toHex(): String {
        return buildString(size * 2) {
            this@toHex.forEach {
                val iih = it.toInt() and 0xFF
                append(HEX_DIGITS[iih ushr 4 and 0xF])
                append(HEX_DIGITS[iih and 0xF])
            }
        }
    }

    private var map = HashMap<String, Any>()
    override fun get(): MutableMap<String, Any> {
        return map
    }

    @TaskAction
    @Suppress("UNCHECKED_CAST")
    fun invoke() {
        val filter: Predicate<Dependency> = map["filter"]
                as? Predicate<Dependency>
                ?: Predicate { true }
        val libs: Supplier<File>? = map["output"] as? Supplier<File>
        val cons: java.util.function.Consumer<String> = map["callback"]
                as? java.util.function.Consumer<String>
                ?: java.util.function.Consumer<String> { println(it) }
        var pom = "";
        project.configurations.named("-runtime").configure {
            cons.accept(buildString {
                append("""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <modelVersion>4.0.0</modelVersion>
    <groupId>${project.group}</groupId>
    <artifactId>${project.name}</artifactId>
    <version>${project.version}</version>
    <dependencies>
""")
                dependencies.forEach {
                    if (!filter.test(it)) return@forEach
                    append("""        <dependency>
            <groupId>${it.group}</groupId>
            <artifactId>${it.name}</artifactId>
            <version>${it.version}</version>
            <scope>runtime</scope>
        </dependency>
""")
                }
                append("    </dependencies>\n</project>")
            }.also { pom = it })
        }
        assert(pom.isNotEmpty()) { "pom is empty" }
        if (System.getProperty("user.name") == "Karlatemp") {
            val rootRepo = File("G:/KarlatempRepo")
            val location = File(rootRepo, "${
            project.group.toString().replace('.', '/')
            }/${project.name}/${project.version}")
            location.mkdirs()
            libs ?: error("No output found.")
            File(location, "${project.name}-${project.version}.jar").also { file ->
                java.io.FileOutputStream(file).use { output ->
                    java.io.FileInputStream(libs.get()).use { it.copyTo(output) }
                }
            }.fastMD("sha1")
            File(location, "${project.name}-${project.version}.pom").also { file ->
                file.writeBytes(pom.toByteArray(Charsets.UTF_8))
            }.fastMD("sha1")
        }
    }
}

tasks.create("publishToMavenLocal", PomTask::class.java).also {
    it.group = "karlatemp"
}
