package com.typingfrontier.mental

object PortugueseGenerator {

    private val mapaPlurais = mapOf(
        "o" to "os", "a" to "as", "um" to "uns", "uma" to "umas",
        "no" to "nos", "na" to "nas", "do" to "dos", "da" to "das",
        "as" to "as", "os" to "os", "nos" to "nos", "nas" to "nas",
        "pão" to "pães", "avião" to "aviões", "lição" to "lições",
        "violão" to "violões", "ladrão" to "ladrões", "plantação" to "plantações",
        "professor" to "professores", "professora" to "professoras",
        "escritor" to "escritores", "jogador" to "jogadores",
        "pintor" to "pintores", "agricultor" to "agricultores",
        "programador" to "programadores", "doutor" to "doutores",
        "jantar" to "jantares", "policial" to "policiais",
        "sinal" to "sinais", "viagem" to "viagens",
        "vírus" to "vírus", "personagens" to "personagens",
        "motores" to "motores", "pistas" to "pistas",
        "turistas" to "turistas", "sementes" to "sementes",
        "dados" to "dados", "cores" to "cores",
        "exames" to "exames", "sintomas" to "sintomas",
        "anotações" to "anotações", "pesquisa" to "pesquisas",
        "dorme" to "dormem", "comprou" to "compraram",
        "analisou" to "analisaram", "explicou" to "explicaram",
        "venceu" to "venceram", "abriu" to "abriram",
        "descobriu" to "descobriram", "resolveu" to "resolveram",
        "passou" to "passaram", "terminou" to "terminaram",
        "examinou" to "examinaram", "derrubou" to "derrubaram",
        "desenhou" to "desenharam", "corrigiu" to "corrigiram",
        "latiu" to "latiram", "parou" to "pararam",
        "pintou" to "pintaram", "criou" to "criaram",
        "colheu" to "colheram", "investigou" to "investigaram",
        "encontrou" to "encontraram", "preparou" to "prepararam",
        "construiu" to "construíram", "projetou" to "projetaram",
        "pousou" to "pousaram", "publicou" to "publicaram",
        "recebeu" to "receberam", "estudou" to "estudaram",
        "chutou" to "chutaram", "salvou" to "salvaram",
        "ensinou" to "ensinaram", "perseguiu" to "perseguiram",
        "leu" to "leram", "correu" to "correram",
        "consertou" to "consertaram", "escreveu" to "escreveram",
        "aprendeu" to "aprenderam", "plantou" to "plantaram",
        "acelerou" to "aceleraram", "brincou" to "brincaram",
        "tocou" to "tocaram", "marcou" to "marcaram",
        "ganhou" to "ganharam", "receitou" to "receitaram",
        "prendeu" to "prenderam", "revisou" to "revisaram",
        "misturou" to "misturaram", "levantou" to "levantaram",
        "fez" to "fizeram", "respondeu" to "responderam",
        "protegeu" to "protegeram", "iniciou" to "iniciaram",
        "calculou" to "calcularam", "imaginou" to "imaginaram",
        "compôs" to "compuseram", "verificou" to "verificaram",
        "treinou" to "treinaram", "interrogou" to "interrogaram",
        "comemorou" to "comemoraram", "trocou" to "trocaram",
        "regou" to "regaram", "avaliou" to "avaliaram",
        "seguiu" to "seguiram", "afinou" to "afinaram",
        "observou" to "observaram", "atendeu" to "atenderam",
        "durante" to "durante", "intensamente" to "intensamente",
        "rápido" to "rápido", "nadar" to "nadar", "para" to "para", "com" to "com"
    )

    private val mapaGeneros = mapOf(
        "problema" to "masculino", "motorista" to "comum de dois gêneros", "artista" to "comum de dois gêneros",
        "jornalista" to "comum de dois gêneros", "atleta" to "comum de dois gêneros", "planeta" to "masculino",
        "mapa" to "masculino", "sintomas" to "masculino", "dia" to "masculino",
        "clima" to "masculino", "tema" to "masculino", "sistema" to "masculino",
        "árvore" to "feminino", "noite" to "feminino", "ponte" to "feminino",
        "cidade" to "feminino", "equipe" to "feminino", "lição" to "feminino",
        "cores" to "feminino", "anotações" to "feminino", "viagem" to "feminino",
        "sementes" to "feminino", "mão" to "feminino", "flor" to "feminino",
        "personagens" to "comum de dois gêneros", "personagem" to "comum de dois gêneros",
        "pistas" to "feminino", "estudante" to "comum de dois gêneros",
        "cliente" to "comum de dois gêneros", "gerente" to "comum de dois gêneros",
        "detetive" to "comum de dois gêneros", "cientista" to "comum de dois gêneros",
        "policial" to "comum de dois gêneros", "piloto" to "comum de dois gêneros",
        "paciente" to "comum de dois gêneros", "turista" to "comum de dois gêneros",
        "turistas" to "comum de dois gêneros"
    )

