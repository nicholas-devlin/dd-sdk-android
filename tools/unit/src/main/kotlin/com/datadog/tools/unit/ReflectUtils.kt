package com.datadog.tools.unit

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.jvm.isAccessible

/**
 * Creates an instance of the given class name.
 * @param className the full name of the class to instantiate
 * @param params the parameters to provide the constructor
 * @return the created instance
 */
@Suppress("SpreadOperator")
fun createInstance(
    className: String,
    vararg params: Any?
): Any {
    return Class.forName(className)
        .kotlin
        .constructors.first()
        .apply { isAccessible = true }
        .call(*params)
}

/**
 * Sets a static value on the target class.
 * @param fieldName the name of the field
 * @param fieldValue the value to set
 */
inline fun <reified T, R> Class<T>.setStaticValue(
    fieldName: String,
    fieldValue: R
) {
    val field = getDeclaredField(fieldName)

    // make it accessible
    field.isAccessible = true

    // Make it non final
    val modifiersField = Field::class.java.getDeclaredField("modifiers")
    modifiersField.isAccessible = true
    modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
    field.set(null, fieldValue)
}

/**
 * Gets the static value from the target class.
 * @param fieldName the name of the field
 */
inline fun <reified T, reified R> Class<T>.getStaticValue(fieldName: String): R {

    val field = getDeclaredField(fieldName)

    // make it accessible
    field.isAccessible = true

    return field.get(null) as R
}

/**
 * Sets the field value on the target instance.
 * @param fieldName the name of the field
 * @param fieldValue the value of the field
 */
inline fun <reified T> Any.setFieldValue(
    fieldName: String,
    fieldValue: T
) {
    val field = javaClass.getDeclaredField(fieldName)

    // make it accessible
    field.isAccessible = true

    // Make it non final
    val modifiersField = Field::class.java.getDeclaredField("modifiers")
    modifiersField.isAccessible = true
    modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())

    field.set(this, fieldValue)
}

/**
 * Gets the field value from the target instance.
 * @param fieldName the name of the field
 */
inline fun <reified T> Any.getFieldValue(fieldName: String): T {
    val field = this.javaClass.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(this) as T
}

/**
 * Invokes a method on the target instance.
 * @param methodName the name of the method
 * @param params the parameters to provide the method
 * @return the result from the invoked method
 */
@Suppress("SpreadOperator", "UNCHECKED_CAST", "TooGenericExceptionCaught")
fun <T : Any> T.invokeMethod(
    methodName: String,
    vararg params: Any?
): Any? {
    val declarationParams = Array<Class<*>?>(params.size) {
        params[it]?.javaClass
    }

    val method = getDeclaredMethodRecursively(methodName, true, declarationParams)
    val wasAccessible = method.isAccessible

    val output: Any?
    method.isAccessible = true
    try {
        output = if (params.isEmpty()) {
            method.invoke(this)
        } else {
            method.invoke(this, *params)
        }
    } catch (e: InvocationTargetException) {
        throw e.cause ?: e
    } finally {
        method.isAccessible = wasAccessible
    }

    return output
}

/**
 * Invokes a method on the target instance, where one or more of the parameters
 * are generics.
 * @param methodName the name of the method
 * @param params the parameters to provide the method
 * @return the result from the invoked method
 */
@Suppress("SpreadOperator", "UNCHECKED_CAST")
fun <T : Any> T.invokeGenericMethod(
    methodName: String,
    vararg params: Any
): Any? {
    val declarationParams = Array<Class<*>?>(params.size) {
        params[it].javaClass
    }

    val method = getDeclaredMethodRecursively(methodName, false, declarationParams)
    val wasAccessible = method.isAccessible

    val output: Any?
    method.isAccessible = true
    try {
        output = if (params.isEmpty()) {
            method.invoke(this)
        } else {
            method.invoke(this, *params)
        }
    } catch (e: InvocationTargetException) {
        throw e.cause ?: e
    } finally {
        method.isAccessible = wasAccessible
    }

    return output
}

@Suppress("TooGenericExceptionCaught", "SwallowedException", "SpreadOperator")
private fun <T : Any> T.getDeclaredMethodRecursively(
    methodName: String,
    matchingParams: Boolean,
    declarationParams: Array<Class<*>?>
): Method {
    val classesToSearch = mutableListOf<Class<*>>(this.javaClass)
    val classesSearched = mutableListOf<Class<*>>()
    var method: Method?
    do {
        val lookingInClass = classesToSearch.removeAt(0)
        classesSearched.add(lookingInClass)
        method = try {
            if (matchingParams) {
                lookingInClass.getDeclaredMethod(methodName, *declarationParams)
            } else {
                lookingInClass.declaredMethods.firstOrNull {
                    it.name == methodName &&
                        it.parameterTypes.size == declarationParams.size
                }
            }
        } catch (e: Throwable) {
            null
        }

        val superclass = lookingInClass.superclass
        if (superclass != null &&
            !classesToSearch.contains(superclass) &&
            !classesSearched.contains(superclass)
        ) {
            classesToSearch.add(superclass)
        }
        lookingInClass.interfaces.forEach {
            if (!classesToSearch.contains(it) && !classesSearched.contains(it)) {
                classesToSearch.add(it)
            }
        }
    } while (method == null && classesToSearch.isNotEmpty())

    checkNotNull(method) {
        "Unable to access method $methodName on ${javaClass.canonicalName}"
    }

    return method
}
