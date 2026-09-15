package com.typingfrontier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ManualActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ajuste da Barra de Status para o tema escuro
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        setContentView(R.layout.activity_manual)

        findViewById<Button>(R.id.btnFecharManual).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvManual)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ManualAdapter(getManualTopics())
    }

    private fun getManualTopics(): List<ManualTopic> {
        return listOf(
            ManualTopic("🎯 Introdução", "Bem-vindo ao Typing Frontier! Um RPG focado no desenvolvimento do seu personagem, onde você resolve desafios e realiza atividades através de respostas digitadas. Evolua seus atributos, escolha uma profissão, explore o mundo e tome decisões que moldam seu futuro!"),
            
            ManualTopic("📊 Atributos", "Cada atributo influencia seu desempenho de forma única:\n\n• Força: Essencial para tarefas físicas e segurança na exploração.\n• Resistência: Diminui o consumo de Energia e permite aguentar mais traumas antes de um colapso.\n• Velocidade: Aumenta sua eficiência e as chances de sucesso em fugas.\n• Inteligência: Melhora seu salário e habilidades de aprendizado.\n• Carisma: Melhora seu impacto social e negociações."),
            
            ManualTopic("❤️ Vida (HP)", "Sua saúde física atual. Se sua Vida chegar a zero durante uma exploração ou incidente, você desmaia e precisará ser hospitalizado. Mantenha-se saudável com alimentação e descanso. Note que Vida e Traumas são coisas diferentes."),

            ManualTopic("🩹 Hospitalização e Trauma", "Quando você desmaia, é levado ao hospital. Isso gera consequências:\n\n" +
                "• Trauma: Cada hospitalização adiciona 1 Trauma ao seu corpo. Traumas são estados negativos persistentes que limitam sua saúde.\n" +
                "• Recuperação: Hospitalizado, seu personagem fica com 40% da Vida máxima, 20% da Energia Física máxima e 40% da Energia Mental máxima. Você também perde 15% da Experiência do nível atual.\n" +
                "• Ciclo de Cura: Seu corpo recupera naturalmente 1 Trauma a cada 2 noites de sono normal (pago)."),

            ManualTopic("🚨 Colapso Corporal", "Seu corpo possui um Limite de Traumas (baseado em sua Vida e Resistência). Se você for hospitalizado e atingir esse limite, sofrerá um Colapso:\n\n" +
                "• Perda de Progresso: Você perde 25% de todo o seu progresso acumulado (Experiência e Atributos), o que pode resultar em redução de nível.\n" +
                "• Sequelas: Seus atributos (Força, Velocidade, etc.) sofrem uma redução imediata.\n" +
                "• Equipamentos: Há uma chance de perder um item equipado (protegida pelo Seguro).\n" +
                "• Reabilitação: As perdas de progresso do Colapso ficam registradas e podem ser recuperadas com Medicamentos."),
            
            ManualTopic("⚡ Energia", "Representa seu vigor físico para o dia a dia. Quase todas as ações gastam Energia. Se você ficar exausto, não poderá trabalhar ou treinar até comer algo ou dormir. Se tentar agir sem Energia, o jogo sugerirá um lanche rápido."),
            
            ManualTopic("🧠 Energia Mental", "Representa seu foco para tarefas intelectuais. Estudar e trabalhar consomem Energia Mental. Ela é mais limitada que a física; se ela acabar, você precisará de uma pausa estratégica ou de uma noite de sono."),
            
            ManualTopic("💼 Trabalho", "Sua principal fonte de renda. O salário aumenta conforme você sobe de nível e melhora sua Inteligência. Você pode trabalhar normalmente uma vez por dia, e o expediente termina às 22h. Lembre-se: trabalhar exige Energia e Energia Mental."),
            
            ManualTopic("⏱️ Hora Extra", "Precisa de mais Frons? Após o turno normal, você pode realizar jornadas extras assistindo a anúncios. \n\nFique atento: quanto mais você trabalha extra no mesmo dia, mais Energia e Energia Mental são consumidas, e menos dinheiro você ganha. É uma atividade exclusivamente financeira e não gera Experiência."),

            ManualTopic("🥪 Alimentação", "Essencial para recuperar Energia durante o dia. Cada refeição custa Frons. Planeje-se bem, pois as lanchonetes fecham rigorosamente às 22h."),
            
            ManualTopic("🧘 Pausa (Descanso)", "Uma pausa rápida ajuda a recuperar um pouco de Energia Mental sem gastar Frons ou tempo. Você pode fazer uma pausa por dia."),
            
            ManualTopic("😴 Dormir", "A hora de recarregar! Dormir em casa recupera toda sua Energia e Energia Mental, além de processar a cura natural de Traumas (1 a cada 2 dias).\n\n" +
                "• Aluguel: Você deve pagar a estadia. Se não tiver Frons, dormirá na rua.\n" +
                "• Dormir na Rua: Uma situação desesperadora. Você acorda com apenas 30% de Energia e perde 20 pontos de Vida. Traumas não são curados na rua."),
            
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
            
            ManualTopic("🎒 Equipamentos e Mochila", "Melhore seu personagem com itens profissionais!\n\n• Mochila: Tudo o que você compra vai para sua mochila, que possui um limite de 5 itens.\n• Slots: Para ganhar os bônus, você deve equipar os itens em seus respectivos slots (Corpo, Cabeça, Mão, Acessório, etc).\n• Troca: Ao equipar um novo item em um slot ocupado, o antigo retornará automaticamente para sua mochila.\n• Loja: Você pode comprar equipamentos de qualquer profissão, desde que tenha o Nível Mínimo e Frons suficientes."),
            
            ManualTopic("💰 Venda de Equipamentos", "Não precisa mais de um item? Você pode vendê-lo na Loja para recuperar parte do investimento.\n\n• Valor: Você recebe 40% do valor atual de mercado do item.\n• Condição: O equipamento deve estar na sua mochila. Itens atualmente equipados não aparecem para venda direta; você precisa desequipá-los primeiro."),

            ManualTopic("🕊️ Seguro de Equipamentos", "Uma proteção essencial para seus bens. O Seguro de Equipamentos protege seus itens equipados durante um Colapso Corporal.\n\n• Funcionamento: Se você sofrer um Colapso (atingir o limite de traumas), existe uma chance de perder um equipamento. O Seguro impede essa perda, sendo consumido no processo.\n• Recomendação: Sempre mantenha um Seguro ativo antes de expedições perigosas!"),
            
            ManualTopic("💊 Medicamento", "O aliado essencial para a reabilitação física e técnica. Ele funciona de forma preventiva (automática).\n\n" +
                "• Proteção Automática: Se você já tiver Medicamentos no estoque quando sofrer uma hospitalização, um será consumido automaticamente para tratar o novo Trauma imediatamente.\n" +
                "• Vida (HP): Recupera sua Vida. Se for o último Trauma, a restauração é total; caso contrário, é parcial.\n" +
                "• Reabilitação: Cada dose restaura 50% da Experiência e dos Atributos perdidos no último Colapso Corporal.\n" +
                "• Regra de Estoque: Medicamentos comprados APÓS um Trauma não são consumidos retroativamente; eles permanecem no estoque para uso posterior."),

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
