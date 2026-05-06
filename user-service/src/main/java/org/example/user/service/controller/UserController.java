package org.example.user.service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.securitycommon.UserPrincipal;
import org.example.user.service.dto.request.RequestAddUserDto;
import org.example.user.service.dto.request.RequestUpdateUserDto;
import org.example.user.service.dto.response.ResponseUserDto;
import org.example.user.service.dto.response.UserShortResponse;
import org.example.user.service.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    @GetMapping("/getAll")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ResponseUserDto> getAll(@AuthenticationPrincipal UserPrincipal userPrincipal){
        return userService.getAll(userPrincipal.stationId());
    }

    @GetMapping("/get/all/workers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserShortResponse> getWorkers(@AuthenticationPrincipal UserPrincipal userPrincipal){
        return userService.getAllWorkers(userPrincipal.stationId());
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id){
        userService.deleteUser(id);
    }

    @GetMapping("/get/info/{id}")
    public UserShortResponse getInfo(@PathVariable Long id){
        return userService.getInfo(id);
    }

    @PutMapping("/update/{id}")
    public void updateUser(@PathVariable Long id, @RequestBody @Valid RequestUpdateUserDto userDto){
        userService.updateUser(id,userDto);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public void addUser(@RequestBody @Valid RequestAddUserDto userDto,@AuthenticationPrincipal UserPrincipal userPrincipal){
        userService.addUser(userDto,userPrincipal);
    }



}
