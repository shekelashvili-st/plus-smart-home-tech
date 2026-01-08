package ru.yandex.practicum.commerce.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.contract.warehouse.WarehouseClient;
import ru.yandex.practicum.commerce.dto.cart.ShoppingCartDto;
import ru.yandex.practicum.commerce.dto.order.CreateNewOrderRequest;
import ru.yandex.practicum.commerce.dto.order.OrderDto;
import ru.yandex.practicum.commerce.dto.order.OrderState;
import ru.yandex.practicum.commerce.dto.order.ProductReturnRequest;
import ru.yandex.practicum.commerce.exception.NoOrderFoundException;
import ru.yandex.practicum.commerce.order.mapper.AddressMapper;
import ru.yandex.practicum.commerce.order.mapper.OrderMapper;
import ru.yandex.practicum.commerce.order.model.OrderModel;
import ru.yandex.practicum.commerce.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final WarehouseClient warehouseClient;
    private final OrderMapper mapper;
    private final AddressMapper addressMapper;

    @Override
    public List<OrderDto> getOrders(String username) {
        return mapper.modelToDto(orderRepository.findAllByUsername(username));
    }

    @Override
    @Transactional
    public OrderDto putOrder(String username,
                             CreateNewOrderRequest newOrderRequest) {
        ShoppingCartDto cart = newOrderRequest.getShoppingCart();
        warehouseClient.checkShoppingCart(cart);
        OrderModel newOrder = OrderModel.builder()
                .address(addressMapper.dtoToModel(newOrderRequest.getDeliveryAddress()))
                .shoppingCartId(cart.getShoppingCartId())
                .products(cart.getProducts())
                .username(username)
                .state(OrderState.NEW)
                .build();
        return mapper.modelToDto(orderRepository.save(newOrder));
    }

    @Override
    @Transactional
    public OrderDto returnOrder(ProductReturnRequest productReturnRequest) {
        UUID orderId = productReturnRequest.getOrderId();
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order with id %s not found".formatted(orderId)));
        if (order.getProducts() != productReturnRequest.getProducts()) {
            throw new RuntimeException("Partial returns not supported");
        }
        warehouseClient.returnProducts(order.getProducts());
        order.setState(OrderState.PRODUCT_RETURNED);
        return mapper.modelToDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDto payForOrder(UUID orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order with id %s not found".formatted(orderId)));
        order.setState(OrderState.PAID);
        return mapper.modelToDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDto payFailedForOrder(UUID orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order with id %s not found".formatted(orderId)));
        order.setState(OrderState.PAYMENT_FAILED);
        return mapper.modelToDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDto deliverOrder(UUID orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order with id %s not found".formatted(orderId)));
        order.setState(OrderState.DELIVERED);
        return mapper.modelToDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDto deliveryFailedOrder(UUID orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order with id %s not found".formatted(orderId)));
        order.setState(OrderState.DELIVERY_FAILED);
        return mapper.modelToDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDto completeOrder(UUID orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order with id %s not found".formatted(orderId)));
        order.setState(OrderState.COMPLETED);
        return mapper.modelToDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDto calculateTotalForOrder(UUID orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order with id %s not found".formatted(orderId)));
        //Need to call another service for calc
        order.setTotalPrice(BigDecimal.valueOf(0));
        return mapper.modelToDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDto calculateDeliveryForOrder(UUID orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order with id %s not found".formatted(orderId)));
        //Need to call another service for calc
        order.setDeliveryPrice(BigDecimal.valueOf(0));
        return mapper.modelToDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDto assembleOrder(UUID orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order with id %s not found".formatted(orderId)));
        //Need to change the numbers in warehouse
        order.setState(OrderState.ASSEMBLED);
        return mapper.modelToDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDto assemblyFailedOrder(UUID orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order with id %s not found".formatted(orderId)));
        //Need to change the numbers in warehouse
        order.setState(OrderState.ASSEMBLY_FAILED);
        return mapper.modelToDto(orderRepository.save(order));
    }
}
