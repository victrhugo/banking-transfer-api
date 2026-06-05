# Roteiro de Defesa - Banco Digital API

Este documento contém possíveis perguntas técnicas e respostas defensáveis para apresentação do projeto.

---

## 1. Arquitetura e Design

### P: Por que você escolheu arquitetura em camadas?

**R:**
Arquitetura em camadas é o padrão de facto para aplicações web em Spring Boot no mercado. É simples, eficaz e não é excessivamente complexa. Para um teste técnico de nível Pleno, demonstra:
- Conhecimento sólido de separação de responsabilidades
- Facilidade de manutenção e testes
- Escalabilidade básica sem overengineering

Alternativas como CQRS ou DDD seriam desnecessárias aqui, pois o domínio ainda é simples.

---

### P: Por que não usar camada de mapper/converter explícita?

**R:**
Para um projeto deste tamanho, mappers explícitos adicionariam boilerplate sem retorno. As conversões entity → DTO são simples (um método `toResponse`). Se o projeto crescesse com múltiplas entidades complexas, aí sim valeria usar MapStruct ou similar.

---

### P: Você considerou usar padrões como Repository Pattern e UnitOfWork?

**R:**
Sim. Repository Pattern já está implementado via `AccountRepository` e `TransferRepository`, que herdam de `JpaRepository`. A Spring Data JPA já abstrai a complexidade de CRUD. UnitOfWork seria redundante aqui, pois `@Transactional` já gerencia o contexto de persistência via `EntityManager`.

---

## 2. Camada de Controller

### P: Por que controllers são thin?

**R:**
Controllers existem para orquestração HTTP, não lógica de negócio. Isso garante que:
- Mudanças de protocolo (HTTP para gRPC, por exemplo) não afetam regras
- Testes de serviço não precisam mockear HTTP
- Código fica mais limpo e focado

Se colocar regras no controller, fica difícil reutilizar a mesma lógica em CLI ou job assíncrono.

---

### P: Como você valida entrada no controller?

**R:**
Usamos `@Valid` + Bean Validation (`@NotNull`, `@Positive`, etc.).

```java
@PostMapping
public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request)
```

Quando validação falha, `GlobalExceptionHandler` captura `MethodArgumentNotValidException` e retorna 400 com lista de erros. Isso centraliza validação e resposta consistente.

---

### P: Por que usar `ResponseEntity<T>` em vez de só retornar T?

**R:**
`ResponseEntity` nos permite controlar status HTTP (201 para POST, 404 para não encontrado, etc.). Sem ele, Spring sempre retorna 200, o que é semanticamente incorreto. Para endpoints específicos:
- `POST /accounts` → 201 CREATED
- `GET /accounts/{id}` → 200 OK (ou 404)

---

## 3. Camada de Service

### P: Como você garante que a transferência é atômica?

**R:**
Usamos `@Transactional` no método `makeTransfer`:

```java
@Transactional
public TransferResponse makeTransfer(TransferRequest request)
```

Isso envolve todo o método em uma transação ACID:
- se qualquer linha falhar, rollback automático de TUDO (contas, transfer, notificação)
- se tudo passar, commit automático

Sem isso, poderíamos debitar uma conta sem creditar a outra em caso de erro.

---

### P: Por que não usar compensação transacional em vez de transação?

**R:**
Compensação é um padrão para saga distribuída (múltiplos bancos de dados). Aqui, tudo está em um PostgreSQL, então transação ACID é suficiente e muito mais simples. Saga seria overengineering.

---

### P: Como você valida regras de negócio no service?

**R:**
Temos um método `validateRequest` que verifica:
- contas origem e destino não são a mesma
- valor é maior que zero

Depois, o próprio repositório/entidade verifica:
- contas existem (via `findByIdForUpdate`)
- saldo suficiente (comparação explícita)

Essa separação deixa claro: validações simples primeiro, depois operações custosas.

---

### P: Por que o service chama `findByIdForUpdate` em vez de `findById`?

**R:**
Porque `findByIdForUpdate` usa `PESSIMISTIC_WRITE`, que bloqueia a linha durante a transação. Isso previne race conditions em cenários de alta concorrência.

Exemplo do problema sem lock:
1. Thread A lê saldo = 100
2. Thread B lê saldo = 100
3. Thread A subtrai 50, escreve 50
4. Thread B subtrai 40, escreve 60 (deveria ser 10!)

Com lock:
1. Thread A bloqueia e lê saldo = 100
2. Thread B espera
3. Thread A subtrai 50, escreve 50, libera lock
4. Thread B bloqueia, lê saldo = 50, subtrai 40, escreve 10 ✓

---

### P: O notificationService pode falhar sem causar rollback?

