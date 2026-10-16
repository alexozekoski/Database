# Plano de refatoração de `Clause`

## Estado

- Fase atual: implementação inicial concluída para PostgreSQL, SQLite e MySQL.
- Limpeza concluída: removidos os nove tipos antigos do pacote `query` (`Clause`, `Column`, `Where`, `Set`, `Join`, `GroupBy`, `OrderBy`, `Limit`, `Offset`). `Query`, `QueryModel` e os joins dos modelos usam `CreatedClause` e a renderização de `Dialect`.
- Implementados: assinaturas em `Dialect`, com implementações de instância separadas em `PostgreSQLDialect`, `SQLiteDialect` e `MySQLDialect`; `CreatedClause`, `RenderedClause`, `QueryType` e integração com as cláusulas existentes. Os 12 métodos estáticos idênticos dos três bancos foram centralizados em `Dialect`, mantendo privados os auxiliares internos. Não há métodos `default`. A classe `ClauseRenderer` foi removida.
- Verificação: testes de renderização nos três dialetos e execução JDBC SQLite em memória; execução em servidores PostgreSQL/MySQL ainda pendente de ambientes próprios.
- Contrato efetivo e instruções de teste: [docs/CLAUSE_RENDERING.md](docs/CLAUSE_RENDERING.md). As etapas abaixo preservam o histórico do desenho; `ClauseContext` e os wrappers adicionais de argumentos não foram necessários nesta primeira implementação.
- Escopo: `Clause` e suas implementações (`Where`, `Column`, `Set`, `Join`, `GroupBy`, `OrderBy`, `Limit` e `Offset`).
- Fora do escopo: redesenhar os nomes, os métodos fluentes ou a ordem de construção de `Query` e `QueryModel`.

## Problema correto a resolver

A construção geral da query está adequada. O problema é que as implementações de `Clause` precisam decidir corretamente o que cada entrada representa:

1. **valor de dados** — deve virar `?` e ser enviado ao `PreparedStatement`;
2. **identificador** — tabela, coluna, schema ou alias; não pode virar `?` e deve ser validado e delimitado conforme o banco;
3. **operador/palavra-chave** — não pode virar `?`; deve vir de uma lista segura aceita pelo dialeto;
4. **expressão SQL** — função, outra coluna, `DEFAULT`, `CURRENT_TIMESTAMP` ou trecho avançado; não deve ser confundida com uma `String` comum e precisa de uma API explícita;
5. **SQL raw** — escape hatch consciente, separado da API segura e com seus valores ainda parametrizados.

O `PreparedStatement` protege **valores**, não a estrutura da consulta. Um placeholder não pode substituir, por exemplo, o nome de uma coluna:

```sql
-- Correto: nome estrutural validado + valor parametrizado
WHERE "users"."name" = ?

-- Incorreto: o primeiro ? seria tratado como valor, não como coluna
WHERE ? = ?
```

## Regras de segurança

### Valores comuns

Qualquer objeto recebido como valor por `where`, `set`, `limit` ou outra cláusula deve usar `?` sempre que aquela posição da gramática aceitar parâmetro.

```java
query.where("name", "Alex");
```

Resultado esperado:

```sql
WHERE "name" = ?
```

Parâmetros:

```text
["Alex"]
```

O valor nunca deve ser concatenado nem receber aspas manualmente. O driver JDBC cuida de texto, números, datas, booleanos, `null` e binários.

### Identificadores

Tabela, coluna, schema e alias fazem parte da estrutura SQL. Eles devem:

- ser separados em componentes (`schema.tabela.coluna`);
- ser validados como identificadores, sem aceitar comentários ou pontuação arbitrária;
- ter cada componente delimitado pelo dialeto;
- escapar o próprio delimitador caso a política permita nomes especiais;
- nunca ser recebidos como um valor comum.

Delimitadores planejados:

| Banco | Identificador | String literal |
|---|---|---|
| PostgreSQL | `"nome"` | `'texto'`, embora valores normais usem `?` |
| SQLite | `"nome"` | `'texto'`, embora valores normais usem `?` |
| MySQL | `` `nome` `` | `'texto'`, embora valores normais usem `?` |

