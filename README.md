# Checkout com colaboradores simulados

Leia o arquivo `Enunciado-alunos.md`. O objetivo é implementar `ServicoCheckout` por TDD, testando seus quatro colaboradores com Mockito, sem banco de dados, rede ou Spring.

## Requisitos

Use JDK 21 ou superior e Maven 3.8 ou superior.

O projeto fixa JUnit Jupiter 5.13.4, Mockito 5.18.0 e Java release 21 para reprodução. O primeiro uso do Maven precisa de acesso à internet para baixar as dependências.

## Execução

Na pasta que contém o `pom.xml`, execute:

```bash
mvn test
```

Execute `mvn test` para validar a implementação.

Para conferir somente a compilação, incluindo a dos testes, sem executá-los:

```bash
mvn test-compile
```

O Maven Surefire já recebe a opção de agente Mockito para Java 21+. Na IDE, prefira delegar a execução ao Maven para usar a mesma configuração.

## Estrutura

`src/main/java/br/edu/checkout` contém records, interfaces, exceções e a classe do serviço com método pendente. `src/test/java/br/edu/checkout/ServicoCheckoutTest.java` contém apenas o primeiro teste. O `pom.xml` contém as dependências e a configuração de execução.

Os mocks representam estoque, pagamento, persistência e notificação porque são colaboradores externos. O serviço e os records são objetos reais.

O teste de compra aprovada verifica estado no `ResultadoCheckout` e no `PedidoConfirmado` capturado. Os demais testes verificam interações, ausência de chamadas, compensação e ordem dos efeitos externos. O captor revela defeitos nos dados enviados ao repositório, como total, cliente, SKU ou transação incorretos. A ordem é parte do contrato porque cobrar antes de reservar ou notificar antes de persistir altera os efeitos observáveis do checkout.

O registro dos três ciclos de TDD está em `TDD.md`.
