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
   // Mocks do Servidor Web
   @Mock HttpServletRequest request;
   @Mock HttpServletResponse response;
  
   // Mocks das Dependências de Negócio
   @Mock ValidadorCookie validadorMock;
   @Mock DaoLanche daoLancheMock;
   @Mock DaoIngrediente daoIngredienteMock;
   @BeforeEach
   public void setup() {
       MockitoAnnotations.openMocks(this);
   }
   // Subclasse para injetar os Mocks
   class salvarLancheComMocks extends salvarLanche {
       @Override protected ValidadorCookie getValidadorCookie() { return validadorMock; }
       @Override protected DaoLanche getDaoLanche() { return daoLancheMock; }
       @Override protected DaoIngrediente getDaoIngrediente() { return daoIngredienteMock; }
   }
   @Test
   public void testProcessRequest_FluxoSucesso_SalvaLancheEIngredientes() throws Exception {
       // --- 1. PREPARAÇÃO (ARRANGE) ---
      
       // A) Cookie Válido
       when(validadorMock.validarFuncionario(any())).thenReturn(true);
       when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "admin")});
       // B) JSON de Entrada (Simulando o Front-end)
       String jsonInput = "{" +
               "\"nome\": \"X-Salada\"," +
               "\"descricao\": \"Muito bom\"," +
               "\"ValorVenda\": 20.0," +
               "\"ingredientes\": {" +
                   "\"Alface\": 1," +
                   "\"Tomate\": 2" +
               "}" +
               "}";
       mockInputStream(request, jsonInput);
       // C) Banco de Dados (Simulando sucesso ao salvar e buscar)
       // Quando pedir para pesquisar o lanche que acabamos de salvar, retorna um objeto válido
       Lanche lancheRetorno = new Lanche();
       lancheRetorno.setId_lanche(10); // ID fictício
       when(daoLancheMock.pesquisaPorNome(any(Lanche.class))).thenReturn(lancheRetorno);
       // Quando pesquisar ingrediente, retorna válido
       Ingrediente ingRetorno = new Ingrediente();
       ingRetorno.setId_ingrediente(5);
       when(daoIngredienteMock.pesquisaPorNome(any(Ingrediente.class))).thenReturn(ingRetorno);
       // D) Capturar resposta
       StringWriter sw = new StringWriter();
       when(response.getWriter()).thenReturn(new PrintWriter(sw));
       // --- 2. EXECUÇÃO (ACT) ---
       salvarLanche servlet = new salvarLancheComMocks();
       servlet.processRequest(request, response);
       // --- 3. VERIFICAÇÃO (ASSERT) ---
       String output = sw.toString().trim();
      
       // Verifica se a mensagem de sucesso apareceu
       assertTrue(output.contains("Lanche Salvo com Sucesso!"), "Falha! Retornou: " + output);
      
       // Verifica se tentou salvar o lanche
       verify(daoLancheMock, times(1)).salvar(any(Lanche.class));
      
       // Verifica se vinculou os ingredientes (Alface e Tomate = 2 chamadas)
       verify(daoLancheMock, times(2)).vincularIngrediente(any(Lanche.class), any(Ingrediente.class));
   }
   @Test
   public void testProcessRequest_CookieInvalido_RetornaErro() throws Exception {
       // --- PREPARAÇÃO ---
       when(validadorMock.validarFuncionario(any())).thenReturn(false); // Acesso negado
       when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "hacker")});
       mockInputStream(request, "{}"); // JSON irrelevante aqui
       StringWriter sw = new StringWriter();
       when(response.getWriter()).thenReturn(new PrintWriter(sw));
       // --- EXECUÇÃO ---
       salvarLanche servlet = new salvarLancheComMocks();
       servlet.processRequest(request, response);
       // --- VERIFICAÇÃO ---
       assertTrue(sw.toString().contains("erro"), "Deveria bloquear acesso inválido");
       verify(daoLancheMock, never()).salvar(any()); // Garante que não tocou no banco
   }
   // Helper para simular o JSON vindo da requisição
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
}