Mesmo que o SQLite aceite sintaxes alternativas por compatibilidade, o projeto deve emitir somente a forma canônica: aspas duplas para identificadores e parâmetros para valores.

### Operadores e palavras-chave

Operadores (`=`, `<>`, `LIKE`, `ILIKE`, `GLOB`, etc.), direção (`ASC`, `DESC`) e tipo de join não podem ser parametrizados. Eles devem passar por allowlists específicas de cada dialeto.

O mecanismo atual de `Dialect.operators()` pode ser preservado e ampliado, em vez de aceitar qualquer texto.

### Coluna comparada com coluna

Uma `String` comum continuará significando valor, nunca nome de coluna. Para comparar duas colunas será necessário um tipo explícito:

```java
query.where("updated_at", ">", Sql.column("created_at"));
```

Resultado PostgreSQL/SQLite:

```sql
WHERE "updated_at" > "created_at"
```

Resultado MySQL:

```sql
WHERE `updated_at` > `created_at`
```

Sem parâmetros nesse exemplo.

### Expressões controladas

Valores como `DEFAULT`, `CURRENT_TIMESTAMP`, funções ou operações não devem ser detectados por tentativa usando o conteúdo de uma `String`. Devem usar wrappers explícitos, por exemplo:

```java
query.set("updated_at", Sql.keyword(SqlKeyword.CURRENT_TIMESTAMP));
query.where("name", "=", Sql.raw("LOWER(?)", "Alex"));
```

Os nomes são provisórios. A regra importante é que uma string fornecida como dado nunca vire SQL executável.

## Problemas observados no código atual

### `Clause` separa SQL e parâmetros

Hoje cada cláusula responde por:

```java
String query(char type);
boolean hasValue(char type);
Object value(char type);
```

O SQL e seus parâmetros são montados em passagens diferentes. Isso permite que a quantidade ou a ordem de `?` fique diferente da lista enviada ao `PreparedStatement`.

### O significado de `value()` varia

- em `Where`, `Set`, `Limit` e `Offset`, pode ser um parâmetro JDBC;
- em `Column`, `GroupBy`, `OrderBy` e `Join`, não representa um parâmetro;
- arrays e listas são expandidos fora da cláusula que produziu os placeholders.

O contrato não expressa essa diferença.

### O dialeto está reduzido a `carrot()`

`Dialect.carrot()` informa apenas um caractere delimitador. O comportamento necessário é mais amplo:

- delimitar e escapar identificadores;
- validar caminhos qualificados;
- validar operadores;
- decidir se determinada posição aceita parâmetro;
- renderizar palavras-chave controladas;
- futuramente tratar diferenças reais de sintaxe.

Além disso, `Query.normalizeIdentifier()` remove tanto `"` e crase quanto aspas simples. Aspa simples representa valor textual, não identificador, e não deve ser tratada como delimitador equivalente.

### `String` pode ter papéis ambíguos

Uma `String` pode significar valor do usuário, coluna, função ou SQL raw dependendo do método e de heurísticas. Essa ambiguidade deve ser removida nas implementações de `Clause`, mantendo compatibilidade nos métodos públicos atuais sempre que possível.

## Desenho proposto

### Ajuste aprovado: entrada `CreatedClause`

O contrato de `Dialect` terá um único método `where`, recebendo o objeto `CreatedClause` existente:

```java
RenderedClause where(CreatedClause clause, QueryType type);
```

`CreatedClause` contém `query`, `operator` opcional, `parameters` e `raw`. O construtor estruturado recebe `(String query, String operator, Object... parameters)` e define `raw = false`: informar operador já caracteriza uma condição estruturada, sem booleano adicional. O construtor de texto customizado recebe `(String query, Object... parameters)` e define `raw = true` e `operator = null`. O operador estruturado passa pela validação de `Dialect`; em raw, o texto já contém a expressão completa. `RenderedClause` continua sendo o resultado, contendo SQL e parâmetros finais. Não haverá método `whereRaw` separado em `Dialect`.

