package zamora.jorge.taskfam.data

data class Task(
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val homeId: String = "",
    val assignments: Map<String, Map<String, Boolean>> = emptyMap()
)