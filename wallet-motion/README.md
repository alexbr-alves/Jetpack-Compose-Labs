# Wallet Motion

Experimento de Motion Design em Jetpack Compose focado em uma carteira de cartões com sensação física.

A experiência simula um stack de cartões bancários que responde a gestos, profundidade, rotação, elevação e transições com molas. O objetivo é explorar uma interface de wallet que pareça tátil, fluida e diretamente manipulável.

## Demonstração

> Adicione o vídeo final aqui.

<!--
Exemplo:

https://github.com/user-attachments/assets/seu-video-id

ou:

<video src="docs/demo.mp4" controls width="360"></video>
-->

## Destaques

- Stack de cartões com profundidade, rotação e elevação dinâmica.
- Gestos horizontais para alternar cartões.
- Gesto vertical para abrir e fechar o modo de detalhes.
- Flip do cartão no modo de detalhes.
- Painel de configurações com switches para recursos do cartão.
- Movimento baseado em `Animatable`, `spring`, velocidade do gesto e interpolação física.
- Cores e gradientes reativos ao cartão selecionado.

## Interações

- Toque no cartão principal para abrir os detalhes.
- Arraste horizontalmente para enviar o cartão para trás da pilha.
- Arraste para cima para expandir.
- Arraste para baixo no painel de detalhes para voltar.
- No modo de detalhes, arraste horizontalmente para virar o cartão.

## Executar

```bash
./gradlew :app:assembleDebug
```

Instale em um dispositivo conectado:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- Compose Animation
