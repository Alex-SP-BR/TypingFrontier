# Typing Frontier: RPG Digitação

## Autoria

**Autor e desenvolvedor:** Alex Cardoso Bento

**Projeto:** Typing Frontier: RPG Digitação

**Plataforma:** Android

**Package:** `com.typingfrontier`

---

## Desenvolvimento

O Typing Frontier: RPG Digitação é um jogo desenvolvido por Alex Cardoso Bento.

O projeto é desenvolvido e mantido utilizando controle de versão com Git e armazenamento do repositório no GitHub.

O histórico do repositório registra a evolução do desenvolvimento do software, incluindo alterações, correções, melhorias e novas funcionalidades.

---

## Controle de versão

O código-fonte do projeto é versionado por meio do Git.

O repositório remoto utilizado para armazenamento do histórico de desenvolvimento é o GitHub.

Cada commit representa uma etapa do desenvolvimento do projeto.

---

## Versão 1.4

A versão 1.4 do Typing Frontier está identificada no Git pela tag:

`v1.4`

### Identificação técnica

* **Tag:** `v1.4`
* **Commit:** `4b10078b15e1887c57aab6604186743123f0b076`
* **versionCode:** `5`
* **versionName:** `1.4`
* **Data do commit:** 27/08/2026
* **Autor do commit:** Alex SP

A tag `v1.4` foi criada para identificar especificamente o estado do código correspondente à versão 1.4 do projeto.

Foram implementados e integrados:

* Central do jogador como hub para acesso aos recursos de coleção;
* sistema de Avatares;
* Avatar Original, mantendo os personagens padrão masculino e feminino;
* desbloqueio e troca de avatares;
* equipamento de avatares já desbloqueados;
* retorno ao Avatar Original;
* avatares de progressão vinculados a níveis do jogador;
* regras de compatibilidade dos avatares com o sexo definido no cadastro;
* sistema de Conquistas;
* sistema de Insígnias associado às conquistas;
* categorias de conquistas relacionadas à exploração, treinamento físico, treinamento mental, economia e conquistas especiais;
* sistema de registro e persistência das conquistas;
* recompensas associadas às conquistas;
* associação de conquistas específicas a recompensas de avatares;
* visualização ampliada dos avatares;
* visualização ampliada das insígnias;
* separação entre visualização, equipamento e desbloqueio;
* ajuda contextual nas telas de Avatares e Conquistas;
* documentação desses sistemas no Manual do Jogador;
* ajustes visuais de posicionamento e espaçamento das telas da Central, Avatares e Conquistas.

A implementação foi realizada preservando a arquitetura existente do projeto, mantendo a compatibilidade com os sistemas de jogo, persistência de dados, economia e demais funcionalidades existentes.

As funcionalidades de visualização ampliada foram implementadas de forma independente das ações de equipamento e desbloqueio, evitando alterações acidentais no estado do jogador durante a visualização de personagens ou insígnias.

---

## Versão 1.5

A versão 1.5 do Typing Frontier representa a expansão do projeto com a implementação da camada social do jogo.

### Identificação técnica

* **Tag:** `v1.5`
* **versionCode:** `6`
* **versionName:** `1.5`
* **Data:** 2026
* **Status:** Em desenvolvimento/publicação

A versão 1.5 amplia o Typing Frontier com recursos sociais complementares ao gameplay principal, mantendo o funcionamento offline do jogo.

Foram implementados e integrados:

* integração inicial com Supabase;
* identidade social baseada em Supabase Auth;
* autenticação anônima;
* perfil social;
* username social;
* integração entre identidade social e perfil;
* Ranking Social;
* ranking por nível;
* ranking por força;
* ranking por resistência;
* ranking por velocidade;
* ranking por inteligência;
* ranking por carisma;
* ranking por aventuras concluídas;
* ranking por melhor sequência de acertos;
* limite de Top 100 no ranking;
* Mural Social associado às categorias do Ranking;
* Fórum Geral;
* criação de tópicos;
* publicação de conteúdo;
* respostas em tópicos;
* edição do próprio conteúdo;
* exclusão do próprio conteúdo;
* denúncias de conteúdo;
* sistema de moderação;
* painel administrativo;
* sistema de roles;
* roles `usuario`, `moderator`, `senior_moderator` e `administrator`;
* hierarquia administrativa protegida pelo backend;
* gerenciamento de roles pelo administrador;
* sistema de claim de denúncias;
* resolução e descarte de denúncias;
* banimento social;
* banimento temporário;
* banimento permanente;
* desbanimento;
* proteção server-side das operações administrativas;
* registro das ações administrativas em `admin_logs`;
* proteção de autoria baseada no identificador autenticado;
* integração entre Fórum, Mural e sistema de denúncias;
* preservação do funcionamento offline do gameplay;
* utilização do Supabase como camada social complementar;
* manutenção do save local como fonte principal do estado do jogo.

### Segurança da camada social

As operações sociais e administrativas utilizam validações server-side no Supabase.

O aplicativo Android atua como interface e solicitante das operações, não como autoridade administrativa.

As roles administrativas são determinadas pelo backend.

Operações sensíveis, como alteração de roles, banimento, desbanimento, resolução de denúncias e ações administrativas sobre conteúdo, permanecem protegidas por mecanismos server-side.

A camada social não substitui o save local nem interfere diretamente no funcionamento principal do jogo.

### Separação entre gameplay e camada social

O Typing Frontier mantém o princípio:

```text
Gameplay local
      ↓
funciona independentemente da internet

Camada social
      ↓
utiliza Supabase quando disponível
```

A indisponibilidade do Supabase não deve impedir o jogador de carregar seu save ou utilizar os sistemas principais do jogo.

A versão 1.5 representa uma etapa importante na evolução do projeto, adicionando recursos de interação social sem substituir a estrutura principal do jogo.

---

## Publicação

O Typing Frontier: RPG Digitação foi disponibilizado por meio do Google Play.

As versões do aplicativo enviadas ao Google Play são associadas às respectivas versões compiladas do projeto.

A versão 1.4 foi publicada no Google Play e corresponde à versão anteriormente disponibilizada aos usuários.

A versão 1.5 corresponde à evolução posterior do projeto e inclui a nova camada social e os recursos associados.

---

## Histórico de desenvolvimento

O projeto possui histórico de desenvolvimento registrado no Git, contendo commits realizados durante sua criação e evolução.

Entre os registros existentes encontram-se:

* Initial commit do projeto;
* criação da documentação;
* atualizações dos desafios de Português;
* melhorias de gameplay;
* ajustes de balanceamento;
* implementação da moeda Fron;
* melhorias de formatação;
* otimizações do processo de build;
* sincronização de metadados;
* implementação da Central;
* implementação do sistema de Avatares;
* implementação do sistema de Conquistas e Insígnias;
* implementação do sistema social;
* integração com Supabase;
* implementação do Ranking Social;
* implementação do Mural Social;
* implementação do Fórum;
* implementação de denúncias e moderação;
* implementação do sistema de roles;
* implementação de banimento e desbanimento;
* versão 1.4;
* versão 1.5.

---

## Finalidade deste documento

Este documento tem finalidade documental e registra informações relacionadas à autoria, identificação e histórico de desenvolvimento do projeto Typing Frontier: RPG Digitação.

Ele faz parte do próprio repositório do projeto e é mantido sob controle de versão do Git.

---

**Autor:** Alex Cardoso Bento

**Projeto:** Typing Frontier: RPG Digitação

**Package:** `com.typingfrontier`
