package com.kotlinadmin.integration

import com.kotlinadmin.config.StorageConfig
import com.kotlinadmin.core.storage.LocalStorageService
import com.kotlinadmin.core.storage.ObjectStorageService
import com.kotlinadmin.module
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Kontrak storage adapter — RENDER URL per driver + penyajian file lokal.
 * Membuktikan berpindah `STORAGE_DRIVER` hanya mengubah URL, bukan kode pemakai.
 */
class StorageServiceTest : DescribeSpec({

    describe("LocalStorageService") {
        val baseDir = File(System.getProperty("java.io.tmpdir"), "kotlinadmin-storage-test-${System.nanoTime()}")
        val storage = LocalStorageService(baseDir.absolutePath)

        it("url(key) mengembalikan prefix stabil /storage/<key> (bukan path filesystem)") {
            // Base path absolut tetap menghasilkan URL valid (dipisah dari fs).
            storage.url("media/logo.webp") shouldBe "/storage/media/logo.webp"
        }

        it("put lalu list menemukan object dengan ukuran benar") {
            runBlocking {
                storage.put("media/a.txt", "hello".toByteArray())
                val listed = storage.list("media")
                listed.any { it.key == "media/a.txt" } shouldBe true
                listed.first { it.key == "media/a.txt" }.size shouldBe 5L
            }
        }

        it("delete menghapus object") {
            runBlocking {
                storage.put("media/b.txt", "x".toByteArray())
                storage.delete("media/b.txt")
                storage.list("media").none { it.key == "media/b.txt" } shouldBe true
            }
        }

        it("localMount memberi (prefix, dir) untuk staticFiles") {
            storage.localMount().first shouldBe "/storage"
        }
    }

    describe("ObjectStorageService (driver=s3/oss) → URL absolut") {
        val s3 = ObjectStorageService(
            StorageConfig(
                driver = "s3",
                basePath = "uploads",
                accessKeyId = "AKIA_TEST",
                secretAccessKey = "secret",
                endpoint = "",
                bucket = "my-bucket",
                region = "ap-southeast-1",
                ssl = true
            )
        )
        val oss = ObjectStorageService(
            StorageConfig(
                driver = "oss",
                basePath = "uploads",
                accessKeyId = "LTAI_TEST",
                secretAccessKey = "secret",
                endpoint = "oss-ap-southeast-5.aliyuncs.com",
                bucket = "my-bucket",
                region = "",
                ssl = true
            )
        )

        it("s3 → presigned URL absolut (virtual-hosted, SigV4)") {
            val u = s3.url("media/logo.webp")
            u shouldStartWith "https://my-bucket.s3.ap-southeast-1.amazonaws.com/media/logo.webp?"
            u shouldContain "X-Amz-Algorithm=AWS4-HMAC-SHA256"
            u shouldContain "X-Amz-Signature="
        }

        it("oss → URL publik virtual-hosted absolut") {
            oss.url("media/logo.webp") shouldBe
                "https://my-bucket.oss-ap-southeast-5.aliyuncs.com/media/logo.webp"
        }

        it("kredensial kosong → fallback path relatif (tak crash render)") {
            val bare = ObjectStorageService(
                StorageConfig(
                    driver = "s3",
                    basePath = "uploads",
                    accessKeyId = "",
                    secretAccessKey = "",
                    endpoint = "",
                    bucket = "b",
                    region = "",
                    ssl = true
                )
            )
            bare.url("media/x.png") shouldEndWith "/media/x.png"
        }

        it("localMount null (tak ada penyajian lokal untuk object storage)") {
            s3.localMount() shouldBe null
        }
    }

    describe("driver=local menyajikan file unggahan di /storage/<key>") {
        it("GET /storage/<file> mengembalikan 200 + isi file") {
            // Tulis file ke base path default (uploads) lalu ambil via route static.
            val name = "storage-route-test-${System.nanoTime()}.txt"
            val f = File("uploads/$name")
            f.parentFile?.mkdirs()
            f.writeText("kotlinadmin-storage-ok")
            try {
                testApplication {
                    application { module() }
                    val res = client.get("/storage/$name")
                    res.status shouldBe HttpStatusCode.OK
                    res.bodyAsText() shouldContain "kotlinadmin-storage-ok"
                }
            } finally {
                f.delete()
            }
        }
    }
})
