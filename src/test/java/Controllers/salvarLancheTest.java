package Controllers;

import DAO.DaoIngrediente;
import DAO.DaoLanche;
import Helpers.ValidadorCookie;
import Model.Ingrediente;
import Model.Lanche;

import org.junit.jupiter.api.Assertions; 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class salvarLancheTest {

    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock ValidadorCookie validadorMock;

    @Mock DaoLanche daoLancheMock;
    @Mock DaoIngrediente daoIngredienteMock;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // Subclasse que injeta os mocks (para Testes Unitários)
    class salvarLancheComMocks extends salvarLanche {
        @Override protected ValidadorCookie getValidadorCookie() { return validadorMock; }
        @Override protected DaoLanche getDaoLanche() { return daoLancheMock; }
        @Override protected DaoIngrediente getDaoIngrediente() { return daoIngredienteMock; }
    }

    // =======================================================================
    // VERSÃO CORRIGIDA — TESTE DE "INTEGRAÇÃO" SEM CHAMAR BANCO
    // (NÃO USA MAIS DaoLancheSimples, NEM CHAMA super())
    // =======================================================================

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

    // =======================================================================
    // TESTE DE MUTAÇÃO
    // =======================================================================

    @Test
    public void testMutacao_VerificaAtribuicaoDeTodosOsValores() throws Exception {

        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "admin")});

        final String NOME = "Lanche VIP";
        final String DESCRICAO = "Teste Completo";
        final double VALOR = 42.0; 
        final int QUANTIDADE_INGR = 3;
        final String NOME_INGR = "Queijo";

        String jsonInput = "{"
                + "\"nome\": \"" + NOME + "\","
                + "\"descricao\": \"" + DESCRICAO + "\","
                + "\"ValorVenda\": " + VALOR + ","
                + "\"ingredientes\": {\"" + NOME_INGR + "\": " + QUANTIDADE_INGR + "}"
                + "}";

        mockInputStream(request, jsonInput);

        Lanche lancheRetorno = new Lanche();
        lancheRetorno.setId_lanche(10); 
        when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenReturn(lancheRetorno);

        Ingrediente ingRetorno = new Ingrediente();
        ingRetorno.setId_ingrediente(5);
        when(daoIngredienteMock.pesquisaPorNome(any(Ingrediente.class))).thenReturn(ingRetorno);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        ArgumentCaptor<Lanche> lancheCaptor = ArgumentCaptor.forClass(Lanche.class);
        ArgumentCaptor<Ingrediente> ingredienteCaptor = ArgumentCaptor.forClass(Ingrediente.class);

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        verify(daoLancheMock, times(1)).salvar(lancheCaptor.capture());
        Lanche lancheSalvo = lancheCaptor.getValue();

        Assertions.assertEquals(NOME, lancheSalvo.getNome());
        Assertions.assertEquals(DESCRICAO, lancheSalvo.getDescricao());
        Assertions.assertEquals(VALOR, lancheSalvo.getValor_venda(), 0.001);

        verify(daoLancheMock, times(1)).vincularIngrediente(any(), ingredienteCaptor.capture());
        Ingrediente ingredienteVinculado = ingredienteCaptor.getValue();
        Assertions.assertEquals(QUANTIDADE_INGR, ingredienteVinculado.getQuantidade());

        verify(daoIngredienteMock, times(1)).pesquisaPorNome(argThat(ing -> 
            ing.getNome().equals(NOME_INGR)
        ));

        assertTrue(sw.toString().contains("Lanche Salvo com Sucesso!"));
    }

    // =======================================================================
    // TESTES ESTRUTURAIS (ERROS E BRANCHES)
    // =======================================================================

    @Test
    public void testProcessRequest_FluxoSucesso_SalvaLancheEIngredientes() throws Exception {

        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("admin", "token")});

        String jsonInput = "{"
                + "\"nome\": \"X-Salada Mock\","
                + "\"descricao\": \"Muito bom\","
                + "\"ValorVenda\": 20.0,"
                + "\"ingredientes\": {\"Alface\": 1, \"Tomate\": 2}"
                + "}";

        mockInputStream(request, jsonInput);

        Lanche retorno = new Lanche();
        retorno.setId_lanche(10);
        when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenReturn(retorno);

        Ingrediente ing = new Ingrediente();
        ing.setId_ingrediente(5);
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
    public void testProcessRequest_CookieInvalido_RetornaErro() throws Exception {
        when(validadorMock.validarFuncionario(any())).thenReturn(false);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "hacker")});

        mockInputStream(request, "{}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"));
        verify(daoLancheMock, never()).salvar(any());
    }

    @Test
    public void testEstrutural_CookiesNulos_RetornaErro() throws Exception {
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
    public void testEstrutural_InputStreamNulo_RetornaErro() throws Exception {
        when(request.getInputStream()).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"));
        verify(daoLancheMock, never()).salvar(any()); 
    }

    @Test
    public void testEstrutural_JSONVazio_RetornaErro() throws Exception {
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "admin")});

        mockInputStream(request, "");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"));
        verify(daoLancheMock, never()).salvar(any()); 
    }

    @Test
    public void testEstrutural_LancheNaoEncontradoParaVinculo_RetornaErro() throws Exception {

        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "admin")});

        String jsonInput =
            "{\"nome\": \"Lanche Falha ID\", \"ingredientes\": {\"Queijo\": 1}}";

        mockInputStream(request, jsonInput);

        when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenReturn(null);

        Ingrediente ing = new Ingrediente();
        ing.setId_ingrediente(5);
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
    public void testEstrutural_IngredienteNaoEncontrado_SucessoParcial() throws Exception {

        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "admin")});

        String jsonInput =
            "{ \"nome\": \"Lanche Misto\", \"ingredientes\": {\"IngredienteInexistente\": 1} }";

        mockInputStream(request, jsonInput);

        Lanche retorno = new Lanche();
        retorno.setId_lanche(10);
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

    // =======================================================================
    // TESTE DE "INTEGRAÇÃO" (SIMULADO, 100% SEM BANCO)
    // =======================================================================

    @Test
    public void testIntegracao_SalvarLancheComDaoSimples() throws Exception {

        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "admin")});

        String jsonInput = "{"
                + "\"nome\": \"Lanche Teste SIMPLES\","
                + "\"descricao\": \"Teste Integrado\","
                + "\"ValorVenda\": 35.5,"
                + "\"ingredientes\": {\"Pao\": 1}"
                + "}";

        mockInputStream(request, jsonInput);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComDAOsSimples();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Lanche Salvo com Sucesso!"));
    }

    // =======================================================================
    // MÉTODO AUXILIAR
    // =======================================================================

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
}
