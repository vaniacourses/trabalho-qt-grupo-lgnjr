package Controllers;

import DAO.DaoIngrediente;
import DAO.DaoLanche;
import Helpers.ValidadorCookie;
import Model.Ingrediente;
import Model.Lanche;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    // Subclasse que injeta os mocks
    class salvarLancheComMocks extends salvarLanche {
        @Override protected ValidadorCookie getValidadorCookie() { return validadorMock; }
        @Override protected DaoLanche getDaoLanche() { return daoLancheMock; }
        @Override protected DaoIngrediente getDaoIngrediente() { return daoIngredienteMock; }
    }

    // Subclasse para integração (Mockando DAOs para não precisar de DB)
    class salvarLancheComDAOsSimples extends salvarLanche {
        @Override protected ValidadorCookie getValidadorCookie() {
            ValidadorCookie v = mock(ValidadorCookie.class);
            when(v.validarFuncionario(any())).thenReturn(true);
            return v;
        }

        @Override protected DaoLanche getDaoLanche() {
            DaoLanche mockDao = mock(DaoLanche.class);

            doNothing().when(mockDao).salvar(any(Lanche.class));

            Lanche l = new Lanche();
            l.setId_lanche(999);

            when(mockDao.pesquisaPorNome(any(Lanche.class))).thenReturn(l);

            doNothing().when(mockDao).vincularIngrediente(any(Lanche.class), any(Ingrediente.class));

            return mockDao;
        }

        @Override protected DaoIngrediente getDaoIngrediente() {
            DaoIngrediente mockIng = mock(DaoIngrediente.class);

            Ingrediente ing = new Ingrediente();
            ing.setId_ingrediente(5);

            when(mockIng.pesquisaPorNome(any(Ingrediente.class))).thenReturn(ing);

            return mockIng;
        }
    }

    // =======================================================================
    // TESTES FUNCIONAIS E ESTRUTURAIS (PARA COBERTURA)
    // =======================================================================

    @Test
    public void testProcessRequest_FluxoSucesso_SalvaLancheEIngredientes() throws Exception {
        // Cobre: Cookies OK, br != null, JSON OK, lancheComID OK, ingredientes OK, ingredienteComID OK.
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "admin")});

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
        // Cobre: resultado == false (acesso negado)
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

    // =======================================================================
    // NOVOS TESTES ESTRUTURAIS PARA AUMENTAR COBERTURA (+80% BRANCHES)
    // =======================================================================
    
    @Test
    public void testEstrutural_InputStreamNulo_RetornaErro() throws Exception {
        // Cobre: if (request.getInputStream() != null) ser FALSE (br == null)
        when(request.getInputStream()).thenReturn(null);
        
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"), "Deveria retornar erro quando InputStream é nulo (br == null).");
    }

    @Test
    public void testEstrutural_JSONVazio_RetornaErro() throws Exception {
        // Cobre: if (json != null && !json.trim().isEmpty()) ser FALSE
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "admin")});

        // Simula um corpo de requisição com JSON vazio (para br.readLine() retornar "")
        mockInputStream(request, ""); 

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"), "Deveria retornar erro com JSON vazio.");
        verify(daoLancheMock, never()).salvar(any()); 
    }

    @Test
    public void testEstrutural_LancheNaoEncontradoParaVinculo_RetornaErro() throws Exception {
        // Cobre: if (lancheComID != null && ingredientes != null) ser FALSE (lancheComID == null)
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "admin")});

        String jsonInput = "{\"nome\": \"Lanche Falha ID\", \"ingredientes\": {\"Queijo\": 1}}";
        mockInputStream(request, jsonInput);

        // Mocka a pesquisaPorNome (após salvar) para retornar NULL
        when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenReturn(null); 
        
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"), "Deveria retornar erro se Lanche ID for nulo após salvar.");
        verify(daoLancheMock, times(1)).salvar(any());
        verify(daoLancheMock, never()).vincularIngrediente(any(), any());
    }
    
    @Test
    public void testEstrutural_IngredienteNaoEncontrado_SucessoParcial() throws Exception {
        // Cobre: if (ingredienteComID != null) ser FALSE (apenas o ingrediente não é vinculado, mas o lanche é salvo)
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "admin")});

        String jsonInput = "{"
                + "\"nome\": \"Lanche Misto\","
                + "\"ingredientes\": {\"IngredienteInexistente\": 1}"
                + "}";
        mockInputStream(request, jsonInput);

        Lanche retorno = new Lanche();
        retorno.setId_lanche(10);
        when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenReturn(retorno);

        // PREPARAÇÃO: Mocka a pesquisa de ingrediente para retornar NULL (ingredienteComID == null)
        when(daoIngredienteMock.pesquisaPorNome(any(Ingrediente.class))).thenReturn(null); 

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        salvarLanche servlet = new salvarLancheComMocks();
        servlet.processRequest(request, response);

        // VERIFICAÇÃO: O sucesso ainda é esperado porque a falha no ingrediente é tratada
        // e o fluxo principal do lanche continua. O vínculo (vincularIngrediente) NÃO deve ser chamado.
        assertTrue(sw.toString().contains("Lanche Salvo com Sucesso!"), "Deve ser sucesso mesmo sem vincular o ingrediente.");
        verify(daoLancheMock, times(1)).salvar(any());
        verify(daoLancheMock, never()).vincularIngrediente(any(), any()); // Garante que o if/else correto foi pego.
    }

    // TESTE DE "INTEGRAÇÃO" (Mantido como estava no código anterior)
   
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

  
    // MÉTODO AUXILIAR
   
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