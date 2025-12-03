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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class Comprar extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            processRequest(request, response);
        } catch (IOException e) {
            handleException(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            processRequest(request, response);
        } catch (IOException e) {
            handleException(response, e);
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (!validarCookies(request)) {
            responderErro(response);
            return;
        }

        JSONObject dados = lerDadosJson(request);

        Cliente cliente = getCliente(dados);

        if (cliente == null) {
            responderErro(response);
            return;
        }

        criarEPersistirPedido(dados, cliente);

        responderOK(response);
    }

    // ---------------- MÉTODOS AUXILIARES ----------------

    private boolean validarCookies(HttpServletRequest request) {
        try {
            Cookie[] cookies = request.getCookies();
            if (cookies == null) return false;
            ValidadorCookie validar = getValidadorCookie();
            return validar.validar(cookies);
        } catch (Exception e) {
            return false; // qualquer exceção aqui é tratada como cookie inválido
        }
    }

    private JSONObject lerDadosJson(HttpServletRequest request) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
            String linha = br.readLine();                   // armazenando o valor lido
            String json = (linha != null) ? linha : "{}";  // fallback caso seja nulo
            return new JSONObject(json);
        }
    }

    private Cliente getCliente(JSONObject dados) {
        DaoCliente clienteDao = getDaoCliente();
        return clienteDao.pesquisaPorID(String.valueOf(dados.optInt("id")));
    }

    private void criarEPersistirPedido(JSONObject dados, Cliente cliente) {
        ItensPedido itens = processarItens(dados);

        Pedido pedido = new Pedido();
        pedido.setData_pedido(Instant.now().toString());
        pedido.setCliente(cliente);
        pedido.setValor_total(itens.valorTotal);

        DaoPedido pedidoDao = getDaoPedido();
        pedidoDao.salvar(pedido);

        Pedido pedidoSalvo = pedidoDao.pesquisaPorData(pedido);
        if (pedidoSalvo != null) pedido = pedidoSalvo;

        vincularItens(pedido, itens);
    }

    private void vincularItens(Pedido pedido, ItensPedido itens) {
        DaoPedido pedidoDao = getDaoPedido();
        for (Lanche l : itens.lanches) pedidoDao.vincularLanche(pedido, l);
        for (Bebida b : itens.bebidas) pedidoDao.vincularBebida(pedido, b);
    }

    private ItensPedido processarItens(JSONObject dados) {
        Iterator<String> keys = dados.keys();
        double valorTotal = 0.0;
        List<Lanche> lanches = new ArrayList<>();
        List<Bebida> bebidas = new ArrayList<>();

        while (keys.hasNext()) {
            String nome = keys.next();
            if (!"id".equals(nome) && dados.optJSONArray(nome) != null) {
                valorTotal += processarItem(dados, nome, lanches, bebidas);
            }
        }
        return new ItensPedido(valorTotal, lanches, bebidas);
    }

    private double processarItem(JSONObject dados, String nome, List<Lanche> lanches, List<Bebida> bebidas) {
        double total = 0.0;
        String tipo = dados.getJSONArray(nome).getString(1);
        int quantidade = dados.getJSONArray(nome).getInt(2);

        if ("lanche".equals(tipo)) {
            DaoLanche lancheDao = getDaoLanche();
            Lanche lanche = lancheDao.pesquisaPorNome(nome);
            if (lanche != null) {
                lanche.setQuantidade(quantidade);
                total += lanche.getValor_venda() * quantidade;
                lanches.add(lanche);
            }
        } else if ("bebida".equals(tipo)) {
            DaoBebida bebidaDao = getDaoBebida();
            Bebida bebida = bebidaDao.pesquisaPorNome(nome);
            if (bebida != null) {
                bebida.setQuantidade(quantidade);
                total += bebida.getValor_venda() * quantidade;
                bebidas.add(bebida);
            }
        }
        return total;
    }

    private void responderErro(HttpServletResponse response) {
        try (PrintWriter out = response.getWriter()) {
            out.println("erro");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void responderOK(HttpServletResponse response) {
        try (PrintWriter out = response.getWriter()) {
            out.println("Pedido Salvo com Sucesso!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleException(HttpServletResponse response, IOException e) {
        e.printStackTrace();
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // ---------------- MÉTODOS PARA MOCK ----------------

    protected ValidadorCookie getValidadorCookie() { return new ValidadorCookie(); }
    protected DaoCliente getDaoCliente() { return new DaoCliente(); }
    protected DaoLanche getDaoLanche() { return new DaoLanche(); }
    protected DaoBebida getDaoBebida() { return new DaoBebida(); }
    protected DaoPedido getDaoPedido() { return new DaoPedido(); }

    @Override
    public String getServletInfo() { return "Short description"; }

    // ---------------- CLASSE AUXILIAR PARA ITENS ----------------

    private static class ItensPedido {
        double valorTotal;
        List<Lanche> lanches;
        List<Bebida> bebidas;

        public ItensPedido(double valorTotal, List<Lanche> lanches, List<Bebida> bebidas) {
            this.valorTotal = valorTotal;
            this.lanches = lanches;
            this.bebidas = bebidas;
        }
    }
}
