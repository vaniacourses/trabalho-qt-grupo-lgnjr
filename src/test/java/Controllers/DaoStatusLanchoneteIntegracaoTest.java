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

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/lanchonete";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "123456";

    static {
        try (MockedConstruction<DaoUtil> mockedDaoUtil = Mockito.mockConstruction(DaoUtil.class,
                (mock, context) -> {
                    Mockito.when(mock.conecta()).thenAnswer(invocation -> {
                        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                    });
                })) {
            
            Connection mockedConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            connection = mockedConnection;
            dao = new DaoStatusLanchonete(); 
            limparTabela();

        } catch (Exception e) {
            throw new RuntimeException("Falha ao iniciar mock de conexão no static block.", e);
        }
    }

    @BeforeAll
    static void setup() {
    }
    
    
    private static void limparTabela() {
        try {
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