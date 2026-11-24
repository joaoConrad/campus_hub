package com.example.campushub

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.RatingBar
import com.google.android.gms.tasks.Task
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.widget.ImageView
import android.widget.FrameLayout
import java.io.File
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import java.io.FileInputStream
import android.media.MediaMetadataRetriever
import android.widget.VideoView
import android.widget.MediaController
import java.io.IOException



// Tela de detalhes: recebe o id de um evento, busca ele no Firestore e mostra os dados
class EventDetailsActivity : AppCompatActivity() {

    private lateinit var registrations: RegistrationRepository
    private lateinit var db: FirebaseFirestore

    private lateinit var tvTitulo: TextView
    private lateinit var tvDescricao: TextView
    private lateinit var tvData: TextView
    private lateinit var tvHorario: TextView
    private lateinit var tvLocal: TextView
    private lateinit var tvVagas: TextView
    private lateinit var tvCategoria: TextView
    private lateinit var btnInscrever: Button
    private lateinit var layoutAvaliacao: View
    private lateinit var ratingBarAvaliacao: RatingBar
    private lateinit var btnAvaliar: Button
    private lateinit var tvMediaDetalhe: TextView
    private lateinit var ratingRef: DocumentReference

    private lateinit var btnVoltar: Button

    private lateinit var mapaEvento: MapView
    private lateinit var tvEnderecoDetalhe: TextView

    private var eventId: String = ""
    private var uid: String = ""

    private var inscrito = false

    private lateinit var eventRef: DocumentReference
    private lateinit var regRef: DocumentReference
    private lateinit var btnFavoritar: Button
    private lateinit var favRef: DocumentReference
    private var favoritado = false

    private lateinit var recyclerComentarios: RecyclerView
    private lateinit var editComentario: EditText
    private lateinit var btnComentar: Button
    private lateinit var tvNenhumComentario: TextView
    private lateinit var comentariosAdapter: CommentAdapter

    private lateinit var btnEditar: Button

    private lateinit var btnEncerrar: Button
    private var nomeUsuario: String = "Aluno"

    private var naFila = false
    private var primeiroDaFila = false
    private var eventoAtual: Event? = null
    private lateinit var btnInscritos: Button
    private lateinit var imgEvento: ImageView
    private var organizador = false

    private lateinit var postAdapter: PostAdapter
    private lateinit var postRepository: PostRepository
    private lateinit var tvNenhumaPublicacao: TextView

    private lateinit var btnTirarFoto: Button
    private lateinit var btnFotoGaleria: Button

    private var arquivoCamera: File? = null

    private lateinit var btnGravarAudio: Button
    private lateinit var btnCancelarAudio: Button

    private lateinit var btnGravarVideo: Button
    private lateinit var btnVideoGaleria: Button

    private var arquivoVideoCamera: File? = null
    private var processandoVideo = false

    private var videoAtivo: VideoView? = null
    private var botaoVideoAtivo: Button? = null

    private var gravadorAudio: MediaRecorder? = null
    private var arquivoAudio: File? = null
    private var gravandoAudio = false

    private var playerAudio: MediaPlayer? = null

    private val handlerAudio = Handler(Looper.getMainLooper())

    private val limiteAudio = Runnable {
        if (gravandoAudio) {
            finalizarGravacaoAudio(true)
        }
    }


    private val permissaoMicrofone = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permitido ->

