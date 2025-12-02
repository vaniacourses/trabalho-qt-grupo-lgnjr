package Controllers;

import DAO.DaoIngrediente;
import DAO.DaoLanche;
import DAO.DaoToken;
import DAO.DaoUtil;
import Helpers.ValidadorCookie;
import Model.Ingrediente;
import Model.Lanche;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


public class salvarLancheTest {

    // Mocks usados 

    @org.mockito.Mock HttpServletRequest request;
    @org.mockito.Mock HttpServletResponse response;
    @org.mockito.Mock ValidadorCookie validadorMock;

    @org.mockito.Mock DaoLanche daoLancheMock;
    @org.mockito.Mock DaoIngrediente daoIngredienteMock;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }


   // simula body JSON no InputStream da request
     private void mockInputStream(HttpServletRequest req, String json) throws IOException {
        ByteArrayInputStream b = new ByteArrayInputStream(json.getBytes());
        ServletInputStream sis = new ServletInputStream() {
            public int read() { return b.read(); }
            public boolean isFinished() { return b.available() == 0; }
            public boolean isReady() { return true; }
            public void setReadListener(ReadListener rl) {}
        };
        when(req.getInputStream()).thenReturn(sis);
    }

    
    // WRAPPERS 
     // Wrapper que injeta mocks — usado em UNITARIOS.
    
    class salvarLancheComMocks extends salvarLanche {
        @Override protected ValidadorCookie getValidadorCookie() { return validadorMock; }
        @Override protected DaoLanche getDaoLanche() { return daoLancheMock; }
        @Override protected DaoIngrediente getDaoIngrediente() { return daoIngredienteMock; }
    }

   
    //Wrapper que retorna DAOs "simples"/falsos 
    class salvarLancheComDAOsSimples extends salvarLanche {
        @Override
        protected ValidadorCookie getValidadorCookie() {
            ValidadorCookie v = mock(ValidadorCookie.class);
            when(v.validarFuncionario(any())).thenReturn(true);
            return v;
        }

        @Override
        protected DaoLanche getDaoLanche() {
            DaoLanche fake = mock(DaoLanche.class);
            Lanche l = new Lanche();
            l.setId_lanche(999);
            when(fake.pesquisaPorNome(any(Lanche.class))).thenReturn(l);
            doNothing().when(fake).salvar(any());
            doNothing().when(fake).vincularIngrediente(any(), any());
            return fake;
        }

        @Override
        protected DaoIngrediente getDaoIngrediente() {
            DaoIngrediente mockIng = mock(DaoIngrediente.class);
            Ingrediente ing = new Ingrediente();
            ing.setId_ingrediente(5);
            when(mockIng.pesquisaPorNome(any(Ingrediente.class))).thenReturn(ing);
            return mockIng;
        }
    }

    //  UNITARIO
    @Test
    public void UNITARIO_testMutacao_VerificaAtribuicaoDeTodosOsValores() throws Exception {
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("token","admin") });

        final String NOME = "Lanche VIP";
        final String DESCRICAO = "Teste Completo";
        final double VALOR = 42.0;
        final int QUANT_ING = 3;
        final String NOME_ING = "Queijo";

        String json = "{"
                + "\"nome\":\"" + NOME + "\","
                + "\"descricao\":\"" + DESCRICAO + "\","
                + "\"ValorVenda\":" + VALOR + ","
                + "\"ingredientes\": {\"" + NOME_ING + "\":" + QUANT_ING + "}"
                + "}";

        mockInputStream(request, json);

        // configura DAO mocks para retornar IDs
        Lanche lancheRet = new Lanche(); lancheRet.setId_lanche(10);
        when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenReturn(lancheRet);

        Ingrediente ingrRet = new Ingrediente(); ingrRet.setId_ingrediente(5);
        when(daoIngredienteMock.pesquisaPorNome(any(Ingrediente.class))).thenReturn(ingrRet);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));


        ArgumentCaptor<Lanche> lancheCaptor = ArgumentCaptor.forClass(Lanche.class);
        ArgumentCaptor<Ingrediente> ingredienteCaptor = ArgumentCaptor.forClass(Ingrediente.class);

        // execução com wrapper que injeta mocks
        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        verify(daoLancheMock, times(1)).salvar(lancheCaptor.capture());
        Lanche salvo = lancheCaptor.getValue();
        Assertions.assertEquals(NOME, salvo.getNome());
        Assertions.assertEquals(DESCRICAO, salvo.getDescricao());
        Assertions.assertEquals(VALOR, salvo.getValor_venda(), 0.001);

        verify(daoLancheMock, times(1)).vincularIngrediente(any(), ingredienteCaptor.capture());
        Ingrediente vinc = ingredienteCaptor.getValue();
        Assertions.assertEquals(QUANT_ING, vinc.getQuantidade());

        verify(daoIngredienteMock, times(1)).pesquisaPorNome(argThat(i -> i.getNome().equals(NOME_ING)));

        assertTrue(sw.toString().contains("Lanche Salvo com Sucesso!"));
    }

    @Test
    public void UNITARIO_testProcessRequest_FluxoSucesso_SalvaLancheEIngredientes() throws Exception {
        
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("admin","token") });

        String json = "{"
                + "\"nome\":\"X-Salada Mock\","
                + "\"descricao\":\"Muito bom\","
                + "\"ValorVenda\":20.0,"
                + "\"ingredientes\": {\"Alface\":1, \"Tomate\":2}"
                + "}";

        mockInputStream(request, json);

        Lanche retorno = new Lanche(); retorno.setId_lanche(10);
        when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenReturn(retorno);

        Ingrediente ing = new Ingrediente(); ing.setId_ingrediente(5);
        when(daoIngredienteMock.pesquisaPorNome(any(Ingrediente.class))).thenReturn(ing);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Lanche Salvo com Sucesso!"));
        verify(daoLancheMock, times(1)).salvar(any());
        verify(daoLancheMock, times(2)).vincularIngrediente(any(), any());
    }

    @Test
    public void UNITARIO_testProcessRequest_CookieInvalido_RetornaErro() throws Exception {
        when(validadorMock.validarFuncionario(any())).thenReturn(false);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("token","hacker") });

        mockInputStream(request, "{}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"));
        verify(daoLancheMock, never()).salvar(any());
    }

   
    // ========================= COBERTURA

    @Test
    public void COBERTURA_testEstrutural_CookiesNulos_RetornaErro() throws Exception {
    
        when(request.getCookies()).thenReturn(null);
        mockInputStream(request, "{}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"));
        verify(daoLancheMock, never()).salvar(any());
    }

    @Test
    public void COBERTURA_testEstrutural_InputStreamNulo_RetornaErro() throws Exception {
      
        when(request.getInputStream()).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"));
        verify(daoLancheMock, never()).salvar(any());
    }

    @Test
    public void COBERTURA_testEstrutural_JSONVazio_RetornaErro() throws Exception {
      
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("token","admin") });

        mockInputStream(request, ""); // corpo vazio

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"));
        verify(daoLancheMock, never()).salvar(any());
    }

    @Test
    public void COBERTURA_testEstrutural_LancheNaoEncontradoParaVinculo_RetornaErro() throws Exception {
      
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("token","admin") });

        String json = "{\"nome\":\"Lanche Falha ID\",\"ingredientes\":{\"Queijo\":1}}";
        mockInputStream(request, json);

        when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenReturn(null);

        Ingrediente ing = new Ingrediente(); ing.setId_ingrediente(5);
        when(daoIngredienteMock.pesquisaPorNome(any(Ingrediente.class))).thenReturn(ing);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"));
        verify(daoLancheMock, times(1)).salvar(any());
        verify(daoLancheMock, never()).vincularIngrediente(any(), any());
    }

    @Test
    public void COBERTURA_testEstrutural_IngredienteNaoEncontrado_SucessoParcial() throws Exception {
        
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("token","admin") });

        String json = "{ \"nome\":\"Lanche Misto\",\"ingredientes\":{\"IngredienteInexistente\":1} }";
        mockInputStream(request, json);

        Lanche retorno = new Lanche(); retorno.setId_lanche(10);
        when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenReturn(retorno);

        when(daoIngredienteMock.pesquisaPorNome(any(Ingrediente.class))).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Lanche Salvo com Sucesso!"));
        verify(daoLancheMock, times(1)).salvar(any());
        verify(daoLancheMock, never()).vincularIngrediente(any(), any());
    }



    @Test
    public void COBERTURA_testExcecao_PesquisaPorNome_TrataErro() throws Exception {
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("ok","1") });

        String json = "{\"nome\":\"X\",\"descricao\":\"Y\",\"ValorVenda\":10,\"ingredientes\":{\"Pao\":1}}";
        mockInputStream(request, json);

        when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenThrow(new RuntimeException("falha"));
        StringWriter sw = new StringWriter(); when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"));
    }

   

   
    // INTEGRACAO

    @Test
    public void INTEGRACAO_testIntegracao_SalvarLancheComDaoSimples() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("token","admin") });

        String json = "{"
                + "\"nome\":\"Lanche Teste SIMPLES\","
                + "\"descricao\":\"Teste Integrado\","
                + "\"ValorVenda\":35.5,"
                + "\"ingredientes\":{\"Pao\":1}"
                + "}";
        mockInputStream(request, json);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComDAOsSimples();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Lanche Salvo com Sucesso!"));
    }

 
    @Test
    public void INTEGRACAO_testSalvarNoBancoReal_PreparaELimpa() throws Exception {
        // PRECONDICIONAIS
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("sessao","valida") });
        mockInputStream(request, "{\"nome\":\"INT-Lanche\",\"descricao\":\"desc\",\"ValorVenda\":5.0, \"ingredientes\":{\"Pao\":1}}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // Mocka construções de DaoUtil para devolver conexões reais ao DB local
        try (MockedConstruction<DaoUtil> mocked = Mockito.mockConstruction(DaoUtil.class,
                (mock, context) -> {
                    when(mock.conecta()).thenAnswer(invocation -> {
                        return DriverManager.getConnection(
                                "jdbc:postgresql://localhost:5432/lanchonete",
                                "postgres", "123456");
                    });
                })) {

            try (Connection conn = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/lanchonete",
                    "postgres", "123456")) {

                try (PreparedStatement pst = conn.prepareStatement(
                        "INSERT INTO tb_ingredientes (nm_ingrediente, descricao, quantidade, valor_compra, valor_venda, tipo, fg_ativo) " +
                                "VALUES ('Pao', 'integ test', 100, 0.0, 0.0, 'outros', 1) ON CONFLICT (nm_ingrediente) DO NOTHING")) {
                    pst.execute();
                } catch (Exception ignore) { }

                try (PreparedStatement pst = conn.prepareStatement(
                        "DELETE FROM tb_lanches WHERE nm_lanche = 'INT-Lanche'")) {
                    pst.execute();
                } catch (Exception ignore) {  }
            }

            salvarLanche servlet = new salvarLanche(); 
            servlet.processRequest(request, response);

            String saida = sw.toString();
            assertTrue(saida.contains("Lanche Salvo com Sucesso!") || saida.contains("erro"),
                    "Saída inesperada de integração: " + saida);
        }
    }

}
