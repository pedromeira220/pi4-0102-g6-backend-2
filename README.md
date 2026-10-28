# Sistema de Gestão de Filas: Backend 2

Recebe os avisos de mudança de etapa do Backend 1 por Socket e os repassa às páginas abertas por WebSocket. Projeto Integrador IV, Turma 0102, Grupo 06.

- Visão geral e fluxos: [pi4-0102-g6-frontend/docs/fluxos](https://github.com/pedromeira220/pi4-0102-g6-frontend/tree/main/docs/fluxos)
- Contratos deste módulo: [docs/contratos](docs/contratos)
- Decisões de arquitetura: [docs/adr](docs/adr)

Ponte entre o Socket TCP do Backend 1 e as páginas inscritas por WebSocket/STOMP.
Não acessa banco nem calcula fila.

## Executar

Requer JDK 17 ou superior e Maven 3.6.3 ou superior.

```sh
mvn test
mvn spring-boot:run
```

HTTP/WebSocket: porta 8082. Socket TCP: porta 5000.
Variáveis opcionais: HTTP_PORT, TCP_PORT e FRONTEND_ORIGINS (origens separadas por vírgula).

## Frontend (CT5 e CT6)

Conectar um cliente STOMP em `ws://localhost:8082/ws` (WebSocket nativo, sem SockJS).
Inscrever-se em `/topic/pedidos/42` para acompanhar a senha 42.
O corpo das mensagens é JSON. A operação STOMP MESSAGE representa etapaAlterada;
não foi acrescentado campo `tipo`, pois o CT6 não define esse campo.
Na abertura e após reconexão, consultar o estado atual no Backend 1.
Não há armazenamento ou reenvio de eventos perdidos.

## Entrada TCP provisória — confirmar com CT4

Cada mensagem é um JSON UTF-8 em uma única linha, terminado por `\n`.
A conexão pode enviar várias linhas. Não há resposta/ACK de aplicação.
Exemplo:

```json
{"senha":42,"etapa":"em_preparo","estimativaMinutos":8,"posicaoNaFila":3,"atualizadoEm":"2026-10-13T19:20:00"}
```

Etapas: recebido, em_preparo, pronto, encerrado. Senha, etapa e atualizadoEm
são obrigatórios. Estimativa e posição são opcionais e não negativas.
`posicaoNaFila` é uma extensão provisória da visão geral, ainda ausente no CT6.
O emissor deve omitir estimativaMinutos nas etapas pronto e encerrado.

Mensagens inválidas são descartadas com registro no log; linhas acima de 8192
caracteres encerram a conexão. Até oito conexões TCP simultâneas, com 60 segundos
de inatividade; o Backend 1 precisa reconectar se necessário.

O serviço usa broker em memória para uma instância e não tem autenticação.
Destina-se à demonstração em ambiente local/controlado.
