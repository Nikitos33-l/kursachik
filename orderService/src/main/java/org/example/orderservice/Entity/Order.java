package org.example.orderservice.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.util.*;

@Entity
@Table(name="orders")
@Setter
@Getter
@NoArgsConstructor
public class Order extends AbstractAggregateRoot<Order> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="orders_id")
    private Long id;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @ManyToOne
    @JoinColumn(name="status")
    @Setter(lombok.AccessLevel.NONE)
    private OrderStatus status;

    @Column(name = "station_id")
    private Long stationId;

    @Column(name = "client_id")
    private Long clientId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderItem> orderItems;

    @ElementCollection
    @CollectionTable(name = "orders_workers",joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "worker_id")
    private Set<Long> workerIds;


    public void setStatus(OrderStatus status){
        this.status = status;
        registerEvent(new Object());
    }

    public void replaceWorkers(Set<Long> newWorkerIds, Map<Long, String> workerEmails) {
        Set<Long> currentIds = new HashSet<>(this.workerIds);

        for (Long oldId : currentIds) {
            if (!newWorkerIds.contains(oldId)) {
                deleteWorker(oldId);
            }
        }

        for (Long newId : newWorkerIds) {
            if (!currentIds.contains(newId)) {
                String email = workerEmails.get(newId);
                addWorker(newId, email);
            }
        }
    }

    public void clearWorkers() {
        this.workerIds.clear();
    }

    private void addWorker(Long workerId, String email) {
        if (this.workerIds.add(workerId)) {
            registerEvent(new Object());
        }
    }

    private void deleteWorker(Long workerId) {
        this.workerIds.remove(workerId);
    }
}
