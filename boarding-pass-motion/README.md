# Boarding Pass Motion

Experimento de Motion Design em Jetpack Compose focado em uma única interação refinada de boarding pass.

A experiência simula um ticket físico dobrado que se abre em três partes conectadas, gira para revelar o verso com QR Code, reage a uma validação via Supabase Realtime e continua acompanhando o voo por meio de uma cápsula com contagem regressiva do portão.

## Demonstração

https://github.com/user-attachments/assets/30995f03-1697-41ce-853e-d36940b3aefb

## Destaques

- Animação física em três dobras usando `graphicsLayer`, `rotationX`, pivôs de transformação, sombras e perspectiva.
- Movimento sequencial de abertura e fechamento, sem atalhos de expansão de card.
- Verso com QR Code integrado à validação por Supabase Realtime.
- Feedback tátil e animação luminosa de leitura quando o ticket é escaneado.
- `FlightCapsule` minimalista sincronizada com o estado do ticket.
- Contagem regressiva para fechamento do portão após aprovação do embarque.
- Seção de tickets antigos com a mesma linguagem visual do boarding pass ativo.

## Fluxo Realtime

A demonstração escuta atualizações do Supabase Realtime em:

```text
public.tickets
```

O ticket ativo da demonstração é:

```text
TICKET_DEMO_001
```

Quando `scan_count` aumenta, o app interpreta o evento como um scan bem-sucedido e executa a sequência de validação.

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
- Supabase Kotlin SDK
- Geração de QR Code com ZXing
