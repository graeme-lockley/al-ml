package io.littlelanguages.alml.dynamic

import io.littlelanguages.alml.*
import io.littlelanguages.alml.dynamic.tst.*
import io.littlelanguages.data.Tuple2
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.LocationCoordinate
import io.littlelanguages.scanpiler.LocationRange
import java.lang.Integer.max

fun <S, T> translate(builtinBindings: List<Binding<S, T>>, p: io.littlelanguages.alml.typed.st.Program, errors: Errors): Program<S, T> =
    Translator(builtinBindings, p, errors).apply()

private class Translator<S, T>(builtinBindings: List<Binding<S, T>>, val ast: io.littlelanguages.alml.typed.st.Program, private val errors: Errors) {
    var nameGenerator = 0

    val bindings = Bindings<S, T>()

    var depth = -1
    var offset = 0

    init {
        builtinBindings.forEach { bindings.add(it.name, it) }
    }

    fun apply(): Program<S, T> = program(ast.expressions)

    private fun program(es: List<io.littlelanguages.alml.typed.st.Expression>): Program<S, T> {
        val declarations = mutableListOf<Procedure<S, T>>()
        val expressions = mutableListOf<Expression<S, T>>()
        val names = mutableListOf<String>()

        es.flatMap { expressionToTST(it, true) }.forEach {
            if (it is Procedure<S, T>) declarations.add(it)
            else expressions.add(it)

            if (it is AssignExpression<S, T>) names.add(it.symbol.name)
        }

        declarations.add(Procedure("_main", emptyList(), 0, offset, expressions))

        return Program(names, declarations)
    }

    private fun expressionToTST(e: io.littlelanguages.alml.typed.st.Expression, toplevelValue: Boolean = false): List<Expression<S, T>> = when (e) {
        is io.littlelanguages.alml.typed.st.BinaryOpExpression -> listOf(
            BinaryOpExpression(
                expressionToTST(e.left),
                e.op,
                expressionToTST(e.right),
                e.op.position().line()
            )
        )

        is io.littlelanguages.alml.typed.st.BlockExpression -> expressionsToTST(e.expressions)

        is io.littlelanguages.alml.typed.st.LetFunction -> listOf(procedureToTST(e.identifier.name, e.parameters.map { it.id }, e.expression).first)

        is io.littlelanguages.alml.typed.st.LetValue -> {
            val name = e.identifier.name
            val type = e.type
            val expression = e.expression

            if (bindings.inCurrentNesting(name)) reportError(DuplicateNameError(name, e.identifier.position))
            else {
                val binding = when {
                    toplevelValue && isToplevel() -> TopLevelValueBinding<S, T>(name, type)
                    else -> ProcedureValueBinding(name, max(depth, 0), offset++)
                }

                bindings.add(name, binding)
                val rhs = expressionToTST(expression)

                listOf(AssignExpression(binding, rhs))
            }
        }

        is io.littlelanguages.alml.typed.st.IfExpression -> ifToTST(
            e.ifThenExpressions.map { Tuple2(expressionToTST(it.a), expressionToTST(it.b)) },
            if (e.elseExpression == null) null else expressionToTST(e.elseExpression)
        )

        is io.littlelanguages.alml.typed.st.LambdaExpression -> {
            val tst = procedureToTST(nextName(), e.parameters.map { it.id }, e.expression)

            listOf(tst.first, IdentifierExpression(tst.second, e.position.line()))
        }

        is io.littlelanguages.alml.typed.st.ApplyExpression -> {
            val first = e.expressions[0]

            if (first is io.littlelanguages.alml.typed.st.Identifier) {
                val arguments = e.expressions.drop(1).map { expressionToTST(it) }

                when (val binding = bindings.get(first.name)) {
                    null -> reportError(UnknownSymbolError(first.name, first.position))

                    is DeclaredProcedureBinding -> if (binding.parameterCount == arguments.size) listOf(
                        CallProcedureExpression(
                            binding,
                            arguments,
                            e.position.line()
                        )
                    )
                    else reportError(ArgumentMismatchError(first.name, binding.parameterCount, arguments.size, e.position))

                    is ExternalProcedureBinding -> when (val error = binding.validateArguments(e, first.name, arguments)) {
                        null -> listOf(CallProcedureExpression(binding, arguments, e.position.line()))

                        else -> reportError(error)
                    }

                    else -> listOf(
                        CallValueExpression(
                            listOf(IdentifierExpression(binding, e.position.line())), arguments.flatten(), e.position.line()
                        )
                    )
                }
            } else listOf(CallValueExpression(expressionToTST(e.expressions[0]), expressionsToTST(e.expressions.drop(1)), e.position.line()))
        }

        is io.littlelanguages.alml.typed.st.LiteralInt -> listOf(LiteralS32(e.value.toInt()))

        is io.littlelanguages.alml.typed.st.LiteralString -> listOf(translateLiteralString(e))

        is io.littlelanguages.alml.typed.st.LiteralUnit -> listOf(LiteralUnit())

        is io.littlelanguages.alml.typed.st.SignalExpression -> listOf(SignalExpression(expressionToTST(e.expression), e.position.line()))

        is io.littlelanguages.alml.typed.st.TryExpression -> {
            val body = procedureToTST(nextName(), emptyList(), e.body)
            val catch = expressionToTST(e.catch)

            if (!isProcedure(catch)) reportError(ExpressionNotProcedureError(e.catch.position()))
            else listOf(body.first) + catch.dropLast(1) + listOf(
                TryExpression(
                    IdentifierExpression(body.second, e.position.line()), catch.last() as IdentifierExpression<S, T>, e.position.line()
                )
            )
        }

        is io.littlelanguages.alml.typed.st.Identifier -> {
            val binding = bindings.get(e.name)

            if (binding == null) reportError(UnknownSymbolError(e.name, e.position))
            else listOf(IdentifierExpression(binding, e.position.line()))
        }

        is io.littlelanguages.alml.typed.st.TypedExpression -> expressionToTST(e.expression)
    }