    private val mapaClasses = mapOf(
        "o" to "artigo", "a" to "artigo", "os" to "artigo", "as" to "artigo",
        "um" to "artigo", "uma" to "artigo", "uns" to "artigo", "umas" to "artigo",
        "no" to "preposição", "na" to "preposição", "nos" to "preposição", "nas" to "preposição",
        "do" to "preposição", "da" to "preposição", "dos" to "preposição", "das" to "preposição",
        "para" to "preposição", "com" to "preposição", "de" to "preposição", "em" to "preposição",
        "e" to "conjunção", "rápido" to "advérbio", "intensamente" to "advérbio", "durante" to "preposição",
        "preto" to "adjetivo", "forte" to "adjetivo", "nova" to "adjetivo", "vermelho" to "adjetivo",
        "dorme" to "verbo", "comprou" to "verbo", "analisou" to "verbo", "explicou" to "verbo",
        "venceu" to "verbo", "abriu" to "verbo", "descobriu" to "verbo", "resolveu" to "verbo",
        "passou" to "verbo", "terminou" to "verbo", "examinou" to "verbo", "derrubou" to "verbo",
        "desenhou" to "verbo", "corrigiu" to "verbo", "latiu" to "verbo", "parou" to "verbo",
        "pintou" to "verbo", "criou" to "verbo", "colheu" to "verbo", "investigou" to "verbo",
        "encontrou" to "verbo", "preparou" to "verbo", "construiu" to "verbo", "projetou" to "verbo",
        "pousou" to "verbo", "fez" to "verbo", "publicou" to "verbo", "recebeu" to "verbo", "estudou" to "verbo",
        "chutou" to "verbo", "salvou" to "verbo", "ensinou" to "verbo", "perseguiu" to "verbo",
        "leu" to "verbo", "correu" to "verbo", "consertou" to "verbo", "escreveu" to "verbo",
        "aprendeu" to "verbo", "plantou" to "verbo", "acelerou" to "verbo", "brincou" to "verbo",
        "tocou" to "verbo", "marcou" to "verbo", "ganhou" to "verbo", "receitou" to "verbo",
        "prendeu" to "verbo", "revisou" to "verbo", "misturou" to "verbo", "levantou" to "verbo",
        "respondeu" to "verbo", "atendeu" to "verbo", "protegeu" to "verbo", "iniciou" to "verbo",
        "calculou" to "verbo", "imaginou" to "verbo", "compôs" to "verbo", "verificou" to "verbo",
        "treinou" to "verbo", "interrogou" to "verbo", "comemorou" to "verbo", "trocou" to "verbo",
        "regou" to "verbo", "avaliou" to "verbo", "seguiu" to "verbo", "afinou" to "verbo",
        "observou" to "verbo", "nadar" to "verbo"
    )

