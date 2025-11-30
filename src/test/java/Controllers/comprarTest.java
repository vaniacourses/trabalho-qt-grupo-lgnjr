package Controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.DriverManager; // <--- Necessário para criar a conexão real

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import DAO.DaoBebida;
import DAO.DaoCliente;
import DAO.DaoLanche;
import DAO.DaoPedido;
import DAO.DaoUtil; // <--- Importante para interceptar a conexão
import Helpers.ValidadorCookie;
import Model.Cliente;
import Model.Lanche;
import Model.Pedido;

import Model.Lanche;
import Model.Bebida;
import DAO.DaoLanche;
import DAO.DaoBebida;
import static org.mockito.ArgumentMatchers.eq;      // Necessário para o teste novo
import static org.mockito.ArgumentMatchers.argThat; // Necessário para o teste novo

public class comprarTest {

    // =========================================================================================
    //                                  MÉTODOS AUXILIARES
    // =========================================================================================
    
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

    // =========================================================================================
    //                                  PARTE 1: TESTES UNITÁRIOS
    // =========================================================================================
    
    public class comprarParaTesteUnitario extends comprar {
        ValidadorCookie validadorMock;
        DaoPedido daoPedidoMock;
        DaoCliente daoClienteMock;

        public comprarParaTesteUnitario(ValidadorCookie v, DaoPedido dp, DaoCliente dc) {
            this.validadorMock = v;
            this.daoPedidoMock = dp;
            this.daoClienteMock = dc;
        }

        @Override protected ValidadorCookie getValidadorCookie() { return validadorMock; }
        @Override protected DaoPedido getDaoPedido() { return daoPedidoMock; }
        @Override protected DaoCliente getDaoCliente() { return daoClienteMock; }
        @Override protected DaoLanche getDaoLanche() { return null; }
        @Override protected DaoBebida getDaoBebida() { return null; }
    }

    @Test
    public void UNITARIO_testeFluxoSimples_ComMocks() throws Exception {
        // Mocks
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ValidadorCookie validador = mock(ValidadorCookie.class);
        DaoPedido daoPedido = mock(DaoPedido.class);
        DaoCliente daoCliente = mock(DaoCliente.class);

        // Comportamento
        when(validador.validar(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("teste", "teste")});
        
        Cliente clienteFake = new Cliente();
        clienteFake.setId_cliente(1);
        when(daoCliente.pesquisaPorID(anyString())).thenReturn(clienteFake);

        mockInputStream(request, "{\"id\": 1}");

        StringWriter textoSaida = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(textoSaida));

        // Execução
        comprar servlet = new comprarParaTesteUnitario(validador, daoPedido, daoCliente);
        servlet.processRequest(request, response);

