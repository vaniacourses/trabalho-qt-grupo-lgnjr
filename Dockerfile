# syntax=docker/dockerfile:1

################################################################################
# 1. Etapa de build: compila o projeto Java e gera o arquivo .war usando Maven
################################################################################

# Usa uma imagem com Java 8 e Maven para compilar o projeto
FROM eclipse-temurin:8-jdk-jammy AS build

# Define o diretório de trabalho dentro do container
WORKDIR /app

# Copia todos os arquivos do projeto para dentro do container
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
COPY web web

# Executa o Maven para compilar o projeto e gerar o arquivo .war
# O parâmetro -DskipTests faz com que os testes não sejam executados durante o build
RUN chmod +x mvnw
RUN ./mvnw package -DskipTests

################################################################################
# 2. Etapa de runtime: roda o arquivo .war usando o servidor Tomcat
################################################################################

# Usa a imagem oficial do Tomcat 8 como base para rodar a aplicação
FROM tomcat:8-jdk8-openjdk

WORKDIR /app

# Remove o webapp padrão do Tomcat (ROOT) para evitar conflitos
RUN rm -rf /usr/local/tomcat/webapps/ROOT

# Copia o .war gerado na etapa de build
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

# Expõe a porta 8080 do container (porta padrão do Tomcat)
EXPOSE 8080