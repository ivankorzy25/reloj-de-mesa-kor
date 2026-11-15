package com.realfamousbae.standby.dock.core.trigger

import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Los casos de docs/PLAN.md, punto 2.3, y de la lista de aceptación. El tiempo corre a mano. */
class TriggerEngineTest {

    private val onSide = Gravity(9.6f, 0f, 2f) // en el soporte, ~12° de la vertical
    private val flat = Gravity(0f, 0f, 9.8f) // acostado boca arriba
    private val portrait = Gravity(0f, 9.8f, 0f) // en la mano

    /** Un banco de pruebas chiquito: guarda estado, tiempo y los efectos que salen. */
    private class Rig(var settings: StandbySettings = StandbySettings(requireCharging = false)) {
        var state = TriggerState()
        var now = 0L
        val effects = mutableListOf<TriggerEffect>()

        fun send(event: TriggerEvent) {
            val r = TriggerEngine.reduce(state, event, now, settings)
            state = r.state
            effects += r.effects
        }

        /** Adelantar el tiempo pasando lecturas del sensor ~10 veces por segundo. */
        fun hold(g: Gravity, ms: Long) {
            val end = now + ms
            while (now < end) {
                now += 100
                send(TriggerEvent.GravityReading(g))
            }
        }

        fun advance(ms: Long) {
            now += ms
            send(TriggerEvent.Tick)
        }

        val shows get() = effects.count { it == TriggerEffect.Show }
        val hides get() = effects.filterIsInstance<TriggerEffect.Hide>()
        val phase get() = state.phase
    }

    private fun rigOnBattery(percent: Int = 80) = Rig().apply {
        send(TriggerEvent.Power(charging = false, batteryPercent = percent))
    }

    private fun rigCharging() = Rig().apply {
        send(TriggerEvent.Power(charging = true, batteryPercent = 50))
    }

    @Test
    fun `sin cargador — lo ponen de costado y apagan la pantalla, el modo a 1_5 s`() {
        val rig = rigOnBattery()
        rig.send(TriggerEvent.ScreenOff)
        assertEquals(Phase.Armed, rig.phase)
        assertTrue(TriggerEffect.SetSensorActive(true) in rig.effects)
        assertTrue(TriggerEffect.HoldWakeLock(true) in rig.effects)

        rig.hold(onSide, 1_000)
        assertEquals(0, rig.shows)
        rig.hold(onSide, 600)
        assertEquals(1, rig.shows)
        assertEquals(Phase.Showing, rig.phase)
        // La pantalla está prendida: el wake lock ya no hace falta; el sensor sí, para ver si lo levantan.
        assertTrue(TriggerEffect.HoldWakeLock(false) in rig.effects)
        assertTrue(rig.state.sensorActive)
    }

    @Test
    fun `la pantalla la prendió el propio modo — no se cancela la muestra`() {
        val rig = rigOnBattery()
        rig.send(TriggerEvent.ScreenOff)
        rig.hold(onSide, 1_600)
        rig.send(TriggerEvent.ScreenOn)
        assertEquals(Phase.Showing, rig.phase)
    }

    @Test
    fun `acostado boca arriba — el modo no arranca y la ventana de prueba se cierra`() {
        val rig = rigOnBattery()
        rig.send(TriggerEvent.ScreenOff)
        rig.hold(flat, TriggerEngine.PROBE_WINDOW_MS + 500)
        assertEquals(0, rig.shows)
        assertEquals(Phase.Idle, rig.phase)
        assertFalse(rig.state.sensorActive)
        assertFalse(rig.state.wakeLockHeld)
    }

    @Test
    fun `la ventana de prueba se cierra por tiempo aunque el sensor no diga nada`() {
        val rig = rigOnBattery()
        rig.send(TriggerEvent.ScreenOff)
        val tick = rig.effects.filterIsInstance<TriggerEffect.ScheduleTick>().single()
        assertEquals(TriggerEngine.PROBE_WINDOW_MS, tick.atMs)
        rig.advance(TriggerEngine.PROBE_WINDOW_MS)
        assertEquals(Phase.Idle, rig.phase)
    }

    @Test
    fun `cargando esperamos sin límite de tiempo`() {
        val rig = rigCharging()
        rig.send(TriggerEvent.ScreenOff)
        assertEquals(null, rig.state.armedUntilMs)
        rig.hold(flat, 60_000)
        assertEquals(Phase.Armed, rig.phase)
        rig.hold(onSide, 1_600)
        assertEquals(1, rig.shows)
    }

    @Test
    fun `enchufan el cargador con la pantalla apagada y en el soporte — aparece el modo`() {
        val rig = rigOnBattery()
        rig.send(TriggerEvent.ScreenOff)
        rig.advance(TriggerEngine.PROBE_WINDOW_MS) // se venció la ventana
        assertEquals(Phase.Idle, rig.phase)
        rig.send(TriggerEvent.Power(charging = true, batteryPercent = 80))
        assertEquals(Phase.Armed, rig.phase)
        rig.hold(onSide, 1_600)
        assertEquals(1, rig.shows)
    }

