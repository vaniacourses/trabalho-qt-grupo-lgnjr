package Controllers;

import DAO.DaoLanche;
import DAO.DaoUtil;
import Helpers.ValidadorCookie;
import Model.Lanche;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class getLanchesTest {

    // =========================================================================================
    //                                  PARTE 1: CONFIGURAÇÃO GERAL
    // =========================================================================================

    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock ValidadorCookie validadorMock;
    @Mock DaoLanche daoLancheMock; // Usado só nos testes unitários

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // =========================================================================================
    //                                  PARTE 2: TESTES UNITÁRIOS (Isolados)
    // =========================================================================================

    // Classe auxiliar que troca tudo por Mocks (não acessa banco)
    class getLanchesUnitario extends getLanches {
        @Override protected ValidadorCookie getValidadorCookie() { return validadorMock; }
        @Override protected DaoLanche getDaoLanche() { return daoLancheMock; }
        @Override protected Gson getGson() { return new Gson(); }
    }

    @Test
    public void UNITARIO_testProcessRequest_FuncionarioNaoAutorizado_RetornaErro() throws Exception {
        // Cenário: Validador nega acesso
        when(validadorMock.validarFuncionario(any())).thenReturn(false);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "invalido")});

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // Execução
        getLanches servlet = new getLanchesUnitario();
        servlet.processRequest(request, response);

        // Validação
        assertTrue(sw.toString().trim().contains("erro"), "Deveria retornar erro de permissão");
        verify(daoLancheMock, never()).listarTodos();
    }

    @Test
    public void UNITARIO_testProcessRequest_FuncionarioAutorizado_RetornaJson() throws Exception {
        // Cenário: Validador permite e DAO retorna lista
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "valido")});

        List<Lanche> listaFake = new ArrayList<>();
        Lanche lanche = new Lanche();
        lanche.setNome("X-Tudo");
        lanche.setValor_venda(25.50);
        listaFake.add(lanche);
        
        when(daoLancheMock.listarTodos()).thenReturn(listaFake);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // Execução
        getLanches servlet = new getLanchesUnitario();
        servlet.processRequest(request, response);

        // Validação
        String saida = sw.toString().trim();
        assertTrue(saida.contains("X-Tudo"), "O JSON deveria conter o nome do lanche");
        verify(daoLancheMock, times(1)).listarTodos();
    }

    // --- TESTES EXTRAS PARA AUMENTAR A COBERTURA (>80%) ---

    @Test
    public void UNITARIO_testProcessRequest_CookiesNulos_NaoQuebra() throws Exception {
        // Testar o if(cookies != null)
        when(request.getCookies()).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        getLanches servlet = new getLanchesUnitario();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().trim().contains("erro"), "Sem cookies deve cair no erro");
    }

    @Test
    public void UNITARIO_testProcessRequest_DaoRetornaListaNula_NaoGeraJson() throws Exception {
        // Testar o if(lanches != null)
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "valido")});
        when(daoLancheMock.listarTodos()).thenReturn(null); // Retorno nulo do banco

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        getLanches servlet = new getLanchesUnitario();
        servlet.processRequest(request, response);

        String saida = sw.toString().trim();
        assertTrue(saida.isEmpty(), "Se a lista for nula, não deve gerar JSON");
    }

    @Test
    public void UNITARIO_testProcessRequest_SimularErroGeral_EntraNoCatch() throws Exception {
        // Testar o catch(Exception)
        when(request.getCookies()).thenThrow(new NullPointerException("Erro forçado"));

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        getLanches servlet = new getLanchesUnitario();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().trim().contains("erro"), "Deve tratar a exceção e retornar erro padrão");
    }

    @Test
    public void UNITARIO_testProcessRequest_ResponseNulo_NaoQuebra() throws Exception {
        // Testar verificação defensiva if(response != null)
        getLanches servlet = new getLanchesUnitario();
        
        try {
            servlet.processRequest(request, null); // Passa null
        } catch (Exception e) {
            throw new RuntimeException("Código não tratou response nulo");
        }
    }

    // =========================================================================================
    //                                  PARTE 3: TESTES DE INTEGRAÇÃO (Banco Real)
    // =========================================================================================

    // Classe auxiliar para integração: Usa DAO Real mas Mocka o Validador
    class getLanchesIntegracao extends getLanches {
        ValidadorCookie v;
        public getLanchesIntegracao(ValidadorCookie v) { this.v = v; }
        @Override protected ValidadorCookie getValidadorCookie() { return v; }
        @Override protected Gson getGson() { return new Gson(); }
        // O getDaoLanche() não foi sobrescrito, então usa o original (conecta no banco)
    }

    @Test
    public void INTEGRACAO_testListarLanchesDoBancoReal() throws Exception {
        // Configura mocks apenas da parte web
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("sessao", "valida")});

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // Intercepta a conexão para forçar LOCALHOST em vez de 'db'
        try (MockedConstruction<DaoUtil> mockedDaoUtil = Mockito.mockConstruction(DaoUtil.class,
                (mock, context) -> {
                    when(mock.conecta()).thenAnswer(invocation -> {
                        return DriverManager.getConnection(
                            "jdbc:postgresql://localhost:5432/lanchonete", 
                            "postgres", "123456"); // Credenciais do Docker
                    });
                })) {

            // SETUP: Insere dados no banco real para garantir que o teste funcione
            try (java.sql.Connection conn = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/lanchonete", "postgres", "123456")) {
                
                // Limpa registro anterior se existir
                try {
                    conn.prepareStatement("DELETE FROM tb_lanches WHERE nm_lanche = 'X-Integracao'").execute();
                } catch (Exception ignore) {}

                // Insere lanche de teste (usando nomes corretos das colunas)
                java.sql.PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO tb_lanches (nm_lanche, descricao, valor_venda, fg_ativo) VALUES (?, ?, ?, ?)");
                
                stmt.setString(1, "X-Integracao");
                stmt.setString(2, "Lanche de teste automatizado");
                stmt.setDouble(3, 99.90);
                stmt.setInt(4, 1); // 1 = Ativo
                
                stmt.execute();
            }

            // Executa a servlet (vai no banco de verdade buscar o X-Integracao)
            getLanches servlet = new getLanchesIntegracao(validadorMock);
            servlet.processRequest(request, response);
        }

        // Validação
        String jsonSaida = sw.toString();
        
        // Se o JSON contiver o nome do lanche que inserimos no banco, sucesso!
        assertTrue(jsonSaida.contains("X-Integracao"), 
            "Falha na integração: O JSON retornado não contem os dados do banco. Saída: " + jsonSaida);
        
        System.out.println("Teste de Integração (Listar Lanches) passou com sucesso!");
    }
}