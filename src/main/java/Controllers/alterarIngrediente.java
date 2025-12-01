
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import org.json.JSONException; // Importante para o catch
import org.json.JSONObject;

/**
 *
 * @author kener_000
 */
public class alterarIngrediente extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Proteção contra NullPointer no getInputStream (útil para testes)
        BufferedReader br = null;
        if (request.getInputStream() != null) {
            br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        }
        
        String json = "";
        
        ////////Validar Cookie
        boolean resultado = false;
        
        try{
            Cookie[] cookies = request.getCookies();
            // REFATORAÇÃO 1: Usar o método getter em vez de 'new' direto
            ValidadorCookie validar = getValidadorCookie(); 
            
            if (validar != null) {
                resultado = validar.validarFuncionario(cookies);
            }
        } catch(java.lang.NullPointerException e){}
        //////////////
        
        if ((br != null) && resultado) {
            try {
                json = br.readLine();
                
                // Validação extra caso o json venha nulo ou vazio
                if (json == null || json.trim().isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    try (PrintWriter out = response.getWriter()) { out.println("Dados ausentes"); }
                    return;
                }

                byte[] bytes = json.getBytes(ISO_8859_1); 
                String jsonStr = new String(bytes, UTF_8);            
                JSONObject dados = new JSONObject(jsonStr);
                
                Ingrediente ingrediente = new Ingrediente();
                // Conversões podem lançar JSONException se o formato estiver errado
                ingrediente.setId_ingrediente(dados.getInt("id"));
                ingrediente.setNome(dados.getString("nome"));
                ingrediente.setDescricao(dados.getString("descricao"));
                ingrediente.setQuantidade(dados.getInt("quantidade"));
                ingrediente.setValor_compra(dados.getDouble("ValorCompra"));
                ingrediente.setValor_venda(dados.getDouble("ValorVenda"));
                ingrediente.setTipo(dados.getString("tipo"));
                ingrediente.setFg_ativo(1);
                
                // REFATORAÇÃO 2: Usar o método getter em vez de 'new' direto
                DaoIngrediente ingredienteDAO = getDaoIngrediente();
                ingredienteDAO.alterar(ingrediente);
                
                try (PrintWriter out = response.getWriter()) {
                    out.println("Ingrediente Alterado!");
                }
            } catch (JSONException | NumberFormatException e) {
                // Captura erro de formatação (ex: letras no ID)
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter out = response.getWriter()) {
                    out.println("Erro no formato: " + e.getMessage());
                }
            }
        } else {
            // Retorna status de erro (400 ou 401) para facilitar validação no teste
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter out = response.getWriter()) {
                out.println("Não autorizado ou erro");
            }
        }
    }

    // ==========================================================================
    // MÉTODOS "SEAM" (COSTURAS) PARA PERMITIR TESTES COM MOCKS
    // ==========================================================================
    
    protected ValidadorCookie getValidadorCookie() {
        return new ValidadorCookie();
    }

    protected DaoIngrediente getDaoIngrediente() {
        return new DaoIngrediente();
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods.">
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
    // </editor-fold>
}