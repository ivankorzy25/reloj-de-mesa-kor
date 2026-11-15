package com.realfamousbae.standby.dock.core.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldCitiesTest {

    @Test
    fun `ida y vuelta del texto guardado`() {
        val cities = listOf(WorldCity("Madrid", "Europe/Madrid"), WorldCity("Tokio", "Asia/Tokyo"))
        assertEquals("Madrid|Europe/Madrid;Tokio|Asia/Tokyo", WorldCities.encode(cities))
        assertEquals(cities, WorldCities.decode(WorldCities.encode(cities)))
    }

    @Test
    fun `lo que no se entiende cae en las de fabrica`() {
        assertEquals(WorldCities.defaults, WorldCities.decode(null))
        assertEquals(WorldCities.defaults, WorldCities.decode(""))
        assertEquals(WorldCities.defaults, WorldCities.decode("basura;;"))
    }

    @Test
    fun `nunca se guardan mas de tres`() {
        val muchas = WorldCities.catalog.take(5)
        assertEquals(WorldCities.MAX, WorldCities.encode(muchas).split(";").size)
        assertEquals(WorldCities.MAX, WorldCities.decode(muchas.joinToString(";") { "${it.name}|${it.zoneId}" }).size)
    }

    @Test
    fun `prender y apagar ciudades`() {
        val madrid = WorldCity("Madrid", "Europe/Madrid")
        val tokio = WorldCity("Tokio", "Asia/Tokyo")
        assertEquals(listOf(madrid, tokio), WorldCities.toggle(listOf(madrid), tokio))
        assertEquals(listOf(tokio), WorldCities.toggle(listOf(madrid, tokio), madrid))
        // Con tres puestas, la nueva saca a la más vieja.
        val tres = WorldCities.catalog.take(3)
        val cuarta = WorldCities.catalog[3]
        assertEquals(tres.drop(1) + cuarta, WorldCities.toggle(tres, cuarta))
        // La última no se puede apagar: alguna ciudad tiene que quedar.
        assertEquals(listOf(madrid), WorldCities.toggle(listOf(madrid), madrid))
    }

    @Test
    fun `todas las ciudades del catalogo tienen huso valido`() {
        val zones = java.util.TimeZone.getAvailableIDs().toSet()
        assertTrue(WorldCities.catalog.all { it.zoneId in zones })
        assertTrue(WorldCities.defaults.all { it.zoneId in zones })
    }
}