    private fun expressionsToTST(es: List<io.littlelanguages.alml.typed.st.Expression>): List<Expression<S, T>> {
        bindings.open()
        val result = es.flatMap { expressionToTST(it) }
        bindings.close()

        return result
    }

    private fun procedureToTST(
        name: String, parameters: List<io.littlelanguages.alml.typed.st.Identifier>, expression: io.littlelanguages.alml.typed.st.Expression
    ): Pair<Procedure<S, T>, DeclaredProcedureBinding<S, T>> {
        val parameterNames = mutableListOf<String>()

        val oldOffset = offset

        depth += 1
        val binding = DeclaredProcedureBinding<S, T>(name, parameters.size, depth)
        bindings.add(name, binding)
        offset = parameters.size
        bindings.open()
        parameters.forEachIndexed { index, symbol ->
            val parameterName = symbol.name

            parameterNames.add(parameterName)
            if (bindings.inCurrentNesting(parameterName)) reportError(DuplicateParameterNameError(parameterName, symbol.position()))
            bindings.add(parameterName, ParameterBinding(parameterName, depth, index))
        }
        bindings.open()
        val es = expressionToTST(expression)
        val procedure = Procedure(name, parameterNames, depth, offset, es)
        bindings.close()
        bindings.close()

        depth -= 1
        offset = oldOffset

        return Pair(procedure, binding)
    }

    private fun ifToTST(
        ifThenExpressions: List<Tuple2<Expressions<S, T>, Expressions<S, T>>>, elseExpression: Expressions<S, T>?
    ): List<Expression<S, T>> = if (ifThenExpressions.isEmpty()) elseExpression ?: listOf(LiteralUnit())
    else {
        val ifThen = ifThenExpressions[0]
        val remainder = ifThenExpressions.drop(1)

        listOf(IfExpression(ifThen.a, ifThen.b, if (remainder.isEmpty() && elseExpression == null) null else ifToTST(remainder, elseExpression)))
    }

    private fun reportError(error: Error): List<Expression<S, T>> {
        errors.report(error)
        return listOf()
    }

    private fun isToplevel(): Boolean = depth == -1

    private fun nextName() = "__n${nameGenerator++}"
}

fun <S, T> translateLiteralString(e: io.littlelanguages.alml.typed.st.LiteralString): LiteralString<S, T> {
    val sb = StringBuilder()
    val eValue = e.value
    val eLength = eValue.length
    var lp = 1

    while (true) {
        when {
            lp >= eLength || eValue[lp] == '"' -> return LiteralString(sb.toString())

            eValue[lp] == '\\' -> {
                when (val c = eValue[lp + 1]) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'x' -> {
                        lp += 2
                        var elp = lp
                        while (elp < eLength && eValue[elp].isHexDigit()) {
                            elp += 1
                        }
                        sb.append(eValue.subSequence(lp, elp).toString().toInt(16).toChar())
                        lp = elp - 2
                    }

                    else -> sb.append(c)
                }
                lp += 2
            }

            else -> {
                sb.append(eValue[lp])
                lp += 1
            }
        }
    }
}

private fun Char.isHexDigit(): Boolean = this.isDigit() || this.uppercaseChar() in 'A'..'F'

private fun Location.line(): Int = when (this) {
    is LocationCoordinate -> this.line
    is LocationRange -> this.start.line
}

private fun <S, T> isProcedure(es: List<Expression<S, T>>): Boolean = if (es.isEmpty()) false
else {
    val e = es.last()

    e is IdentifierExpression && (e.symbol is ProcedureBinding || e.symbol is ProcedureValueBinding)
}
