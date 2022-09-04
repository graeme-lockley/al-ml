package io.littlelanguages.alml.typed.typing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private typealias S = Int
private typealias T = Int

class SchemeTests : StringSpec({
    "given S32 should generalist to <> S32" {
        generalise<S, T>(typeS32).shouldBe(Scheme(listOf(), typeS32))
    }

    "given S32 -> S32 should generalist to <> S32 -> S32" {
        generalise<S, T>(TArr(typeS32, typeS32)).shouldBe(Scheme(listOf(), TArr(typeS32, typeS32)))
    }

    "given '1 should generalist to <1> '1" {
        generalise<S, T>(TVar(1)).shouldBe(Scheme(listOf(1), TVar(1)))
    }

    "given S32 -> '1 should generalist to <1> S32 -> '1" {
        generalise<S, T>(TArr(typeS32, TVar(1))).shouldBe(Scheme(listOf(1), TArr(typeS32, TVar(1))))
    }

    "given '1 and {'1 -> S32} should generalist to <> S32" {
        generalise<S, T>(TVar(1), Substitution(mapOf(Pair(1, typeS32)))).shouldBe(Scheme(listOf(), typeS32))
    }

    "given '0 -> '4 and {'0: '3, '1: '3, '2: List '3 -> List '3, '4: List '3} should generalise to <3> '3 -> List '3" {
        generalise<S, T>(
            TArr(TVar(0), TVar(4)),
            Substitution(
                mapOf(
                    Pair(0, TVar(3)),
                    Pair(1, TVar(3)),
                    Pair(
                        2,
                        TArr(TCon("List", listOf(TVar(3))), TCon("List", listOf(TVar(3))))
                    ),
                    Pair(4, TCon("List", listOf(TVar(3))))
                )
            )
        ).shouldBe(Scheme(listOf(3), TArr(TVar(3), TCon("List", listOf(TVar(3))))))
    }

    "given <1> '1 -> '1 should instantiate to '0 -> '0" {
        Scheme<S, T>(listOf(1), TArr(TVar(1), TVar(1))).instantiate(VarPump())
            .shouldBe(TArr(TVar(0), TVar(0)))
    }
})