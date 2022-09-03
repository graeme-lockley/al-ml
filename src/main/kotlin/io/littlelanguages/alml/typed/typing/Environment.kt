package io.littlelanguages.alml.typed.typing

import io.littlelanguages.alml.dynamic.Binding

data class Environment<S, T>(
    private val valueBindings: Map<String, Binding<S, T>> = emptyMap(),
    private val typeBindings: Map<String, Type> = emptyMap()
) {
    fun value(name: String): Binding<S, T>? =
        valueBindings[name]

    fun type(name: String): Type? =
        typeBindings[name]

    fun types() = typeBindings.entries.toList()

    fun removeValue(name: String): Environment<S, T> =
        Environment(valueBindings - name, typeBindings)


//    fun variable(name: String): Scheme? {
//        val valueBinding =
//            value(name)
//
//        return when (valueBinding) {
//            is VariableBinding ->
//                valueBinding.scheme
//
//            is OperatorBinding ->
//                valueBinding.scheme
//
//            is ImportVariableBinding ->
//                valueBinding.scheme
//
//            else ->
//                null
//        }
//    }

    fun newValue(name: String, value: Binding<S, T>): Environment<S, T> =
        Environment(valueBindings + Pair(name, value), typeBindings)

    operator fun plus(binding: Pair<String, Type>): Environment<S, T> =
        Environment(valueBindings, typeBindings + binding)

    fun containsValue(name: String): Boolean =
        valueBindings.contains(name)
}
