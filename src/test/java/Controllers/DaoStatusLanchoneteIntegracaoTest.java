package Controllers;

import static org.junit.jupiter.api.Assertions.*;

import DAO.DaoStatusLanchonete;
import DAO.DaoUtil;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DaoStatusLanchoneteIntegracaoTest {

    private static Connection connection;
    private static DaoStatusLanchonete dao;

    // Credenciais para a conexão local via Mock
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/lanchonete";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "123456";

    // 1. Bloco estático para interceptar o DaoUtil (Antes de tudo!)
    static {
        try (MockedConstruction<DaoUtil> mockedDaoUtil = Mockito.mockConstruction(DaoUtil.class,
                (mock, context) -> {
                    // Quando o DaoStatusLanchonete chamar 'new DaoUtil().conecta()'
                    Mockito.when(mock.conecta()).thenAnswer(invocation -> {
                        // Força a conexão para localhost
                        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                    });
                })) {
            
            // 2. Tenta estabelecer a conexão inicial e instanciar o DAO dentro do bloco static
            // Isso garante que o mock está ativo quando o setup for chamado
            Connection mockedConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            connection = mockedConnection;
            dao = new DaoStatusLanchonete(); 
            limparTabela();

        } catch (Exception e) {
            // Se falhar, relança como RuntimeException (necessário para o static block)
            throw new RuntimeException("Falha ao iniciar mock de conexão no static block.", e);
        }
    }

    // O @BeforeAll original (agora ajustado para usar a conexão estática)
    @BeforeAll
    static void setup() {
        // Nada mais é necessário aqui, pois o bloco static {} já fez todo o trabalho.
        // Apenas garantir que o JUnit reconheça o método.
    }
    
    // O RESTO DOS MÉTODOS DE TESTE CONTINUAM IGUAIS:
    
    private static void limparTabela() {
        try {
            // Usa a conexão estática que foi estabelecida no bloco static {}
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM tb_status_lanchonete");
            stmt.execute();
            stmt.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void resetarTabela() {
        limparTabela();
    }

    @Test
    void testAlterarStatusInsereNoBanco() throws Exception {
        dao.alterarStatus("ABERTO");

        PreparedStatement stmt = connection.prepareStatement(
            "SELECT status FROM tb_status_lanchonete ORDER BY id_status DESC LIMIT 1"
        );

        ResultSet rs = stmt.executeQuery();
        assertTrue(rs.next());
        assertEquals("ABERTO", rs.getString("status"));
    }

    @Test
    void testGetStatusRetornaUltimoStatus() throws Exception {
        dao.alterarStatus("FECHADO");
        dao.alterarStatus("ABERTO");

        String status = dao.getStatus();

        assertEquals("ABERTO", status);
    }

    @Test
    void testGetStatusRetornaFechadoQuandoTabelaVazia() {
        String status = dao.getStatus();
        assertEquals("FECHADO", status);
    }
}