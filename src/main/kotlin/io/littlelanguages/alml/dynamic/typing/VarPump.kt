package io.littlelanguages.alml.dynamic.typing

import io.littlelanguages.scanpiler.Location

class VarPump {
    private var counter =
        0

    fun fresh(location: Location? = null): TVar {
        val result =
            counter

        counter += 1

        return TVar(location, result)
    }
}