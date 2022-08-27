package io.littlelanguages.alml.static.ast

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
    override fun yaml(): Any {
        TODO("Not yet implemented")
    }

    override fun position(): Location =
        position
}

data class Identifier(
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

data class ApplyExpression(
    override val position: Location,
    val expressions: List<Expression>
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "SExpression", expressions.map { it.yaml() }
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
                Pair("right", right.yaml()),
                Pair("position", position.yaml())
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
                Pair("expressions", expressions.map { it.yaml() }),
                Pair("position", position.yaml())
            )
        )
}

data class LetValue(
    override val position: Location,
    val identifier: Identifier,
    val type: Type?,
    val expression: Expression
) : Expression(position) {
    override fun yaml(): Any {
        val value = mapOf(
            Pair("identifier", identifier.yaml()),
            Pair("expression", expression.yaml()),
            Pair("position", position.yaml())
        )

        return singletonMap(
            "ConstValue",
            if (type == null) value else value + Pair("type", type.yaml())
        )
    }
}

data class LetFunction(
    override val position: Location,
    val identifier: Identifier,
    val parameters: List<TypedIdentifier>,
    val returnType: Type?,
    val expression: Expression
) : Expression(position) {
    override fun yaml(): Any {
        val value = mapOf(
            Pair("identifier", identifier.yaml()),
            Pair("parameters", parameters.map { it.yaml() }),
            Pair("expression", expression.yaml()),
            Pair("position", position.yaml())
        )

        return singletonMap(
            "ConstProcedure",
            if (returnType == null) value else value + Pair("return-type", returnType.yaml())
        )
    }
}

data class IfExpression(
    override val position: Location,
    val ifThenExpressions: List<Tuple2<Expression, Expression>>,
    val elseExpression: Expression?
) : Expression(position) {
    override fun yaml(): Any {
        val value = mapOf(
            Pair("if-expressions", ifThenExpressions.map { Pair(it.a.yaml(), it.b.yaml()) }),
            Pair("position", position.yaml())
        )

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
            Pair("expression", expression.yaml()),
            Pair("position", position.yaml())
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

data class TypedExpression(
    override val position: Location,
    val expression: Expression,
    val type: Type
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "Try",
            mapOf(
                Pair("expression", expression.yaml()),
                Pair("type", type.yaml()),
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

sealed class BinaryOperator(open val position: Location) : Yamlable, Locationable {
    override fun position(): Location = position
}

data class Plus(override val position: Location) : BinaryOperator(position) {
    override fun yaml(): Any = "Plus"
}

data class Minus(override val position: Location) : BinaryOperator(position) {
    override fun yaml(): Any = "Minus"
}

data class Multiply(override val position: Location) : BinaryOperator(position) {
    override fun yaml(): Any = "Multiply"
}

data class Divide(override val position: Location) : BinaryOperator(position) {
    override fun yaml(): Any = "Divide"
}

data class Equals(override val position: Location) : BinaryOperator(position) {
    override fun yaml(): Any = "Equals"
}

data class NotEquals(override val position: Location) : BinaryOperator(position) {
    override fun yaml(): Any = "NotEquals"
}

data class LessThan(override val position: Location) : BinaryOperator(position) {
    override fun yaml(): Any = "LessThan"
}

data class LessEquals(override val position: Location) : BinaryOperator(position) {
    override fun yaml(): Any = "LessEquals"
}

data class GreaterThan(override val position: Location) : BinaryOperator(position) {
    override fun yaml(): Any = "GreaterThan"
}

data class GreaterEquals(override val position: Location) : BinaryOperator(position) {
    override fun yaml(): Any = "GreaterEquals"
}

sealed class Type(
    open val position: Location
) : Yamlable, Locationable {
    override fun position(): Location = position
}

data class FunctionType(
    override val position: Location,
    val signature: List<Type>
) : Type(position) {
    override fun yaml(): Any =
        singletonMap(
            "FunctionType", mapOf(
                Pair("signature", signature.map { it.yaml() }),
                Pair("position", position.yaml())
            )
        )
}

data class AbstractDataType(
    override val position: Location,
    val identifier: Identifier,
    val arguments: List<Type>
) : Type(position) {
    override fun yaml(): Any =
        singletonMap(
            "AbstractDataType", mapOf(
                Pair("identifier", identifier.yaml()),
                Pair("arguments", arguments.map { it.yaml() }),
                Pair("position", position.yaml())
            )
        )
}

data class VariableType(
    override val position: Location,
    val identifier: Identifier
) : Type(position) {
    override fun yaml(): Any =
        singletonMap(
            "VariableType", mapOf(
                Pair("identifier", identifier.yaml()),
                Pair("position", position.yaml())
            )
        )
}

