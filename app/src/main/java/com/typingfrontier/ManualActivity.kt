package com.typingfrontier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ManualActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual)

        findViewById<Button>(R.id.btnFecharManual).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvManual)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ManualAdapter(getManualTopics())
    }

    private fun getManualTopics(): List<ManualTopic> {
        return listOf(
            ManualTopic("🎯 Introdução", "Bem-vindo ao TypingFrontier! Este é um simulador de vida e RPG focado em evolução pessoal através da digitação. Aqui, suas escolhas e sua dedicação definem o seu sucesso."),
            
            ManualTopic("📊 Status do Personagem", "Seu personagem possui diversos atributos que influenciam seu desempenho:\n\n• Força: Ajuda em tarefas físicas e exploração.\n• Resistência: Reduz o cansaço e dano recebido.\n• Velocidade: Aumenta a eficiência e sucesso em fugas.\n• Inteligência: Melhora o salário e habilidades mentais.\n• Carisma: Melhora recompensas sociais e diálogos."),
            
            ManualTopic("❤️ Vida (HP)", "Sua saúde física. Se chegar a zero, você desmaia e é hospitalizado. Dormir bem ou comer ajuda a manter sua vida estável. Cuidado com explorações perigosas!"),

            ManualTopic("🩹 Traumas e Colapso", "Hospitalizações sucessivas sem descanso geram traumas no corpo:\n\n• Limite: Calculado pela sua Vida e Resistência. Quanto mais forte, mais traumas você aguenta.\n• Trauma Leve: Perda de XP e lucro, mas sem perda de nível.\n• Estado Crítico (Colapso): Ocorre ao atingir seu limite de traumas. Gera perda de nível e sequelas nos atributos.\n• Recuperação: Cada trauma exige 2 noites de sono em casa para ser curado."),
            
            ManualTopic("⚡ Energia", "A energia é o seu fôlego para o dia. Quase todas as ações consomem energia. Se ela acabar, você não poderá mais trabalhar ou treinar até comer ou dormir."),
            
            ManualTopic("🧠 Energia Mental", "Reflete sua concentração. É um recurso mais limitado que a energia física. Estudar, trabalhar e treinar consomem esta energia. Se acabar, você precisará de uma pausa ou uma boa noite de sono."),
            
            ManualTopic("💼 Trabalho", "Sua fonte primária de sustento. Trabalhar gera dinheiro baseado no seu nível e atributos mentais. O trabalho serve para pagar as contas e financiar sua evolução. Você pode trabalhar apenas uma vez por dia."),
            
            ManualTopic("🥪 Alimentação", "Essencial para recuperar energia física durante o dia. Cada refeição custa dinheiro e ajuda você a continuar produzindo sem precisar dormir cedo."),
            
            ManualTopic("🧘 Descanso (Pausa)", "Uma pausa rápida ajuda a recuperar um pouco de energia mental sem gastar dinheiro. Disponível apenas uma vez por dia."),
            
            ManualTopic("😴 Dormir", "A ação mais importante para fechar o ciclo. Dormir reseta sua energia e mente, e restaura sua capacidade de trabalhar. Exige o pagamento do aluguel diário. Se não tiver dinheiro, você dormirá na rua com penalidades."),
            
            ManualTopic("🧠 Treino Mental", "O pilar do jogo! Pratique português e matemática para subir Inteligência e Carisma. É barato e incentiva o aprendizado constante. Disponível a qualquer hora, mas estudar durante a madrugada consome muito mais recursos do seu personagem."),
            
            ManualTopic("🏋️ Treino Físico", "Melhora Força, Resistência e Velocidade. Cada sessão consome tempo, energia física e mental.\n\n• Tempo: Entre 30min e 1h por treino.\n• Risco de Falha: Treinos possuem chance de erro. Você ganha +1% de chance de sucesso a cada 3 níveis de personagem.\n• Exaustão: Se a Energia Mental estiver abaixo de 20%, o risco de falha aumenta consideravelmente."),

            ManualTopic("🌍 Exploração", "Aventure-se por São Paulo! Ganhe muito dinheiro e XP arriscando-se em zonas desconhecidas.\n\n• Limites: Máximo de 5 avanços (etapas) por expedição.\n• Tempo: Cada etapa consome 1 hora e 15 minutos do seu dia.\n• Recompensas: Quanto mais fundo você for, maior o multiplicador de recompensa (até 4.5x na 5ª etapa).\n• Fuga: Você pode sair a qualquer momento com o que coletou."),

            ManualTopic("📍 Requisitos de Exploração", "Recomendações de nível de atributo para ter boas chances de sucesso. Atributos mentais são essenciais, enquanto físicos servem como apoio tático:\n\n" +
                "• Parque da Cidade\n  - Foco: Inteligência Lv.1+\n  - Apoio: Velocidade Lv.1+\n\n" +
                "• Centro Comercial\n  - Foco: Carisma Lv.6+\n  - Apoio: Força Lv.5+\n\n" +
                "• Subúrbio Industrial\n  - Foco: Inteligência Lv.20+\n  - Apoio: Resistência Lv.15+\n\n" +
                "• Beco Escuro\n  - Foco: Inteligência Lv.35+\n  - Apoio: Força Lv.25+\n\n" +
                "• Lab Abandonado\n  - Foco: Inteligência Lv.45+\n  - Apoio Secundário: Resistência Lv.35+\n  - Apoio Terciário: Velocidade Lv.30+\n\n" +
                "• Cassino Clandestino\n  - Foco: Carisma Lv.55+\n  - Apoio Secundário: Força Lv.45+\n  - Apoio Terciário: Velocidade Lv.40+\n\n" +
                "• Esgotos Profundos\n  - Foco: Inteligência Lv.65+\n  - Apoio Secundário: Velocidade Lv.55+\n  - Apoio Terciário: Resistência Lv.50+"),
            
            ManualTopic("🎒 Equipamentos", "Itens permanentes que dão bônus massivos. Você pode trocar seu item atual na loja recebendo um crédito de 40% do valor do antigo."),
            
            ManualTopic("🕊️ Benções (Blessings)", "Proteção divina que impede a perda de Nível e Atributos caso você desmaie. Sempre compre uma antes de ir para lugares perigosos!"),
            
            ManualTopic("💰 Economia", "O jogo equilibra ganhos e gastos. O trabalho garante o sustento (aluguel e comida), enquanto a Exploração e Missões são os caminhos para a verdadeira riqueza."),
            
            ManualTopic("👥 Profissões", 
                "• Policial: Especialista em Força. Ganha mais vida ao subir nível. Perfeito para exploração.\n" +
                "• Médico: Mestre da Inteligência. Ganha mais mente ao evoluir. Evolui atributos rápido.\n" +
                "• Engenheiro: Focado em Resistência e Mente. Ótima gestão de energia.\n" +
                "• Professor: Líder em Carisma. Ganha bônus de salário mais facilmente.\n" +
                "• Detetive: Equilibrado entre Velocidade e Inteligência. Ótimo para achar pistas."),
            
            ManualTopic("💡 Dicas para Iniciantes", "1. Treine sua mente cedo para aumentar seu salário.\n2. Sempre guarde dinheiro para o aluguel.\n3. Não explore áreas difíceis sem uma Benção.\n4. Use a Pausa estratégica após o trabalho para recuperar sua concentração.")
        )
    }

    data class ManualTopic(val title: String, val content: String)

    private class ManualAdapter(private val topics: List<ManualTopic>) : RecyclerView.Adapter<ManualAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val t: TextView = v.findViewById(R.id.txtTopicTitle)
            val c: TextView = v.findViewById(R.id.txtTopicContent)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_manual_topic, p, false))
        override fun onBindViewHolder(h: VH, p: Int) {
            h.t.text = topics[p].title
            h.c.text = topics[p].content
        }
        override fun getItemCount() = topics.size
    }
}
