package org.example.kursach;

import org.example.kursach.entity.*;
import org.example.kursach.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderIntegrationsTest extends KursachApplicationTests{

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    VehicleRepository vehicleRepository;

    @Autowired
    private OrderStatusRepository order_statusRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @BeforeEach
    public void add_orders(){
        Order order = create_order(create_vehicle_and_save_inDB()
                ,create_order_statuse_and_save_in_DB(),create_services_and_save_in_DB(),
                create_user_and_save_inDB());
        orderRepository.save(order);
    }

    private Order create_order(Vehicle vehicle, OrderStatus statuse, List<Service> services, User client){
        Order order = new Order();
        order.setVehicle(vehicle);
        order.setWorkers(new ArrayList<>());
        order.setStatuse(statuse);
        order.setServices(services);
        order.setClient(client);
        return order;
    }

    private Vehicle create_vehicle_and_save_inDB(){
        Vehicle vehicle = new Vehicle();
        vehicle.setModel("BMW");
        vehicle.setMake("I5");
        vehicle.setNumber("AB-34342");
        vehicleRepository.save(vehicle);
        return vehicle;
    }

    private OrderStatus create_order_statuse_and_save_in_DB(){
        OrderStatus orderStatuse = new OrderStatus();
        orderStatuse.setId("NEW");
        orderStatuse.setName("Новый");
        order_statusRepository.save(orderStatuse);
        return orderStatuse;
    }

    private List<Service> create_services_and_save_in_DB(){
        Service service = new Service();
        service.setName("Замена колеса");
        service.setPrice(22);
        serviceRepository.save(service);
        return List.of(service);
    }

    private User create_user_and_save_inDB(){
        User user = new User();
        user.setName("Иван");
        user.setPassword("11111");
        user.setEmail("ivan@gmail.com");
        userRepository.save(user);
        return user;
    }

    @AfterEach
    public void deleteAll(){
        userRepository.deleteAll();
        orderRepository.deleteAll();;
        serviceRepository.deleteAll();
        vehicleRepository.deleteAll();
        order_statusRepository.deleteAll();
    }

    @Test
    @DisplayName("Успешное получение информации всех заказов")
    public void successful_getAll_orders_test() throws Exception {

        mockMvc.perform(
                get("/api/order/getAll")
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("Новый"))
                .andExpect(jsonPath("$[0].client.email").value("ivan@gmail.com"));
    }
}
