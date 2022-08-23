package io.littlelanguages.alml.static

import io.littlelanguages.alml.Errors
import io.littlelanguages.alml.ParseError
import io.littlelanguages.alml.static.ast.*
import io.littlelanguages.data.*
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.Locationable

fun parse(scanner: Scanner): Either<Errors, Program> = try {
    Right(Parser(scanner, ParseVisitor()).program())
} catch (e: ParsingException) {
    Left(ParseError(e.found, e.expected))
}

class ParseVisitor :
    Visitor<Program, List<Expression>, Expression, Expression, Expression, Expression, Expression, BinaryOperator, Expression, Expression, Expression> {
    override fun visitProgram(a: List<Expression>): Program = Program(a)

    override fun visitExpressions(a1: Expression, a2: List<Tuple2<Token, Expression>>): List<Expression> =
        listOf(a1) + a2.map { it.b }

    override fun visitExpression1(a1: Token, a2: Token, a3: List<Token>, a4: Token, a5: Expression): Expression =
        if (a3.isEmpty())
            ConstValue(a1.location + a5.position(), Symbol(a2.location, a2.lexeme), a5)
        else
            ConstProcedure(a1.location + a5.position(), Symbol(a2.location, a2.lexeme), a3.map { Symbol(it.location, it.lexeme) }, a5)

    override fun visitExpression2(a: Expression): Expression =
        a

    override fun visitIfExpression1(
        a1: Token,
        a2: Expression,
        a3: Token,
        a4: Expression,
        a5: List<Tuple4<Token, Expression, Token, Expression>>,
        a6: Tuple2<Token, Expression>?
    ): Expression {
        val location =
            a1.location + (a6?.b?.position() ?: if (a5.isEmpty()) a4.position() else a5.last().d.position())

        return IfExpression(location, listOf(Tuple2(a2, a4)) + a5.map { Tuple2(it.b, it.d) }, a6?.b)
    }

    override fun visitIfExpression2(a: Expression): Expression =
        a

    override fun visitLambdaExpression1(a1: Token, a2: Token, a3: List<Token>, a4: Token, a5: Expression): Expression =
        ProcExpression(a1.location + a5.position(), listOf(Symbol(a2.location, a2.lexeme)) + a3.map { Symbol(it.location, it.lexeme) }, a5)

    override fun visitLambdaExpression2(a: Expression): Expression =
        a

    override fun visitRelationalExpression(a1: Expression, a2: Tuple2<BinaryOperator, Expression>?): Expression =
        if (a2 == null) a1 else BinaryOpExpression(a1.position() + a2.b.position(), a1, a2.a, a2.b)

    override fun visitRelationalOp1(a: Token): BinaryOperator =
        Equals(a.location)

    override fun visitRelationalOp2(a: Token): BinaryOperator =
        NotEquals(a.location)

    override fun visitRelationalOp3(a: Token): BinaryOperator =
        LessThan(a.location)

    override fun visitRelationalOp4(a: Token): BinaryOperator =
        LessEquals(a.location)

    override fun visitRelationalOp5(a: Token): BinaryOperator =
        GreaterThan(a.location)

    override fun visitRelationalOp6(a: Token): BinaryOperator =
        GreaterEquals(a.location)

    override fun visitMultiplicativeExpression(a1: Expression, a2: List<Tuple2<Union2<Token, Token>, Expression>>): Expression =
        a2.fold(
            a1
        ) { acc, opExpr ->
            BinaryOpExpression(
                acc.position() + opExpr.b.position(),
                acc,
                if (opExpr.a.isA()) Multiply(opExpr.a.a().location) else Divide(opExpr.a.b().location),
                opExpr.b
            )
        }

    override fun visitAdditiveExpression(a1: Expression, a2: List<Tuple2<Union2<Token, Token>, Expression>>): Expression =
        a2.fold(
            a1
        ) { acc, opExpr ->
            BinaryOpExpression(
                acc.position() + opExpr.b.position(),
                acc,
                if (opExpr.a.isA()) Plus(opExpr.a.a().location) else Minus(opExpr.a.b().location),
                opExpr.b
            )
        }

    override fun visitCallExpression(a1: Expression, a2: List<Expression>): Expression =
        if (a2.isEmpty()) a1 else SExpression(locationOf(a1.position, a2), listOf(a1) + a2)

    override fun visitTerm1(a1: Token, a2: Expression?, a3: Token): Expression =
        a2 ?: LiteralUnit(a1.location + a3.location)

    override fun visitTerm2(a: Token): Expression =
        LiteralInt(a.location, a.lexeme)

    override fun visitTerm3(a: Token): Expression =
        LiteralString(a.location, a.lexeme)

    override fun visitTerm4(a: Token): Expression =
        Symbol(a.location, a.lexeme)

    override fun visitTerm5(a1: Token, a2: Expression, a3: Expression): Expression =
        TryExpression(a1.location + a3.position(), a2, a3)

    override fun visitTerm6(a1: Token, a2: Expression): Expression =
        SignalExpression(a1.location + a2.position(), a2)

    override fun visitTerm7(a1: Token, a2: Expression, a3: List<Tuple2<Token, Expression>>, a4: Token): Expression =
        if (a3.isEmpty()) a2 else BlockExpression(a1.location + a4.location, listOf(a2) + a3.map { it.b })

//    override fun visitExpressionBody(a1: Expression, a2: List<Expression>): Expression {
//        val es = listOf(a1) + a2
//
//        return SExpression(locationOf(es[0].position, es.drop(1)), es)
//    }
}


private fun locationOf(loc: Location, ls: List<Locationable>): Location = ls.fold(loc) { l1, l2 -> l1 + l2.position() }
