package io.littlelanguages.alml.typed.yaml

import io.littlelanguages.alml.typed.st.*
import io.littlelanguages.alml.typed.typing.Type

fun x(p: Program): Any =
    p.expressions.map { x(it) }

private fun x(e: Expression): Any = when (e) {
    is Identifier -> e.name

    is ApplyExpression -> singletonMap("ApplyExpression", e.expressions.map { x(it) })

    is BinaryOpExpression -> mapOf(
        "BinaryOpExpression" to mapOf(
            "left" to x(e.left),
            "op" to x(e.op),
            "right" to x(e.right)
        )
    )

    is BlockExpression -> singletonMap("Block", e.expressions.map { x(it) })

    is LetValue -> mapOf(
        "ConstValue" to mapOf(
            "identifier" to x(e.identifier),
            "type" to x(e.type),
            "expression" to x(e.expression)
        )
    )

    is LetFunction -> mapOf(
        "ConstProcedure" to mapOf(
            "identifier" to x(e.identifier),
            "parameters" to e.parameters.map { x(it) },
            "expression" to x(e.expression),
            "scheme" to e.scheme
        )
    )

    is IfExpression -> {
        val value = mapOf("if-expressions" to e.ifThenExpressions.map { mapOf("guard" to x(it.a), "body" to x(it.b)) })
        val elseExpression = e.elseExpression

        singletonMap(
            "If", if (elseExpression == null) value else value + ("else-expression" to x(elseExpression))
        )
    }

    is LambdaExpression ->
        if (e.returnType == null)
            mapOf(
                "proc" to mapOf(
                    "parameters" to e.parameters.map { x(it) },
                    "expression" to x(e.expression)
                )
            )
        else
            mapOf(
                "proc" to mapOf(
                    "parameters" to e.parameters.map { x(it) },
                    "return-type" to x(e.returnType),
                    "expression" to x(e.expression)
                )
            )

    is SignalExpression -> mapOf(
        "Signal" to mapOf(
            "expression" to x(e.expression)
        )
    )

    is TryExpression -> mapOf(
        "Try" to mapOf(
            "body" to x(e.body),
            "catch" to x(e.catch)
        )
    )

    is TypedExpression -> mapOf(
        "Typed" to mapOf(
            "expression" to x(e.expression),
            "type" to x(e.type)
        )
    )

    is LiteralInt -> singletonMap("LiteralInt", e.value)
    is LiteralString -> singletonMap("LiteralString", e.value)
    is LiteralUnit -> "LiteralUnit"
}

private fun x(ti: TypedIdentifier): Any =
    if (ti.type == null)
        x(ti.id)
    else
        singletonMap(
            "TypedIdentifier", mapOf(
                "identifier" to x(ti.id),
                "type" to x(ti.type)
            )
        )

private fun x(type: Type?): Any =
    type?.toString() ?: "null"

private fun x(op: BinaryOperator): Any = op.operator.name

private fun singletonMap(key: String, value: Any): Map<String, Any> =
    mapOf(key to value)