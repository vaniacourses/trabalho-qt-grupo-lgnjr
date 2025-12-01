package Controllers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditarIngredienteSeleniumTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private final String BASE_URL = "http://localhost:8080/";

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
        if (driver != null) driver.quit();
    }

    @Test
    void deveCadastrarEEditarIngrediente() throws InterruptedException {

        // 1. Acessa a Home
        driver.get(BASE_URL + "/view/home/home.html");
        Thread.sleep(1500);

        // 2. Clica em Acessar Cardápio
        try {
            WebElement botao = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'Acessar Cardápio')]")));
            botao.click();
        } catch (Exception ignored) { }

        // 3. Lida com alerta se existir
        try {
            Alert alerta = wait.until(ExpectedConditions.alertIsPresent());
            alerta.accept();
        } catch (Exception ignored) {}

        // 4. Vai para área do funcionário
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Meu Carrinho"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Funcionário?"))).click();

        // 5. Login admin
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginInput"))).sendKeys("admin");
        driver.findElement(By.id("senhaInput")).sendKeys("admin");
        driver.findElement(By.xpath("//button[contains(text(),'Entrar')]")).click();

        // 6. Vai para Cadastrar Ingredientes
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(),'Cadastrar Ingredientes')]"))).click();

        // 7. Preenche formulário de cadastro de ingrediente
        WebElement campoNome = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("nome")));
        campoNome.sendKeys("Pão Australiano Selenium");

        // Tipo
        WebElement selectElement = driver.findElement(By.name("tipo"));
        new Select(selectElement).selectByIndex(1);

        driver.findElement(By.name("quantidade")).sendKeys("50");

        WebElement compra = driver.findElement(By.name("ValorCompra"));
        compra.clear();
        compra.sendKeys("1.50");

        WebElement venda = driver.findElement(By.name("ValorVenda"));
        venda.clear();
        venda.sendKeys("3.00");

        try {
            driver.findElement(By.id("textArea1")).sendKeys("Pão escuro delicioso");
        } catch (Exception e) {
            driver.findElement(By.name("TextArea1")).sendKeys("Pão escuro delicioso");
        }

        // 8. Salvar
        driver.findElement(By.name("salvar")).click();

        // Alerta de sucesso
        try {
            Alert alertaSucesso = wait.until(ExpectedConditions.alertIsPresent());
            alertaSucesso.accept();
        } catch (Exception ignored) { }

        // 9. Ir para Estoque
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Estoque"))).click();
        Thread.sleep(1500);

        // 10. Esperar tabela de ingredientes carregar
        WebElement tabela = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("tabelaIngredientes")));
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.cssSelector("#tabelaIngredientes tr"),
                1
        ));


        // 11. Encontrar a linha do ingrediente pelo texto
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("tabelaIngredientes"),
                "Pão Australiano Selenium"
        ));

        WebElement linhaIngrediente = tabela.findElements(By.tagName("tr"))
            .stream()
            .filter(tr -> tr.getText().contains("Pão Australiano Selenium"))
            .findFirst()
            .get();
            
        // 12. Clicar exatamente na célula do NOME (2ª coluna)
        WebElement celulaNome = linhaIngrediente.findElements(By.tagName("td")).get(1);
        celulaNome.click();

        // 13. Aguarda formulário aparecer
        WebElement nomeEdit = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ingredientesNome")));
        WebElement descEdit = driver.findElement(By.id("ingredientesDescricao"));
        WebElement qtdEdit = driver.findElement(By.id("ingredientesQuantidade"));
        WebElement compraEdit = driver.findElement(By.id("ingredientesPrecoCompra"));
        WebElement vendaEdit = driver.findElement(By.id("ingredientesPrecoVenda"));

        // 14. Alterar valores
        nomeEdit.clear();
        nomeEdit.sendKeys("Pão Italiano Selenium");

        descEdit.clear();
        descEdit.sendKeys("Pão claro crocante");

        qtdEdit.clear();
        qtdEdit.sendKeys("60");

        compraEdit.clear();
        compraEdit.sendKeys("2.00");

        vendaEdit.clear();
        vendaEdit.sendKeys("4.50");

        // 15. Clicar no botão ALTERAR
        WebElement btnAlterar = driver.findElement(
                By.xpath("//input[@type='button' and @value='Alterar' and contains(@onclick,'alterarIngrediente')]")
        );

        btnAlterar.click();

        // 16. Aceitar alerta de sucesso
        Alert alertaAlterar = wait.until(ExpectedConditions.alertIsPresent());
        alertaAlterar.accept();

        // 17. Página recarrega → esperar tabela novamente
        WebElement tabelaAtualizada = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("tabelaIngredientes")));
        Thread.sleep(1500);

        // 18. Validar ingrediente atualizado 
        WebElement linhaAtualizada = tabelaAtualizada.findElements(By.tagName("tr"))
                .stream()
                .filter(tr -> tr.getText().contains("Pão Italiano Selenium"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Ingrediente alterado não aparece na tabela"));
        String textoLinha = linhaAtualizada.getText();

        assertTrue(textoLinha.contains("Pão Italiano Selenium"));
        assertTrue(textoLinha.contains("Pão claro crocante"));
        assertTrue(textoLinha.contains("60"));
        assertTrue(textoLinha.contains("2.00"));
        assertTrue(textoLinha.contains("4.50"));
    }
}