Atenção à resolução das sobrecargas Java: `new CreatedClause("name = ?", "Alex")` seleciona o construtor com operador, pois o segundo argumento é `String`. Para raw com parâmetro textual, passar explicitamente um array: `new CreatedClause("name = ?", new Object[]{"Alex"})`. Essa limitação deve ser considerada antes de integrar o contrato às cláusulas.

O formato de `query` e `parameters` quando `raw == false` ainda precisa ser definido para preservar a distinção entre coluna, operador e valor. Uma string SQL completa não deve ser dividida ou ter suas aspas reescritas por heurísticas. As propostas anteriores de sobrecargas de `where` abaixo são substituídas por este contrato único; o restante da migração continua em planejamento.

### 1. Resultado único de renderização

Cada cláusula deve produzir texto e parâmetros juntos:

```java
public final class RenderedClause {
    private final String query;
    private final Object[] parameters;

    public RenderedClause(String query, Object... parameters) {
        this.query = java.util.Objects.requireNonNull(query, "query");
        this.parameters = java.util.Objects.requireNonNull(parameters, "parameters").clone();
    }

    public String getQuery() {
        return query;
    }

    public Object[] getParameters() {
        return parameters.clone();
    }
}
```

Contrato planejado:

```java
public enum QueryType {
    SELECT, INSERT, UPDATE, DELETE
}

public interface Clause {
    RenderedClause render(ClauseContext context);
}
```

`QueryType` substitui os booleanos `isInsert`, `isUpdate` e `isDelete`. Cada renderização recebe exatamente uma operação. Consultas de contagem usam `SELECT`; a expressão de contagem é tratada na seleção.

`ClauseContext` informa:

- operação atual, representada por `QueryType`;
- dialeto;
- tabela base quando necessária.

A montagem e a ordem geral da query permanecem em `Query`; somente a forma como cada cláusula entrega seu fragmento muda.

### 2. Estratégia de dialeto

Em vez de as cláusulas consultarem apenas `carrot()`, o dialeto oferece operações semânticas:

```java
public interface Dialect {
    String normalizeOperator(String operator);

    RenderedClause where(CreatedClause clause, QueryType type);
    RenderedClause renderColumn(Column column);
    RenderedClause renderSet(Set set, ClauseContext context);
    RenderedClause renderJoin(Join join);
    RenderedClause renderGroupBy(GroupBy groupBy);
    RenderedClause renderOrderBy(OrderBy orderBy);
    RenderedClause renderLimit(Limit limit);
    RenderedClause renderOffset(Offset offset);

    // Demais métodos existentes de Dialect permanecem.
}
```

As regras ficam diretamente em `Dialect`, conforme definido na discussão. Cada função de renderização de cláusula retorna `RenderedClause`. As assinaturas acima são ilustrativas; o contexto necessário será ajustado durante o planejamento de cada implementação.

A assinatura única usa `CreatedClause`. Quando `raw` é verdadeiro, o SQL customizado é preservado como fornecido e seus parâmetros são vinculados separadamente. O dialeto não deve tentar inferir identificadores ou reescrever aspas dentro de texto arbitrário.

```java
dialect.where(new CreatedClause("LOWER(name) = LOWER(?)", new Object[]{"Alex"}), QueryType.SELECT);
dialect.where(new CreatedClause("deleted_at IS NULL"), QueryType.SELECT);
```

O retorno segue estas regras:

- `query` contém o fragmento SQL pronto para composição;
- `parameters` é sempre um `Object[]` não nulo;
- sem parâmetros: `new RenderedClause("\"deleted_at\" IS NULL")`, com array vazio;
- com parâmetros: `new RenderedClause("\"name\" = ?", "Alex")`;
- um valor SQL nulo é um parâmetro real: `new RenderedClause("\"name\" = ?", (Object) null)`;
- array vazio significa zero binds; um array contendo `null` significa um bind nulo;
- raw pode retornar qualquer formato de fragmento SQL, sem exigir coluna, operador e valor;
- o código consumidor só percorre os parâmetros existentes; com array vazio, não chama nenhum setter de parâmetro;
- array vazio não exige trocar para `Statement`: o `PreparedStatement` pode executar sem binds;
- na composição, os arrays são concatenados na mesma ordem dos fragmentos SQL.

