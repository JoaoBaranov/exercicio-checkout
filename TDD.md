# Registro de TDD

## Ciclo 1

- Teste: `deveRejeitarSolicitacaoNulaSemConsultarColaboradores`
- Red: `UnsupportedOperationException` em vez de `IllegalArgumentException`.
- Green: validação da solicitação nula.
- Refactor: validação centralizada no método privado `validar`.

## Ciclo 2

- Teste: `deveFinalizarCompraAprovadaComOrdemEPedidoCorretos`
- Red: o fluxo ainda não calculava o total nem coordenava os colaboradores.
- Green: cálculo do total, reserva, cobrança, persistência e notificação.
- Refactor: construção do pedido e retorno do resultado permaneceram no fluxo principal por refletirem a ordem do contrato.

## Ciclo 3

- Teste: `deveRetornarCompraRegistradaQuandoNotificacaoFalha`
- Red: a falha de notificação era propagada.
- Green: captura de `FalhaNotificacaoException` e retorno com `notificacaoEnviada = false`.
- Refactor: nenhum refactor adicional foi necessário.

