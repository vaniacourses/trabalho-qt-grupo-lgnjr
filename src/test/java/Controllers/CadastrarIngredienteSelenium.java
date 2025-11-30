package Controllers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;

class CadastrarIngredienteSelenium {

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
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
    void deveAcessarLoginFuncionario() throws InterruptedException {
        driver.get("http://localhost:8080/view/home/home.html");
        Thread.sleep(10000);
        
        // 1. clicar em Acessar Cardápio
        WebElement botao = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(text(),'Acessar Cardápio')]")
                )
        );
        botao.click();

        // 2. lidar com alerta
        Alert alerta = wait.until(ExpectedConditions.alertIsPresent());
        alerta.accept();

        // 3. clicar em Meu Carrinho
        WebElement carrinho = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.linkText("Meu Carrinho")
                )
        );
        carrinho.click();

        // 4. clicar em “Funcionário? Login Administrativo.”
        WebElement funcionario = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.linkText("Funcionário? Login Administrativo.")
                )
        );
        funcionario.click();

        Thread.sleep(3000);
    
    
    
 // 5.Login/Senha
    
 WebElement campoUsuario = wait.until(
         ExpectedConditions.visibilityOfElementLocated(By.id("loginInput"))
 );

 // campo da senha
 WebElement campoSenha = driver.findElement(By.id("senhaInput"));

 // botão ENTRAR -> <button ...>Entrar</button>
 WebElement botaoEntrar = wait.until(
         ExpectedConditions.elementToBeClickable(
                 By.xpath("//button[text()='Entrar']")
         )
 );
 
 
 // preencher admin / admin
 campoUsuario.sendKeys("admin");
 campoSenha.sendKeys("admin");

 // clicar em Entrar
 botaoEntrar.click();

 // só para visualizar
 Thread.sleep(5000);
 
 
 

//6. Cadastrar Ingrediente


//6.1 — Clicar no botão "Cadastrar Ingredientes"
WebElement botaoCadIngredientes = wait.until(
      ExpectedConditions.elementToBeClickable(
              By.xpath("//button[contains(text(),'Cadastrar Ingredientes')]")
      )
);
botaoCadIngredientes.click();

//Pausa para visualizar a tela de cadastro
Thread.sleep(3000);



//6.2 — Preencher Formulário de Cadastro de Ingrediente


//Campo: Produto (name="nome")
WebElement campoNome = wait.until(
      ExpectedConditions.visibilityOfElementLocated(
              By.name("nome")
      )
);
campoNome.sendKeys("Pão Australiano ");

//Campo: Tipo (select name="tipo")
WebElement campoTipo = driver.findElement(By.name("tipo"));
campoTipo.click();                          // abrir o select
campoTipo.sendKeys(Keys.ARROW_DOWN);        // escolher primeira opção
campoTipo.sendKeys(Keys.ENTER);             // confirmar

//Campo: Quantidade (name="quantidade")
WebElement campoQuantidade = driver.findElement(By.name("quantidade"));
campoQuantidade.sendKeys("20");


//Campo: Valor de Compra (name="ValorCompra")
WebElement campoValorCompra = driver.findElement(By.name("ValorCompra"));
campoValorCompra.clear();
campoValorCompra.sendKeys("1,50");

//Campo: Valor de Venda (name="ValorVenda")
WebElement campoValorVenda = driver.findElement(By.name("ValorVenda"));
campoValorVenda.clear();
campoValorVenda.sendKeys("3,00");




//Campo: Descrição (name="TextArea1")
WebElement campoDescricao = wait.until(
	    ExpectedConditions.visibilityOfElementLocated(By.id("textArea1"))
	);
	campoDescricao.sendKeys("Um pão Australiano");
	
	
	//Campo: Botão Salvar")
	WebElement botaoSalvar = wait.until(
	        ExpectedConditions.elementToBeClickable(By.name("salvar"))
	);
	botaoSalvar.click();

	

//Pausa para visualizar o resultado
Thread.sleep(3000);

//tratar o ALERT "Ingrediente Salvo!"
Alert alertaSucesso = wait.until(ExpectedConditions.alertIsPresent());


//clicar em OK
alertaSucesso.accept();

//7 — ir para a página ESTOQUE
WebElement botaoEstoque = wait.until(
     ExpectedConditions.elementToBeClickable(By.linkText("Estoque"))
);
botaoEstoque.click();
//Pausa para visualizar o resultado
Thread.sleep(3000);//Pausa para visualizar o resultado



    }}
