package org.example.kursach.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.kursach.dto.*;
import org.example.kursach.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){
        this.userService=userService;
    }

    @GetMapping("/getAll")
    @PreAuthorize("hasRole('ADMIN')")
    public List<All_User_infoDTO> findAll(){
        return userService.findAll();
    }

    @GetMapping("/get/all/workers")
    public List<UserDTO> get_workers(){
        return userService.get_all_workers();
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id){
        userService.delete_element(id);
    }

    @GetMapping("/get/info/{id}")
    public UserDTO get_info(@PathVariable Long id){
        return userService.get_info(id);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void update_user(@PathVariable Long id,@RequestBody @Valid UserDTO user){
        userService.update(id,user);
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public void addUser(@RequestBody @Valid Reguest_User_DTO user, @AuthenticationPrincipal UserPrincipal currentUser){
        userService.add_user(user,currentUser);
    }

    @GetMapping("/find/workerOrders")
    public List<OrderDTO> get_worker_order(HttpServletRequest request){
        return userService.find_worker_orders(request);
    }
}
