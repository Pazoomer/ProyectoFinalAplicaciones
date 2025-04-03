package zamora.jorge.taskfam.data

data class Home(
    var id: String = "",
    var nombre: String = "",
    var code: String = "",
    var color: Int = 0,
    var editable: Boolean = true,
    var members: List<String> = emptyList()
)
