package zamora.jorge.taskfam.data

data class Member(
    var id: String = "",
    var name: String = "",
    var email: String = "",
    //var tasks: List<String> = emptyList
    //
    // ()
) {
    override fun toString(): String {
        return name
    }
}
