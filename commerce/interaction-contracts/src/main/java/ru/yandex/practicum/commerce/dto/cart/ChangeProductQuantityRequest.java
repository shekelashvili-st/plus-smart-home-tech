package ru.yandex.practicum.commerce.dto.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Value;
import org.hibernate.validator.constraints.UUID;

@Value
public class ChangeProductQuantityRequest {
    @NotNull
    @UUID
    String productId;

    @Positive
    int newQuantity;
}
