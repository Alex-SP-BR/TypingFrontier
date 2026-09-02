# TypingFrontier

🇧🇷 Português | 🇺🇸 English

---

# 🇧🇷 Português

## Sobre o projeto

**TypingFrontier** é um jogo educativo de RPG desenvolvido para Android que combina aprendizado, aventura, evolução de personagem e interação social em uma experiência gamificada.

O jogador assume o papel de um personagem que desenvolve suas habilidades através de diferentes formas de treinamento.

O jogo integra aprendizado de **Português e Matemática** como treinamento mental, além de atividades de treinamento físico para melhorar os atributos do personagem.

Durante sua jornada, o jogador pode escolher uma profissão para seu personagem, evoluir suas habilidades, explorar novos locais, enfrentar desafios e conquistar níveis dentro do universo do jogo.

O jogo também possui uma camada social integrada, permitindo que os jogadores participem de um **Ranking Social**, utilizem um **Mural Social**, participem do **Fórum**, criem discussões, respondam a outros jogadores e utilizem recursos de perfil social.

A interação acontece através de desafios, comandos digitados, escolhas do jogador e recursos de progressão, tornando o aprendizado parte da própria experiência de RPG.

O projeto foi desenvolvido utilizando **Kotlin e Android**, aplicando conceitos de desenvolvimento mobile, persistência local e integração com serviços de backend.

---

## Principais sistemas

### RPG e progressão

* RPG com evolução de personagem
* Sistema de níveis e progressão
* Escolha de profissões para o personagem
* Evolução de atributos
* Sistema de economia com Frons
* Sistema de exploração e aventura

### Treinamento

* Treinamento mental com Português e Matemática
* Treinamento físico
* Evolução de Força, Resistência e Velocidade
* Desafios baseados em comandos digitados
* Sequências de acertos e evolução de desempenho

### Avatares e Conquistas

* Sistema de Avatares
* Avatar Original masculino e feminino
* Avatares de progressão por nível
* Sistema de desbloqueio de avatares
* Sistema de Conquistas
* Sistema de Insígnias
* Recompensas associadas às conquistas
* Visualização ampliada de avatares e insígnias

### Sistema Social

* Identidade Social
* Perfil Social
* Username social
* Ranking Social
* Ranking por diferentes atributos e progressos
* Mural Social
* Fórum Geral
* Criação de tópicos
* Respostas e discussões
* Edição do próprio conteúdo
* Exclusão do próprio conteúdo
* Sistema de denúncias
* Sistema de moderação
* Roles administrativas
* Banimento social temporário e permanente
* Desbanimento
* Registro de ações administrativas

A camada social funciona como um complemento ao jogo e não substitui o funcionamento offline do gameplay principal.

---

## Arquitetura social

O TypingFrontier utiliza o **Supabase** como infraestrutura complementar para os recursos sociais.

A estrutura foi projetada mantendo o gameplay local independente da camada social.

De forma simplificada:

```text
Gameplay
   ↓
Save local
   ↓
PlayerManager
```

Enquanto os recursos sociais utilizam:

```text
Aplicativo Android
        ↓
Supabase Auth
        ↓
Perfil Social
        ↓
Ranking / Mural / Fórum
        ↓
Moderação
```

As operações administrativas e sensíveis são protegidas no backend.

O aplicativo Android atua como interface e solicitante das operações, enquanto o Supabase/PostgreSQL realiza as validações de autorização.

---

## Tecnologias utilizadas

* Kotlin
* Android SDK
* Android Studio
* Material Design
* XML / ViewBinding
* Git
* GitHub
* Supabase
* PostgreSQL

---

## Screenshots

<img width="1254" height="1254" alt="Tela do jogo" src="https://github.com/user-attachments/assets/04ff4a76-7a62-4212-a7d3-08db63e9bc49" />

<img width="1254" height="1254" alt="Tela do jogo" src="https://github.com/user-attachments/assets/74a026ab-10df-4b0c-ab7f-2f82412fb93e" />

<img width="702" height="1600" alt="Tela do jogo" src="https://github.com/user-attachments/assets/c4d8837b-4c46-47d8-ae55-b444dff286f1" />

<img width="702" height="1600" alt="Tela do jogo" src="https://github.com/user-attachments/assets/c621cbcd-9343-4153-8c97-9e2781148677" />

<img width="702" height="1600" alt="Tela do jogo" src="https://github.com/user-attachments/assets/39bff9d1-f134-4091-8776-62a5a6b72274" />

---

## Como executar o projeto

1. Clone este repositório:

```bash
git clone https://github.com/Alex-SP-BR/TypingFrontier.git
```

2. Abra o projeto no Android Studio.

3. Aguarde a sincronização do Gradle.

4. Execute o aplicativo em um dispositivo Android ou emulador.

---

## Funcionamento offline

O TypingFrontier foi projetado para manter o gameplay principal independente da conexão com a internet.

Os recursos principais do jogo continuam utilizando o armazenamento local.

Os recursos sociais dependem de conexão com o Supabase quando necessário.

Portanto:

```text
Sem internet
   ↓
Gameplay continua funcionando

Com internet
   ↓
Recursos sociais disponíveis
```

A indisponibilidade temporária dos serviços sociais não deve impedir o jogador de continuar utilizando o jogo.