    private val mapaObjetosDiretos = mapOf(
        "Maria comprou pão na padaria" to "pão",
        "O detetive analisou as pistas" to "pistas",
        "A professora explicou a matéria" to "matéria",
        "O jogador venceu a partida" to "partida",
        "A criança abriu o presente" to "presente",
        "O cientista descobriu uma nova estrela" to "estrela",
        "O aluno resolveu o problema" to "problema",
        "O escritor terminou o livro" to "livro",
        "O médico examinou o paciente" to "paciente",
        "O vento forte derrubou a árvore" to "árvore",
        "A menina desenhou uma casa" to "casa",
        "O professor corrigiu a prova" to "prova",
        "O artista pintou um quadro" to "quadro",
        "O programador criou um aplicativo" to "aplicativo",
        "A equipe venceu o campeonato" to "campeonato",
        "O fazendeiro colheu o milho" to "milho",
        "O policial investigou o caso" to "caso",
        "O garoto encontrou uma moeda" to "moeda",
        "A cozinheira preparou o jantar" to "jantar",
        "O pássaro construiu um ninho" to "ninho",
        "O engenheiro projetou a ponte" to "ponte",
        "O piloto pousou o avião" to "avião",
        "O aluno fez a tarefa" to "tarefa",
        "O escritor publicou um artigo" to "artigo",
        "A cidade recebeu turistas" to "turistas",
        "O cientista estudou o vírus" to "vírus",
        "O jogador chutou a bola" to "bola",
        "O médico salvou o paciente" to "paciente",
        "O professor ensinou matemática" to "matemática",
        "O cachorro perseguiu o gato" to "gato",
        "A menina leu um livro" to "livro",
        "O atleta correu a maratona" to "maratona",
        "O mecânico consertou o carro" to "carro",
        "O jornalista escreveu a notícia" to "notícia",
        "O detetive resolveu o mistério" to "mistério",
        "O garoto aprendeu a nadar" to "nadar",
        "O agricultor plantou sementes" to "sementes",
        "O piloto acelerou o carro" to "carro",
        "O professor explicou a lição" to "lição",
        "O cientista analisou os dados" to "dados",
        "O músico tocou violão" to "violão",
        "O jogador marcou um gol" to "gol",
        "A menina ganhou um presente" to "presente",
        "O médico receitou o remédio" to "remédio",
        "O policial prendeu o ladrão" to "ladrão",
        "O escritor revisou o texto" to "texto",
        "O pintor misturou as cores" to "cores",
        "O garoto chutou a bola" to "bola",
        "A professora leu a história" to "história",
        "O atleta levantou o peso" to "peso",
        "O cientista fez um experimento" to "experimento",
        "O detetive encontrou pistas" to "pistas",
        "O aluno respondeu a pergunta" to "pergunta",
        "O médico atendeu o paciente" to "paciente",
        "O policial protegeu a cidade" to "cidade",
        "O professor iniciou a aula" to "aula",
        "O engenheiro calculou a estrutura" to "estrutura",
        "O escritor imaginou personagens" to "personagens",
        "O músico compôs uma melodia" to "melodia",
        "O estudante fez anotações" to "anotações",
        "O piloto verificou os motores" to "motores",
        "O programador escreveu código" to "código",
        "O cientista publicou pesquisa" to "pesquisa",
        "O garoto abriu a porta" to "porta",
        "O professor explicou o conceito" to "conceito",
        "O detetive interrogou o suspeito" to "suspeito",
        "O jogador comemorou a vitória" to "vitória",
        "A menina escreveu uma carta" to "carta",
        "O cientista estudou o planeta" to "planeta",
        "O mecânico trocou o pneu" to "pneu",
        "O agricultor regou a plantação" to "plantação",
        "O médico analisou exames" to "exames",
        "O aluno fez uma pergunta" to "pergunta",
        "O professor avaliou o trabalho" to "trabalho",
        "O atleta ganhou a corrida" to "corrida",
        "O escritor publicou um conto" to "conto",
        "O cientista fez uma descoberta" to "descoberta",
        "O detetive seguiu a pista" to "pista",
        "O músico afinou o instrumento" to "instrumento",
        "O garoto encontrou um mapa" to "mapa",
        "O piloto iniciou a viagem" to "viagem",
        "O médico observou os sintomas" to "sintomas",
        "O aluno terminou o exercício" to "exercício"
    )

