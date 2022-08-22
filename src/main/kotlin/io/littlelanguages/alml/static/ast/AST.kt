package io.littlelanguages.alml.static.ast

import io.littlelanguages.data.Yamlable
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.Locationable


data class Program(
    val expressions: List<Expression>
) : Yamlable {
    override fun yaml(): Any =
        expressions.map { it.yaml() }
}

sealed class Expression(open val position: Location) : Yamlable, Locationable {
    override fun position(): Location = position
}


data class Symbol(
    override val position: Location,
    val name: String
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "Symbol",
            mapOf(
                Pair("value", name),
                Pair("position", position.yaml())
            )
        )
}

data class SExpression(
    override val position: Location,
    val expressions: List<Expression>
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "SExpression", expressions.map { it.yaml() }
        )
}

data class BlockExpression(
    override val position: Location,
    val expressions: List<Expression>
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "Block",
            mapOf(
                Pair("expressions", expressions.map { it.yaml() }),
                Pair("position", position.yaml())
            )
        )
}

data class ConstValue(
    override val position: Location,
    val symbol: Symbol,
    val expression: Expression
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "ConstValue",
            mapOf(
                Pair("symbol", symbol.yaml()),
                Pair("expression", expression.yaml()),
                Pair("position", position.yaml())
            )
        )
}

data class ConstProcedure(
    override val position: Location,
    val symbol: Symbol,
    val parameters: List<Symbol>,
    val expression: Expression
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "ConstProcedure",
            mapOf(
                Pair("symbol", symbol.yaml()),
                Pair("parameters", parameters.map { it.yaml() }),
                Pair("expression", expression.yaml()),
                Pair("position", position.yaml())
            )
        )
}

data class IfExpression(
    override val position: Location,
    val expressions: List<Expression>
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "If",
            mapOf(
                Pair("expressions", expressions.map { it.yaml() }),
                Pair("position", position.yaml())
            )
        )
}

data class ProcExpression(
    override val position: Location,
    val parameters: List<Symbol>,
    val expressions: List<Expression>
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "proc",
            mapOf(
                Pair("parameters", parameters.map { it.yaml() }),
                Pair("expressions", expressions.map { it.yaml() }),
                Pair("position", position.yaml())
            )
        )
}

data class SignalExpression(
    override val position: Location,
    val expression: Expression
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "Signal",
            mapOf(
                Pair("expression", expression.yaml()),
                Pair("position", position.yaml())
            )
        )
}

data class TryExpression(
    override val position: Location,
    val body: Expression,
    val catch: Expression
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "Try",
            mapOf(
                Pair("body", body.yaml()),
                Pair("catch", catch.yaml()),
                Pair("position", position.yaml())
            )
        )
}

data class LiteralInt(
    override val position: Location,
    val value: String
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "LiteralInt", mapOf(
                Pair("value", value),
                Pair("position", position.yaml())
            )
        )
}

data class LiteralString(
    override val position: Location,
    val value: String
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "LiteralString", mapOf(
                Pair("value", value),
                Pair("position", position.yaml())
            )
        )
}

data class LiteralUnit(
    override val position: Location,
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "LiteralUnit", mapOf(
                Pair("position", position.yaml())
            )
        )
}