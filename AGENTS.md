# AGENTS.md

## Objetivo
Este repositorio es un backend en Java 21 con Spring Boot 3.4.3, Maven y MongoDB Atlas. La intención de esta guía es ayudar a agentes de IA a trabajar de forma segura y consistente con la arquitectura actual del proyecto.

## Contexto rápido
- Proyecto principal: [pom.xml](pom.xml)
- Documentación base de Spring Boot: [HELP.md](HELP.md)
- Aplicación principal: [src/main/java/com/stepside/StepSide/StepSideApplication.java](src/main/java/com/stepside/StepSide/StepSideApplication.java)
- Configuración raíz de entorno: [src/main/resources/application.properties](src/main/resources/application.properties)
- Perfil local de ejemplo: [src/main/resources/application-local.properties](src/main/resources/application-local.properties)

## Cómo compilar y probar
- Arranque local con perfil por defecto: `./mvnw spring-boot:run`
- Ejecutar tests: `./mvnw test`
- Build sin tests: `./mvnw -DskipTests package`
- Verificar compilación rápida: `./mvnw -q -DskipTests compile`

## Convenciones de arquitectura
- La base está organizada por features: `auth`, `users`, `ttos`, `notification`, `integration/arca`, `common`.
- Cada feature suele seguir la separación clásica:
  - `controller` para endpoints REST
  - `service` para lógica de negocio
  - `repository` para acceso a MongoDB
  - `dto` para contratos de entrada/salida
  - `model` para entidades/documentos
- Los servicios usan `@Service`, los repositorios `@Repository`, y los controladores REST suelen estar anotados con `@RestController` y `@RequestMapping`.
- El proyecto usa `@Valid` en DTOs para validación de entrada y un `@RestControllerAdvice` global para estandarizar errores.

## Convenciones de estilo
- Preferir `@RequiredArgsConstructor` para inyección de dependencias.
- Mantener nombres y paquetes coherentes con la feature actual; no mezclar responsabilidades entre módulos.
- Cuando agregues endpoints nuevos, conviene mantener el prefijo REST coherente con el área funcional, por ejemplo:
  - `/api/auth`
  - `/api/users`
  - `/api/ttos/reports`
  - `/api/integration/arca`
- Si un endpoint es público, debe estar permitido explícitamente en la configuración de seguridad.

## Seguridad y configuración
- La seguridad está centralizada en [src/main/java/com/stepside/StepSide/common/security/SecurityConfig.java](src/main/java/com/stepside/StepSide/common/security/SecurityConfig.java).
- El flujo principal usa JWT y CORS configurado por variables de entorno.
- La configuración de propiedades se resuelve desde variables de entorno en [src/main/resources/application.properties](src/main/resources/application.properties). 
- No hardcodear secretos ni credenciales. Usar variables de entorno y perfil local cuando corresponda.

## Integraciones externas
- La integración con ARCA está orientada a consultas SOAP y requiere propiedades específicas de entorno.
- El proyecto usa Spring WebFlux y OpenAPI/Swagger, por lo que cualquier cambio de endpoint debería mantener la documentación y la compatibilidad REST.

## Recomendaciones para agentes de IA
- Antes de cambiar una feature, seguir la estructura ya existente en su paquete correspondiente.
- Preferir cambios pequeños y consistentes con el diseño por capas.
- Si agregas una nueva operación a un controlador, revisar también el servicio, DTOs y el tratamiento de errores globales.
- Al trabajar con propiedades, conservar el patrón de mapeo por variables de entorno y no duplicar configuraciones.

## Recursos útiles
- [HELP.md](HELP.md)
- [pom.xml](pom.xml)
- [src/main/resources/application.properties](src/main/resources/application.properties)
- [src/main/resources/application-local.properties](src/main/resources/application-local.properties)
- [src/main/java/com/stepside/StepSide/common/security/SecurityConfig.java](src/main/java/com/stepside/StepSide/common/security/SecurityConfig.java)
- [src/main/java/com/stepside/StepSide/common/exception/GlobalExceptionHandler.java](src/main/java/com/stepside/StepSide/common/exception/GlobalExceptionHandler.java)
