# Tonin-Api

API REST de frases motivacionales por estado de animo. Cada estado de animo puede tener musica e imagen asociada almacenada en Wasabi S3.

## Stack

- Java 21 + Spring Boot 3.4
- PostgreSQL 17
- Spring Security + JWT
- Wasabi S3 (almacenamiento de archivos)
- Swagger/OpenAPI
- Docker + Docker Compose

## Requisitos

- Java 21
- PostgreSQL 17
- Variables de entorno configuradas (ver `.env.example`)

## Levantar en local

1. Crear la base de datos:
```sql
CREATE DATABASE tonindb;
```

2. Configurar variables de entorno en IntelliJ (Run > Edit Configurations > Environment Variables):
```
WASABI_ACCESS_KEY=tu_access_key
WASABI_SECRET_KEY=tu_secret_key
JWT_SECRET=tu_clave_secreta_minimo_256_bits
```

3. Ejecutar `Main.java`

La API estara disponible en `http://localhost:8080`

## Levantar con Docker

```bash
cp .env.example .env
# Editar .env con tus valores reales
docker-compose up --build
```

## Endpoints

### Publicos

| Metodo | Ruta | Descripcion |
|--------|------|-------------|
| POST | `/api/auth/register` | Registrar usuario (1er usuario = ADMIN) |
| POST | `/api/auth/login` | Login, retorna JWT |
| GET | `/api/frases/random?animo={nombre}` | Frase aleatoria por estado de animo |
| GET | `/api/estados` | Listar estados de animo |

### Protegidos (ADMIN + JWT)

| Metodo | Ruta | Descripcion |
|--------|------|-------------|
| GET | `/api/frases` | Listar frases |
| GET | `/api/frases/{id}` | Obtener frase por ID |
| POST | `/api/frases` | Crear frase |
| PUT | `/api/frases/{id}` | Actualizar frase |
| DELETE | `/api/frases/{id}` | Eliminar frase |
| POST | `/api/estados` | Crear estado de animo |
| PUT | `/api/estados/{id}` | Actualizar estado de animo |
| DELETE | `/api/estados/{id}` | Eliminar estado de animo |
| POST | `/api/uploads/imagen` | Subir imagen (JPEG, PNG, GIF, WebP) |
| POST | `/api/uploads/musica` | Subir audio (MP3, WAV, OGG, MP4, WebM) |
| DELETE | `/api/uploads?url={url}` | Eliminar archivo de Wasabi |

### Documentacion

Swagger UI disponible en: `http://localhost:8080/swagger-ui.html`

## Variables de entorno

| Variable | Descripcion | Requerida |
|----------|-------------|-----------|
| `SPRING_DATASOURCE_URL` | URL de conexion a PostgreSQL | No (default: localhost) |
| `SPRING_DATASOURCE_USERNAME` | Usuario de DB | No (default: postgres) |
| `SPRING_DATASOURCE_PASSWORD` | Password de DB | No (default: postgres) |
| `JWT_SECRET` | Clave secreta para firmar JWT | Si |
| `WASABI_ACCESS_KEY` | Access key de Wasabi | Si |
| `WASABI_SECRET_KEY` | Secret key de Wasabi | Si |
| `WASABI_BUCKET_NAME` | Nombre del bucket | No (default: sunat-consultas) |
| `WASABI_REGION` | Region de Wasabi | No (default: us-west-1) |
| `CORS_ALLOWED_ORIGINS` | Origenes permitidos para CORS | No (default: http://localhost:3000) |
| `SPRING_PROFILES_ACTIVE` | Perfil activo (dev/prod) | No (default: dev) |

## Tests

```bash
mvn test
```
