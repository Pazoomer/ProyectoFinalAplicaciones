package zamora.jorge.taskfam.data

data class Member(
    var id: String = "",
    var name: String = "",
    var email: String = "",
    var color: Int = 0
) {
    override fun toString(): String {
        return name
    }
}
