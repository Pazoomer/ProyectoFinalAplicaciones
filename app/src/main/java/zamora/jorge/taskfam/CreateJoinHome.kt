package zamora.jorge.taskfam

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import zamora.jorge.taskfam.data.Home
import zamora.jorge.taskfam.databinding.ActivityCreateJoinHomeBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class CreateJoinHome : AppCompatActivity() {

    private lateinit var binding: ActivityCreateJoinHomeBinding
    private lateinit var casaAdapter: CasaAdapter
    private val listaCasas = mutableListOf<Home>()
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = getColor(android.R.color.black)
        binding = ActivityCreateJoinHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("homes")

        window.statusBarColor = Color.BLACK

        //Obtener datos del intent
        val correo = intent.getStringExtra("correo")

        casaAdapter = CasaAdapter(this, listaCasas)
        binding.listaCasas.adapter = casaAdapter

        // Manejar clic en los ítems del GridView
        binding.listaCasas.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val tvCasaNombre = view.findViewById<TextView>(R.id.tv_casa_nombre)
            val casaNombre = tvCasaNombre.text.toString()

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("CASA_NOMBRE", casaNombre)
            startActivity(intent)
        }

        // Configurar botones de navegación
        binding.btnCrearHogar.setOnClickListener {
            startActivity(Intent(this, CreateHome::class.java))
        }
        binding.btnCrearHogarGrande.setOnClickListener {
            startActivity(Intent(this, CreateHome::class.java))
        }
        binding.btnNuevoHogar.setOnClickListener {
            startActivity(Intent(this, JoinHouse::class.java))
        }
        binding.btnNuevoHogarGrande.setOnClickListener {
            startActivity(Intent(this, JoinHouse::class.java))
        }

        binding.btnCerrarSesion.setOnClickListener{
            Firebase.auth.signOut()
            startActivity(Intent(this,Login::class.java))
            finish()
        }

        // Se ejecuta solo la primera vez
        if (isFirstLoad) {
            obtenerCasasDeUsuario()
            isFirstLoad = false
        }
    }

    override fun onResume() {
        super.onResume()
        //obtenerCasasDeUsuario()
    }

    private fun obtenerCasasDeUsuario() {
        val userId = auth.currentUser?.uid ?: return

        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val home = snapshot.getValue(Home::class.java)
                home?.let {
                    if (it.members.contains(userId)) {
                        listaCasas.add(it)
                        casaAdapter.notifyDataSetChanged()
                    }
                }
                //Actualiza la visiblidad de los elementos en caso de tener o no tener casas
                actualizarVista()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val home = snapshot.getValue(Home::class.java)
                home?.let {
                    val index = listaCasas.indexOfFirst { h -> h.id == it.id }
                    if (index != -1) {
                        listaCasas[index] = it
                        casaAdapter.notifyDataSetChanged()
                    }
                    //Actualiza la visiblidad de los elementos en caso de tener o no tener casas
                    actualizarVista()
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val home = snapshot.getValue(Home::class.java)
                home?.let {
                    listaCasas.removeAll { h -> h.id == it.id }
                    casaAdapter.notifyDataSetChanged()
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CreateJoinHome, "Error al obtener hogares: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun actualizarVista() {
        if (listaCasas.isEmpty()) {
            binding.listaCasas.visibility = View.GONE
            binding.btnCrearHogar.visibility = View.GONE
            binding.btnNuevoHogar.visibility = View.GONE

            binding.botonesGrandes.visibility = View.VISIBLE
        } else {
            binding.listaCasas.visibility = View.VISIBLE
            binding.btnCrearHogar.visibility = View.VISIBLE
            binding.btnNuevoHogar.visibility = View.VISIBLE

            binding.btnCrearHogarGrande.visibility = View.GONE
            binding.btnNuevoHogarGrande.visibility = View.GONE
        }
    }

    inner class CasaAdapter(private val context: Context, private val casas: List<Home>) : BaseAdapter() {
        override fun getCount(): Int = casas.size
        override fun getItem(position: Int): Any = casas[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_home, parent, false)

            val tvCasaNombre = view.findViewById<TextView>(R.id.tv_casa_nombre)
            tvCasaNombre.text = casas[position].nombre

            return view
        }
    }
}
