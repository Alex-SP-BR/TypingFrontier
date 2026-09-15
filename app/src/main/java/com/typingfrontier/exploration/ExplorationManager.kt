package com.typingfrontier.exploration

import com.typingfrontier.Player
import com.typingfrontier.economy.ProfessionManager
import kotlin.random.Random

object ExplorationManager {

    fun calcularSucesso(player: Player, zona: ExplorationZone, etapa: Int): Boolean {
        // Agora suportamos múltiplos atributos, priorizando o foco principal
        val v1 = getValorAtributo(player, zona.atributoPrincipal)
        val v2 = getValorAtributo(player, zona.atributoSecundario)
        val v3 = getValorAtributo(player, zona.atributoTerciario)
        
        // Atributo total = 100% do principal + 30% do secundário + 20% do terciário
        // Isso valoriza o mental (principal) e dá bônus físico estratégico (apoio)
        val atributoValor = (v1 + (v2 * 0.3) + (v3 * 0.2)).toInt().coerceAtLeast(1)

        // Dificuldade reduzida na Etapa 1 para ser mais intuitivo
        // Dificuldade = Base da zona + (etapa * 8) - bônus inicial de etapa 1
        var dificuldade = zona.riscoBase + (etapa * 8)
        if (etapa == 1) dificuldade -= 10 // Torna a primeira etapa muito fácil
        
        // Mecânica Noturna: Mais perigoso à noite (após as 18:00)
        if (player.hora >= 18 || player.hora < 6) {
            dificuldade += 15
        }

        // Poder do jogador: Atributo + Sorte (1-20)
        // A FÓRMULA DE SUCESSO PERMANECE IDÊNTICA
        val sorte = Random.nextInt(1, 21)
        val poderTotal = (atributoValor * 2) + sorte

        // Passiva Policial: 20% de chance de converter falha em sucesso por instinto
        if (player.profissao == "Policial" && poderTotal < dificuldade) {
            if (Random.nextInt(1, 101) <= 20) return true
        }

        return poderTotal >= dificuldade
    }

    private fun getValorAtributo(player: Player, nome: String?): Int {
        return when (nome) {
            "FORCA" -> player.forcaEfetiva
            "INTELIGENCIA" -> player.inteligenciaEfetiva
            "CARISMA" -> player.carismaEfetiva
            "RESISTENCIA" -> player.resistenciaEfetiva
            "VELOCIDADE" -> player.velocidadeEfetiva
            else -> 0
        }
    }

    fun gerarRecompensa(player: Player, zona: ExplorationZone, etapa: Int): Map<String, Int> {
        // Multiplicador Progressivo: 
        // Etapa 1: 1.0x
        // Etapa 2: 1.5x
        // Etapa 3: 2.2x
        // Etapa 4: 3.2x
        // Etapa 5: 4.5x
        val multiplicador = when (etapa) {
            1 -> 1.0
            2 -> 1.5
            3 -> 2.2
            4 -> 3.2
            5 -> 4.5
            else -> 1.0
        }
        
        var xp = (zona.recompensaBaseXp * multiplicador).toInt()
        var dinheiro = (zona.recompensaBaseDinheiro * multiplicador).toInt()

        // Bônus Noturno: +50% XP e Dinheiro
        if (player.hora >= 18 || player.hora < 6) {
            xp = (xp * 1.5).toInt()
            dinheiro = (dinheiro * 1.5).toInt()
        }

        // Passiva Médico: Ganha 20% mais XP em exploração
        if (player.profissao == "Médico") xp = (xp * 1.2).toInt()
        
        // Passiva Professor: Ganha 30% mais dinheiro (negociação)
        if (player.profissao == "Professor") dinheiro = (dinheiro * 1.3).toInt()

        return mapOf("xp" to xp, "dinheiro" to dinheiro)
    }

