# Jetpack Compose Labs 🧪

Coleção curada de experimentos avançados em **Jetpack Compose** focados em motion design, animações, gráficos, gestos e interfaces Android modernas.

Este repositório é um laboratório onde ideias viram interações reutilizáveis. Cada projeto explora um aspecto específico do Compose com código de qualidade, arquitetura simples e experiências fluidas.

---

## 🎯 Objetivos

- Explorar recursos avançados do Jetpack Compose.
- Construir padrões reutilizáveis de interação.
- Experimentar Motion Design e Material 3 Expressive.
- Demonstrar técnicas modernas de UI Android.
- Compartilhar implementações bem acabadas com a comunidade.

---

## 📦 Projetos

| Projeto | Descrição | Status |
|---------|-------------|--------|
| 🚀 [wallet-motion](wallet-motion) | Carteira de cartões com gestos, profundidade, flip e transições físicas. | ✅ Completo |
| ✈️ [boarding-pass-motion](boarding-pass-motion) | Boarding pass físico com dobra tripla, verso com QR Code e validação Realtime. | ✅ Completo |
| 🎧 [music-play](music-play) | Player musical neumórfico com vinil, troca de faixas, álbuns e playlists animadas. | ✅ Completo |
| ⏳ Mais em breve... | Motion, gráficos, Canvas, shaders e novas interações. | 🚧 |

---

## 🔬 Tópicos

- Motion Design
- Material 3 Expressive
- Animações avançadas
- Gestos
- Interações inspiradas em física
- Canvas
- GraphicsLayer
- Layouts customizados
- Otimização de performance
- Arquitetura de UI
- Gerenciamento de estado
- Microinterações

---

## ▶️ Executar

Cada lab é um projeto Android independente. Entre na pasta do experimento e gere o APK de debug:

```bash
cd music-play
./gradlew :app:assembleDebug
```

Instale em um dispositivo conectado:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Troque `music-play` por `wallet-motion` ou `boarding-pass-motion` para executar os outros labs.

---

## 📚 Por que este repositório?

A maioria dos exemplos de Compose demonstra APIs.

**Jetpack Compose Labs** foca em demonstrar **experiências**.

O objetivo não é criar aplicativos completos, mas sim interações polidas, reutilizáveis e com sensação de produto.

---

## 🛠 Stack

- Kotlin
- Jetpack Compose
- Material 3
- Compose Animation
- Compose Foundation
- Kotlin Coroutines
- AndroidX

---

## 🤝 Contribuições

Ideias, feedbacks e sugestões são sempre bem-vindos.

Se houver alguma interação ou animação interessante que você gostaria de ver implementada, fique à vontade para abrir uma issue ou iniciar uma discussão.
