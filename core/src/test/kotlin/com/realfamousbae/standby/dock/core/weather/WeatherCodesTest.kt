package com.realfamousbae.standby.dock.core.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodesTest {

    @Test
    fun `los códigos conocidos se traducen`() {
        assertEquals("Despejado", WeatherCodes.description(0))
        assertEquals("Lluvia fuerte", WeatherCodes.description(65))
        assertEquals("Tormenta con granizo", WeatherCodes.description(99))
    }

    @Test
    fun `un código raro no rompe nada`() {
        assertEquals("Sin datos", WeatherCodes.description(1234))
        assertEquals(WeatherIcon.Nubes, WeatherCodes.icon(1234, isDay = true))
    }

    @Test
    fun `de noche el sol es luna`() {
        assertEquals(WeatherIcon.Sol, WeatherCodes.icon(0, isDay = true))
        assertEquals(WeatherIcon.Luna, WeatherCodes.icon(0, isDay = false))
        assertEquals(WeatherIcon.SolNubes, WeatherCodes.icon(2, isDay = true))
        assertEquals(WeatherIcon.LunaNubes, WeatherCodes.icon(2, isDay = false))
    }
}
