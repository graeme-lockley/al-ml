package io.littlelanguages.alml.typed.st

import io.littlelanguages.alml.typed.typing.Scheme
import io.littlelanguages.alml.typed.typing.Substitution
import io.littlelanguages.alml.typed.typing.Type
import io.littlelanguages.data.Tuple2
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.Locationable


data class Program(
    val expressions: List<Expression>
) {
    fun apply(s: Substitution): Program =
        Program(expressions.map { it.apply(s) })
}

sealed class Expression(
    open val position: Location
) : Locationable {
    override fun position(): Location = position

    abstract fun apply(s: Substitution): Expression
}


data class TypedIdentifier(
    val position: Location,
    val id: Identifier,
    val type: Type?
) : Locationable {
    fun apply(s: Substitution): TypedIdentifier = TypedIdentifier(position, id, type?.apply(s))

    override fun position(): Location = position
}

data class Identifier(
    override val position: Location,
    val name: String
) : Expression(position) {
    override fun apply(s: Substitution): Expression = this
}

data class ApplyExpression(
    override val position: Location,
    val expressions: List<Expression>
) : Expression(position) {
    override fun apply(s: Substitution): Expression = ApplyExpression(position, expressions.map { it.apply(s) })
}

data class BinaryOpExpression(
    override val position: Location,
    val left: Expression,
    val op: BinaryOperator,
    val right: Expression
) : Expression(position) {
    override fun apply(s: Substitution): Expression = BinaryOpExpression(position, left.apply(s), op, right.apply(s))
}

data class BlockExpression(
    override val position: Location,
    val expressions: List<Expression>
) : Expression(position) {
    override fun apply(s: Substitution): Expression = BlockExpression(position, expressions.map { it.apply(s) })
}

data class LetValue(
    override val position: Location,
    val identifier: Identifier,
    val type: Type,
    val expression: Expression
) : Expression(position) {
    override fun apply(s: Substitution): Expression = LetValue(position, identifier, type.apply(s), expression.apply(s))
}

data class LetFunction(
    override val position: Location,
    val identifier: Identifier,
    val parameters: List<TypedIdentifier>,
    val scheme: Scheme,
    val expression: Expression
) : Expression(position) {
    override fun apply(s: Substitution): Expression =
        LetFunction(position, identifier, parameters.map { it.apply(s) }, scheme.apply(s), expression.apply(s))
}

data class IfExpression(
    override val position: Location,
    val ifThenExpressions: List<Tuple2<Expression, Expression>>,
    val elseExpression: Expression?
) : Expression(position) {
    override fun apply(s: Substitution): Expression =
        IfExpression(position, ifThenExpressions.map { Tuple2(it.a.apply(s), it.b.apply(s)) }, elseExpression?.apply(s))
}

data class LambdaExpression(
    override val position: Location,
    val parameters: List<TypedIdentifier>,
    val returnType: Type?,
    val expression: Expression
) : Expression(position) {
    override fun apply(s: Substitution): Expression =
        LambdaExpression(position, parameters.map { it.apply(s) }, returnType?.apply(s), expression.apply(s))
}

data class SignalExpression(
    override val position: Location,
    val expression: Expression
) : Expression(position) {
    override fun apply(s: Substitution): Expression =
        SignalExpression(position, expression.apply(s))
}

data class TryExpression(
    override val position: Location,
    val body: Expression,
    val catch: Expression
) : Expression(position) {
    override fun apply(s: Substitution): Expression =
        TryExpression(position, body.apply(s), catch.apply(s))
}

data class TypedExpression(
    override val position: Location,
    val expression: Expression,
    val type: Type
) : Expression(position) {
    override fun apply(s: Substitution): Expression =
        TypedExpression(position, expression.apply(s), type.apply(s))
}

data class LiteralInt(
    override val position: Location,
    val value: String
) : Expression(position) {
    override fun apply(s: Substitution): Expression =
        this
}

data class LiteralString(
    override val position: Location,
    val value: String
) : Expression(position) {
    override fun apply(s: Substitution): Expression =
        this
}

data class LiteralUnit(
    override val position: Location,
) : Expression(position) {
    override fun apply(s: Substitution): Expression =
        this
}

class BinaryOperator(
    private val position: Location,
    val operator: Operators
) : Locationable {
    override fun position(): Location = position
}

enum class Operators {
    Plus, Minus, Multiply, Divide, Equals, NotEquals, LessThan, LessEquals, GreaterThan, GreaterEquals
}
