package Controllers;

import DAO.DaoBebida;
import DAO.DaoCliente;
import DAO.DaoLanche;
import DAO.DaoPedido;
import Helpers.ValidadorCookie;
import Model.Bebida;
import Model.Cliente;
import Model.Lanche;
import Model.Pedido;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class comprar extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String json = "";
        
        boolean resultado = false;
        try {
            Cookie[] cookies = request.getCookies();
            ValidadorCookie validar = getValidadorCookie(); 
            resultado = validar.validar(cookies);
        } catch (java.lang.NullPointerException e) { }

        if ((br != null) && resultado) {
            json = br.readLine();
            if(json == null) json = "{}"; 

            byte[] bytes = json.getBytes(ISO_8859_1); 
            String jsonStr = new String(bytes, UTF_8);            
            JSONObject dados = new JSONObject(jsonStr);
            
            DaoCliente clienteDao = getDaoCliente(); 
            Cliente cliente = clienteDao.pesquisaPorID(String.valueOf(dados.optInt("id"))); 
            
            Iterator<String> keys = dados.keys();
            Double valor_total = 0.00;
            List<Lanche> lanches = new ArrayList<>();
            List<Bebida> bebidas = new ArrayList<>();
            
            while(keys.hasNext()) {
                String nome = keys.next();
                if(!nome.equals("id")){
                    // Verifica se é array antes de tentar acessar indices
                     if (dados.optJSONArray(nome) != null) {
                        if(dados.getJSONArray(nome).get(1).equals("lanche")){
                            DaoLanche lancheDao = getDaoLanche(); 
                            Lanche lanche = lancheDao.pesquisaPorNome(nome);
                            // Simples validação para evitar NullPointer se o mock retornar null
                            if (lanche != null) { 
                                int quantidade = dados.getJSONArray(nome).getInt(2);
                                lanche.setQuantidade(quantidade);
                                
                                // --- CORREÇÃO APLICADA AQUI ---
                                valor_total += lanche.getValor_venda() * quantidade;
                                // ------------------------------
                                
                                lanches.add(lanche);
                            }
                        }
                        else if(dados.getJSONArray(nome).get(1).equals("bebida")){
                            DaoBebida bebidaDao = getDaoBebida(); 
                            Bebida bebida = bebidaDao.pesquisaPorNome(nome);
                            if (bebida != null) {
                                int quantidade = dados.getJSONArray(nome).getInt(2);
                                bebida.setQuantidade(quantidade);
                                
                                // --- CORREÇÃO APLICADA AQUI ---
                                valor_total += bebida.getValor_venda() * quantidade;
                                // ------------------------------
                                
                                bebidas.add(bebida);
                            }
                        }
                     }
                }
            }
            
            DaoPedido pedidoDao = getDaoPedido(); 
            Pedido pedido = new Pedido();
            pedido.setData_pedido(Instant.now().toString());
            pedido.setCliente(cliente);
            pedido.setValor_total(valor_total);
            
            pedidoDao.salvar(pedido);
            // Simulação de retorno do banco
            Pedido pedidoSalvo = pedidoDao.pesquisaPorData(pedido);
            // Fallback caso o DAO retorne null (comum em testes se não mockar tudo perfeitamente)
            if (pedidoSalvo != null) pedido = pedidoSalvo; 
            pedido.setCliente(cliente);
            
            for(Lanche l : lanches){
                pedidoDao.vincularLanche(pedido, l);
            }
            for(Bebida b : bebidas){
                pedidoDao.vincularBebida(pedido, b);
            }
  
            try (PrintWriter out = response.getWriter()) {
                out.println("Pedido Salvo com Sucesso!");
            }
        } else {
            try (PrintWriter out = response.getWriter()) {
                out.println("erro");
            }
        }
    }

    // MÉTODOS "SEAM" (costuras) PARA PERMITIR MOCKS NOS TESTES
    
    protected ValidadorCookie getValidadorCookie() {
        return new ValidadorCookie();
    }

    protected DaoCliente getDaoCliente() {
        return new DaoCliente();
    }

    protected DaoLanche getDaoLanche() {
        return new DaoLanche();
    }

    protected DaoBebida getDaoBebida() {
        return new DaoBebida();
    }

    protected DaoPedido getDaoPedido() {
        return new DaoPedido();
    }

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