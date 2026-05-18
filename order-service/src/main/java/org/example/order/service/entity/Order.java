package org.example.order.service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
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
    private UUID clientId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderItem> orderItems;

    @ElementCollection
    @CollectionTable(name = "orders_workers",joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "worker_id")
    @BatchSize(size = 20)
    private Set<UUID> workerIds;

    public void setStatus(OrderStatus status){
        this.status = status;
        registerEvent(new Object());
    }

    public void replaceWorkers(Set<UUID> newWorkerIds, Map<UUID, String> workerEmails) {
        Set<UUID> currentIds = new HashSet<>(this.workerIds);

        for (UUID oldId : currentIds) {
            if (!newWorkerIds.contains(oldId)) {
                deleteWorker(oldId);
            }
        }

        for (UUID newId : newWorkerIds) {
            if (!currentIds.contains(newId)) {
                String email = workerEmails.get(newId);
                addWorker(newId, email);
            }
        }
    }

    public void clearWorkers() {
        this.workerIds.clear();
    }

    private void addWorker(UUID workerId, String email) {
        if (this.workerIds.add(workerId)) {
            registerEvent(new Object());
        }
    }

    private void deleteWorker(UUID workerId) {
        this.workerIds.remove(workerId);
    }
}