As funções de renderização usarão `?` para os parâmetros do SQL enviado via JDBC. No PostgreSQL o driver JDBC faz a adaptação para o protocolo do servidor; a biblioteca não deve trocar manualmente por `$1`, `$2`.

Cada função em `Dialect` considera o suporte a parâmetros na posição específica, por exemplo:

- valor de comparação;
- valor de `INSERT`/`UPDATE`;
- elemento de `IN`;
- limites/paginação;
- posição estrutural que não aceita bind.

Na maior parte dos casos, a decisão vem do papel sintático, não do banco: identificadores e palavras-chave nunca são parâmetros. Diferenças por dialeto ficam centralizadas e testadas.

### 3. Tipos explícitos de argumento

Criar uma abstração interna ou pública controlada:

```text
SqlArgument
├── BoundValue       -> ?, adiciona parâmetro
├── IdentifierValue  -> identificador validado e delimitado
├── KeywordValue     -> palavra-chave de enum/allowlist
└── RawExpression    -> SQL explícito + parâmetros próprios
```

Regras:

- objetos normais da API atual viram `BoundValue`;
- somente `Sql.column(...)` cria `IdentifierValue`;
- somente enums/fábricas controladas criam `KeywordValue`;
- somente métodos terminados em `Raw` aceitam `RawExpression` arbitrária;
- nenhuma heurística converte uma string comum em SQL.

### 4. Renderização por implementação

#### `Where`

- renderiza coluna pelo dialeto;
- valida operador pelo dialeto;
- renderiza valor conforme seu tipo;
- expande `IN`/`NOT IN` para a quantidade correta de `?`;
- exige exatamente dois argumentos para `BETWEEN`;
- não adiciona parâmetro para `IS NULL`/`IS NOT NULL`;
- permite identificador ou expressão explícita do lado direito;
- retorna o fragmento e os parâmetros na mesma operação.

#### `Set`

- renderiza o nome da coluna como identificador;
- por padrão usa `coluna = ?` no `UPDATE` e `?` na área de valores do `INSERT`;
- aceita palavra-chave/expressão somente por wrapper explícito;
- não interpreta strings como funções ou palavras-chave.

#### `Column`, `GroupBy` e `OrderBy`

- recebem identificadores estruturais, não valores;
- validam e delimitam cada componente;
- `OrderBy` valida `ASC`/`DESC` por enum ou allowlist;
- funções/aliases complexos ficam na variante explícita `Raw` ou em objetos próprios.

#### `Join`

- renderiza a tabela e as colunas como identificadores;
- valida o tipo de join;
- mantém `joinRaw` como escape hatch separado;
- comparação entre colunas não gera parâmetros.

#### `Limit` e `Offset`

- tentam usar parâmetros vinculados quando o driver/dialeto aceitar essa posição;
- possuem fallback controlado apenas para números já tipados e validados, nunca para texto livre;
- o fallback formata `long`, portanto não cria superfície de SQL injection;
- são cobertos por teste real em cada driver, pois suporte teórico do banco e comportamento do JDBC podem diferir.

## Plano passo a passo

### Etapa 0 — Confirmar os casos reais

- [ ] Listar exemplos atuais nos quais um valor deveria ser outra coluna ou expressão.
- [ ] Listar posições concretas nas quais SQLite ou PostgreSQL rejeitaram `?`.
- [ ] Registrar o SQL gerado, parâmetros e erro do driver desses exemplos.
- [ ] Definir se a compatibilidade do JAR precisa ser binária ou apenas de código-fonte.

Critério de saída: conjunto de exemplos que reproduz o problema sem mudar o código.

