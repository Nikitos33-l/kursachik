package org.example.kursach.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.kursach.dto.*;
import org.example.kursach.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){
        this.userService=userService;
    }

    @GetMapping("/getAll")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AllUserInfoDTO> findAll(){
        return userService.findAll();
    }

    @GetMapping("/get/all/workers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDTO> getWorkers(@AuthenticationPrincipal UserPrincipal userPrincipal){
        return userService.getAllWorkers(userPrincipal.stationId());
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id){
        userService.deleteElement(id);
    }

    @GetMapping("/get/info/{id}")
    public UserDTO getInfo(@PathVariable Long id){
        return userService.getInfo(id);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void updateUser(@PathVariable Long id, @RequestBody @Valid UserDTO user){
        userService.update(id,user);
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public void addUser(@RequestBody @Valid ReguestUserDTO user, @AuthenticationPrincipal UserPrincipal currentUser){
        userService.addUser(user,currentUser);
    }

    @GetMapping("/find/workerOrders")
    public List<OrderDTO> getWorkerOrder(HttpServletRequest request){
        return userService.findWorkerOrders(request);
    }
}
