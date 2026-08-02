package com.typingfrontier

/**
 * Representa o resultado de qualquer processamento da Engine.
 * Success contém mensagens de feedback positivo.
 * Failure contém o motivo pelo qual a ação foi bloqueada (Requirement Gate).
 */
sealed class EngineResult {
    data class Success(val message: String, val extra: String? = null) : EngineResult()
    data class Failure(val message: String) : EngineResult()
}
