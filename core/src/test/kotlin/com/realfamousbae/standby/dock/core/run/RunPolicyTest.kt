package com.realfamousbae.standby.dock.core.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunPolicyTest {

    @Test
    fun `se reconocen el forzar detención y el administrador de tareas`() {
        // Forzar detención desde la info de la app.
        assertEquals(ExitKind.UserStopped, RunPolicy.classify(true, "stop com.example due to from pid 1234"))
        // "Detener" en la lista de apps activas (Android 13+).
        assertEquals(ExitKind.UserStopped, RunPolicy.classify(true, "fully stop com.example/0 by user request"))
        // Otra app limpiando memoria.
        assertEquals(ExitKind.UserStopped, RunPolicy.classify(true, "kill background"))
        // Un texto raro del firmware: también cuenta como parada pedida por Iván.
        assertEquals(ExitKind.UserStopped, RunPolicy.classify(true, null))
    }

    @Test
    fun `sacarla de Recientes y actualizarla no cuentan como parada`() {
        assertEquals(ExitKind.Other, RunPolicy.classify(true, "remove task"))
        assertEquals(ExitKind.Other, RunPolicy.classify(true, "stop com.example due to update pkg"))
        assertEquals(ExitKind.Other, RunPolicy.classify(true, "stop com.example due to replace pkg"))
    }

    @Test
    fun `otras causas de cierre no son una parada`() {
        assertEquals(ExitKind.Other, RunPolicy.classify(false, "stop com.example due to from pid 1234"))
        assertEquals(ExitKind.Other, RunPolicy.classify(false, null))
    }

    @Test
    fun `una parada nueva apaga el modo y queda contada`() {
        val d = RunPolicy.onProcessStart(LastExit(ExitKind.UserStopped, 1_000), lastHandledExitMs = 500)
        assertTrue(d.disable)
        assertEquals(1_000L, d.markHandledMs)
    }

    @Test
    fun `una parada ya contada no apaga de nuevo`() {
        val d = RunPolicy.onProcessStart(LastExit(ExitKind.UserStopped, 1_000), lastHandledExitMs = 1_000)
        assertFalse(d.disable)
        assertNull(d.markHandledMs)
    }

    @Test
    fun `que el proceso se muera solo no apaga nada`() {
        val d = RunPolicy.onProcessStart(LastExit(ExitKind.Other, 2_000), lastHandledExitMs = 0)
        assertFalse(d.disable)
        assertEquals(2_000L, d.markHandledMs)
        assertFalse(RunPolicy.onProcessStart(null, 0).disable)
    }
}