    private val mapaSinonimos = mapOf(
        "aluno" to listOf("estudante"),
        "professor" to listOf("mestre", "docente", "educador"),
        "professora" to listOf("mestra", "docente", "educadora"),
        "felicidade" to listOf("alegria", "contentamento"),
        "bonito" to listOf("belo", "lindo"),
        "carro" to listOf("automóvel", "veículo"),
        "casa" to listOf("residência", "moradia"),
        "garoto" to listOf("menino", "rapaz"),
        "menina" to listOf("garota", "moça"),
        "cachorro" to listOf("cão"),
        "presente" to listOf("brinde", "mimo"),
        "problema" to listOf("questão", "adversidade", "dificuldade"),
        "ajuda" to listOf("auxílio", "socorro", "assistência"),
        "vencer" to listOf("ganhar", "triunfar"),
        "terminar" to listOf("finalizar", "concluir", "encerrar"),
        "rápido" to listOf("veloz", "ligeiro"),
        "forte" to listOf("robusto", "vigoroso", "potente"),
        "mistério" to listOf("enigma", "segredo"),
        "história" to listOf("conto", "narrativa", "relato"),
        "vitória" to listOf("triunfo", "conquista"),
        "exercício" to listOf("atividade", "tarefa", "treino"),
        "trabalho" to listOf("serviço", "ofício"),
        "pesquisa" to listOf("investigação", "estudo", "exame"),
        "sintoma" to listOf("indício", "sinal"),
        "pistas" to listOf("indícios"),
        "iniciou" to listOf("começou"),
        "encontrou" to listOf("achou"),
        "verificou" to listOf("conferiu")
    )

    private fun obterPlural(palavra: String): String {
        return mapaPlurais.getOrDefault(palavra.lowercase(), palavra + "s")
    }

