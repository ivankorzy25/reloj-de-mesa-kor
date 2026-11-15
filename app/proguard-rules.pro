# Reglas de R8 para la versión final (la que se pasa a los amigos).
#
# Compose y DataStore traen las suyas; acá queda solo lo propio de esta app.

# Los modelos que se leen de JSON (clima de Open-Meteo, fotos de Immich) se arman por
# reflexión desde kotlinx.serialization: sus serializadores no se pueden borrar.
-keepclassmembers class com.realfamousbae.standby.dock.**$$serializer { *; }
-keepclasseswithmembers class com.realfamousbae.standby.dock.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Componentes de Android que nombra el manifiesto (el nombre tiene que quedar igual).
-keep class com.realfamousbae.standby.dock.App
-keep class com.realfamousbae.standby.dock.service.** extends android.app.Service
-keep class com.realfamousbae.standby.dock.service.** extends android.content.BroadcastReceiver
-keep class com.realfamousbae.standby.dock.notifications.StandbyNotificationListener
