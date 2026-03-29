package org.example.kursach.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @OneToOne
    @JoinColumn(name="venicle_id")
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name="status")
    private OrderStatus statuse;

    @ManyToOne
    @JoinColumn(name = "station_id")
    private Stations station;

    @ManyToOne()
    @JoinColumn(name="client_id")
    private User client;

    @ManyToMany
    @JoinTable(
            name = "order_workers",
            joinColumns = @JoinColumn(name="order_id"),
            inverseJoinColumns = @JoinColumn(name="worker_id")
    )
    private List<User> workers = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name="order_services",
            joinColumns = @JoinColumn(name="order_id"),
            inverseJoinColumns = @JoinColumn(name="service_id")
    )
    private List<Service> services;

    public void replace_workers(HashSet<User> new_workers){
        Set<User> current = new HashSet<>(workers);

        for(User old_worker : current){
            if(!new_workers.contains(old_worker)) {
                deleteWorkers(old_worker);
            }
        }
        for(User new_worker : new_workers){
            if(!current.contains(new_worker)) {
                addWorkers(new_worker);
            }
        }
    }

    public void clearWorkers(){
        for(User worker : new HashSet<>(workers)){
            deleteWorkers(worker);
        }
    }

    private void addWorkers(User worker){
        if(workers.add(worker)){
            worker.getWorker_orders().add(this);
        }
    }

    private void deleteWorkers(User worker){
        if(workers.remove(worker)){
            worker.getWorker_orders().remove(this);
        }
    }
}