**R:**
Não, porque está dentro de `@Transactional`. Se lançar exceção, toda a transação faz rollback. No caso atual, é um log simples que dificilmente falha, mas se fosse enviar para fila Kafka, a falha causaria rollback.

Se quisermos notificação resiliente (que não impacte a transferência), poderíamos usar `@Transactional(propagation = REQUIRES_NEW)` em um método separado ou executar em thread background.

---

## 4. Camada de Repository

### P: Por que usar Spring Data JPA em vez de Hibernate puro?

**R:**
Spring Data JPA é uma abstração sobre Hibernate que:
- reduz boilerplate
- fornece `findAll()`, `save()`, etc. de graça
- permite queries sem SQL puro (com `@Query`)
- é padrão de mercado

Hibernate puro exigiria mais código de gerenciamento de sessão.

---

### P: Como o lock pessimista funciona no PostgreSQL?

**R:**
Com `@Lock(LockModeType.PESSIMISTIC_WRITE)`, Spring/Hibernate gera SQL:

```sql
SELECT * FROM accounts WHERE id = ? FOR UPDATE
```

O `FOR UPDATE` bloqueia a linha no banco até commit/rollback. Outras transações que tentarem ler a mesma linha com `PESSIMISTIC_WRITE` ficam aguardando.

Alternativa seria lock otimista com `@Version`, mas exigiria coluna adicional e falharia em conflito (em vez de aguardar).

---

### P: Você testou o lock em cenário concorrente?

**R:**
Não há teste de integração concreto disso. Para demonstrar, seria necessário:
- criar teste com `@DataJpaTest` (Spring testa contexto JPA)
- disparar dois `makeTransfer` paralelos
- verificar que um espera o outro completar

Mas isso não é necessário para nível Pleno, pois a lógica é simples e documentada.

---

## 5. DTOs e Validação

### P: Por que usar `record` do Java 21?

**R:**
`record` é imutável, conciso e expressa bem "dados" sem estado mutável:

```java
public record CreateAccountRequest(String name, BigDecimal initialBalance) { }
```

Gera automaticamente:
- construtor
- getters
- `equals()`, `hashCode()`, `toString()`

Alternativas:
- `class` com Lombok: mais verboso, permite mutação
- `@Data class`: mais flexível, mas menos seguro

Para contrato de API, `record` é ideal.

---

### P: Como você valida DTOs?

**R:**
Bean Validation via anotações:

```java
public record CreateAccountRequest(
    @NotBlank(message = "O nome da conta é obrigatório")
    String name,
    @NotNull
    @PositiveOrZero
    BigDecimal initialBalance
)
```

Quando `@Valid` é usado no controller, Spring valida automaticamente. Violações geram `MethodArgumentNotValidException` → 400.

---

### P: Por que nunca retornar entidades diretamente?

**R:**
Se retornarmos `Account` direto:
- mudanças futuras no schema são quebras na API
- segurança: exposição de campos internos (senhas hash, flags, etc.)
- acoplamento: cliente da API depende da estrutura do banco

DTOs desacoplam persistência de contrato público.

---

## 6. Tratamento de Exceções

### P: Como você estruturou o tratamento de erro?

**R:**
Exceções customizadas + `@RestControllerAdvice`:

```
AccountNotFoundException    → 404 NOT_FOUND
InsufficientBalanceException → 400 BAD_REQUEST
InvalidTransferException    → 400 BAD_REQUEST
MethodArgumentNotValidException → 400 BAD_REQUEST
Exception (catchall)        → 500 INTERNAL_SERVER_ERROR
```

Cada uma retorna estrutura consistente:
```json
{
  "timestamp": "2026-06-05T10:30:00",
  "status": 400,
  "message": "Saldo insuficiente",
  "errors": []
}
```

---

### P: Por que não usar `HttpStatus.NO_CONTENT` para deleção?

**R:**
Não há endpoint de deleção no projeto. Para manter dados históricos de contas e transferências, é comum não permitir delete direto. Se houvesse, `204 NO_CONTENT` seria apropriado.

---

### P: Como você evita expor stacktraces em erro 500?

