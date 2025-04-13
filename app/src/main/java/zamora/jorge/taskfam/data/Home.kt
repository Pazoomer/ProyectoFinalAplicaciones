package zamora.jorge.taskfam.data

import android.os.Parcel
import android.os.Parcelable

data class Home(
    var id: String = "",
    var nombre: String = "",
    var code: String = "",
    var color: Int = 0,
    var editable: Boolean = true,
    var adminId:String ="",
    var members: List<String> = emptyList()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readString()?:"",
        parcel.createStringArrayList() ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(nombre)
        parcel.writeString(code)
        parcel.writeInt(color)
        parcel.writeByte(if (editable) 1 else 0)
        parcel.writeString(adminId)
        parcel.writeStringList(members)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Home> {
        override fun createFromParcel(parcel: Parcel): Home {
            return Home(parcel)
        }

        override fun newArray(size: Int): Array<Home?> {
            return arrayOfNulls(size)
        }
    }
}

