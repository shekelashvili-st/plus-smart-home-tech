package ru.yandex.practicum.commerce.dto.payment;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode(of = "paymentId")
public class PaymentDto {

    private UUID paymentId;

    private BigDecimal totalPayment;

    private BigDecimal deliveryTotal;

    private BigDecimal feeTotal;
}
