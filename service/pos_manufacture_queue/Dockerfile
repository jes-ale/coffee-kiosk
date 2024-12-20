FROM openjdk:8

WORKDIR /app

# Copiar el código fuente de la aplicación y el archivo .env al contenedor
COPY . .

# Copiar el keystore generado al contenedor
COPY keystore.jks build/keystore.jks

# Construir el ejecutable JVM
RUN ./gradlew installDist

# EXPOSE 8080
# EXPOSE 8081
# HTTPS
EXPOSE 8443

# Ejecutar la aplicación
CMD ./build/install/com.coffee_service.quadro.org.manufacture_service/bin/com.coffee_service.quadro.org.manufacture_service

