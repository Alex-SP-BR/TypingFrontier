package com.typingfrontier.collection

import com.typingfrontier.R

object CollectionRepository {

    private val avataresNivel = listOf(
        // Nível 10
        Avatar("lvl10_m", "Novato", "Masculino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_10_masculino, 10, 1),
        Avatar("lvl10_f", "Novata", "Feminino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_10_feminino, 10, 1),
        
        // Nível 20
        Avatar("lvl20_m", "Explorador", "Masculino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_20_masculino, 20, 2),
        Avatar("lvl20_f", "Exploradora", "Feminino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_20_feminino, 20, 2),
        
        // Nível 30
        Avatar("lvl30_m", "Veterano", "Masculino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_30_masculino, 30, 3),
        Avatar("lvl30_f", "Veterana", "Feminino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_30_feminino, 30, 3),
        
        // Nível 40
        Avatar("lvl40_m", "Elite", "Masculino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_40_masculino, 40, 4),
        Avatar("lvl40_f", "Elite", "Feminino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_40_feminino, 40, 4),
        
        // Nível 50
        Avatar("lvl50_m", "Lenda", "Masculino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_50_masculino, 50, 5),
        Avatar("lvl50_f", "Lenda", "Feminino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_50_feminino, 50, 5),

        // Nível 100
        Avatar("lvl100_m", "Mestre", "Masculino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_100_masculino, 100, 5),
        Avatar("lvl100_f", "Mestra", "Feminino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_100_feminino, 100, 5),

        // Nível 150
        Avatar("lvl150_m", "Herói", "Masculino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_150_masculino, 150, 6),
        Avatar("lvl150_f", "Heroína", "Feminino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_150_feminino, 150, 6),

        // Nível 200
        Avatar("lvl200_m", "Guardião", "Masculino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_200_masculino, 200, 7),
        Avatar("lvl200_f", "Guardiã", "Feminino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_200_feminino, 200, 7),

        // Nível 300
        Avatar("lvl300_m", "Soberano", "Masculino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_300_masculino, 300, 8),
        Avatar("lvl300_f", "Soberana", "Feminino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_300_feminino, 300, 8),

        // Nível 500
        Avatar("lvl500_m", "Imortal", "Masculino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_500_masculino, 500, 9),
        Avatar("lvl500_f", "Imortal", "Feminino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_500_feminino, 500, 9),

        // Nível 1000
        Avatar("lvl1000_m", "Divino", "Masculino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_1000_masculino, 1000, 10),
        Avatar("lvl1000_f", "Divina", "Feminino", CollectionCategory.NIVEL, R.drawable.avatar_nivel_1000_feminino, 1000, 10)
    )

    private val avataresAdmin = listOf(
        // MODERATOR
        Avatar("adm_mod_m1", "Sentinela", "Masculino", CollectionCategory.ADMINISTRATIVO, R.drawable.admin_mod_m1, 1, 0, "moderator"),
        Avatar("adm_mod_m2", "Guardião", "Masculino", CollectionCategory.ADMINISTRATIVO, R.drawable.admin_mod_m2, 1, 0, "moderator"),
        Avatar("adm_mod_f1", "Sentinela", "Feminino", CollectionCategory.ADMINISTRATIVO, R.drawable.admin_mod_f1, 1, 0, "moderator"),
        Avatar("adm_mod_f2", "Guardiã", "Feminino", CollectionCategory.ADMINISTRATIVO, R.drawable.admin_mod_f2, 1, 0, "moderator"),
        
        // SENIOR_MODERATOR
        Avatar("adm_smod_m1", "Vigilante", "Masculino", CollectionCategory.ADMINISTRATIVO, R.drawable.admin_smod_m1, 1, 0, "senior_moderator"),
        Avatar("adm_smod_m2", "Comandante", "Masculino", CollectionCategory.ADMINISTRATIVO, R.drawable.admin_smod_m2, 1, 0, "senior_moderator"),
        Avatar("adm_smod_f1", "Vigilante", "Feminino", CollectionCategory.ADMINISTRATIVO, R.drawable.admin_smod_f1, 1, 0, "senior_moderator"),
        Avatar("adm_smod_f2", "Comandante", "Feminino", CollectionCategory.ADMINISTRATIVO, R.drawable.admin_smod_f2, 1, 0, "senior_moderator"),
        
        // ADMINISTRATOR
        Avatar("adm_admin_m1", "Fundador", "Masculino", CollectionCategory.ADMINISTRATIVO, R.drawable.admin_admin_m1, 1, 0, "administrator"),
        Avatar("adm_admin_m2", "Líder", "Masculino", CollectionCategory.ADMINISTRATIVO, R.drawable.admin_admin_m2, 1, 0, "administrator")
    )

    private val avataresComerciais = listOf(
        // 01-05: Frons OU Ads
        Avatar("comercial_01_masculino", "Coleção #01", "Masculino", CollectionCategory.COLECAO, R.drawable.comercial_01_masculino, 1, 5, precoFrons = 5000),
        Avatar("comercial_01_feminino", "Coleção #01", "Feminino", CollectionCategory.COLECAO, R.drawable.comercial_01_feminino, 1, 5, precoFrons = 5000),
        Avatar("comercial_02_masculino", "Coleção #02", "Masculino", CollectionCategory.COLECAO, R.drawable.comercial_02_masculino, 1, 10, precoFrons = 10000),
        Avatar("comercial_02_feminino", "Coleção #02", "Feminino", CollectionCategory.COLECAO, R.drawable.comercial_02_feminino, 1, 10, precoFrons = 10000),
        Avatar("comercial_03_masculino", "Coleção #03", "Masculino", CollectionCategory.COLECAO, R.drawable.comercial_03_masculino, 1, 15, precoFrons = 20000),
        Avatar("comercial_03_feminino", "Coleção #03", "Feminino", CollectionCategory.COLECAO, R.drawable.comercial_03_feminino, 1, 15, precoFrons = 20000),
        Avatar("comercial_04_masculino", "Coleção #04", "Masculino", CollectionCategory.COLECAO, R.drawable.comercial_04_masculino, 1, 20, precoFrons = 35000),
        Avatar("comercial_04_feminino", "Coleção #04", "Feminino", CollectionCategory.COLECAO, R.drawable.comercial_04_feminino, 1, 20, precoFrons = 35000),
        Avatar("comercial_05_masculino", "Coleção #05", "Masculino", CollectionCategory.COLECAO, R.drawable.comercial_05_masculino, 1, 25, precoFrons = 50000),
        Avatar("comercial_05_feminino", "Coleção #05", "Feminino", CollectionCategory.COLECAO, R.drawable.comercial_05_feminino, 1, 25, precoFrons = 50000),
        
        // 06-10: SOMENTE Frons
        Avatar("comercial_06_masculino", "Coleção #06", "Masculino", CollectionCategory.COLECAO, R.drawable.comercial_06_masculino, 1, 0, precoFrons = 100000),
        Avatar("comercial_06_feminino", "Coleção #06", "Feminino", CollectionCategory.COLECAO, R.drawable.comercial_06_feminino, 1, 0, precoFrons = 100000),
        Avatar("comercial_07_masculino", "Coleção #07", "Masculino", CollectionCategory.COLECAO, R.drawable.comercial_07_masculino, 1, 0, precoFrons = 250000),
        Avatar("comercial_07_feminino", "Coleção #07", "Feminino", CollectionCategory.COLECAO, R.drawable.comercial_07_feminino, 1, 0, precoFrons = 250000),
        Avatar("comercial_08_masculino", "Coleção #08", "Masculino", CollectionCategory.COLECAO, R.drawable.comercial_08_masculino, 1, 0, precoFrons = 750000),
        Avatar("comercial_08_feminino", "Coleção #08", "Feminino", CollectionCategory.COLECAO, R.drawable.comercial_08_feminino, 1, 0, precoFrons = 750000),
        Avatar("comercial_09_masculino", "Coleção #09", "Masculino", CollectionCategory.COLECAO, R.drawable.comercial_09_masculino, 1, 0, precoFrons = 2500000),
        Avatar("comercial_09_feminino", "Coleção #09", "Feminino", CollectionCategory.COLECAO, R.drawable.comercial_09_feminino, 1, 0, precoFrons = 2500000),
        Avatar("comercial_10_masculino", "Coleção #10", "Masculino", CollectionCategory.COLECAO, R.drawable.comercial_10_masculino, 1, 0, precoFrons = 10000000),
        Avatar("comercial_10_feminino", "Coleção #10", "Feminino", CollectionCategory.COLECAO, R.drawable.comercial_10_feminino, 1, 0, precoFrons = 10000000)
    )

    private val conquistas = listOf(
        // 🌍 EXPLORAÇÃO (16)
        Achievement("exp_1", "Primeira Aventura Concluída", "Vença as 5 etapas de sua primeira zona de exploração.", CollectionCategory.EXPLORACAO, "Concluir 5 etapas de 1 zona", R.drawable.insignia_exp_1, 50),
        Achievement("exp_2", "Segunda Aventura Concluída", "Vença as 5 etapas de sua segunda zona de exploração única.", CollectionCategory.EXPLORACAO, "Concluir 5 etapas de 2 zonas", R.drawable.insignia_exp_2, 100),
        Achievement("exp_3", "Terceira Aventura Concluída", "Vença as 5 etapas de sua terceira zona de exploração única.", CollectionCategory.EXPLORACAO, "Concluir 5 etapas de 3 zonas", R.drawable.insignia_exp_3, 200),
        Achievement("exp_4", "Aventura Avançada Concluída", "Vença as 5 etapas de sua quarta zona de exploração única.", CollectionCategory.EXPLORACAO, "Concluir 5 etapas de 4 zonas", R.drawable.insignia_exp_4, 400),
        Achievement("exp_5", "Grande Explorador", "Vença as 5 etapas de sua quinta zona de exploração única.", CollectionCategory.EXPLORACAO, "Concluir 5 etapas de 5 zonas", R.drawable.insignia_exp_5, 800),
        Achievement("exp_6", "Exploração Completa", "Vença as 5 etapas de sua sexta zona de exploração única.", CollectionCategory.EXPLORACAO, "Concluir 5 etapas de 6 zonas", R.drawable.insignia_exp_6, 1600),
        Achievement("exp_7", "Nova Região Descoberta", "Vença as 5 etapas de sua sétima zona de exploração única.", CollectionCategory.EXPLORACAO, "Concluir 5 etapas de 7 zonas", R.drawable.insignia_exp_7, 3200),
        Achievement("exp_8", "Jornada Concluída", "Desbloqueie o acesso a todas as zonas iniciais.", CollectionCategory.EXPLORACAO, "Complete todos os locais", R.drawable.insignia_exp_8, 5000),
        Achievement("exp_9", "Explorador Veterano", "Mostre constância em suas viagens.", CollectionCategory.EXPLORACAO, "Progresso avançado", R.drawable.insignia_exp_9, 6000),
        Achievement("exp_10", "Mestre Explorador", "Domine a arte da exploração urbana.", CollectionCategory.EXPLORACAO, "Mestre da zona", R.drawable.insignia_exp_10, 7500),
        Achievement("exp_11", "Estação Ferroviária", "Encontre e explore as redondezas da estação.", CollectionCategory.EXPLORACAO, "Explorar estação", R.drawable.insignia_exp_11, 8000),
        Achievement("exp_12", "Trem de Alta Velocidade Desbloqueado", "Alcance a maestria necessária para viagens rápidas.", CollectionCategory.EXPLORACAO, "Desbloquear trem", R.drawable.insignia_exp_12, 10000),
        Achievement("exp_13", "Viajante Experiente", "Conheça cada beco e avenida.", CollectionCategory.EXPLORACAO, "Viajante", R.drawable.insignia_exp_13, 12000),
        Achievement("exp_14", "Grande Jornada", "Uma vida dedicada a descobrir o novo.", CollectionCategory.EXPLORACAO, "Explorador de elite", R.drawable.insignia_exp_14, 15000),
        Achievement("exp_15", "Conquista Máxima da Exploração", "Reconhecimento oficial por explorar tudo.", CollectionCategory.EXPLORACAO, "Lendário", R.drawable.insignia_exp_15, 25000),
        Achievement("exp_16", "Símbolo Supremo da Exploração", "A insígnia definitiva para o maior dos viajantes.", CollectionCategory.EXPLORACAO, "Supremo", R.drawable.insignia_exp_16, 50000),

        // 🏋️ TREINAMENTO FÍSICO (Equilíbrio) (16)
        Achievement("fis_10", "Equilíbrio Inicial", "Alcance nível 5 em Força, Velocidade e Resistência.", CollectionCategory.TREINO_FISICO, "Atributos Físicos Lv 5", R.drawable.insignia_fis_10, 100),
        Achievement("fis_10_2", "Equilíbrio Nível 10", "Alcance nível 10 em todos os atributos físicos.", CollectionCategory.TREINO_FISICO, "Atributos Físicos Lv 10", R.drawable.insignia_fis_10_2, 500),
        Achievement("fis_30", "Equilíbrio Nível 30", "Alcance nível 30 em todos os atributos físicos.", CollectionCategory.TREINO_FISICO, "Atributos Físicos Lv 30", R.drawable.insignia_fis_30, 2000),
        Achievement("fis_50", "Equilíbrio Nível 50", "Alcance nível 50 em todos os atributos físicos.", CollectionCategory.TREINO_FISICO, "Atributos Físicos Lv 50", R.drawable.insignia_fis_50, 5000),
        Achievement("fis_100", "Equilíbrio Nível 100", "Alcance nível 100 em todos os atributos físicos.", CollectionCategory.TREINO_FISICO, "Atributos Físicos Lv 100", R.drawable.insignia_fis_100, 10000),
        Achievement("fis_200", "Equilíbrio Nível 200", "Alcance nível 200 em todos os atributos físicos.", CollectionCategory.TREINO_FISICO, "Atributos Físicos Lv 200", R.drawable.insignia_fis_200, 25000),
        Achievement("fis_300", "Equilíbrio Nível 300", "Alcance nível 300 em todos os atributos físicos.", CollectionCategory.TREINO_FISICO, "Atributos Físicos Lv 300", R.drawable.insignia_fis_300, 50000),
        Achievement("fis_500", "Equilíbrio Nível 500", "Alcance nível 500 em todos os atributos físicos.", CollectionCategory.TREINO_FISICO, "Atributos Físicos Lv 500", R.drawable.insignia_fis_500, 100000),
        Achievement("fis_1000", "Equilíbrio Nível 1.000", "Alcance nível 1.000 em todos os atributos físicos.", CollectionCategory.TREINO_FISICO, "Atributos Físicos Lv 1.000", R.drawable.insignia_fis_1000, 500000),
        Achievement("fis_consistente", "Treinamento Consistente", "Mostre sua rotina de atleta.", CollectionCategory.TREINO_FISICO, "Físico Lv 15", R.drawable.insignia_fis_consistente, 1000),
        Achievement("fis_disciplina", "Disciplina", "Nunca falhe na sua rotina.", CollectionCategory.TREINO_FISICO, "Físico Lv 25", R.drawable.insignia_fis_disciplina, 2500),
        Achievement("fis_evolucao", "Evolução Física", "Seu corpo atingiu um novo patamar.", CollectionCategory.TREINO_FISICO, "Físico Lv 75", R.drawable.insignia_fis_evolucao, 7500),
        Achievement("fis_equilibrado", "Atleta Equilibrado", "Mantenha seus atributos em harmonia.", CollectionCategory.TREINO_FISICO, "Harmonia física", R.drawable.insignia_fis_equilibrado, 15000),
        Achievement("fis_mestre", "Mestre do Treinamento", "Lidere pelo exemplo físico.", CollectionCategory.TREINO_FISICO, "Mestre Físico", R.drawable.insignia_fis_mestre, 30000),
        Achievement("fis_raro", "Conquista Física Extremamente Rara", "Um corpo que desafia os limites humanos.", CollectionCategory.TREINO_FISICO, "Lendário Físico", R.drawable.insignia_fis_raro, 75000),
        Achievement("fis_supremo", "Símbolo Supremo da Evolução Física", "O ápice do desenvolvimento corporal.", CollectionCategory.TREINO_FISICO, "Supremo Físico", R.drawable.insignia_fis_supremo, 150000),

        // 🧠 TREINAMENTO MENTAL (Streaks e Maestria) (16)
        Achievement("men_1", "Primeira Sequência de Acertos", "Acerte 5 questões consecutivas no treino mental.", CollectionCategory.TREINO_MENTAL, "Streak de 5", R.drawable.insignia_men_1, 50),
        Achievement("men_10", "10 Respostas Corretas", "Acerte 10 questões consecutivas.", CollectionCategory.TREINO_MENTAL, "Streak de 10", R.drawable.insignia_men_10, 200),
        Achievement("men_20", "20 Respostas Corretas", "Acerte 20 questões consecutivas.", CollectionCategory.TREINO_MENTAL, "Streak de 20", R.drawable.insignia_men_20, 500),
        Achievement("men_30", "30 Respostas Corretas", "Acerte 30 questões consecutivas.", CollectionCategory.TREINO_MENTAL, "Streak de 30", R.drawable.insignia_men_30, 1000),
        Achievement("men_50", "50 Respostas Corretas", "Acerte 50 questões consecutivas.", CollectionCategory.TREINO_MENTAL, "Streak de 50", R.drawable.insignia_men_50, 2500),
        Achievement("men_100", "100 Respostas Corretas", "Acerte 100 questões consecutivas.", CollectionCategory.TREINO_MENTAL, "Streak de 100", R.drawable.insignia_men_100, 5000),
        Achievement("men_200", "200 Respostas Corretas", "Acerte 200 questões consecutivas.", CollectionCategory.TREINO_MENTAL, "Streak de 200", R.drawable.insignia_men_200, 12000),
        Achievement("men_300", "300 Respostas Corretas", "Acerte 300 questões consecutivas.", CollectionCategory.TREINO_MENTAL, "Streak de 300", R.drawable.insignia_men_300, 25000),
        Achievement("men_500", "500 Respostas Corretas", "Acerte 500 questões consecutivas.", CollectionCategory.TREINO_MENTAL, "Streak de 500", R.drawable.insignia_men_500, 75000),
        Achievement("men_1000", "1.000 Respostas Corretas", "Acerte 1.000 questões consecutivas.", CollectionCategory.TREINO_MENTAL, "Streak de 1.000", R.drawable.insignia_men_1000, 250000),
        Achievement("men_math", "Mestre da Matemática", "Alcance nível 50 em Inteligência.", CollectionCategory.TREINO_MENTAL, "Inteligência Lv 50", R.drawable.insignia_men_math, 10000),
        Achievement("men_port", "Mestre do Português", "Alcance nível 50 em Carisma.", CollectionCategory.TREINO_MENTAL, "Carisma Lv 50", R.drawable.insignia_men_port, 10000),
        Achievement("men_knowledge", "Mestre do Conhecimento", "Alcance nível 100 em ambos os atributos mentais.", CollectionCategory.TREINO_MENTAL, "Mental Lv 100", R.drawable.insignia_men_knowledge, 30000),
        Achievement("men_focus", "Concentração Excepcional", "Mantenha o foco absoluto.", CollectionCategory.TREINO_MENTAL, " Streak de 150", R.drawable.insignia_men_focus, 20000),
        Achievement("men_precision", "Precisão Excepcional", "Nunca erre quando realmente importa.", CollectionCategory.TREINO_MENTAL, "Streak de 250", R.drawable.insignia_men_precision, 40000),
        Achievement("men_supremo", "Símbolo Supremo do Domínio Mental", "A mente mais rápida e precisa da fronteira.", CollectionCategory.TREINO_MENTAL, "Supremo Mental", R.drawable.insignia_men_supremo, 150000),

        // 💰 ECONOMIA (8)
        Achievement("eco_1", "Primeira Grande Economia", "Acumule seus primeiros 10.000 Frons.", CollectionCategory.ECONOMIA, "Saldo de 10k Frons", R.drawable.insignia_eco_1, 1000),
        Achievement("eco_100k", "100 Mil Frons", "Junte uma pequena fortuna.", CollectionCategory.ECONOMIA, "Saldo de 100k Frons", R.drawable.insignia_eco_100k, 5000),
        Achievement("eco_500k", "500 Mil Frons", "Você está ficando rico.", CollectionCategory.ECONOMIA, "Saldo de 500k Frons", R.drawable.insignia_eco_500k, 25000),
        Achievement("eco_1m", "1 Milhão Frons", "O primeiro milhão a gente nunca esquece.", CollectionCategory.ECONOMIA, "Saldo de 1M Frons", R.drawable.insignia_eco_1m, 100000),
        Achievement("eco_5m", "5 Milhões Frons", "Magnata da fronteira.", CollectionCategory.ECONOMIA, "Saldo de 5M Frons", R.drawable.insignia_eco_5m, 250000),
        Achievement("eco_10m", "10 Milhões Frons", "Dono de metade da cidade.", CollectionCategory.ECONOMIA, "Saldo de 10M Frons", R.drawable.insignia_eco_10m, 1000000),
        Achievement("eco_rich", "Grande Riqueza", "Seu nome é sinônimo de Frons.", CollectionCategory.ECONOMIA, "Acúmulo de riqueza", R.drawable.insignia_eco_rich, 500000),
        Achievement("eco_mestre", "Mestre da Economia", "Controle o mercado com maestria.", CollectionCategory.ECONOMIA, "Mestre da Fortuna", R.drawable.insignia_eco_mestre, 2000000),

        // 🏆 CONQUISTAS SUPREMAS (8)
        Achievement("sup_explora", "Explorador Supremo", "Conquiste todos os marcos de exploração.", CollectionCategory.ESPECIAL, "Domínio da Exploração", R.drawable.insignia_sup_explora, 100000),
        Achievement("sup_fisico", "Treinador Supremo", "Conquiste todos os marcos físicos.", CollectionCategory.ESPECIAL, "Domínio Físico", R.drawable.insignia_sup_fisico, 100000),
        Achievement("sup_mental", "Mestre Mental", "Conquiste todos os marcos mentais.", CollectionCategory.ESPECIAL, "Domínio Mental", R.drawable.insignia_sup_mental, 100000),
        Achievement("sup_economia", "Mestre da Economia", "Conquiste todos os marcos econômicos.", CollectionCategory.ESPECIAL, "Domínio da Riqueza", R.drawable.insignia_sup_economia, 100000),
        Achievement("sup_excelencia", "Excelência Geral", "Tenha todas as maestrias básicas.", CollectionCategory.ESPECIAL, "Mestre Geral", R.drawable.insignia_sup_excelencia, 500000),
        Achievement("sup_dedicado", "Jogador Extremamente Dedicado", "Centenas de horas dedicadas à fronteira.", CollectionCategory.ESPECIAL, "Dedicação Total", R.drawable.insignia_sup_dedicado, 1000000),
        Achievement("sup_lendaria", "Conquista Lendária", "Um feito que será lembrado por gerações.", CollectionCategory.ESPECIAL, "Lenda Viva", R.drawable.insignia_sup_lendaria, 2000000),
        Achievement("sup_maxima", "Conquista Máxima do Jogo", "Você é o mestre absoluto do Typing Frontier.", CollectionCategory.ESPECIAL, "Mestre Absoluto", R.drawable.insignia_sup_maxima, 10000000, "lvl1000_m")
    )

    fun getAvatarById(id: String?): Avatar? {
        return (avataresNivel + avataresAdmin + avataresComerciais).find { it.id == id }
    }
    
    fun getAvataresPorSexo(sexo: String): List<Avatar> {
        val list = mutableListOf<Avatar>()
        
        // 1. Progressão
        list.addAll(avataresNivel.filter { it.sexo.equals(sexo, ignoreCase = true) })
        
        // 2. Coleção (Comerciais)
        list.addAll(avataresComerciais.filter { it.sexo.equals(sexo, ignoreCase = true) })
        
        // 3. Administrativos
        val userRole = com.typingfrontier.social.SocialProfileRepository.currentProfile?.role ?: "usuario"
        list.addAll(avataresAdmin.filter { 
            it.sexo.equals(sexo, ignoreCase = true) && isRoleCompatible(userRole, it.roleRequisito)
        })
        
        return list
    }

    private fun isRoleCompatible(userRole: String, requiredRole: String?): Boolean {
        if (requiredRole == null) return true
        if (userRole == "administrator") return true 
        
        // Regra 6 simplificada: cada role tem seus avatares exclusivos.
        return userRole == requiredRole
    }

    fun getAllAvatares(): List<Avatar> = avataresNivel + avataresAdmin + avataresComerciais

    fun getAvataresPorCategoria(sexo: String, categoria: String): List<Avatar> {
        val userRole = com.typingfrontier.social.SocialProfileRepository.currentProfile?.role ?: "usuario"
        return when (categoria) {
            "PROGRESSÃO" -> avataresNivel.filter { it.sexo.equals(sexo, ignoreCase = true) }
            "COLEÇÃO" -> avataresComerciais.filter { it.sexo.equals(sexo, ignoreCase = true) }
            "ADMINISTRATIVOS" -> avataresAdmin.filter { 
                it.sexo.equals(sexo, ignoreCase = true) && isRoleCompatible(userRole, it.roleRequisito) 
            }
            else -> getAvataresPorSexo(sexo) // TODOS
        }
    }

    fun getAvatarPadrao(sexo: String): Avatar {
        return if (sexo.equals("Masculino", ignoreCase = true)) {
            Avatar("default", "Avatar Original", "Masculino", CollectionCategory.ESPECIAL, R.drawable.homem, 1, 0)
        } else {
            Avatar("default", "Avatar Original", "Feminino", CollectionCategory.ESPECIAL, R.drawable.mulher, 1, 0)
        }
    }

    fun isAvatarValidoParaPlayer(avatarId: String?, p: com.typingfrontier.Player): Boolean {
        if (avatarId == null || avatarId == "default") return true // Avatar padrão é sempre válido
        val avatar = getAvatarById(avatarId) ?: return false
        
        // 1. Sexo
        if (!avatar.sexo.equals(p.sexo, ignoreCase = true)) return false
        
        // 2. Role (para avatares administrativos)
        if (avatar.roleRequisito != null) {
            val userRole = com.typingfrontier.social.SocialProfileRepository.currentProfile?.role ?: "usuario"
            if (userRole != avatar.roleRequisito) return false
            // Avatares administrativos não exigem desbloqueio por anúncio/nível, apenas a role.
            return true
        }
        
        // 3. Desbloqueado (para avatares de nível)
        if (!p.avataresDesbloqueados.contains(avatarId)) return false
        
        return true
    }

    fun getAchievementById(id: String): Achievement? = conquistas.find { it.id == id }
    fun getAllAchievements(): List<Achievement> = conquistas
}