        // Verificação
        verify(daoPedido, times(1)).salvar(any(Pedido.class));
        assertTrue(textoSaida.toString().contains("Pedido Salvo") || textoSaida.toString().contains("ok"));
    }
    
    @Test
    public void UNITARIO_testeFluxoCompleto_LancheEBebida_CalculaPrecoEVincula() throws Exception {
        // --- 1. CRIAÇÃO DOS MOCKS ---
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ValidadorCookie validador = mock(ValidadorCookie.class);
        DaoPedido daoPedido = mock(DaoPedido.class);
        DaoCliente daoCliente = mock(DaoCliente.class);
        
        // Mocks adicionais para Produtos (Lanche e Bebida)
        DaoLanche daoLanche = mock(DaoLanche.class);
        DaoBebida daoBebida = mock(DaoBebida.class);

        // --- 2. CONFIGURAÇÃO DOS COMPORTAMENTOS (STUBS) ---
        
        // Autenticação OK
        when(validador.validar(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth", "ok")});
        
        // Cliente existe
        Cliente clienteFake = new Cliente();
        clienteFake.setId_cliente(1);
        when(daoCliente.pesquisaPorID(anyString())).thenReturn(clienteFake);

        // Configurar o retorno do Lanche (Simulando o Banco)
        Lanche lancheFake = new Lanche();
        lancheFake.setId_lanche(10);
        lancheFake.setValor_venda(20.0); // Preço unitário: 20.0
        when(daoLanche.pesquisaPorNome("X-Bacon")).thenReturn(lancheFake);

        // Configurar o retorno da Bebida (Simulando o Banco)
        Bebida bebidaFake = new Bebida();
        bebidaFake.setId_bebida(20);
        bebidaFake.setValor_venda(5.0); // Preço unitário: 5.0
        when(daoBebida.pesquisaPorNome("Coca")).thenReturn(bebidaFake);

        // Mockar o retorno do pedido salvo (necessário para o método vincular funcionar)
        Pedido pedidoSalvo = new Pedido();
        pedidoSalvo.setId_pedido(123); // ID gerado pelo banco
        // Quando o sistema tentar buscar o pedido recém salvo, retornamos este objeto com ID
        when(daoPedido.pesquisaPorData(any(Pedido.class))).thenReturn(pedidoSalvo);

        // --- 3. JSON DE ENTRADA ---
        // Simula o JSON enviado pelo Front-end:
        // "NomeProduto": ["descrição", "tipo", quantidade]
        String jsonInput = "{" +
                "\"id\": 1," +
                "\"X-Bacon\": [\"delicioso\", \"lanche\", 2]," + 
                "\"Coca\": [\"gelada\", \"bebida\", 1]" +        
                "}";
        mockInputStream(request, jsonInput);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // --- 4. EXECUÇÃO ---
        
        // Instanciamos a classe de teste, mas SOBRESCREVENDO os métodos de Lanche e Bebida
        // que na classe 'comprarParaTesteUnitario' original retornavam null.
        comprar servlet = new comprarParaTesteUnitario(validador, daoPedido, daoCliente) {
            @Override protected DaoLanche getDaoLanche() { return daoLanche; }
            @Override protected DaoBebida getDaoBebida() { return daoBebida; }
        };
        
        servlet.processRequest(request, response);

        // --- 5. VERIFICAÇÕES (ASSERTIONS) ---
        
        // A) Verifica se os produtos foram buscados no banco
        verify(daoLanche, times(1)).pesquisaPorNome("X-Bacon");
        verify(daoBebida, times(1)).pesquisaPorNome("Coca");

        // B) Verifica se o cálculo do valor total está correto usando ArgumentCaptor
        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(daoPedido).salvar(captor.capture()); // Captura o pedido enviado para o salvar
        
        Pedido pedidoCapturado = captor.getValue();
        
        // Lógica do seu código: soma o valor de venda de cada item encontrado
        // 20.0 (Lanche) + 5.0 (Bebida) = 25.0
        assertEquals(25.0, pedidoCapturado.getValor_total(), 0.01, "O Valor Total do pedido está incorreto!");

        // C) Verifica se os vínculos foram feitos corretamente
        // Garante que vinculou o Lanche correto ao Pedido correto
        verify(daoPedido, times(1)).vincularLanche(eq(pedidoSalvo), any(Lanche.class));
        
        // Garante que vinculou a Bebida correta ao Pedido correto
        verify(daoPedido, times(1)).vincularBebida(eq(pedidoSalvo), any(Bebida.class));
    }
    
    @Test
    public void UNITARIO_testeErro_CookieInvalido_ImprimeErro() throws Exception {
        // --- 1. Mocks ---
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ValidadorCookie validador = mock(ValidadorCookie.class);

        // --- 2. Comportamento: Validação Falha ---
        when(validador.validar(any())).thenReturn(false); // <--- AQUI FORÇAMOS O ERRO
        // Mesmo que tenha cookies, o validador diz não
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth", "invalido")});
        
        // Input stream não importa muito aqui, mas colocamos vazio para não dar erro de null
        mockInputStream(request, "{}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // --- 3. Execução ---
        // Não precisamos dos DAOs aqui pois vai falhar antes
        comprar servlet = new comprarParaTesteUnitario(validador, null, null);
        servlet.processRequest(request, response);

        // --- 4. Verificação ---
        String saida = sw.toString().trim();
        
        // Cobre a linha: out.println("erro");
        assertTrue(saida.contains("erro"), "Deveria ter imprimido 'erro' no console");
    }

    // =========================================================================================
    //                                  PARTE 2: TESTES DE INTEGRAÇÃO
    // =========================================================================================
    
    public class comprarParaTesteIntegracao extends comprar {
        ValidadorCookie validadorMock;

        public comprarParaTesteIntegracao(ValidadorCookie v) {
            this.validadorMock = v;
        }

        @Override protected ValidadorCookie getValidadorCookie() { return validadorMock; }
        // DAOs REAIS são usados aqui (DaoPedido e DaoCliente originais)
    }

    @Test
    public void INTEGRACAO_testeSalvarNoBancoReal() throws Exception {
        // ... Mocks de Infra Web (Iguais) ...
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ValidadorCookie validador = mock(ValidadorCookie.class);

        when(validador.validar(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("sessao", "valida")});
        
        // Vamos tentar comprar com o Cliente ID 1
        mockInputStream(request, "{\"id\": 1}"); 

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // Interceptando a conexão para usar localhost
        try (MockedConstruction<DaoUtil> mockedDaoUtil = Mockito.mockConstruction(DaoUtil.class,
                (mock, context) -> {
                    when(mock.conecta()).thenAnswer(invocation -> {
                        return DriverManager.getConnection(
                            "jdbc:postgresql://localhost:5432/lanchonete", 
                            "postgres", "123456"); // Suas credenciais
                    });
                })) {

            // =====================================================================
            // [NOVO] SETUP DE DADOS: INSERIR CLIENTE 1 ANTES DE TESTAR
            // =====================================================================
            // Como o banco é real, precisamos criar o cliente ou o teste falha (FK Error)
            try (java.sql.Connection conn = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/lanchonete", "postgres", "123456")) {
                
                // 1. Limpa o cliente 1 se já existir (para não dar erro de duplicidade)
                // Nota: Talvez precise limpar pedidos antes por causa da FK, mas vamos tentar assim
                java.sql.PreparedStatement stmtDelete = conn.prepareStatement(
                    "DELETE FROM tb_clientes WHERE id_cliente = 1");
                try { stmtDelete.execute(); } catch (Exception ignore) {} // Ignora se falhar por FK

                // 2. Insere o Cliente 1 na marra
                java.sql.PreparedStatement stmtInsert = conn.prepareStatement(
                    "INSERT INTO tb_clientes (id_cliente, nome, sobrenome, telefone, usuario, senha, fg_ativo) " +
                    "VALUES (1, 'Usuario', 'Teste', '1199999999', 'user_teste', '123', 1) " +
                    "ON CONFLICT (id_cliente) DO NOTHING" // Se seu Postgres for novo suporta isso
                );
                stmtInsert.execute();
            }
            // =====================================================================

            // Agora sim, rodamos a Servlet
            comprar servlet = new comprarParaTesteIntegracao(validador);
            servlet.processRequest(request, response);
        }

        // Verificação
        String saida = sw.toString();
        assertTrue(saida.contains("Pedido Salvo") || saida.contains("ok"), 
            "Falha na integração. Saída: " + saida);
            
        System.out.println("Teste de Integração passou! Cliente criado e Pedido salvo.");
    }
}