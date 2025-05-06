package zamora.jorge.taskfam

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = getColor(android.R.color.black)
        binding = ActivityCreateJoinHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtiene la instancia del servicio de autenticación de Firebase
        auth = FirebaseAuth.getInstance()

        // Obtiene la referencia a la base de datos de firebase apuntado a homes
        database = FirebaseDatabase.getInstance().reference.child("homes")

        //Barra de arriba del dispositivo ovil cambiado a color negro
        window.statusBarColor = Color.BLACK

        // Inicializa el CasaAdapter con el contexto y lista de casas vacias
        casaAdapter = CasaAdapter(this, listaCasas)
        // Asigna el adaptador a la listView
        binding.listaCasas.adapter = casaAdapter


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

        // Listener para el botón de cerrar sesión.
        binding.btnCerrarSesion.setOnClickListener{
            // Cierra la sesión del usuario en firebase auth
            Firebase.auth.signOut()
            //V Vuelve a la actividad Login
            startActivity(Intent(this,Login::class.java))
            // Finaliza la actividad para que no se pueda regresar
            finish()
        }
            // Empieza el proceso para obtener los lugares a los que el usuario pertenece
            obtenerCasasDeUsuario()
    }

    /**
     * Establece un listener en homes de firebase para escuchar los cambios de los hogares
     * Filtra los hogares para que solo aparezcan esos a los que el usuario pertenece.
     */
    private fun obtenerCasasDeUsuario() {
        //Obtiene el ID del usurio que esta logueado, si no lo hay sale de la funcíón
        val userId = auth.currentUser?.uid ?: return

        // Este listener reacciona a eventos en el nodo de homes
        database.addChildEventListener(object : ChildEventListener {

            /**
             * Para cada hogar que tiene el usuario cuando el listener se llama por primera vez
             */
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // Conversión del DataSnapchot a objeto home
                val home = snapshot.getValue(Home::class.java)
                home?.let {
                    // Verifica si el usuario logueado pertenece a la lista de miembros del hoogar obtenido
                    if (it.members.contains(userId)) {
                        // Se agrega a la lista de casa si el usuario es miembro
                        listaCasas.add(it)
                        // Le notifica ala daptador que los datos cambiaron para que la listView actualice su vista
                        casaAdapter.notifyDataSetChanged()
                    }
                }

                //Actualiza la visiblidad de los elementos en caso de tener o no tener casas
                actualizarVista()
            }

            /**
             * Se llama cuando los datos de un hogar existente en el nodo de homes de la firebase se actualiza
             */
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Conversión del DataSnapchot a objeto home
                val home = snapshot.getValue(Home::class.java)
                home?.let {
                    val index = listaCasas.indexOfFirst { h -> h.id == it.id }
                    // Si el hogar se encuentra en la lista local
                    if (index != -1) {
                        // Reemplaza el hogar existente en la lista local con los datos actualizados
                        listaCasas[index] = it
                        // Le notifica ala daptador que los datos cambiaron
                        casaAdapter.notifyDataSetChanged()
                    }
                    //Actualiza la visiblidad de los elementos en caso de tener o no tener casas
                    actualizarVista()
                }
            }

            /**
             * Se llama cuando un hogar se elimina del nodo homes en firebase
             */
            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Conversión del DataSnapchot a objeto home
                val home = snapshot.getValue(Home::class.java)
                home?.let {
                    // Remueve el hogar de la lista local basándose en su ID
                    listaCasas.removeAll { h -> h.id == it.id }
                    // Notifica al adaptador que los datos han cambiado
                    casaAdapter.notifyDataSetChanged()
                }
                //Actualiza la vista en caso de que esta quede vacia
                actualizarVista()
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            /**
             * Se llama si la operación es cancelada
             */
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CreateJoinHome, "Error al obtener hogares: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Actualiza la visibilidad de la ListView de casas y los botones de crear/unirse a hogar.
     * Si la lista local 'listaCasas' está vacía (el usuario no tiene casas unidas),
     * oculta la ListView y los botones "normales" y muestra los botones "grandes".
     * Si la lista tiene casas, muestra la ListView y los botones "normales"
     * y oculta los botones "grandes".
     */
    private fun actualizarVista() {
        // Veirica si la lista de casas esta vacia
        if (listaCasas.isEmpty()) {
            // Si no hay casas los botones pequeños se ocultan junto con la lista de casas
            binding.listaCasas.visibility = View.GONE
            binding.btnCrearHogar.visibility = View.GONE
            binding.btnNuevoHogar.visibility = View.GONE

            // Se muestra el contenedor de los botones grandes
            binding.botonesGrandes.visibility = View.VISIBLE
        } else {
            // Si hay casas se muestran los botones pequeños y la lista de casas
            binding.listaCasas.visibility = View.VISIBLE
            binding.btnCrearHogar.visibility = View.VISIBLE
            binding.btnNuevoHogar.visibility = View.VISIBLE

            // Se ocultan los botones grandes
            binding.botonesGrandes.visibility = View.GONE
        }
    }

    /**
     * Adapador para mostrar una lista de objetos Home en una listView
     * @param context contexto
     * @param casas lista de los objetos Home a mostrar
     */
    inner class CasaAdapter(private val context: Context, private val casas: List<Home>) : BaseAdapter() {
        /**
         * Retorna el numero de elementos en la lista
         * @return cantidad de casas en la lista
         */
        override fun getCount(): Int = casas.size

        /**
         * Retorna el elemento en la posición especificada
         * @param position posición del elemento
         * @return Objeto Home de la posición
         */
        override fun getItem(position: Int): Any = casas[position]

        /**
         * Retorna el id del elemento de la posición especificada
         * @param position posición del elemento
         * @return ID de home
         */
        override fun getItemId(position: Int): Long = position.toLong()

        /**
         * Retorna el view que visualiza los datos de las casas
         * @param position La posición del elemento que se solicita para añadir al view
         * @param convertView La lista vieja del elemento
         * @param parent el viewgroup padre al que la vista se adjuntara
         * @return view para el elemento que se especifico
         */
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

            // Infla el layout item_home si convertView es null (no hay vista para reutilizar).
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_home, parent, false)

            //Pbtiene el objeto Home para la posicion actual
            val home = casas[position]

            // Referenci los elementos de la UI dentro del layout item_home
            val tvCasaNombre = view.findViewById<TextView>(R.id.tv_casa_nombre)
            val icon = view.findViewById<ImageView>(R.id.ivHomeIcon)

            // Establece el texto del textview del nombre de la casa
            tvCasaNombre.text = casas[position].nombre
            // Establece el color del icono que se usará psegún el objeto Home
            icon.setColorFilter(home.color, PorterDuff.Mode.SRC_IN)

            //Listener para el elemento de la lista
            view.setOnClickListener {
                // Creación del intent para la MainActivity
                val intent = Intent(context, MainActivity::class.java)
                // Pasa el objeto Home que se selecciono al intent
                intent.putExtra("HOME", home)
                // Inicia la MainActivity
                context.startActivity(intent)
            }
            // Retorna la vista para el elemento
            return view
        }
    }
}