    fun verificarItemRaro(player: Player, zona: ExplorationZone, etapa: Int): String? {
        var chance = zona.chanceItemRaro + (etapa * 2)
        
        // Passiva Detetive: Dobra a chance de achar itens raros
        if (player.profissao == "Detetive") chance *= 2
        
        return if (Random.nextInt(1, 101) <= chance) {
            "Relíquia de ${zona.nome}" // Por enquanto um nome genérico
        } else null
    }

    fun processarFalhaCritica(player: Player, zona: ExplorationZone? = null): String {
        // Mensagem personalizada baseada em riscos da aventura e do local
        val causaIncapacitacao = when(player.profissao) {
            "Engenheiro" -> {
                val falhas = mutableListOf(
                    "A estrutura cedeu de repente, deixando você preso sob os escombros até o resgate.",
                    "Um curto-circuito imprevisto causou uma pequena explosão e muita fumaça asfixiante.",
                    "Você ignorou um aviso sonoro de falha e sofreu um acidente que te deixou sem forças."
                )
                when(zona?.id) {
                    "cassino" -> falhas.add("O sistema de ventilação do cassino falhou criticamente, sufocando sua tentativa de reparo.")
                    "esgotos" -> falhas.add("Uma tubulação de alta pressão rompeu-se nos esgotos, arremessando você contra a parede.")
                    "laboratorio" -> falhas.add("O reator emitiu um pulso eletromagnético que desorientou seus sentidos.")
                }
                falhas.random()
            }
            "Policial" -> {
                val falhas = mutableListOf(
                    "Você foi cercado por suspeitos antes que pudesse pedir reforço e acabou rendido.",
                    "Seu instinto falhou e você caiu em uma armadilha preparada no local, ferindo-se seriamente.",
                    "Uma briga generalizada saiu do controle e você foi atingido na confusão, perdendo os sentidos."
                )
                when(zona?.id) {
                    "beco" -> falhas.add("Uma emboscada nas sombras do beco superou sua guarda tática.")
                    "cassino" -> falhas.add("A segurança do cassino reagiu de forma muito mais agressiva do que o esperado.")
                    "centro" -> falhas.add("A multidão em pânico no centro comercial impediu sua manobra de contenção.")
                }
                falhas.random()
            }
            "Médico" -> {
                val falhas = mutableListOf(
                    "Você inalou vapores tóxicos e sentiu suas pernas fraquejarem antes de conseguir sair.",
                    "O cansaço acumulado fez você ignorar os riscos de contaminação e seu corpo colapsou.",
                    "Ao tentar salvar outra pessoa, você acabou se ferindo e precisou de resgate imediato."
                )
                when(zona?.id) {
                    "laboratorio" -> falhas.add("Um vazamento biológico não detectado comprometeu sua saúde durante a análise.")
                    "esgotos" -> falhas.add("A alta concentração de gases nos esgotos causou um desmaio repentino.")
                }
                falhas.random()
            }
            "Detetive" -> {
                val falhas = mutableListOf(
                    "A pista falsa levou você direto para uma emboscada. Você foi rendido e desarmado.",
                    "Sua investigação atraiu a atenção das pessoas erradas e você foi pego desprevenido.",
                    "Um detalhe que parecia insignificante revelou-se um perigo: você baixou a guarda e desmaiou."
                )
                when(zona?.id) {
                    "beco" -> falhas.add("Enquanto seguia uma pista no beco, você foi atingido por trás e perdeu os sentidos.")
                    "suburbio" -> falhas.add("O terreno instável das fábricas abandonadas causou uma queda perigosa durante sua perseguição.")
                }
                falhas.random()
            }
            "Professor" -> {
                val falhas = mutableListOf(
                    "Suas palavras não foram suficientes para acalmar a situação e ela fugiu do controle.",
                    "A tensão do momento causou um colapso físico e você perdeu a consciência.",
                    "Você se desorientou e a exaustão impediu que encontrasse o caminho de volta sozinho."
                )
                when(zona?.id) {
                    "cassino" -> falhas.add("A atmosfera caótica do cassino sobrecarregou sua capacidade de análise e você colapsou.")
                    "parque" -> falhas.add("A situação social na praça escalou mais rápido do que sua mediação pôde acompanhar.")
                }
                falhas.random()
            }
            else -> "Você não teve fôlego suficiente para superar os perigos do local."
        }

        // Passiva Policial: Chance de fugir sem hospitalização
        if (player.profissao == "Policial" && Random.nextInt(1, 101) <= 40) {
            player.energia = (player.energia - 30).coerceAtLeast(1)
            return "🏃 $causaIncapacitacao\n\nSeu instinto policial permitiu uma retirada tática imediata, evitando o hospital, mas o esforço consumiu muita Energia."
        }
        
        // Punição por colapso
        val msgHospital = ProfessionManager.hospitalizar(player)
        return "🏥 $causaIncapacitacao\n\n$msgHospital"
    }

