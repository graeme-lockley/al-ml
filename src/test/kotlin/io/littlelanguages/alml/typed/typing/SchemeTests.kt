package io.littlelanguages.alml.typed.typing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

val environment = initialEnvironment()

class SchemeTests : StringSpec({
    "given S32 should generalist to <> S32" {
        environment.generalise(typeS32).shouldBe(Scheme(setOf(), typeS32))
    }

    "given S32 -> S32 should generalist to <> S32 -> S32" {
        environment.generalise(TArr(typeS32, typeS32)).shouldBe(Scheme(setOf(), TArr(typeS32, typeS32)))
    }

    "given '1 should generalist to <1> '1" {
        environment.generalise(TVar(1)).shouldBe(Scheme(setOf(1), TVar(1)))
    }

    "given S32 -> '1 should generalist to <1> S32 -> '1" {
        environment.generalise(TArr(typeS32, TVar(1))).shouldBe(Scheme(setOf(1), TArr(typeS32, TVar(1))))
    }

    "given <1> '1 -> '1 should instantiate to '0 -> '0" {
        Scheme(setOf(1), TArr(TVar(1), TVar(1))).instantiate(VarPump())
            .shouldBe(TArr(TVar(0), TVar(0)))
    }
})