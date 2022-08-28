package io.littlelanguages.alml.dynamic

import io.littlelanguages.alml.ArgumentMismatchError
import io.littlelanguages.alml.Errors
import io.littlelanguages.alml.dynamic.tst.Expressionss
import io.littlelanguages.alml.dynamic.tst.mapOfType
import io.littlelanguages.alml.static.ast.ApplyExpression
import io.littlelanguages.data.NestedMap
import io.littlelanguages.data.Yamlable

sealed interface Binding<S, T> : Yamlable {
    val name: String

    fun typeOf(): Type? =
        null
}

data class TopLevelValueBinding<S, T>(override val name: String) : Binding<S, T> {
    override fun yaml(): Any =
        singletonMap("toplevel-value", mapOfNameType(name, typeOf()))
}

data class ProcedureValueBinding<S, T>(override val name: String, val depth: Int, val offset: Int) : Binding<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "procedure-value", mapOfType(
                typeOf(),
                Pair("name", name),
                Pair("depth", depth),
                Pair("offset", offset)
            )
        )
}

sealed interface ProcedureBinding<S, T> : Binding<S, T>

data class DeclaredProcedureBinding<S, T>(override val name: String, val parameterCount: Int, val depth: Int) : ProcedureBinding<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "declared-procedure", mapOfType(
                typeOf(),
                Pair("name", name),
                Pair("parameter-count", parameterCount),
                Pair("depth", depth)
            )
        )

    fun isToplevel(): Boolean =
        depth == 0
}

abstract class ExternalValueBinding<S, T>(override val name: String) : Binding<S, T> {
    override fun yaml(): Any =
        singletonMap("external-value", mapOfNameType(name, typeOf()))

    abstract fun compile(state: S, lineNumber: Int = -1): T?
}

abstract class ExternalProcedureBinding<S, T>(
    override val name: String,
    open val arity: Int?
) : ProcedureBinding<S, T> {
    override fun yaml(): Any =
        singletonMap("external-procedure", mapOfNameType(name, typeOf()))

    fun validateArguments(e: ApplyExpression, name: String, arguments: Expressionss<S, T>): Errors? =
        when (arity) {
            null -> null
            arguments.size -> null
            else -> ArgumentMismatchError(name, arity!!, arguments.size, e.position)
        }

    abstract fun compile(state: S, lineNumber: Int = -1, arguments: Expressionss<S, T>): T?
}

data class ParameterBinding<S, T>(override val name: String, val depth: Int, val offset: Int) : Binding<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "parameter", mapOfType(
                typeOf(),
                Pair("name", name),
                Pair("depth", depth),
                Pair("offset", offset)
            )
        )
}

typealias Bindings<S, T> =
        NestedMap<String, Binding<S, T>>

private fun mapOfNameType(name: String, type: Type?): Any =
    if (type == null) name else mapOf(Pair("name", name), Pair("type", type.yaml()))