---

## Segurança

Os recursos sociais e administrativos utilizam validações realizadas no backend.

A interface Android não é considerada uma autoridade administrativa.

As operações sensíveis são protegidas por autenticação, regras de acesso, políticas de segurança e funções server-side.

Informações administrativas não são utilizadas como autoridade apenas com base em valores armazenados localmente no dispositivo.

---

## Controle de versão

O código-fonte do projeto é versionado utilizando Git.

O repositório remoto utilizado para armazenamento do histórico de desenvolvimento é o GitHub.

As versões relevantes do projeto são identificadas através de commits e tags.

---

## Versão atual

A versão atualmente em desenvolvimento é:

`1.5`

A versão anterior publicada é:

`1.4`

A versão `1.4` corresponde ao estado anteriormente publicado do aplicativo.

A versão `1.5` representa a evolução posterior do projeto, incluindo a camada social e os recursos relacionados.

---

# 🇺🇸 English

## About the project

**TypingFrontier** is an educational RPG game developed for Android that combines learning, adventure, character progression, and social interaction into a gamified experience.

The player takes the role of a character who develops their abilities through different types of training.

The game integrates **Portuguese and Mathematics** learning as mental training, along with physical training activities to improve character attributes.

During the journey, players can choose a profession for their character, develop their skills, explore new locations, overcome challenges, and gain levels inside the game world.

The game also includes an integrated social layer, allowing players to participate in a **Social Ranking**, use a **Social Wall**, participate in the **Forum**, create discussions, reply to other players, and use social profile features.

Interaction happens through challenges, typed commands, and player choices, making learning part of the RPG experience.

The project was developed using **Kotlin and Android**, applying mobile development, local persistence, and backend integration concepts.

---

## Main systems

### RPG and progression

* RPG character progression
* Leveling and progression system
* Character profession choices
* Attribute progression
* In-game economy using Frons
* Exploration and adventure system

### Training

* Mental training with Portuguese and Mathematics
* Physical training
* Strength, Endurance, and Speed progression
* Typed command challenges
* Correct-answer streaks and performance progression

### Avatars and Achievements

* Avatar system
* Original male and female avatars
* Level progression avatars
* Avatar unlocking system
* Achievement system
* Badge system
* Achievement rewards
* Enlarged avatar and badge visualization

### Social System

* Social Identity
* Social Profile
* Social username
* Social Ranking
* Ranking by different attributes and progression metrics
* Social Wall
* General Forum
* Topic creation
* Replies and discussions
* Editing own content
* Deleting own content
* Reporting system
* Moderation system
* Administrative roles
* Temporary and permanent social bans
* Unbanning
* Administrative action logging

The social layer complements the game and does not replace the offline gameplay system.

---

## Social architecture

TypingFrontier uses **Supabase** as complementary infrastructure for its social features.

The architecture keeps the main gameplay independent from the social layer.

Simplified:

```text
Gameplay
   ↓
Local Save
   ↓
PlayerManager
```

While social features use:

```text
Android Application
        ↓
Supabase Auth
        ↓
Social Profile
        ↓
Ranking / Social Wall / Forum
        ↓
Moderation
```

Sensitive and administrative operations are protected by the backend.

The Android application acts as the interface and requester, while Supabase/PostgreSQL performs authorization checks.

---

## Technologies

* Kotlin
* Android SDK
* Android Studio
* Material Design
* XML / ViewBinding
* Git
* GitHub
* Supabase
* PostgreSQL

---

## Screenshots

<img width="1254" height="1254" alt="Game screen" src="https://github.com/user-attachments/assets/04ff4a76-7a62-4212-a7d3-08db63e9bc49" />

<img width="1254" height="1254" alt="Game screen" src="https://github.com/user-attachments/assets/74a026ab-10df-4b0c-ab7f-2f82412fb93e" />

<img width="702" height="1600" alt="Game screen" src="https://github.com/user-attachments/assets/c4d8837b-4c46-47d8-a55b-b444dff286f1" />

---

## How to run the project

1. Clone this repository:

```bash
git clone https://github.com/Alex-SP-BR/TypingFrontier.git
```

2. Open the project in Android Studio.

3. Wait for Gradle synchronization.

4. Run the application on an Android device or emulator.

---

## Offline operation

TypingFrontier is designed to keep its main gameplay independent from an internet connection.

Core game features continue to use local storage.

Social features depend on a connection to Supabase when required.

Therefore:

```text
Offline
   ↓
Gameplay continues to work

Online
   ↓
Social features become available
```

Temporary unavailability of social services should not prevent the player from continuing to use the game.

---

## Security

Social and administrative features use backend validation.

The Android interface is not considered an administrative authority.

Sensitive operations are protected through authentication, access rules, security policies, and server-side functions.

Administrative privileges are not granted solely through values stored locally on the device.

---

## Version control

The project source code is versioned using Git.

The remote repository used to store the development history is GitHub.

Relevant project versions are identified through commits and tags.

---

## Current version

The version currently under development is:

`1.5`

The previously published version is:

`1.4`

Version `1.4` corresponds to the previously published state of the application.

Version `1.5` represents the subsequent evolution of the project, including the social layer and related features.

---

## License

This project is currently available for learning and development purposes.
