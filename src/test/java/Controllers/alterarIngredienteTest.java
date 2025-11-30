package Controllers;

import DAO.DaoIngrediente;
import Helpers.ValidadorCookie;
import Model.Ingrediente;
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

public class alterarIngredienteTest {

    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock ValidadorCookie validadorMock;
    @Mock DaoIngrediente daoIngredienteMock;

    @BeforeEach
    void iniciar() {
        MockitoAnnotations.openMocks(this);
    }
    
    // Helper para simular o corpo da requisição JSON
    private void mockInputStream(String json) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());
        ServletInputStream servletInputStream = new ServletInputStream() {
            public int read() throws IOException { return bais.read(); }
            public boolean isFinished() { return bais.available() == 0; }
            public boolean isReady() { return true; }
            public void setReadListener(ReadListener readListener) {}
        };
        when(request.getInputStream()).thenReturn(servletInputStream);
    }
    
    // Subclasse que injeta os mocks (Garantindo Isolamento)
    class AlterarIngredienteControlado extends alterarIngrediente {
        @Override protected ValidadorCookie getValidadorCookie() { return validadorMock; }
        @Override protected DaoIngrediente getDaoIngrediente() { return daoIngredienteMock; }
    }


    // 1. TESTE  FELIZ
    
    @Test
    void testeAlteracao_ComDadosValidos_DeveChamarAlterarDAO() throws Exception {
        // Cenário: Login de funcionário autorizado
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth", "ok")});
        
        // JSON de entrada com todos os dados preenchidos corretamente
        String jsonInput = "{\"id\": 15, \"nome\": \"Pao Frances\", \"descricao\": \"Pao fresquinho\", \"quantidade\": 10, \"ValorCompra\": 0.50, \"ValorVenda\": 1.00, \"tipo\": \"PAO\"}";
        mockInputStream(jsonInput);

        // Captura a saída
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // Execução
        alterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.processRequest(request, response);

        // Verificação: 
        // 1. Deve retornar a mensagem de sucesso
        assertTrue(sw.toString().contains("Ingrediente Alterado!"), "Deveria retornar mensagem de sucesso.");
        // 2. Deve chamar o método DAO.alterar() exatamente uma vez
        verify(daoIngredienteMock, times(1)).alterar(any(Ingrediente.class));
        
        // 3.  Captura o objeto para verificar se o ID foi passado corretamente
        ArgumentCaptor<Ingrediente> captor = ArgumentCaptor.forClass(Ingrediente.class);
        verify(daoIngredienteMock).alterar(captor.capture());
        assertTrue(captor.getValue().getId_ingrediente() == 15, "O ID do ingrediente não foi extraído corretamente do JSON.");
    }

    //  2. TESTE DE FLUXO DE FALHA: AUTENTICAÇÃO 
    
    @Test
    void testeAlteracao_SemAutenticacao_DeveRetornarErro() throws Exception {
        // Cenário: Cookie é inválido
        when(validadorMock.validarFuncionario(any())).thenReturn(false);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth", "bad")});
        
        // JSON Válido para garantir que a falha não é no JSON
        mockInputStream("{\"id\": 15, \"nome\": \"Pao\"}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // Execução
        alterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.processRequest(request, response);

        // Verificação: Deve retornar o erro de não autorizado e NÃO deve chamar o DAO
        assertTrue(sw.toString().contains("Não autorizado"), "Deveria retornar erro de não autorizado.");
        verify(daoIngredienteMock, never()).alterar(any());
    }
    
    // 3. TESTE DE FALHA: DADOS INVÁLIDOS (JSON/TIPO)

    @Test
    void testeAlteracao_ComLetrasNoID_DeveRetornarErro() throws Exception {
        // Cenário: Tentativa de alterar com letras no campo ID (que é int)
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth", "ok")});
        
        // ID inválido no JSON - Isso força o JSONException no .getInt("id")
        String jsonInput = "{\"id\": \"ABC\", \"nome\": \"Pao\"}";
        mockInputStream(jsonInput); 

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // Execução
        alterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.processRequest(request, response);

        // Verificação: Deve cair no catch(JSONException) e retornar erro
        assertTrue(sw.toString().contains("Erro no formato dos dados"), "Deveria retornar erro por formato numérico inválido.");
        verify(daoIngredienteMock, never()).alterar(any());
    }
    
    //  4. TESTE DE FALHA: REQUISIÇÃO VAZIA

    @Test
    void testeAlteracao_ComJsonNuloOuVazio_DeveRetornarErro() throws Exception {
        // Cenário: Autorizado, mas o corpo da requisição está vazio
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth", "ok")});
        
        // Simula JSON vazio (br.readLine() retorna null ou string vazia)
        mockInputStream(" "); 

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // Execução
        alterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.processRequest(request, response);

        // Verificação: Deve retornar o erro de dados ausentes
        assertTrue(sw.toString().contains("Dados ausentes"), "Deveria retornar erro por falta de dados.");
        verify(daoIngredienteMock, never()).alterar(any());
    }
}