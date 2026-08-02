package com.typingfrontier

/**
 * Gerenciador de Tempo Deterministico.
 * O tempo só avança através de ações do jogador.
 * Não possui dependência com o relógio real do dispositivo.
 */
object TimeManager {

    /**
     * Avança o relógio interno baseado no custo da ação.
     */
    fun avancarTempo(horas: Int = 0, minutos: Int = 0) {
        val p = PlayerManager.player

        p.minuto += minutos
        p.hora += horas + (p.minuto / 60)
        p.minuto %= 60

        // O dia termina às 22:00 (Curfew)
        if (p.hora >= 22) {
            p.hora = 22
            p.minuto = 0
        }
    }

    /**
     * Verifica se o jogador ainda está dentro do horário comercial/útil.
     */
    fun podeAgir(): Boolean {
        val p = PlayerManager.player
        return p.hora < 22
    }

    /**
     * Reseta o relógio para o início do próximo dia.
     * Esta função é chamada exclusivamente pela GameEngine.processSleep().
     */
    fun resetarDia() {
        val p = PlayerManager.player
        p.dia++
        p.hora = 8
        p.minuto = 0
        p.trabalhouHoje = false
        p.pausouHoje = false
        
        // Nota: Custos financeiros e resets de status são processados 
        // pela GameEngine antes de chamar este reset de relógio.
    }

    /**
     * Formatação visual para o HUD.
     */
    fun tempoFormatado(): String {
        val p = PlayerManager.player
        return "Dia ${p.dia} - %02d:%02d".format(p.hora, p.minuto)
    }
}
