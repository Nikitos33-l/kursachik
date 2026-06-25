package org.example.order.service.integration;

import org.example.order.service.dto.OrderNotificationDto;
import org.example.order.service.dto.request.PutOrderRequestDto;
import org.example.order.service.dto.request.RequestOrderDto;
import org.example.order.service.dto.request.RequestOrderStatusDto;
import org.example.order.service.dto.request.RequestVehicleDto;
import org.example.order.service.entity.Order;
import org.example.order.service.entity.OrderItem;
import org.example.order.service.entity.OrderStatus;
import org.example.order.service.repository.OutboxEventRepository;
import org.example.station.service.api.common.client.StationServiceClient;
import org.example.station.service.api.common.dto.response.ServiceDetailDto;
import org.example.station.service.api.common.dto.response.StationServicesResponse;
import org.example.station.service.api.common.dto.response.SummaryResponseStationDto;
import org.example.user.api.client.UserServiceFeignClient;
import org.example.user.api.responceDto.OrderInfoFromUserServiceDto;
import org.example.user.api.responceDto.ValidationResponse;
import org.example.user.api.responceDto.VehicleDto;
import org.example.user.contracts.UserUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message; // Важно: импортируем AMQP Message
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class OrderIntegrationTest extends BaseIntegrationTest {

    @MockitoBean private UserServiceFeignClient userServiceClient;
    @MockitoBean private StationServiceClient stationServiceClient;

    @MockitoSpyBean private RabbitTemplate rabbitTemplate;

    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private CacheManager cacheManager;

    @Value("${station.delete.queue}") private String stationDeleteQueue;
    @Value("${station.services.updated.queue}") private String stationServicesUpdatedQueue;
    @Value("${user.delete.queue}") private String userDeleteQueue;
    @Value("${user.update.queue}") private String userUpdateQueue;

    private UUID clientId;
    private UUID workerId;
    private UUID adminId;
    private final Long stationId = 1L;

    private OrderStatus statusNew;
    private OrderStatus statusInProgress;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();

        statusNew = new OrderStatus();
        statusNew.setId("NEW");
        statusNew.setName("Новый");
        orderStatusRepository.save(statusNew);

        statusInProgress = new OrderStatus();
        statusInProgress.setId("IN_PROGRESS");
        statusInProgress.setName("В работе");
        orderStatusRepository.save(statusInProgress);

        clientId = UUID.randomUUID();
        workerId = UUID.randomUUID();
        adminId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Создание заказа: успешное сохранение в БД и асинхронная отправка сырого JSON в RabbitMQ")
    void createOrder_ShouldSaveToDbAndSendNotification() throws Exception {
        RequestOrderDto requestDto = new RequestOrderDto(
                new RequestVehicleDto("Tesla", "Model Y", "7777-AA-7"),
                List.of(10L, 11L),
                stationId
        );

        VehicleDto mockVehicle = new VehicleDto(100L, "Tesla", "Model Y", "7777-AA-7");
        when(userServiceClient.getOrCreateCar(any())).thenReturn(mockVehicle);

        StationServicesResponse mockStationResponse = new StationServicesResponse(
                true,
                List.of(
                        new ServiceDetailDto(10L, "Замена масла", BigDecimal.valueOf(150.0)),
                        new ServiceDetailDto(11L, "Диагностика подвески", BigDecimal.valueOf(50.0))
                )
        );
        when(stationServiceClient.validateStationAndGetServices(eq(stationId), any())).thenReturn(mockStationResponse);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/create")
                        .headers(getSecurityHeaders("ROLE_CLIENT", stationId, clientId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        List<Order> savedOrders = orderRepository.findAll();
        assertThat(savedOrders).hasSize(1);
        Order savedOrder = savedOrders.get(0);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> verify(rabbitTemplate, times(1)).send(
                        anyString(),
                        anyString(),
                        argThat(message -> {
                            try {
                                // Парсим payload из байтов обратно в DTO для проверки полей
                                String json = new String(message.getBody(), StandardCharsets.UTF_8);
                                OrderNotificationDto dto = objectMapper.readValue(json, OrderNotificationDto.class);
                                return dto.orderId().equals(savedOrder.getId()) &&
                                        "test@mail.com".equals(dto.email()) &&
                                        "NEW".equals(dto.type());
                            } catch (Exception e) {
                                return false;
                            }
                        })
                ));
    }

    @Test
    @DisplayName("Получение заказа: корректный сбор данных из БД и Feign-клиента пользователя")
    void findOrder_ShouldReturnOrderDto() throws Exception {
        Order order = createAndSaveSampleOrder();

        OrderInfoFromUserServiceDto mockUserServiceResponse = OrderInfoFromUserServiceDto.builder()
                .workers(List.of())
                .build();
        when(userServiceClient.getOrderInfo(any())).thenReturn(mockUserServiceResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/order/get/{id}", order.getId())
                        .headers(getSecurityHeaders("ROLE_CLIENT", stationId, clientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.status").value("Новый"));
    }

    @Test
    @DisplayName("Обновление статуса: успешное изменение в БД и асинхронный пуш байтового JSON в брокер")
    void updateOrderStatus_ShouldUpdateDbAndTriggerRabbitTemplate() throws Exception {
        Order order = createAndSaveSampleOrder();
        RequestOrderStatusDto statusDto = new RequestOrderStatusDto("IN_PROGRESS");

        when(userServiceClient.getEmailByUserId(order.getClientId())).thenReturn("lazy-client@test.com");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/order/updateStatus/{id}", order.getId())
                        .headers(getSecurityHeaders("ROLE_WORKER", stationId, workerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk());

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus().getId()).isEqualTo("IN_PROGRESS");

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> verify(rabbitTemplate, times(1)).send(
                        anyString(),
                        anyString(),
                        argThat(message -> {
                            try {
                                String json = new String(message.getBody(), StandardCharsets.UTF_8);
                                OrderNotificationDto dto = objectMapper.readValue(json, OrderNotificationDto.class);
                                return dto.orderId().equals(order.getId()) &&
                                        "lazy-client@test.com".equals(dto.email()) &&
                                        "IN_PROGRESS".equals(dto.type());
                            } catch (Exception e) {
                                return false;
                            }
                        })
                ));
    }

    @Test
    @DisplayName("Редактирование заказа администратором: назначение воркеров и асинхронная отправка события")
    void updateOrder_ShouldAssignWorkersAndSendNotification() throws Exception {
        Order order = createAndSaveSampleOrder();
        UUID newWorkerId = UUID.randomUUID();
        PutOrderRequestDto putOrderRequestDto = new PutOrderRequestDto(Set.of(newWorkerId), "NEW");

        ValidationResponse mockValidation = new ValidationResponse(
                true,
                Map.of(newWorkerId, "new-worker@test.com")
        );
        when(userServiceClient.validateWorkers(any())).thenReturn(mockValidation);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/order/updateOrder/{id}", order.getId())
                        .headers(getSecurityHeaders("ROLE_ADMIN", stationId, adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(putOrderRequestDto)))
                .andExpect(status().isOk());

        Order updatedOrder = orderRepository.findWithWorkerIdsById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getWorkerIds()).containsExactly(newWorkerId);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> verify(rabbitTemplate, times(1)).send(
                        anyString(),
                        anyString(),
                        argThat(message -> {
                            try {
                                String json = new String(message.getBody(), StandardCharsets.UTF_8);
                                OrderNotificationDto dto = objectMapper.readValue(json, OrderNotificationDto.class);
                                return dto.orderId().equals(order.getId()) &&
                                        "new-worker@test.com".equals(dto.email()) &&
                                        "NEW_ORDER_FOR_WORKER".equals(dto.type());
                            } catch (Exception e) {
                                return false;
                            }
                        })
                ));
    }

    @Test
    @DisplayName("Поиск всех заказов: успешный возврат списка заказов текущей станции для администратора")
    void findAll_ShouldReturnStationOrdersForAdmin() throws Exception {
        Order order = createAndSaveSampleOrder();

        OrderInfoFromUserServiceDto mockInfo = OrderInfoFromUserServiceDto.builder()
                .workers(List.of())
                .build();
        when(userServiceClient.getOrdersInfo(any())).thenReturn(Map.of(order.getId(), mockInfo));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/order/getAll")
                        .headers(getSecurityHeaders("ROLE_ADMIN", stationId, adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(order.getId()));
    }

    @Test
    @DisplayName("Личный кабинет клиента: получение информации о машинах и СТО для истории заказов клиента")
    void findUserOrder_ShouldReturnSummaryForClient() throws Exception {
        Order order = createAndSaveSampleOrder();

        when(userServiceClient.getCarsInfo(any()))
                .thenReturn(Map.of(order.getVehicleId(), new VehicleDto(100L, "BMW", "X5", "1111-BB-7")));

        when(stationServiceClient.getStationsByOrders(any()))
                .thenReturn(Map.of(order.getStationId(), new SummaryResponseStationDto(stationId, "Центральная СТО", "Минск")));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/order/getClientOrder")
                        .headers(getSecurityHeaders("ROLE_CLIENT", stationId, clientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("Новый"))
                .andExpect(jsonPath("$[0].stationName").value("Центральная СТО"));
    }

    @Test
    @DisplayName("Заказы воркера: успешный возврат списка всех заказов, на которые назначен текущий сотрудник")
    void findWorkerOrders_ShouldReturnOrdersForAssignedWorker() throws Exception {
        Order order = createAndSaveSampleOrder();
        order.setWorkerIds(Set.of(workerId));
        orderRepository.save(order);

        OrderInfoFromUserServiceDto mockInfo = OrderInfoFromUserServiceDto.builder()
                .workers(List.of())
                .build();
        when(userServiceClient.getOrdersInfo(any())).thenReturn(Map.of(order.getId(), mockInfo));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/order/getWorkerOrder")
                        .headers(getSecurityHeaders("ROLE_WORKER", stationId, workerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(order.getId()));
    }

    @Test
    @DisplayName("Consumer: Удаление СТО — каскадное удаление всех заказов станции из БД")
    void handleStationDelete_ShouldDeleteAllStationOrders() {
        Order order = createAndSaveSampleOrder();
        Long targetStationId = order.getStationId();
        String cacheKey = "order-service:station-validation::station:" + targetStationId + ":services:[10,20]";
        redisTemplate.opsForValue().set(cacheKey, "cached-station-services-data");

        assertThat(orderRepository.findById(order.getId())).isPresent();

        rabbitTemplate.convertAndSend(stationDeleteQueue, targetStationId);

        Awaitility.await()
                .atMost(java.time.Duration.ofSeconds(5))
                .pollInterval(java.time.Duration.ofMillis(200))
                .untilAsserted(() -> {
                    List<Order> remainingOrders = orderRepository.findAllByStationId(targetStationId);
                    assertThat(remainingOrders).isEmpty();
                    Boolean hasKey = redisTemplate.hasKey(cacheKey);
                    assertThat(hasKey).isFalse();
                });
    }

    @Test
    @DisplayName("Consumer: Обновление услуг СТО — инвалидация и очистка кэша валидации в Redis")
    void onStationServicesUpdated_ShouldEvictStationValidationCache() {
        Long targetStationId = 999L;
        String cacheKey = "order-service:station-validation::station:" + targetStationId + ":services:[10,20]";

        redisTemplate.opsForValue().set(cacheKey, "cached-station-services-data");
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

        rabbitTemplate.convertAndSend(stationServicesUpdatedQueue, targetStationId);

        Awaitility.await()
                .atMost(java.time.Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Boolean hasKey = redisTemplate.hasKey(cacheKey);
                    assertThat(hasKey).isFalse();
                });
    }

    @Test
    @DisplayName("Consumer: Удаление пользователя — удаление истории заказов и очистка email-кэша")
    void handleUserDelete_ShouldDeleteOrdersAndEvictEmailCache() {
        Order order = createAndSaveSampleOrder();
        UUID targetUserId = order.getClientId();

        Cache emailCache = cacheManager.getCache(org.example.order.service.constant.CacheNames.USER_EMAIL_CACHE);
        assertThat(emailCache).isNotNull();
        emailCache.put(targetUserId, "client-email@test.com");

        rabbitTemplate.convertAndSend(userDeleteQueue, targetUserId);

        Awaitility.await()
                .atMost(java.time.Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<Order> remainingOrders = orderRepository.findAllByClientId(targetUserId);
                    assertThat(remainingOrders).isEmpty();

                    String cachedEmail = emailCache.get(targetUserId, String.class);
                    assertThat(cachedEmail).isNull();
                });
    }

    @Test
    @DisplayName("Consumer: Изменение данных пользователя — инвалидация устаревшего кэша email")
    void handleUpdateUser_ShouldEvictCacheOnUserUpdateEvent() {
        UUID targetUserId = UUID.randomUUID();
        Cache emailCache = cacheManager.getCache(org.example.order.service.constant.CacheNames.USER_EMAIL_CACHE);
        assertThat(emailCache).isNotNull();
        emailCache.put(targetUserId, "old-email@test.com");

        UserUpdateEvent updateEvent = new UserUpdateEvent(targetUserId, "new-email@test.com");

        rabbitTemplate.convertAndSend(userUpdateQueue, updateEvent);

        Awaitility.await()
                .atMost(java.time.Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    String cachedEmail = emailCache.get(targetUserId, String.class);
                    assertThat(cachedEmail).isNull();
                });
    }

    private Order createAndSaveSampleOrder() {
        Order order = new Order();
        order.setStationId(stationId);
        order.setVehicleId(100L);
        order.setClientId(clientId);
        order.setWorkerIds(new HashSet<>());
        order.setStatus(statusNew);

        OrderItem item = new OrderItem();
        item.setServiceId(10L);
        item.setServiceName("Тест услуга");
        item.setPriceAtOrder(BigDecimal.valueOf(100));

        order.setOrderItems(new ArrayList<>(List.of(item)));

        return orderRepository.save(order);
    }
}