**R:**
Em `GlobalExceptionHandler`:

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor");
}
```

Não retornamos `ex.getMessage()` ou stacktrace. Em produção, o log fica no servidor via SLF4J.

---

## 7. Persistência e Transações

### P: Por que `hibernate.ddl-auto: update`?

**R:**
Para facilitar teste local. Hibernate automaticamente cria/atualiza tabelas baseado em `@Entity`.

Em produção, seria `validate` ou `none`, com migrations via Liquibase/Flyway.

Isso garante que deploy não quebra schema.

---

### P: Como você garante que `createdAt` é preenchido automaticamente?

**R:**
Via `@PrePersist`:

```java
@PrePersist
public void prePersist() {
    this.createdAt = LocalDateTime.now();
}
```

Antes de persistir, Hibernate chama esse método. Alternativa seria usar banco para gerar timestamp via trigger, mas aplicação é mais portável.

---

### P: Você considerou versionamento otimista?

**R:**
`@Version` com `@OptimisticLockException` é útil quando múltiplas transações leem e modificam dados sem bloquear. Aqui:
- mudanças são raras (só na transferência)
- concorrência é esperada (múltiplas transferências)
- failfast (rejeitar em vez de awaitar) não é aceitável

Logo, lock pessimista é melhor que otimista aqui.

---

## 8. Notificação

### P: Por que não usar Kafka ou RabbitMQ?

**R:**
Requisito explícito: "Não utilizar mensageria, Kafka ou filas. Manter simples."

Para nível Pleno, demonstrar que você entende quando usar/não usar tecnologia é valioso. Kafka seria overkill para um log de transferência.

---

### P: Como escalar notificação para produção?

**R:**
Opções em ordem de complexidade:

1. **AsyncService** (simples): executar em thread separada com `@Async`
   - risco: crash da thread perde notificação

2. **Event + Listener** (intermediário): publicar evento, listener consome assincronamente
   - risco: ainda em memória

3. **Message Queue** (produção): Kafka, RabbitMQ, AWS SQS
   - garantia: persistência, retry, deadletter

Para agora, log é suficiente. Interface do `NotificationService` permite trocar implementação.

---

## 9. Testes

### P: Por que não tem testes de integração?

**R:**
Testes unitários cobrem lógica crítica. Testes de integração exigem:
- banco de dados rodando
- muito mais tempo
- setup complexo

Para nível Pleno, testes unitários bem feitos com Mockito demonstram:
- compreensão de dependências
- isolamento de lógica
- casos de erro

Testes de integração seria valor agregado, não requisito.

---

### P: Como você mockeia o repositório?

**R:**
```java
@Mock
private AccountRepository accountRepository;

@InjectMocks
private TransferService transferService;
```

Mockito cria mock do repositório e injeta em `TransferService` automaticamente. Em cada teste:

```java
when(accountRepository.findByIdForUpdate(senderId))
    .thenReturn(Optional.of(sender));
```

Isso isola `TransferService` da camada de persistência.

---

### P: Como você verifica que notificação foi chamada?

**R:**
```java
verify(notificationService, times(1)).notifyTransferSuccess(receiverId);
```

Mockito registra chamadas. `times(1)` verifica que foi chamado exatamente uma vez. Se falhar em transferência, verifica:

```java
verify(notificationService, never()).notifyTransferSuccess(any());
```

Garante que erro não gera notificação falsa.

---

### P: Faltam testes do controller?

**R:**
Idealmente, sim. Mas o controller é thin (só orquestra). Testar seria:

```java
@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {
    @MockBean
    AccountService service;
    
    @Autowired
    MockMvc mvc;
}
```

Para nível Pleno, cobertura do service é suficiente. Controller test seria valor agregado.

---

## 10. Configuração e Deployment

### P: Como você configurou o PostgreSQL?

**R:**
Via `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/compass_bank
    username: compass
    password: compass
```

E `docker-compose.yml` fornece serviço pronto:

```yaml
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: compass_bank
```

Isso permite `docker compose up -d` e aplicação conecta imediatamente.

---

### P: Por que não usar H2 em memória para testes?

**R:**
Poderia usar, mas PostgreSQL é mais próximo da produção. Se houvesse incompatibilidade H2/PostgreSQL, teste não cobrira. Para este projeto, não é crítico, mas é boas práticas usar mesmo banco localmente.

---

### P: Como você escalaria a aplicação?

**R:**
Para escalar de verdade:

1. **Sem estado**: remover `@Transactional` se possível, deixar estado no banco
2. **Connection pooling**: HikariCP já está configurado (padrão Spring)
3. **Índices**: adicionar `@Index` em `Transfer.senderId`, `Transfer.receiverId`
4. **Caching**: Redis para contas lidas frequentemente
5. **Read replicas**: PostgreSQL replicação, ler de replica

Mas para um teste técnico, não é necessário.

---

## 11. Swagger/OpenAPI

### P: Por que documentar com Swagger?

**R:**
Swagger (OpenAPI) gera documentação automática da API:
- `/swagger-ui.html`: interface web para testar endpoints
- JSON schema de requests/responses
- sem manutenção manual

Facilita:
- novo desenvolvedor entender a API
- cliente testar sem curl
- documentação sempre sincronizada

---

### P: Como você adicionaria exemplos no Swagger?

**R:**
Não adicionei por manter código simples. Poderia usar anotações:

```java
@PostMapping
@Operation(summary = "Criar conta", 
    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = @Content(
            schema = @Schema(implementation = CreateAccountRequest.class),
            examples = @ExampleObject(value = "{\"name\": \"João\", \"initialBalance\": 1000}")
        )
    )
)
```

Ficaria verboso. Para teste técnico, simples é melhor.

---

## 12. Decisões de Design Discutíveis

### P: Por que não usar `Optional` em lugar de `Optional.orElseThrow`?

**R:**
Ambas são válidas. `Optional.orElseThrow` é explícito e legível. Alternativa:

```java
return accountRepository.findById(accountId)
    .map(this::toResponse)
    .orElseThrow(() -> new AccountNotFoundException(...));
