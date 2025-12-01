package Controllers;

import DAO.DaoIngrediente;
import Helpers.ValidadorCookie;
import Model.Ingrediente;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;

public class alterarIngrediente extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Proteção para evitar NullPointer
        BufferedReader br = null;
        if (request.getInputStream() != null) {
            br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        }
        
        String json = "";
        boolean resultado = false;
        
        // --- VALIDAÇÃO DE COOKIE ---
        try{
            Cookie[] cookies = request.getCookies();
            ValidadorCookie validar = getValidadorCookie(); // <--- HOOK
            
            if (cookies != null && validar != null) {
                 resultado = validar.validarFuncionario(cookies);
            }
        }catch(java.lang.NullPointerException e){}
        
        // -
        if ((br != null) && resultado) {
            try {
                json = br.readLine();
                
                // Vaidação de segurançal
                if (json == null || json.trim().isEmpty()) {
                    enviarErro(response, "Dados ausentes.");
                    return;
                }
                
                byte[] bytes = json.getBytes(ISO_8859_1);
                String jsonStr = new String(bytes, UTF_8);
                JSONObject dados = new JSONObject(jsonStr);
                
                // CRÍTICO: Conversão de tipos. Se 'quantidade' ou 'id' tiverem letras,
                // o org.json lança JSONException, capturada abaixo.
                Ingrediente ingrediente = new Ingrediente();
                ingrediente.setId_ingrediente(dados.getInt("id"));
                ingrediente.setNome(dados.getString("nome"));
                ingrediente.setDescricao(dados.getString("descricao"));
                ingrediente.setQuantidade(dados.getInt("quantidade"));
                ingrediente.setValor_compra(dados.getDouble("ValorCompra"));
                ingrediente.setValor_venda(dados.getDouble("ValorVenda"));
                ingrediente.setTipo(dados.getString("tipo"));
                ingrediente.setFg_ativo(1);
                
                DaoIngrediente ingredienteDAO = getDaoIngrediente(); // <--- HOOK
                ingredienteDAO.alterar(ingrediente);
                
                try (PrintWriter out = response.getWriter()) {
                    out.println("Ingrediente Alterado!");
                }
                
            } catch (JSONException e) {
                // Captura erro de parse ou erro de conversão numérica
                enviarErro(response, "Erro no formato dos dados: verifique ID, quantidade e valores.");
                return;
            }
        } else {
            enviarErro(response, "Não autorizado ou requisição inválida.");
        }
    }
    
    //  MÉTODOS AUXILIARES E HOOKS ---
    
    // Método para erro padronizado
    private void enviarErro(HttpServletResponse response, String mensagem) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        try (PrintWriter out = response.getWriter()) {
            out.println("erro: " + mensagem);
        }
    }

    // HOOKS para injeção de dependência
    protected ValidadorCookie getValidadorCookie() { return new ValidadorCookie(); }
    protected DaoIngrediente getDaoIngrediente() { return new DaoIngrediente(); }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { processRequest(request, response); }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { processRequest(request, response); }
    @Override public String getServletInfo() { return "Short description"; }
}