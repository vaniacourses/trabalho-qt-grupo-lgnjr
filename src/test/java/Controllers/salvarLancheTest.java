package Controllers;

import DAO.DaoIngrediente;
import DAO.DaoLanche;
import Helpers.ValidadorCookie;
import Model.Ingrediente;
import Model.Lanche;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class salvarLancheTest {

    @org.mockito.Mock HttpServletRequest request;
    @org.mockito.Mock HttpServletResponse response;
    @org.mockito.Mock ValidadorCookie validadorMock;

    @org.mockito.Mock DaoLanche daoLancheMock;
    @org.mockito.Mock DaoIngrediente daoIngredienteMock;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // ============================================================
    // UTILITÁRIO PARA SIMULAR JSON NO INPUTSTREAM
    // ============================================================
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

    // ============================================================
    // WRAPPER PARA INJETAR MOCKS
    // ============================================================
    class SalvarLancheComMocks extends SalvarLanche {
        @Override protected ValidadorCookie getValidadorCookie() { return validadorMock; }
        @Override protected DaoLanche getDaoLanche() { return daoLancheMock; }
        @Override protected DaoIngrediente getDaoIngrediente() { return daoIngredienteMock; }
    }

    // ============================================================
    // WRAPPER COM DAOs "FAKE" PARA INTEGRAÇÃO PARCIAL
    // ============================================================
    class SalvarLancheComDAOsSimples extends SalvarLanche {
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

    // ============================================================
    // TESTES UNITÁRIOS
    // ============================================================

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

        Lanche lancheRet = new Lanche();
        lancheRet.setId_lanche(10);
        when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenReturn(lancheRet);

        Ingrediente ingrRet = new Ingrediente();
        ingrRet.setId_ingrediente(5);
        when(daoIngredienteMock.pesquisaPorNome(any(Ingrediente.class))).thenReturn(ingrRet);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        ArgumentCaptor<Lanche> lancheCaptor = ArgumentCaptor.forClass(Lanche.class);
        ArgumentCaptor<Ingrediente> ingredienteCaptor = ArgumentCaptor.forClass(Ingrediente.class);

        SalvarLanche servlet = new SalvarLancheComMocks();
        servlet.processRequest(request, response);

        // verifica LANCHE salvo
        verify(daoLancheMock).salvar(lancheCaptor.capture());
        Lanche salvo = lancheCaptor.getValue();
        assertEquals(NOME, salvo.getNome());
        assertEquals(DESCRICAO, salvo.getDescricao());
        assertEquals(VALOR, salvo.getValor_venda(), 0.001);

        // verifica INGREDIENTE vinculado
        verify(daoLancheMock).vincularIngrediente(any(), ingredienteCaptor.capture());
        assertEquals(QUANT_ING, ingredienteCaptor.getValue().getQuantidade());

        assertTrue(sw.toString().contains("Lanche Salvo com Sucesso!"));
    }

    @Test
    public void UNITARIO_testProcessRequest_FluxoSucesso() throws Exception {
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("admin","token") });

        String json = "{"
                + "\"nome\":\"X-Salada Mock\","
                + "\"descricao\":\"Muito bom\","
                + "\"ValorVenda\":20.0,"
                + "\"ingredientes\": {\"Alface\":1, \"Tomate\":2}"
                + "}";

        mockInputStream(request, json);

        Lanche retorno = new Lanche();
        retorno.setId_lanche(10);
        when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenReturn(retorno);

        Ingrediente ing = new Ingrediente();
        ing.setId_ingrediente(5);
        when(daoIngredienteMock.pesquisaPorNome(any(Ingrediente.class))).thenReturn(ing);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        SalvarLanche servlet = new SalvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Lanche Salvo com Sucesso!"));
        verify(daoLancheMock).salvar(any());
        verify(daoLancheMock, times(2)).vincularIngrediente(any(), any());
    }

    @Test
    public void UNITARIO_testCookieInvalido() throws Exception {
        when(validadorMock.validarFuncionario(any())).thenReturn(false);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("token","hacker") });

        mockInputStream(request, "{}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        SalvarLanche servlet = new SalvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"));
        verify(daoLancheMock, never()).salvar(any());
    }

    // ============================================================
    // COBERTURA E CASOS EXCEPCIONAIS
    // ============================================================

    @Test
    public void COBERTURA_CookiesNulos() throws Exception {
        when(request.getCookies()).thenReturn(null);

        mockInputStream(request, "{}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        SalvarLanche servlet = new SalvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"));
    }

    @Test
    public void COBERTURA_InputStreamNulo() throws Exception {
        when(request.getInputStream()).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        SalvarLanche servlet = new SalvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"));
    }

    @Test
    public void COBERTURA_JSONVazio() throws Exception {
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("token","admin") });

        mockInputStream(request, "");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        SalvarLanche servlet = new SalvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"));
    }

    @Test
    public void COBERTURA_LancheNaoEncontrado() throws Exception {
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("token","admin") });

        mockInputStream(request, "{\"nome\":\"Teste\"}");

        when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        SalvarLanche servlet = new SalvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"));
    }

    @Test
    public void COBERTURA_IngredienteNaoEncontrado() throws Exception {
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("token","admin") });

        String json = "{ \"nome\":\"Lanche Misto\",\"ingredientes\":{\"IngredienteInexistente\":1} }";
        mockInputStream(request, json);

        Lanche retorno = new Lanche();
        retorno.setId_lanche(10);

        when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenReturn(retorno);
        when(daoIngredienteMock.pesquisaPorNome(any())).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        SalvarLanche servlet = new SalvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Lanche Salvo com Sucesso!"));
        verify(daoLancheMock, never()).vincularIngrediente(any(), any());
    }

    @Test
    public void COBERTURA_ExcecaoPesquisaPorNome() throws Exception {
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("ok","1") });

        String json = "{\"nome\":\"X\",\"descricao\":\"Y\",\"ValorVenda\":10,\"ingredientes\":{\"Pao\":1}}";
        mockInputStream(request, json);

        when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenThrow(new RuntimeException("falha"));

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        SalvarLanche servlet = new SalvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"));
    }

    // ============================================================
    // TESTES DE INTEGRAÇÃO
    // ============================================================

    @Test
    public void INTEGRACAO_ComDAOSimples() throws Exception {
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

        SalvarLanche servlet = new SalvarLancheComDAOsSimples();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Lanche Salvo com Sucesso!"));
    }

}
