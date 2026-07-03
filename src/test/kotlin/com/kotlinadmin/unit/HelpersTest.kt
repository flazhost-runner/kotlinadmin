package com.kotlinadmin.unit

import com.kotlinadmin.core.helpers.OtpHelper
import com.kotlinadmin.core.helpers.paginate
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe

class HelpersTest : DescribeSpec({

    describe("OtpHelper") {
        it("generates 6-digit numeric OTP") {
            val otp = OtpHelper.generate()
            otp.length shouldBeExactly 6
            otp.all { it.isDigit() }.shouldBeTrue()
        }

        it("hashes OTP and verifies correct value") {
            val otp = OtpHelper.generate()
            val hashed = OtpHelper.hash(otp)
            OtpHelper.verify(otp, hashed).shouldBeTrue()
        }

        it("rejects wrong OTP against hash") {
            val otp = OtpHelper.generate()
            val hashed = OtpHelper.hash(otp)
            OtpHelper.verify("000000", hashed).shouldBeFalse()
        }

        it("detects expired OTP (negative minutes)") {
            val expiredAt = OtpHelper.expiry(-1)
            OtpHelper.isExpired(expiredAt).shouldBeTrue()
        }

        it("accepts not-yet-expired OTP") {
            val validUntil = OtpHelper.expiry(10)
            OtpHelper.isExpired(validUntil).shouldBeFalse()
        }

        it("generates different OTPs on each call") {
            val otps = (1..10).map { OtpHelper.generate() }.toSet()
            (otps.size > 1) shouldBe true
        }
    }

    describe("paginate") {
        it("paginates 25 items at page 1, size 10") {
            val items = (1..25).toList()
            val result = paginate(items, page = 1, pageSize = 10)
            result.items.size shouldBeExactly 10
            result.paginateData.page shouldBeExactly 1
            result.paginateData.totalPages shouldBeExactly 3
            result.paginateData.total shouldBe 25L
            result.paginateData.hasNext.shouldBeTrue()
            result.paginateData.hasPrev.shouldBeFalse()
        }

        it("paginates 25 items at page 2, size 10") {
            val items = (1..25).toList()
            val result = paginate(items, page = 2, pageSize = 10)
            result.items.size shouldBeExactly 10
            result.paginateData.hasNext.shouldBeTrue()
            result.paginateData.hasPrev.shouldBeTrue()
        }

        it("paginates 25 items at last page") {
            val items = (1..25).toList()
            val result = paginate(items, page = 3, pageSize = 10)
            result.items.size shouldBeExactly 5
            result.paginateData.hasNext.shouldBeFalse()
            result.paginateData.hasPrev.shouldBeTrue()
        }

        it("handles empty list") {
            val result = paginate(emptyList<Int>(), page = 1, pageSize = 10)
            result.items.size shouldBeExactly 0
            result.paginateData.totalPages shouldBeExactly 0
            result.paginateData.total shouldBe 0L
            result.paginateData.hasNext.shouldBeFalse()
            result.paginateData.hasPrev.shouldBeFalse()
        }

        it("clamps out-of-range page to last available page") {
            val items = (1..5).toList()
            val result = paginate(items, page = 99, pageSize = 10)
            result.paginateData.page shouldBeExactly 1
        }
    }
})
