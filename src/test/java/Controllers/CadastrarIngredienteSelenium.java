package Controllers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CadastrarIngredienteSelenium {

    private WebDriver driver;
    private WebDriverWait wait;

    private final String BASE_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() {
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

        
        // DADOS DO TESTE
        
        String nomeIngrediente = "Pão Australiano Selenium";
        String descricao = "Pão escuro delicioso";
        String quantidade = "50";
        String valorCompra = "1.50";
        String valorVenda = "3.00";

       
        // INÍCIO DO FLUXO
       

        // 1. Acessa Home
        driver.get(BASE_URL + "/view/home/home.html");
        Thread.sleep(2000);

        // 2. Clicar em “Acessar Cardápio” (se existir no fluxo atual)
        WebElement botaoCardapio = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(),'Acessar Cardápio')]")));
        botaoCardapio.click();

        // 3. Lida com alert inicial (se houver)
        Alert alertaInicial = wait.until(ExpectedConditions.alertIsPresent());
        alertaInicial.accept();

        // 4. Ir para login de funcionário
        WebElement carrinho = wait.until(
                ExpectedConditions.elementToBeClickable(By.linkText("Meu Carrinho")));
        carrinho.click();

        WebElement funcionarioLink = wait.until(
                ExpectedConditions.elementToBeClickable(By.partialLinkText("Funcionário?")));
        funcionarioLink.click();

        // 5. Efetuar login
        WebElement campoUsuario = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("loginInput")));
        WebElement campoSenha = driver.findElement(By.id("senhaInput"));
        WebElement botaoEntrar = driver.findElement(By.xpath("//button[contains(text(),'Entrar')]"));

        campoUsuario.sendKeys("admin");
        campoSenha.sendKeys("admin");
        botaoEntrar.click();

        // 6. Acessar Cadastro de Ingredientes
        WebElement botaoCadIngredientes = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(),'Cadastrar Ingredientes')]")));
        botaoCadIngredientes.click();

        // 7. Preencher formulário
        WebElement campoNome = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.name("nome")));
        campoNome.sendKeys(nomeIngrediente);

        // Select do tipo
        Select selectTipo = new Select(driver.findElement(By.name("tipo")));
        selectTipo.selectByIndex(1);

        // Quantidade
        driver.findElement(By.name("quantidade")).sendKeys(quantidade);

        // Valor Compra
        WebElement campoVlrCompra = driver.findElement(By.name("ValorCompra"));
        campoVlrCompra.clear();
        campoVlrCompra.sendKeys(valorCompra);

        // Valor Venda
        WebElement campoVlrVenda = driver.findElement(By.name("ValorVenda"));
        campoVlrVenda.clear();
        campoVlrVenda.sendKeys(valorVenda);

        // Descrição
        driver.findElement(By.id("textArea1")).sendKeys(descricao);

        // 8. Salvar
        WebElement botaoSalvar = driver.findElement(By.name("salvar"));
        botaoSalvar.click();

        
        // ASSERT – mensagem do alert
      
        Alert alertaSucesso = wait.until(ExpectedConditions.alertIsPresent());
        String textoAlerta = alertaSucesso.getText().trim();

        assertEquals(
                "Ingrediente Salvo!",
                textoAlerta,
                "Mensagem de sucesso do cadastro não corresponde ao esperado."
        );

        alertaSucesso.accept();
    }
}