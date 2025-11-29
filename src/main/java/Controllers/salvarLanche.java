/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controllers;

import DAO.DaoIngrediente;
import DAO.DaoLanche;
import Helpers.ValidadorCookie;
import Model.Ingrediente;
import Model.Lanche;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Iterator;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 *
 * @author kener_000
 */
public class salvarLanche extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String json = "";
        
        ////////Validar Cookie
        boolean resultado = false;
        
        try{
            Cookie[] cookies = request.getCookies();
            ValidadorCookie validar = new ValidadorCookie();
            
            // Ponto de complexidade (+1 do catch implícito)
            if (cookies != null) { // Verificação redundante para aumentar complexidade (+1)
                resultado = validar.validarFuncionario(cookies);
            }
        } catch(java.lang.NullPointerException e){
            // (+1 pelo catch)
            resultado = false;
        }
        //////////////
        
        // Estratégia de aninhamento (Nesting) para aumentar a complexidade ciclomática.
        // Originalmente era: if ((br != null) && resultado)
        
        if (br != null) { // (+1)
            
            if (resultado) { // (+1)
                
                json = br.readLine();
                
                // Verificação defensiva extra (+1)
                if (json != null && !json.trim().isEmpty()) { // (&& conta como +1, if conta como +1)
                    
                    byte[] bytes = json.getBytes(ISO_8859_1); 
                    String jsonStr = new String(bytes, UTF_8);            
                    JSONObject dados = new JSONObject(jsonStr);
                    
                    // Verificação extra de existência do objeto JSON (+1)
                    if (dados != null) { 
                        
                        JSONObject ingredientes = dados.getJSONObject("ingredientes");
                   
                        Lanche lanche = new Lanche();
                        lanche.setNome(dados.getString("nome"));
                        lanche.setDescricao(dados.getString("descricao"));
                        lanche.setValor_venda(dados.getDouble("ValorVenda"));
                        
                        DaoLanche lancheDao = new DaoLanche();
                        DaoIngrediente ingredienteDao = new DaoIngrediente();
                        
                        lancheDao.salvar(lanche);
                        
                        Lanche lancheComID = lancheDao.pesquisaPorNome(lanche);
                        
                        // Verificação extra se o lanche foi salvo corretamente (+1)
                        if (lancheComID != null) { 
                            
                            Iterator<String> keys = ingredientes.keys();
                            
                            while(keys.hasNext()) { // (+1 pelo loop)
                                
                                String key = keys.next(); 
                                
                                // Verificação extra dentro do loop (+1)
                                if (key != null) {
                                    Ingrediente ingredienteLanche = new Ingrediente();
                                    ingredienteLanche.setNome(key);

                                    Ingrediente ingredienteComID = ingredienteDao.pesquisaPorNome(ingredienteLanche);
                                    
                                    // Verificação extra se o ingrediente existe no banco (+1)
                                    if (ingredienteComID != null) {
                                        ingredienteComID.setQuantidade(ingredientes.getInt(key));
                                        lancheDao.vincularIngrediente(lancheComID, ingredienteComID);
                                    }
                                }
                            }
                            
                            try (PrintWriter out = response.getWriter()) {
                                out.println("Lanche Salvo com Sucesso!");
                            }
                        } else {
                            // Caminho de falha caso o banco não retorne o ID (Defensivo)
                             enviarErro(response);
                        }
                    } else {
                        enviarErro(response);
                    }
                } else {
                    enviarErro(response);
                }
            } else {
                // Else do 'resultado' false
                enviarErro(response);
            }
        } else {
            // Else do 'br' null
            enviarErro(response);
        }
    }

    // Método auxiliar privado criado apenas para evitar repetição de código no tratamento de erro
    // e manter a lógica visualmente limpa, embora a complexidade estrutural tenha aumentado.
    private void enviarErro(HttpServletResponse response) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.println("erro");
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}