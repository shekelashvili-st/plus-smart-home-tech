package ru.yandex.practicum.commerce.dto.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.UUID;

import java.util.Map;

@Getter
@Setter
@Builder
@EqualsAndHashCode(of = "shoppingCardId")
public class ShoppingCartDto {
    @NotNull
    @UUID
    private String shoppingCardId;

    @NotNull
    private Map<@NotNull @UUID String, @NotNull @Positive Integer> products;
}