### Etapa 1 — Testes de caracterização por driver

- [ ] Criar testes que executem uma tabela temporária/isolada em SQLite.
- [ ] Preparar a mesma matriz para PostgreSQL e MySQL quando conexões de teste estiverem disponíveis.
- [ ] Testar bind em comparação, `INSERT`, `UPDATE`, `IN`, `BETWEEN`, `LIMIT` e `OFFSET`.
- [ ] Testar que tabela, coluna, operador e direção não são enviados como `?`.
- [ ] Testar identificadores reservados e identificadores contendo o delimitador do banco.
- [ ] Testar strings com `'`, `"`, barra, comentário SQL e tentativas de injeção como valores.
- [ ] Verificar juntos SQL, quantidade de placeholders e ordem dos parâmetros.

Critério de saída: matriz objetiva indicando em quais posições cada driver aceita bind.

### Etapa 2 — Evoluir o dialeto

- [ ] Adicionar diretamente a `Dialect` os métodos de validação, delimitação e renderização de cada cláusula, retornando `RenderedClause`.
- [x] Remover `quoteIdentifier` do contrato e dos dialetos; cada renderização valida seus campos e usa o delimitador de `carrot()`.
- [ ] Não remover aspas simples como se delimitassem identificadores.
- [ ] Escapar o delimitador duplicando-o quando nomes especiais forem permitidos.
- [ ] Centralizar allowlists de operador, direção e tipo de join.
- [ ] Criar testes unitários para PostgreSQL, SQLite e MySQL.

Critério de saída: nenhuma implementação nova de cláusula concatena delimitadores manualmente.

### Etapa 3 — Criar `RenderedClause` e `ClauseContext`

- [ ] Criar `QueryType` com `SELECT`, `INSERT`, `UPDATE` e `DELETE` e usá-lo nas funções de renderização em vez de booleanos.
- [ ] Criar os dois valores imutáveis.
- [ ] Garantir cópia defensiva do `Object[]` de parâmetros.
- [ ] Representar ausência de parâmetros por array vazio e distinguir de um parâmetro cujo valor é `null`.
- [ ] Garantir que array vazio não execute chamadas de bind.
- [ ] Adicionar validação opcional: quantidade de `?` compatível com os parâmetros, respeitando SQL raw e operadores que contenham `?`.
- [ ] Manter os métodos antigos de `Clause` como adaptadores durante a migração, se a compatibilidade exigir.

Critério de saída: uma cláusula pode entregar SQL e parâmetros atomicamente.

### Etapa 4 — Criar argumentos tipados

- [ ] Implementar `BoundValue`.
- [ ] Implementar `IdentifierValue` e fábrica `Sql.column(...)`.
- [ ] Implementar palavras-chave por enum, inicialmente somente as necessárias.
- [ ] Implementar `RawExpression` com parâmetros separados.
- [ ] Garantir que a API existente converta valores comuns apenas em `BoundValue`.
- [ ] Documentar claramente qual API é segura e qual é raw.

Critério de saída: o tipo do argumento, e não o conteúdo de uma string, determina entre `?`, identificador ou expressão.

### Etapa 5 — Migrar as cláusulas

Ordem sugerida:

1. `Limit` e `Offset`;
2. `Column`, `GroupBy` e `OrderBy`;
3. `Set`;
4. `Join`;
5. `Where`.

Para cada classe:

- [ ] usar o dialeto para estrutura SQL;
- [ ] produzir parâmetros junto com o fragmento;
- [ ] remover concatenação de valores;
- [ ] cobrir variante segura e variante raw;
- [ ] executar a matriz dos três bancos antes de seguir.

Critério de saída: todas as implementações de `Clause` seguem a mesma classificação de entrada.

### Etapa 6 — Integrar sem redesenhar `Query`

- [ ] Fazer os métodos atuais de montagem consumirem `RenderedClause`, compondo a query e os arrays de parâmetros na mesma ordem.
- [ ] Preservar nomes e encadeamento dos métodos públicos.
- [ ] Preservar a ordem atual das cláusulas SQL.
- [ ] Remover gradualmente `hasValue()` e `value()` após a migração completa.
- [ ] Manter adaptadores obsoletos pelo período de compatibilidade escolhido.

