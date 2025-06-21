package com.ikeda.payment.dtos;

import java.util.UUID;

public record PaymentCommandRecordDto(UUID userId,
                                      UUID paymentoId,
                                      UUID cardID) {
}
