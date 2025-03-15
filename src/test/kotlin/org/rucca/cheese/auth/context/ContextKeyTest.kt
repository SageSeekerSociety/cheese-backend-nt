package org.rucca.cheese.auth.context

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Context Key Tests")
class ContextKeyTest {

    @Test
    @DisplayName("Should get typed value from context")
    fun shouldGetTypedValueFromContext() {
        // Arrange
        val stringKey = ContextKey.of<String>("stringKey")
        val intKey = ContextKey.of<Int>("intKey")
        val context = mapOf("stringKey" to "Hello", "intKey" to 42)

        // Act
        val stringValue = stringKey.get(context)
        val intValue = intKey.get(context)

        // Assert
        assertThat(stringValue).isEqualTo("Hello")
        assertThat(intValue).isEqualTo(42)
    }

    @Test
    @DisplayName("Should put typed value into mutable context")
    fun shouldPutTypedValueIntoMutableContext() {
        // Arrange
        val stringKey = ContextKey.of<String>("stringKey")
        val intKey = ContextKey.of<Int>("intKey")
        val mutableContext = mutableMapOf<String, Any>()

        // Act
        stringKey.put(mutableContext, "World")
        intKey.put(mutableContext, 99)

        // Assert
        assertThat(mutableContext["stringKey"]).isEqualTo("World")
        assertThat(mutableContext["intKey"]).isEqualTo(99)
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    fun shouldHandleNullValuesGracefully() {
        // Arrange
        val stringKey = ContextKey.of<String>("missingKey")
        val context = mapOf("otherKey" to "Hello")

        // Act
        val value = stringKey.get(context)

        // Assert
        assertThat(value).isNull()
    }

    @Test
    @DisplayName("Should handle type mismatches gracefully")
    fun shouldHandleTypeMismatchesGracefully() {
        // Arrange
        val intKey = ContextKey.of<Int>("stringKey")
        val context = mapOf("stringKey" to "Not an integer")

        // Act
        val value = intKey.get(context)

        // Assert
        assertThat(value).isNull()
    }
}
