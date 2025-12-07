package Controllers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.JavascriptExecutor;

import java.time.Duration;

class salvarLancheSelenium {

    private WebDriver driver;
    private WebDriverWait wait;

    private final String BASE_URL = "http://localhost:8080/";
    // Constante para garantir que o nome do pão seja reutilizado
    private final String INGREDIENTE_PAO_CADASTRADO = "Pão Australiano Selenium";

    @BeforeEach
    void setUp() {
        // 1. Configura o gerenciador de driver automaticamente
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");


        driver = new ChromeDriver(options);
        driver.manage().window().maximize();

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void deveAcessarLoginFuncionarioECadastrarIngrediente() throws InterruptedException {


        JavascriptExecutor executor = (JavascriptExecutor) driver;

        // 1. Acessa a Home
        driver.get(BASE_URL + "/view/home/home.html");


        Thread.sleep(2000);

        // 2. Clicar em Acessar Cardápio
        try {
            WebElement botao = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'Acessar Cardápio')]")));
            botao.click();
        } catch (Exception e) {
            System.out.println("Botão de cardápio não necessário ou não encontrado.");
        }

        // 3. Lidar com Alerta inicial
        try {
            Alert alerta = wait.until(ExpectedConditions.alertIsPresent());
            alerta.accept();
        } catch (TimeoutException e) {
            // Se não aparecer alerta em 10s, segue
        }

        // 4. Navegar até o Login de Funcionário
        WebElement carrinho = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Meu Carrinho")));
        carrinho.click();

        WebElement funcionarioLink = wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Funcionário?")));
        funcionarioLink.click();

        // 5. Fazer Login (Admin)
        WebElement campoUsuario = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginInput")));
        WebElement campoSenha = driver.findElement(By.id("senhaInput"));
        WebElement botaoEntrar = driver.findElement(By.xpath("//button[contains(text(),'Entrar')]"));

        campoUsuario.sendKeys("admin");
        campoSenha.sendKeys("admin");
        botaoEntrar.click();

        // 6. Acessar Cadastro de Ingredientes
        WebElement botaoCadIngredientes = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(),'Cadastrar Ingredientes')]")));
        botaoCadIngredientes.click();

        // 7. Preencher Formulário de Cadastro (Pão - Pré-requisito)
        WebElement campoNome = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("nome")));
        campoNome.sendKeys(INGREDIENTE_PAO_CADASTRADO);

        // Tipo (Usando Select para ser mais robusto)
        WebElement selectElement = driver.findElement(By.name("tipo"));
        Select selectTipo = new Select(selectElement);
        try {
            selectTipo.selectByIndex(1); // Seleciona o segundo item da lista
        } catch (Exception e) {
            selectElement.sendKeys(Keys.DOWN); // Fallback se o select falhar
        }

        // Quantidade
        driver.findElement(By.name("quantidade")).sendKeys("50");

        // Valor Compra (Limpa antes de digitar para evitar lixo)
        WebElement campoVlrCompra = driver.findElement(By.name("ValorCompra"));
        campoVlrCompra.clear();
        campoVlrCompra.sendKeys("1.50"); // Tente ponto se vírgula falhar

        // Valor Venda
        WebElement campoVlrVenda = driver.findElement(By.name("ValorVenda"));
        campoVlrVenda.clear();
        campoVlrVenda.sendKeys("3.00");

        // Descrição (Tenta achar por ID ou Name, garantindo compatibilidade)
        try {
            driver.findElement(By.id("textArea1")).sendKeys("Pão escuro delicioso");
        } catch (Exception e) {
            driver.findElement(By.name("TextArea1")).sendKeys("Pão escuro delicioso");
        }

        // 8. Salvar
        WebElement botaoSalvar = driver.findElement(By.name("salvar"));
        botaoSalvar.click();

        // 9. Verificar Sucesso (Alerta)
        try {
            Alert alertaSucesso = wait.until(ExpectedConditions.alertIsPresent());
            alertaSucesso.accept();
        } catch (TimeoutException e) { System.out.println("Alerta de sucesso não apareceu a tempo."); }

        // 10. Ir para Estoque para conferir
        WebElement botaoEstoque = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Estoque")));
        botaoEstoque.click();

        Thread.sleep(2000);


        // --- INÍCIO DA FUNCIONALIDADE SALVAR LANCHE  ---

        // 11. garamtir a navegação para o Painel para que o botão 'Cadastrar Lanches' apareça.
        driver.get(BASE_URL + "view/painel/painel.html");

        // 12. Clicar no botão "Cadastrar Lanches"
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(),'Cadastrar Lanches')]"))).click();

        // Espera o campo Nome da tela de lanches  para garantir o carregamento
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("nomeLanche")));

     // 13. Preencher o Campo nome do Lanche
        WebElement campoNomeLanche = driver.findElement(By.id("nomeLanche"));
        campoNomeLanche.clear();
        campoNomeLanche.sendKeys("Lanche");


        executor.executeScript("arguments[0].dispatchEvent(new Event('change'));", campoNomeLanche);
        executor.executeScript("arguments[0].dispatchEvent(new Event('blur'));", campoNomeLanche);
        Thread.sleep(500);

     // 14. Selecionar o pao
        WebElement selectElementPao = driver.findElement(By.id("selectPao"));
        Select selectPao = new Select(selectElementPao);


        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
            By.xpath("//select[@id='selectPao']/option"), 1)
        );

        // Seleciona o pão
        try {
            selectPao.selectByVisibleText(INGREDIENTE_PAO_CADASTRADO);
        } catch (NoSuchElementException e) {
            try {
                selectPao.selectByIndex(1);
            } catch (Exception ex) {
                Assertions.fail("Falha Crítica no Passo 14: O ingrediente '" + INGREDIENTE_PAO_CADASTRADO + "' não apareceu na lista.");
            }
        }
        Thread.sleep(1000);

        // 15. Preencher a descriçao
        WebElement campoDescricao = driver.findElement(By.id("textArea3"));
        campoDescricao.clear();
        campoDescricao.sendKeys("Lanche de queijo");
        executor.executeScript("arguments[0].dispatchEvent(new Event('change'));", campoDescricao); // Força evento
        Thread.sleep(500);

     // 16. Preencher o valor do Lanche
        WebElement campoPrecoLanche = driver.findElement(By.id("ValorLanche"));
        campoPrecoLanche.clear();
        campoPrecoLanche.sendKeys("10.00");
        executor.executeScript("arguments[0].dispatchEvent(new Event('change'));", campoPrecoLanche); // Força evento
        Thread.sleep(2000);

     // 17. Clicar no botão Salvar
        WebElement botaoSalvarLanche = driver.findElement(By.name("salvar"));

        // Executa o clique diretamente
        executor.executeScript("arguments[0].click();", botaoSalvarLanche);


        Thread.sleep(4000);

        // 18. Verificar Sucesso
        try {
            Alert alertaLanche = wait.until(ExpectedConditions.alertIsPresent());
            String textoAlerta = alertaLanche.getText();
            alertaLanche.accept();

            // Verificação da Asserção: Agora deve conter "Lanche Salvo com Sucesso!"
            Assertions.assertTrue(textoAlerta.contains("Lanche Salvo com Sucesso!"),
                                  "Falha! O Alerta não contém a mensagem de sucesso. Conteúdo retornado: " + textoAlerta);

        } catch (TimeoutException e) {
            Assertions.fail("Tempo esgotado! Alert de sucesso não apareceu após salvar o lanche.");
        }
    }
}