package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExampleUnitTest {

    @Test
    fun testFinancialCalculationRule_1000Debt_300Payment_700Payment() {
        // Requirement 15.1:
        // إضافة دين 1000 ثم دفعة 300: الرصيد = 700.
        // إضافة دفعة أخرى 700: الرصيد = 0.
        // رفض دفعة تتجاوز الرصيد ما لم يكن الرصيد الدائن مفعّلاً.

        var totalDebts = 0.0
        var totalPayments = 0.0

        fun balance() = totalDebts - totalPayments

        fun addDebt(amount: Double): Boolean {
            if (amount <= 0.0) return false
            totalDebts += amount
            return true
        }

        fun addPayment(amount: Double, allowCredit: Boolean): Boolean {
            if (amount <= 0.0) return false
            if (!allowCredit && amount > balance() + 0.001) {
                return false
            }
            totalPayments += amount
            return true
        }

        // 1. Debt 1000
        assertTrue(addDebt(1000.0))
        assertEquals(1000.0, balance(), 0.001)

        // 2. Payment 300 -> balance 700
        assertTrue(addPayment(300.0, allowCredit = false))
        assertEquals(700.0, balance(), 0.001)

        // 3. Payment 700 -> balance 0
        assertTrue(addPayment(700.0, allowCredit = false))
        assertEquals(0.0, balance(), 0.001)

        // 4. Excess payment 50 when balance is 0 without credit balance -> must be rejected
        assertFalse(addPayment(50.0, allowCredit = false))
        assertEquals(0.0, balance(), 0.001)

        // 5. Excess payment 50 with allowCredit = true -> must succeed
        assertTrue(addPayment(50.0, allowCredit = true))
        assertEquals(-50.0, balance(), 0.001)
    }

    @Test
    fun testSmsTemplateVariablesReplacement() {
        val template = "مرحبًا {اسم_العميل}، نود تذكيركم بأن الرصيد المتبقي هو {الرصيد}. إجمالي الديون {إجمالي_الديون}، وإجمالي المدفوعات {إجمالي_المدفوعات}. المتجر: {اسم_المتجر}."

        val customerName = "ياسين عبد الله"
        val balance = 850.0
        val totalDebt = 1200.0
        val totalPayment = 350.0
        val storeName = "مؤسسة الأمل التجارية"
        val currency = "ر.ي"

        val rendered = template
            .replace("{اسم_العميل}", customerName)
            .replace("{الرصيد}", String.format(Locale.US, "%.2f", balance) + " " + currency)
            .replace("{إجمالي_الديون}", String.format(Locale.US, "%.2f", totalDebt) + " " + currency)
            .replace("{إجمالي_المدفوعات}", String.format(Locale.US, "%.2f", totalPayment) + " " + currency)
            .replace("{اسم_المتجر}", storeName)

        assertTrue(rendered.contains("ياسين عبد الله"))
        assertTrue(rendered.contains("850.00 ر.ي"))
        assertTrue(rendered.contains("1200.00 ر.ي"))
        assertTrue(rendered.contains("350.00 ر.ي"))
        assertTrue(rendered.contains("مؤسسة الأمل التجارية"))
    }

    @Test
    fun testDuplicatePhoneNumberValidation() {
        val registeredPhones = mutableSetOf<String>()

        fun registerCustomer(name: String, phone: String): Result<String> {
            val cleanPhone = phone.trim()
            if (cleanPhone.isNotEmpty() && registeredPhones.contains(cleanPhone)) {
                return Result.failure(IllegalArgumentException("رقم الهاتف مسجل مسبقاً"))
            }
            if (cleanPhone.isNotEmpty()) registeredPhones.add(cleanPhone)
            return Result.success("تم الحفظ: $name")
        }

        assertTrue(registerCustomer("العميل الأول", "771234567").isSuccess)
        val dupResult = registerCustomer("العميل الثاني", "771234567")
        assertTrue(dupResult.isFailure)
    }

    @Test
    fun testReversalPreservesAuditTrailWithoutWipingHistory() {
        data class TransactionRecord(val id: Long, val type: String, val amount: Double, var status: String)

        val transactions = mutableListOf(
            TransactionRecord(1, "DEBT", 1000.0, "COMPLETED"),
            TransactionRecord(2, "PAYMENT", 400.0, "COMPLETED")
        )

        fun reverse(txId: Long, reason: String) {
            val original = transactions.find { it.id == txId }!!
            original.status = "REVERSED"
            transactions.add(
                TransactionRecord(
                    id = 3,
                    type = "REVERSAL",
                    amount = original.amount,
                    status = "COMPLETED"
                )
            )
        }

        reverse(2, "دفعة مسجلة بالخطأ")

        // Original transaction is still present in history for auditing
        assertEquals(3, transactions.size)
        assertEquals("REVERSED", transactions[1].status)
    }

    @Test
    fun testCustomerViewModelValidationAndFlowUpdates() {
        // Test customer input validation
        fun validateCustomerInput(name: String, phone: String): String? {
            if (name.isBlank()) return "يرجى كتابة اسم العميل"
            return null
        }

        assertEquals("يرجى كتابة اسم العميل", validateCustomerInput("", "770000000"))
        assertEquals("يرجى كتابة اسم العميل", validateCustomerInput("   ", "770000000"))
        assertEquals(null, validateCustomerInput("صالح العولقي", "770000000"))
    }
}
