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
import org.json.JSONException; // Adicionado para tratar erro de JSON
import org.json.JSONObject;

public class alterarIngrediente extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Proteção contra NullPointer no getInputStream (Melhoria para testes)
        BufferedReader br = null;
        if (request.getInputStream() != null) {
            br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        }
        
        String json = "";
        boolean resultado = false;
        
      
        try{
            Cookie[] cookies = request.getCookies();
            ValidadorCookie validar = getValidadorCookie(); // <--- AQUI
            
            // Proteção extra caso o método retorne null no teste
            if (validar != null) {
                resultado = validar.validarFuncionario(cookies);
            }
        } catch(java.lang.NullPointerException e){}
        
        if ((br != null) && resultado) {
            try {
                json = br.readLine();
                
                // Validação se JSON vier vazio
                if (json == null || json.trim().isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    try (PrintWriter out = response.getWriter()) { out.println("Dados ausentes"); }
                    return;
                }

                byte[] bytes = json.getBytes(ISO_8859_1); 
                String jsonStr = new String(bytes, UTF_8);            
                JSONObject dados = new JSONObject(jsonStr);
                
                Ingrediente ingrediente = new Ingrediente();
                // O JSONObject lança JSONException ou NumberFormatException se falhar a conversão
                ingrediente.setId_ingrediente(dados.getInt("id"));
                ingrediente.setNome(dados.getString("nome"));
                ingrediente.setDescricao(dados.getString("descricao"));
                ingrediente.setQuantidade(dados.getInt("quantidade"));
                ingrediente.setValor_compra(dados.getDouble("ValorCompra"));
                ingrediente.setValor_venda(dados.getDouble("ValorVenda"));
                ingrediente.setTipo(dados.getString("tipo"));
                ingrediente.setFg_ativo(1);
                
             
                DaoIngrediente ingredienteDAO = getDaoIngrediente(); //
                ingredienteDAO.alterar(ingrediente);
                
                try (PrintWriter out = response.getWriter()) {
                    out.println("Ingrediente Alterado!");
                }
            } catch (JSONException | NumberFormatException e) {
                // Captura erros de conversão (ex: ID com letras)
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter out = response.getWriter()) {
                    out.println("Erro no formato: " + e.getMessage());
                }
            }
        } else {
            // Status de erro para facilitar a validação do teste
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter out = response.getWriter()) {
                out.println("Não autorizado ou erro");
            }
        }
    }

  

    
    protected ValidadorCookie getValidadorCookie() {
        return new ValidadorCookie();
    }

    protected DaoIngrediente getDaoIngrediente() {
        return new DaoIngrediente();
    }

    // Métodos padrão do HttpServlet
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}