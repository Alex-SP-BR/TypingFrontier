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
            ManualTopic("🎯 Introdução", "Bem-vindo ao Typing Frontier! Explore um mundo onde sua velocidade de digitação e suas escolhas moldam seu futuro. Evolua seu personagem, gerencie sua vida e conquiste a cidade!"),
            
            ManualTopic("📊 Atributos", "Cada atributo influencia seu desempenho de forma única:\n\n• Força: Essencial para tarefas físicas e segurança na exploração.\n• Resistência: Diminui o consumo de Energia e permite aguentar mais traumas antes de um colapso.\n• Velocidade: Aumenta sua eficiência e as chances de sucesso em fugas.\n• Inteligência: Melhora seu salário e habilidades de aprendizado.\n• Carisma: Melhora seu impacto social e negociações."),
            
            ManualTopic("❤️ Vida (HP)", "Sua saúde física. Se sua Vida chegar a zero, você desmaia e precisará ser hospitalizado. Mantenha sua saúde em dia com uma boa alimentação e noites de sono tranquilo."),

            ManualTopic("🩹 Traumas e Colapso", "Se for hospitalizado muitas vezes seguidas sem descansar, seu corpo acumulará traumas:\n\n• Limite de Traumas: Depende de quanta Vida e Resistência você possui. Quanto mais forte você for, mais traumas aguentará.\n• Trauma Leve: Causa perda de Experiência e dos lucros da exploração atual.\n• Colapso Corporal: Ocorre ao atingir seu limite de traumas. Você perde um nível de personagem e alguns atributos podem sofrer reduções permanentes.\n• Recuperação: Para curar seus traumas, basta ter noites de sono normal em sua casa."),
            
            ManualTopic("⚡ Energia", "Representa seu vigor físico para o dia a dia. Quase todas as ações gastam Energia. Se você ficar exausto, não poderá trabalhar ou treinar até comer algo ou dormir. Se tentar agir sem Energia, o jogo sugerirá um lanche rápido."),
            
            ManualTopic("🧠 Energia Mental", "Representa seu foco para tarefas intelectuais. Estudar e trabalhar consomem Energia Mental. Ela é mais limitada que a física; se ela acabar, você precisará de uma pausa estratégica ou de uma noite de sono."),
            
            ManualTopic("💼 Trabalho", "Sua principal fonte de renda. O salário aumenta conforme você sobe de nível e melhora sua Inteligência. Você pode trabalhar normalmente uma vez por dia, e o expediente termina às 22h. Lembre-se: trabalhar exige Energia e Energia Mental."),
            
            ManualTopic("⏱️ Hora Extra", "Precisa de mais Frons? Após o turno normal, você pode realizar jornadas extras assistindo a anúncios. \n\nFique atento: quanto mais você trabalha extra no mesmo dia, mais Energia e Energia Mental são consumidas, e menos dinheiro você ganha. É uma atividade exclusivamente financeira e não gera Experiência."),

            ManualTopic("🥪 Alimentação", "Essencial para recuperar Energia durante o dia. Cada refeição custa Frons. Planeje-se bem, pois as lanchonetes fecham rigorosamente às 22h."),
            
            ManualTopic("🧘 Pausa (Descanso)", "Uma pausa rápida ajuda a recuperar um pouco de Energia Mental sem gastar Frons ou tempo. Você pode fazer uma pausa por dia."),
            
            ManualTopic("😴 Dormir", "A hora de recarregar! Dormir recupera toda sua Energia e Energia Mental. Você precisa pagar o aluguel do dia para dormir bem. Se não tiver dinheiro, terá que dormir na rua, o que prejudica sua Vida. \n\nDica: se você ainda tiver disposição, o jogo recomenda estudar um pouco mais antes de dormir."),
            
            ManualTopic("🧠 Treino Mental", "Estude Português e Matemática para ficar mais inteligente e carismático. É um investimento barato e essencial para sua evolução. Você pode estudar até de madrugada, mas o esforço de Energia e Energia Mental será muito maior fora do horário comercial."),
            
            ManualTopic("🏋️ Treino Físico", "Melhore sua Força, Resistência e Velocidade na academia ou no parque. Cada sessão consome tempo e recursos. Fique atento: o treino físico é bloqueado após as 22h e o risco de lesão aumenta se você estiver com a Energia Mental baixa."),

            ManualTopic("🌍 Exploração", "Aventure-se em zonas perigosas para ganhar grandes quantidades de Frons e Experiência. \n\nCada expedição tem 5 etapas. Quanto mais longe você for, maior será o prêmio! Mas cuidado: após as 18h o risco aumenta, embora as recompensas também sejam 50% maiores. Você pode fugir a qualquer momento com o que coletou."),

            ManualTopic("📍 Dicas de Exploração", "Cada local exige habilidades diferentes para o sucesso. O sistema avalia seu atributo principal e usa os secundários como apoio tático:\n\n" +
                "• Parque da Cidade: Foco em Inteligência.\n" +
                "• Centro Comercial: Foco em Carisma.\n" +
                "• Subúrbio Industrial: Foco em Inteligência e Resistência.\n" +
                "• Beco Escuro: Foco em Inteligência e Força.\n" +
                "• Lab Abandonado: Exige alta Inteligência e Resistência.\n" +
                "• Cassino Clandestino: Exige alto Carisma e Força.\n" +
                "• Esgotos Profundos: O desafio máximo de Inteligência e Velocidade."),
            
            ManualTopic("🎒 Equipamentos", "Itens que aumentam seus atributos permanentemente. Se quiser algo melhor, pode trocar seu item atual na loja e ganhar um crédito de 40% do valor do antigo para abater no preço do novo."),
            
            ManualTopic("🕊️ Benções (Blessings)", "Uma proteção especial que impede a perda de Nível e Atributos em caso de Colapso Corporal. A benção é consumida ao te salvar. Sempre compre uma antes de enfrentar grandes perigos!"),
            
            ManualTopic("💰 Economia", "A moeda oficial é o Fron. Guarde suas moedas para financiar sua evolução! Para facilitar a leitura, valores altos podem aparecer como 1K (mil) ou 1KK (milhão). Toque no saldo no topo da tela para ver o valor exato."),
            
            ManualTopic("👥 Profissões", 
                "• Policial: Ganha mais Vida ao evoluir (+15 por nível). Especialista em exploração e combate.\n" +
                "• Médico: Especialista em Inteligência e Energia Mental. Ganha bônus de Experiência (+20%) em explorações.\n" +
                "• Engenheiro: Possui a melhor gestão de vigor físico. Ganha mais Energia (+10 por nível) ao evoluir.\n" +
                "• Professor: Especialista em Carisma e persuasão. Ganha bônus de Frons (+30%) negociando em explorações.\n" +
                "• Detetive: Focado em Velocidade e investigação. Possui o dobro de sorte para encontrar itens raros."),
            
            ManualTopic("👤 Avatares", "Sua imagem no mundo de Typing Frontier. Além do visual padrão, você pode desbloquear novos estilos subindo de nível, completando a coleção comercial ou através de cargos especiais na comunidade."),

            ManualTopic("🏆 Conquistas", "Celebre sua evolução! Ganhe Frons extras e desbloqueie avatares exclusivos ao alcançar marcos de exploração, treinamento e riqueza.")
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
