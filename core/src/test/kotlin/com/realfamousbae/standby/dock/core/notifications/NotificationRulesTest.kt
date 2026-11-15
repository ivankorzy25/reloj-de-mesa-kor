package com.realfamousbae.standby.dock.core.notifications

import com.realfamousbae.standby.dock.core.notifications.LockscreenPrivacy.VISIBILITY_NO_OVERRIDE
import com.realfamousbae.standby.dock.core.notifications.LockscreenPrivacy.VISIBILITY_PRIVATE
import com.realfamousbae.standby.dock.core.notifications.LockscreenPrivacy.VISIBILITY_PUBLIC
import com.realfamousbae.standby.dock.core.notifications.LockscreenPrivacy.VISIBILITY_SECRET
import com.realfamousbae.standby.dock.core.prefs.StandbySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRulesTest {

    private val message = NotificationInfo(
        packageName = "app.messenger",
        isOwnPackage = false,
        isOngoing = false,
        isGroupSummary = false,
        importance = 4,
        matchesInterruptionFilter = true,
    )
    private val settings = StandbySettings()

    @Test
    fun `un mensaje común se muestra`() {
        assertTrue(NotificationFilter.shouldShow(message, settings))
        assertTrue(NotificationFilter.shouldShow(message.copy(importance = 3), settings))
    }

    @Test
    fun `sin sonido, fijas, resúmenes, las propias y las que tapa el DND no se muestran`() {
        assertFalse(NotificationFilter.shouldShow(message.copy(importance = 2), settings))
        assertFalse(NotificationFilter.shouldShow(message.copy(isOngoing = true), settings))
        assertFalse(NotificationFilter.shouldShow(message.copy(isGroupSummary = true), settings))
        assertFalse(NotificationFilter.shouldShow(message.copy(isOwnPackage = true), settings))
        assertFalse(NotificationFilter.shouldShow(message.copy(matchesInterruptionFilter = false), settings))
    }

    @Test
    fun `nuestro silencio no apaga los avisos, el No molestar de Iván sí`() {
        val intercepted = message.copy(matchesInterruptionFilter = false)
        // La interceptó solo nuestro modo: el aviso se muestra.
        assertTrue(NotificationFilter.shouldShow(intercepted.copy(ownQuietModeActive = true), settings))
        // El No molestar ya estaba prendido antes que nosotros: lo respetamos.
        assertFalse(
            NotificationFilter.shouldShow(intercepted.copy(ownQuietModeActive = true, userDndActive = true), settings),
        )
        // Nuestro modo no está: si la interceptaron, fue el No molestar de Iván.
        assertFalse(NotificationFilter.shouldShow(intercepted.copy(userDndActive = true), settings))
        // Lo que pasa el filtro del DND se muestra igual.
        assertTrue(NotificationFilter.shouldShow(message.copy(ownQuietModeActive = true, userDndActive = true), settings))
    }

    @Test
    fun `la lista de apps tapadas y el interruptor de avisos`() {
        val blocked = settings.copy(notificationBlockedPackages = setOf("app.messenger"))
        assertFalse(NotificationFilter.shouldShow(message, blocked))
        assertTrue(NotificationFilter.shouldShow(message.copy(packageName = "app.other"), blocked))
        assertFalse(NotificationFilter.shouldShow(message, settings.copy(notificationBannersEnabled = false)))
    }

    @Test
    fun `la privacidad, igual que en el sistema`() {
        fun d(v: Int, o: Int = VISIBILITY_NO_OVERRIDE, allow: Boolean? = true, pub: Boolean = false) =
            LockscreenPrivacy.decide(v, o, allow, pub)

        assertEquals(BannerContent.Full, d(VISIBILITY_PRIVATE, allow = true))
        assertEquals(BannerContent.AppOnly, d(VISIBILITY_PRIVATE, allow = false))
        assertEquals(BannerContent.PublicVersion, d(VISIBILITY_PRIVATE, allow = false, pub = true))
        // No se pudo leer el ajuste: por las dudas, tapamos.
        assertEquals(BannerContent.AppOnly, d(VISIBILITY_PRIVATE, allow = null))
        assertEquals(BannerContent.Full, d(VISIBILITY_PUBLIC, allow = false))
        assertEquals(BannerContent.Hidden, d(VISIBILITY_SECRET))
        // El canal tapó más de lo que pedía la notificación.
        assertEquals(BannerContent.Hidden, d(VISIBILITY_PUBLIC, o = VISIBILITY_SECRET))
        assertEquals(BannerContent.AppOnly, d(VISIBILITY_PUBLIC, o = VISIBILITY_PRIVATE, allow = false))
    }

    @Test
    fun `volver a publicar lo mismo no se muestra de nuevo`() {
        val dedupe = BannerDedupe(capacity = 2)
        assertTrue(dedupe.accept("k1", "Ana", "Hola"))
        assertFalse(dedupe.accept("k1", "Ana", "Hola"))
        assertTrue(dedupe.accept("k1", "Ana", "¿Dónde estás?"))
        dedupe.forget("k1")
        assertTrue(dedupe.accept("k1", "Ana", "¿Dónde estás?"))
        // Las claves viejas se van cayendo.
        dedupe.accept("k2", "a", "b")
        dedupe.accept("k3", "a", "b")
        assertTrue(dedupe.accept("k1", "Ana", "¿Dónde estás?"))
    }
}
