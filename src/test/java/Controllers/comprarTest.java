package Controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.DriverManager;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import DAO.DaoBebida;
import DAO.DaoCliente;
import DAO.DaoLanche;
import DAO.DaoPedido;
import DAO.DaoUtil;
import Helpers.ValidadorCookie;
import Model.Bebida;
import Model.Cliente;
import Model.Lanche;
import Model.Pedido;

public class comprarTest {

    // ----------------- MÉTODO AUXILIAR PARA MOCKAR O INPUT STREAM -----------------
    private void mockInputStream(HttpServletRequest req, String json) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(json.getBytes());
        ServletInputStream servletInputStream = new ServletInputStream() {
            public int read() throws IOException { return byteArrayInputStream.read(); }
            public boolean isFinished() { return byteArrayInputStream.available() == 0; }
            public boolean isReady() { return true; }
            public void setReadListener(ReadListener readListener) {}
        };
        when(req.getInputStream()).thenReturn(servletInputStream);
    }

    // ----------------- WRAPPER PARA TESTE UNITÁRIO -----------------
    public class comprarParaTesteUnitario extends Comprar {
        ValidadorCookie validadorMock;
        DaoPedido daoPedidoMock;
        DaoCliente daoClienteMock;
        DaoLanche daoLancheMock;
        DaoBebida daoBebidaMock;

        public comprarParaTesteUnitario(ValidadorCookie v, DaoPedido dp, DaoCliente dc, DaoLanche dl, DaoBebida db) {
            this.validadorMock = v;
            this.daoPedidoMock = dp;
            this.daoClienteMock = dc;
            this.daoLancheMock = dl;
            this.daoBebidaMock = db;
        }

        @Override protected ValidadorCookie getValidadorCookie() { return validadorMock; }
        @Override protected DaoPedido getDaoPedido() { return daoPedidoMock; }
        @Override protected DaoCliente getDaoCliente() { return daoClienteMock; }
        @Override protected DaoLanche getDaoLanche() { return daoLancheMock != null ? daoLancheMock : mock(DaoLanche.class); }
        @Override protected DaoBebida getDaoBebida() { return daoBebidaMock != null ? daoBebidaMock : mock(DaoBebida.class); }
    }

    // ----------------- TESTE UNITÁRIO: FLUXO SIMPLES -----------------
    @Test
    public void UNITARIO_testeFluxoSimples_VerificaSetters() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ValidadorCookie validador = mock(ValidadorCookie.class);
        DaoPedido daoPedido = mock(DaoPedido.class);
        DaoCliente daoCliente = mock(DaoCliente.class);

        when(validador.validar(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("teste", "teste")});

        Cliente clienteFake = new Cliente();
        clienteFake.setId_cliente(99);
        when(daoCliente.pesquisaPorID(anyString())).thenReturn(clienteFake);

        mockInputStream(request, "{\"id\": 99}");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        Comprar servlet = new comprarParaTesteUnitario(validador, daoPedido, daoCliente, null, null);
        servlet.processRequest(request, response);

        verify(response).setCharacterEncoding("UTF-8");
        verify(response).setContentType("application/json");

        ArgumentCaptor<Pedido> pedidoCaptor = ArgumentCaptor.forClass(Pedido.class);
        verify(daoPedido).salvar(pedidoCaptor.capture());

        Pedido pedidoCapturado = pedidoCaptor.getValue();
        assertNotNull(pedidoCapturado.getCliente());
        assertEquals(99, pedidoCapturado.getCliente().getId_cliente());
    }

    // ----------------- TESTE UNITÁRIO: CÁLCULOS -----------------
    @Test
    public void UNITARIO_testeCalculos_E_AtribuicaoQuantidade() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ValidadorCookie validador = mock(ValidadorCookie.class);
        DaoPedido daoPedido = mock(DaoPedido.class);
        DaoCliente daoCliente = mock(DaoCliente.class);
        DaoLanche daoLanche = mock(DaoLanche.class);
        DaoBebida daoBebida = mock(DaoBebida.class);

        when(validador.validar(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth", "ok")});
        when(daoCliente.pesquisaPorID(anyString())).thenReturn(new Cliente());

        Lanche lancheFake = new Lanche();
        lancheFake.setValor_venda(10.0);
        when(daoLanche.pesquisaPorNome("Burguer")).thenReturn(lancheFake);

        Bebida bebidaFake = new Bebida();
        bebidaFake.setValor_venda(5.0);
        when(daoBebida.pesquisaPorNome("Refri")).thenReturn(bebidaFake);

        Pedido pedidoSalvo = new Pedido();
        pedidoSalvo.setId_pedido(123);
        when(daoPedido.pesquisaPorData(any(Pedido.class))).thenReturn(pedidoSalvo);

        String jsonInput = "{\"id\": 1,\"Burguer\": [\"desc\", \"lanche\", 3],\"Refri\": [\"desc\", \"bebida\", 2]}";
        mockInputStream(request, jsonInput);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        Comprar servlet = new comprarParaTesteUnitario(validador, daoPedido, daoCliente, daoLanche, daoBebida);
        servlet.processRequest(request, response);

        ArgumentCaptor<Pedido> pedidoCaptor = ArgumentCaptor.forClass(Pedido.class);
        verify(daoPedido).salvar(pedidoCaptor.capture());
        assertEquals(40.0, pedidoCaptor.getValue().getValor_total(), 0.01);

        ArgumentCaptor<Lanche> lancheCaptor = ArgumentCaptor.forClass(Lanche.class);
        verify(daoPedido).vincularLanche(eq(pedidoSalvo), lancheCaptor.capture());
        assertEquals(3, lancheCaptor.getValue().getQuantidade());

        ArgumentCaptor<Bebida> bebidaCaptor = ArgumentCaptor.forClass(Bebida.class);
        verify(daoPedido).vincularBebida(eq(pedidoSalvo), bebidaCaptor.capture());
        assertEquals(2, bebidaCaptor.getValue().getQuantidade());
    }

    // ----------------- TESTE UNITÁRIO: COOKIE INVÁLIDO -----------------
    @Test
    public void UNITARIO_testeErro_CookieInvalido() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ValidadorCookie validador = mock(ValidadorCookie.class);

        when(validador.validar(any())).thenReturn(false);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth", "mock")});

        mockInputStream(request, "{}");
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        Comprar servlet = new comprarParaTesteUnitario(validador, null, null, null, null);
        servlet.processRequest(request, response);

        assertTrue(sw.toString().trim().contains("erro"));
    }

    // cobertura de metodos

    @Test
    public void UNITARIO_testeMetodosProtegidos_GarantirInstancias() {
        // testa se os métodos getDao...() retornam instâncias válidas


        Comprar servletReal = new Comprar();

        // 1. ValidadorCookie
        assertNotNull(servletReal.getValidadorCookie(), "getValidadorCookie() não deve retornar null");

        // interceptar quando o DAO fizer "new DaoUtil()"
        try (MockedConstruction<DaoUtil> mockedDaoUtil = Mockito.mockConstruction(DaoUtil.class,
                (mock, context) -> {
                    java.sql.Connection conexaoFake = mock(java.sql.Connection.class);
                    when(mock.conecta()).thenReturn(conexaoFake);
                })) {



            DaoCliente daoCliente = servletReal.getDaoCliente();
            assertNotNull(daoCliente, "getDaoCliente() não deve retornar null");

            DaoPedido daoPedido = servletReal.getDaoPedido();
            assertNotNull(daoPedido, "getDaoPedido() não deve retornar null");

            DaoLanche daoLanche = servletReal.getDaoLanche();
            assertNotNull(daoLanche, "getDaoLanche() não deve retornar null");

            DaoBebida daoBebida = servletReal.getDaoBebida();
            assertNotNull(daoBebida, "getDaoBebida() não deve retornar null");
        }
    }

    // conrindo todas-arestas

    @Test
    public void COBERTURA_testeCatchNullPointer() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ValidadorCookie validador = mock(ValidadorCookie.class);

        mockInputStream(request, "{}");
        when(request.getCookies()).thenThrow(new NullPointerException()); // Força Exception

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        Comprar servlet = new comprarParaTesteUnitario(validador, null, null, null, null);
        servlet.processRequest(request, response);

        assertTrue(sw.toString().trim().contains("erro"));
    }

    @Test
    public void COBERTURA_testeItensDesconhecidos_Ou_BancoNull() throws Exception {
        // Cobre os 'else' dos IFs de tipo e verificação de null do banco
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ValidadorCookie validador = mock(ValidadorCookie.class);
        DaoPedido daoPedido = mock(DaoPedido.class);
        DaoCliente daoCliente = mock(DaoCliente.class);
        DaoLanche daoLanche = mock(DaoLanche.class);
        DaoBebida daoBebida = mock(DaoBebida.class);

        when(validador.validar(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth", "ok")});
        when(daoCliente.pesquisaPorID(anyString())).thenReturn(new Cliente());

        // Simula itens não encontrados no banco
        when(daoLanche.pesquisaPorNome("X-Fantasma")).thenReturn(null);
        when(daoBebida.pesquisaPorNome("Suco-Fantasma")).thenReturn(null);


        String jsonInput = "{" +
                "\"id\": 1," +
                "\"Movel\": [\"desc\", \"mesa\", 1]," + // Tipo desconhecido
                "\"X-Fantasma\": [\"...\", \"lanche\", 1]," + // Retorna null do banco
                "\"Suco-Fantasma\": [\"...\", \"bebida\", 1]" + // Retorna null do banco
                "}";
        mockInputStream(request, jsonInput);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        Comprar servlet = new comprarParaTesteUnitario(validador, daoPedido, daoCliente, daoLanche, daoBebida);
        servlet.processRequest(request, response);

        // Verifica que NADA foi vinculado (pois tudo era inválido)
        verify(daoPedido, never()).vincularLanche(any(), any());
        verify(daoPedido, never()).vincularBebida(any(), any());

        // Verifica que mesmo assim tentou salvar o pedido (comportamento atual do código)
        verify(daoPedido).salvar(any(Pedido.class));
    }

    @Test
    public void COBERTURA_testePedidoSalvoNull() throws Exception {
        // Testa quando if (pedidoSalvo != null) -> else
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ValidadorCookie validador = mock(ValidadorCookie.class);
        DaoPedido daoPedido = mock(DaoPedido.class);
        DaoCliente daoCliente = mock(DaoCliente.class);

        when(validador.validar(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth", "ok")});
        when(daoCliente.pesquisaPorID(anyString())).thenReturn(new Cliente());

        // DAO retorna null após salvar
        when(daoPedido.pesquisaPorData(any(Pedido.class))).thenReturn(null);

        mockInputStream(request, "{\"id\": 1}");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        Comprar servlet = new comprarParaTesteUnitario(validador, daoPedido, daoCliente, null, null);
        servlet.processRequest(request, response);

        verify(daoPedido).salvar(any(Pedido.class));
    }

    @Test
    public void COBERTURA_testeDoPost() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ValidadorCookie validador = mock(ValidadorCookie.class);
        mockInputStream(request, "{}");

        when(validador.validar(any())).thenReturn(false);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        Comprar servlet = new comprarParaTesteUnitario(validador, null, null, null, null);
        servlet.doPost(request, response);
        verify(request).getCookies();
    }

    // integração com banco

    public class comprarParaTesteIntegracao extends Comprar {
        ValidadorCookie validadorMock;
        public comprarParaTesteIntegracao(ValidadorCookie v) { this.validadorMock = v; }
        @Override protected ValidadorCookie getValidadorCookie() { return validadorMock; }
    }

    @Test
    public void INTEGRACAO_testeSalvarNoBancoReal() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ValidadorCookie validador = mock(ValidadorCookie.class);

        when(validador.validar(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("sessao", "valida")});
        mockInputStream(request, "{\"id\": 1}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        try (MockedConstruction<DaoUtil> mockedDaoUtil = Mockito.mockConstruction(DaoUtil.class,
                (mock, context) -> {
                    when(mock.conecta()).thenAnswer(invocation -> {
                        return DriverManager.getConnection(
                            "jdbc:postgresql://localhost:5432/lanchonete",
                            "postgres", "123456");
                    });
                })) {

            try (java.sql.Connection conn = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/lanchonete", "postgres", "123456")) {

                // Limpeza e Preparação do Banco
                java.sql.PreparedStatement stmtDelete = conn.prepareStatement(
                    "DELETE FROM tb_clientes WHERE id_cliente = 1");
                try { stmtDelete.execute(); } catch (Exception ignore) {}

                java.sql.PreparedStatement stmtInsert = conn.prepareStatement(
                    "INSERT INTO tb_clientes (id_cliente, nome, sobrenome, telefone, usuario, senha, fg_ativo) " +
                    "VALUES (1, 'Usuario', 'Teste', '1199999999', 'user_teste', '123', 1) " +
                    "ON CONFLICT (id_cliente) DO NOTHING"
                );
                stmtInsert.execute();
            }

            Comprar servlet = new comprarParaTesteIntegracao(validador);
            servlet.processRequest(request, response);
        }

        String saida = sw.toString();
        assertTrue(saida.contains("Pedido Salvo") || saida.contains("ok"),
            "Falha na integração. Saída: " + saida);
    }
}