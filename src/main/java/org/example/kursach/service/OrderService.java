package org.example.kursach.service;


import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.example.kursach.dto.Client_OrderDTO;
import org.example.kursach.dto.OrderDTO;
import org.example.kursach.dto.ReguestOrderDTO;
import org.example.kursach.dto.Update_orderDTO;
import org.example.kursach.entity.Order;
import org.example.kursach.entity.Order_statuse;
import org.example.kursach.entity.User;
import org.example.kursach.entity.Venicle;
import org.example.kursach.mapping.Client_OrderDTO_map;
import org.example.kursach.mapping.OrderDTO_Map;
import org.example.kursach.repository.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


@Service
public class OrderService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final StatusRepository statusRepository;
    private final OrderDTO_Map orderDTOMap;
    private final Client_OrderDTO_map clientOrderDTO_map;
    private final JWTService jwtService;
    private final VehicleRepository vehicleRepository;
    private final ServiceRepository serviceRepository;

    public OrderService(UserRepository userRepository, OrderRepository orderRepository, StatusRepository statusRepository, Client_OrderDTO_map clientOrderDTOMap, JWTService jwtService, VehicleRepository vehicleRepository, ServiceRepository serviceRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.statusRepository=statusRepository;
        this.clientOrderDTO_map = clientOrderDTOMap;
        this.jwtService = jwtService;
        this.vehicleRepository = vehicleRepository;
        this.serviceRepository = serviceRepository;
        this.orderDTOMap=new OrderDTO_Map();
    }

    public List<OrderDTO> findAll(){
        return orderRepository.findAll().stream().map(orderDTOMap).toList();
    }

    public List<Client_OrderDTO> find_user_order(String token){
        User user = userRepository.findByEmail(jwtService.get_email(token));
        return orderRepository.findAllByClient(user).stream().map(clientOrderDTO_map).toList();
    }

    @Transactional
    public void update_status(Long id, Order_statuse status){
        Order order=orderRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        if(!order.getStatuse().getName().equals(status.getName())){
            order.setStatuse(statusRepository.findById(status.getId()).orElseThrow(EntityNotFoundException::new));
        }
    }

    @Transactional
    public void update_order(Update_orderDTO orderDTO, Long order_id){
        Order order = orderRepository.findById(order_id).orElseThrow(EntityNotFoundException::new);
        if(!order.getStatuse().getId().equals(orderDTO.getStatus_id())){
            Order_statuse new_status = statusRepository.findById(orderDTO.getStatus_id()).orElseThrow(EntityNotFoundException::new);
            order.setStatuse(new_status);
        }

        List<Long> new_workers_id = orderDTO.getWorkers_id();
        if(new_workers_id == null || new_workers_id.isEmpty()){
            order.clear_workers();
        }
        else {
            order.replace_workers(new HashSet<>(userRepository.findAllById(new_workers_id)));
        }
    }

    @Transactional
    public void create_element(ReguestOrderDTO create_order, String token){
            Order save_order = new Order();
            save_order.setWorkers(new ArrayList<>());
            Venicle venicle = vehicleRepository.save(create_order.getVehicle());
            save_order.setVenicle(venicle);
        save_order.setServices(create_order.getServiceId().
                stream().
                map(s->serviceRepository.findById(s).orElseThrow()).toList());
        save_order.setStatuse(statusRepository.findById("NEW").get());
        save_order.setClient(userRepository.findByEmail(jwtService.get_email(token)));
        orderRepository.save(save_order);
    }

    public OrderDTO find(Long id) {
        return orderDTOMap.apply(orderRepository.findById(id).orElseThrow());
    }


}
