package io.littlelanguages.alml.typed

import io.littlelanguages.alml.Errors
import io.littlelanguages.alml.typed.st.*
import io.littlelanguages.alml.typed.typing.TArr
import io.littlelanguages.alml.typed.typing.TCon
import io.littlelanguages.alml.typed.typing.Type
import io.littlelanguages.alml.typed.typing.typeUnit
import io.littlelanguages.data.Either
import io.littlelanguages.data.Right
import io.littlelanguages.data.Tuple2

fun translate(p: io.littlelanguages.alml.static.ast.Program): Either<List<Errors>, Program> =
    Translator().apply(p)

private class Translator {
    fun apply(p: io.littlelanguages.alml.static.ast.Program): Either<List<Errors>, Program> =
        Right(Program(expressionsToST(p.expressions)))

    private fun expressionsToST(expressions: List<io.littlelanguages.alml.static.ast.Expression>): List<Expression> =
        expressions.map { expressionToST(it) }

    private fun expressionToST(expression: io.littlelanguages.alml.static.ast.Expression): Expression =
        when (expression) {
            is io.littlelanguages.alml.static.ast.ApplyExpression ->
                ApplyExpression(expression.position, expressionsToST(expression.expressions))

            is io.littlelanguages.alml.static.ast.BinaryOpExpression ->
                BinaryOpExpression(
                    expression.position,
                    expressionToST(expression.left),
                    binaryOperatorToST(expression.op),
                    expressionToST(expression.right)
                )

            is io.littlelanguages.alml.static.ast.BlockExpression ->
                BlockExpression(expression.position, expressionsToST(expression.expressions))

            is io.littlelanguages.alml.static.ast.Identifier ->
                Identifier(expression.position, expression.name)

            is io.littlelanguages.alml.static.ast.IfExpression ->
                IfExpression(
                    expression.position,
                    expression.ifThenExpressions.map { Tuple2(expressionToST(it.a), expressionToST(it.b)) },
                    if (expression.elseExpression == null) null else expressionToST(expression.elseExpression)
                )

            is io.littlelanguages.alml.static.ast.LambdaExpression ->
                LambdaExpression(
                    expression.position,
                    expression.parameters.map { typedIdentifierToST(it) },
                    if (expression.returnType == null) null else typeToST(expression.returnType),
                    expressionToST(expression.expression)
                )

            is io.littlelanguages.alml.static.ast.LetFunction ->
                LetFunction(
                    expression.position,
                    identifierToST(expression.identifier),
                    expression.parameters.map { typedIdentifierToST(it) },
                    if (expression.returnType == null) null else typeToST(expression.returnType),
                    expressionToST(expression.expression)
                )

            is io.littlelanguages.alml.static.ast.LetValue ->
                LetValue(
                    expression.position,
                    identifierToST(expression.identifier),
                    if (expression.type == null) null else typeToST(expression.type),
                    expressionToST(expression.expression)
                )

            is io.littlelanguages.alml.static.ast.LiteralInt ->
                LiteralInt(expression.position, expression.value)

            is io.littlelanguages.alml.static.ast.LiteralString ->
                LiteralString(expression.position, expression.value)

            is io.littlelanguages.alml.static.ast.LiteralUnit ->
                LiteralUnit(expression.position)

            is io.littlelanguages.alml.static.ast.SignalExpression ->
                SignalExpression(expression.position, expressionToST(expression.expression))

            is io.littlelanguages.alml.static.ast.TryExpression ->
                TryExpression(expression.position, expressionToST(expression.body), expressionToST(expression.catch))

            is io.littlelanguages.alml.static.ast.TypedExpression ->
                TypedExpression(expression.position, expressionToST(expression.expression), typeToST(expression.type))
        }

    private fun binaryOperatorToST(op: io.littlelanguages.alml.static.ast.BinaryOperator): BinaryOperator {
        val operator = when (op.operator) {
            io.littlelanguages.alml.static.ast.Operators.Plus -> Operators.Plus
            io.littlelanguages.alml.static.ast.Operators.Minus -> Operators.Minus
            io.littlelanguages.alml.static.ast.Operators.Multiply -> Operators.Multiply
            io.littlelanguages.alml.static.ast.Operators.Divide -> Operators.Divide
            io.littlelanguages.alml.static.ast.Operators.Equals -> Operators.Equals
            io.littlelanguages.alml.static.ast.Operators.NotEquals -> Operators.NotEquals
            io.littlelanguages.alml.static.ast.Operators.LessThan -> Operators.LessThan
            io.littlelanguages.alml.static.ast.Operators.LessEquals -> Operators.LessEquals
            io.littlelanguages.alml.static.ast.Operators.GreaterThan -> Operators.GreaterThan
            io.littlelanguages.alml.static.ast.Operators.GreaterEquals -> Operators.GreaterEquals
        }

        return BinaryOperator(op.position(), operator)
    }

    private fun typedIdentifierToST(typedIdentifier: io.littlelanguages.alml.static.ast.TypedIdentifier): TypedIdentifier =
        TypedIdentifier(
            typedIdentifier.position,
            identifierToST(typedIdentifier.id),
            if (typedIdentifier.type == null) null else typeToST(typedIdentifier.type)
        )

    private fun typeToST(type: io.littlelanguages.alml.static.ast.Type): Type =
        when (type) {
            is io.littlelanguages.alml.static.ast.AbstractDataType -> TCon(
                type.position,
                type.identifier.name,
                type.arguments.map { typeToST(it) })

            is io.littlelanguages.alml.static.ast.FunctionType -> {
                val types = type.signature.map { typeToST(it) }

                if (types.isEmpty()) typeUnit else types.dropLast(1).foldRight(types.last()) { a, b -> TArr(a, b) }
            }

            is io.littlelanguages.alml.static.ast.VariableType ->
                TODO("typeToST: $type")
        }

    private fun identifierToST(identifier: io.littlelanguages.alml.static.ast.Identifier): Identifier =
        Identifier(identifier.position, identifier.name)
}