package org.example.order.service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.order.service.event.WorkerAssignmentEvent;
import org.hibernate.annotations.BatchSize;

import java.util.*;

@Entity
@Table(name="orders")
@Setter
@Getter
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="orders_id")
    private Long id;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @ManyToOne
    @JoinColumn(name="status", nullable = false)
    private OrderStatus status;

    @Column(name = "station_id")
    private Long stationId;

    @Column(name = "client_id")
    private UUID clientId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false)
    private List<OrderItem> orderItems = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "orders_workers", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "worker_id")
    @BatchSize(size = 20)
    private Set<UUID> workerIds = new HashSet<>();

    public List<WorkerAssignmentEvent> replaceWorkers(Set<UUID> newWorkerIds, Map<UUID, String> workerEmails) {
        Set<UUID> currentIds = new HashSet<>(this.workerIds);
        List<WorkerAssignmentEvent> eventsToPublish = new ArrayList<>();

        for (UUID oldId : currentIds) {
            if (!newWorkerIds.contains(oldId)) {
                this.workerIds.remove(oldId);
            }
        }

        for (UUID newId : newWorkerIds) {
            if (!currentIds.contains(newId)) {
                this.workerIds.add(newId);
                String email = workerEmails.get(newId);
                eventsToPublish.add(new WorkerAssignmentEvent(this.getId(), email));
            }
        }

        return eventsToPublish;
    }

    public void clearWorkers() {
        this.workerIds.clear();
    }
}