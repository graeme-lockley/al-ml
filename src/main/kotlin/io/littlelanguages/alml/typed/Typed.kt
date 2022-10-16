package io.littlelanguages.alml.typed

import io.littlelanguages.alml.Errors
import io.littlelanguages.alml.static.parse
import io.littlelanguages.alml.typed.st.*
import io.littlelanguages.alml.typed.typing.*
import io.littlelanguages.data.Tuple2
import java.io.Reader

fun translate(p: io.littlelanguages.alml.static.ast.Program, errors: Errors): Program =
    Translator(errors).apply(p)

fun translate(reader: Reader, errors: Errors): Program {
    val ast = parse(reader, errors)

    return if (errors.reported())
        Program(listOf())
    else
        Translator(errors).apply(ast)
}

private class Translator(private val errors: Errors) {
    val environment = initialEnvironment()
    val pump = VarPump()
    val constraints = Constraints()

    fun apply(program: io.littlelanguages.alml.static.ast.Program): Program {
        val p = Program(expressionsToST(program.expressions))

        return if (errors.reported()) {
            p
        } else {
            val substitution = constraints.solve(errors)

            p.apply(substitution)
        }
    }

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
                    nullTypeToType(expression.returnType),
                    expressionToST(expression.expression)
                )

            is io.littlelanguages.alml.static.ast.LetFunction -> {
                val scheme = inferAndBindProcedureType(expression, errors, pump, environment)

                LetFunction(
                    expression.position,
                    identifierToST(expression.identifier),
                    expression.parameters.map { typedIdentifierToST(it) },
                    scheme,
                    expressionToST(expression.expression)
                )
            }

            is io.littlelanguages.alml.static.ast.LetValue -> {
                val type = inferAndBindValueType(expression, errors, pump, environment)

                LetValue(
                    expression.position,
                    identifierToST(expression.identifier),
                    type,
                    expressionToST(expression.expression)
                )
            }

            is io.littlelanguages.alml.static.ast.LiteralS32 ->
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
                TypedExpression(expression.position, expressionToST(expression.expression), typeToType(expression.type))
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
            nullTypeToType(typedIdentifier.type)
        )

    private fun identifierToST(identifier: io.littlelanguages.alml.static.ast.Identifier): Identifier =
        Identifier(identifier.position, identifier.name)
}

fun nullTypeToType(type: io.littlelanguages.alml.static.ast.Type?): Type? =
    nullMap(type) { typeToType(it) }

fun typeToType(type: io.littlelanguages.alml.static.ast.Type): Type =
    when (type) {
        is io.littlelanguages.alml.static.ast.AbstractDataType -> TCon(
            type.position,
            type.identifier.name
        )

        is io.littlelanguages.alml.static.ast.FunctionType -> {
            val types = type.signature.map { typeToType(it) }

            if (types.isEmpty()) typeUnit else types.dropLast(1).foldRight(types.last()) { a, b -> TArr(a, b) }
        }

        is io.littlelanguages.alml.static.ast.VariableType ->
            TODO("typeToST: $type")
    }

fun <S, T> nullMap(value: S?, f: (S) -> T): T? =
    if (value == null) null else f(value)