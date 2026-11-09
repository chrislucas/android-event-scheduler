# Formato do arquivo de agenda (`rememberme-agenda`)

> Spec 17 (importar/exportar). Este documento define o arquivo `.json` gerado por **Exportar** e aceito por **Importar**.
> O exemplo abaixo é lido com sucesso por `AgendaJsonCodecTest` — se o formato mudar, o teste obriga a atualizar este arquivo.

## Visão geral

- JSON em UTF-8, nome sugerido `rememberme-agenda-AAAA-MM-DD.json` (data local da exportação), MIME `application/json`.
- Um objeto com cabeçalho e a lista `items`. Datas em `AAAA-MM-DD`, horas em `HH:mm` (24 h), enums pelo **nome estável** (nunca pelo ordinal).
- **Não** são exportados: configurações do usuário (e-mail da conta, preferências), filtros da gaveta e adesivos do dia.
- O arquivo **não é criptografado**: quem tiver o arquivo poderá ver a agenda.

## Exemplo

```json
{
  "format": "rememberme-agenda",
  "version": 1,
  "exportedAt": "2026-11-08T12:00:00Z",
  "appVersion": "1.0",
  "items": [
    {
      "id": "7b0f2c1e-5d3a-4c1b-9a55-0d8f3f9e1a01",
      "title": "Treino de corrida",
      "type": "EVENT",
      "startDate": "2026-11-02",
      "endDate": "2026-11-02",
      "startTime": "06:30",
      "endTime": "07:45",
      "isAllDay": false,
      "location": "Parque Ibirapuera",
      "notes": "Levar água",
      "colorHex": "#FF7E57C2",
      "calendarSource": "family_calendar",
      "reminderOffset": "THIRTY_MINUTES",
      "recurrence": { "type": "WEEKLY", "daysOfWeek": ["MONDAY", "FRIDAY"], "occurrenceLimit": 8 },
      "stickerEmoji": "🏃"
    },
    {
      "id": "0c9d4b7a-2e61-4f08-8a3d-6b1e7c2f9d02",
      "title": "Tomar remédio",
      "type": "REMINDER",
      "startDate": "2026-11-01",
      "endDate": "2026-11-01",
      "startTime": "08:00",
      "endTime": null,
      "isAllDay": false,
      "location": "",
      "notes": "",
      "colorHex": "#FF29B6F6",
      "calendarSource": "my_reminders",
      "reminderOffset": "NONE",
      "recurrence": { "type": "DAILY", "daysOfWeek": [], "occurrenceLimit": null },
      "stickerEmoji": null
    }
  ]
}
```

## Cabeçalho

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `format` | texto | sim | Sempre `rememberme-agenda`. Outro valor → "Este arquivo não é um backup do RememberMe" |
| `version` | inteiro | sim | Versão do formato (hoje `1`). Maior que a suportada → "Versão do arquivo mais nova que o app" |
| `exportedAt` | texto ISO-8601 | não | Instante da exportação (informativo) |
| `appVersion` | texto | não | `versionName` do app que exportou (informativo) |
| `items` | lista | sim | Agendamentos. Ausente ou não-lista → arquivo corrompido |

Chaves desconhecidas (no cabeçalho ou nos itens) são **ignoradas**, o que permite ler arquivos de versões futuras compatíveis.

## Item (`ScheduleItem`)

| Campo | Tipo | Obrigatório | Valores / regras |
|---|---|---|---|
| `id` | texto | sim | Identidade do item; 1 a 100 caracteres. Mesmo `id` já existente na agenda = "já existe" (R5) |
| `title` | texto | sim | Não vazio; truncado em 200 caracteres |
| `type` | enum | sim | `EVENT` ou `REMINDER` |
| `startDate` | data | sim | `AAAA-MM-DD`; ano entre 1900 e 2200. Para recorrentes, é o início da série |
| `endDate` | data | não | `≥ startDate`; ausente = `startDate` |
| `startTime` | hora | sim | `HH:mm` |
| `endTime` | hora | não | `HH:mm` ou `null` |
| `isAllDay` | booleano | não | Padrão `false` |
| `location` | texto | não | Truncado em 200 caracteres |
| `notes` | texto | não | Truncado em 4000 caracteres |
| `colorHex` | texto | não | `#AARRGGBB` (ou `#RRGGBB`, opaco); ausente = cor padrão |
| `calendarSource` | texto | não | `id` do calendário: `personal_account`, `family_calendar` ou `my_reminders`; ausente = padrão do tipo |
| `reminderOffset` | enum | não | `NONE`, `TEN_MINUTES`, `FIFTEEN_MINUTES`, `THIRTY_MINUTES` ou `ONE_HOUR`; ausente = `FIFTEEN_MINUTES` |
| `recurrence` | objeto | não | Ver abaixo; ausente = sem recorrência |
| `stickerEmoji` | texto ou `null` | não | Até 16 caracteres; vazio ou maior é descartado |

### `recurrence`

| Campo | Regras |
|---|---|
| `type` | `NONE`, `DAILY` ou `WEEKLY` |
| `daysOfWeek` | Para `WEEKLY`: ao menos um de `MONDAY` … `SUNDAY`. Ignorado nos demais |
| `occurrenceLimit` | Inteiro `≥ 1` ou `null` (sem limite) |

## Validação na importação

1. Tamanho: arquivos acima de **5 MB** são recusados, mesmo quando o provedor não informa o tamanho.
2. Estrutura: JSON válido, `format`, `version` e `items`. Qualquer falha recusa o arquivo inteiro e **nada** é gravado. Antes de montar a árvore, o texto é varrido e recusado ("arquivo corrompido") se tiver aninhamento acima de 16 níveis (o formato usa 5) ou mais de 1 milhão de valores/contêineres (um arquivo real de 5 MB tem cerca de 250 mil): isso protege o app de estouro de pilha e de memória com arquivos maliciosos.
3. Por item: qualquer valor inválido (data, hora, enum desconhecido, `endDate < startDate`, `WEEKLY` sem dias, limite `< 1`, `id` vazio ou longo demais, título vazio, cor malformada) **ignora só aquele item**, que é contado ("N itens ignorados por dados inválidos"). Um `id` repetido dentro do arquivo vale pela última ocorrência.
4. Só depois de validar tudo, e com a confirmação do usuário, os itens são gravados numa **única transação**.

## Evolução

Mudanças compatíveis (campo novo opcional) mantêm `version: 1`. Mudanças incompatíveis incrementam `version`; apps antigos recusam o arquivo com a mensagem de versão mais nova.
