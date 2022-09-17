package io.littlelanguages.alml.typed.st

import io.littlelanguages.alml.typed.typing.Scheme
import io.littlelanguages.alml.typed.typing.Type
import io.littlelanguages.data.Tuple2
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


data class TypedIdentifier(
    val position: Location,
    val id: Identifier,
    val type: Type?
) : Yamlable, Locationable {
    override fun yaml(): Any = if (type == null) id.yaml() else singletonMap(
        "TypedIdentifier",
        mapOf(
            Pair("identifier", id.yaml()),
            Pair("type", type.yaml())
        )
    )

    override fun position(): Location =
        position
}

data class Identifier(
    override val position: Location,
    val name: String
) : Expression(position) {
    override fun yaml(): Any =
        name
}

data class ApplyExpression(
    override val position: Location,
    val expressions: List<Expression>
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "ApplyExpression", expressions.map { it.yaml() }
        )
}

data class BinaryOpExpression(
    override val position: Location,
    val left: Expression,
    val op: BinaryOperator,
    val right: Expression
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "BinaryOpExpression",
            mapOf(
                Pair("left", left.yaml()),
                Pair("op", op.yaml()),
                Pair("right", right.yaml())
            )
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
                Pair("expressions", expressions.map { it.yaml() })
            )
        )
}

data class LetValue(
    override val position: Location,
    val identifier: Identifier,
    val type: Type,
    val expression: Expression
) : Expression(position) {
    override fun yaml(): Any {
        return singletonMap(
            "ConstValue", mapOf(
                Pair("identifier", identifier.yaml()),
                Pair("type", type.yaml()),
                Pair("expression", expression.yaml())
            )
        )
    }
}

data class LetFunction(
    override val position: Location,
    val identifier: Identifier,
    val parameters: List<TypedIdentifier>,
    val scheme: Scheme,
    val expression: Expression
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "ConstProcedure", mapOf(
                Pair("identifier", identifier.yaml()),
                Pair("parameters", parameters.map { it.yaml() }),
                Pair("expression", expression.yaml()),
                Pair("scheme", scheme)
            )
        )
}

data class IfExpression(
    override val position: Location,
    val ifThenExpressions: List<Tuple2<Expression, Expression>>,
    val elseExpression: Expression?
) : Expression(position) {
    override fun yaml(): Any {
        val value = mapOf(Pair("if-expressions", ifThenExpressions.map { mapOf(Pair("guard", it.a.yaml()), Pair("body", it.b.yaml())) }))

        return singletonMap(
            "If",
            if (elseExpression == null) value else value + Pair("else-expression", elseExpression.yaml())
        )
    }
}

data class LambdaExpression(
    override val position: Location,
    val parameters: List<TypedIdentifier>,
    val returnType: Type?,
    val expression: Expression
) : Expression(position) {
    override fun yaml(): Any {
        val value = mapOf(
            Pair("parameters", parameters.map { it.yaml() }),
            Pair("expression", expression.yaml())
        )

        return singletonMap(
            "proc",
            if (returnType == null) value else value + Pair("return-type", returnType.yaml())
        )
    }
}

data class SignalExpression(
    override val position: Location,
    val expression: Expression
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "Signal",
            mapOf(
                Pair("expression", expression.yaml())
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
                Pair("catch", catch.yaml())
            )
        )
}

data class TypedExpression(
    override val position: Location,
    val expression: Expression,
    val type: Type
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "Typed",
            mapOf(
                Pair("expression", expression.yaml()),
                Pair("type", type.yaml())
            )
        )
}

data class LiteralInt(
    override val position: Location,
    val value: String
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap("LiteralInt", value)
}

data class LiteralString(
    override val position: Location,
    val value: String
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap("LiteralString", value)
}

data class LiteralUnit(
    override val position: Location,
) : Expression(position) {
    override fun yaml(): Any =
        "LiteralUnit"
}

class BinaryOperator(private val position: Location, val operator: Operators) : Yamlable, Locationable {
    override fun position(): Location = position

    override fun yaml(): Any =
        operator.name
}

enum class Operators {
    Plus, Minus, Multiply, Divide,
    Equals, NotEquals,
    LessThan, LessEquals, GreaterThan, GreaterEquals
}
