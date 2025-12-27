package org.example.kursach.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.example.kursach.dto.All_User_infoDTO;
import org.example.kursach.dto.OrderDTO;
import org.example.kursach.dto.Reguest_User_DTO;
import org.example.kursach.dto.UserDTO;
import org.example.kursach.entity.User;
import org.example.kursach.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
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
    public void add_user(@RequestBody @Valid Reguest_User_DTO user){
        userService.add_user(user);
    }

    @GetMapping("/find/workerOrders")
    public List<OrderDTO> get_worker_order(HttpServletRequest request){
        return userService.find_worker_orders(request);
    }
}