    fun gerar(atributo: Int): PortugueseQuestion {

        val tipo = PortugueseExerciseSelector.escolher(atributo)

        val frase = if (tipo == PortugueseExerciseType.OBJETO_DIRETO) {
            PortugueseSentenceRepository.frases.filter { mapaObjetosDiretos.containsKey(it) }.random()
        } else {
            PortugueseSentenceRepository.frases.random()
        }

        val palavras = frase
            .lowercase()
            .replace(Regex("[^a-záéíóúâêôãõç ]"), "")
            .split(" ")
            .filter { it.isNotBlank() }

        return when (tipo) {

            PortugueseExerciseType.SILABAS -> {
                val palavra = palavras.randomOrNull() ?: "palavra"
                PortugueseQuestion(
                    "Quantas sílabas tem a palavra: $palavra ?",
                    contarSilabas(palavra).toString(),
                    "Conte os sons vocálicos.",
                    1,
                    tipo
                )
            }

            PortugueseExerciseType.PLURAL -> {
                // Filtra apenas palavras que não estão no plural (evita redundância)
                val palavrasValidas = palavras.filter { obterPlural(it) != it }
                val palavra = palavrasValidas.randomOrNull() ?: "palavra"
                val plural = obterPlural(palavra)
                PortugueseQuestion(
                    "Qual é o plural de: $palavra ?",
                    plural,
                    "Identifique a forma plural correta.",
                    10,
                    tipo
                )
            }

            PortugueseExerciseType.GENERO -> {
                val artigos = listOf("o", "a", "os", "as", "um", "uma", "uns", "umas", "no", "na", "nos", "nas", "do", "da", "dos", "das", "com", "para", "em", "de", "e")
                
                // Filtra as palavras para garantir que apenas substantivos/adjetivos com gênero fixo sejam sorteados.
                // Exclui verbos (ex: analisou, salvou), preposições e substantivos comuns de dois gêneros.
                val palavrasValidas = palavras.filter { 
                    it !in artigos && 
                    mapaGeneros[it] != "comum de dois gêneros" &&
                    mapaClasses[it] != "verbo" &&
                    mapaClasses[it] != "advérbio" &&
                    mapaClasses[it] != "preposição" &&
                    mapaClasses[it] != "conjunção"
                }
                val palavra = palavrasValidas.randomOrNull() ?: "casa"

                val genero = mapaGeneros[palavra] ?: if (palavra.endsWith("a") || palavra.endsWith("as")) "feminino" else "masculino"
                PortugueseQuestion(
                    "A palavra \"$palavra\" está no masculino ou feminino?",
                    genero,
                    "Identifique o gênero gramatical correto.",
                    20,
                    tipo
                )
            }

            PortugueseExerciseType.SUJEITO -> {
                val primeira = palavras.getOrNull(0) ?: "indefinido"
                val artigos = listOf("o", "a", "os", "as", "um", "uma", "uns", "umas")
                val sujeito = if (artigos.contains(primeira)) palavras.getOrNull(1) ?: primeira else primeira

                PortugueseQuestion(
                    "Qual é o sujeito da frase:\n\n\"$frase\"",
                    sujeito,
                    "O sujeito pratica a ação.",
                    30,
                    tipo
                )
            }

            PortugueseExerciseType.VERBO -> {
                val verbo = palavras.firstOrNull { mapaPlurais[it]?.endsWith("m") == true } 
                    ?: palavras.getOrNull(1) 
                    ?: "indefinido"

                PortugueseQuestion(
                    "Qual é o verbo da frase:\n\n\"$frase\"",
                    verbo,
                    "O verbo indica ação.",
                    40,
                    tipo
                )
            }

            PortugueseExerciseType.OBJETO_DIRETO -> {
                val objeto = mapaObjetosDiretos[frase] ?: "indefinido"
                PortugueseQuestion(
                    "Qual palavra recebe a ação do verbo na frase:\n\n\"$frase\"",
                    objeto,
                    "Objeto direto recebe a ação.",
                    50,
                    tipo
                )
            }

            PortugueseExerciseType.CLASSE_GRAMATICAL -> {
                val palavra = palavras.randomOrNull() ?: "palavra"
                val classe = mapaClasses[palavra] ?: "substantivo"
                PortugueseQuestion(
                    "A palavra \"$palavra\" é qual classe gramatical?",
                    classe,
                    "Identifique se é substantivo, verbo, adjetivo, artigo, etc.",
                    60,
                    tipo
                )
            }

            PortugueseExerciseType.ACENTUACAO -> {
                val palavra = palavras.randomOrNull() ?: "palavra"
                val temAcento = if (palavra.any { "áéíóúâêôãõ".contains(it) }) "sim" else "não"
                PortugueseQuestion(
                    "A palavra \"$palavra\" possui acento?",
                    temAcento,
                    "Observe a acentuação gráfica.",
                    70,
                    tipo
                )
            }

            PortugueseExerciseType.INTERPRETACAO -> {
                val primeira = palavras.getOrNull(0) ?: "indefinido"
                val artigos = listOf("o", "a", "os", "as", "um", "uma", "uns", "umas")
                val agente = if (artigos.contains(primeira)) palavras.getOrNull(1) ?: primeira else primeira
                
                PortugueseQuestion(
                    "Leia a frase:\n\n\"$frase\"\n\nQuem realizou a ação?",
                    agente,
                    "Interpretação da frase.",
                    80,
                    tipo
                )
            }

            PortugueseExerciseType.VOCABULARIO -> {
                // Filtra apenas palavras que possuem sinônimos mapeados
                val palavrasValidas = palavras.filter { mapaSinonimos.containsKey(it) }
                val palavra = palavrasValidas.randomOrNull() ?: "aluno"
                
                // Escolhe um dos sinônimos mapeados aleatoriamente como a resposta principal
                val sinonimos = mapaSinonimos[palavra] ?: listOf("estudante")
                val resposta = sinonimos.random()
                
                PortugueseQuestion(
                    "Digite um sinônimo simples para: $palavra",
                    resposta,
                    "Exemplos de sinônimos: ${sinonimos.joinToString(", ")}.",
                    90,
                    tipo
                )
            }

            PortugueseExerciseType.ORTOGRAFIA -> {
                val desafio = PortugueseOrthographyRepository.desafios.random()
                val aCerta = (1..2).random() == 1
                val questao = "Qual está correta?\n\n" + 
                    (if (aCerta) "A) ${desafio.correct}\nB) ${desafio.wrong}" 
                     else "A) ${desafio.wrong}\nB) ${desafio.correct}")
                
                PortugueseQuestion(
                    questao,
                    desafio.correct,
                    desafio.tip,
                    45,
                    tipo
                )
            }
        }
    }

    fun validarSinonimo(pergunta: PortugueseQuestion, respostaUser: String): Boolean {
        if (pergunta.tipo == PortugueseExerciseType.VOCABULARIO) {
            // Extrai a palavra original da pergunta (está após o ": ")
            val palavraOriginal = pergunta.pergunta.substringAfter(": ").lowercase()
            val sinonimos = mapaSinonimos[palavraOriginal] ?: return false
            return sinonimos.any { it.equals(respostaUser.trim(), ignoreCase = true) }
        }

        if (pergunta.tipo == PortugueseExerciseType.ORTOGRAFIA) {
            val resp = respostaUser.trim().uppercase()
            if (resp == "A" || resp == "B") {
                // Verifica se a letra escolhida contém a palavra correta na pergunta original
                return pergunta.pergunta.split("\n")
                    .any { it.startsWith("$resp)") && it.contains(pergunta.respostaCorreta, ignoreCase = true) }
            }
        }
        
        return false
    }

