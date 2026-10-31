# Geolocalización con IPinfo Lite — Plan de implementación

Fecha: 2026-10-30
Diseño: `docs/plans/2026-10-25-geolocalizacion-ipinfo-design.md`
Rama: `feat/fase8-ipinfo-geolocalizacion`

## Objetivo

Que la tarjeta del perfil y la pantalla Detalles muestren bandera y país del
endpoint. Hoy nunca funcionó: `IpApiClient` pedía `https://ipapi.co/json/{ip}/`
cuando ipapi.co sirve el país en `https://ipapi.co/{ip}/json/`, así que toda
petición devolvía 404.

## Decisiones (heredadas del diseño)

- IPinfo Lite es el proveedor principal: sin límite de cuota, token por
  BuildConfig, atribución visible en Acerca de.
- ipwho.is es el respaldo de emergencia: HTTPS sin token, 1.000 peticiones/día
  por IP de cliente. Solo se consulta si el primario falla por infraestructura.
- El país nunca es un error visible en pantalla; el detalle va al registro.
- El registro nunca lleva la IP completa ni el token.

## Pasos (TDD: test rojo → código verde)

1. **`IpValidator`** (`data/geo/IpValidator.kt`): acepta IPv4 e IPv6 completas y
   válidas; rechaza IP cortada, con puerto, con corchetes, con CIDR, con zona
   IPv6 y dominios. Test: `IpValidatorTest`.
2. **Transporte HTTP inyectable** (`data/geo/HttpTransport.kt`): `get(url)` →
   estado, cuerpo y cabeceras. Permite probar los clientes sin red.
3. **`IpinfoLiteClient`**: URL `https://api.ipinfo.io/lite/{ip}?token=…`, token
   ausente → no consulta, `country_code`/`country`, `bogon` → sin país, 429 →
   `RateLimitedException`, resto de errores → excepción con código HTTP.
   Test: `IpinfoLiteClientTest`.
4. **`IpwhoIsClient`**: URL `https://ipwho.is/{ip}`, `success:false` → sin país.
   Test: `IpwhoIsClientTest`.
5. **`GeoLocator`**: encadena primario y respaldo. Fallo de infraestructura del
   primario → respaldo; `429` → se propaga sin respaldo; éxito → no consulta el
   respaldo. Test: `GeoLocatorTest`.
6. **`CountryRepository`**: valida la IP antes de pedir (sin red si es inválida),
   caché de 30 días por perfil (renueva si cambia la IP), un reintento a los
   ~3 s, enfriamiento por perfil tras el fallo, cachea solo cuando hay país, y
   registra sin IP ni token. Test: `CountryRepositoryTest`,
   `CountryRepositoryLogTest`.
7. **`CountryPingViewModel`**: consulta solo con la app en primer plano y
   cancela la petición en vuelo al desconectar. Test: `CountryPingViewModelTest`.
8. **Cableado**: `AppContainer` compone la cadena, `build.gradle.kts` inyecta
   `IPINFO_TOKEN` desde el entorno, el workflow de CI lo pasa como secret,
   `CountryInfo` documenta el proveedor nuevo, Acerca de muestra la atribución.
9. **Fuera de código**: crear el secret `IPINFO_TOKEN` en el repositorio.

## Verificación

- `testDebugUnitTest`, `assembleDebug` y `assembleRelease` en GitHub Actions
  (esta máquina no tiene toolchain; la CI es el veredicto).
- Prueba manual con la APK debug: conectar y ver la bandera, reconectar y
  comprobar que sale de caché, y comprobar que sin token o sin red la pantalla
  no muestra errores.

## Riesgo

El respaldo comparte cuota por IP de salida del túnel: si IPinfo se cae y
todos los usuarios caen al respaldo, el techo de 1.000/día se agota y la app
degrada a «sin país» (sin error visible). Es aceptable: el primario es
ilimitado y el respaldo solo existe para la caída.
