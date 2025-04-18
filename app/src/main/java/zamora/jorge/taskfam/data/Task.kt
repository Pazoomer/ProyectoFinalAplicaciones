package zamora.jorge.taskfam.data

import android.os.Parcel
import android.os.Parcelable

data class Task(
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val homeId: String = "",
    val assignments: Map<String, Map<String, Boolean>> = emptyMap()
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        mutableMapOf<String, Map<String, Boolean>>().apply {
            val size = parcel.readInt()
            for (i in 0 until size) {
                val memberId = parcel.readString() ?: ""
                val innerMapSize = parcel.readInt()
                val dias = mutableMapOf<String, Boolean>()
                for (j in 0 until innerMapSize) {
                    val dia = parcel.readString() ?: ""
                    val estado = parcel.readByte().toInt() != 0
                    dias[dia] = estado
                }
                this[memberId] = dias
            }
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(titulo)
        parcel.writeString(descripcion)
        parcel.writeString(homeId)

        parcel.writeInt(assignments.size)
        for ((memberId, dias) in assignments) {
            parcel.writeString(memberId)
            parcel.writeInt(dias.size)
            for ((dia, estado) in dias) {
                parcel.writeString(dia)
                parcel.writeByte(if (estado) 1 else 0)
            }
        }
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Task> {
        override fun createFromParcel(parcel: Parcel): Task = Task(parcel)
        override fun newArray(size: Int): Array<Task?> = arrayOfNulls(size)
    }
}
