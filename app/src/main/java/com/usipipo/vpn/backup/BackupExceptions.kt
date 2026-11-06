package com.usipipo.vpn.backup

/** Contraseña incorrecta: la copia no se descifra y no se importa nada. */
class WrongPasswordException(
    message: String = "Contraseña incorrecta",
) : Exception(message)

/** Copia ilegible: no es un zip válido, le falta settings.json o trae entradas peligrosas. */
class CorruptBackupException(
    message: String = "La copia de seguridad está corrupta",
) : Exception(message)
