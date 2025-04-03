package zamora.jorge.taskfam

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import zamora.jorge.taskfam.data.Home
import zamora.jorge.taskfam.data.Member
import zamora.jorge.taskfam.databinding.ActivitySettingsBinding

class Settings : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var home: Home? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

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

        window.statusBarColor = Color.BLACK

        home = intent.getParcelableExtra<Home>("HOME")

        //TODO: Si el usuario no es creador del hogar, no dejarle editar nada

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

        binding.etNombreHogar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No es necesario implementar este método
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No es necesario implementar este método
            }

            override fun afterTextChanged(s: Editable?) {
                guardarNombre(s.toString())
            }
        })

        //home = intent.getParcelableExtra<Home>("HOME")
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("members")

        getMembers()
        //colocarDatosEjemplo()

        binding.tvSubtitulo.text = "Codigo del hogar: ${home!!.code}"

        home?.let {
            binding.tvSubtitulo.text = "Código del hogar: ${it.code}"
            binding.etNombreHogar.setText(it.nombre)
        } ?: run {
            binding.tvSubtitulo.text = "Error: No se encontró el hogar"
        }
    }

    fun getMembers() {

        if(home==null){
            return
        }else if(home?.members==null){
            return
        }

        val membersIds = home?.members ?: emptyList()

        if (membersIds.isEmpty()) {
            Log.d("Firebase", "No hay miembros en este hogar.")
            return
        }

        val membersHome = mutableListOf<Member>()

        for (memberId in membersIds) {
            database.child(memberId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val member = snapshot.getValue(Member::class.java)
                        member?.let { membersHome.add(it) }
                    }

                    if (membersHome.size == membersIds.size) {
                        val listView = findViewById<ListView>(R.id.listViewMiembros)
                        val adapter = MiembroAdapter(this@Settings, membersHome)
                        listView.adapter = adapter
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error al obtener miembro: ${error.message}")
                }
            })
        }
    }

    fun guardarNombre(texto: String) {
        //Comprobar que no esté vacío
        if (texto.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese un nombre", Toast.LENGTH_SHORT).show()
            return
        }

        //TODO: Guardar el nombre del hogar en la base de datos
        Toast.makeText(this, "Texto actualizado: $texto", Toast.LENGTH_SHORT).show()
    }

    /*
    fun colocarDatosEjemplo(){
        val listView = findViewById<ListView>(R.id.listViewMiembros)
        val datosEjemplo = listOf(
            Member("Juan", true),
            Member("María", false),
            Member("Carlos", true),
            Member("Ana", false),
            Member("Luis", true)
        )

        val adapter = MiembroAdapter(this, datosEjemplo)
        listView.adapter = adapter
    }*/

    class MiembroAdapter(private val context: Context, private val data: List<Member>) : BaseAdapter() {

        override fun getCount(): Int = data.size

        override fun getItem(position: Int): Any = data[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.settings_member, parent, false)

            val tvNombre = view.findViewById<TextView>(R.id.tvNombre)
            val checkBoxRol = view.findViewById<CheckBox>(R.id.rol)
            val btnEliminar = view.findViewById<ImageView>(R.id.btnEliminar)

            val miembro = data[position]

            tvNombre.text = miembro.name
            //checkBoxRol.isChecked = miembro.
            //TODO: Member debe tener puede editar
            /*
            checkBoxRol.setOnCheckedChangeListener { _, isChecked ->
                miembro.puedeEditar = isChecked
            }*/

            btnEliminar.setOnClickListener {
                Toast.makeText(context, "Eliminar a ${miembro.name}", Toast.LENGTH_SHORT).show()
            }
            return view
        }
    }
}
