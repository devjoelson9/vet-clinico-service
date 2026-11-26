# Serviço Clínico — Sistema de Gestão Veterinária

Microsserviço responsável pelo prontuário do animal: atendimentos, itens (procedimentos e medicamentos), prescrições e cálculo do valor do atendimento.

## Tecnologias

- Java 21 e Spring Boot 4.1.1
- Spring Web MVC, Spring Data JPA, Spring Security (JWT) e Bean Validation
- PostgreSQL com migrações Flyway
- RabbitMQ para publicação de eventos de atendimento
- springdoc-openapi (Swagger UI)

## Rotas principais

| Recurso | Prefixo |
|---|---|
| Atendimentos (iniciar, consultar, finalizar, cancelar) | `/api/v1/atendimentos` |
| Itens do atendimento | `/api/v1/atendimentos/{id}/itens` |
| Prescrição e PDF | `/api/v1/atendimentos/{id}/prescricao` |
| Valor do atendimento | `/api/v1/atendimentos/{id}/valor` |
| Medicamentos | `/api/v1/medicamentos` |
| Catálogo de itens | `/api/v1/catalogo-itens` |

Todas as rotas exigem o header `Authorization: Bearer <token>`, exceto `actuator/health` e a documentação.

Documentação interativa: `http://localhost:8082/swagger-ui.html`.

## Integrações

- **Identidade** (`IDENTIDADE_BASE_URL`): resolve o veterinário vinculado ao usuário autenticado.
- **Agendamento** (`AGENDAMENTO_BASE_URL`): valida o agendamento no início do atendimento e informa o veterinário responsável.
- **RabbitMQ**: publica no exchange `vet.events` as routing keys `atendimento.iniciado`, `atendimento.realizado` e `atendimento.cancelado`. O serviço de agendamento consome esses eventos para atualizar o status.

## Variáveis de ambiente

| Variável | Descrição |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS` | Conexão com o PostgreSQL |
| `JWT_SECRET` | Mesmo segredo usado pelos demais serviços |
| `IDENTIDADE_BASE_URL` | Ex.: `http://servico-identidade:8080` |
| `AGENDAMENTO_BASE_URL` | Ex.: `http://servico-agendamento:8080` |
| `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD` | Broker de mensageria |

## Como executar

Ambiente completo (recomendado), a partir de `vet-infrastructure`:

```bash
docker compose up -d --build servico-clinico
```

Execução local, com um PostgreSQL disponível:

```bash
./mvnw spring-boot:run
```

Testes:

```bash
./mvnw test
```

## Estrutura

```text
src/main/java/br/com/vet/clinico/
├── application/     # serviços de caso de uso, gateways, mappers e DTOs
├── domain/          # entidades, enums e repositórios
├── infrastructure/  # integrações HTTP/AMQP, segurança e configuração
└── web/             # controllers e tratamento de exceções
```
