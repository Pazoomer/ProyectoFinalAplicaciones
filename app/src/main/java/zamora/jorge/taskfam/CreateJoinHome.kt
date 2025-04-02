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
import androidx.appcompat.app.AppCompatActivity
import zamora.jorge.taskfam.data.Home
import zamora.jorge.taskfam.databinding.ActivityCreateJoinHomeBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class CreateJoinHome : AppCompatActivity() {

    private lateinit var binding: ActivityCreateJoinHomeBinding
    private lateinit var casaAdapter: CasaAdapter
    private val listaCasas = mutableListOf<Home>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = getColor(android.R.color.black)
        binding = ActivityCreateJoinHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.BLACK

        //Obtener datos del intent
        val correo = intent.getStringExtra("correo")
        //TODO: HACER UNA CONSULTA A LA BASE DE DATOS PARA SABER SI EL USUARIO TIENE UN HOGAR

        //TODO: SI TIENE UN HOGAR, MOSTRARLO EN EL GRIDVIEW

        //TODO: SI NO TIENE UN HOGAR, MOSTRAR LOS DOS BOTONES EN GRANDE

//        llenarListaCasas()

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
        }
    }

//    private fun llenarListaCasas() {
//        listaCasas.apply {
//            add(Home("Casa Mochis"))
//            add(Casa("Casa Monterrey"))
//            add(Casa("Casa Guadalajara"))
//            add(Casa("Casa CDMX"))
//            add(Casa("Casa Cancún"))
//            add(Casa("Casa Puebla"))
//            add(Casa("Casa Mochis"))
//            add(Casa("Casa Monterrey"))
//            add(Casa("Casa Guadalajara"))
//            add(Casa("Casa CDMX"))
//            add(Casa("Casa Cancún"))
//            add(Casa("Casa Puebla"))
//            add(Casa("Casa Mochis"))
//            add(Casa("Casa Monterrey"))
//            add(Casa("Casa Guadalajara"))
//            add(Casa("Casa CDMX"))
//            add(Casa("Casa Cancún"))
//            add(Casa("Casa Puebla"))
//        }
//    }

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
