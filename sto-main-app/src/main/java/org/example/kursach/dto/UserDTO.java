package org.example.kursach.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public class UserDTO {
   @Null
   private final Long id;

   @NotBlank
   private final String name;

   @NotBlank
   @Email
   private final String email;

}
