package org.example.user.service.mapper;

import org.example.user.api.responceDto.UserDto;
import org.example.user.api.responceDto.VehicleDto;
import org.example.user.service.dto.response.ResponseUserDto;
import org.example.user.service.dto.response.UserShortResponse;
import org.example.user.service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "role",source = "user.role.name")
    ResponseUserDto toResponseUserDto(User user);

    List<ResponseUserDto> toListResponseUserDto(List<User> users);

    UserShortResponse toShortResponse(User user);

    List<UserShortResponse> toListShortResponse(List<User> users);

    UserDto toDto(User user);

    List<UserDto> toDtoList(List<User> users);
}
