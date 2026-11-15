package com.realfamousbae.standby.dock.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.SizeF

/**
 * Envoltorio de [AppWidgetHost]: una sola instancia por proceso (vive en AppGraph). Los
 * widgets se eligen en Ajustes y se muestran en StandbyActivity.
 *
 * Enganchar un widget a un host de otra app necesita que Iván lo permita: si
 * [bindIfAllowed] devolvió false, hay que mostrar el diálogo del sistema [bindIntent].
 */
class StandbyWidgetHost(context: Context) {

    private val appContext = context.applicationContext
    private val manager = AppWidgetManager.getInstance(appContext)
    private val host = AppWidgetHost(appContext, HOST_ID)

    /** Los widgets de escritorio de todas las apps instaladas, por nombre. */
    fun providers(): List<AppWidgetProviderInfo> =
        manager.installedProviders
            .filter { it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0 }
            .sortedBy { label(it).lowercase() }

    fun label(info: AppWidgetProviderInfo): String = info.loadLabel(appContext.packageManager).orEmpty()

    /** Nombre de la app dueña del widget. */
    fun appLabel(info: AppWidgetProviderInfo): String = runCatching {
        val pm = appContext.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
    }.getOrDefault(info.provider.packageName)

    fun allocate(): Int = host.allocateAppWidgetId()

    fun bindIfAllowed(widgetId: Int, provider: ComponentName): Boolean =
        manager.bindAppWidgetIdIfAllowed(widgetId, provider)

    /** Diálogo del sistema: "¿Permitir que la app cree widgets y reciba sus datos?". */
    fun bindIntent(widgetId: Int, provider: ComponentName): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)

    /**
     * Abrir la pantalla de configuración del widget, si tiene. Va por el host y no por
     * startActivity: esa pantalla casi nunca está exportada y solo la puede abrir el host. El
     * resultado llega a `onActivityResult` con [requestCode]. Devuelve false si no hay nada que configurar.
     */
    fun startConfigureIfNeeded(activity: Activity, widgetId: Int, requestCode: Int): Boolean {
        val info = manager.getAppWidgetInfo(widgetId) ?: return false
        if (info.configure == null) return false
        host.startAppWidgetConfigureActivityForResult(activity, widgetId, 0, requestCode, null)
        return true
    }

    fun delete(widgetId: Int) = host.deleteAppWidgetId(widgetId)

    /** null = el widget ya no está (por ejemplo, desinstalaron su app). */
    fun info(widgetId: Int?): AppWidgetProviderInfo? = widgetId?.let { manager.getAppWidgetInfo(it) }

    fun createView(context: Context, widgetId: Int): AppWidgetHostView? {
        val info = manager.getAppWidgetInfo(widgetId) ?: return null
        return host.createView(context, widgetId, info)
    }

    /** Avisarle al widget el tamaño real de la tarjeta, para que elija cómo acomodarse. */
    fun updateSize(view: AppWidgetHostView, widthDp: Float, heightDp: Float) {
        view.updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp, heightDp)))
    }

    /** Borrar todos los id que reservó el host menos [keep] (restos de elecciones canceladas). */
    fun deleteAllExcept(keep: Set<Int>) {
        host.appWidgetIds.filter { it !in keep }.forEach(host::deleteAppWidgetId)
    }

    fun startListening() = host.startListening()

    fun stopListening() = host.stopListening()

    companion object {
        private const val HOST_ID = 0x5717
    }
}
