package com.typingfrontier

import org.junit.Test
import org.junit.Assert.*

class XpFormulaTest {

    @Test
    fun testXpFormulaValues() {
        // Nível 1 -> 20 + 15 + (1^1.5 * 8) = 35 + 8 = 43
        assertEquals(43, PlayerManager.calcularXpParaProximoNivel(1))
        
        // Nível 2 -> 20 + 30 + (2^1.5 * 8) = 50 + (2.82 * 8) = 50 + 22.56 = 72
        assertEquals(72, PlayerManager.calcularXpParaProximoNivel(2))
        
        // Nível 10 -> 20 + 150 + (10^1.5 * 8) = 170 + (31.62 * 8) = 170 + 252.96 = 422
        assertEquals(422, PlayerManager.calcularXpParaProximoNivel(10))
        
        // Nível 50 -> 20 + 750 + (50^1.5 * 8) = 770 + (353.55 * 8) = 770 + 2828.4 = 3598
        assertEquals(3598, PlayerManager.calcularXpParaProximoNivel(50))
        
        // Nível 100 -> 20 + 1500 + (100^1.5 * 8) = 1520 + (1000 * 8) = 9520
        assertEquals(9520, PlayerManager.calcularXpParaProximoNivel(100))
    }

    @Test
    fun testXpIsAlwaysIncreasing() {
        var lastXp = 0
        for (i in 1..1000) {
            val currentXp = PlayerManager.calcularXpParaProximoNivel(i)
            assertTrue("XP no nível $i ($currentXp) deve ser maior que no nível ${i-1} ($lastXp)", currentXp > lastXp)
            lastXp = currentXp
        }
    }

    @Test
    fun testNoOverflowAtHighLevel() {
        // Nível 1000 -> 20 + 15000 + (1000^1.5 * 8) = 15020 + (31622.7 * 8) = 15020 + 252981 = 268001
        val xp1000 = PlayerManager.calcularXpParaProximoNivel(1000)
        assertTrue(xp1000 > 0)
        assertTrue(xp1000 < Int.MAX_VALUE)
    }
}