    fun gerarDescricaoSucesso(profissao: String, etapa: Int, zona: ExplorationZone): String {
        val local = zona.ambiente
        
        // 🌳 REESCRITA NARRATIVA: PARQUE DA CIDADE (AVENTURA 1)
        if (zona.id == "parque") {
            return when (profissao) {
                "Policial" -> when (etapa) {
                    1 -> "Você inicia a patrulha na praça e logo nota um princípio de confusão perto do coreto."
                    2 -> "Ao se aproximar, você vê um grupo de jovens intimidando um senhor que apenas tentava descansar."
                    3 -> "Você intervém com firmeza, acalmando os ânimos e garantindo que o idoso saia em segurança."
                    4 -> "Após dispersar o grupo, você realiza uma ronda preventiva nas trilhas próximas para evitar retaliações."
                    5 -> "A paz retornou ao parque. Você sente que cumpriu seu dever de proteger a comunidade hoje."
                    else -> "Você garantiu a segurança dos frequentadores do parque."
                }
                "Médico" -> when (etapa) {
                    1 -> "Enquanto caminha na praça, você ouve um grito agudo vindo da área das árvores mais densas."
                    2 -> "Uma criança caiu de um galho baixo. Ela está assustada e com um corte feio no braço."
                    3 -> "Você higieniza o ferimento com calma, tranquilizando a mãe que acaba de chegar correndo."
                    4 -> "Após fazer um curativo improvisado, você orienta a família sobre os cuidados necessários."
                    5 -> "A criança já parou de chorar e te agradece com um sorriso. Sua intervenção evitou o pânico."
                    else -> "Você prestou os primeiros socorros necessários no parque."
                }
                "Professor" -> when (etapa) {
                    1 -> "Você observa uma agitação incomum na praça e percebe um grupo de crianças assustadas."
                    2 -> "Ao conversar com elas, você nota que o medo está começando a tomar conta dos pequenos."
                    3 -> "Você organiza uma roda de conversa e propõe uma atividade rápida para mantê-los focados e calmos."
                    4 -> "Enquanto isso, você usa seu celular para localizar o responsável pela excursão escolar."
                    5 -> "O monitor aparece aliviado. Você transformou um momento de crise em uma lição de organização."
                    else -> "Você mediou uma situação social delicada no parque."
                }
                "Detetive" -> when (etapa) {
                    1 -> "Seu olhar capta algo estranho: uma mochila de marca deixada sozinha sob um banco na praça."
                    2 -> "Você observa o movimento ao redor e nota um indivíduo olhando para trás com frequência."
                    3 -> "Seguindo os rastros discretos na grama, você encontra um envelope que caiu do bolso dele."
                    4 -> "O envelope contém documentos pessoais. Você consegue ligar os pontos e localizar o dono real."
                    5 -> "Você devolve os itens ao proprietário distraído antes que alguém mal-intencionado os levasse."
                    else -> "Você resolveu um pequeno mistério cotidiano no parque."
                }
                "Engenheiro" -> when (etapa) {
                    1 -> "O som da fonte na praça soa estranho para seus ouvidos treinados. Há algo vibrando incorretamente."
                    2 -> "Você abre a tampa de inspeção e nota que uma das válvulas de pressão está prestes a romper."
                    3 -> "Utilizando suas ferramentas básicas, você ajusta o fluxo e interrompe o vazamento iminente."
                    4 -> "Você calibra o painel de controle da bomba, garantindo que o sistema opere sem sobrecarga."
                    5 -> "A água volta a jorrar com força e clareza. Você evitou um alagamento que estragaria o lazer de todos."
                    else -> "Você realizou a manutenção preventiva necessária no parque."
                }
                else -> "Você obteve sucesso na sua busca $local."
            }
        }

        // 🌍 NARRATIVAS POR ZONA E PROFISSÃO
        return when (zona.id) {
            "centro" -> when (profissao) {
                "Policial" -> when (etapa) {
                    1 -> "Você caminha pelos corredores e nota um movimento suspeito perto da joalheria."
                    2 -> "Ao abordar um indivíduo, você percebe que ele estava prestes a realizar um furto."
                    3 -> "Você detém o suspeito e chama a segurança do shopping para assumir a custódia."
                    4 -> "A gerência agradece sua eficiência e você aproveita para checar outras lojas."
                    5 -> "O Centro Comercial está mais seguro agora. Você parte com o reconhecimento de todos."
                    else -> "Você garantiu a ordem no centro comercial."
                }
                "Médico" -> when (etapa) {
                    1 -> "Em meio à multidão, você vê um idoso encostado na parede, parecendo muito pálido."
                    2 -> "O calor excessivo causou uma queda de pressão. Você o acomoda e oferece água."
                    3 -> "Você monitora os sinais vitais até que ele se sinta pronto para caminhar novamente."
                    4 -> "Antes de sair, você orienta os seguranças sobre como lidar com a exaustão térmica."
                    5 -> "Você salvou o dia de alguém sem precisar de um hospital. Missão cumprida."
                    else -> "Você prestou atendimento médico no centro comercial."
                }
                "Professor" -> when (etapa) {
                    1 -> "Você nota um grupo de turistas estrangeiros completamente perdidos e aflitos."
                    2 -> "Usando sua didática, você explica as rotas e os costumes locais para ajudá-los."
                    3 -> "A conversa atrai locais que também precisavam de orientação sobre o novo sistema de transporte."
                    4 -> "Você organiza um pequeno grupo de informação improvisado no meio do shopping."
                    5 -> "Você partiu deixando um grupo de pessoas muito mais informadas e gratas."
                    else -> "Você mediou informações úteis no centro comercial."
                }
                "Detetive" -> when (etapa) {
                    1 -> "Seu olhar foca em alguém que circula pelas lojas sem olhar para as vitrines."
                    2 -> "Você percebe um padrão: ele está trocando pacotes discretamente com os vendedores."
                    3 -> "Ao interceptar um dos pacotes descartados, você descobre um esquema de contrabando."
                    4 -> "Você coleta as evidências necessárias para desmantelar a rede de receptação local."
                    5 -> "Mais um caso resolvido. As lojas do centro agora operam sob a lei."
                    else -> "Você desvendou um crime no centro comercial."
                }
                "Engenheiro" -> when (etapa) {
                    1 -> "O som rítmico da escada rolante está fora do tom. Uma engrenagem está batendo."
                    2 -> "Você desativa o painel e nota que um objeto estranho travou o mecanismo de rolagem."
                    3 -> "Com habilidade, você remove a obstrução e recalibra os sensores de segurança."
                    4 -> "Você aproveita para ajustar o painel elétrico que estava prestes a superaquecer."
                    5 -> "Tudo volta a funcionar perfeitamente. Você evitou um acidente e um prejuízo enorme."
                    else -> "Você realizou reparos essenciais no centro comercial."
                }
                else -> "Você obteve sucesso na sua busca nas lojas."
            }
            "suburbio" -> when (profissao) {
                "Policial" -> when (etapa) {
                    1 -> "As fábricas abandonadas são o esconderijo perfeito. Você entra com a lanterna em punho."
                    2 -> "Você encontra sinais de invasão recente em um dos galpões de máquinas pesadas."
                    3 -> "Um grupo de saqueadores foge ao notar sua presença autoritária no local."
                    4 -> "Você lacra as entradas vulneráveis e garante que o perímetro esteja isolado."
                    5 -> "O silêncio retorna ao subúrbio. A área está sob controle, por enquanto."
                    else -> "Você patrulhou a zona industrial com sucesso."
                }
                "Médico" -> when (etapa) {
                    1 -> "O ar aqui é pesado. Você nota um morador local com uma tosse preocupante."
                    2 -> "Você avalia a situação e percebe que ele foi exposto a resíduos de amianto antigos."
                    3 -> "Você presta o atendimento paliativo e fornece uma máscara de proteção de alta qualidade."
                    4 -> "Você encontra um kit de primeiros socorros industrial esquecido e recupera o que é útil."
                    5 -> "Sua presença trouxe alívio para quem o mundo esqueceu. Você parte satisfeito."
                    else -> "Você prestou auxílio médico na zona industrial."
                }
                "Professor" -> when (etapa) {
                    1 -> "Você caminha pelas ruínas e reflete sobre a história do trabalho nesta região."
                    2 -> "Você encontra um grupo de jovens desocupados e inicia uma conversa franca sobre o futuro."
                    3 -> "Eles mostram a você antigos manuais técnicos que sobreviveram ao fechamento das fábricas."
                    4 -> "Você usa seu conhecimento para explicar a importância histórica daqueles equipamentos."
                    5 -> "Você transformou um lugar de abandono em uma sala de aula a céu aberto."
                    else -> "Você ensinou lições valiosas no subúrbio."
                }
                "Detetive" -> when (etapa) {
                    1 -> "Você segue o rastro de pneus que leva a um galpão supostamente vazio."
                    2 -> "Lá dentro, você encontra caixas com o selo de uma carga roubada na semana passada."
                    3 -> "Você anota as matrículas dos veículos e coleta amostras da substância nos contêineres."
                    4 -> "Sua investigação revela o destino final dos recursos desviados da cidade."
                    5 -> "As pistas estão completas. A justiça será feita graças ao seu olhar clínico."
                    else -> "Você descobriu um esconderijo de carga no subúrbio."
                }
                "Engenheiro" -> when (etapa) {
                    1 -> "Um antigo gerador a diesel está parado, mas parece estar em boas condições."
                    2 -> "Você limpa os injetores e reconecta a fiação que foi cortada por vândalos."
                    3 -> "Com um rugido, a máquina volta à vida, iluminando parte da rua abandonada."
                    4 -> "Você ajusta a pressão das tubulações de vapor para evitar uma explosão por pressão."
                    5 -> "A infraestrutura básica foi restaurada. Você deu vida nova a um lugar morto."
                    else -> "Você restaurou sistemas industriais no subúrbio."
                }
                else -> "Você obteve sucesso na sua busca nas fábricas."
            }
            "beco" -> when (profissao) {
                "Policial" -> when (etapa) {
                    1 -> "Você entra nas sombras com a guarda alta. O cheiro de perigo é real aqui."
                    2 -> "Você interrompe uma transação suspeita entre dois indivíduos no fundo do beco."
                    3 -> "Eles tentam fugir, mas sua tática de cerco os obriga a abandonar o material ilícito."
                    4 -> "Você confisca os itens e garante que a rota de fuga deles seja bloqueada permanentemente."
                    5 -> "A escuridão recua diante da sua autoridade. O beco está limpo por esta noite."
                    else -> "Você limpou o beco escuro com sucesso."
                }
                "Médico" -> when (etapa) {
                    1 -> "Você encontra alguém caído entre os sacos de lixo, respirando com dificuldade."
                    2 -> "É uma overdose de uma substância desconhecida. Você age rápido para estabilizá-lo."
                    3 -> "Usando seu kit de emergência, você administra o antídoto e monitora a recuperação."
                    4 -> "Você higieniza a área ao redor para evitar infecções oportunistas no paciente."
                    5 -> "Você salvou uma vida onde poucos ousariam entrar. Sua ética é inabalável."
                    else -> "Você prestou socorro de emergência no beco."
                }
                "Professor" -> when (etapa) {
                    1 -> "Você é abordado por um informante nervoso que parece não confiar em ninguém."
                    2 -> "Com paciência e retórica, você ganha a confiança dele e obtém informações vitais."
                    3 -> "Ele revela a você onde os recursos mais valiosos da área estão escondidos."
                    4 -> "Você o instrui sobre como sair daquela vida e oferece um contato seguro na cidade."
                    5 -> "Suas palavras foram a luz que ele precisava. Você sai com informações e a consciência limpa."
                    else -> "Você mediou uma negociação perigosa no beco."
                }
                "Detetive" -> when (etapa) {
                    1 -> "Você encontra uma marca de sapato incomum no asfalto úmido e decide segui-la."
                    2 -> "O rastro leva a um cofre de parede disfarçado atrás de uma placa de metal solta."
                    3 -> "Você usa técnicas de observação para encontrar a chave escondida e acessa os documentos sigilosos."
                    4 -> "As cartas revelam um esquema de corrupção que chega aos altos escalões da prefeitura."
                    5 -> "O que estava escondido nas sombras agora está em suas mãos. Caso encerrado."
                    else -> "Você encontrou segredos escondidos no beco."
                }
                "Engenheiro" -> when (etapa) {
                    1 -> "A fiação do poste de luz está em curto, criando faíscas perigosas no chão molhado."
                    2 -> "Você usa suas ferramentas isolantes para cortar a energia e refazer as conexões."
                    3 -> "Ao abrir uma caixa de controle próxima, você nota que alguém tentou hackear a rede elétrica."
                    4 -> "Você instala um bypass de segurança que impede novas tentativas de sabotagem no local."
                    5 -> "A luz volta, espantando as sombras e os perigos elétricos. Trabalho bem feito."
                    else -> "Você reparou a infraestrutura do beco."
                }
                else -> "Você obteve sucesso na sua busca nas sombras."
            }
            "laboratorio" -> when (profissao) {
                "Policial" -> when (etapa) {
                    1 -> "O alarme silencioso foi disparado. Você entra procurando por sinais de intrusão."
                    2 -> "Você encontra um terminal de computador que ainda está tentando fazer upload de dados."
                    3 -> "Você bloqueia a transmissão e extrai o disco rígido com evidências de espionagem."
                    4 -> "Você neutraliza os sistemas de defesa automatizados que tentavam impedir sua saída."
                    5 -> "Dados confidenciais protegidos. O laboratório está seguro sob custódia oficial."
                    else -> "Você protegeu o laboratório com sucesso."
                }
                "Médico" -> when (etapa) {
                    1 -> "O cheiro de ozônio e produtos químicos é forte. Você checa os sensores de bio-risco."
                    2 -> "Você identifica um frasco quebrado contendo um agente patogênico experimental."
                    3 -> "Com precisão cirúrgica, você aplica o agente de neutralização e sela a área afetada."
                    4 -> "Você coleta amostras do soro original que podem ser usadas para criar curas no futuro."
                    5 -> "Você evitou um surto antes mesmo dele começar. A ciência agradece sua perícia."
                    else -> "Você conteve um risco biológico no laboratório."
                }
                "Professor" -> when (etapa) {
                    1 -> "Você encontra quadros negros cheios de equações que parecem não ter fim."
                    2 -> "Ao analisar as anotações, você percebe um erro fundamental que causou o abandono do lab."
                    3 -> "Você corrige a fórmula, o que permite desbloquear logicamente o acesso aos arquivos."
                    4 -> "A história das descobertas feitas aqui é fascinante e você documenta cada detalhe."
                    5 -> "Você transformou confusão científica em conhecimento organizado para a posteridade."
                    else -> "Você decifrou segredos científicos no laboratório."
                }
                "Detetive" -> when (etapa) {
                    1 -> "Você entra na sala do diretor e nota que um dos quadros está levemente torto."
                    2 -> "Atrás dele, há um cofre biométrico. Você usa o pó de revelação para encontrar as digitais."
                    3 -> "O cofre abre, revelando a verdadeira razão por trás do projeto secreto 'Fronteira'."
                    4 -> "Você conecta as pistas e descobre quem financiou o experimento ilegal por anos."
                    5 -> "A verdade está exposta. Os responsáveis não terão para onde fugir agora."
                    else -> "Você desvendou um mistério científico no laboratório."
                }
                "Engenheiro" -> when (etapa) {
                    1 -> "O reator central está zumbindo de forma preocupante. O superaquecimento é iminente."
                    2 -> "Você acessa o terminal de controle e inicia o protocolo de resfriamento de emergência."
                    3 -> "Ao abrir o painel do reator, você substitui as barras de controle que estavam gastas."
                    4 -> "Você redireciona o fluxo de energia para os servidores, recuperando sistemas antigos."
                    5 -> "A máquina está estável e funcional. Você domou uma tecnologia que estava fora de controle."
                    else -> "Você estabilizou o reator do laboratório."
                }
                else -> "Você obteve sucesso na sua busca nas bancadas."
            }
            "cassino" -> when (profissao) {
                "Policial" -> when (etapa) {
                    1 -> "Você entra disfarçado e mapeia visualmente todos os seguranças armados nas mesas."
                    2 -> "Você identifica um esquema de lavagem de dinheiro ocorrendo na sala dos grandes apostadores."
                    3 -> "Com um sinal discreto, você coordena a entrada da equipe de apoio para o flagrante."
                    4 -> "Você apreende as maletas com dinheiro ilegal e garante a custódia dos responsáveis."
                    5 -> "A operação foi concluída com sucesso. Você garantiu a ordem no local e coletou evidências cruciais."
                    else -> "Você realizou uma batida policial no cassino."
                }
                "Médico" -> when (etapa) {
                    1 -> "O estresse do jogo é alto. Você nota um apostador que está perdendo a cor e o ar."
                    2 -> "É um princípio de infarto. Você o retira da mesa e inicia as manobras de ressuscitação."
                    3 -> "Com o desfibrilador de emergência do local, você estabiliza o ritmo cardíaco dele."
                    4 -> "Você orienta os funcionários sobre os perigos da exaustão nervosa naquele ambiente."
                    5 -> "Em um lugar de ganância, você trouxe humanidade e salvou uma vida."
                    else -> "Você prestou atendimento médico no cassino."
                }
                "Professor" -> when (etapa) {
                    1 -> "Você observa o comportamento dos frequentadores, analisando os padrões estatísticos das mesas."
                    2 -> "Você identifica um jovem apostador compulsivo e usa sua didática para orientá-lo a parar."
                    3 -> "Com sua habilidade de mediação, você acalma uma discussão acalorada entre um cliente e a banca."
                    4 -> "Você compartilha conhecimentos sobre probabilidades, educando os presentes sobre os riscos reais."
                    5 -> "Sua presença trouxe ordem e consciência ao ambiente. Você encerrou sua análise com sucesso."
                    else -> "Você realizou uma análise comportamental no cassino."
                }
                "Detetive" -> when (etapa) {
                    1 -> "Você nota que as cartas de uma das mesas possuem marcas invisíveis à luz comum."
                    2 -> "Usando seu filtro UV, você descobre como a casa está trapaceando os clientes."
                    3 -> "Você segue o gerente até o cofre e nota a combinação que ele digita distraidamente."
                    4 -> "Você acessa a contabilidade secreta do cassino, provando o crime de fraude financeira."
                    5 -> "Você tem todas as provas. O dono do cassino não poderá mais blefar com a justiça."
                    else -> "Você provou a fraude no cassino clandestino."
                }
                "Engenheiro" -> when (etapa) {
                    1 -> "Você realiza uma inspeção técnica discreta na fiação exposta das máquinas de jogo."
                    2 -> "Você detecta uma falha crítica no sistema elétrico que poderia causar um incêndio no salão."
                    3 -> "Com precisão, você repara os painéis de controle, estabilizando a rede de energia do cassino."
                    4 -> "Você calibra os sistemas de ventilação para garantir a segurança técnica dos clientes e funcionários."
                    5 -> "A infraestrutura do cassino foi estabilizada. Sua competência técnica garantiu a segurança do local."
                    else -> "Você realizou manutenções técnicas no cassino."
                }
                else -> "Você obteve sucesso na sua busca entre as mesas."
            }
            "esgotos" -> when (profissao) {
                "Policial" -> when (etapa) {
                    1 -> "Você desce para os túneis úmidos, mantendo a lanterna focada nos cantos escuros."
                    2 -> "Sussurros ecoam na água. Você prepara sua abordagem tática para um possível encontro."
                    3 -> "Você localiza o esconderijo de uma gangue e dispersa os criminosos antes que reajam."
                    4 -> "Você marca as rotas seguras nas paredes para futuras operações de resgate."
                    5 -> "Os túneis estão pacificados. O perigo que vinha de baixo foi neutralizado."
                    else -> "Você patrulhou os esgotos com sucesso."
                }
                "Médico" -> when (etapa) {
                    1 -> "O cheiro e a umidade são terríveis. Você checa seu traje de proteção contra bactérias."
                    2 -> "Você encontra uma amostra de fungo bioluminescente que possui propriedades curativas raras."
                    3 -> "Com cuidado, você coleta a amostra e neutraliza uma poça de resíduo químico tóxico próxima."
                    4 -> "Você trata um ferimento infeccionado em um morador de rua que se escondia nos túneis."
                    5 -> "Você trouxe saúde para o lugar mais insalubre da cidade. Trabalho heroico."
                    else -> "Você conteve infecções nos esgotos profundos."
                }
                "Professor" -> when (etapa) {
                    1 -> "Você observa as marcas nas paredes que contam a história das antigas construções da cidade."
                    2 -> "Você guia o grupo através do labirinto de túneis usando seu conhecimento de mapas antigos."
                    3 -> "Sua liderança impede que o pânico se espalhe quando a água começa a subir repentinamente."
                    4 -> "Você instrui todos sobre como usar o eco para se localizarem na escuridão total."
                    5 -> "Você transformou uma descida aos infernos em uma lição de sobrevivência e história."
                    else -> "Você guiou uma expedição nos esgotos com sucesso."
                }
                "Detetive" -> when (etapa) {
                    1 -> "Você encontra um objeto metálico brilhando no meio da lama negra do esgoto."
                    2 -> "É um relógio de luxo que pertence a um empresário desaparecido há três meses."
                    3 -> "Você segue o rastro de arrasto na lama que leva a um acesso bloqueado por grades."
                    4 -> "Atrás das grades, você descobre evidências de que o local foi usado como cativeiro."
                    5 -> "O mistério do desaparecimento foi resolvido nas profundezas. O caso está encerrado."
                    else -> "Você resolveu um desaparecimento nos esgotos."
                }
                "Engenheiro" -> when (etapa) {
                    1 -> "A pressão da água nos túneis principais está subindo perigosamente rápido."
                    2 -> "Você localiza a válvula de alívio que está enferrujada e travada há décadas."
                    3 -> "Com força e técnica, você libera a válvula, drenando o excesso de água da região."
                    4 -> "Você reforça as vigas de sustentação que estavam corroídas pelo tempo e pela umidade."
                    5 -> "Você evitou um colapso estrutural que inundaria a cidade e parte para a superfície."
                    else -> "Você estabilizou a infraestrutura dos esgotos."
                }
                else -> "Você obteve sucesso na sua busca nos túneis."
            }
            else -> "Você obteve sucesso na sua busca $local."
        }
    }
}