```

vs

```java
Account account = accountRepository.findById(accountId)
    .orElseThrow(...);
return toResponse(account);
```

A primeira é mais funcional (stream style). Escolhi primeira para demonstrar fluência com Streams.

---

### P: Por que `LocalDateTime` em vez de `Instant`?

**R:**
Ambas são válidas. Escolhi `LocalDateTime` porque:
- mais legível em logs e UI
- transferências são conceito "local" (não precisa UTC)

Se fosse API global com timezones, `Instant` seria melhor. Para banco brasileiro, `LocalDateTime` é suficiente.

---

### P: Como você trataria idempotência em transferências?

**R:**
Requisito não menciona. Em real, seria necessário:
- cliente passa `idempotencyKey` na requisição
- banco de dados registra key → UUID da transferência
- se mesma key chegar, retorna resultado anterior

Isso é complexo e fora do escopo. Bom valor agregado se implementado.

---

## 13. O Que Você Não Fez (E Por Quê)

### CQRS (Command Query Responsibility Segregation)
- desnecessário para projeto simples
- futuro: se leitura ficar muito complexa, aí sim separaria read model

### Event Sourcing
- overkill para auditoria de contas
- transfer log já fornece histórico

### Circuit Breaker
- não há serviço externo para chamar
- futuro: se integrar com outro banco, aí usa Resilience4j

### Cache distribuído
- não há operação repetitiva que precise
- futuro: listar transferências com Redis

### GraphQL
- REST é simples e suficiente
- cliente pode fazer múltiplos requests sem problema

### Autenticação/Autorização
- requisito não pediu
- futuro: Spring Security + JWT

---

## 14. Checklist Final

Ao defender, mencione que cumpriu:

- ✅ Gestão de contas (CRUD básico)
- ✅ Transferência entre contas
- ✅ Consulta de movimentações
- ✅ Notificações (simuladas com log)
- ✅ Testes unitários (TransferService)
- ✅ Swagger/OpenAPI
- ✅ Código limpo (separação de responsabilidades)
- ✅ Tratamento de exceções
- ✅ Validações (Bean Validation)
- ✅ Transações ACID
- ✅ Lock pessimista para concorrência
- ✅ Docker Compose pronto
- ✅ Java 21 + Spring Boot 3
- ✅ Maven
- ✅ JUnit 5 + Mockito
- ✅ PostgreSQL

---

## 15. Possíveis Objeções e Respostas Rápidas

| Objeção | Resposta |
|---------|----------|
| "Por que não usar Lombok?" | Usamos! `@Data`, `@Builder`, `@Slf4j` |
| "Onde estão os testes do controller?" | Service é o mais crítico. Controller testes seria valor agregado |
| "Por que sem autenticação?" | Requisito não pediu. Escopo bem definido |
| "O projeto é muito simples" | Correto! Objetivo é demonstrar fundamentos sólidos, não overengineering |
| "Faltam testes de integração" | Unitários cobrem lógica. Integração precisaria banco rodando |
| "Por que não usar Hexagonal/DDD?" | Overengineering. Nível Pleno precisa de bom design, não padrão complexo |
| "Como você escalaria?" | [Veja seção 10] |

---

## Roteiro de Apresentação (10 minutos)

1. **Requisito** (1 min): API banco digital, transferências, notificações
2. **Arquitetura** (2 min): camadas, separação de responsabilidades
3. **Domínio** (1 min): Account, Transfer como agregados
4. **Fluxo de transferência** (3 min): validações, lock, transação, rollback
5. **Concorrência** (1 min): lock pessimista, por quê
6. **Testes** (1 min): 6 casos cobertos, como validar
7. **Deploy** (1 min): Docker Compose, conforme descrito em README

Total: 10 min, deixa espaço para perguntas.

---

**Boa sorte! Qualquer pergunta não coberta aqui, pense no princípio: simplicidade, clareza, boas práticas sólidas.**
