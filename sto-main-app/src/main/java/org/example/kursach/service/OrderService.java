package org.example.kursach.service;


import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.kursach.dto.*;
import org.example.kursach.entity.*;
import org.example.kursach.mapping.ClientOrderDTOMap;
import org.example.kursach.mapping.OrderDTOMap;
import org.example.kursach.repository.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final StatusRepository statusRepository;
    private final OrderDTOMap orderDTOMap;
    private final ClientOrderDTOMap clientOrderDTOMap;
    private final VehicleRepository vehicleRepository;
    private final ServiceRepository serviceRepository;
    private final StationsRepository stationsRepository;

    @Transactional
    public List<OrderDTO> findAll(Long serviceId){
        Stations station = stationsRepository.
                findById(serviceId).orElseThrow(() -> new EntityNotFoundException("Станции с таким id не существует"));

        return orderRepository.findAllByStation(station).stream().map(orderDTOMap).toList();
    }

    public List<ClientOrderDTO> findUserOrder(UserPrincipal userPrincipal){
        User user = userRepository.findByEmail(userPrincipal.email());
        return orderRepository.findAllByClient(user).stream().map(clientOrderDTOMap).toList();
    }

    @Transactional
    public void updateStatus(Long id, OrderStatus status){
        Order order=orderRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        if(!order.getStatuse().getName().equals(status.getName())){
            order.setStatuse(statusRepository.findById(status.getId()).orElseThrow(EntityNotFoundException::new));
        }
    }

    @Transactional
    public void updateOrder(Update_orderDTO orderDTO, Long order_id){
        Order order = orderRepository.findById(order_id).orElseThrow(EntityNotFoundException::new);
        if(!order.getStatuse().getId().equals(orderDTO.getStatus_id())){
            OrderStatus new_status = statusRepository.findById(orderDTO.getStatus_id()).orElseThrow(EntityNotFoundException::new);
            order.setStatuse(new_status);
        }

        List<Long> new_workers_id = orderDTO.getWorkers_id();
        if(new_workers_id == null || new_workers_id.isEmpty()){
            order.clearWorkers();
        }
        else {
            order.replace_workers(new HashSet<>(userRepository.findAllById(new_workers_id)));
        }
    }

    @Transactional
    public void createElement(ReguestOrderDTO create_order, UserPrincipal userPrincipal){
            Order save_order = new Order();
            save_order.setWorkers(new ArrayList<>());
            Vehicle vehicle = vehicleRepository.save(create_order.getVehicle());
            save_order.setVehicle(vehicle);
        save_order.setServices(create_order.getServiceId().
                stream().
                map(s->serviceRepository.findById(s).orElseThrow()).toList());
        save_order.setStatuse(statusRepository.findById("NEW").get());
        save_order.setClient(userRepository.findByEmail(userPrincipal.email()));
        save_order.setStation(stationsRepository.findById(create_order.getStationId()).orElseThrow());
        orderRepository.save(save_order);
    }

    public OrderDTO find(Long id) {
        return orderDTOMap.apply(orderRepository.findById(id).orElseThrow());
    }

}
