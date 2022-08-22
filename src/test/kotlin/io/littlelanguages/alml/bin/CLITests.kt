package io.littlelanguages.alml.bin

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.littlelanguages.alml.*
import io.littlelanguages.alml.static.TToken
import io.littlelanguages.alml.static.Token
import io.littlelanguages.scanpiler.LocationCoordinate

val L1 = LocationCoordinate(0, 10, 11)

class FormatErrorTests : StringSpec({
    "ArgumentMismatchError" {
        formatError(
            ArgumentMismatchError(
                "null?",
                1,
                2,
                L1
            )
        ) shouldBe "Argument Mismatch: (10, 11): Procedure \"null?\" expects 1 argument but was passed 2"

        formatError(
            ArgumentMismatchError(
                "pair",
                2,
                1,
                L1
            )
        ) shouldBe "Argument Mismatch: (10, 11): Procedure \"pair\" expects 2 arguments but was passed 1"
    }

    "CompilationError" {
        formatError(CompilationError("Some LLVM message")) shouldBe "Compilation Error: Some LLVM message"
    }

    "DuplicateNameError" {
        formatError(DuplicateNameError("a", L1)) shouldBe "Duplicate Name: (10, 11): Attempt to redefine \"a\""
    }

    "DuplicateParameterNameError" {
        formatError(
            DuplicateParameterNameError("a", L1)
        ) shouldBe "Duplicate Parameter Name: (10, 11): Attempt to redefine \"a\""
    }

    "ParseError" {
        formatError(
            ParseError(
                Token(TToken.TLet, L1, ""),
                setOf(TToken.TLCurly, TToken.TIf)
            )
        ) shouldBe "Parse Error: (10, 11): Expected one of '{', 'if' but found 'let'"

        formatError(
            ParseError(
                Token(TToken.TSymbol, L1, "fred"),
                setOf(TToken.TLiteralInt, TToken.TLiteralString)
            )
        ) shouldBe "Parse Error: (10, 11): Expected one of Literal Int, Literal String but found Symbol (fred)"

        formatError(
            ParseError(
                Token(TToken.TSymbol, L1, "fred"),
                setOf(TToken.TLiteralInt)
            )
        ) shouldBe "Parse Error: (10, 11): Expected Literal Int but found Symbol (fred)"
    }

    "UnknownSymbolError" {
        formatError(
            UnknownSymbolError("a", L1)
        ) shouldBe "Unknown Symbol: (10, 11): Reference to unknown symbol \"a\""
    }
})