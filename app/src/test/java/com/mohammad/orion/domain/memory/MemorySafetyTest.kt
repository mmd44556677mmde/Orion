package com.mohammad.orion.domain.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemorySafetyTest {
    private val scanner = MemorySecretScanner()
    private val evaluator = MemoryPolicyEvaluator()

    @Test fun defaultPolicyDeniesEveryAgentOperationAndSensitivity() {
        MemoryOperation.entries.forEach { op -> MemorySensitivity.entries.forEach { sensitivity ->
            assertEquals(MemoryDecision.DENY, evaluator.evaluate(MemoryPolicy(), op, sensitivity))
        } }
    }
    @Test fun enabledOperationsRequireConfirmationForSensitiveAndMutations() {
        val policy = MemoryPolicy(true, true, true, true)
        MemoryOperation.entries.forEach { op ->
            assertEquals(MemoryDecision.REQUIRE_CONFIRMATION, evaluator.evaluate(policy, op, MemorySensitivity.SENSITIVE))
            val expected = if (op in setOf(MemoryOperation.SEARCH, MemoryOperation.READ, MemoryOperation.ADD)) MemoryDecision.ALLOW else MemoryDecision.REQUIRE_CONFIRMATION
            assertEquals(expected, evaluator.evaluate(policy, op, MemorySensitivity.NORMAL))
        }
    }
    @Test fun independentPermissionsCannotEnableOtherOperations() {
        assertEquals(MemoryDecision.ALLOW, evaluator.evaluate(MemoryPolicy(agentReadAllowed = true), MemoryOperation.READ, MemorySensitivity.NORMAL))
        assertEquals(MemoryDecision.DENY, evaluator.evaluate(MemoryPolicy(agentReadAllowed = true), MemoryOperation.ADD, MemorySensitivity.NORMAL))
        assertEquals(MemoryDecision.DENY, evaluator.evaluate(MemoryPolicy(agentEditAllowed = true), MemoryOperation.DELETE, MemorySensitivity.NORMAL))
    }
    @Test fun detectsCredentialsAcrossFieldsWithoutReturningTheirValues() {
        val found = scanner.inspect(listOf("ordinary title", "Password: hunter22", "OTP = 123456", "Bearer abcdefghijklmnop", "-----BEGIN PRIVATE KEY-----"))
        assertEquals(setOf(SecretKind.CREDENTIAL, SecretKind.PRIVATE_KEY), found)
    }
    @Test fun validatesCardChecksumAndAvoidsCommonFalsePositives() {
        assertEquals(setOf(SecretKind.PAYMENT_CARD), scanner.inspect(listOf("4111 1111 1111 1111")))
        assertTrue(scanner.inspect(listOf("4111 1111 1111 1112", "password policy", "1234567890123", "1111111111111111")).isEmpty())
    }
    @Test fun detectsCommonNamedCredentialsCaseInsensitively() {
        listOf("API key = abc123", "refresh-token: abcdef", "one-time code is 123456", "passphrase: phrase123").forEach {
            assertEquals(setOf(SecretKind.CREDENTIAL), scanner.inspect(listOf(it)))
        }
    }
}
