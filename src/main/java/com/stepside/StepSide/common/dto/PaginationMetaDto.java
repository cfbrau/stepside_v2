package com.stepside.StepSide.common.dto;

/**
 * Contrato inmutable público para el transporte de metadatos de paginación de grillas NoSQL.
 */
public record PaginationMetaDto(
        int totalElements,
        int totalPages,
        int currentPage
) {
    /**
     * Factoría estática Senior para inicializar la paginación inicial elástica.
     */
    public static PaginationMetaDto ofSinglePage(int size) {
        return new PaginationMetaDto(size, 1, 1);
    }
}
