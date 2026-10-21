# Downly

App Android para descargar videos y audio de TikTok (sin marca de agua), Instagram (reels y posts), YouTube, Facebook, X/Twitter, Pinterest, Reddit, Vimeo y más de 1000 sitios, en distintas calidades o como MP3.

> iOS: disponible pronto.

## Descargar el APK

Cada cambio en `main` compila el APK automáticamente. Descárgalo desde **Releases** del repositorio:

| Archivo | Para quién |
|---|---|
| `Downly-arm64-v8a.apk` | La mayoría de móviles actuales (recomendado) |
| `Downly-armeabi-v7a.apk` | Móviles antiguos de 32 bits |
| `Downly-universal.apk` | Si no sabes cuál; funciona en todos |

Todas las versiones se firman con la misma clave, así que las nuevas se instalan encima sin perder el historial.

## Funciones

- Pegar enlace, botón **Pegar** o **Compartir → Downly** desde cualquier app
- Lista de calidades reales del video (144p → 4K) con tamaño estimado
- TikTok sin marca de agua (descarta los formatos marcados)
- Audio en **MP3** 128 / 192 / 320 kbps
- Descargas en segundo plano con notificación de progreso, cancelar y reintentar
- Guarda en `Películas/Downly` y `Música/Downly` (visibles en galería y reproductor)
- Historial con filtros, abrir, compartir y borrar
- Actualización del motor (yt-dlp) desde Ajustes, para cuando una plataforma cambia

## Anuncios

Configurados en `app/src/main/java/com/downly/app/ads/AdConfig.kt` (ahora con **IDs de prueba** de AdMob):

| Espacio | Dónde aparece |
|---|---|
| Banner adaptable | Siempre, encima de la barra inferior |
| Rectángulo mediano (300×250) | Pantalla de inicio, antes de pegar un enlace |
| Intersticial (repentino) | Al iniciar descargas (1 de cada 2, máx. 1 por minuto) |
| Recompensa | Desbloquea Full HD, 2K, 4K y MP3 320 durante 30 min |
| App Open | Al volver a la app (máx. 1 cada 4 h) |

Para cobrar: crea la app en AdMob, pon tu App ID en `res/values/strings.xml` (`admob_app_id`) y los IDs de bloque en `AdConfig.kt`. `ADS_ENABLED = false` desactiva todos los anuncios.

## Firma de release propia (opcional)

En *Settings → Secrets and variables → Actions* añade `DOWNLY_KEYSTORE_B64` (keystore en base64), `DOWNLY_KEYSTORE_PASSWORD`, `DOWNLY_KEY_ALIAS` y `DOWNLY_KEY_PASSWORD`. Si existen, el workflow firma con esa clave; si no, usa `app/debug.keystore`.

> Ojo: cambiar de clave obliga a desinstalar la versión anterior una vez.

## Compilar en local

```bash
./gradlew assembleRelease
```

Requiere JDK 17 y Android SDK 35.

## Estructura

```
app/src/main/java/com/downly/app/
├── engine/   # yt-dlp + ffmpeg: análisis, calidades, descarga, guardado
├── ads/      # AdMob: banner, MREC, intersticial, recompensa, app open
└── ui/       # Compose: Inicio, Descargas, Ajustes
```

## Uso responsable

Descarga solo contenido propio, de dominio público o con permiso del autor, y respeta los términos de cada plataforma.
