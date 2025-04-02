package zamora.jorge.taskfam.data

data class Home(
                var nombre:String,
                var code: String,
                var color: Int,
                var editable: Boolean,
                var members: List<Member>
)
