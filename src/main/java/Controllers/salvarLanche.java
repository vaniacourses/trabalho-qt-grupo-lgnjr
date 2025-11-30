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
public class salvarLanche extends HttpServlet {
   protected void processRequest(HttpServletRequest request, HttpServletResponse response)
           throws ServletException, IOException {
       response.setContentType("application/json");
       response.setCharacterEncoding("UTF-8");
      
       // Proteção para evitar NullPointer se o stream estiver vazio no teste
       BufferedReader br = null;
       if(request.getInputStream() != null) {
           br = new BufferedReader(new InputStreamReader(request.getInputStream()));
       }
      
       String json = "";
      
       ////////Validar Cookie
       boolean resultado = false;
      
       try{
           Cookie[] cookies = request.getCookies();
           // AQUI MUDOU: Usa o método protegido
           ValidadorCookie validar = getValidadorCookie();
          
           if (cookies != null) {
               resultado = validar.validarFuncionario(cookies);
           }
       } catch(java.lang.NullPointerException e){
           resultado = false;
       }
       //////////////
      
       if (br != null) {
          
           if (resultado) {
              
               json = br.readLine();
              
               // Validação defensiva para evitar erro no trim()
               if (json != null && !json.trim().isEmpty()) {
                  
                   byte[] bytes = json.getBytes(ISO_8859_1);
                   String jsonStr = new String(bytes, UTF_8);           
                   JSONObject dados = new JSONObject(jsonStr);
                  
                   if (dados != null) {
                      
                       // Lógica de Ingredientes
                       JSONObject ingredientes = null;
                       if(dados.has("ingredientes")) {
                           ingredientes = dados.getJSONObject("ingredientes");
                       }
                  
                       Lanche lanche = new Lanche();
                       // Uso de optString para evitar crash se faltar campo
                       lanche.setNome(dados.optString("nome"));
                       lanche.setDescricao(dados.optString("descricao"));
                       lanche.setValor_venda(dados.optDouble("ValorVenda"));
                      
                       // AQUI MUDOU: Usa métodos protegidos
                       DaoLanche lancheDao = getDaoLanche();
                       DaoIngrediente ingredienteDao = getDaoIngrediente();
                      
                       lancheDao.salvar(lanche);
                      
                       Lanche lancheComID = lancheDao.pesquisaPorNome(lanche);
                      
                       if (lancheComID != null && ingredientes != null) {
                          
                           Iterator<String> keys = ingredientes.keys();
                          
                           while(keys.hasNext()) {
                              
                               String key = keys.next();
                              
                               if (key != null) {
                                   Ingrediente ingredienteLanche = new Ingrediente();
                                   ingredienteLanche.setNome(key);
                                   Ingrediente ingredienteComID = ingredienteDao.pesquisaPorNome(ingredienteLanche);
                                  
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
                            enviarErro(response);
                       }
                   } else {
                       enviarErro(response);
                   }
               } else {
                   enviarErro(response);
               }
           } else {
               enviarErro(response);
           }
       } else {
           enviarErro(response);
       }
   }
   private void enviarErro(HttpServletResponse response) throws IOException {
       try (PrintWriter out = response.getWriter()) {
           out.println("erro");
       }
   }
   // ==========================================================================
   // MÉTODOS "SEAM" (Costuras) PARA O TESTE
   // ==========================================================================
   protected ValidadorCookie getValidadorCookie() { return new ValidadorCookie(); }
   protected DaoLanche getDaoLanche() { return new DaoLanche(); }
   protected DaoIngrediente getDaoIngrediente() { return new DaoIngrediente(); }
   // ... (Métodos doGet, doPost e getServletInfo continuam iguais) ...
   @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException { processRequest(req, resp); }
   @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException { processRequest(req, resp); }
   @Override public String getServletInfo() { return "Short description"; }
}
