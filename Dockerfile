# ETAPA 1: Construcción (Build)
# Usamos una imagen de Maven con JDK 21 para compilar el código
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos el archivo de configuración de Maven y las dependencias primero (para cachear)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiamos el código fuente y generamos el archivo .jar
COPY src ./src
RUN mvn clean package -DskipTests

# ETAPA 2: Ejecución (Runtime)
# Usamos una imagen ligera de JRE para correr la aplicación
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copiamos el .jar generado en la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Exponemos el puerto 8080
EXPOSE 8080

# Límites de memoria de la JVM. Sin ellos, la JVM dimensiona el heap según la RAM que ve el
# contenedor (en Railway puede ser de varios GB) y Railway cobra por la RAM usada.
# - Heap máximo de 256 MB y GC Serial (el más liviano para una sola instancia).
# - Pilas de hilo de 512 KB y metaspace/code cache acotados.
# - Si se agota la memoria, el proceso termina y Railway lo reinicia (en vez de quedar degradado).
# Se puede sobrescribir definiendo JAVA_TOOL_OPTIONS en las variables de Railway.
ENV JAVA_TOOL_OPTIONS="-Xmx256m -Xss512k -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=64m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

# Comando para arrancar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
