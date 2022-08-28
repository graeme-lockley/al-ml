package io.littlelanguages.alml.dynamic.typing

import io.littlelanguages.alml.dynamic.Binding

data class Environment<S, T>(private val valueBindings: Map<String, Binding<S, T>>) {
    fun value(name: String): Binding<S, T>? =
        valueBindings[name]


    fun removeValue(name: String): Environment<S, T> =
        Environment(valueBindings - name)


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
        Environment(valueBindings + Pair(name, value))

    fun containsValue(name: String): Boolean =
        valueBindings.contains(name)
}
