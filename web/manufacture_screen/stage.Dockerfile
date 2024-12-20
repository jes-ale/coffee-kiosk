# Etapa 1: Base para instalar dependencias
FROM oven/bun:1 AS base
WORKDIR /usr/src/app

# Copiar solo el archivo de definición de dependencias
COPY bun.lockb bun.lockb
COPY package.json package.json

# Instalar dependencias
RUN bun install

# Etapa 2: Construcción de la aplicación
FROM base AS builder

# Copiar el resto de la aplicación
COPY . .

# Construir la aplicación
RUN bun run build

# Etapa 3: Imagen final
FROM oven/bun:1 AS final

WORKDIR /usr/src/app

# Copiar solo los archivos necesarios de la etapa de construcción
COPY --from=builder /usr/src/app ./

# Cambiar al usuario 'bun'
USER bun
EXPOSE 3000
ENTRYPOINT [ "bun", ".output/server/index.mjs" ]
