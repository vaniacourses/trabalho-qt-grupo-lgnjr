# 🧪 Trabalho Final de Qualidade e Teste de Software — Projeto Lanchonete

Este repositório contém os artefatos de teste, código-fonte refatorado e documentação referentes à entrega final da disciplina de **Qualidade e Teste de Software**.

**Grupo:** LGNJR

---

## 👥 Equipe e Responsabilidades Individuais

Para atender aos requisitos da **Entrega 2**, cada membro ficou responsável pela refatoração, testes unitários (Mock), testes estruturais (>80% cobertura) e testes de mutação (>80% score) de uma classe com **alta complexidade ciclomática (CC > 10)**.

| Integrante | Classe de Teste | Tipo de teste | 
|-----------|---------------------------|--------------------|
| **Lylian** | `src/java/Controllers/comprar.java` | Unitário e integração | 
| **Lylian** | `src/test/java/Controllers/comprarTest.java ` | Sistema (Selenium) | 
| **Nayara** | `src/java/Controllers/salvarLanche.java` | Unitário e integração | 
| **Nayara** | `src/test/java/Controllers/salvarLancheSelenium.java` |  Sistema (Selenium) | 
| **Rodrigo** | `src/java/Controllers/alterarStatusLanchonete.java` | Unitário |
| **Rodrigo** | `src/test/java/Controllers/DaoStatusLanchoneteIntegracaoTest.java ` | Integração | 
| **Rodrigo** | `src/test/java/Controllers/EditarIngredienteSeleniumTest.java` | Sistema (Selenium) |
| **Geiziane** | `src/java/Controllers/alterarIngrediente.java` | Unitário |
| **Geiziane** | `src/test/java/Controllers/CadastrarIngredienteSelenium.java` | Sistema (Selenium) |
| **João** | `src/java/Controllers/getIngredientesPorLanche.java` | Unitário | 

---

## 📍 Mapa dos Entregáveis

Itens e evidências relacionadas aos requisitos avaliados.

---

### 1. ✅ Testes Unitários e Cobertura Estrutural
- **Técnica:** Caixa Branca com isolamento de dependências usando Mockito.  
- **Meta atingida:** Todas as 5 classes obtiveram **>80% de cobertura de arestas (branches)**.  
- **Localização dos testes:** `src/test/java/Controllers/`

---

### 2. 🗄️ Testes de Integração (Banco de Dados Real)
- **Objetivo:** Validar persistência real no PostgreSQL.  
- **Abrange:** Driver JDBC, consultas SQL e constraints.  
- **Exemplo:** Método `INTEGRACAO_testeSalvarNoBancoReal` em  
  `src/test/java/Controllers/comprarTest.java`.

---

### 3. 🧬 Testes Baseados em Defeitos (Mutação — PITest)
- **Ferramenta:** PITest  
- **Meta:** classes com **>80% Mutation Score**.  
- **Evidências do escore de mutação:** *https://docs.google.com/document/d/1cWw8QR-QYhvLskCFNF5J_nuCdz6zysa6NuJYaQGKXzM/edit?usp=sharing* 

---

### 4. 🌐 Testes de Sistema (Selenium WebDriver)
- **Requisitos testados:** Cadastro e Edição de Ingredientes, cadastro de usuário, salvar lache.  
- **Fluxos completos:**  
    - Acessar Home → Entrar no Carrinho → Login Admin → Abrir Lanchonete → Logout → Voltar ao Carrinho → Abrir Cadastro → Preencher dados → Validar erro Telefone → Corrigir → Validar erro Número → Corrigir → Cadastrar com sucesso (cadastrarUsuarioSelenium)
    - Acessar Home → Acessar Cardápio → Login Admin → Cadastrar Ingrediente → Salvar → Ir ao Estoque → Selecionar Ingrediente → Editar → Validar atualização na tabela (EditarIngredienteSeleniumTest)
    - Home → Cardápio → Login Funcionário → Login Admin → Cadastrar Ingrediente → Preencher Formulário → Salvar → Validar Alerta → Abrir Estoque (CadastrarIngredienteSelenium)
    - Home → Cardápio → Meu Carrinho → Funcionário → Login Admin → Cadastrar Ingrediente → Preencher Formulário (Ingrediente) → Salvar → Validar Alerta → Estoque → Painel → Cadastrar Lanches → Preencher Nome → Selecionar Pão → Preencher Descrição → Preencher Preço → Salvar → Validar Alerta de Sucesso (salvarLancheSelenium)
  
- **Arquivos:**  
  `src/test/java/Controllers/cadastrarUsuarioSelenium.java`
  `src/test/java/Controllers/EditarIngredienteSeleniumTest.java`
  `src/test/java/Controllers/CadastrarIngredienteSelenium.java`
  `/src/test/java/Controllers/salvarLancheSelenium.java`
---

### 5. 🔍 Qualidade de Código e Inspeção (Sonar)
- **Evidências do Sonar:** *https://docs.google.com/document/d/1gNYRPsF9l-dFJsSyKcX5e35pl-TeTy2gmgu79uPhFP4/edit?usp=sharing*

---

### 6. 📚 Documentação
- **Plano de Teste:** *https://docs.google.com/document/d/1Obq0Ee-HCQhP71YuqeNLDTcpW3OKBTwuY8DSv9ThhRU/edit?tab=t.0*  
- **Relatório ISO 25010 (Atributos de Qualidade):** *https://docs.google.com/document/d/1U9sODUAbO4gxgEsVTI7mHnLLqilglDJC1jWOnhlMves/edit?usp=sharing*  

---

## 🚀 Guia de Execução

### **Pré-requisitos**
- Java 8+  
- Maven  
- Docker (PostgreSQL)  
- Google Chrome (para o Selenium)  

---

## 🚀 Como executar o projeto

### 1️⃣ Clone o repositório
```bash
git clone https://github.com/vaniacourses/trabalho-qt-grupo-lgnjr.git
cd trabalho-qt-grupo-lgnjr.git
```


### 2️⃣ Suba os containers com Docker Compose
```
docker-compose up --build -d
```

### 3️⃣ Acesse a aplicação
```
http://localhost:8080
```
### 4️⃣ Parar e remover tudo
```
docker-compose down
```
