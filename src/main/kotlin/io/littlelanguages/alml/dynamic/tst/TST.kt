package io.littlelanguages.alml.dynamic.tst

import io.littlelanguages.alml.dynamic.*
import io.littlelanguages.alml.dynamic.typing.*
import io.littlelanguages.alml.static.ast.BinaryOperator
import io.littlelanguages.alml.static.ast.Operators
import io.littlelanguages.data.Yamlable

data class Program<S, T>(val values: List<String>, val declarations: List<Declaration<S, T>>) : Yamlable {
    override fun yaml(): Any =
        singletonMap(
            "program", mapOf(
                Pair("values", values),
                Pair("procedures", declarations.map { it.yaml() })
            )
        )
}

sealed interface Declaration<S, T> : Yamlable

data class Procedure<S, T>(val name: String, val parameters: List<String>, val depth: Int, val offsets: Int, val es: Expressions<S, T>) :
    Declaration<S, T>, Expression<S, T> {
    override fun typeOf(): Type? =
        null

    override fun yaml(): Any =
        singletonMap(
            "procedure", mapOfType(
                typeOf(),
                Pair("name", name),
                Pair("parameters", parameters),
                Pair("depth", depth),
                Pair("offsets", offsets),
                Pair("es", es.map { it.yaml() })
            )
        )
}

typealias Expressions<S, T> = List<Expression<S, T>>
typealias Expressionss<S, T> = List<List<Expression<S, T>>>

interface Expression<S, T> : Yamlable {
    fun typeOf(): Type?
}

data class AssignExpression<S, T>(val symbol: Binding<S, T>, val es: Expressions<S, T>) : Expression<S, T> {
    override fun typeOf(): Type? =
        symbol.typeOf()

    override fun yaml(): Any =
        singletonMap(
            "assign", mapOfType(
                typeOf(),
                Pair("symbol", symbol.yaml()),
                Pair("es", es.map { it.yaml() })
            )
        )
}

data class BinaryOpExpression<S, T>(
    val left: Expressions<S, T>,
    val op: BinaryOperator,
    val right: Expressions<S, T>,
    val lineNumber: Int
) : Expression<S, T> {
    override fun typeOf(): Type =
        op.typeOf()

    override fun yaml(): Any =
        singletonMap(
            "binary-op-expression",
            mapOfType(
                typeOf(),
                Pair("left", left.map { it.yaml() }),
                Pair("op", op.yaml()),
                Pair("right", right.map { it.yaml() }),
                Pair("line-number", lineNumber)
            )
        )
}

fun BinaryOperator.typeOf(): Type =
    when (this.operator) {
        Operators.Plus,
        Operators.Minus,
        Operators.Multiply,
        Operators.Divide -> typeS32
        Operators.Equals,
        Operators.NotEquals,
        Operators.LessThan,
        Operators.LessEquals,
        Operators.GreaterThan,
        Operators.GreaterEquals -> typeBool
    }

data class CallProcedureExpression<S, T>(val procedure: ProcedureBinding<S, T>, val es: List<Expressions<S, T>>, val lineNumber: Int) :
    Expression<S, T> {
    override fun typeOf(): Type? =
        procedure.typeOf()

    override fun yaml(): Any =
        singletonMap(
            "call-procedure", mapOfType(
                typeOf(),
                Pair("procedure", procedure.yaml()),
                Pair("es", es.map { e -> e.map { it.yaml() } }),
                Pair("line-number", lineNumber)
            )
        )
}

data class CallValueExpression<S, T>(val operand: Expressions<S, T>, val es: Expressions<S, T>, val lineNumber: Int) : Expression<S, T> {
    override fun typeOf(): Type? =
        operand.lastOrNull()?.typeOf()

    override fun yaml(): Any =
        singletonMap(
            "call-value", mapOfType(
                typeOf(),
                Pair("operand", operand.map { it.yaml() }),
                Pair("es", es.map { it.yaml() })
            )
        )
}

data class IfExpression<S, T>(val e1: Expressions<S, T>, val e2: Expressions<S, T>, val e3: Expressions<S, T>) : Expression<S, T> {
    override fun typeOf(): Type? =
        e2.lastOrNull()?.typeOf() ?: e3.lastOrNull()?.typeOf()

    override fun yaml(): Any =
        singletonMap(
            "if", mapOfType(
                typeOf(),
                Pair("e1", e1.map { it.yaml() }),
                Pair("e2", e2.map { it.yaml() }),
                Pair("e3", e3.map { it.yaml() })
            )
        )
}

data class SignalExpression<S, T>(val es: Expressions<S, T>, val lineNumber: Int) : Expression<S, T> {
    override fun typeOf(): Type =
        typeUnit

    override fun yaml(): Any =
        singletonMap(
            "signal", es.map { it.yaml() }
        )
}

data class IdentifierExpression<S, T>(val symbol: Binding<S, T>, val lineNumber: Int) : Expression<S, T> {
    override fun typeOf(): Type? =
        symbol.typeOf()

    override fun yaml(): Any =
        symbol.yaml()
}

data class TryExpression<S, T>(val body: IdentifierExpression<S, T>, val catch: IdentifierExpression<S, T>, val lineNumber: Int) :
    Expression<S, T> {
    override fun typeOf(): Type? =
        body.typeOf() ?: catch.typeOf()

    override fun yaml(): Any =
        singletonMap(
            "try", mapOfType(
                typeOf(),
                Pair("body", body.yaml()),
                Pair("catch", catch.yaml())
            )
        )
}

data class LiteralS32<S, T>(val value: Int) : Expression<S, T> {
    override fun typeOf(): Type =
        typeS32

    override fun yaml(): Any =
        value
}

data class LiteralString<S, T>(val value: String) : Expression<S, T> {
    override fun typeOf(): Type =
        typeString

    override fun yaml(): Any =
        value
}

class LiteralUnit<S, T> : Expression<S, T> {
    override fun typeOf(): Type =
        typeUnit

    override fun yaml(): Any = "()"
}

fun mapOfType(type: Type?, vararg pairs: Pair<String, Any>): Map<String, Any> {
    val result = mapOf(*pairs)

    return if (type == null) result else result + Pair("type", type.yaml())
}
