package ru.yandex.practicum.commerce.delivery.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.commerce.contract.delivery.DeliveryOperations;
import ru.yandex.practicum.commerce.dto.delivery.DeliveryDto;
import ru.yandex.practicum.commerce.dto.order.OrderDto;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/delivery")
@RequiredArgsConstructor
@Validated
public class DeliveryController implements DeliveryOperations {

    @Override
    public DeliveryDto createDelivery(@Valid @NotNull DeliveryDto delivery) {
        return null;
    }

    @Override
    public void successfulDelivery(@NotNull UUID orderId) {

    }

    @Override
    public void failedDelivery(@NotNull UUID orderId) {

    }

    @Override
    public void pickedDelivery(@NotNull UUID orderId) {

    }

    @Override
    public BigDecimal calculateDeliveryCost(@Valid @NotNull OrderDto order) {
        return null;
    }
}