    @Test
    fun `el sensor de movimiento despierta al teléfono apagado y abre otra prueba`() {
        val rig = rigOnBattery()
        rig.send(TriggerEvent.ScreenOff)
        rig.advance(TriggerEngine.PROBE_WINDOW_MS)
        rig.send(TriggerEvent.MotionWake)
        assertEquals(Phase.Armed, rig.phase)
        rig.hold(onSide, 1_600)
        assertEquals(1, rig.shows)
    }

    @Test
    fun `con la pantalla prendida, ni el movimiento ni la posición arrancan el modo`() {
        val rig = rigOnBattery()
        rig.send(TriggerEvent.ScreenOn)
        rig.send(TriggerEvent.MotionWake)
        rig.hold(onSide, 5_000)
        assertEquals(Phase.Idle, rig.phase)
        assertEquals(0, rig.shows)
    }

    @Test
    fun `si Iván prende la pantalla durante la prueba, la espera se corta`() {
        val rig = rigOnBattery()
        rig.send(TriggerEvent.ScreenOff)
        rig.hold(onSide, 800)
        rig.send(TriggerEvent.ScreenOn)
        rig.hold(onSide, 2_000)
        assertEquals(0, rig.shows)
        assertEquals(Phase.Idle, rig.phase)
    }

    @Test
    fun `lo levantan — el modo se cierra a los 2 s`() {
        val rig = rigOnBattery()
        rig.send(TriggerEvent.ScreenOff)
        rig.hold(onSide, 1_600)
        rig.hold(portrait, 1_500)
        assertTrue(rig.hides.isEmpty())
        rig.hold(portrait, 600)
        assertEquals("sacado del soporte", rig.hides.single().reason)
        assertEquals(Phase.Idle, rig.phase)
        assertFalse(rig.state.suppressedUntilScreenOn)
    }

    @Test
    fun `un bamboleo dentro de la histéresis no cierra el modo`() {
        val rig = rigOnBattery()
        rig.send(TriggerEvent.ScreenOff)
        rig.hold(onSide, 1_600)
        // ~50°: pasa el umbral de 45°, pero no llega a 45° + 10°.
        rig.hold(Gravity(6.3f, 0f, 7.5f), 5_000)
        assertEquals(Phase.Showing, rig.phase)
    }

    @Test
    fun `batería por debajo del mínimo sin cargador — no arranca y se cierra`() {
        val low = rigOnBattery(percent = 15)
        low.send(TriggerEvent.ScreenOff)
        low.hold(onSide, 3_000)
        assertEquals(0, low.shows)

        val rig = rigOnBattery(percent = 21)
        rig.send(TriggerEvent.ScreenOff)
        rig.hold(onSide, 1_600)
        rig.send(TriggerEvent.Power(charging = false, batteryPercent = 19))
        assertEquals(1, rig.hides.size)
    }

    @Test
    fun `cargando, el mínimo de batería no corre`() {
        val rig = Rig()
        rig.send(TriggerEvent.Power(charging = true, batteryPercent = 3))
        rig.send(TriggerEvent.ScreenOff)
        rig.hold(onSide, 1_600)
        assertEquals(1, rig.shows)
    }

    @Test
    fun `solo cargando — sin cargador no arranca y, si lo desenchufan, se cierra`() {
        val rig = Rig(StandbySettings(requireCharging = true))
        rig.send(TriggerEvent.Power(charging = false, batteryPercent = 90))
        rig.send(TriggerEvent.ScreenOff)
        rig.hold(onSide, 3_000)
        assertEquals(0, rig.shows)

        rig.send(TriggerEvent.Power(charging = true, batteryPercent = 90))
        rig.hold(onSide, 1_600)
        assertEquals(1, rig.shows)
        rig.send(TriggerEvent.Power(charging = false, batteryPercent = 90))
        assertEquals(1, rig.hides.size)
    }

    /** Mostrar el modo en el soporte (la pantalla la prende el propio modo). */
    private fun Rig.showOnStand() {
        send(TriggerEvent.ScreenOff)
        hold(onSide, 1_600)
        send(TriggerEvent.ScreenOn)
        assertEquals(Phase.Showing, phase)
    }

    @Test
    fun `salen con un toque y lo dejan — cuando la pantalla se apaga, el modo vuelve`() {
        val rig = rigOnBattery()
        rig.showOnStand()
        rig.send(TriggerEvent.UserExit(screenOn = true))
        assertEquals(Phase.Idle, rig.phase)
        assertFalse(rig.state.suppressedUntilScreenOn)
        assertTrue("UserExit no debe mandar Hide de vuelta", rig.hides.isEmpty())
        // Mientras la pantalla está prendida, el modo no aparece solo.
        rig.hold(onSide, 5_000)
        assertEquals(1, rig.shows)

        rig.send(TriggerEvent.ScreenOff) // la pantalla de bloqueo se apagó por tiempo
        rig.hold(onSide, 1_600)
        assertEquals(2, rig.shows)
    }