        if (permitido) {
            iniciarGravacaoAudio()
        } else {
            Toast.makeText(
                this,
                "Permissão do microfone negada",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun alternarGravacaoAudio() {

        if (gravandoAudio) {
            finalizarGravacaoAudio(true)
            return
        }

        val permissao = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        )

        if (permissao == PackageManager.PERMISSION_GRANTED) {
            iniciarGravacaoAudio()
        } else {
            permissaoMicrofone.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }


    private fun iniciarGravacaoAudio() {

        if (gravandoAudio) return

        var novoGravador: MediaRecorder? = null
        var novoArquivo: File? = null

        try {
            val pasta = File(filesDir, "audios")
            if (!pasta.exists() && !pasta.mkdirs()) {
                throw IllegalStateException(
                    "Não foi possível criar a pasta de áudios"
                )
            }

            val arquivo = File.createTempFile(
                "audio_${uid}_",
                ".m4a",
                pasta
            )

            novoArquivo = arquivo

            val recorder = MediaRecorder(this)
            novoGravador = recorder

            recorder.setAudioSource(
                MediaRecorder.AudioSource.MIC
            )

            recorder.setOutputFormat(
                MediaRecorder.OutputFormat.MPEG_4
            )

            recorder.setAudioEncoder(
                MediaRecorder.AudioEncoder.AAC
            )

            recorder.setAudioEncodingBitRate(64000)
            recorder.setAudioSamplingRate(44100)

            recorder.setOutputFile(arquivo.absolutePath)

            recorder.prepare()
            recorder.start()

            gravadorAudio = recorder
            arquivoAudio = arquivo
            gravandoAudio = true

            handlerAudio.postDelayed(
                limiteAudio,
                30_000L
            )

            atualizarBotoesAudio()

            Toast.makeText(
                this,
                "Gravando áudio...",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {

            runCatching { novoGravador?.release() }
            novoArquivo?.delete()

            Toast.makeText(
                this,
                "Erro ao gravar áudio: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun finalizarGravacaoAudio(publicar: Boolean) {

        if (!gravandoAudio) return

        handlerAudio.removeCallbacks(limiteAudio)

        gravandoAudio = false

        val recorder = gravadorAudio
        val arquivo = arquivoAudio

        gravadorAudio = null
        arquivoAudio = null

        var gravacaoValida = false

        try {
            recorder?.stop()
            gravacaoValida = recorder != null

        } catch (e: RuntimeException) {
            Toast.makeText(
                this,
                "Gravação muito curta ou inválida",
                Toast.LENGTH_SHORT
            ).show()

        } finally {
            recorder?.release()
        }

        atualizarBotoesAudio()

        if (!publicar) {
            arquivo?.delete()
            return
        }

        if (
            gravacaoValida &&
            arquivo != null &&
            arquivo.exists() &&
            arquivo.length() > 0L
        ) {

            publicarPost(
                PostType.AUDIO,
                arquivo.absolutePath
            )

        } else {
            arquivo?.delete()
        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "CampusHub/1.0 (trabalho academico URI Santiago)"
        setContentView(R.layout.activity_event_details)

        savedInstanceState
            ?.getString("arquivoVideoCamera")
            ?.let { caminho ->
                arquivoVideoCamera = File(caminho)
            }

        savedInstanceState
            ?.getString("arquivoCamera")
            ?.let { caminho ->
                arquivoCamera = File(caminho)
            }


        db = FirebaseFirestore.getInstance()
        registrations = RegistrationRepository(db)

        tvTitulo = findViewById(R.id.tvTituloDetalhe)
        tvDescricao = findViewById(R.id.tvDescricaoDetalhe)
        tvData = findViewById(R.id.tvDataDetalhe)
        tvHorario = findViewById(R.id.tvHorarioDetalhe)
        tvLocal = findViewById(R.id.tvLocalDetalhe)
        tvVagas = findViewById(R.id.tvVagasDetalhe)
        tvCategoria = findViewById(R.id.tvCategoriaDetalhe)
        btnInscrever = findViewById(R.id.btnInscrever)
        btnFavoritar = findViewById(R.id.btnFavoritar)
        imgEvento = findViewById(R.id.imgEvento)


        btnVoltar = findViewById(R.id.btnVoltar)
        btnFavoritar = findViewById(R.id.btnFavoritar)

        btnVoltar.setOnClickListener {
            finish()
        }

        val usuario = FirebaseAuth.getInstance().currentUser
        eventId = intent.getStringExtra("eventId") ?: ""

        if (usuario == null || eventId.isEmpty()) {
            Toast.makeText(this, "Evento não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        uid = usuario.uid

        eventRef = db.collection("events").document(eventId)
        regRef = db.collection("registrations").document("${uid}_$eventId")
        favRef = db.collection("favorites").document("${uid}_$eventId")

        btnFavoritar.isEnabled = false
        btnFavoritar.setOnClickListener { alternarFavorito() }

        btnInscrever.isEnabled = false

        btnInscrever.setOnClickListener { acaoDoBotao() }
        btnEditar = findViewById(R.id.btnEditar)
        btnEncerrar = findViewById(R.id.btnEncerrar)
        btnInscritos = findViewById(R.id.btnInscritos)

        btnInscritos.setOnClickListener {
            val intent = Intent(this, EventAttendeesActivity::class.java)
            intent.putExtra("eventId", eventId)
            startActivity(intent)
        }

        btnEditar.setOnClickListener {
            val intent = Intent(this, EventFormActivity::class.java)
            intent.putExtra("eventId", eventId)
            startActivity(intent)
        }
        btnEncerrar.setOnClickListener { confirmarEncerramento() }

        verificarFavorito()
        configurarComentarios()
        configurarPublicacoes()
        configurarAvaliacao()
        configurarMapa()
    }

    private val escolherFoto = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->

        if (uri != null) {
            publicarFoto(uri)
        }
    }

    // Tirar foto utilizando a câmera
    private val tirarFoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { sucesso ->

        val arquivo = arquivoCamera

        if (sucesso && arquivo != null && arquivo.exists()
            && arquivo.length() > 0) {

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                arquivo
            )

            publicarFoto(uri)

        } else {
            Toast.makeText(
                this,
                "Foto cancelada",
                Toast.LENGTH_SHORT
            ).show()
        }

        arquivo?.delete()
        arquivoCamera = null
    }

    private fun carregarEvento() {
        db.collection("events").document(eventId).get()
            .addOnSuccessListener { documento ->
                val evento = documento.toObject(Event::class.java)

                if (evento == null) {
                    Toast.makeText(this, "Evento não encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    mostrarEvento(evento)
                    verificarInscricao(evento)
                }
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao carregar evento: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun mostrarEvento(evento: Event) {
        val dataHora = evento.dataHora?.toDate()

        tvTitulo.text = evento.titulo
        tvCategoria.text = "Categoria: ${evento.categoria}"
        tvDescricao.text = evento.descricao
        tvData.text = "Data: " + Formatters.date(dataHora)
        tvHorario.text = "Horário: " + Formatters.time(dataHora)
        tvLocal.text = "Local: ${evento.local}"
        eventoAtual = evento
        tvVagas.text = "Vagas restantes: ${evento.vagas}" +
                if (evento.fila > 0) " · Lista de espera: ${evento.fila}" else ""
        val dono = evento.organizadorId == uid
        btnInscritos.visibility = if (dono) View.VISIBLE else View.GONE
        btnEditar.visibility = if (dono) View.VISIBLE else View.GONE
        btnEncerrar.visibility = if (dono && !evento.estaEncerrado()) View.VISIBLE else View.GONE

        val bitmap = ImageUtils.toBitmap(evento.imagem)
        if (bitmap != null) {
            imgEvento.setImageBitmap(bitmap)
            imgEvento.visibility = View.VISIBLE
        } else {
            imgEvento.visibility = View.GONE
        }

        mostrarLocalizacao(evento)
    }

    private fun verificarInscricao(evento: Event) {
        regRef.get()
            .addOnSuccessListener { documento ->
                inscrito = documento.exists()
                atualizarAvaliacao(evento)
                carregarFila(evento)
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao verificar inscrição: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun carregarFila(evento: Event) {
        db.collection("waitlist").whereEqualTo("eventId", eventId).get()
            .addOnSuccessListener { resultado ->
                val ordenados = resultado.documents
                    .sortedBy { it.getTimestamp("data")?.seconds ?: Long.MAX_VALUE }
                naFila = ordenados.any { it.getString("userId") == uid }
                primeiroDaFila = ordenados.firstOrNull()?.getString("userId") == uid
                atualizarBotao(evento)
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao carregar lista de espera: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun atualizarBotao(evento: Event) {
        if (organizador) {
            btnInscrever.visibility = View.GONE
            return
        }
        val estado = ButtonState.registration(
            evento.estaEncerrado(), inscrito, evento.vagas, evento.fila, naFila, primeiroDaFila
        )
        btnInscrever.visibility = View.VISIBLE
        btnInscrever.text = estado.text
        btnInscrever.isEnabled = estado.enabled
    }

    private fun acaoDoBotao() {
        val evento = eventoAtual ?: return
        when {
            inscrito -> cancelarInscricao()
            naFila && primeiroDaFila && evento.vagas > 0 ->
                executar(registrations.confirmFromWaitlist(uid, eventId), "Inscrição confirmada!")
            naFila ->
                executar(registrations.leaveWaitlist(uid, eventId), "Você saiu da lista de espera")
            evento.vagas - evento.fila > 0 -> inscrever()
            else ->
                executar(registrations.joinWaitlist(uid, eventId), "Você entrou na lista de espera")
        }
    }

    private fun executar(tarefa: Task<Void?>, sucesso: String) {
        btnInscrever.isEnabled = false
        tarefa
            .addOnSuccessListener {
                Toast.makeText(this, sucesso, Toast.LENGTH_SHORT).show()
                carregarEvento()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, erro.message, Toast.LENGTH_LONG).show()
                carregarEvento()
            }
    }

    private fun inscrever() {
        btnInscrever.isEnabled = false

        registrations.register(uid, eventId)
            .addOnSuccessListener {
                Toast.makeText(this, "Inscrição realizada!", Toast.LENGTH_SHORT).show()
                carregarEvento()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, erro.message, Toast.LENGTH_LONG).show()
                carregarEvento()
            }
    }

    private fun cancelarInscricao() {
        btnInscrever.isEnabled = false

        registrations.cancel(uid, eventId)
            .addOnSuccessListener {
                Toast.makeText(this, "Inscrição cancelada", Toast.LENGTH_SHORT).show()
                carregarEvento()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, erro.message, Toast.LENGTH_LONG).show()
                carregarEvento()
            }
    }

    private fun verificarFavorito() {
        favRef.get()
            .addOnSuccessListener { documento ->
                favoritado = documento.exists()
                atualizarBotaoFavorito()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao verificar favorito: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun atualizarBotaoFavorito() {
        btnFavoritar.text = if (favoritado) "Remover dos favoritos" else "Favoritar"
        btnFavoritar.isEnabled = true
    }

    private fun alternarFavorito() {
        btnFavoritar.isEnabled = false

        if (favoritado) {
            favRef.delete()
                .addOnSuccessListener {
                    favoritado = false
                    atualizarBotaoFavorito()
                    Toast.makeText(this, "Removido dos favoritos", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { erro ->
                    btnFavoritar.isEnabled = true
                    Toast.makeText(this, "Erro ao remover: ${erro.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            favRef.set(
                hashMapOf(
                    "userId" to uid,
                    "eventId" to eventId,
                    "dataFavorito" to FieldValue.serverTimestamp()
                )
            )
                .addOnSuccessListener {
                    favoritado = true
                    atualizarBotaoFavorito()
                    Toast.makeText(this, "Adicionado aos favoritos", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { erro ->
                    btnFavoritar.isEnabled = true
                    Toast.makeText(this, "Erro ao favoritar: ${erro.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun configurarComentarios() {
        recyclerComentarios = findViewById(R.id.recyclerComentarios)
        editComentario = findViewById(R.id.editComentario)
        btnComentar = findViewById(R.id.btnComentar)
        tvNenhumComentario = findViewById(R.id.tvNenhumComentario)

        comentariosAdapter = CommentAdapter(
            uid,
            { c -> editarComentario(c) },
            { c -> excluirComentario(c) }
        )
        recyclerComentarios.layoutManager = LinearLayoutManager(this)
        recyclerComentarios.adapter = comentariosAdapter

        db.collection("users").document(uid).get()
            .addOnSuccessListener {
                nomeUsuario = it.getString("nome") ?: "Aluno"
                organizador = UserRole.isOrganizador(it.getString("tipo"))
                eventoAtual?.let { evento -> atualizarBotao(evento) }
            }

        btnComentar.setOnClickListener { publicarComentario() }
        carregarComentarios()
    }

    private fun carregarComentarios() {
        db.collection("comments")
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnSuccessListener { resultado ->
                val lista = resultado.toObjects(Comment::class.java)
                    .sortedByDescending { it.data?.seconds ?: Long.MAX_VALUE }
                comentariosAdapter.atualizar(lista)
                tvNenhumComentario.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao carregar comentários: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun publicarComentario() {
        val texto = editComentario.text.toString().trim()
        val erro = Validation.comment(texto)
        if (erro != null) {
            Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
            return
        }

        btnComentar.isEnabled = false

        db.collection("comments").add(
            hashMapOf(
                "eventId" to eventId,
                "autorId" to uid,
                "autorNome" to nomeUsuario,
                "texto" to texto,
                "data" to FieldValue.serverTimestamp()
            )
        )
            .addOnSuccessListener {
                editComentario.text.clear()
                btnComentar.isEnabled = true
                carregarComentarios()
            }
            .addOnFailureListener { erro ->
                btnComentar.isEnabled = true
                Toast.makeText(this, "Erro ao comentar: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun editarComentario(c: Comment) {
        if (c.autorId != uid) return

        val campo = EditText(this)
        campo.setText(c.texto)

        AlertDialog.Builder(this)
            .setTitle("Editar comentário")
            .setView(campo)
            .setPositiveButton("Salvar") { _, _ ->
                val novo = campo.text.toString().trim()
                val erro = Validation.commentEdit(novo)
                if (erro != null) {
                    Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                db.collection("comments").document(c.id).update("texto", novo)
                    .addOnSuccessListener { carregarComentarios() }
                    .addOnFailureListener { erro ->
                        Toast.makeText(this, "Erro ao editar: ${erro.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirComentario(c: Comment) {
        if (c.autorId != uid) return

        AlertDialog.Builder(this)
            .setTitle("Excluir comentário")
            .setMessage("Deseja excluir este comentário?")
            .setPositiveButton("Excluir") { _, _ ->
                db.collection("comments").document(c.id).delete()
                    .addOnSuccessListener { carregarComentarios() }
                    .addOnFailureListener { erro ->
                        Toast.makeText(this, "Erro ao excluir: ${erro.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    private fun configurarPublicacoes() {

        postRepository = PostRepository(db)

        tvNenhumaPublicacao =
            findViewById(R.id.tvNenhumaPublicacao)

        btnTirarFoto = findViewById(R.id.btnTirarFoto)
        btnFotoGaleria = findViewById(R.id.btnFotoGaleria)

        // Botão para abrir a câmera
        btnTirarFoto.setOnClickListener {
            abrirCamera()
        }

        // Botão para escolher foto da galeria
        btnFotoGaleria.setOnClickListener {
            escolherFoto.launch("image/*")
        }


        btnGravarAudio = findViewById(R.id.btnGravarAudio)
        btnCancelarAudio = findViewById(R.id.btnCancelarAudio)

        btnGravarAudio.setOnClickListener {
            alternarGravacaoAudio()
        }

        btnCancelarAudio.setOnClickListener {
            finalizarGravacaoAudio(false)
        }

        atualizarBotoesAudio()


        btnGravarVideo = findViewById(R.id.btnGravarVideo)
        btnVideoGaleria = findViewById(R.id.btnVideoGaleria)

        btnGravarVideo.setOnClickListener {
            abrirCameraVideo()
        }

        btnVideoGaleria.setOnClickListener {
            escolherVideo.launch("video/*")
        }

        atualizarBotoesVideo()



        postAdapter = PostAdapter(
            uid,
            { post -> excluirPost(post) },
            { post, container ->
                mostrarConteudoPost(post, container)
            }
        )

        val recycler =
            findViewById<RecyclerView>(R.id.recyclerPublicacoes)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = postAdapter

        carregarPublicacoes()
    }


    private fun carregarPublicacoes() {
        db.collection("posts")
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnSuccessListener { resultado ->
                val lista = resultado.toObjects(Post::class.java)
                    .sortedByDescending { it.data?.seconds ?: Long.MAX_VALUE }

                pararVideoAtivo()

                postAdapter.atualizar(lista)
                tvNenhumaPublicacao.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao carregar publicações: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }


    private fun mostrarConteudoPost(
        post: Post,
        container: FrameLayout
    ) {

        container.removeAllViews()


        if (post.tipo == PostType.VIDEO) {

            val arquivo = arquivoVideoLocal(post.caminho)

            if (arquivo == null) {

                val aviso = TextView(this).apply {
                    text = "Vídeo disponível apenas no aparelho do autor"
                }

                container.addView(aviso)
                return
            }

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val video = VideoView(this).apply {

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (220 * resources.displayMetrics.density).toInt()
                )

                visibility = View.GONE
            }

            val btnAssistir = Button(this).apply {
                text = "Assistir vídeo"
            }

            btnAssistir.setOnClickListener {

                if (video.visibility != View.VISIBLE) {

                    pararVideoAtivo()

                    videoAtivo = video
                    botaoVideoAtivo = btnAssistir

                    video.visibility = View.VISIBLE

                    val controles = MediaController(this)

                    controles.setAnchorView(video)
                    video.setMediaController(controles)

                    video.setOnPreparedListener {

                        video.start()
                        controles.show(3000)
                        btnAssistir.text = "Pausar vídeo"
                    }

                    video.setOnCompletionListener {
                        btnAssistir.text = "Reproduzir novamente"
                    }

                    video.setOnErrorListener { _, _, _ ->
                        btnAssistir.text = "Erro ao reproduzir"

                        Toast.makeText(
                            this,
                            "Não foi possível reproduzir o vídeo",
                            Toast.LENGTH_SHORT
                        ).show()

                        true
                    }

                    video.setVideoPath(arquivo.absolutePath)

                } else if (video.isPlaying) {

                    video.pause()
                    btnAssistir.text = "Continuar vídeo"

                } else {

                    video.start()
                    btnAssistir.text = "Pausar vídeo"
                }
            }

            layout.addView(video)
            layout.addView(btnAssistir)

            container.addView(layout)

            return
        }



        if (post.tipo == PostType.AUDIO) {

            val arquivo = arquivoAudioLocal(post.caminho)

            if (arquivo != null) {

                val controles = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                val btnReproduzir = Button(this).apply {
                    text = "Reproduzir"

                    setOnClickListener {
                        reproduzirAudio(post)
                    }
                }

                val btnParar = Button(this).apply {
                    text = "Parar"

                    setOnClickListener {
                        pararReproducaoAudio()
                    }
                }

                controles.addView(btnReproduzir)
                controles.addView(btnParar)

                container.addView(controles)

            } else {

                val aviso = TextView(this).apply {
                    text = "Áudio disponível apenas no aparelho do autor"
                }

                container.addView(aviso)
            }

            return
        }


        if (post.tipo == PostType.FOTO) {

            val bitmap = ImageUtils.toBitmap(
                post.imagem,
                600
            )

            if (bitmap != null) {

                val imagem = ImageView(this)

                val altura = (
                        220 * resources.displayMetrics.density
                        ).toInt()

                imagem.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    altura
                )

                imagem.scaleType =
                    ImageView.ScaleType.CENTER_CROP

                imagem.setImageBitmap(bitmap)

                container.addView(imagem)
                return
            }
        }

        // Mensagem temporária para outros tipos de publicação
        val aviso = TextView(this)

        aviso.text = when {
            post.tipo == PostType.FOTO ->
                "Foto indisponível"

            post.caminho.isNotBlank() &&
                    File(post.caminho).exists() ->
                "${Posts.rotulo(post.tipo)} salvo neste aparelho"

            else ->
                "${Posts.rotulo(post.tipo)} disponível só no aparelho do autor"
        }

        container.addView(aviso)
    }



    private fun publicarPost(tipo: String, caminho: String) {

        val erro = Posts.validate(tipo, caminho)

        if (erro != null) {
            Toast.makeText(
                this,
                erro,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        postRepository.publish(
            eventId,
            tipo,
            uid,
            nomeUsuario,
            caminho
        )
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Publicação realizada!",
                    Toast.LENGTH_SHORT
                ).show()

                carregarPublicacoes()
            }
            .addOnFailureListener { erro2 ->

                if (tipo == PostType.AUDIO) {
                    arquivoAudioLocal(caminho)?.delete()
                }

                Toast.makeText(
                    this,
                    "Erro ao publicar: ${erro2.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    private fun excluirPost(post: Post) {
        if (!Posts.podeExcluir(post.autorId, uid)) return

        AlertDialog.Builder(this)
            .setTitle("Excluir publicação")
            .setMessage("Deseja excluir esta publicação?")
            .setPositiveButton("Excluir") { _, _ ->
                postRepository.delete(post)
                    .addOnSuccessListener { carregarPublicacoes() }
                    .addOnFailureListener { erro ->
                        Toast.makeText(this, "Erro ao excluir: ${erro.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun configurarAvaliacao() {
        layoutAvaliacao = findViewById(R.id.layoutAvaliacao)
        ratingBarAvaliacao = findViewById(R.id.ratingBarAvaliacao)
        btnAvaliar = findViewById(R.id.btnAvaliar)
        tvMediaDetalhe = findViewById(R.id.tvMediaDetalhe)

        ratingRef = db.collection("ratings").document("${uid}_$eventId")

        btnAvaliar.setOnClickListener { enviarAvaliacao() }
        carregarMedia()
    }

    // Só aparece pra aluno inscrito em evento já encerrado
    private fun atualizarAvaliacao(evento: Event) {
        if (inscrito && evento.estaEncerrado()) {
            layoutAvaliacao.visibility = View.VISIBLE
            carregarMinhaNota()
        } else {
            layoutAvaliacao.visibility = View.GONE
        }
    }

    private fun carregarMinhaNota() {
        ratingRef.get()
            .addOnSuccessListener { documento ->
                if (documento.exists()) {
                    ratingBarAvaliacao.rating = (documento.getLong("nota") ?: 0L).toFloat()
                    btnAvaliar.text = "Atualizar avaliação"
                } else {
                    btnAvaliar.text = "Enviar avaliação"
                }
            }
    }

    private fun enviarAvaliacao() {
        val nota = ratingBarAvaliacao.rating.toInt()
        val erro = Validation.rating(nota)
        if (erro != null) {
            Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
            return
        }

        btnAvaliar.isEnabled = false

        ratingRef.set(
            hashMapOf(
                "userId" to uid,
                "eventId" to eventId,
                "nota" to nota,
                "data" to FieldValue.serverTimestamp()
            )
        )
            .addOnSuccessListener {
                btnAvaliar.isEnabled = true
                btnAvaliar.text = "Atualizar avaliação"
                Toast.makeText(this, "Avaliação salva!", Toast.LENGTH_SHORT).show()
                carregarMedia()
            }
            .addOnFailureListener { erro ->
                btnAvaliar.isEnabled = true
                Toast.makeText(this, "Erro ao avaliar: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun carregarMedia() {
        db.collection("ratings")
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnSuccessListener { resultado ->
                val notas = resultado.documents.mapNotNull { it.getLong("nota") }
                tvMediaDetalhe.text = Formatters.averageText(Ratings.average(notas), notas.size)
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao carregar média: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun configurarMapa() {
        mapaEvento = findViewById(R.id.mapaEvento)
        tvEnderecoDetalhe = findViewById(R.id.tvEnderecoDetalhe)

        mapaEvento.setTileSource(TileSourceFactory.MAPNIK)
        mapaEvento.setMultiTouchControls(true)
        // Deixa mexer no mapa sem a tela rolar junto
        mapaEvento.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
    }

    private fun mostrarLocalizacao(evento: Event) {
        tvEnderecoDetalhe.text = "Endereço: " + evento.endereco.ifEmpty { evento.local }

        val lat = evento.latitude
        val lng = evento.longitude

        if (lat == null || lng == null) {
            mapaEvento.visibility = View.GONE
            return
        }

        mapaEvento.visibility = View.VISIBLE

        val ponto = GeoPoint(lat, lng)
        val marcador = Marker(mapaEvento)
        marcador.position = ponto
        marcador.title = evento.titulo
        marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        mapaEvento.overlays.clear()
        mapaEvento.overlays.add(marcador)
        mapaEvento.controller.setZoom(17.0)
        mapaEvento.controller.setCenter(ponto)
        mapaEvento.invalidate()
    }

    private fun confirmarEncerramento() {
        AlertDialog.Builder(this)
            .setTitle("Encerrar evento")
            .setMessage("Depois de encerrado, ninguém mais pode se inscrever. Deseja continuar?")
            .setPositiveButton("Encerrar") { _, _ ->
                eventRef.update("status", EventStatus.ENCERRADO)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Evento encerrado", Toast.LENGTH_SHORT).show()
                        carregarEvento()
                    }
                    .addOnFailureListener { erro ->
                        Toast.makeText(this, "Erro ao encerrar: ${erro.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (::mapaEvento.isInitialized) mapaEvento.onResume()
        if (eventId.isNotEmpty()) carregarEvento()
    }

    override fun onPause() {
        super.onPause()
        if (::mapaEvento.isInitialized) mapaEvento.onPause()
    }


    private fun abrirCamera() {

        try {
            val pasta = File(cacheDir, "capturas")
            pasta.mkdirs()

            val arquivo = File.createTempFile(
                "foto_",
                ".jpg",
                pasta
            )

            arquivoCamera = arquivo

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                arquivo
            )

            tirarFoto.launch(uri)

        } catch (e: Exception) {

            arquivoCamera?.delete()
            arquivoCamera = null

            Toast.makeText(
                this,
                "Erro ao abrir câmera: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun publicarFoto(uri: Uri) {

        val imagem = try {
            ImageUtils.toBase64(this, uri)
        } catch (e: Exception) {
            null
        }

        if (imagem.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Não foi possível processar a imagem",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        btnTirarFoto.isEnabled = false
        btnFotoGaleria.isEnabled = false

        db.collection("users").document(uid).get()
            .addOnSuccessListener { documento ->

                val autor = documento.getString("nome")
                    ?.takeIf { it.isNotBlank() }
                    ?: nomeUsuario

                postRepository.publishPhoto(
                    eventId,
                    uid,
                    autor,
                    imagem
                ).addOnCompleteListener { tarefa ->

                    btnTirarFoto.isEnabled = true
                    btnFotoGaleria.isEnabled = true

                    if (tarefa.isSuccessful) {

                        Toast.makeText(
                            this,
                            "Foto publicada!",
                            Toast.LENGTH_SHORT
                        ).show()

                        carregarPublicacoes()

                    } else {

                        Toast.makeText(
                            this,
                            "Erro ao publicar: ${tarefa.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .addOnFailureListener { erro ->

                btnTirarFoto.isEnabled = true
                btnFotoGaleria.isEnabled = true

                Toast.makeText(
                    this,
                    "Erro ao carregar autor: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
            "arquivoCamera",
            arquivoCamera?.absolutePath
        )

        outState.putString(
            "arquivoVideoCamera",
            arquivoVideoCamera?.absolutePath
        )
        super.onSaveInstanceState(outState)
    }


    private fun atualizarBotoesAudio() {

        btnGravarAudio.text = if (gravandoAudio) {
            "Parar e publicar"
        } else {
            "Gravar áudio"
        }

        btnCancelarAudio.visibility = if (gravandoAudio) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }


    private fun arquivoAudioLocal(caminho: String): File? {

        if (caminho.isBlank()) return null

        return runCatching {

            val pasta = File(
                filesDir,
                "audios"
            ).canonicalFile

            val arquivo = File(caminho).canonicalFile

            arquivo.takeIf {
                it.parentFile == pasta && it.isFile
            }

        }.getOrNull()
    }


    private fun reproduzirAudio(post: Post) {

        val arquivo = arquivoAudioLocal(post.caminho)

        if (arquivo == null) {
            Toast.makeText(
                this,
                "Áudio não disponível neste aparelho",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        pararReproducaoAudio()

        val player = MediaPlayer()

        try {

            FileInputStream(arquivo).use { entrada ->
                player.setDataSource(entrada.fd)
                player.prepare()
            }

            player.setOnCompletionListener {
                pararReproducaoAudio()
            }

            playerAudio = player
            player.start()

        } catch (e: Exception) {

            playerAudio = null
            player.release()

            Toast.makeText(
                this,
                "Erro ao reproduzir áudio: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun pararReproducaoAudio() {

        playerAudio?.release()
        playerAudio = null
    }

    // Selecionar vídeo existente
    private val escolherVideo = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->

        if (uri != null) {
            importarVideo(uri)
        }
    }

    // Gravar vídeo com a câmera
    private val capturarVideo = registerForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { sucesso ->

        val arquivo = arquivoVideoCamera
        arquivoVideoCamera = null

        if (sucesso && arquivo != null &&
            arquivo.exists() && arquivo.length() > 0L) {

            importarVideo(
                Uri.fromFile(arquivo),
                arquivo
            )

        } else {

            arquivo?.delete()

            Toast.makeText(
                this,
                "Gravação cancelada ou inválida",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun abrirCameraVideo() {

        if (processandoVideo) return

        try {
            val pasta = File(cacheDir, "capturas")

            if (!pasta.exists() && !pasta.mkdirs()) {
                throw IOException(
                    "Não foi possível criar a pasta temporária"
                )
            }

            val arquivo = File.createTempFile(
                "video_",
                ".mp4",
                pasta
            )

            arquivoVideoCamera = arquivo

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                arquivo
            )

            capturarVideo.launch(uri)

        } catch (e: Exception) {

            arquivoVideoCamera?.delete()
            arquivoVideoCamera = null

            Toast.makeText(
                this,
                "Erro ao abrir câmera: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun importarVideo(
        uri: Uri,
        temporario: File? = null
    ) {

        if (processandoVideo) {
            temporario?.delete()
            return
        }

        processandoVideo = true
        atualizarBotoesVideo()

        Thread {

            var destino: File? = null

            try {
                val pasta = File(filesDir, "videos")

                if (!pasta.exists() && !pasta.mkdirs()) {
                    throw IOException(
                        "Não foi possível criar a pasta de vídeos"
                    )
                }

                val arquivo = File.createTempFile(
                    "video_${uid}_",
                    ".mp4",
                    pasta
                )

                destino = arquivo

                val limiteBytes = 30L * 1024 * 1024

                val entrada = contentResolver.openInputStream(uri)
                    ?: throw IOException("Vídeo não encontrado")

                entrada.use { origem ->
                    arquivo.outputStream().use { saida ->

                        val buffer = ByteArray(8192)
                        var total = 0L

                        while (true) {
                            val lidos = origem.read(buffer)

                            if (lidos == -1) break

                            total += lidos

                            if (total > limiteBytes) {
                                throw IOException(
                                    "O vídeo deve ter no máximo 30 MB"
                                )
                            }

                            saida.write(buffer, 0, lidos)
                        }
                    }
                }

                if (arquivo.length() == 0L) {
                    throw IOException("Vídeo vazio")
                }

                // Verificar duração
                val retriever = MediaMetadataRetriever()

                val duracao = try {
                    retriever.setDataSource(
                        arquivo.absolutePath
                    )

                    retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )?.toLongOrNull()

                } finally {
                    retriever.release()
                }

                if (duracao == null ||
                    duracao <= 0L ||
                    duracao > 30_000L) {

                    throw IOException(
                        "O vídeo deve ter no máximo 30 segundos"
                    )
                }

                runOnUiThread {

                    if (isFinishing || isDestroyed) {
                        arquivo.delete()
                        processandoVideo = false
                        atualizarBotoesVideo()
                        return@runOnUiThread
                    }

                    postRepository.publish(
                        eventId,
                        PostType.VIDEO,
                        uid,
                        nomeUsuario,
                        arquivo.absolutePath
                    )
                        .addOnSuccessListener {

                            processandoVideo = false
                            atualizarBotoesVideo()

                            Toast.makeText(
                                this,
                                "Vídeo publicado!",
                                Toast.LENGTH_SHORT
                            ).show()

                            carregarPublicacoes()
                        }
                        .addOnFailureListener { erro ->

                            arquivo.delete()

                            processandoVideo = false
                            atualizarBotoesVideo()

                            Toast.makeText(
                                this,
                                "Erro ao publicar: ${erro.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }

            } catch (e: Exception) {

                destino?.delete()

                runOnUiThread {

                    processandoVideo = false
                    atualizarBotoesVideo()

                    Toast.makeText(
                        this,
                        "Erro no vídeo: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {
                temporario?.delete()
            }

        }.start()
    }


    private fun atualizarBotoesVideo() {

        btnGravarVideo.isEnabled = !processandoVideo
        btnVideoGaleria.isEnabled = !processandoVideo

    }


    private fun arquivoVideoLocal(caminho: String): File? {

        if (caminho.isBlank()) return null

        return runCatching {

            val pasta = File(
                filesDir,
                "videos"
            ).canonicalFile

            val arquivo = File(caminho).canonicalFile

            arquivo.takeIf {
                it.parentFile == pasta && it.isFile
            }

        }.getOrNull()
    }


    private fun pararVideoAtivo() {

        videoAtivo?.stopPlayback()
        videoAtivo?.visibility = View.GONE

        botaoVideoAtivo?.text = "Assistir vídeo"

        videoAtivo = null
        botaoVideoAtivo = null
    }

    override fun onStop() {
        if (gravandoAudio) {
            finalizarGravacaoAudio(false)
        }

        pararReproducaoAudio()
        pararVideoAtivo()

        super.onStop()
    }


}