package com.example.campushub

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Apoio dos testes: liga o app nos emuladores, limpa os dados e cria usuários
object TestEnv {
    private const val HOST = "10.0.2.2"
    private const val PROJECT = "campushub-d1b64"
    const val SENHA = "123456"
    private var conectado = false

    val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    // Aponta o Firebase do app pros emuladores (só pode rodar uma vez por processo)
    @Synchronized
    fun conectar() {
        if (conectado) return
        db.useEmulator(HOST, 8080)
        auth.useEmulator(HOST, 9099)
        conectado = true
    }

    // Apaga todos os dados e contas dos emuladores (chamar antes de cada teste)
    fun limpar() {
        auth.signOut()
        requisicao("DELETE", "http://$HOST:8080/emulator/v1/projects/$PROJECT/databases/(default)/documents")
        requisicao("DELETE", "http://$HOST:9099/emulator/v1/projects/$PROJECT/accounts")
    }

    // Cria a conta no Auth (já fica logada) e o documento dela em users. Devolve o uid
    fun criarUsuario(email: String, nome: String, tipo: String = "aluno"): String {
        val uid = Tasks.await(auth.createUserWithEmailAndPassword(email, SENHA)).user!!.uid
        val dados = mapOf("nome" to nome, "email" to email, "curso" to "Ciência da Computação", "tipo" to tipo)
        if (tipo == "aluno") {
            Tasks.await(db.collection("users").document(uid).set(dados))
        } else {
            // as regras não deixam criar organizador pelo app, então vai direto pelo emulador
            criarDocAdmin("users", uid, dados)
        }
        return uid
    }

    fun entrar(email: String) {
        Tasks.await(auth.signInWithEmailAndPassword(email, SENHA))
    }

    // Cria um documento ignorando as regras de segurança (usado pra montar o cenário do teste)
    fun criarDocAdmin(colecao: String, id: String, campos: Map<String, Any>) {
        val fields = JSONObject()
        campos.forEach { (chave, valor) -> fields.put(chave, valorFirestore(valor)) }
        val corpo = JSONObject().put("fields", fields).toString()
        requisicao(
            "POST",
            "http://$HOST:8080/v1/projects/$PROJECT/databases/(default)/documents/$colecao?documentId=$id",
            corpo,
            "owner"
        )
    }

    private fun valorFirestore(v: Any): JSONObject = when (v) {
        is String -> JSONObject().put("stringValue", v)
        is Int, is Long -> JSONObject().put("integerValue", v.toString())
        is Boolean -> JSONObject().put("booleanValue", v)
        is Date -> {
            val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            formato.timeZone = TimeZone.getTimeZone("UTC")
            JSONObject().put("timestampValue", formato.format(v))
        }
        else -> error("Tipo não suportado: $v")
    }

    private fun requisicao(metodo: String, url: String, corpo: String? = null, token: String? = null) {
        val con = URL(url).openConnection() as HttpURLConnection
        con.requestMethod = metodo
        if (token != null) con.setRequestProperty("Authorization", "Bearer $token")
        if (corpo != null) {
            con.setRequestProperty("Content-Type", "application/json")
            con.doOutput = true
            con.outputStream.use { it.write(corpo.toByteArray()) }
        }
        val codigo = con.responseCode
        val resposta = (if (codigo in 200..299) con.inputStream else con.errorStream)
            ?.bufferedReader()?.readText() ?: ""
        con.disconnect()
        check(codigo in 200..299) { "HTTP $codigo em $url: $resposta" }
    }
}