    @Test
    fun `botón de encendido con el modo en pantalla — mientras esté apagada, no esperamos nada`() {
        val rig = rigCharging()
        rig.showOnStand()
        rig.effects.clear()
        rig.send(TriggerEvent.ScreenOff)
        assertEquals("apagado con el botón de encendido", rig.hides.single().reason)
        assertTrue(rig.state.suppressedUntilScreenOn)
        // Ni sensor ni wake lock toda la noche.
        assertFalse(rig.state.sensorActive)
        assertFalse(rig.state.wakeLockHeld)
        assertFalse(TriggerEffect.HoldWakeLock(true) in rig.effects)
        rig.send(TriggerEvent.MotionWake)
        assertEquals(Phase.Idle, rig.phase)
        rig.hold(onSide, 5_000)
        assertEquals(0, rig.shows) // los efectos se limpiaron después de la primera muestra
    }

    @Test
    fun `botón de encendido, después prenden la pantalla y no la tocan — el modo vuelve`() {
        val rig = rigOnBattery()
        rig.showOnStand()
        rig.send(TriggerEvent.ScreenOff) // botón de encendido
        rig.send(TriggerEvent.ScreenOn) // prendieron la pantalla, pero no desbloquearon
        assertFalse(rig.state.suppressedUntilScreenOn)
        rig.hold(onSide, 5_000)
        assertEquals(1, rig.shows)
        rig.send(TriggerEvent.ScreenOff) // se apagó por tiempo
        rig.hold(onSide, 1_600)
        assertEquals(2, rig.shows)
    }

    @Test
    fun `la Activity se fue antes del SCREEN_OFF — cuenta como botón de encendido`() {
        fun finalState(exitFirst: Boolean): TriggerState {
            val rig = rigCharging()
            rig.showOnStand()
            if (exitFirst) {
                rig.send(TriggerEvent.UserExit(screenOn = false))
                rig.send(TriggerEvent.ScreenOff)
            } else {
                rig.send(TriggerEvent.ScreenOff)
                rig.send(TriggerEvent.UserExit(screenOn = false))
            }
            rig.hold(onSide, 5_000)
            assertEquals(1, rig.shows)
            return rig.state
        }
        val a = finalState(exitFirst = true)
        val b = finalState(exitFirst = false)
        assertEquals(a, b)
        assertTrue(a.suppressedUntilScreenOn)
        assertEquals(Phase.Idle, a.phase)
    }

    @Test
    fun `desbloquear con el modo en pantalla lo cierra`() {
        val rig = rigOnBattery()
        rig.send(TriggerEvent.ScreenOff)
        rig.hold(onSide, 1_600)
        rig.send(TriggerEvent.UserPresent)
        assertEquals("teléfono desbloqueado", rig.hides.single().reason)
        assertFalse(rig.state.suppressedUntilScreenOn)
        assertFalse(rig.state.sensorActive)
    }

    @Test
    fun `botón de encendido, después desbloquean y vuelven a bloquear — la espera se arma`() {
        val rig = rigOnBattery()
        rig.showOnStand()
        rig.send(TriggerEvent.ScreenOff)
        rig.send(TriggerEvent.UserPresent)
        assertFalse(rig.state.suppressedUntilScreenOn)
        rig.send(TriggerEvent.ScreenOff)
        assertEquals(Phase.Armed, rig.phase)
    }

    @Test
    fun `enchufar el cargador después del botón de encendido saca el veto y arma la espera`() {
        val rig = rigOnBattery()
        rig.showOnStand()
        rig.send(TriggerEvent.ScreenOff)
        assertTrue(rig.state.suppressedUntilScreenOn)
        rig.send(TriggerEvent.Power(charging = true, batteryPercent = 50))
        assertFalse(rig.state.suppressedUntilScreenOn)
        assertEquals(Phase.Armed, rig.phase)
        rig.hold(onSide, 1_600)
        assertEquals(2, rig.shows)
    }

    @Test
    fun `un video apaisado cargando — la pantalla está prendida y el modo no aparece`() {
        val rig = rigCharging()
        rig.send(TriggerEvent.ScreenOn)
        rig.send(TriggerEvent.UserPresent)
        rig.hold(onSide, 30_000)
        assertEquals(0, rig.shows)
        assertFalse(rig.state.sensorActive)
    }

    @Test
    fun `la muestra a mano ignora la posición y la cierra solo Iván`() {
        val rig = rigOnBattery()
        rig.send(TriggerEvent.ManualShow)
        assertEquals(1, rig.shows)
        assertFalse(rig.state.sensorActive)
        rig.hold(portrait, 5_000)
        assertEquals(Phase.Showing, rig.phase)
        rig.send(TriggerEvent.UserExit())
        assertEquals(Phase.Idle, rig.phase)
    }

    @Test
    fun `los eventos de energía repetidos no generan efectos de más`() {
        val rig = rigOnBattery()
        rig.effects.clear()
        repeat(5) { rig.send(TriggerEvent.Power(charging = false, batteryPercent = 80)) }
        assertTrue(rig.effects.isEmpty())
    }
}