    private val mapaSilabas = mapOf(
        "que" to 1,
        "aquele" to 3,
        "pão" to 1, "mão" to 1, "pneu" to 1, "leu" to 1, "gol" to 1, "com" to 1, "o" to 1, "a" to 1, "os" to 1, "as" to 1, "um" to 1, "e" to 1,
        "aula" to 2, "peixe" to 2, "noite" to 2, "viagem" to 3, "avião" to 3, "lição" to 2, "ladrão" to 2, "violão" to 3,
        "plantação" to 3, "anotações" to 4, "notícia" to 3, "mistério" to 3, "remédio" to 3, "história" to 3, "vitória" to 3,
        "exercício" to 4, "matéria" to 3, "venceu" to 2, "abriu" to 2, "explicou" to 3, "resolveu" to 3, "terminou" to 3,
        "estudou" to 3, "chutou" to 2, "correu" to 2, "aprendeu" to 3, "brincou" to 2, "tocou" to 2, "ganhou" to 2,
        "receitou" to 3, "revisou" to 3, "misturou" to 3, "levantou" to 3, "regou" to 2, "seguiu" to 2, "afinou" to 3,
        "observou" to 3, "atendeu" to 3, "comprou" to 2, "analisou" to 4, "descobriu" to 3, "passou" to 2, "examinou" to 4,
        "derrubou" to 3, "desenhou" to 3, "corrigiu" to 3, "parou" to 2, "pintou" to 2, "criou" to 2, "colheu" to 2,
        "investigou" to 4, "encontrou" to 3, "preparou" to 3, "construiu" to 3, "projetou" to 3, "pousou" to 2,
        "publicou" to 3, "recebeu" to 3, "salvou" to 2, "ensinou" to 3, "perseguiu" to 3, "consertou" to 3,
        "escreveu" to 3, "plantou" to 2, "acelerou" to 4, "marcou" to 2, "prendeu" to 2, "respondeu" to 3,
        "protegeu" to 3, "iniciou" to 4, "calculou" to 3, "imaginou" to 4, "compôs" to 2, "verificou" to 4,
        "treinou" to 2, "interrogou" to 4, "comemorou" to 4, "trocou" to 2, "avaliou" to 4, "pergunta" to 3,
        "gato" to 2, "preto" to 2, "dorme" to 2, "sofá" to 2, "padaria" to 4, "pistas" to 2, "criança" to 3,
        "estrela" to 3, "aluno" to 3, "problema" to 3, "carro" to 2, "escritor" to 3, "médico" to 3,
        "paciente" to 3, "árvore" to 3, "prova" to 2, "sinal" to 2, "artista" to 3, "quadro" to 2,
        "milho" to 2, "moeda" to 3, "jantar" to 2, "ninho" to 2, "ponte" to 2, "tarefa" to 3, "cidade" to 3,
        "turistas" to 3, "vírus" to 2, "bola" to 2, "matemática" to 5, "maratona" to 4, "mecânico" to 4,
        "jornalista" to 4, "nadar" to 2, "sementes" to 3, "parque" to 2, "dados" to 2, "músico" to 3,
        "estudante" to 4, "texto" to 2, "pintor" to 2, "cores" to 2, "atleta" to 3, "peso" to 2,
        "experimento" to 5, "estrutura" to 4, "personagens" to 4, "melodia" to 4, "motores" to 3,
        "código" to 3, "pesquisa" to 3, "intensamente" to 5, "porta" to 2, "conceito" to 3, "suspeito" to 3,
        "vitória" to 3, "carta" to 2, "planeta" to 3, "pneu" to 1, "exames" to 3, "trabalho" to 3,
        "corrida" to 3, "conto" to 2, "descoberta" to 4, "instrumento" to 4, "mapa" to 2, "sintomas" to 3,
        "exercício" to 4
    )

    private fun contarSilabas(palavra: String): Int {
        val lower = palavra.lowercase()
        if (mapaSilabas.containsKey(lower)) return mapaSilabas[lower]!!

        val vogais = "aeiouáéíóúâêôãõ"
        var contador = 0

        for (letra in lower) {
            if (vogais.contains(letra)) contador++
        }

        return if (contador == 0) 1 else contador
    }
}