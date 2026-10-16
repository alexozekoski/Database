# Renderização de cláusulas

O contrato e suas implementações ficam em `github.alexozk.database.dialect`: `Dialect`, `PostgreSQLDialect`, `SQLiteDialect` e `MySQLDialect`. O acesso pelo banco é `database.getDialect()`; a constante de cada classe de banco é `DIALECT`. Código consumidor deve atualizar os imports e os acessos aos nomes antigos.

`Dialect` declara as assinaturas do contrato e reúne os métodos estáticos comuns aos três bancos. `PostgreSQLDialect`, `SQLiteDialect` e `MySQLDialect` implementam separadamente as funções de instância e delegam aos estáticos compartilhados quando o comportamento é igual. Operadores, delimitadores e particularidades como o limite implícito de paginação continuam em cada banco. Não há métodos `default` na interface. Os auxiliares usados somente pelos estáticos compartilhados são privados em `Dialect`.

## Entrada e resultado

```java
Dialect dialect = new PostgreSQLDialect();

CreatedClause condition = new CreatedClause("users.name", "=", "O'Brien");
RenderedClause result = dialect.where(condition, QueryType.SELECT);
// result.getQuery(): "users"."name" = ?
// result.getParameters(): ["O'Brien"]

RenderedClause custom = dialect.where(
        CreatedClause.raw("LOWER(name) = LOWER(?)", "Alex"),
        QueryType.SELECT);
```

`CreatedClause.raw(...)` evita a ambiguidade entre os dois construtores quando o primeiro parâmetro é uma string. Não é um método separado em `Dialect`: ambos os casos usam `where(CreatedClause, QueryType)`.

O SQL raw é preservado, incluindo aspas e operadores. Seu texto deve vir do código da aplicação; dados externos devem entrar pelo array de parâmetros. O renderizador não tenta corrigir ou interpretar SQL arbitrário.

`RenderedClause` contém um fragmento `String` e os parâmetros ordenados em `Object[]`, com cópias defensivas do array. Array vazio significa zero binds. Um array contendo `null` significa um parâmetro de valor nulo. O objeto não converte os valores nem exige trocar `PreparedStatement` por `Statement`.

## Funções de Dialect

| Função | Entrada estruturada | Resultado |
|---|---|---|
| `where` | `CreatedClause` com coluna, operador e valores | Predicado, sem a palavra `WHERE` |
| `column` | `String` ou `CreatedClause` | Identificador ou seleção raw |
| `set` | coluna e valor, ou `CreatedClause` | Atribuição no UPDATE; expressão do valor no INSERT |
| `join` | tipo, tabela e duas colunas, ou `CreatedClause` raw | JOIN completo, incluindo `ON` |
| `groupBy` | coluna ou `CreatedClause` | Expressão, sem `GROUP BY` |
| `orderBy` | coluna e direção, ou `CreatedClause` | Expressão e direção, sem `ORDER BY` |
| `limit` / `offset` | `long` | Fragmento do valor, sem palavra-chave |

Todas recebem `QueryType`. Para uma seleção ou agrupamento estruturado com `CreatedClause`, use um operador marcador e nenhum parâmetro, por exemplo `new CreatedClause("users.id", "=")`; a sobrecarga `column("users.id", type)` é mais simples. Em `orderBy`, o campo `operator` recebe `ASC` ou `DESC`. Um JOIN estruturado usa a sobrecarga com os quatro argumentos próprios; um JOIN customizado usa o mesmo método `join` com `CreatedClause.raw(...)`.

Cada função de renderização valida os campos recebidos. Não existem métodos `quoteIdentifier` ou `quoteIdentifierPath` no contrato de `Dialect`. Os delimitadores vêm de `carrot()`: PostgreSQL/SQLite usam aspas duplas; MySQL usa crases. Identificadores são delimitados por componente (`schema.table.column`), com duplicação de delimitadores internos. Nomes qualificados já delimitados são aceitos na renderização de cláusulas. Aspas simples não são interpretadas como delimitadores de identificadores. Funções e expressões arbitrárias devem usar entrada raw na nova API.

`IN` e `BETWEEN` aceitam parâmetros individuais, listas ou um array passado como objeto. `byte[]` permanece um único parâmetro binário. Na atribuição por `set`, cada valor é um único parâmetro; arrays não são expandidos automaticamente em várias colunas.

## Integração com Query

Os nomes e o encadeamento dos métodos fluentes continuam disponíveis. `Query` armazena entradas `CreatedClause` por categoria e chama diretamente as funções de `Dialect`, coletando SQL e parâmetros de `RenderedClause`. Os antigos arquivos `Clause`, `Column`, `Where`, `Set`, `Join`, `GroupBy`, `OrderBy`, `Limit` e `Offset` do pacote `query` foram removidos. As classes `Column` de `model` e `migration` permanecem. `clearSelects()` passa a retornar `List<CreatedClause>`; `ModelUtil.getJoinColumn()` passa a retornar `CreatedClause[]`.

```java
database.query().table("users")
        .where(new CreatedClause("age", ">", 18))
        .orWhere(CreatedClause.raw("LOWER(name) = LOWER(?)", "Alex"))
        .orderByAsc("id")
        .limit(10);
```

As convenções legadas de funções SQL em seleção/agrupamento/ordenação são preservadas pelo builder. Os novos métodos de dialeto com identificadores não inferem funções pelo conteúdo da string.

## Comportamentos definidos nesta implementação

- `IN` vazio produz `1 = 0`; `NOT IN` vazio produz `1 = 1`. O builder não descarta mais silenciosamente o filtro vazio.
- `BETWEEN` exige dois valores; comparações escalares exigem um.
- `IS NULL` e `IS NOT NULL` exigem zero parâmetros na nova API. A chamada legada `where("name", "IS NULL", null)` continua funcionando.
- `IS` estruturado aceita `Boolean` ou `null` e gera somente as palavras-chave controladas `TRUE`, `FALSE` ou `NULL`, evitando um placeholder em posição não aceita por PostgreSQL/MySQL. Para outras expressões específicas de SQLite, use raw.
- Valores negativos de paginação são rejeitados. A paginação estruturada é suportada em `SELECT`; paginação de escrita não faz parte do contrato portátil.
- `OFFSET` sem limite recebe `LIMIT -1` em SQLite e `LIMIT 18446744073709551615` em MySQL. PostgreSQL não precisa de limite implícito.
- O suporte de JOIN depende da versão do servidor. Por exemplo, o SQLite 3.34 incluído no projeto não executa RIGHT JOIN; raw não adiciona suporte a recursos ausentes no servidor.

## Verificação

O teste independente `tests/ClauseRenderingTest.java` verifica os três renderizadores, integração com o builder e execução SQLite em memória. Não precisa de banco externo nem cria arquivos de banco.

Na raiz do projeto, com JDK 17 ou posterior (PowerShell):

```powershell
$taskBuild = Join-Path ([System.IO.Path]::GetTempPath()) ('database-tests-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $taskBuild | Out-Null
$taskSources = @(rg --files src tests -g '*.java')
javac --release 17 -encoding UTF-8 -cp 'lib/*' -d $taskBuild $taskSources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
java -cp "$taskBuild;lib/*" ClauseRenderingTest
```

Os testes de PostgreSQL e MySQL validam SQL e parâmetros sem conexão de rede. A execução real nesses servidores depende de ambientes de teste próprios; não foi realizada nesta implementação.
