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

        String nomeIngrediente = "Pão Australiano Selenium";

        // 1. Acessa Home
        driver.get(BASE_URL + "/view/home/home.html");
        Thread.sleep(2000);

        // 2. Clicar em “Acessar Cardápio”, se existir
        try {
            WebElement botao = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'Acessar Cardápio')]")));
            botao.click();
        } catch (Exception e) {
            System.out.println("Botão de cardápio não necessário ou não encontrado.");
        }

        // 3. Lida com alert inicial (opcional)
        try {
            Alert alerta = wait.until(ExpectedConditions.alertIsPresent());
            alerta.accept();
        } catch (TimeoutException ignored) {
        }

        // 4. Ir para login de funcionário
        WebElement carrinho =
                wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Meu Carrinho")));
        carrinho.click();

        WebElement funcionarioLink =
                wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Funcionário?")));
        funcionarioLink.click();

        // 5. Efetuar login (admin/admin)
        WebElement campoUsuario =
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginInput")));
        WebElement campoSenha = driver.findElement(By.id("senhaInput"));
        WebElement botaoEntrar =
                driver.findElement(By.xpath("//button[contains(text(),'Entrar')]"));

        campoUsuario.sendKeys("admin");
        campoSenha.sendKeys("admin");
        botaoEntrar.click();

        // 6. Acessar Cadastro de Ingredientes
        WebElement botaoCadIngredientes =
                wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(text(),'Cadastrar Ingredientes')]")));
        botaoCadIngredientes.click();

        // 7. Preencher formulário
        WebElement campoNome =
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("nome")));
        campoNome.sendKeys(nomeIngrediente);

        // Select do tipo
        WebElement selectElement = driver.findElement(By.name("tipo"));
        Select selectTipo = new Select(selectElement);
        try {
            selectTipo.selectByIndex(1);
        } catch (Exception e) {
            selectElement.sendKeys(Keys.DOWN);
        }

        // Quantidade
        driver.findElement(By.name("quantidade")).sendKeys("50");

        // Valor Compra
        WebElement campoVlrCompra = driver.findElement(By.name("ValorCompra"));
        campoVlrCompra.clear();
        campoVlrCompra.sendKeys("1.50");

        // Valor Venda
        WebElement campoVlrVenda = driver.findElement(By.name("ValorVenda"));
        campoVlrVenda.clear();
        campoVlrVenda.sendKeys("3.00");

        // Descrição
        try {
            driver.findElement(By.id("textArea1")).sendKeys("Pão escuro delicioso");
        } catch (Exception e) {
            driver.findElement(By.name("TextArea1")).sendKeys("Pão escuro delicioso");
        }

        // 8. Salvar
        WebElement botaoSalvar = driver.findElement(By.name("salvar"));
        botaoSalvar.click();

     // 9. ASSERT principal — verificar mensagem do alert
        Alert alertaSucesso = wait.until(ExpectedConditions.alertIsPresent());
        String textoAlerta = alertaSucesso.getText().trim(); // "Ingrediente Salvo!"

        assertEquals("Ingrediente Salvo!", textoAlerta,
                "Mensagem de sucesso do cadastro não corresponde ao esperado.");

        // >>> AQUI ELE CLICA NO OK DO ALERTA <<<
        alertaSucesso.accept();

        // pequena pausa só pra garantir que a tela "descongela" depois do alert
        Thread.sleep(1000);

        // 10. Clicar no link ESTOQUE do menu superior
        WebElement botaoEstoque = wait.until(
                ExpectedConditions.elementToBeClickable(By.linkText("Estoque"))
        );
        botaoEstoque.click();

        // 11. Pausa pra você visualizar a tela de Estoque
        Thread.sleep(3000);
    
    }
}
