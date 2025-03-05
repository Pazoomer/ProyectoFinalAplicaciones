package zamora.jorge.taskfam

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import zamora.jorge.taskfam.databinding.ActivityLoginBinding
import zamora.jorge.taskfam.databinding.ActivitySettingsBinding

class Settings : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

         override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
             binding = ActivitySettingsBinding.inflate(layoutInflater)
             setContentView(binding.root)

             binding.ivBackArrow.setOnClickListener {
                 val intent = Intent(this, MainActivity::class.java)
                 startActivity(intent)
             }

             binding.tvBorrarHogar.setOnClickListener {
                 val intent = Intent(this, Login::class.java)
                 startActivity(intent)
             }
             colocarDatosEjemplo()
    }

    fun colocarDatosEjemplo(){
        val listView = findViewById<ListView>(R.id.listViewMiembros)
        val datosEjemplo = listOf(
            Miembro("Juan", true),
            Miembro("María", false),
            Miembro("Carlos", true),
            Miembro("Ana", false),
            Miembro("Luis", true)
        )

        val adapter = MiembroAdapter(this, datosEjemplo)
        listView.adapter = adapter
    }

    data class Miembro(val nombre: String, var puedeEditar: Boolean)

    class MiembroAdapter(private val context: Context, private val data: List<Miembro>) : BaseAdapter() {

        override fun getCount(): Int = data.size

        override fun getItem(position: Int): Any = data[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.settings_member, parent, false)

            val tvNombre = view.findViewById<TextView>(R.id.tvNombre)
            val checkBoxRol = view.findViewById<CheckBox>(R.id.rol)
            val btnEliminar = view.findViewById<ImageView>(R.id.btnEliminar)

            val miembro = data[position]

            tvNombre.text = miembro.nombre
            checkBoxRol.isChecked = miembro.puedeEditar

            checkBoxRol.setOnCheckedChangeListener { _, isChecked ->
                miembro.puedeEditar = isChecked
            }

            btnEliminar.setOnClickListener {
                Toast.makeText(context, "Eliminar a ${miembro.nombre}", Toast.LENGTH_SHORT).show()
            }
            return view
        }
    }
}