Critério de saída: chamadas existentes continuam iguais, mas SQL e parâmetros passam pelo novo contrato seguro.

### Etapa 7 — Documentação e validação final

- [ ] Documentar a diferença entre valor, identificador, palavra-chave e raw.
- [ ] Adicionar exemplos de comparação valor/coluna e coluna/coluna.
- [ ] Documentar delimitadores emitidos por banco.
- [ ] Documentar posições nas quais cada driver não aceita bind e o fallback usado.
- [ ] Rodar testes de integração com SQLite, PostgreSQL e MySQL.
- [ ] Gerar o JAR e executar exemplos de compatibilidade.

## Matriz de comportamento esperada

| Entrada | Classificação | SQL | Parâmetro |
|---|---|---|---|
| `where("name", "Alex")` | valor | `"name" = ?` | `Alex` |
| `where("age", ">", 18)` | valor | `"age" > ?` | `18` |
| `where("a", "=", Sql.column("b"))` | identificador | `"a" = "b"` | nenhum |
| `whereIn("id", [1,2])` | valores | `"id" IN (?, ?)` | `1, 2` |
| `isNull("deleted_at")` | palavra-chave | `"deleted_at" IS NULL` | nenhum |
| `set("updated_at", CURRENT_TIMESTAMP)` | palavra-chave controlada | `"updated_at" = CURRENT_TIMESTAMP` | nenhum |
| `orderBy("name", ASC)` | identificador + enum | `"name" ASC` | nenhum |
| `whereRaw("LOWER(name) = LOWER(?)", "Alex")` | raw explícito | texto informado | `Alex` |

Os exemplos da tabela usam aspas de PostgreSQL/SQLite. No MySQL, o dialeto produzirá crases.

## Decisões pendentes

- Compatibilidade exigida: **pendente**.
- Local das regras: **definido — interface `Dialect`**.
- Operação da query: **definido — enum `QueryType`, substituindo os booleanos**.
- Assinatura única para WHERE: **definido — `RenderedClause where(CreatedClause clause, QueryType type)`; `CreatedClause.isRaw()` diferencia a entrada customizada**.
- Resultado de cada função de cláusula: **definido — `RenderedClause`, com `String query` e `Object[] parameters`; array vazio quando não houver parâmetros**.
- `SqlArgument` fará parte da API pública ou será inicialmente interno: **pendente**.
- Funções SQL terão objetos próprios ou usarão somente `RawExpression` na primeira versão: **pendente**.
- Política de identificadores: regex restritiva ou qualquer nome com escape correto: **pendente**.
- Casos reais de posições que rejeitam `?`: **pendente de exemplos/erros**.

## Próxima ação

Antes de implementar, adicionar a este documento dois ou três exemplos que atualmente falham, contendo:

1. chamada Java;
2. SQL gerado;
3. parâmetros enviados;
4. banco/versão do driver;
5. mensagem de erro esperada ou recebida.

Com esses casos, fechamos a matriz da Etapa 1 e confirmamos se o fallback pertence realmente ao dialeto ou ao papel sintático da entrada.

## Referências técnicas para a implementação

- [SQLite — parâmetros em expressões](https://sqlite.org/lang_expr.html#varparam)
- [SQLite — palavras-chave e formas de delimitar identificadores](https://sqlite.org/lang_keywords.html)
- [PostgreSQL — identificadores e constantes](https://www.postgresql.org/docs/current/sql-syntax-lexical.html)
- [pgJDBC — uso de `?` em `PreparedStatement`](https://jdbc.postgresql.org/documentation/query/)
- [MySQL — parâmetros somente em posições de valores](https://dev.mysql.com/doc/refman/9.7/en/prepare.html)
- [MySQL — identificadores qualificados](https://dev.mysql.com/doc/refman/9.7/en/identifier-qualifiers.html)
