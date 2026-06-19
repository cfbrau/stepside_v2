package com.stepside.StepSide.notification.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitario de infraestructura encargado de la interpolación en caliente de textos.
 * Refactorizado bajo estándares Staff para optimizar el Heap de memoria en Google Cloud Run.
 */
public final class TemplateProcessor {

    // Compilamos la expresión regular una sola vez de forma estática en el bytecode
    private static final Pattern COMPILED_TOKEN_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    // Bloqueamos instanciaciones huérfanas mediante constructor privado
    private TemplateProcessor() {
        throw new UnsupportedOperationException("Clase utilitaria estática de infraestructura.");
    }

    /**
     * Procesa un texto crudo reemplazando las llaves dobles por los valores del mapa.
     * Optimiza la complejidad temporal buscando únicamente los tokens declarados en la plantilla.
     *
     * @param rawTemplate Cadena con el formato HTML o texto extraído de MongoDB Atlas.
     * @param model Mapa asociativo elástico con las variables dinámicas calculadas.
     * @return El texto final procesado e interpolado listo para producción.
     */
    public static String process(String rawTemplate, Map<String, Object> model) {
        // Mantenemos tu impecable lógica defensiva original
        if (rawTemplate == null || rawTemplate.isBlank()) {
            return "";
        }
        if (model == null || model.isEmpty()) {
            return rawTemplate;
        }

        Matcher matcher = COMPILED_TOKEN_PATTERN.matcher(rawTemplate);
        StringBuilder builder = new StringBuilder();

        // El bucle se ejecuta ÚNICAMENTE la cantidad exacta de veces que existan comodines en el texto
        while (matcher.find()) {
            String tokenKey = matcher.group(1).trim();
            Object valueObject = model.get(tokenKey);

            // Si la variable no fue provista por el servicio, se limpia con un string vacío por protección
            String replacement = (valueObject != null) ? valueObject.toString() : "";

            // Evitamos la inyección de caracteres quoteando el reemplazo de forma segura
            matcher.appendReplacement(builder, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(builder);

        return builder.toString();
    }
}
