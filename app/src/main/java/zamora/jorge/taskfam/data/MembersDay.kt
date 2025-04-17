package zamora.jorge.taskfam.data

data class MembersDay(
    val member: Member,
    val diasSeleccionados: MutableMap<String, Boolean> = mutableMapOf(
        "Lunes" to false,
        "Martes" to false,
        "Miércoles" to false,
        "Jueves" to false,
        "Viernes" to false,
        "Sábado" to false,
        "Domingo" to false
    )


)
